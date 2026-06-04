package dev.stafflens.model;

import java.util.UUID;

/**
 * A single staff action. The last seven components (server, location, ip, flagged) were added later;
 * the short constructor below keeps the original shape working and defaults them.
 */
public record AuditEntry(
        UUID staffUuid,
        String staffName,
        ActionType action,
        String targetName,
        String reason,
        String details,
        long timestamp,
        String serverName,
        String world,
        Integer x,
        Integer y,
        Integer z,
        String ip,
        boolean flagged
) {

    public AuditEntry(UUID staffUuid, String staffName, ActionType action, String targetName,
                      String reason, String details, long timestamp) {
        this(staffUuid, staffName, action, targetName, reason, details, timestamp,
                null, null, null, null, null, null, false);
    }

    /** Returns a copy with the given context, keeping any value already present. */
    public AuditEntry withContext(String serverName, String world, Integer x, Integer y, Integer z, String ip) {
        return new AuditEntry(
                staffUuid, staffName, action, targetName, reason, details, timestamp,
                this.serverName != null ? this.serverName : serverName,
                this.world != null ? this.world : world,
                this.x != null ? this.x : x,
                this.y != null ? this.y : y,
                this.z != null ? this.z : z,
                this.ip != null ? this.ip : ip,
                flagged
        );
    }

    public AuditEntry withFlagged(boolean flagged) {
        if (this.flagged == flagged) {
            return this;
        }
        return new AuditEntry(staffUuid, staffName, action, targetName, reason, details, timestamp,
                serverName, world, x, y, z, ip, flagged);
    }

    public boolean hasLocation() {
        return world != null && x != null && y != null && z != null;
    }
}
