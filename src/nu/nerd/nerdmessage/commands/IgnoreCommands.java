package nu.nerd.nerdmessage.commands;


import nu.nerd.nerdmessage.NerdMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class IgnoreCommands implements CommandExecutor {


    private NerdMessage plugin;


    public IgnoreCommands(NerdMessage plugin) {
        this.plugin = plugin;
        plugin.getCommand("ignore").setExecutor(this);
        plugin.getCommand("unignore").setExecutor(this);
    }


    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 1) return false;
        if (cmd.getName().equalsIgnoreCase("ignore")) {
            ignore(sender, args[0]);
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("unignore")) {
            unignore(sender, args[0]);
            return true;
        }
        else {
            return false;
        }
    }


    /**
     * Ignore a specified player until the next restart
     */
    public void ignore(CommandSender sender, String ignoreName) {

        Player ignorePlayer = plugin.getPlayer(ignoreName);
        if (ignorePlayer != null) {
            ignoreName = ignorePlayer.getName();
        }

        if (ignoreName.equalsIgnoreCase(sender.getName())) {
            sender.sendMessage(ChatColor.RED + "You can't ignore yourself!");
            return;
        }

        if (plugin.getOrCreateUser(sender.getName()).addIgnoredPlayer(ignoreName.toLowerCase())) {
            sender.sendMessage(ChatColor.GOLD + "Ignoring " + ignoreName + " until the next restart.");
            this.handleStaffAlert(ignoreName.toLowerCase());
        } else {
            sender.sendMessage(ChatColor.RED + "You are already ignoring that player.");
        }

    }


    /**
     * Unignore a player
     */
    public void unignore(CommandSender sender, String unignoreName) {
        if (plugin.getOrCreateUser(sender.getName()).removeIgnoredPlayer(unignoreName)) {
            sender.sendMessage(ChatColor.GOLD + "Removed " + unignoreName + " from your ignore list.");
        } else {
            sender.sendMessage(ChatColor.RED + "You aren't ignoring " + unignoreName + ".");
        }
    }


    /**
     * Send an alert to online staff when a player is muted several times
     */
    private void handleStaffAlert(String name) {
        Integer count = 1;
        if (plugin.getMuteCounts().containsKey(name)) {
            count = plugin.getMuteCounts().get(name);
            count = count + 1;
            if (plugin.getAlertThreshold() > 0) {
                if (count % plugin.getAlertThreshold() == 0) {
                    String msg = String.format("Player \"%s\" has been ignored by %d people.", name, count);
                    Bukkit.broadcast(ChatColor.GRAY + msg, "nerdmessage.ignore.alert");
                }
            }
        }
        plugin.getMuteCounts().put(name, count);
    }


}
