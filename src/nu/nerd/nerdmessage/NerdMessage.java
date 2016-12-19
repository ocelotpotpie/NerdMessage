package nu.nerd.nerdmessage;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import com.avaje.ebean.EbeanServer;
import nu.nerd.nerdmessage.commands.BroadcastCommands;
import nu.nerd.nerdmessage.commands.ChatCommands;
import nu.nerd.nerdmessage.commands.IgnoreCommands;
import nu.nerd.nerdmessage.commands.MOTDCommands;
import nu.nerd.nerdmessage.commands.MailCommands;
import nu.nerd.nerdmessage.commands.OtherCommands;

import nu.nerd.nerdmessage.mail.MailHandler;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


public class NerdMessage extends JavaPlugin {


    public static NerdMessage instance;
    private List<NMUser> users = new CopyOnWriteArrayList<NMUser>();
    private HashMap<String, Integer> muteCounts = new HashMap<String, Integer>();
    private Integer alertThreshold;
    private String serverName;
    private JedisPool jedisPool;
    private MySQLHandler mySQLHandler;
    private MailHandler mailHandler;


    @Override
    public void onEnable() {
        instance = this;
        loadConfig();
        establishRedisConnection();
        mySQLHandler = new MySQLHandler(this, getConfig());
        registerCommands();
        mailHandler = new MailHandler(this);
        new NerdMessageListener(this);
    }


    @Override
    public void onDisable() {
    	if (jedisPool != null) {
    		jedisPool.destroy(); //clean up the pool of Redis connections
    	}
        if (mySQLHandler != null) {
            mySQLHandler.close(); //shut down the pool of MySQL connections
        }
    }


    /**
     * Register command executors
     */
    public void registerCommands() {
        new ChatCommands(this);
        new IgnoreCommands(this);
        new BroadcastCommands(this);
        new MOTDCommands(this);
        new OtherCommands(this);
        new MailCommands(this);
    }


    /**
     * Save the default config if not present and load values from the config file.
     * Redis connection details are handled by establishRedisConnection()
     */
    public void loadConfig() {
        this.saveDefaultConfig();
        this.alertThreshold = this.getConfig().getInt("alert_threshold", 3);
        this.serverName = this.getConfig().getString("server_name", null);
    }
    
    
    /*
     * Reload the configuration file and redis
     */
    public void reload() {
    	if (jedisPool != null) {
    		jedisPool.destroy();
    		jedisPool = null;
    	}
    	this.reloadConfig();
        this.alertThreshold = this.getConfig().getInt("alert_threshold", 3);
        this.serverName = this.getConfig().getString("server_name", null);
    	establishRedisConnection();
    }


    /**
     * Connect to the redis server and subscribe to "nerdmessage.*"
     * Redis is used for cross-server chat.
     */
    private void establishRedisConnection() {

        Boolean enabled = getConfig().getBoolean("redis.enabled", false);
        String server = getConfig().getString("redis.server", "localhost");
        String password = getConfig().getString("redis.password", null);
        Integer port = getConfig().getInt("redis.port", 6379);
        Integer timeout = getConfig().getInt("redis.timeout", 30);
        Integer connections = getConfig().getInt("redis.max_connections", 4);

        JedisPoolConfig poolconfig = new JedisPoolConfig();
        if (connections < 2) connections = 2; // redis requires at least two connections for pubsub
        poolconfig.setMaxTotal(connections);

        if (enabled && server != null && password != null && !password.equals("")) {
            jedisPool = new JedisPool(poolconfig, server, port, timeout, password);
        } else if (enabled && server != null) {
            jedisPool = new JedisPool(poolconfig, server, port, timeout);
        } else {
            getLogger().log(Level.WARNING, "Redis is not configured. Global broadcasts will not be available.");
            return;
        }

        getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
            public void run() {
                try {
                    Jedis jedis = getJedisResource();
                    jedis.psubscribe(new RedisListener(NerdMessage.this), "nerdmessage.*");
                } catch (Exception ex) {
                    getLogger().log(Level.SEVERE, ex.getMessage());
                }
            }
        });

    }


    /**
     * Publish a message to a Redis channel.
     * @param channel This will be concatenated with a "nerdmessage." namespace. e.g. nerdmessage.[channel]
     * @param message The message to send to the Redis channel
     * @throws Exception
     */
    public void redisPublish(final String channel, final String message) {
        redisPublish(null, channel, message);
    }


    /**
     * Publish a message to a Redis channel.
     * @param sender Who issued the command. This player/console will recieve notice if sending fails.
     * @param channel This will be concatenated with a "nerdmessage." namespace. e.g. nerdmessage.[channel]
     * @param message The message to send to the Redis channel
     */
    public void redisPublish(final CommandSender sender, final String channel, final String message) {
        getServer().getScheduler().runTaskAsynchronously(this, new BukkitRunnable() {
            public void run()  {
                Jedis jedis = null;
                try {
                    jedis = getJedisResource();
                    jedis.publish("nerdmessage." + channel, message);
                } catch (Exception ex) {
                    final String error = ex.getMessage();
                    getLogger().log(Level.SEVERE, error);
                    if (sender != null) {
                        getServer().getScheduler().runTask(NerdMessage.this, new BukkitRunnable() {
                            public void run() {
                                sender.sendMessage(ChatColor.RED + "NerdMessage Error: " + error);
                            }
                        });
                    }
                } finally {
                    if (jedis != null) jedis.close(); //return the resource to the pool
                }
            }
        });
    }


    /**
     * Check out a Jedis resource from the pool.
     * Don't forget to return it to the pool after...
     * @throws Exception
     */
    private Jedis getJedisResource() throws Exception {
        if (jedisPool != null) {
            return jedisPool.getResource();
        } else {
            throw new Exception("NerdMessage cannot connect to Redis.");
        }
    }


    /**
     * Whether Redis is configured
     */
    public boolean crossServerEnabled() {
        return jedisPool != null;
    }


    /**
     * Get a connection from the MySQL pool
     * @throws SQLException
     */
    public Connection getSQLConnection() throws SQLException {
        return mySQLHandler.getConnection();
    }


    /**
     * Get the eBean ORM instance
     */
    public EbeanServer getDatabase() {
        return mySQLHandler.getDatabase();
    }


    /**
     * Get the mail handler instance
     */
    public MailHandler getMailHandler() {
        return mailHandler;
    }


    public Player getPlayer(final String name) {
        Collection<? extends Player> players = getServer().getOnlinePlayers();

        Player found = null;
        String lowerName = name.toLowerCase();
        int delta = Integer.MAX_VALUE;
        for (Player player : players) {
            if (ChatColor.stripColor(player.getName()).toLowerCase().startsWith(lowerName)) {
                int curDelta = player.getName().length() - lowerName.length();
                if (curDelta < delta) {
                    found = player;
                    delta = curDelta;
                }
                if (curDelta == 0) break;
            }
        }
        return found;
    }


    public NMUser addUser(String username) {
        username = ChatColor.stripColor(username);
        NMUser u = new NMUser(this, username);
        users.add(u);
        return u;
    }


    public NMUser getOrCreateUser(String username) {
        username = ChatColor.stripColor(username);
        NMUser u = getUser(username);
        if (u == null) {
            u = addUser(username);
        }

        return u;
    }


    public NMUser getUser(String username) {
        username = ChatColor.stripColor(username);
        for (NMUser u : users) {
            if (username.equalsIgnoreCase(u.getName())) {
                return u;
            }
        }

        return null;
    }


    public void removeUser(String username) {
        NMUser u = getUser(username);
        if (u != null) {
            users.remove(u);
        }
    }


    public List<NMUser> getUsers() {
        return users;
    }


    public int getAlertThreshold() {
        return alertThreshold;
    }


    public String getServerName() {
        return serverName;
    }


    public HashMap<String, Integer> getMuteCounts() {
        return muteCounts;
    }


}
