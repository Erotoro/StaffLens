package dev.stafflens.integrations.impl;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.integrations.BaseIntegration;
import dev.stafflens.model.ActionType;
import dev.stafflens.model.AuditEntry;
import me.leoko.advancedban.bukkit.event.PunishmentEvent;
import org.bukkit.event.EventHandler;

public class AdvancedBanIntegration extends BaseIntegration {
    public AdvancedBanIntegration(StaffLensPlugin plugin) { super(plugin); }

    @EventHandler
    public void onPunish(PunishmentEvent e) {
        if (e.getPunishment() == null || e.getPunishment().getType() == null) return;

        String pType = e.getPunishment().getType().getName();
        if (pType == null) return;
        ActionType type = mapPunishmentType(pType);

        if (type != null) {
            String operator = e.getPunishment().getOperator();
            auditService.log(new AuditEntry(null, operator != null ? operator : "Console", type, e.getPunishment().getName(), e.getPunishment().getReason(), "", System.currentTimeMillis()));
        }
    }

    private ActionType mapPunishmentType(String punishmentType) {
        return switch (punishmentType.toLowerCase()) {
            case "ban" -> ActionType.BAN;
            case "tempban", "temporaryban" -> ActionType.TEMP_BAN;
            case "unban" -> ActionType.UNBAN;
            case "kick" -> ActionType.KICK;
            case "mute" -> ActionType.MUTE;
            case "tempmute", "temporarymute" -> ActionType.TEMP_MUTE;
            case "unmute" -> ActionType.UNMUTE;
            case "warn", "warning" -> ActionType.WARN;
            default -> null;
        };
    }
}
