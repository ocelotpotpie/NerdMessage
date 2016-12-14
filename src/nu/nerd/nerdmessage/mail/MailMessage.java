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
    private String toName;
    private String fromName;


    public MailMessage() {
        id = 0;
        to = null;
        from = null;
        body = null;
        dateSent = 0;
        read = false;
        notified = false;
        sourceServer = null;
        toName = null;
        fromName = null;
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
     * Mark a message as read
     * @param user the UUID of the user to work on
     * @param index the number of the message in the list (starting with 1)
     */
    public static void flagRead(UUID user, int index) {
        try {
            Connection conn = NerdMessage.instance.getSQLConnection();
            String sql = "SELECT `id` from message WHERE `to`=? AND `read`=0;";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, user.toString());
            stmt.execute();
            ResultSet res = stmt.executeQuery();
            if (res != null) {
                res.absolute(index);
                int rowId = res.getInt("id");
                sql = "UPDATE message SET `read`=1 WHERE `id`=?;";
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, rowId);
                stmt.execute();
            }
            conn.close();
        } catch (SQLException ex) {
            NerdMessage.instance.getLogger().warning(String.format("Error updating read state: %s", ex.getMessage()));
        }
    }


    /**
     * Mark all of a user's messages as read
     * @param user the UUID of the user
     */
    public static void flagAllRead(UUID user) {
        try {
            Connection conn = NerdMessage.instance.getSQLConnection();
            String sql = "UPDATE message SET `read`=1 WHERE `to`=? AND `read`=0;";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, user.toString());
            stmt.executeUpdate();
            conn.close();
        } catch (SQLException ex) {
            NerdMessage.instance.getLogger().warning(String.format("Error updating read state: %s", ex.getMessage()));
        }
    }


    /**
     * Obtain a list of unread messages
     * @param user the UUID of the player to check messages for
     * @return List of messages
     */
    public static List<MailMessage> findUnread(UUID user) {
        List<MailMessage> list = new ArrayList<>();
        try {
            Connection conn = NerdMessage.instance.getSQLConnection();
            String sql = "SELECT message.*, u1.last_display_name AS from_name, u2.last_display_name AS to_name " +
                    "FROM message INNER JOIN user u1 ON message.from=u1.uuid INNER JOIN user u2 on message.to=u2.uuid " +
                    "WHERE `to`=? AND `read`=0;";
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


    /**
     * Obtain a list of all messages between two users
     * @param user1 the UUID of the first user
     * @param user2 the UUID of the second user
     * @return List of messages
     */
    public static List<MailMessage> findThread(UUID user1, UUID user2) {
        List<MailMessage> list = new ArrayList<>();
        try {
            Connection conn = NerdMessage.instance.getSQLConnection();
            String sql = "SELECT message.*, u1.last_display_name AS from_name, u2.last_display_name AS to_name " +
                    "FROM message INNER JOIN user u1 ON message.from=u1.uuid INNER JOIN user u2 on message.to=u2.uuid " +
                    "WHERE (`to`=? AND `from`=?) OR (`to`=? AND `from`=?) ORDER BY `date_sent` DESC;";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, user1.toString());
            stmt.setString(2, user2.toString());
            stmt.setString(3, user2.toString());
            stmt.setString(4, user1.toString());
            ResultSet res = stmt.executeQuery();
            if (res != null) {
                while (res.next()) {
                    MailMessage msg = buildObject(res);
                    list.add(msg);
                }
            }
            conn.close();
        } catch (SQLException ex) {
            NerdMessage.instance.getLogger().warning(String.format("Error querying message thread: %s", ex.getMessage()));
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
        if (res.getString("to_name") != null) {
            msg.setToName(res.getString("to_name"));
        }
        if (res.getString("from_name") != null) {
            msg.setFromName(res.getString("from_name"));
        }
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


    public String getToName() {
        return toName;
    }


    public void setToName(String toName) {
        this.toName = toName;
    }


    public String getFromName() {
        return fromName;
    }


    public void setFromName(String fromName) {
        this.fromName = fromName;
    }


}
