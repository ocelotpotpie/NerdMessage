package nu.nerd.nerdmessage;


public class NMUser {
    private String name;
    private String replyTo;
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
    
    public String getReplyTo() {
        return this.replyTo;
    }
    
    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }
}
