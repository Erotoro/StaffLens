package dev.stafflens;

import dev.stafflens.anomaly.AnomalyDetector;
import dev.stafflens.audit.AuditService;
import dev.stafflens.command.StaffLensCommand;
import dev.stafflens.command.StaffLensTabCompleter;
import dev.stafflens.config.ConfigManager;
import dev.stafflens.database.Database;
import dev.stafflens.database.MySQLDatabase;
import dev.stafflens.database.SQLiteDatabase;
import dev.stafflens.integrations.IntegrationManager;
import dev.stafflens.logger.AuditLogger;
import dev.stafflens.notify.DiscordNotifier;
import dev.stafflens.tracking.CommandMappingService;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class StaffLensPlugin extends JavaPlugin {

    private static final long LOGGER_DRAIN_TIMEOUT_MILLIS = 5000L;
    // bStats plugin id. Get yours at https://bstats.org and set it here to enable metrics.
    private static final int BSTATS_PLUGIN_ID = 31802;

    private ConfigManager configManager;
    private Database database;
    private AuditLogger auditLogger;
    private AuditService auditService;
    private AnomalyDetector anomalyDetector;
    private DiscordNotifier discordNotifier;
    private CommandMappingService commandMappingService;
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

        buildServices();

        this.integrationManager = new IntegrationManager(this);
        this.integrationManager.loadAll();

        registerCommand();
        scheduleRetentionCleanup();
        setupMetrics();

        getLogger().info("StaffLens enabled successfully (Folia compatible)!");
    }

    @Override
    public void onDisable() {
        shutdownRuntime();
    }

    private void registerCommand() {
        PluginCommand command = getCommand("stafflens");
        if (command != null) {
            StaffLensCommand executor = new StaffLensCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(new StaffLensTabCompleter(executor));
        }
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

    private void buildServices() {
        this.auditLogger = new AuditLogger(this, database);
        this.anomalyDetector = new AnomalyDetector(this);
        this.discordNotifier = new DiscordNotifier(this);
        this.commandMappingService = new CommandMappingService(this);
        this.auditService = new AuditService(this);
    }

    private void setupMetrics() {
        if (!getConfig().getBoolean("metrics", true)) {
            return;
        }
        if (BSTATS_PLUGIN_ID <= 0) {
            getLogger().info("bStats metrics are bundled but inactive: set BSTATS_PLUGIN_ID to your id from https://bstats.org.");
            return;
        }
        Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        metrics.addCustomChart(new SimplePie("database_type",
                () -> getConfig().getString("database.type", "sqlite")));
        metrics.addCustomChart(new SimplePie("anomaly_detection",
                () -> getConfig().getBoolean("anomaly.enabled", true) ? "enabled" : "disabled"));
        metrics.addCustomChart(new SimplePie("discord_webhook",
                () -> getConfig().getBoolean("discord.enabled", false) ? "enabled" : "disabled"));
    }

    private void scheduleRetentionCleanup() {
        int intervalHours = getConfig().getInt("log.cleanup-interval-hours", 0);
        if (intervalHours <= 0) {
            return;
        }
        long periodSeconds = intervalHours * 60L * 60L;
        getServer().getAsyncScheduler().runAtFixedRate(this, task -> {
            if (database != null) {
                database.cleanupOldEntries(getConfig().getInt("log.retention-days", 90));
            }
        }, periodSeconds, periodSeconds, TimeUnit.SECONDS);
    }

    public void reloadRuntime() throws SQLException {
        if (integrationManager != null) {
            integrationManager.unloadAll();
        }

        shutdownLogger();
        closeDatabase();

        configManager.load();
        setupDatabase();
        buildServices();

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

    public AnomalyDetector getAnomalyDetector() {
        return anomalyDetector;
    }

    public DiscordNotifier getDiscordNotifier() {
        return discordNotifier;
    }

    public CommandMappingService getCommandMappingService() {
        return commandMappingService;
    }
}
