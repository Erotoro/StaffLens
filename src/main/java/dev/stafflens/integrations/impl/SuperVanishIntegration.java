package dev.stafflens.integrations.impl;

import de.myzelyam.api.vanish.PlayerVanishStateChangeEvent;
import dev.stafflens.StaffLensPlugin;
import dev.stafflens.integrations.BaseIntegration;
import dev.stafflens.model.ActionType;
import dev.stafflens.model.AuditEntry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

/** SuperVanish / PremiumVanish (shared API): records when a player enters or leaves vanish. */
public class SuperVanishIntegration extends BaseIntegration {

    public SuperVanishIntegration(StaffLensPlugin plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVanishStateChange(PlayerVanishStateChangeEvent event) {
        ActionType type = event.isVanishing() ? ActionType.VANISH_ON : ActionType.VANISH_OFF;
        String cause = event.getCause();
        auditService.log(new AuditEntry(
                event.getUUID(),
                event.getName(),
                type,
                event.getName(),
                "Vanish",
                cause != null ? "Cause: " + cause : "",
                System.currentTimeMillis()
        ));
    }
}
