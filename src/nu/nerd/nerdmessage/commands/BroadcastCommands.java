package nu.nerd.nerdmessage.commands;

import nu.nerd.nerdmessage.NerdMessage;
import nu.nerd.nerdmessage.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import redis.clients.jedis.exceptions.JedisException;

public class BroadcastCommands implements CommandExecutor {


    private NerdMessage plugin;
    private final String redisError = ChatColor.RED + "Error: could not deliver message, as the Redis server could not be reached.";


    public BroadcastCommands(NerdMessage plugin) {
        this.plugin = plugin;
        plugin.getCommand("mb").setExecutor(this);
        plugin.getCommand("ab").setExecutor(this);
        plugin.getCommand("broadcast").setExecutor(this);
        plugin.getCommand("o").setExecutor(this);
        plugin.getCommand("mbg").setExecutor(this);
        plugin.getCommand("abg").setExecutor(this);
        plugin.getCommand("global-broadcast").setExecutor(this);
    }


    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) return false;
        if (cmd.getName().equalsIgnoreCase("mb")) {
            mb(sender, StringUtil.join(args));
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("ab")) {
            ab(sender, StringUtil.join(args));
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("broadcast")) {
            broadcast(sender, StringUtil.join(args));
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("o")) {
            o(sender, StringUtil.join(args));
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("mbg")) {
            mbg(sender, StringUtil.join(args));
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("abg")) {
            abg(sender, StringUtil.join(args));
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("global-broadcast")) {
            globalBroadcast(sender, StringUtil.join(args));
            return true;
        }
        else {
            return false;
        }
    }


    public void mb(CommandSender sender, String message) {
        message = tag("Mod - " + sender.getName()) + ChatColor.GREEN + message;
        Bukkit.broadcast(message, "nerdmessage.mb");
    }


    public void ab(CommandSender sender, String message) {
        message = tag("Admin - " + sender.getName()) + ChatColor.GOLD + message;
        Bukkit.broadcast(message, "nerdmessage.ab");
    }


    public void broadcast(CommandSender sender, String message) {
        message = tag("Broadcast") + ChatColor.GREEN + message;
        Bukkit.broadcastMessage(message);
    }


    public void o(CommandSender sender, String message) {
        message = String.format("<%s%s%s>%s %s", ChatColor.RED, sender.getName(), ChatColor.WHITE, ChatColor.GREEN, message);
        Bukkit.broadcastMessage(message);
    }


    public void mbg(CommandSender sender, String message) {
        message = globalTag("MBG", sender.getName()) + ChatColor.GREEN + message;
        try {
            plugin.redisPublish("mbg", message);
        } catch (JedisException ex) {
            sender.sendMessage(redisError);
        }
    }


    public void abg(CommandSender sender, String message) {
        message = globalTag("ABG", sender.getName()) + ChatColor.GOLD + message;
        try {
            plugin.redisPublish("abg", message);
        } catch (JedisException ex) {
            sender.sendMessage(redisError);
        }
    }


    public void globalBroadcast(CommandSender sender, String message) {
        String tag = String.format("[%sGlobal %sBroadcast%s] ", ChatColor.DARK_PURPLE, ChatColor.RED, ChatColor.WHITE);
        message = tag + ChatColor.GREEN + message;
        try {
            plugin.redisPublish("globalbroadcast", message);
        } catch (JedisException ex) {
            sender.sendMessage(redisError);
        }
    }


    private String tag(String str) {
        return String.format("[%s%s%s] ", ChatColor.RED, str, ChatColor.WHITE);
    }


    private String globalTag(String prefix, String name) {
        if (plugin.getServerName() != null) {
            return String.format("[%s%s(%s)%s - %s%s] ", ChatColor.DARK_PURPLE, prefix, plugin.getServerName(), ChatColor.RED, name, ChatColor.WHITE);
        } else {
            return String.format("[%s%s%s - %s%s] ", ChatColor.DARK_PURPLE, prefix, ChatColor.RED, name, ChatColor.WHITE);
        }
    }


}
