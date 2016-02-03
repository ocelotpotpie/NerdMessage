package nu.nerd.nerdmessage.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import nu.nerd.nerdmessage.NerdMessage;
import nu.nerd.nerdmessage.StringUtil;

public class MOTDCommands implements CommandExecutor {

	
    private NerdMessage plugin;
    
    
    public MOTDCommands(NerdMessage plugin) {
    	this.plugin = plugin;
    	plugin.getCommand("motd").setExecutor(this);
    	plugin.getCommand("setmotd").setExecutor(this);
    	plugin.getCommand("mbmotd").setExecutor(this);
    	plugin.getCommand("setmbmotd").setExecutor(this);
    	plugin.getCommand("abmotd").setExecutor(this);
    	plugin.getCommand("setabmotd").setExecutor(this);
    }

    
    /**
     * Executes the given command, returning its success
     *
     * @param sender Source of the command
     * @param command Command which was executed
     * @param label Alias of the command which was used
     * @param args Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
    	switch(command.getName().toLowerCase()) {
    	case "motd":
    		printMOTD(sender);
    		return true;
    	case "setmotd":
    		setMOTD(sender, StringUtil.join(args));
    		return true;
    	case "mbmotd":
    		printMBMOTD(sender);
    		return true;
    	case "setmbmotd":
    		setMBMOTD(sender, StringUtil.join(args));
    		return true;
    	case "abmotd":
    		printABMOTD(sender);
    		return true;
    	case "setabmotd":
    		setABMOTD(sender, StringUtil.join(args));
    		return true;
    	default:
    		return false;
    	}
    }
    
    
    /**
     * Print the MOTD to the sender
     */
    public void printMOTD(CommandSender sender) {
		String motd = plugin.getConfig().getString("MOTD");
		if (motd == null || motd.equals("")) {
			sender.sendMessage(ChatColor.GREEN + "No Message Of The Day is set");
		} else {
			sender.sendMessage(ChatColor.AQUA + "[MOTD]: " + motd);
		}
    }
    
    
    /**
     * Set the MOTD
     */
    public void setMOTD(CommandSender sender, String motd) {
    	plugin.getConfig().set("MOTD", motd);
    	plugin.saveConfig();
    	sender.sendMessage(ChatColor.GREEN + "Message Of The Day set");
    }
    
    
    /**
     * Print the MB MOTD to the sender
     */
    public void printMBMOTD(CommandSender sender) {
		String mbmotd = plugin.getConfig().getString("MBMOTD");
		if (mbmotd == null || mbmotd.equals("")) {
			sender.sendMessage(ChatColor.GREEN + "No Moderator Message Of The Day is set");
		} else {
			sender.sendMessage(ChatColor.GREEN + "[MB MOTD]: " + mbmotd);
		}
    }
    
    
    /**
     * Set the MB MOTD
     */
    public void setMBMOTD(CommandSender sender, String mbmotd) {
    	plugin.getConfig().set("MBMOTD", mbmotd);
    	plugin.saveConfig();
    	sender.sendMessage(ChatColor.GREEN + "Moderator Message Of The Day set");
    }
    
    
    /**
     * Print the AB MOTD to the sender
     */
    public void printABMOTD(CommandSender sender) {
		String abmotd = plugin.getConfig().getString("ABMOTD");
		if (abmotd == null || abmotd.equals("")) {
			sender.sendMessage(ChatColor.GREEN + "No Administrator Message Of The Day is set");
		} else {
			sender.sendMessage(ChatColor.GOLD + "[AB MOTD]: " + abmotd);
		}
    }
    
    
    /**
     * Set the AB MOTD
     */
    public void setABMOTD(CommandSender sender, String abmotd) {
    	plugin.getConfig().set("ABMOTD", abmotd);
    	plugin.saveConfig();
    	sender.sendMessage(ChatColor.GREEN + "Administrator Message Of The Day set");
    }
}

