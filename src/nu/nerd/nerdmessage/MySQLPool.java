package nu.nerd.nerdmessage;


import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.PoolInitializationException;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;


public class MySQLPool {


    private NerdMessage plugin;
    private final HikariDataSource dataSource;


    public MySQLPool(NerdMessage plugin, FileConfiguration config) {
        this.plugin = plugin;
        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(config.getString("mysql.url"));
        dataSource.setUsername(config.getString("mysql.username"));
        dataSource.setPassword(config.getString("mysql.password"));
        dataSource.setMaximumPoolSize(config.getInt("max_connections", 5));
        dataSource.setPoolName("NerdMessage");
        dataSource.addDataSourceProperty("cachePrepStmts", "true");
    }


    public void close() {
        dataSource.close();
    }


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


}
