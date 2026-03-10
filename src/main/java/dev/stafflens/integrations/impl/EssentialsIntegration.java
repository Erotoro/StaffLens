package dev.stafflens.integrations.impl;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.integrations.BaseIntegration;
import dev.stafflens.model.ActionType;
import dev.stafflens.model.AuditEntry;
import net.ess3.api.events.MuteStatusChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class EssentialsIntegration extends BaseIntegration {
    public EssentialsIntegration(StaffLensPlugin plugin) { super(plugin); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMute(MuteStatusChangeEvent e) {
        if (e.getAffected() == null) {
            return;
        }
        String controller = e.getController() != null ? e.getController().getName() : "Console";
        auditService.log(new AuditEntry(
                e.getController() != null ? e.getController().getUniqueId() : null,
                controller,
                e.getValue() ? ActionType.MUTE : ActionType.UNMUTE,
                e.getAffected().getName(),
                e.getReason(),
                "",
                System.currentTimeMillis()
        ));
    }
}
