package nu.nerd.nerdmessage;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import nu.nerd.nerdmessage.commands.BroadcastCommands;
import nu.nerd.nerdmessage.commands.ChatCommands;
import nu.nerd.nerdmessage.commands.IgnoreCommands;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class NerdMessage extends JavaPlugin {


    private List<NMUser> users = new CopyOnWriteArrayList<NMUser>();
    private HashMap<String, Integer> muteCounts = new HashMap<String, Integer>();
    private Integer alertThreshold;


    @Override
    public void onEnable() {
        loadConfig();
        registerCommands();
        this.getServer().getPluginManager().registerEvents(new NerdMessageListener(this), this);
    }


    public void registerCommands() {
        ChatCommands chatCommands = new ChatCommands(this);
        IgnoreCommands ignoreCommands = new IgnoreCommands(this);
        BroadcastCommands broadcastCommands = new BroadcastCommands(this);
    }


    public void loadConfig() {
        this.saveDefaultConfig();
        this.alertThreshold = this.getConfig().getInt("alert_threshold", 3);
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


    public HashMap<String, Integer> getMuteCounts() {
        return muteCounts;
    }


}
