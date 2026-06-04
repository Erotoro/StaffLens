package dev.stafflens.model;

import java.util.UUID;

/**
 * Optional filter for audit queries. Null fields are ignored, so one query method can serve the
 * staff / target / search / today / flagged views as well as the {@code key:value} command filters.
 */
public final class AuditFilter {

    private UUID staffUuid;
    private boolean staffUuidSet;
    private String staffName;
    private String targetName;
    private ActionType action;
    private Long sinceMillis;
    private String text;
    private boolean flaggedOnly;

    public AuditFilter staffUuid(UUID uuid) {
        this.staffUuid = uuid;
        this.staffUuidSet = true;
        return this;
    }

    public AuditFilter staffName(String name) {
        this.staffName = name;
        return this;
    }

    public AuditFilter targetName(String name) {
        this.targetName = name;
        return this;
    }

    public AuditFilter action(ActionType action) {
        this.action = action;
        return this;
    }

    public AuditFilter since(Long sinceMillis) {
        this.sinceMillis = sinceMillis;
        return this;
    }

    public AuditFilter text(String text) {
        this.text = text;
        return this;
    }

    public AuditFilter flaggedOnly(boolean flaggedOnly) {
        this.flaggedOnly = flaggedOnly;
        return this;
    }

    public UUID staffUuid() {
        return staffUuid;
    }

    public boolean staffUuidSet() {
        return staffUuidSet;
    }

    public String staffName() {
        return staffName;
    }

    public String targetName() {
        return targetName;
    }

    public ActionType action() {
        return action;
    }

    public Long sinceMillis() {
        return sinceMillis;
    }

    public String text() {
        return text;
    }

    public boolean flaggedOnly() {
        return flaggedOnly;
    }
}
