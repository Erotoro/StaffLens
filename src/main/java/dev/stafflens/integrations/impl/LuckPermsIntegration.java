package dev.stafflens.integrations.impl;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.integrations.BaseIntegration;
import dev.stafflens.model.ActionType;
import dev.stafflens.model.AuditEntry;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.log.LogPublishEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Locale;
import java.util.UUID;

/**
 * Records LuckPerms permission/group changes. Uses the action log (not the raw node events) because
 * it carries the executor as well as the target, so every change is attributed to the right staff.
 */
public class LuckPermsIntegration extends BaseIntegration {

    private static final UUID CONSOLE_UUID = new UUID(0L, 0L);

    private EventSubscription<LogPublishEvent> logSubscription;

    public LuckPermsIntegration(StaffLensPlugin plugin) {
        super(plugin);
    }

    @Override
    public void register() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            EventBus bus = provider.getProvider().getEventBus();
            logSubscription = bus.subscribe(this.plugin, LogPublishEvent.class, this::onLogPublish);
        }
    }

    @Override
    public void unregister() {
        if (logSubscription != null) {
            logSubscription.close();
            logSubscription = null;
        }
        super.unregister();
    }

    private void onLogPublish(LogPublishEvent event) {
        Action action = event.getEntry();
        Action.Source source = action.getSource();

        UUID sourceUuid = source.getUniqueId();
        String sourceName = source.getName();
        if (sourceUuid == null || CONSOLE_UUID.equals(sourceUuid)) {
            sourceUuid = null;
            sourceName = "Console";
        }

        String description = action.getDescription();
        auditService.log(new AuditEntry(
                sourceUuid,
                sourceName,
                classify(description),
                action.getTarget().getName(),
                "LuckPerms",
                description,
                action.getTimestamp().toEpochMilli()
        ));
    }

    private ActionType classify(String description) {
        String d = description == null ? "" : description.toLowerCase(Locale.ROOT);
        if (d.startsWith("parent add") || d.startsWith("parent set")) {
            return ActionType.GROUP_ADD;
        }
        if (d.startsWith("parent remove")) {
            return ActionType.GROUP_REMOVE;
        }
        if (d.startsWith("permission set")) {
            return ActionType.PERMISSION_ADD;
        }
        if (d.startsWith("permission unset")) {
            return ActionType.PERMISSION_REMOVE;
        }
        return ActionType.LUCKPERMS_COMMAND;
    }
}
