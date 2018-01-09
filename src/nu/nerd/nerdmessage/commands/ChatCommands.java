package nu.nerd.nerdmessage.commands;

import nu.nerd.nerdmessage.NMUser;
import nu.nerd.nerdmessage.NerdMessage;
import nu.nerd.nerdmessage.StringUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class ChatCommands implements CommandExecutor {


    private NerdMessage plugin;


    public ChatCommands(NerdMessage plugin) {
        this.plugin = plugin;
        plugin.getCommand("msg").setExecutor(this);
        plugin.getCommand("cmsg").setExecutor(this);
        plugin.getCommand("me").setExecutor(this);
        plugin.getCommand("s").setExecutor(this);
    }


    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) return false;
        if (cmd.getName().equalsIgnoreCase("msg")) {
            if (label.equalsIgnoreCase("r") || label.equalsIgnoreCase("reply")) {
                reply(sender, StringUtil.join(args), false, false, false);
            } else if (label.equalsIgnoreCase("m") || label.equalsIgnoreCase("msg") || label.equalsIgnoreCase("t") || label.equalsIgnoreCase("tell")) {
                msg(sender, args[0], StringUtil.join(args, 1), false, false, false);
            } else if (label.equalsIgnoreCase("rs")) {
                reply(sender, StringUtil.join(args), false, true, false);
            } else if (label.equalsIgnoreCase("rme")) {
                reply(sender, StringUtil.join(args), false, false, true);
            } else if (label.equalsIgnoreCase("rsme")) {
                reply(sender, StringUtil.join(args), false, true, true);
            } else {
                return false;
            }
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("cmsg")) {
            msg(sender, args[0], StringUtil.join(args, 1), true, false, false);
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("me")) {
                me(sender, StringUtil.join(args));
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("s")) {
                sarcasm(sender, StringUtil.join(args));
            return true;
        }
        else {
            return false;
        }
    }


    /**
     * Send a private message to a player
     * @param sender The CommandSender object from onCommand()
     * @param recipientName The username string of the recipient
     * @param message The message to send
     * @param green Whether this is a staff green message from /cmsg
     * @param sarcastic Whether this message will be italic
     * @param action Whether this message will be an action
     */
    public void msg(CommandSender sender, String recipientName, String message, boolean green, boolean sarcastic, boolean action) {

        NMUser user = plugin.getOrCreateUser(sender.getName());
        CommandSender recipient = null;

        // Find the recipient of this message
        if (recipientName.equalsIgnoreCase("console")) {
            recipient = plugin.getServer().getConsoleSender();
        }
        else {
            recipient = plugin.getPlayer(recipientName);
        }

        // If a recipient couldn't be found, notify the sender and nullify the reply-to
        if (recipient == null) {
            sender.sendMessage(ChatColor.RED + "User is not online.");
            if (user != null) {
                user.setReplyTo(null);
            }
            return;
        }

        // Set reply-to for both players
        NMUser r = plugin.getOrCreateUser(recipient.getName());
        r.setReplyTo(user.getName());
        user.setReplyTo(recipient.getName());

        // Whether to actually send the message to the player or not (for /ignore).
        boolean doSendMessage = sender.hasPermission("nerdmessage.ignore.bypass-msg") || !(r.isIgnoringPlayer(sender.getName()));

        // Send the message
        if (sarcastic) {
            sender.sendMessage(tag("Me", recipient.getName(), action) + ChatColor.ITALIC + message);
        } else {
            sender.sendMessage(tag("Me", recipient.getName(), action) + message);
        }
        if (doSendMessage) {
            if (green) {
                message = ChatColor.GREEN + message;
            }
	    if (sarcastic) {
                message = ChatColor.ITALIC + message;
            }
	    recipient.sendMessage(tag(sender.getName(), "Me", action) + message);
        }

        // Logs
        System.out.println(user.getName() + ":/msg " + recipient.getName() + " " + message);
        if (recipient != plugin.getServer().getConsoleSender()) {
            System.out.println((doSendMessage ? "" : "Blocked by /ignore: ") + "[" + sender.getName() + " -> " + recipient.getName() + "] " + message);
        }

    }


    /**
     * Reply to the user the sender is currently conversing with, from the /r command.
     * The "reply-to" recipient is either the last person you messaged or the last person to message you,
     * depending on which is the newest event.
     */
    public void reply(CommandSender sender, String message, boolean green, boolean sarcastic, boolean action) {
        NMUser user = plugin.getUser(sender.getName());
        if (user == null || user.getReplyTo() == null) {
            sender.sendMessage(ChatColor.RED + "No user to reply to.");
            return;
        }
        msg(sender, user.getReplyTo(), message, green, sarcastic, action);
    }


    /**
     * Handle IRC-style /me messages
     */
    public void me(CommandSender sender, String message) {
        if (StringUtil.isAllCaps(message)) {
            sender.sendMessage(ChatColor.RED + "Please don't chat in all caps.");
            return;
        }
        message = "* " + ChatColor.stripColor(sender.getName()) + " " + message;
        sendPublicMessage(sender, message);
        plugin.getServer().getLogger().info(message);
    }


    /**
     * Handle italicized messages from the /s command
     */
    public void sarcasm(CommandSender sender, String message) {
        if (StringUtil.isAllCaps(message)) {
            sender.sendMessage(ChatColor.RED + "All-caps? How original...");
            return;
        }
        message = "<" + sender.getName() + "> " + ChatColor.ITALIC + message;
        sendPublicMessage(sender, message);
        plugin.getServer().getLogger().info(message);
    }


    /**
     * Format a [sender -> recipient] tag for a private message
     * @param leftUser The username on the left of the arrow (e.g. "Me" or "redwall_hp")
     * @param rightUser The username on the right of the arrow
     * @param action Wheter the message should be formated as an action
     */
    private String tag(String leftUser, String rightUser, boolean action) {
        if (action) {
            return String.format("[*%s%s%s -> %s%s%s] ", ChatColor.RED, leftUser, ChatColor.WHITE, ChatColor.GOLD, rightUser, ChatColor.WHITE);

        } else {
            return String.format("[%s%s%s -> %s%s%s] ", ChatColor.RED, leftUser, ChatColor.WHITE, ChatColor.GOLD, rightUser, ChatColor.WHITE);
        }
    }


    /**
     * Sends an arbitrary message string to every player online
     */
    private void sendPublicMessage(CommandSender sender, String message) {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            NMUser recipient = plugin.getUser(ChatColor.stripColor(p.getName()));
            if (recipient == null || !recipient.isIgnoringPlayer(sender.getName())) {
                p.sendMessage(message);
            }
        }
    }


}
