package nu.nerd.nerdmessage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class NerdMessage extends JavaPlugin {

    List<NMUser> users = new CopyOnWriteArrayList<NMUser>();
    HashMap<String, Integer> muteCounts = new HashMap<String, Integer>();

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(new NerdMessageListener(this), this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
        if (args.length == 0)
            return false;
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

            // Whether to actually send the message to the player or not (for /ignore).
            boolean doSendMessage = sender.hasPermission("nerdmessage.ignore.bypass-msg") || !(r.isIgnoringPlayer(sender.getName()));

            if (name.equalsIgnoreCase("cmsg")) {
                sender.sendMessage("[" + ChatColor.RED + "Me" + ChatColor.WHITE + " -> " + ChatColor.GOLD + receiver.getName() + ChatColor.WHITE + "] " + message);
                if (doSendMessage) {
                    receiver.sendMessage("[" + ChatColor.RED + sender.getName() + ChatColor.WHITE + " -> " + ChatColor.GOLD + "Me" + ChatColor.WHITE + "] " + ChatColor.GREEN + message);
                }
            }
            else {
                System.out.println(user.getName() + ":/msg " + receiver.getName() + " " + message);
                sender.sendMessage("[" + ChatColor.RED + "Me" + ChatColor.WHITE + " -> " + ChatColor.GOLD + receiver.getName() + ChatColor.WHITE + "] " + message);
                if (doSendMessage) {
                    receiver.sendMessage("[" + ChatColor.RED + sender.getName() + ChatColor.WHITE + " -> " + ChatColor.GOLD + "Me" + ChatColor.WHITE + "] " + message);
                }
            }
            
            if (receiver != getServer().getConsoleSender()) {
                System.out.println((doSendMessage ? "" : "Blocked by /ignore: ")
                        + "[" + sender.getName() + " -> " + receiver.getName() + "] " + message);
            }
            return true;
        }
        else if (command.getName().equalsIgnoreCase("me")) {
            String message = "* " + ChatColor.stripColor(sender.getName()) + " " + Join(args, 0);
            if (sender instanceof Player) {
                for (Player p : getServer().getOnlinePlayers()) {
                    NMUser recipient = getUser(ChatColor.stripColor(p.getName()));
                    if (recipient == null || recipient != null && !recipient.isIgnoringPlayer(sender.getName().toLowerCase())) {
                        p.sendMessage(message);
                    }
                }
            }
            getServer().getLogger().info(message);
        }
        else if (command.getName().equalsIgnoreCase("s")) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.GOLD + "Sends a message to global chat - in italics, to indicate sarcasm.");
            } else {
                String message = "<" + sender.getName() + "> " + ChatColor.ITALIC + Join(args, 0);
                if (sender instanceof Player) {
                    for (Player p : getServer().getOnlinePlayers()) {
                        NMUser recipient = getUser(ChatColor.stripColor(p.getName()));
                        if (recipient == null || recipient != null && !recipient.isIgnoringPlayer(sender.getName().toLowerCase())) {
                            p.sendMessage(message);
                        }
                    }
                }
                getServer().getLogger().info(message);
            }
        }
        // Non-persistent ignore command.
        else if (command.getName().equalsIgnoreCase("ignore")) {
            if(args.length != 1 || !(sender instanceof Player)) {
                return false;
            }

            String ignoreName = args[0];
            Player ignorePlayer = getPlayer(ignoreName);
            if(ignorePlayer != null) {
                ignoreName = ignorePlayer.getName();
            }

            if(ignoreName.equalsIgnoreCase(sender.getName())) {
                sender.sendMessage(ChatColor.RED + "You can't ignore yourself!");
                return true;
            }

            if(getOrCreateUser(sender.getName()).addIgnoredPlayer(ignoreName.toLowerCase())) {
                sender.sendMessage(ChatColor.GOLD + "Ignoring " + ignoreName + " until the next restart.");
                this.handleStaffAlert(ignoreName.toLowerCase());
            } else {
                sender.sendMessage(ChatColor.RED + "You are already ignoring that player.");
            }
        }
        else if (command.getName().equalsIgnoreCase("unignore")) {
            if(args.length != 1 || !(sender instanceof Player)) {
                return false;
            }

            if(getOrCreateUser(sender.getName()).removeIgnoredPlayer(args[0].toLowerCase())) {
                sender.sendMessage(ChatColor.GOLD + "Removed " + args[0] + " from your ignore list.");
            } else {
                sender.sendMessage(ChatColor.RED + "You aren't ignoring " + args[0] + ".");
            }
        }
        
        return false;
    }

    public void handleStaffAlert(String name) {
        Integer count = 1;
        if (this.muteCounts.containsKey(name)) {
            count = this.muteCounts.get(name);
            count = count + 1;
            if (count % 3 == 0) {
                String msg = String.format("Player \"%s\" has been muted by %d people.", name, count);
                Bukkit.broadcast(ChatColor.GRAY + msg, "nerdmessage.ignore.alert");
            }
        }
        this.muteCounts.put(name, count);
    }
    
    public Player getPlayer(final String name) {
        Collection<? extends Player> players = getServer().getOnlinePlayers();

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

    public List<NMUser> getUsers() {
        return users;
    }
}
