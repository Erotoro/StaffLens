package dev.stafflens.integrations.impl;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.integrations.BaseIntegration;
import dev.stafflens.model.ActionType;
import dev.stafflens.model.AuditEntry;
import litebans.api.Entry;
import litebans.api.Events;

import java.util.UUID;

public class LiteBansIntegration extends BaseIntegration {

    private Events.Listener listener;

    public LiteBansIntegration(StaffLensPlugin plugin) {
        super(plugin);
    }

    @Override
    public void register() {
        listener = new Events.Listener() {
            @Override
            public void entryAdded(Entry entry) {
                if (entry == null || entry.getType() == null) {
                    return;
                }
                ActionType type = switch (entry.getType().toLowerCase()) {
                    case "ban" -> ActionType.BAN;
                    case "tempban" -> ActionType.TEMP_BAN;
                    case "unban" -> ActionType.UNBAN;
                    case "mute" -> ActionType.MUTE;
                    case "tempmute" -> ActionType.TEMP_MUTE;
                    case "unmute" -> ActionType.UNMUTE;
                    case "kick" -> ActionType.KICK;
                    case "warn" -> ActionType.WARN;
                    default -> null;
                };

                if (type != null) {
                    UUID staffUuid = parseUuid(entry.getExecutorUUID());
                    auditService.log(new AuditEntry(
                            staffUuid,
                            entry.getExecutorName() != null && !entry.getExecutorName().isBlank() ? entry.getExecutorName() : "Console",
                            type,
                            entry.getUuid() != null ? entry.getUuid() : "IP/Unknown",
                            entry.getReason(),
                            "Duration: " + entry.getDurationString(),
                            System.currentTimeMillis()
                    ));
                }
            }
        };
        Events.get().register(listener);
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
