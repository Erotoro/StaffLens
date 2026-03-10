package dev.stafflens;

import dev.stafflens.audit.AuditService;
import dev.stafflens.command.StaffLensCommand;
import dev.stafflens.config.ConfigManager;
import dev.stafflens.database.Database;
import dev.stafflens.database.MySQLDatabase;
import dev.stafflens.database.SQLiteDatabase;
import dev.stafflens.integrations.IntegrationManager;
import dev.stafflens.logger.AuditLogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public class StaffLensPlugin extends JavaPlugin {

    private static final long LOGGER_DRAIN_TIMEOUT_MILLIS = 5000L;

    private ConfigManager configManager;
    private Database database;
    private AuditLogger auditLogger;
    private AuditService auditService;
    private IntegrationManager integrationManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        try {
            setupDatabase();
        } catch (SQLException e) {
            getLogger().severe("Could not initialize database! Disabling plugin.");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.auditLogger = new AuditLogger(this, database);
        this.auditService = new AuditService(this);

        this.integrationManager = new IntegrationManager(this);
        this.integrationManager.loadAll();

        if (getCommand("stafflens") != null) {
            getCommand("stafflens").setExecutor(new StaffLensCommand(this));
        }

        getLogger().info("StaffLens enabled successfully (Folia compatible)!");
    }

    @Override
    public void onDisable() {
        shutdownRuntime();
    }

    private void setupDatabase() throws SQLException {
        String type = configManager.getConfig().getString("database.type", "sqlite");
        if ("mysql".equalsIgnoreCase(type)) {
            database = new MySQLDatabase(this);
        } else {
            database = new SQLiteDatabase(this);
        }
        database.init();
        database.cleanupOldEntries(configManager.getConfig().getInt("log.retention-days", 90));
    }

    public void reloadRuntime() throws SQLException {
        if (integrationManager != null) {
            integrationManager.unloadAll();
        }

        shutdownLogger();
        closeDatabase();

        configManager.load();
        setupDatabase();
        this.auditLogger = new AuditLogger(this, database);
        this.auditService = new AuditService(this);

        if (integrationManager == null) {
            integrationManager = new IntegrationManager(this);
        }
        integrationManager.loadAll();
    }

    private void shutdownRuntime() {
        if (integrationManager != null) {
            integrationManager.unloadAll();
        }
        shutdownLogger();
        closeDatabase();
    }

    private void shutdownLogger() {
        if (auditLogger != null) {
            auditLogger.shutdownAndDrain(LOGGER_DRAIN_TIMEOUT_MILLIS);
        }
    }

    private void closeDatabase() {
        if (database != null) {
            database.close();
            database = null;
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public Database getDatabase() {
        return database;
    }

    public AuditLogger getAuditLogger() {
        return auditLogger;
    }

    public AuditService getAuditService() {
        return auditService;
    }
}
