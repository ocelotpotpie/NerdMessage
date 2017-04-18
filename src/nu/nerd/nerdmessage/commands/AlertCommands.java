package nu.nerd.nerdmessage.commands;

import com.google.common.collect.Lists;
import nu.nerd.nerdmessage.NerdMessage;
import nu.nerd.nerdmessage.StringUtil;
import nu.nerd.nerdmessage.alerts.AlertMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AlertCommands implements CommandExecutor {


    private NerdMessage plugin;


    public AlertCommands(NerdMessage plugin) {
        this.plugin = plugin;
        plugin.getCommand("alert").setExecutor(this);
    }


    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("alert")) return false;
        // Help text
        if (args.length == 0) {
            printHelp(sender);
            return true;
        }
        // List command
        if (args[0].equalsIgnoreCase("list")) {
            listCommand(sender);
            return true;
        }
        // Administrative commands
        if (sender.hasPermission("nerdmessage.alert.admin")) {
            if (args[0].equalsIgnoreCase("add")) {
                addCommand(sender, args);
                return true;
            }
            else if (args[0].equalsIgnoreCase("insert")) {
                insertCommand(sender, args);
                return true;
            }
            else if (args[0].equalsIgnoreCase("remove")) {
                removeCommand(sender, args);
                return true;
            }
            else if (args[0].equalsIgnoreCase("interval")) {
                intervalCommand(sender, args);
                return true;
            }
        }
        return false;
    }


    private void printHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Usage:");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "    /alert list");
        sender.sendMessage("        List all broadcast messages.");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "    /alert add [color] <message>");
        sender.sendMessage("        Add the message to the broadcast rotation.");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "    /alert insert <index> [color] <message>");
        sender.sendMessage("        Insert the message into the broadcast rotation.");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "    /alert remove <number>");
        sender.sendMessage("        Remove a message by number.");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "    /alert interval <seconds>");
        sender.sendMessage("        Get or set the interval between broadcasts in seconds.");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "    /alert reload");
        sender.sendMessage("        Reload the alerts from the YAML file.");
    }


    private void listCommand(CommandSender sender) {
        List<AlertMessage> alerts = plugin.getAlertHandler().getAlerts();
        sender.sendMessage(String.format("%sThere are %d alerts.", ChatColor.LIGHT_PURPLE, alerts.size()));
        for (AlertMessage alert : alerts) {
            sender.sendMessage(String.format("%s(%d) %s", alert.getColor(), alerts.indexOf(alert)+1, alert.getText()));
        }
    }


    /**
     * Add an alert
     */
    private void addCommand(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /alert add [color] <message>");
            return;
        }

        String possibleColor = args[1].toLowerCase().replace("_", "");
        AlertMessage alert;
        if (getColorMap().containsKey(possibleColor)) {
            alert = new AlertMessage(StringUtil.join(args, 2), getColorMap().get(possibleColor));
        } else {
            alert = new AlertMessage(StringUtil.join(args, 1));
        }

        plugin.getAlertHandler().addAlert(alert, plugin.getAlertHandler().getAlerts().size());
        sender.sendMessage(String.format("%sAlert #%d added.", ChatColor.LIGHT_PURPLE, plugin.getAlertHandler().getAlerts().size()));

    }


    /**
     * Insert an alert at a given index
     */
    private void insertCommand(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /alert insert <index> [color] <message>");
            return;
        }

        int max = plugin.getAlertHandler().getAlerts().size() + 1;
        Integer index;
        try {
            index = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "A numerical index must be specified.");
            return;
        }
        if (index < 1 || index > max) {
            sender.sendMessage(String.format("%sThe index must be a number between 1 and %d inclusive.", ChatColor.RED, max));
            return;
        }

        String possibleColor = args[2].toLowerCase().replace("_", "");
        AlertMessage alert;
        if (getColorMap().containsKey(possibleColor)) {
            alert = new AlertMessage(StringUtil.join(args, 3), getColorMap().get(possibleColor));
        } else {
            alert = new AlertMessage(StringUtil.join(args, 2));
        }

        plugin.getAlertHandler().addAlert(alert, index - 1);
        sender.sendMessage(String.format("%sAlert #%d added.", ChatColor.LIGHT_PURPLE, index));

    }


    /**
     * Remove alert at index
     * @param sender
     */
    private void removeCommand(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /alert remove <number>");
            return;
        }

        Integer index;
        int numAlerts = plugin.getAlertHandler().getAlerts().size();
        try {
            index = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "A numerical index must be specified.");
            return;
        }
        if (numAlerts < 1) {
            sender.sendMessage(ChatColor.RED + "There are no alerts to remove.");
            return;
        }
        if (index < 1 || index > numAlerts + 1) {
            sender.sendMessage(String.format("%sThe index must be a number between 1 and %d inclusive.", ChatColor.RED, numAlerts + 1));
            return;
        }

        AlertMessage alert = plugin.getAlertHandler().removeAlert(index - 1);
        if (alert != null) {
            sender.sendMessage(String.format("%sRemoved alert: %s%s", ChatColor.LIGHT_PURPLE, alert.getColor(), alert.getText()));
        }

    }

    /**
     * Admin command to set the interval between broadcast messages, in seconds.
     */
    private void intervalCommand(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /alert interval <seconds>");
            return;
        }

        int minInterval = 30;
        Integer seconds;
        try {
            seconds = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "You must specify the interval in seconds as a number.");
            return;
        }
        if (seconds < minInterval) {
            sender.sendMessage(String.format("%sThe interval must be at least %d seconds.", ChatColor.RED, minInterval));
            return;
        }

        plugin.getAlertHandler().changeInterval(seconds);
        sender.sendMessage(String.format("%sThe alert broadcast interval was set to %d seconds.", ChatColor.LIGHT_PURPLE, seconds));

    }


    /**
     * Map of user-friendly color names to use for alerts' primary color
     */
    private Map<String, ChatColor> getColorMap() {
        List<String> excluded = Lists.newArrayList("BOLD", "ITALIC", "PLAIN_WHITE", "RANDOM", "STRIKETHROUGH", "UNDERLINE");
        Map<String, ChatColor> colors = new HashMap<String, ChatColor>();
        colors.put("darkgrey", ChatColor.DARK_GRAY);
        colors.put("orange", ChatColor.GOLD);
        colors.put("grey", ChatColor.GRAY);
        colors.put("pink", ChatColor.LIGHT_PURPLE);
        colors.put("purple", ChatColor.LIGHT_PURPLE);
        String key;
        for (ChatColor c : ChatColor.values()) {
            if (!excluded.contains(c.toString())) {
                key = c.name().toLowerCase().replace("_", "");
                key = key.replace("gray", "grey");
                colors.put(key, c);
            }
        }
        return colors;
    }


}
