package nu.nerd.nerdmessage.alerts;

import org.bukkit.ChatColor;


public class AlertMessage {


    private String text;
    private ChatColor color;


    public AlertMessage(String text) {
        this.text = text;
        this.color = ChatColor.LIGHT_PURPLE;
    }


    public AlertMessage(String text, ChatColor color) {
        this.text = text;
        this.color = color;
    }


    public AlertMessage(String text, String color) {
        try {
            this.text = text;
            this.color = ChatColor.valueOf(color.toUpperCase());
        } catch (Exception ex) {
            this.color = ChatColor.LIGHT_PURPLE;
        }
    }


    public String getText() {
        return text;
    }


    public ChatColor getColor() {
        return color;
    }


}
