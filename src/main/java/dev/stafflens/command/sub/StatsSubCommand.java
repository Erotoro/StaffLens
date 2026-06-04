package dev.stafflens.command.sub;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.command.CommandArgs;
import dev.stafflens.command.StaffLensCommand;
import dev.stafflens.model.ActionType;
import dev.stafflens.model.AuditFilter;
import dev.stafflens.util.MessageUtil;
import dev.stafflens.util.SchedulerUtil;
import org.bukkit.command.CommandSender;

import java.util.Map;

/**
 * {@code /sl stats <staff> [time:..]} — a compact summary of a staff member's activity: total
 * actions and a breakdown by action type.
 */
public class StatsSubCommand implements StaffLensCommand.SubCommand {

    private static final int TOP_ACTIONS = 10;

    private final StaffLensPlugin plugin;

    public StatsSubCommand(StaffLensPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        CommandArgs parsed = CommandArgs.parse(args);
        String staff = parsed.firstPositional();
        if (staff == null) {
            MessageUtil.sendMessage(sender, plugin, "usage-stats");
            return;
        }

        AuditFilter filter = parsed.filter();
        if ("console".equalsIgnoreCase(staff)) {
            filter.staffUuid(null);
        } else {
            filter.staffName(staff);
        }

        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            Map<ActionType, Integer> breakdown = plugin.getDatabase().actionBreakdown(filter);
            int total = breakdown.values().stream().mapToInt(Integer::intValue).sum();

            SchedulerUtil.runSync(plugin, () -> {
                if (total == 0) {
                    MessageUtil.sendMessage(sender, plugin, "no-data");
                    return;
                }
                sender.sendMessage(MessageUtil.parse("<gray>Stats for <gold>"
                        + MessageUtil.escapeMiniMessage(staff) + " <gray>(<aqua>" + total + "<gray> total):"));
                breakdown.entrySet().stream().limit(TOP_ACTIONS).forEach(e ->
                        sender.sendMessage(MessageUtil.parse("<dark_gray>- <yellow>"
                                + MessageUtil.escapeMiniMessage(e.getKey().displayName())
                                + " <gray>x<white>" + e.getValue())));
            });
        });
    }
}
