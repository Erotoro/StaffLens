package dev.stafflens.model;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public enum ActionType {
    BAN("Ban"),
    TEMP_BAN("Temp Ban"),
    UNBAN("Unban"),
    MUTE("Mute"),
    TEMP_MUTE("Temp Mute"),
    UNMUTE("Unmute"),
    KICK("Kick"),
    SMITE("Smite"),
    WARN("Warn"),
    OP_GIVE("Grant OP"),
    OP_REMOVE("Remove OP"),
    PERMISSION_ADD("Add Permission"),
    PERMISSION_REMOVE("Remove Permission"),
    GROUP_ADD("Add Group"),
    GROUP_REMOVE("Remove Group"),
    TELEPORT("Teleport"),
    TELEPORT_POSITION("Teleport To Position"),
    TELEPORT_HERE("Teleport Here"),
    TELEPORT_ALL("Teleport All"),
    FLY_ON("Enable Fly"),
    FLY_OFF("Disable Fly"),
    INVENTORY_VIEW("View Inventory"),
    ENDERCHEST_VIEW("View Ender Chest"),
    GOD_ON("Enable God Mode"),
    GOD_OFF("Disable God Mode"),
    GIVE_ITEM("Give Item"),
    CLEAR_INVENTORY("Clear Inventory"),
    REPAIR("Repair"),
    HEAL("Heal"),
    FEED("Feed"),
    VANISH_ON("Enable Vanish"),
    VANISH_OFF("Disable Vanish"),
    BACK("Back Teleport"),
    BROADCAST("Broadcast"),
    SUDO("Force Command"),
    NICK_CHANGE("Change Nickname"),
    JAIL("Jail"),
    UNJAIL("Unjail"),
    SOCIALSPY_ON("Enable SocialSpy"),
    SOCIALSPY_OFF("Disable SocialSpy"),
    WORKBENCH_OPEN("Open Workbench"),
    ANVIL_OPEN("Open Anvil"),
    SMITHINGTABLE_OPEN("Open Smithing Table"),
    ENCHANT_ITEM("Enchant Item"),
    SEEN_LOOKUP("Seen Lookup"),
    HISTORY_LOOKUP("History Lookup"),
    LUCKPERMS_COMMAND("LuckPerms Command"),
    GAMEMODE_CHANGE("Change Gamemode");

    private final String displayName;

    ActionType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static ActionType fromSerialized(String value) {
        return ActionType.valueOf(value.toUpperCase(Locale.ROOT));
    }

    public static ActionType fromConfig(String value) {
        return ActionType.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
    }

    public static Set<ActionType> parseConfigList(Collection<String> values) {
        Set<ActionType> result = EnumSet.noneOf(ActionType.class);
        for (String value : values) {
            try {
                result.add(fromConfig(value));
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid config entries rather than breaking plugin startup.
            }
        }
        return result;
    }
}
