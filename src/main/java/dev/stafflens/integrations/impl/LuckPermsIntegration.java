package dev.stafflens.integrations.impl;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.integrations.BaseIntegration;
import dev.stafflens.model.ActionType;
import dev.stafflens.model.AuditEntry;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class LuckPermsIntegration extends BaseIntegration {

    private EventSubscription<NodeAddEvent> nodeAddSubscription;
    private EventSubscription<NodeRemoveEvent> nodeRemoveSubscription;

    public LuckPermsIntegration(StaffLensPlugin plugin) {
        super(plugin);
    }

    @Override
    public void register() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            LuckPerms api = provider.getProvider();
            EventBus bus = api.getEventBus();

            nodeAddSubscription = bus.subscribe(this.plugin, NodeAddEvent.class, this::onNodeAdd);
            nodeRemoveSubscription = bus.subscribe(this.plugin, NodeRemoveEvent.class, this::onNodeRemove);
        }
    }

    @Override
    public void unregister() {
        if (nodeAddSubscription != null) {
            nodeAddSubscription.close();
            nodeAddSubscription = null;
        }
        if (nodeRemoveSubscription != null) {
            nodeRemoveSubscription.close();
            nodeRemoveSubscription = null;
        }
        super.unregister();
    }

    private void onNodeAdd(NodeAddEvent event) {
        if (event.isUser()) {
            User target = (User) event.getTarget();
            String nodeKey = event.getNode().getKey();
            auditService.log(new AuditEntry(
                    null,
                    "Unknown Actor",
                    classifyNodeAdd(nodeKey),
                    target.getUsername() != null ? target.getUsername() : target.getUniqueId().toString(),
                    "LP Add (actor unavailable from event)",
                    nodeKey,
                    System.currentTimeMillis()
            ));
        }
    }

    private void onNodeRemove(NodeRemoveEvent event) {
        if (event.isUser()) {
            User target = (User) event.getTarget();
            String nodeKey = event.getNode().getKey();
            auditService.log(new AuditEntry(
                    null,
                    "Unknown Actor",
                    classifyNodeRemove(nodeKey),
                    target.getUsername() != null ? target.getUsername() : target.getUniqueId().toString(),
                    "LP Remove (actor unavailable from event)",
                    nodeKey,
                    System.currentTimeMillis()
            ));
        }
    }

    private ActionType classifyNodeAdd(String nodeKey) {
        return nodeKey.startsWith("group.") ? ActionType.GROUP_ADD : ActionType.PERMISSION_ADD;
    }

    private ActionType classifyNodeRemove(String nodeKey) {
        return nodeKey.startsWith("group.") ? ActionType.GROUP_REMOVE : ActionType.PERMISSION_REMOVE;
    }
}
