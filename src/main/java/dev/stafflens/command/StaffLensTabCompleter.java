package dev.stafflens.command;

import dev.stafflens.model.ActionType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tab completion: subcommands the sender may use, online player names, filter keys, and action names
 * when completing {@code action:}.
 */
public class StaffLensTabCompleter implements TabCompleter {

    private static final List<String> FILTER_KEYS = List.of("action:", "time:", "target:", "staff:", "flagged");
    private static final List<String> TIME_SUGGESTIONS = List.of("time:1h", "time:24h", "time:7d", "time:30d");
    private static final Set<String> PLAYER_ARG_COMMANDS = Set.of("log", "who", "stats", "export");

    private final StaffLensCommand command;

    public StaffLensTabCompleter(StaffLensCommand command) {
        this.command = command;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            return List.of();
        }

        if (args.length == 1) {
            return filterByPrefix(availableSubcommands(sender), args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        String current = args[args.length - 1];

        if (current.toLowerCase(Locale.ROOT).startsWith("action:")) {
            return filterByPrefix(actionSuggestions(), current);
        }
        if (current.toLowerCase(Locale.ROOT).startsWith("time:")) {
            return filterByPrefix(TIME_SUGGESTIONS, current);
        }

        List<String> suggestions = new ArrayList<>();
        if (PLAYER_ARG_COMMANDS.contains(sub) && args.length == 2) {
            suggestions.addAll(onlinePlayerNames());
            suggestions.add("console");
        }
        if (!sub.equals("export") && !sub.equals("reload") && !sub.equals("verify")) {
            suggestions.addAll(FILTER_KEYS);
        }
        return filterByPrefix(suggestions, current);
    }

    private List<String> availableSubcommands(CommandSender sender) {
        List<String> names = new ArrayList<>();
        command.getSubCommands().values().forEach(sub -> {
            if (sender.hasPermission(sub.permission())) {
                names.add(sub.getName());
            }
        });
        return names;
    }

    private List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private List<String> actionSuggestions() {
        List<String> actions = new ArrayList<>();
        for (ActionType type : ActionType.values()) {
            actions.add("action:" + type.name().toLowerCase(Locale.ROOT));
        }
        return actions;
    }

    @Nullable
    private List<String> filterByPrefix(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> matched = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matched.add(option);
            }
        }
        return matched;
    }
}
