package nu.nerd.nerdmessage;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NMUser {
    private String name;
    private String replyTo;
    private NerdMessage plugin;

    private Set<String> ignoring = new HashSet<String>();

    public NMUser(NerdMessage plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getReplyTo() {
        return this.replyTo;
    }
    
    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public Set<String> getIgnoredPlayers() {
        return ignoring;
    }

    public boolean addIgnoredPlayer(String playerName) {
        return ignoring.add(playerName);
    }

    public boolean removeIgnoredPlayer(String playerName) {
        return ignoring.remove(playerName);
    }

    public boolean isIgnoringPlayer(String playerName) {
        return ignoring.contains(playerName);
    }
}
