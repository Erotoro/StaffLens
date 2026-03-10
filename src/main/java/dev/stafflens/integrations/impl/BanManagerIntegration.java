package dev.stafflens.integrations.impl;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.integrations.BaseIntegration;
import dev.stafflens.model.ActionType;
import dev.stafflens.model.AuditEntry;
import me.confuser.banmanager.prebukkitevents.PlayerBannedEvent;
import me.confuser.banmanager.prebukkitevents.PlayerKickedEvent;
import me.confuser.banmanager.prebukkitevents.PlayerMutedEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.UUID;

public class BanManagerIntegration extends BaseIntegration {
    public BanManagerIntegration(StaffLensPlugin plugin) { super(plugin); }

    private UUID getActorUUID(CommandSender actor) {
        if (actor instanceof Player) {
            return ((Player) actor).getUniqueId();
        }
        return null; // Console or non-player
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBan(PlayerBannedEvent e) {
        if (e.getActor() == null || e.getTarget() == null) {
            return;
        }
        auditService.log(new AuditEntry(getActorUUID(e.getActor()), e.getActor().getName(), ActionType.BAN, e.getTarget().getName(), e.getReason(), "", System.currentTimeMillis()));
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKick(PlayerKickedEvent e) {
        if (e.getActor() == null || e.getTarget() == null) {
            return;
        }
        auditService.log(new AuditEntry(getActorUUID(e.getActor()), e.getActor().getName(), ActionType.KICK, e.getTarget().getName(), e.getReason(), "", System.currentTimeMillis()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMute(PlayerMutedEvent e) {
        if (e.getActor() == null || e.getTarget() == null) {
            return;
        }
        auditService.log(new AuditEntry(getActorUUID(e.getActor()), e.getActor().getName(), ActionType.MUTE, e.getTarget().getName(), e.getReason(), "", System.currentTimeMillis()));
    }
}
