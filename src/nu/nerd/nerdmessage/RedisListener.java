package nu.nerd.nerdmessage;

import redis.clients.jedis.JedisPubSub;

public class RedisListener extends JedisPubSub {


    private NerdMessage plugin;


    public RedisListener(NerdMessage plugin) {
        this.plugin = plugin;
    }


    @Override
    public void onPMessage(String pattern, String channel, String message) {
        if (channel.equalsIgnoreCase("nerdmessage.mbg")) {
            plugin.getServer().broadcast(message, "nerdmessage.mb");
        } else if (channel.equalsIgnoreCase("nerdmessage.abg")) {
            plugin.getServer().broadcast(message, "nerdmessage.ab");
        } else if (channel.equalsIgnoreCase("nerdmessage.globalbroadcast")) {
            plugin.getServer().broadcastMessage(message);
        }
    }


}
