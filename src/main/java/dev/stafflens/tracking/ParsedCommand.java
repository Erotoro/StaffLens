package dev.stafflens.tracking;

import dev.stafflens.model.ActionType;

/** A command classified into an auditable action, its target, and the raw command text. */
public record ParsedCommand(ActionType type, String target, String details) {
}
