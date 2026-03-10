package dev.stafflens.audit;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.model.ActionType;
import dev.stafflens.model.AuditEntry;
import dev.stafflens.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;

public class AuditService {

    private final StaffLensPlugin plugin;

    public AuditService(StaffLensPlugin plugin) {
        this.plugin = plugin;
    }

    public void log(AuditEntry entry) {
        plugin.getAuditLogger().log(entry);
        notifyCritical(entry);
    }

    public void log(CommandSender actor, String actorName, ActionType action, String targetName, String reason, String details) {
        log(new AuditEntry(
                actor instanceof Player player ? player.getUniqueId() : null,
                actorName,
                action,
                targetName,
                reason,
                details,
                System.currentTimeMillis()
        ));
    }

    private void notifyCritical(AuditEntry entry) {
        Set<ActionType> critical = ActionType.parseConfigList(plugin.getConfig().getStringList("notify.critical-actions"));
        if (!critical.contains(entry.action())) {
            return;
        }

        String message = "<red>[StaffLens] <yellow>" + MessageUtil.escapeMiniMessage(entry.staffName())
                + " <gray>used <gold>" + MessageUtil.escapeMiniMessage(entry.action().displayName())
                + " <gray>on <white>" + MessageUtil.escapeMiniMessage(entry.targetName());

        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.hasPermission("stafflens.notify")) {
                    player.sendMessage(MessageUtil.parse(message));
                }
            }
            plugin.getLogger().info("[StaffLens] " + entry.staffName() + " used " + entry.action().name() + " on " + entry.targetName());
        });
    }
}
