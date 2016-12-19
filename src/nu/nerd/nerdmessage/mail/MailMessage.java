package nu.nerd.nerdmessage.mail;


import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.ExpressionFactory;
import com.avaje.ebean.Query;
import com.avaje.ebean.SqlUpdate;
import com.avaje.ebean.validation.NotNull;
import nu.nerd.nerdmessage.NerdMessage;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity()
@Table(name="`message`")
public class MailMessage {


    @Id
    private int id;

    @NotNull
    @Column(name="`to`")
    private UUID to;

    @NotNull
    @Column(name="`from`")
    private UUID from;

    @NotNull
    @Column(name="`body`", columnDefinition="TEXT")
    private String body;

    @NotNull
    @Column(name="`date_sent`")
    private long dateSent;

    @Column(name="`read`", columnDefinition = "BIT(1)")
    private boolean read;

    @Column(name="`notified`", columnDefinition = "BIT(1)")
    private boolean notified;

    @Column(name="`source_server`")
    private String sourceServer;

    @Transient
    @ManyToOne()
    @JoinColumn(name="`from`", insertable = false, updatable = false)
    private MailUser fromUser;

    @Transient
    @ManyToOne()
    @JoinColumn(name="`to`", insertable = false, updatable = false)
    private MailUser toUser;


    public MailMessage() {
        to = null;
        from = null;
        body = null;
        dateSent = 0;
        read = false;
        notified = false;
        sourceServer = null;
        fromUser = null;
        toUser = null;
    }


    /**
     * Send a message
     * @param from The sender
     * @param to The recipient
     * @param msg The message to send
     */
    public static void send(MailUser from, MailUser to, String msg) throws MailException {
        try {
            MailMessage message = new MailMessage();
            message.setTo(to.getUuid());
            message.setFrom(from.getUuid());
            message.setBody(msg);
            message.setDateSent(System.currentTimeMillis());
            message.setRead(false);
            message.setNotified(false);
            message.setSourceServer(NerdMessage.instance.getServerName());
            message.save();
        } catch (Exception ex) {
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
            EbeanServer db = NerdMessage.instance.getDatabase();
            SqlUpdate update = db.createSqlUpdate("UPDATE message SET `notified`=1 WHERE `to`=:to AND `notified`=0;");
            update.setParameter("to", user.toString());
            update.execute();
        } catch (Exception ex) {
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
            List<MailMessage> messages = findUnread(user);
            if (messages.size() > 0) {
                MailMessage message = messages.get(index - 1);
                message.setRead(true);
                message.update();
            }
        } catch (Exception ex) {
            NerdMessage.instance.getLogger().warning(String.format("Error updating read state: %s", ex.getMessage()));
        }
    }


    /**
     * Mark all of a user's messages as read
     * @param user the UUID of the user
     */
    public static void flagAllRead(UUID user) {
        try {
            EbeanServer db = NerdMessage.instance.getDatabase();
            SqlUpdate update = db.createSqlUpdate("UPDATE message SET `read`=1 WHERE `to`=:to AND `read`=0;");
            update.setParameter("to", user.toString());
            update.execute();
        } catch (Exception ex) {
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
            EbeanServer db = NerdMessage.instance.getDatabase();
            Query<MailMessage> query = db.find(MailMessage.class).where().ieq("to", user.toString()).eq("read", 0).query();
            if (query != null) {
                list = query.findList();
            }
        } catch (Exception ex) {
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
            EbeanServer db = NerdMessage.instance.getDatabase();
            Query<MailMessage> query = db.find(MailMessage.class).where().ieq("to", user.toString()).eq("notified", 0).query();
            if (query != null) {
                list = query.findList();
            }
        } catch (Exception ex) {
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
            EbeanServer db = NerdMessage.instance.getDatabase();
            ExpressionFactory expr = db.getExpressionFactory();
            Query<MailMessage> query = db.find(MailMessage.class).where().or(
                    expr.and(expr.eq("to", user1), expr.eq("from", user2)),
                    expr.and(expr.eq("to", user2), expr.eq("from", user1))
            ).orderBy().desc("dateSent");
            if (query != null) {
                list = query.findList();
            }
        } catch (Exception ex) {
            NerdMessage.instance.getLogger().warning(String.format("Error querying message thread: %s", ex.getMessage()));
        }
        return list;
    }


    public void save() {
        NerdMessage.instance.getDatabase().save(this);
    }


    public void update() {
        NerdMessage.instance.getDatabase().update(this);
    }


    public void delete() {
        NerdMessage.instance.getDatabase().delete(this);
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


    public MailUser getFromUser() {
        return fromUser;
    }


    public MailUser getToUser() {
        return toUser;
    }


    public void setFromUser(MailUser fromUser) {
        this.fromUser = fromUser;
    }


    public void setToUser(MailUser toUser) {
        this.toUser = toUser;
    }


    public String getToName() {
        return (toUser == null) ? null : toUser.getDisplayname();
    }


    public String getFromName() {
        return (fromUser == null) ? null : fromUser.getDisplayname();
    }


}
