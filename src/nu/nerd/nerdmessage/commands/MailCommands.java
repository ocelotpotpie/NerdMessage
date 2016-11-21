package nu.nerd.nerdmessage.commands;

import nu.nerd.nerdmessage.mail.MailException;
import nu.nerd.nerdmessage.mail.MailMessage;
import nu.nerd.nerdmessage.mail.MailUser;
import nu.nerd.nerdmessage.NerdMessage;
import nu.nerd.nerdmessage.StringUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;


public class MailCommands implements CommandExecutor {


    private NerdMessage plugin;


    public MailCommands(NerdMessage plugin) {
        this.plugin = plugin;
        plugin.getCommand("mail").setExecutor(this);
    }


    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("mail")) {

            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console cannot use /mail.");
                return true;
            }

            if (!plugin.getConfig().getBoolean("mysql.enabled")) {
                sender.sendMessage(ChatColor.RED + "This feature is not enabled.");
                return true;
            }

            if (args.length == 0) {
                printHelp(sender);
                return true;
            }
            else if (args[0].equalsIgnoreCase("send")) {
                sendCommand(sender, args);
                return true;
            }
            else if (args[0].equalsIgnoreCase("inbox")) {
                inboxCommand(sender, args);
                return true;
            }

        }
        return false;
    }


    /**
     * Print the help message sent when /mail is invoked without a subcommand
     */
    private void printHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Sends and recieves messages, even for offline players.");
        String fmt = "- " + ChatColor.GREEN + "/mail %s" + ChatColor.WHITE + ": %s";
        sender.sendMessage(String.format(fmt, "send <player> <message>", "send <message> to <player>."));
        sender.sendMessage(String.format(fmt, "read <id>", "read messages by id."));
        sender.sendMessage(String.format(fmt, "inbox [<page>]", "read mail index."));
    }


    /**
     * Send a message to the CommandSender as a sync task.
     * @param sender CommandSender to notify
     * @param message Message to send
     */
    private void msgSync(final CommandSender sender, final String message) {
        new BukkitRunnable() {
            public void run() {
                sender.sendMessage(message);
            }
        }.runTask(plugin);
    }


    /**
     * /mail send command
     */
    private void sendCommand(final CommandSender sender, final String[] args) {

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /mail send <user> <message>");
            return;
        }

        new BukkitRunnable() {
            public void run() {
                try {
                    MailUser to = new MailUser(args[1]);
                    MailUser from = new MailUser(sender.getName());
                    String msg = StringUtil.join(args, 2);
                    if (to.getUuid() == null) {
                        msgSync(sender, String.format("%sThe player '%s' could not be found.", ChatColor.RED, args[1]));
                    } else {
                        MailMessage.send(from, to, msg);
                        plugin.getMailHandler().notifyNewMessages(to.getUuid(), true);
                        msgSync(sender, String.format("%sMessage sent: %s", ChatColor.GREEN, msg));
                    }
                } catch (MailException ex) {
                    msgSync(sender, ChatColor.RED + "Error: Message could not be sent.");
                }
            }
        }.runTaskAsynchronously(plugin);

    }


    /**
     * /mail inbox command
     */
    private void inboxCommand(final CommandSender sender, final String[] args) {

        final int perPage = 5;
        final Player player = (Player) sender;
        int num = 1;
        if (args.length == 2) {
            try {
                num = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                num = 1;
            }
        }
        final int page = num;

        new BukkitRunnable() {
            public void run() {

                List<MailMessage> messages = MailMessage.findUnread(player.getUniqueId());
                int pages = (messages.size() + perPage - 1) / perPage; //integer division
                int offset = (page - 1) * perPage;

                if (messages.size() == 0) {
                    msgSync(sender, ChatColor.RED + "You have no messages.");
                    return;
                } else if (pages < page) {
                    msgSync(sender, ChatColor.RED + "Invalid inbox page!");
                    return;
                }

                msgSync(sender, String.format("%sInbox for %s: [ Page %d of %d ]", ChatColor.YELLOW, player.getName(), page, pages));
                StringBuilder sb = new StringBuilder("");
                for (int i = offset; i < offset+perPage && i < messages.size(); i++) {
                    MailMessage msg = messages.get(i);
                    sb.append(String.format("%d)", i + 1));
                    sb.append(String.format("(%s%s%s)", ChatColor.YELLOW, msg.getSourceServer(), ChatColor.WHITE));
                    sb.append(String.format(" [%s%s%s] ", ChatColor.RED, msg.getFromName(), ChatColor.WHITE));
                    sb.append(StringUtil.truncateEllipsis(msg.getBody(), 30));
                    sb.append("\n");
                }
                msgSync(sender, sb.toString());
                msgSync(sender, String.format("%s/mail inbox <page>%s for more pages.", ChatColor.LIGHT_PURPLE, ChatColor.YELLOW));

            }
        }.runTaskAsynchronously(plugin);

    }


}
