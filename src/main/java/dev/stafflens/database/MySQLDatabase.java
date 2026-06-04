package dev.stafflens.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.stafflens.StaffLensPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class MySQLDatabase extends AbstractSqlDatabase {

    public MySQLDatabase(StaffLensPlugin plugin) {
        super(plugin);
    }

    @Override
    protected HikariDataSource createDataSource() {
        FileConfiguration cfg = plugin.getConfig();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + cfg.getString("database.mysql.host")
                + ":" + cfg.getString("database.mysql.port")
                + "/" + cfg.getString("database.mysql.database"));
        config.setUsername(cfg.getString("database.mysql.username"));
        config.setPassword(cfg.getString("database.mysql.password"));
        config.setPoolName("StaffLens-MySQL");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        return new HikariDataSource(config);
    }

    @Override
    protected String idColumnDefinition() {
        return "INT AUTO_INCREMENT PRIMARY KEY";
    }
}
