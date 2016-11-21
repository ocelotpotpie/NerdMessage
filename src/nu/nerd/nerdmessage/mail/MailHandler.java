package nu.nerd.nerdmessage.mail;

import nu.nerd.nerdmessage.NerdMessage;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;


public class MailHandler implements Listener {


    private NerdMessage plugin;


    public MailHandler(NerdMessage plugin) {
        this.plugin = plugin;
        if (!plugin.getConfig().getBoolean("mysql.enabled")) return;
        createTables();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        updateUUIDCacheOnJoin(event.getPlayer());
        checkNewMessagesOnJoin(event.getPlayer());
    }


    /**
     * Update the player UUID/name cache on join
     */
    private void updateUUIDCacheOnJoin(final Player player) {
        new BukkitRunnable() {
            public void run() {
                try {
                    Connection conn = plugin.getSQLConnection();
                    String sql = "INSERT INTO `user` (uuid, last_username, last_display_name, email) VALUES (?, ?, ?, NULL) ON DUPLICATE KEY UPDATE last_username=?, last_display_name=?;";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, player.getUniqueId().toString());
                    stmt.setString(2, player.getName().toLowerCase());
                    stmt.setString(3, player.getDisplayName());
                    stmt.setString(4, player.getName().toLowerCase());
                    stmt.setString(5, player.getDisplayName());
                    stmt.executeUpdate();
                    conn.close();
                } catch (SQLException ex) {
                    plugin.getLogger().warning(String.format("Error updating user table for player %s: %s", player.getName(), ex.getMessage()));
                }
            }
        }.runTaskLaterAsynchronously(plugin, 20L);
    }


    /**
     * Print a message to the user on join if they have waiting messages
     */
    private void checkNewMessagesOnJoin(final Player player) {
        new BukkitRunnable() {
            public void run() {
                if (MailMessage.findUnnotified(player.getUniqueId()).size() > 0) {
                    notifyNewMessages(player.getUniqueId(), true);
                    MailMessage.flagNotified(player.getUniqueId());
                }
            }
        }.runTaskLaterAsynchronously(plugin, 40L);
    }


    /**
     * Notify a user that they have a freshly sent message waiting, using redis for cross-server notifications.
     * If the user is online on this server, we can skip redis and update the "notified" flag in the database.
     * Otherwise, push to redis and listening servers will update if the player sees it there.
     * @param recipient the recipient of the message
     * @param isOrigin this is the server the message is originating from
     */
    public void notifyNewMessages(UUID recipient, boolean isOrigin) {
        boolean notified = false;
        Player player = plugin.getServer().getPlayer(recipient);
        String msg = String.format("%sYou have new mail! Type %s/mail inbox%s to read it.", ChatColor.GREEN, ChatColor.LIGHT_PURPLE, ChatColor.GREEN);
        if (player != null && player.isOnline()) {
            player.sendMessage(msg);
            notified = true;
        } else {
            if (plugin.crossServerEnabled() && isOrigin) {
                plugin.redisPublish("mail.new", recipient.toString());
            }
        }
        if (notified) {
            MailMessage.flagNotified(recipient);
        }
    }


    /**
     * Load the database schema if the tables don't exist yet
     */
    private void createTables() {
        try {
            Connection conn = plugin.getSQLConnection();
            if (conn == null) return;
            String message = "CREATE TABLE IF NOT EXISTS `message` (" +
                    "`id` mediumint(9) NOT NULL AUTO_INCREMENT," +
                    "`to` varchar(36) NOT NULL," +
                    "`from` varchar(36) NOT NULL," +
                    "`body` text NOT NULL," +
                    "`date_sent` bigint(20) NOT NULL," +
                    "`read` bit(1) DEFAULT NULL," +
                    "`notified` bit(1) DEFAULT NULL," +
                    "`source_server` varchar(32) DEFAULT NULL," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=latin1;";
            String user = "CREATE TABLE IF NOT EXISTS `user` (" +
                    "`uuid` varchar(36) NOT NULL," +
                    "`last_username` varchar(16) NOT NULL," +
                    "`email` varchar(255) DEFAULT NULL," +
                    "`last_display_name` varchar(16) DEFAULT NULL," +
                    "PRIMARY KEY (`uuid`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=latin1;";
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(message);
            stmt.executeUpdate(user);
            conn.close();
        } catch (SQLException ex) {
            plugin.getLogger().warning("Error creating database tables: " + ex.getMessage());
        }
    }


}
