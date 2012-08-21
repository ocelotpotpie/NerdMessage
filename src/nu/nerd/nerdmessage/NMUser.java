package nu.nerd.nerdmessage;


public class NMUser {
    private String name;
    private String lastSent;
    private String lastReceived;
    private NerdMessage plugin;

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
    
    public String getLastSent() {
        return this.lastSent;
    }
    
    public void setLastSent(String lastSent) {
        this.lastSent = lastSent;
    }
    
    public String getLastReceived() {
        return this.lastReceived;
    }
    
    public void setLastReceived(String lastReceived) {
        this.lastReceived = lastReceived;
    }
}
