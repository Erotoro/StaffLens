package dev.stafflens.integrations.impl;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.integrations.BaseIntegration;
import dev.stafflens.model.ActionType;
import dev.stafflens.model.AuditEntry;
import me.libraryaddict.disguise.events.DisguiseEvent;
import me.libraryaddict.disguise.events.UndisguiseEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.UUID;

/** LibsDisguises: records when an entity is disguised or undisguised, and by whom. */
public class LibsDisguisesIntegration extends BaseIntegration {

    public LibsDisguisesIntegration(StaffLensPlugin plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDisguise(DisguiseEvent event) {
        log(ActionType.DISGUISE, event.getCommandSender(), event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUndisguise(UndisguiseEvent event) {
        log(ActionType.UNDISGUISE, event.getCommandSender(), event.getEntity());
    }

    private void log(ActionType type, CommandSender sender, Entity target) {
        UUID actorUuid = sender instanceof Player player ? player.getUniqueId() : null;
        String actorName = sender != null ? sender.getName() : "Console";
        String targetName = target != null ? target.getName() : "Unknown";
        auditService.log(new AuditEntry(actorUuid, actorName, type, targetName, "Disguise", "",
                System.currentTimeMillis()));
    }
}
