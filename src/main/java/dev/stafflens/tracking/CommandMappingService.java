package dev.stafflens.tracking;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.model.ActionType;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Config-driven command mappings, so owners can audit commands from any plugin without code changes.
 * Consulted only as a fallback after the built-in parser, so existing behaviour is untouched.
 *
 * <pre>
 * tracking:
 *   command-tracking:
 *     freeze: { action: JAIL, target-arg: 1 }
 * </pre>
 */
public class CommandMappingService {

    // targetArg is the 1-based index of the argument naming the target (0 = the actor).
    private record Mapping(ActionType action, int targetArg) {
    }

    private final Map<String, Mapping> mappings = new HashMap<>();

    public CommandMappingService(StaffLensPlugin plugin) {
        load(plugin);
    }

    private void load(StaffLensPlugin plugin) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("tracking.command-tracking");
        if (section == null) {
            return;
        }
        for (String rawKey : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(rawKey);
            if (entry == null) {
                continue;
            }
            String actionName = entry.getString("action");
            if (actionName == null) {
                continue;
            }
            try {
                ActionType action = ActionType.fromConfig(actionName);
                int targetArg = Math.max(0, entry.getInt("target-arg", 0));
                mappings.put(normalizeLabel(rawKey), new Mapping(action, targetArg));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Ignoring command-tracking entry '" + rawKey
                        + "': unknown action '" + actionName + "'.");
            }
        }
        if (!mappings.isEmpty()) {
            plugin.getLogger().info("Loaded " + mappings.size() + " custom command mapping(s).");
        }
    }

    public boolean isEmpty() {
        return mappings.isEmpty();
    }

    /** Classifies a command via the configured mappings, or returns {@code null} if none match. */
    public ParsedCommand match(String[] parts, String rawMessage, String actorName) {
        if (parts.length == 0 || parts[0].isEmpty()) {
            return null;
        }
        Mapping mapping = mappings.get(parts[0].toLowerCase(Locale.ROOT));
        if (mapping == null) {
            return null;
        }
        String target = actorName;
        if (mapping.targetArg() > 0 && parts.length > mapping.targetArg()) {
            target = parts[mapping.targetArg()];
        }
        return new ParsedCommand(mapping.action(), target, rawMessage);
    }

    private static String normalizeLabel(String key) {
        String label = key.trim().toLowerCase(Locale.ROOT);
        if (label.startsWith("/")) {
            label = label.substring(1);
        }
        int space = label.indexOf(' ');
        if (space > 0) {
            label = label.substring(0, space);
        }
        return label;
    }
}
