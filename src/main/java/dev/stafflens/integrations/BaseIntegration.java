package dev.stafflens.integrations;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.audit.AuditService;
import dev.stafflens.logger.AuditLogger;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public abstract class BaseIntegration implements Listener {
    protected final StaffLensPlugin plugin;
    protected final AuditLogger logger;
    protected final AuditService auditService;

    public BaseIntegration(StaffLensPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getAuditLogger();
        this.auditService = plugin.getAuditService();
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
    }
}
