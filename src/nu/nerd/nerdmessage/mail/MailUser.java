package nu.nerd.nerdmessage.mail;

import nu.nerd.nerdmessage.NerdMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;


public class MailUser {


    private UUID uuid;
    private String username;
    private String displayname;
    private String email;


    public MailUser() {
        this.uuid = null;
        this.username = null;
        this.displayname = null;
        this.email = null;
    }


    public MailUser(String name) throws MailException {
        this();
        try {
            Connection conn = NerdMessage.instance.getSQLConnection();
            String sql = "SELECT * FROM user WHERE last_username=? LIMIT 1;";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, name.toLowerCase());
            ResultSet res = stmt.executeQuery();
            if (res != null) {
                res.first();
                this.uuid = UUID.fromString(res.getString("uuid"));
                this.username = res.getString("last_username");
                this.displayname = res.getString("last_display_name");
                this.email = res.getString("email");
            }
            conn.close();
        } catch (SQLException ex) {
            NerdMessage.instance.getLogger().warning(String.format("Error retrieving user from database for name %s: %s", name, ex.getMessage()));
            throw new MailException("Could not retrieve user information.");
        }
    }


    public MailUser(UUID uuid) throws MailException {
        this();
        try {
            Connection conn = NerdMessage.instance.getSQLConnection();
            String sql = "SELECT * FROM user WHERE uuid=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, uuid.toString());
            ResultSet res = stmt.executeQuery();
            if (res != null) {
                res.first();
                this.uuid = UUID.fromString(res.getString("uuid"));
                this.username = res.getString("last_username");
                this.displayname = res.getString("last_display_name");
                this.email = res.getString("email");
            }
            conn.close();
        } catch (SQLException ex) {
            NerdMessage.instance.getLogger().warning(String.format("Error retrieving user from database for UUID %s: %s", uuid.toString(), ex.getMessage()));
            throw new MailException("Could not retrieve user information.");
        }
    }


    public UUID getUuid() {
        return uuid;
    }


    public String getUsername() {
        return username;
    }


    public String getDisplayname() {
        return displayname;
    }


    public String getEmail() {
        return email;
    }


    public void setUsername(String username) {
        this.username = username;
    }


    public void setDisplayname(String displayname) {
        this.displayname = displayname;
    }


    public void setEmail(String email) {
        this.email = email;
    }


    public void save() {
        try {
            Connection conn = NerdMessage.instance.getSQLConnection();
            String sql = "UPDATE user SET last_username=?, last_display_name=?, email=? WHERE uuid=?;";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, this.username.toLowerCase());
            stmt.setString(2, this.displayname);
            stmt.setString(3, this.email);
            stmt.setString(4, this.uuid.toString());
            stmt.executeUpdate();
            conn.close();
        } catch (SQLException ex) {
            NerdMessage.instance.getLogger().warning(String.format("Error saving user record: %s", ex.getMessage()));
        }
    }


}
