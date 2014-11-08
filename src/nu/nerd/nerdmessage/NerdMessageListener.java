package nu.nerd.nerdmessage;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Iterator;

public class NerdMessageListener implements Listener {
    private NerdMessage plugin;

    public NerdMessageListener (NerdMessage plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat (AsyncPlayerChatEvent event) {
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
}
