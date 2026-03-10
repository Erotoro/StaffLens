package dev.stafflens.integrations.impl;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.integrations.BaseIntegration;
import dev.stafflens.model.ActionType;
import dev.stafflens.model.AuditEntry;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public class VanillaIntegration extends BaseIntegration {

    private final Map<String, Boolean> flyStates = new ConcurrentHashMap<>();
    private final Map<String, Boolean> vanishStates = new ConcurrentHashMap<>();
    private final Map<String, Boolean> godStates = new ConcurrentHashMap<>();
    private final Map<String, Boolean> socialSpyStates = new ConcurrentHashMap<>();

    public VanillaIntegration(StaffLensPlugin plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGamemode(PlayerGameModeChangeEvent event) {
        if (event.getPlayer().hasPermission("stafflens.admin")) { 
            auditService.log(new AuditEntry(
                    event.getPlayer().getUniqueId(),
                    event.getPlayer().getName(),
                    ActionType.GAMEMODE_CHANGE,
                    event.getPlayer().getName(),
                    "Self Change",
                    "To " + event.getNewGameMode().name(),
                    System.currentTimeMillis()
            ));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!event.getPlayer().hasPermission("stafflens.admin")) {
            return;
        }
        logCommand(event.getPlayer(), event.getPlayer().getName(), event.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsoleCommand(ServerCommandEvent event) {
        logCommand(event.getSender(), "Console", event.getCommand());
    }

    private void logCommand(CommandSender sender, String actorName, String rawCommand) {
        ParsedCommand parsed = parseCommand(rawCommand, actorName);
        if (parsed == null) {
            return;
        }

        auditService.log(sender, actorName, parsed.type(), parsed.target(), "Command", parsed.details());
    }

    private ParsedCommand parseCommand(String rawCommand, String actorName) {
        String msg = rawCommand.startsWith("/") ? rawCommand.substring(1).trim() : rawCommand.trim();
        if (msg.isEmpty()) {
            return null;
        }

        String[] parts = normalizeParts(msg.split("\\s+"));
        if (parts.length == 0 || parts[0].isEmpty()) {
            return null;
        }

        ActionType type = null;
        String target = actorName;

        if (isAny(parts[0], "op", "minecraft:op") && parts.length >= 2) {
            type = ActionType.OP_GIVE;
            target = parts[1];
        } else if (isAny(parts[0], "deop", "minecraft:deop") && parts.length >= 2) {
            type = ActionType.OP_REMOVE;
            target = parts[1];
        } else if (isAny(parts[0], "ban", "minecraft:ban") && parts.length >= 2) {
            type = ActionType.BAN;
            target = parts[1];
        } else if (isAny(parts[0], "tempban") && parts.length >= 2) {
            type = ActionType.TEMP_BAN;
            target = parts[1];
        } else if (isAny(parts[0], "pardon", "unban", "minecraft:pardon") && parts.length >= 2) {
            type = ActionType.UNBAN;
            target = parts[1];
        } else if (isAny(parts[0], "kick", "minecraft:kick") && parts.length >= 2) {
            type = ActionType.KICK;
            target = parts[1];
        } else if (isAny(parts[0], "smite") && parts.length >= 2) {
            type = ActionType.SMITE;
            target = parts[1];
        } else if (isAny(parts[0], "mute", "silence") && parts.length >= 2) {
            type = ActionType.MUTE;
            target = parts[1];
        } else if (isAny(parts[0], "tempmute") && parts.length >= 2) {
            type = ActionType.TEMP_MUTE;
            target = parts[1];
        } else if (isAny(parts[0], "unmute") && parts.length >= 2) {
            type = ActionType.UNMUTE;
            target = parts[1];
        } else if (isAny(parts[0], "warn") && parts.length >= 2) {
            type = ActionType.WARN;
            target = parts[1];
        } else if (isAny(parts[0], "tp", "teleport", "minecraft:tp", "minecraft:teleport") && parts.length >= 2) {
            type = ActionType.TELEPORT;
            target = parts[parts.length - 1];
        } else if (isAny(parts[0], "tppos") && parts.length >= 4) {
            type = ActionType.TELEPORT_POSITION;
            target = parseLastPlayerArgument(parts, actorName);
        } else if (isAny(parts[0], "tphere") && parts.length >= 2) {
            type = ActionType.TELEPORT_HERE;
            target = parts[1];
        } else if (isAny(parts[0], "tpall")) {
            type = ActionType.TELEPORT_ALL;
            target = parts.length >= 2 ? parts[1] : "all";
        } else if (isAny(parts[0], "fly")) {
            boolean enable = resolveToggle(parts, actorName, flyStates);
            type = enable ? ActionType.FLY_ON : ActionType.FLY_OFF;
            target = parseOptionalTarget(parts, actorName);
        } else if (isAny(parts[0], "invsee", "inv")) {
            type = ActionType.INVENTORY_VIEW;
            target = parseOptionalTarget(parts, actorName);
        } else if (isAny(parts[0], "ec", "enderchest")) {
            type = ActionType.ENDERCHEST_VIEW;
            target = parseOptionalTarget(parts, actorName);
        } else if (isAny(parts[0], "god")) {
            boolean enable = resolveToggle(parts, actorName, godStates);
            type = enable ? ActionType.GOD_ON : ActionType.GOD_OFF;
            target = parseOptionalTarget(parts, actorName);
        } else if (isAny(parts[0], "give", "minecraft:give", "itemgive", "i") && parts.length >= 2) {
            type = ActionType.GIVE_ITEM;
            target = parts[1];
        } else if (isAny(parts[0], "clear", "minecraft:clear")) {
            type = ActionType.CLEAR_INVENTORY;
            target = parseOptionalTarget(parts, actorName);
        } else if (isAny(parts[0], "repair", "fix")) {
            type = ActionType.REPAIR;
            target = parseOptionalTarget(parts, actorName);
        } else if (isAny(parts[0], "heal")) {
            type = ActionType.HEAL;
            target = parseOptionalTarget(parts, actorName);
        } else if (isAny(parts[0], "feed")) {
            type = ActionType.FEED;
            target = parseOptionalTarget(parts, actorName);
        } else if (isAny(parts[0], "vanish", "v")) {
            boolean enable = resolveToggle(parts, actorName, vanishStates);
            type = enable ? ActionType.VANISH_ON : ActionType.VANISH_OFF;
            target = parseOptionalTarget(parts, actorName);
        } else if (isAny(parts[0], "back")) {
            type = ActionType.BACK;
        } else if (isAny(parts[0], "broadcast", "bc")) {
            type = ActionType.BROADCAST;
            target = "server";
        } else if (isAny(parts[0], "sudo") && parts.length >= 2) {
            type = ActionType.SUDO;
            target = parts[1];
        } else if (isAny(parts[0], "nick", "nickname")) {
            type = ActionType.NICK_CHANGE;
            target = parseNickTarget(parts, actorName);
        } else if (isAny(parts[0], "jail") && parts.length >= 2) {
            type = ActionType.JAIL;
            target = parts[1];
        } else if (isAny(parts[0], "unjail") && parts.length >= 2) {
            type = ActionType.UNJAIL;
            target = parts[1];
        } else if (isAny(parts[0], "socialspy", "sspy")) {
            boolean enable = resolveToggle(parts, actorName, socialSpyStates);
            type = enable ? ActionType.SOCIALSPY_ON : ActionType.SOCIALSPY_OFF;
        } else if (isAny(parts[0], "workbench", "wb", "craft")) {
            type = ActionType.WORKBENCH_OPEN;
        } else if (isAny(parts[0], "anvil")) {
            type = ActionType.ANVIL_OPEN;
        } else if (isAny(parts[0], "smithingtable", "smithing")) {
            type = ActionType.SMITHINGTABLE_OPEN;
        } else if (isAny(parts[0], "enchant", "enchat")) {
            type = ActionType.ENCHANT_ITEM;
            target = parseOptionalTarget(parts, actorName);
        } else if (isAny(parts[0], "seen") && parts.length >= 2) {
            type = ActionType.SEEN_LOOKUP;
            target = parts[1];
        } else if (isAny(parts[0], "history") && parts.length >= 2) {
            type = ActionType.HISTORY_LOOKUP;
            target = parts[1];
        } else if (isAny(parts[0], "lp", "luckperms")) {
            type = parseLuckPermsAction(parts);
            target = parts.length >= 3 ? parts[2] : actorName;
        } else if (isAny(parts[0], "gm", "gamemode", "minecraft:gamemode", "gmc", "gms", "gma", "gmsp")) {
            type = ActionType.GAMEMODE_CHANGE;
            target = parseGamemodeTarget(parts, actorName);
        }

        if (type == null) {
            return null;
        }

        return new ParsedCommand(type, target, msg);
    }

    private ActionType parseLuckPermsAction(String[] parts) {
        if (parts.length < 2) {
            return ActionType.LUCKPERMS_COMMAND;
        }

        if (parts.length >= 5 && isAny(parts[1], "user", "group")) {
            String subCommand = parts[3].toLowerCase(Locale.ROOT);
            if (subCommand.equals("parent") && parts.length >= 6) {
                String action = parts[4].toLowerCase(Locale.ROOT);
                if (action.equals("add") || action.equals("set")) {
                    return ActionType.GROUP_ADD;
                }
                if (action.equals("remove")) {
                    return ActionType.GROUP_REMOVE;
                }
            }

            if (subCommand.equals("permission") && parts.length >= 6) {
                String action = parts[4].toLowerCase(Locale.ROOT);
                if (action.equals("set") || action.equals("add")) {
                    return ActionType.PERMISSION_ADD;
                }
                if (action.equals("unset") || action.equals("remove")) {
                    return ActionType.PERMISSION_REMOVE;
                }
            }
        }

        return ActionType.LUCKPERMS_COMMAND;
    }

    private String[] normalizeParts(String[] originalParts) {
        if (originalParts.length >= 2 && isAny(originalParts[0], "cmi", "essentials", "ess", "e")) {
            String[] normalized = new String[originalParts.length - 1];
            System.arraycopy(originalParts, 1, normalized, 0, normalized.length);
            return normalized;
        }
        return originalParts;
    }

    private String parseGamemodeTarget(String[] parts, String actorName) {
        if (parts.length == 0) {
            return actorName;
        }

        if (isAny(parts[0], "gmc", "gms", "gma", "gmsp")) {
            return parts.length >= 2 ? parts[1] : actorName;
        }

        return parts.length >= 3 ? parts[2] : actorName;
    }

    private String parseOptionalTarget(String[] parts, String actorName) {
        if (parts.length < 2) {
            return actorName;
        }
        if (isTruthy(parts[1]) || isFalsy(parts[1])) {
            return parts.length >= 3 ? parts[2] : actorName;
        }
        return parts[1];
    }

    private String parseLastPlayerArgument(String[] parts, String actorName) {
        if (parts.length < 5) {
            return actorName;
        }
        return parts[parts.length - 1];
    }

    private String parseNickTarget(String[] parts, String actorName) {
        return parts.length >= 3 ? parts[1] : actorName;
    }

    private boolean resolveToggle(String[] parts, String actorName, Map<String, Boolean> stateMap) {
        String key = actorName.toLowerCase(Locale.ROOT);
        if (parts.length >= 2) {
            if (isTruthy(parts[1])) {
                stateMap.put(key, true);
                return true;
            }
            if (isFalsy(parts[1])) {
                stateMap.put(key, false);
                return false;
            }
        }

        boolean nextState = !stateMap.getOrDefault(key, false);
        stateMap.put(key, nextState);
        return nextState;
    }

    private boolean isAny(String value, String... options) {
        String lowered = value.toLowerCase(Locale.ROOT);
        for (String option : options) {
            if (lowered.equals(option)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTruthy(String value) {
        return isAny(value, "on", "enable", "enabled", "true", "1");
    }

    private boolean isFalsy(String value) {
        return isAny(value, "off", "disable", "disabled", "false", "0");
    }

    private record ParsedCommand(ActionType type, String target, String details) {
    }
}
