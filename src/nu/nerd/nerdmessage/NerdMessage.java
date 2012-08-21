package nu.nerd.nerdmessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class NerdMessage extends JavaPlugin {

    List<NMUser> users = new ArrayList<NMUser>();
    
    @Override
    public void onEnable() {
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
        NMUser user = null;
        Player receiver = null;
        String message;
        if ("r".equalsIgnoreCase(name) || "reply".equalsIgnoreCase(name)) {
            message = Join(args, 0);
            user = getUser(sender.getName());
            if (user == null || user.getReplyTo() == null) {
                sender.sendMessage(ChatColor.RED + "No user to reply to.");
                return true;
            }
        }
        else {
            message = Join(args, 1);
        }
        
        if (user == null) {
            receiver = getServer().getPlayer(args[0]);
        }
        else {
            receiver = getServer().getPlayer(user.getReplyTo());
        }
        
        if (receiver == null) {
            sender.sendMessage(ChatColor.RED + "User is not online.");
            if (user != null) {
                user.setReplyTo(null);
            }
            return true;
        }
        
        NMUser r = getOrCreateUser(receiver.getName());
        
        r.setReplyTo(user.getName());
        user.setReplyTo(receiver.getName());
        
        receiver.sendMessage("[" + ChatColor.RED + sender.getName() + ChatColor.BLACK + " -> " + ChatColor.GOLD + receiver.getName() + ChatColor.BLACK + "] " + message);
        System.out.println("[" + sender.getName() + " -> " + receiver.getName() + "] " + message);
        return true;
    }
    
    public String Join(String[] args, int start) {
        String s = "";
        for (int i = start; i < args.length; i++) {
            if (s.length() == 0)
                s += " ";

            s += args[i];
        }
        return s;
    }
    
    public NMUser addUser(String username) {
        NMUser u = new NMUser(this, username);
        users.add(u);
        return u;
    }
    
    public NMUser getOrCreateUser(String username) {
        NMUser u = getUser(username);
        if (u == null) {
            u = addUser(username);
        }
        
        return u;
    }
    
    public NMUser getUser(String username) {
        username = ChatColor.stripColor(username);
        for (NMUser u : users) {
            if (username.equalsIgnoreCase(u.getName())) {
                return u;
            }
        }
        
        return null;
    }
    
    public void removeUser(String username) {
        NMUser u = getUser(username);
        if (u != null) {
            users.remove(u);
        }
    }
}