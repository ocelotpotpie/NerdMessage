package nu.nerd.nerdmessage;

import java.sql.Connection;
import java.sql.SQLException;

import org.bukkit.configuration.file.FileConfiguration;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.ServerConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.PoolInitializationException;

import nu.nerd.nerdmessage.mail.MailMessage;
import nu.nerd.nerdmessage.mail.MailUser;

public class MySQLHandler {

    private NerdMessage plugin;
    private final HikariDataSource dataSource;
    private EbeanServer ebeanServer;

    public MySQLHandler(NerdMessage plugin, FileConfiguration config) {
        this.plugin = plugin;
        dataSource = new HikariDataSource();
        if (!plugin.getConfig().getBoolean("mysql.enabled"))
            return;
        try {
            // Load the JDBC driver. See:
            // https://stackoverflow.com/questions/5556664/how-to-fix-no-suitable-driver-found-for-jdbcmysql-localhost-dbname-error-w/19259608#19259608
            Class.forName("com.mysql.jdbc.Driver");
            setUpDataSource(config);
            setUpEbean();
        } catch (PoolInitializationException | ClassNotFoundException ex) {
            plugin.getConfig().set("mysql.enabled", false);
            plugin.getLogger().warning("Could not connect to database. Mail will be disabled. Reason: " + ex.getMessage());
        }
    }

    /**
     * Get the eBean handle
     */
    public EbeanServer getDatabase() {
        return ebeanServer;
    }

    /**
     * Shut down the MySQL pool
     */
    public void close() {
        dataSource.close();
    }

    /**
     * Get a connection from the MySQL pool
     * 
     * @return connection
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        try {
            return dataSource.getConnection();
        } catch (SQLException ex) {
            plugin.getLogger().warning("MySQL connection error: " + ex.getMessage());
            throw ex;
        } catch (PoolInitializationException ex) {
            throw new SQLException("MySQL pool was not initialized.");
        }
    }

    /**
     * Set up the MySQL connection pool
     */
    private void setUpDataSource(FileConfiguration config) {
        dataSource.setJdbcUrl(config.getString("mysql.url"));
        dataSource.setUsername(config.getString("mysql.username"));
        dataSource.setPassword(config.getString("mysql.password"));
        dataSource.setMaximumPoolSize(config.getInt("max_connections", 5));
        dataSource.setPoolName("NerdMessage");
        dataSource.addDataSourceProperty("cachePrepStmts", "true");
    }

    /**
     * Set up the eBean ORM
     */
    private void setUpEbean() {
        ServerConfig config = new ServerConfig();
        config.setName("NerdMessage");
        config.setDefaultServer(false);
        config.setDataSource(dataSource);
        config.setDdlGenerate(false);
        config.setDdlRun(false);
        config.addClass(MailUser.class);
        config.addClass(MailMessage.class);
        ClassLoader previousCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(plugin.getClass().getClassLoader());
        ebeanServer = EbeanServerFactory.create(config);
        Thread.currentThread().setContextClassLoader(previousCL);
    }

}
