package dev.stafflens.util;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.model.AuditEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

public class MessageUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void sendMessage(CommandSender sender, StaffLensPlugin plugin, String key, String... placeholders) {
        String raw = plugin.getConfigManager().getMessages().getString(key, "<red>Missing key: " + key);

        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                raw = raw.replace(placeholders[i], placeholders[i + 1]);
            }
        }

        sender.sendMessage(MM.deserialize(raw));
    }

    public static Component parse(String text) {
        return MM.deserialize(text);
    }

    public static Component auditEntry(AuditEntry entry, String line) {
        StringBuilder hover = new StringBuilder();
        if (entry.details() != null && !entry.details().isBlank()) {
            hover.append("Details: ").append(entry.details());
        } else {
            hover.append("No extra details.");
        }
        if (entry.hasLocation()) {
            hover.append("\nLocation: ").append(entry.world())
                    .append(" ").append(entry.x()).append(", ").append(entry.y()).append(", ").append(entry.z());
        }
        if (entry.serverName() != null && !entry.serverName().isBlank()) {
            hover.append("\nServer: ").append(entry.serverName());
        }
        if (entry.ip() != null && !entry.ip().isBlank()) {
            hover.append("\nIP: ").append(entry.ip());
        }
        if (entry.flagged()) {
            hover.append("\n⚠ Flagged as anomalous");
        }
        return parse(line).hoverEvent(HoverEvent.showText(Component.text(hover.toString())));
    }

    /** Prefixes flagged entries with a warning marker for at-a-glance scanning. */
    public static String flagPrefix(AuditEntry entry) {
        return entry.flagged() ? "<red>⚠ </red>" : "";
    }

    public static Component pageControls(String baseCommand, int page, int totalPages) {
        Component previous = page > 1
                ? parse("<yellow>Previous</yellow>").clickEvent(ClickEvent.runCommand(baseCommand + " " + (page - 1)))
                .hoverEvent(HoverEvent.showText(parse("<gray>Go to page <white>" + (page - 1))))
                : parse("<dark_gray>Previous</dark_gray>");

        Component next = page < totalPages
                ? parse("<yellow>Next</yellow>").clickEvent(ClickEvent.runCommand(baseCommand + " " + (page + 1)))
                .hoverEvent(HoverEvent.showText(parse("<gray>Go to page <white>" + (page + 1))))
                : parse("<dark_gray>Next</dark_gray>");

        return Component.empty()
                .append(previous)
                .append(parse(" <gray>| Page <white>" + page + "<gray>/<white>" + totalPages + " <gray>| "))
                .append(next);
    }

    public static String escapeMiniMessage(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("<", "\\<")
                .replace(">", "\\>");
    }
}
