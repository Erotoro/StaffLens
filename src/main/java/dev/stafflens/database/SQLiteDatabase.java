package dev.stafflens.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.stafflens.StaffLensPlugin;

import java.io.File;

public class SQLiteDatabase extends AbstractSqlDatabase {

    public SQLiteDatabase(StaffLensPlugin plugin) {
        super(plugin);
    }

    @Override
    protected HikariDataSource createDataSource() {
        File file = new File(plugin.getDataFolder(), "database.db");
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        config.setPoolName("StaffLens-SQLite");
        return new HikariDataSource(config);
    }

    @Override
    protected String idColumnDefinition() {
        return "INTEGER PRIMARY KEY AUTOINCREMENT";
    }

    @Override
    protected String caseInsensitive() {
        return " COLLATE NOCASE";
    }
}
