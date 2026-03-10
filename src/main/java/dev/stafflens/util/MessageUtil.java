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
        Component hover = entry.details() != null && !entry.details().isBlank()
                ? Component.text("Full command/details:\n" + entry.details())
                : Component.text("No extra details.");
        return parse(line).hoverEvent(HoverEvent.showText(hover));
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
