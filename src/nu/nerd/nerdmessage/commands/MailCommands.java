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

import java.text.SimpleDateFormat;
import java.util.Date;
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
            else if (args[0].equalsIgnoreCase("read")) {
                readCommand(sender, args);
            }
            else if (args[0].equalsIgnoreCase("inbox")) {
                inboxCommand(sender, args);
                return true;
            }
            else if (args[0].equalsIgnoreCase("clear")) {
                clearCommand(sender);
            }
            else if (args[0].equalsIgnoreCase("archive")) {
                archiveCommand(sender, args);
            }
            else if (args[0].equalsIgnoreCase("thread")) {
                threadCommand(sender, args);
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
        sender.sendMessage(String.format(fmt, "inbox [page]", "read mail index."));
        sender.sendMessage(String.format(fmt, "clear", "clear all messages."));
        sender.sendMessage(String.format(fmt, "archive <id>", "remove message <id> from inbox."));
        sender.sendMessage(String.format(fmt, "thread <user> [page]", "thread between you and <user>."));
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
     * /mail read command
     */
    private  void readCommand(final CommandSender sender, final String[] args) {

        Integer id;

        if (args[1].equals("")) {
            sender.sendMessage(ChatColor.RED + "Usage /mail read <id>");
            return;
        }
        try {
            id = Integer.parseInt(args[1]);
        } catch (ArithmeticException ex) {
            sender.sendMessage(ChatColor.RED + "ID must be an integer.");
            return;
        }
        if (id <= 0) {
            sender.sendMessage(ChatColor.RED + "ID must be greater than zero.");
            return;
        }

        final int index = id;
        final Player player = (Player) sender;

        new BukkitRunnable() {
            public void run() {

                List<MailMessage> messages = MailMessage.findUnread(player.getUniqueId());

                if (messages.size() == 0) {
                    msgSync(sender, ChatColor.RED + "You have no messages.");
                    return;
                }
                if (messages.size() < index) {
                    msgSync(sender, ChatColor.RED + "Invalid id!");
                    return;
                }

                MailMessage message = messages.get(index - 1);
                SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyy HH:mm:ss z");
                Date date = new Date(message.getDateSent());
                String when = sdf.format(date);
                StringBuilder sb = new StringBuilder("");
                sb.append(String.format("%sFrom: %s%s\n", ChatColor.YELLOW, ChatColor.RED, message.getFromName()));
                sb.append(String.format("%sSent: %s(%s%s%s) %s\n", ChatColor.YELLOW, ChatColor.WHITE, ChatColor.YELLOW, message.getSourceServer(), ChatColor.WHITE, when));
                sb.append(message.getBody());
                sb.append("\n");
                msgSync(sender, sb.toString());

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
                    sb.append(String.format("%d) ", i + 1));
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


    /**
     * /mail clear command
     */
    private void clearCommand(final CommandSender sender) {
        final Player player = (Player) sender;
        new BukkitRunnable() {
            public void run() {
                MailMessage.flagAllRead(player.getUniqueId());
                msgSync(sender, ChatColor.LIGHT_PURPLE + "All messages have been cleared.");
            }
        }.runTaskAsynchronously(plugin);
    }


    /**
     * /mail archive command
     */
    private void archiveCommand(final CommandSender sender, final String[] args) {

        Integer id;

        if (args[1].equals("")) {
            sender.sendMessage(ChatColor.RED + "Usage: /mail archive <id>");
            return;
        }
        try {
            id = Integer.parseInt(args[1]);
        } catch (ArithmeticException ex) {
            sender.sendMessage(ChatColor.RED + "ID must be an integer.");
            return;
        }
        if (id <= 0) {
            sender.sendMessage(ChatColor.RED + "ID must be greater than zero.");
            return;
        }

        final int index = id;
        final Player player = (Player) sender;

        new BukkitRunnable() {
            public void run() {
                List<MailMessage> messages = MailMessage.findUnread(player.getUniqueId());
                if (messages.size() == 0) {
                    msgSync(sender, ChatColor.RED + "You have no messages to delete");
                }
                else if (messages.size() < index) {
                    msgSync(sender, String.format("%sID must be less than %d.", ChatColor.RED, messages.size()+1));
                }
                else {
                    MailMessage.flagRead(player.getUniqueId(), index);
                    msgSync(sender, ChatColor.RED + "Message archived");
                }
            }
        }.runTaskAsynchronously(plugin);

    }


    /**
     * /mail thread command
     */
    private void threadCommand(final CommandSender sender, final String[] args) {

        if (args.length < 2 || args[1].equals("")) {
            sender.sendMessage(ChatColor.RED + "Usage: /mail thread <user> [page]");
            return;
        }
        int num = 1;
        if (args.length == 3) {
            try {
                num = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                num = 1;
            }
        }

        final String username = args[1];
        final Player player = (Player) sender;
        final int perPage = 4;
        final int page = num;

        new BukkitRunnable() {
            public void run() {
                try {

                    MailUser otherUser = new MailUser(username);
                    List<MailMessage> messages = MailMessage.findThread(player.getUniqueId(), otherUser.getUuid());
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");

                    int pages = (messages.size() + perPage - 1) / perPage; //integer division
                    int offset = (page - 1) * perPage;
                    if (messages.size() == 0) {
                        msgSync(sender, ChatColor.RED + "You have no messages to or from that user.");
                        return;
                    } else if (pages < page) {
                        msgSync(sender, ChatColor.RED + "Invalid thread page!");
                        return;
                    }

                    StringBuilder sb = new StringBuilder("");
                    sb.append(String.format("%sThread for %s and %s: [ Page %d of %d ]\n", ChatColor.YELLOW, player.getName(), otherUser.getDisplayname(), page, pages));
                    sb.append(String.format("%s---\n", ChatColor.GRAY));
                    for (int i = offset; i < offset+perPage && i < messages.size(); i++) {
                        MailMessage msg = messages.get(i);
                        Date date = new Date(msg.getDateSent());
                        String when = sdf.format(date);
                        String fromtag = String.format("%s -> %s%s", msg.getFromName(), msg.getToName(), ChatColor.YELLOW);
                        if (msg.getFrom().equals(player.getUniqueId())) {
                            fromtag = ChatColor.GREEN + fromtag;
                        } else {
                            fromtag = ChatColor.RED + fromtag;
                        }
                        sb.append(String.format("%s[%s %s]%s\n", ChatColor.YELLOW, fromtag, when, ChatColor.RESET));
                        sb.append(msg.getBody());
                        sb.append(String.format("\n%s---\n", ChatColor.GRAY));
                    }
                    sb.append(String.format("%s/mail thread %s <page>%s for more pages.\n", ChatColor.LIGHT_PURPLE, otherUser.getDisplayname(), ChatColor.YELLOW));
                    msgSync(sender, sb.toString());

                } catch (MailException ex) {
                    msgSync(sender, ChatColor.RED + "Error: Messages could not be retrieved.");
                }
            }
        }.runTaskAsynchronously(plugin);

    }


}
