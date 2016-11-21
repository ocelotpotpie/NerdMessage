package nu.nerd.nerdmessage;

import org.bukkit.scheduler.BukkitRunnable;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;

public class RedisListener extends JedisPubSub {


    private NerdMessage plugin;


    public RedisListener(NerdMessage plugin) {
        this.plugin = plugin;
    }


    @Override
    public void onPMessage(final String pattern, final String channel, final String message) {
        if (channel.equalsIgnoreCase("nerdmessage.mbg")) {
            plugin.getServer().broadcast(message, "nerdmessage.mb");
        }
        else if (channel.equalsIgnoreCase("nerdmessage.abg")) {
            plugin.getServer().broadcast(message, "nerdmessage.ab");
        }
        else if (channel.equalsIgnoreCase("nerdmessage.globalbroadcast")) {
            plugin.getServer().broadcastMessage(message);
        }
        else if (channel.equalsIgnoreCase("nerdmessage.mail.new")) {
            new BukkitRunnable() {
                public void run() {
                    plugin.getMailHandler().notifyNewMessages(UUID.fromString(message), false);
                }
            }.runTaskAsynchronously(plugin);
        }
    }


}
