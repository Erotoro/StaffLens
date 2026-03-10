package dev.stafflens.integrations.impl;

import com.Zrips.CMI.events.CMIPlayerBanEvent;
import com.Zrips.CMI.events.CMIPlayerKickEvent;
import com.Zrips.CMI.events.CMIPlayerMuteEvent;
import dev.stafflens.StaffLensPlugin;
import dev.stafflens.integrations.BaseIntegration;
import dev.stafflens.model.ActionType;
import dev.stafflens.model.AuditEntry;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.UUID;

public class CMIIntegration extends BaseIntegration {
    public CMIIntegration(StaffLensPlugin plugin) { super(plugin); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBan(CMIPlayerBanEvent e) {
        if (e.getBannedBy() == null || e.getBannedPlayer() == null) {
            return;
        }
        auditService.log(new AuditEntry(convertToUUID(e.getBannedBy()), e.getBannedBy().getName(), ActionType.BAN, e.getBannedPlayer().getName(), e.getReason(), "", System.currentTimeMillis()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKick(CMIPlayerKickEvent e) {
        if (e.getKickedBy() == null || e.getKickedPlayer() == null) {
            return;
        }
        auditService.log(new AuditEntry(convertToUUID(e.getKickedBy()), e.getKickedBy().getName(), ActionType.KICK, e.getKickedPlayer().getName(), e.getReason(), "", System.currentTimeMillis()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMute(CMIPlayerMuteEvent e) {
        if (e.getMutedBy() == null || e.getMutedPlayer() == null) {
            return;
        }
        auditService.log(new AuditEntry(convertToUUID(e.getMutedBy()), e.getMutedBy().getName(), ActionType.MUTE, e.getMutedPlayer().getName(), e.getReason(), "", System.currentTimeMillis()));
    }

    private UUID convertToUUID(CommandSender source) {
        if (source instanceof org.bukkit.entity.Player player) {
            return player.getUniqueId();
        }
        return null;
    }
}
