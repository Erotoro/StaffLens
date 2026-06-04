package dev.stafflens.anomaly;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.model.ActionType;
import dev.stafflens.model.AuditEntry;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * In-memory behavioural checks over the audit stream. Rules are configurable and run against sliding
 * windows; {@link #inspect} is synchronized because staff actions arrive from several event threads.
 * A reload simply builds a new detector, which also clears the windows.
 */
public class AnomalyDetector {

    private static final Set<ActionType> PUNISH_ACTIONS = EnumSet.of(
            ActionType.BAN, ActionType.TEMP_BAN, ActionType.KICK,
            ActionType.MUTE, ActionType.TEMP_MUTE, ActionType.WARN, ActionType.SMITE);
    private static final Set<ActionType> INSPECT_ACTIONS = EnumSet.of(
            ActionType.INVENTORY_VIEW, ActionType.ENDERCHEST_VIEW);
    private static final Set<ActionType> GIVE_ACTIONS = EnumSet.of(
            ActionType.GIVE_ITEM, ActionType.CLEAR_INVENTORY);
    private static final long CREATIVE_GIVE_WINDOW_MILLIS = 30_000L;

    private final boolean enabled;
    private final Set<ActionType> selfTargetActions;
    private final boolean massPunishEnabled;
    private final int massPunishThreshold;
    private final long massPunishWindowMillis;
    private final boolean repeatedInspectEnabled;
    private final int repeatedInspectThreshold;
    private final long repeatedInspectWindowMillis;
    private final boolean creativeThenGive;

    private final Map<String, Deque<Long>> punishWindows = new HashMap<>();
    private final Map<String, Deque<Long>> inspectWindows = new HashMap<>();
    private final Map<String, Long> creativeSelfAt = new HashMap<>();

    public AnomalyDetector(StaffLensPlugin plugin) {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("anomaly");
        if (root == null) {
            this.enabled = false;
            this.selfTargetActions = EnumSet.noneOf(ActionType.class);
            this.massPunishEnabled = false;
            this.massPunishThreshold = 0;
            this.massPunishWindowMillis = 0;
            this.repeatedInspectEnabled = false;
            this.repeatedInspectThreshold = 0;
            this.repeatedInspectWindowMillis = 0;
            this.creativeThenGive = false;
            return;
        }

        this.enabled = root.getBoolean("enabled", true);
        this.selfTargetActions = ActionType.parseConfigList(root.getStringList("self-target-actions"));

        ConfigurationSection mass = root.getConfigurationSection("mass-punish");
        this.massPunishEnabled = mass != null && mass.getBoolean("enabled", true);
        this.massPunishThreshold = mass != null ? Math.max(2, mass.getInt("threshold", 5)) : 5;
        this.massPunishWindowMillis = (mass != null ? Math.max(1, mass.getInt("window-seconds", 60)) : 60) * 1000L;

        ConfigurationSection inspect = root.getConfigurationSection("repeated-invsee");
        this.repeatedInspectEnabled = inspect != null && inspect.getBoolean("enabled", true);
        this.repeatedInspectThreshold = inspect != null ? Math.max(2, inspect.getInt("threshold", 4)) : 4;
        this.repeatedInspectWindowMillis = (inspect != null ? Math.max(1, inspect.getInt("window-seconds", 120)) : 120) * 1000L;

        this.creativeThenGive = root.getBoolean("creative-then-give", true);
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Inspects an entry, updates the sliding windows, and returns any matched rules. */
    public synchronized AnomalyResult inspect(AuditEntry entry) {
        if (!enabled) {
            return AnomalyResult.clean();
        }

        List<String> reasons = new ArrayList<>();
        String staffKey = staffKey(entry);
        long now = entry.timestamp();

        if (isSelfTarget(entry) && selfTargetActions.contains(entry.action())) {
            reasons.add("Self-targeted " + entry.action().displayName());
        }

        if (massPunishEnabled && PUNISH_ACTIONS.contains(entry.action())) {
            int count = recordAndCount(punishWindows, staffKey, now, massPunishWindowMillis);
            if (count >= massPunishThreshold) {
                reasons.add(count + " punishments in " + (massPunishWindowMillis / 1000) + "s");
            }
        }

        if (repeatedInspectEnabled && INSPECT_ACTIONS.contains(entry.action())) {
            String key = staffKey + "->" + lower(entry.targetName());
            int count = recordAndCount(inspectWindows, key, now, repeatedInspectWindowMillis);
            if (count >= repeatedInspectThreshold) {
                reasons.add(count + " inventory checks of " + entry.targetName());
            }
        }

        if (creativeThenGive) {
            if (entry.action() == ActionType.GAMEMODE_CHANGE && isSelfTarget(entry)
                    && entry.details() != null && entry.details().toUpperCase(Locale.ROOT).contains("CREATIVE")) {
                creativeSelfAt.put(staffKey, now);
            } else if (GIVE_ACTIONS.contains(entry.action())) {
                Long creativeAt = creativeSelfAt.get(staffKey);
                if (creativeAt != null && now - creativeAt <= CREATIVE_GIVE_WINDOW_MILLIS) {
                    reasons.add("Item grant shortly after self creative mode");
                }
            }
        }

        return reasons.isEmpty() ? AnomalyResult.clean() : AnomalyResult.flagged(reasons);
    }

    private int recordAndCount(Map<String, Deque<Long>> windows, String key, long now, long windowMillis) {
        Deque<Long> deque = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
        deque.addLast(now);
        long cutoff = now - windowMillis;
        while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
            deque.pollFirst();
        }
        return deque.size();
    }

    private boolean isSelfTarget(AuditEntry entry) {
        if (entry.targetName() == null || entry.staffName() == null) {
            return false;
        }
        return entry.targetName().equalsIgnoreCase(entry.staffName());
    }

    private String staffKey(AuditEntry entry) {
        if (entry.staffUuid() != null) {
            return entry.staffUuid().toString();
        }
        return "name:" + lower(entry.staffName());
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
