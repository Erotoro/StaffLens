package dev.stafflens.command.sub;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.command.StaffLensCommand;
import dev.stafflens.model.AuditEntry;
import dev.stafflens.util.MessageUtil;
import dev.stafflens.util.SchedulerUtil;
import dev.stafflens.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.List;

public class LogSubCommand implements StaffLensCommand.SubCommand {
    private final StaffLensPlugin plugin;

    public LogSubCommand(StaffLensPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "log";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            MessageUtil.sendMessage(sender, plugin, "usage-log");
            return;
        }

        String targetName = args[0];
        int page = parsePage(args, 1);
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            int limit = plugin.getConfig().getInt("log.page-size", 15);
            int offset = (page - 1) * limit;
            QueryResult result = resolveStaffEntries(targetName, limit, offset);
            int totalPages = Math.max(1, (int) Math.ceil(result.total() / (double) limit));

            SchedulerUtil.runSync(plugin, () -> {
                if (result.entries().isEmpty()) {
                    MessageUtil.sendMessage(sender, plugin, "no-data");
                    return;
                }

                String safeTarget = MessageUtil.escapeMiniMessage(targetName);
                sender.sendMessage(MessageUtil.parse("<gray>Log for <gold>" + safeTarget + "<gray>:"));
                for (AuditEntry e : result.entries()) {
                    sender.sendMessage(MessageUtil.auditEntry(
                            e,
                            "<dark_gray>[" + TimeUtil.format(e.timestamp()) + "] <yellow>" + MessageUtil.escapeMiniMessage(e.action().displayName())
                                    + " <gray>-> <white>" + MessageUtil.escapeMiniMessage(e.targetName())
                                    + " <gray>(" + MessageUtil.escapeMiniMessage(e.reason()) + ")"
                    ));
                }
                sender.sendMessage(MessageUtil.pageControls("/sl log " + targetName, page, totalPages));
            });
        });
    }

    private QueryResult resolveStaffEntries(String targetName, int limit, int offset) {
        if ("console".equalsIgnoreCase(targetName)) {
            return new QueryResult(plugin.getDatabase().getByStaff(null, limit, offset), plugin.getDatabase().countByStaff(null));
        }

        OfflinePlayer off = Bukkit.getOfflinePlayerIfCached(targetName);
        if (off != null && off.getUniqueId() != null) {
            List<AuditEntry> entries = plugin.getDatabase().getByStaff(off.getUniqueId(), limit, offset);
            if (!entries.isEmpty()) {
                return new QueryResult(entries, plugin.getDatabase().countByStaff(off.getUniqueId()));
            }
        }

        return new QueryResult(plugin.getDatabase().getByStaffName(targetName, limit, offset), plugin.getDatabase().countByStaffName(targetName));
    }

    private int parsePage(String[] args, int defaultPage) {
        if (args.length < 2) {
            return defaultPage;
        }
        try {
            return Math.max(1, Integer.parseInt(args[1]));
        } catch (NumberFormatException ignored) {
            return defaultPage;
        }
    }

    private record QueryResult(List<AuditEntry> entries, int total) {
    }
}
