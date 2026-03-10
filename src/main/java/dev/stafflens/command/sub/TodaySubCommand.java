package dev.stafflens.command.sub;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.command.StaffLensCommand;
import dev.stafflens.model.AuditEntry;
import dev.stafflens.util.MessageUtil;
import dev.stafflens.util.SchedulerUtil;
import dev.stafflens.util.TimeUtil;
import org.bukkit.command.CommandSender;

import java.util.List;

public class TodaySubCommand implements StaffLensCommand.SubCommand {
    private final StaffLensPlugin plugin;

    public TodaySubCommand(StaffLensPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "today";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        int page = parsePage(args, 1);
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            int limit = plugin.getConfig().getInt("log.page-size", 15);
            int offset = (page - 1) * limit;
            List<AuditEntry> entries = plugin.getDatabase().getToday(limit, offset);
            int totalCount = plugin.getDatabase().countToday();
            int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) limit));
            SchedulerUtil.runSync(plugin, () -> {
                if (entries.isEmpty()) {
                    MessageUtil.sendMessage(sender, plugin, "no-data");
                    return;
                }

                sender.sendMessage(MessageUtil.parse("<gray>Actions today (<aqua>" + totalCount + "<gray>):"));
                for (AuditEntry e : entries) {
                    sender.sendMessage(MessageUtil.auditEntry(
                            e,
                            "<dark_gray>[" + TimeUtil.format(e.timestamp()) + "] <red>" + MessageUtil.escapeMiniMessage(e.staffName())
                                    + " <yellow>" + MessageUtil.escapeMiniMessage(e.action().displayName())
                                    + " <white>" + MessageUtil.escapeMiniMessage(e.targetName())
                    ));
                }
                sender.sendMessage(MessageUtil.pageControls("/sl today", page, totalPages));
            });
        });
    }

    private int parsePage(String[] args, int defaultPage) {
        if (args.length < 1) {
            return defaultPage;
        }
        try {
            return Math.max(1, Integer.parseInt(args[0]));
        } catch (NumberFormatException ignored) {
            return defaultPage;
        }
    }
}
