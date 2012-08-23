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
        if (command.getName().equalsIgnoreCase("msg") || command.getName().equalsIgnoreCase("cmsg")) {
            NMUser user = null;
            //Player receiver = null;
            CommandSender receiver = null;
            String message;
            if ("r".equalsIgnoreCase(name) || "reply".equalsIgnoreCase(name)) {
                message = Join(args, 0);
                user = getUser(sender.getName());
                if (user == null || user.getReplyTo() == null) {
                    sender.sendMessage(ChatColor.RED + "No user to reply to.");
                    return true;
                }
            } else {
                message = Join(args, 1);
            }

            if (user == null) {
                if (args[0].equalsIgnoreCase("console")) {
                    receiver = getServer().getConsoleSender();
                }
                else {
                    receiver = getPlayer(args[0]);
                }
                user = getOrCreateUser(sender.getName());
            } else {
                if (user.getReplyTo().equalsIgnoreCase("console")) {
                    receiver = getServer().getConsoleSender();
                }
                else {
                    receiver = getPlayer(user.getReplyTo());
                }
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

            if (name.equalsIgnoreCase("cmsg")) {
                sender.sendMessage("[" + ChatColor.RED + "Me" + ChatColor.WHITE + " -> " + ChatColor.GOLD + receiver.getName() + ChatColor.WHITE + "] " + message);
                receiver.sendMessage("[" + ChatColor.RED + sender.getName() + ChatColor.WHITE + " -> " + ChatColor.GOLD + "Me" + ChatColor.WHITE + "] " + ChatColor.GREEN + message);
            }
            else {
                System.out.println(user.getName() + ":/msg " + receiver.getName() + " " + message);
                sender.sendMessage("[" + ChatColor.RED + "Me" + ChatColor.WHITE + " -> " + ChatColor.GOLD + receiver.getName() + ChatColor.WHITE + "] " + message);
                receiver.sendMessage("[" + ChatColor.RED + sender.getName() + ChatColor.WHITE + " -> " + ChatColor.GOLD + "Me" + ChatColor.WHITE + "] " + message);
            }
            
            if (receiver != getServer().getConsoleSender()) {
                System.out.println("[" + sender.getName() + " -> " + receiver.getName() + "] " + message);
            }
            return true;
        }
        else if (command.getName().equalsIgnoreCase("me")) {
            if (sender instanceof Player) {
                getServer().broadcastMessage("* " + ChatColor.stripColor(sender.getName()) + " " + Join(args, 0));
            }
        }
        
        return false;
    }
    
    public Player getPlayer(final String name) {
        Player[] players = getServer().getOnlinePlayers();

        Player found = null;
        String lowerName = name.toLowerCase();
        int delta = Integer.MAX_VALUE;
        for (Player player : players) {
            if (ChatColor.stripColor(player.getName()).toLowerCase().startsWith(lowerName)) {
                int curDelta = player.getName().length() - lowerName.length();
                if (curDelta < delta) {
                    found = player;
                    delta = curDelta;
                }
                if (curDelta == 0) break;
            }
        }
        return found;
    }

    public String Join(String[] args, int start) {
        String s = "";
        for (int i = start; i < args.length; i++) {
            if (s.length() > 0) {
                s += " ";
            }

            s += args[i];
        }
        return s;
    }

    public NMUser addUser(String username) {
        username = ChatColor.stripColor(username);
        NMUser u = new NMUser(this, username);
        users.add(u);
        return u;
    }

    public NMUser getOrCreateUser(String username) {
        username = ChatColor.stripColor(username);
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