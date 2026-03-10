package dev.stafflens.model;

import java.util.UUID;

public record AuditEntry(
        UUID staffUuid,
        String staffName,
        ActionType action,
        String targetName,
        String reason,
        String details,
        long timestamp
) {
}
