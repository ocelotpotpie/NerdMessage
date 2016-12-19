package nu.nerd.nerdmessage.mail;

import com.avaje.ebean.Query;
import com.avaje.ebean.validation.NotNull;
import nu.nerd.nerdmessage.NerdMessage;

import javax.persistence.*;
import java.util.UUID;


@Entity()
@Table(name="user")
public class MailUser {


    @Id
    @Column(name="uuid")
    private UUID uuid;

    @NotNull
    @Column(name="last_username")
    private String username;

    @NotNull
    @Column(name="last_display_name")
    private String displayname;

    @Column(name="email")
    private String email;


    public MailUser() {
        this.uuid = null;
        this.username = null;
        this.displayname = null;
        this.email = null;
    }


    public MailUser(UUID uuid, String username, String displayname) {
        this();
        this.uuid = uuid;
        this.username = username;
        this.displayname = displayname;
    }


    public static MailUser find(String name) throws MailException {
        Query<MailUser> query = NerdMessage.instance.getDatabase().find(MailUser.class).where().ieq("username", name).query();
        if (query != null) {
            return query.findUnique();
        }
        throw new MailException("Could not retrieve user information.");
    }


    public static MailUser find(UUID uuid) throws MailException {
        Query<MailUser> query = NerdMessage.instance.getDatabase().find(MailUser.class).where().eq("uuid", uuid).query();
        if (query != null) {
            return query.findUnique();
        }
        throw new MailException("Could not retrieve user information.");
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


    public void setUuid(UUID uuid) {
        this.uuid = uuid;
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
        NerdMessage.instance.getDatabase().save(this);
    }


    public void update() {
        NerdMessage.instance.getDatabase().update(this);
    }


    public void delete() {
        NerdMessage.instance.getDatabase().delete(this);
    }


}
