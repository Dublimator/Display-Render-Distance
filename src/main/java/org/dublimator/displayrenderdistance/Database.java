package org.dublimator.displayrenderdistance;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.swing.*;
import java.sql.*;
import java.util.UUID;

public class Database {

    public HikariConfig config;
    public HikariDataSource dataSource;

    private final DisplayRenderDistance plugin;

    public Database(DisplayRenderDistance plugin) {
        this.plugin = plugin;
    }

    private Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    public void initializeDatabase() throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS DRDistance(player_uuid varchar(36), distance int, PRIMARY KEY (player_uuid))";
            statement.execute(sql);
        }

    }

    public void updatePlayerDistance(UUID uuid, Integer distance) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO DRDistance (player_uuid, distance) VALUES (?,?) ON DUPLICATE KEY UPDATE distance=?")) {

            statement.setString(1, uuid.toString());
            statement.setInt(2, distance);
            statement.setInt(3, distance);
            statement.executeUpdate();

        }
    }


    public int getDistance(UUID uuid) throws SQLException {

        String sql = "SELECT * FROM DRDistance WHERE player_uuid='" + uuid.toString() + "'";
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            int distance = -1;
            try (ResultSet resultSet = statement.executeQuery(sql)) {
                if (resultSet.next()) {
                    distance = resultSet.getInt("distance");
                }
            }

            return Math.max(distance, -1);

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }


    public void setup() {
        String host = plugin.getConfig().getString("mySQL.host");
        String user = plugin.getConfig().getString("mySQL.user");
        String password = plugin.getConfig().getString("mySQL.password");
        String database = plugin.getConfig().getString("mySQL.database");

        this.config = new HikariConfig();
        this.config.setJdbcUrl("jdbc:mysql://" + host + "/" + database + "?allowPublicKeyRetrieval=true&useSSL=false");
        if (user != null) this.config.setUsername(user);
        if (password != null) this.config.setPassword(password);
        this.config.addDataSourceProperty("cachePrepStmts", "true");
        this.config.addDataSourceProperty("prepStmtCacheSize", "250");
        this.config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        // SQLite don't need more than 1 active connection.

        this.config.setPoolName(plugin.getName() + "-" + UUID.randomUUID().toString().substring(3, 5));
        this.dataSource = new HikariDataSource(this.config);
        this.dataSource.setMaxLifetime(17000);
        plugin.getLogger().info("Database connected");
    }

    public void shutdown() throws SQLException {
        if (this.dataSource != null)
            this.dataSource.close();
    }

}
