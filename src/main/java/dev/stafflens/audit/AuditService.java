package dev.stafflens.audit;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.anomaly.AnomalyDetector;
import dev.stafflens.anomaly.AnomalyResult;
import dev.stafflens.model.ActionType;
import dev.stafflens.model.AuditEntry;
import dev.stafflens.notify.DiscordNotifier;
import dev.stafflens.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.UUID;

/**
 * Single entry point for recording staff actions. Everything funnels through {@link #log(AuditEntry)}
 * so enrichment, anomaly detection, persistence and notifications happen the same way for every source.
 */
public class AuditService {

    private final StaffLensPlugin plugin;

    public AuditService(StaffLensPlugin plugin) {
        this.plugin = plugin;
    }

    /** Records a ready-made entry: stamp server name, screen for anomalies, persist, notify. */
    public void log(AuditEntry entry) {
        AuditEntry enriched = entry.withContext(serverName(), null, null, null, null, null);

        AnomalyDetector detector = plugin.getAnomalyDetector();
        AnomalyResult anomaly = detector != null ? detector.inspect(enriched) : AnomalyResult.clean();
        AuditEntry finalEntry = anomaly.flagged() ? enriched.withFlagged(true) : enriched;

        plugin.getAuditLogger().log(finalEntry);
        dispatchNotifications(finalEntry, anomaly);
    }

    /** Builds an entry from a live sender, capturing location and IP when it's an online player. */
    public void log(CommandSender actor, String actorName, ActionType action, String targetName, String reason, String details) {
        log(buildEntry(actor, actorName, action, targetName, reason, details));
    }

    private AuditEntry buildEntry(CommandSender actor, String actorName, ActionType action,
                                  String targetName, String reason, String details) {
        UUID uuid = actor instanceof Player player ? player.getUniqueId() : null;
        AuditEntry entry = new AuditEntry(uuid, actorName, action, targetName, reason, details, System.currentTimeMillis());

        if (actor instanceof Player player) {
            Location location = player.getLocation();
            String world = location.getWorld() != null ? location.getWorld().getName() : null;
            entry = entry.withContext(serverName(), world,
                    location.getBlockX(), location.getBlockY(), location.getBlockZ(), resolveIp(player));
        }
        return entry;
    }

    private void dispatchNotifications(AuditEntry entry, AnomalyResult anomaly) {
        boolean critical = criticalActions().contains(entry.action());

        if (anomaly.flagged()) {
            String message = "<dark_red>[StaffLens] <red>Anomaly: <yellow>" + escape(entry.staffName())
                    + " <gray>" + escape(entry.action().displayName())
                    + " <gray>on <white>" + escape(entry.targetName())
                    + " <dark_gray>(" + escape(anomaly.describe()) + ")";
            broadcast("stafflens.alerts", message);
            plugin.getLogger().warning("[Anomaly] " + entry.staffName() + " " + entry.action().name()
                    + " on " + entry.targetName() + " :: " + anomaly.describe());

            DiscordNotifier discord = plugin.getDiscordNotifier();
            if (discord != null) {
                discord.sendAnomaly(entry, anomaly.reasons());
            }
        }

        if (critical) {
            String message = "<red>[StaffLens] <yellow>" + escape(entry.staffName())
                    + " <gray>used <gold>" + escape(entry.action().displayName())
                    + " <gray>on <white>" + escape(entry.targetName());
            broadcast("stafflens.notify", message);
            plugin.getLogger().info("[StaffLens] " + entry.staffName() + " used "
                    + entry.action().name() + " on " + entry.targetName());

            DiscordNotifier discord = plugin.getDiscordNotifier();
            if (discord != null) {
                discord.sendCritical(entry);
            }
        }
    }

    private void broadcast(String permission, String message) {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.hasPermission(permission)) {
                    player.sendMessage(MessageUtil.parse(message));
                }
            }
        });
    }

    private Set<ActionType> criticalActions() {
        return ActionType.parseConfigList(plugin.getConfig().getStringList("notify.critical-actions"));
    }

    private String serverName() {
        String name = plugin.getConfig().getString("server-name", "");
        return name == null || name.isBlank() ? null : name;
    }

    private String resolveIp(Player player) {
        InetSocketAddress address = player.getAddress();
        if (address == null || address.getAddress() == null) {
            return null;
        }
        return address.getAddress().getHostAddress();
    }

    private static String escape(String value) {
        return MessageUtil.escapeMiniMessage(value);
    }
}
