package nu.nerd.nerdmessage.mail;

import com.avaje.ebean.Query;
import nu.nerd.nerdmessage.NerdMessage;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;


public class MailHandler implements Listener {


    private NerdMessage plugin;


    public MailHandler(NerdMessage plugin) {
        this.plugin = plugin;
        if (!plugin.getConfig().getBoolean("mysql.enabled")) return;
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
                    Query<MailUser> query = plugin.getDatabase().find(MailUser.class).where().eq("uuid", player.getUniqueId().toString()).query();
                    if (query != null) {
                        MailUser user = query.findUnique();
                        if (user != null) {
                            user.setUsername(player.getName().toLowerCase());
                            user.setDisplayname(player.getDisplayName());
                            user.update();
                        } else {
                            user = new MailUser(player.getUniqueId(), player.getName().toLowerCase(), player.getDisplayName());
                            user.save();
                        }
                    }
                } catch (Exception ex) {
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


}
