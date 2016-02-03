package nu.nerd.nerdmessage;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.ref.WeakReference;
import java.util.Iterator;

public class NerdMessageListener implements Listener {


    private NerdMessage plugin;


    public NerdMessageListener (NerdMessage plugin) {
        this.plugin = plugin;
    }


    /**
     * Suppress messages from ignored players.
     * When an ignored player sends a message, loop through the recipients and remove
     * ones that have a mute on the player.
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String senderName = ChatColor.stripColor(event.getPlayer().getName()).toLowerCase();
        for (NMUser user : plugin.getUsers()) {
            if (user.isIgnoringPlayer(senderName)) {
                Iterator<Player> iter = event.getRecipients().iterator();
                while(iter.hasNext()) {
                    Player player = iter.next();
                    String name = ChatColor.stripColor(player.getName()).toLowerCase();
                    if(name.equalsIgnoreCase(user.getName())) {
                        iter.remove();
                    }
                }
            }
        }
    }
    
    
    /**
     * Send players the MOTDs when they join
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        final String motd = plugin.getConfig().getString("MOTD");

        final String mbmotd;
        if (player.hasPermission("nerdmessage.mb")) {
            mbmotd  = plugin.getConfig().getString("MBMOTD");
        } else {
            mbmotd = null;
        }
        
        final String abmotd;
        if (player.hasPermission("nerdmessage.ab")) {
            abmotd  = plugin.getConfig().getString("ABMOTD");
        } else {
            abmotd = null;
        }

        final WeakReference<Player> playerRef = new WeakReference<Player>(player);

        // Create the task anonymously and schedule to run it once, after 40 ticks
        new BukkitRunnable() {
 
            @Override
            public void run() {
                Player p = playerRef.get();
                if (p == null) {
                    return;
                }
                if (motd != null && !motd.equals("")) {
                    p.sendMessage(ChatColor.AQUA + "[MOTD]: " + motd);
                }
                if (mbmotd != null && !mbmotd.equals("")) {
                    p.sendMessage(ChatColor.GREEN + "[MB MOTD]: " + mbmotd);
                }
                if (abmotd != null && !abmotd.equals("")) {
                    p.sendMessage(ChatColor.GOLD + "[AB MOTD]: " + abmotd);
                }
            }
        }.runTaskLater(plugin, 40);
    }
}
