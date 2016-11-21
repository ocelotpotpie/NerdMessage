package nu.nerd.nerdmessage.mail;


import nu.nerd.nerdmessage.NerdMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MailMessage {


    private int id;
    private UUID to;
    private UUID from;
    private String body;
    private long dateSent;
    private boolean read;
    private boolean notified;
    private String sourceServer;


    public MailMessage() {
        id = 0;
        to = null;
        from = null;
        body = null;
        dateSent = 0;
        read = false;
        notified = false;
        sourceServer = null;
    }


    /**
     * Send a message
     * @param from The sender
     * @param to The recipient
     * @param msg The message to send
     */
    public static void send(MailUser from, MailUser to, String msg) throws MailException {
        try {
            Connection conn = NerdMessage.instance.getSQLConnection();
            String sql = "INSERT INTO message (`to`, `from`, `body`, `date_sent`, `read`, `notified`, `source_server`) VALUES (?, ?, ?, ?, 0, 0, ?);";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, to.getUuid().toString());
            stmt.setString(2, from.getUuid().toString());
            stmt.setString(3, msg);
            stmt.setLong(4, System.currentTimeMillis());
            stmt.setString(5, NerdMessage.instance.getServerName());
            stmt.executeUpdate();
            conn.close();
        } catch (SQLException ex) {
            NerdMessage.instance.getLogger().warning(String.format("Error sending message to player %s: %s", to.getUsername(), ex.getMessage()));
            throw new MailException("Message could not be sent.");
        }
    }


    /**
     * Mark a user's pending messages when the user has been alerted to their existence.
     * @param user the UUID of the user
     */
    public static void flagNotified(UUID user) {
        try {
            Connection conn = NerdMessage.instance.getSQLConnection();
            String sql = "UPDATE message SET `notified`=1 WHERE `to`=? AND `notified`=0;";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, user.toString());
            stmt.executeUpdate();
            conn.close();
        } catch (SQLException ex) {
            NerdMessage.instance.getLogger().warning(String.format("Error updating notified state: %s", ex.getMessage()));
        }
    }


    /**
     * Obtain a list of new messages waiting for player notification
     * @param user the UUID of the player to check messages for
     * @return List of messages
     */
    public static List<MailMessage> findUnnotified(UUID user) {
        List<MailMessage> list = new ArrayList<>();
        try {
            Connection conn = NerdMessage.instance.getSQLConnection();
            String sql = "SELECT * FROM message WHERE `to`=? AND `notified`=0;";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, user.toString());
            ResultSet res = stmt.executeQuery();
            if (res != null) {
                while (res.next()) {
                    MailMessage msg = buildObject(res);
                    list.add(msg);
                }
            }
            conn.close();
        } catch (SQLException ex) {
            NerdMessage.instance.getLogger().warning(String.format("Error querying messages: %s", ex.getMessage()));
        }
        return list;
    }


    public static MailMessage buildObject(ResultSet res) throws SQLException {
        MailMessage msg = new MailMessage();
        msg.setId(res.getInt("id"));
        msg.setTo(UUID.fromString(res.getString("to")));
        msg.setFrom(UUID.fromString(res.getString("from")));
        msg.setBody(res.getString("body"));
        msg.setDateSent(res.getLong("date_sent"));
        msg.setRead(res.getBoolean("read"));
        msg.setNotified(res.getBoolean("notified"));
        msg.setSourceServer(res.getString("source_server"));
        return msg;
    }


    /**
     * Persist the object to the database
     */
    public void save() {
        try {
            Connection conn = NerdMessage.instance.getSQLConnection();
            String sql = "UPDATE message SET to=?, from=?, body=?, date_sent=?, read=?, notified=?, source_server=? WHERE id=?;";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, this.to.toString());
            stmt.setString(2, this.from.toString());
            stmt.setString(3, this.body);
            stmt.setLong(4, this.dateSent);
            stmt.setBoolean(5, this.read);
            stmt.setBoolean(6, this.notified);
            stmt.setString(7, this.sourceServer);
            stmt.executeUpdate();
            conn.close();
        } catch (SQLException ex) {
            NerdMessage.instance.getLogger().warning(String.format("Error saving message record: %s", ex.getMessage()));
        }
    }


    public int getId() {
        return id;
    }


    public void setId(int id) {
        this.id = id;
    }


    public UUID getTo() {
        return to;
    }


    public void setTo(UUID to) {
        this.to = to;
    }


    public UUID getFrom() {
        return from;
    }


    public void setFrom(UUID from) {
        this.from = from;
    }


    public String getBody() {
        return body;
    }


    public void setBody(String body) {
        this.body = body;
    }


    public long getDateSent() {
        return dateSent;
    }


    public void setDateSent(long dateSent) {
        this.dateSent = dateSent;
    }


    public boolean isRead() {
        return read;
    }


    public void setRead(boolean read) {
        this.read = read;
    }


    public boolean isNotified() {
        return notified;
    }


    public void setNotified(boolean notified) {
        this.notified = notified;
    }


    public String getSourceServer() {
        return sourceServer;
    }


    public void setSourceServer(String sourceServer) {
        this.sourceServer = sourceServer;
    }


}
