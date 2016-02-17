package nu.nerd.nerdmessage.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import nu.nerd.nerdmessage.NerdMessage;

public class OtherCommands implements CommandExecutor {

	private NerdMessage plugin;
	
	
	
	public OtherCommands(NerdMessage plugin) {
		this.plugin = plugin;
		plugin.getCommand("nerdmessagereload").setExecutor(this);
	}
	
	
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		switch(command.getName().toLowerCase()) {
		case "nerdmessagereload":
			// Reload the config and restart redis connection
			if (args.length != 0) {
				sender.sendMessage(ChatColor.RED + "Invalid arguments.");
				return true;
			}
			plugin.reload();
			sender.sendMessage(ChatColor.GREEN + "Reload Complete.");
			return true;
		default:
			return false;
		}
	}
}
