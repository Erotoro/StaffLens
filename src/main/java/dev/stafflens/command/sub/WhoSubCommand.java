package dev.stafflens.command.sub;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.command.StaffLensCommand;
import dev.stafflens.model.AuditEntry;
import dev.stafflens.util.MessageUtil;
import dev.stafflens.util.SchedulerUtil;
import dev.stafflens.util.TimeUtil;
import org.bukkit.command.CommandSender;

import java.util.List;

public class WhoSubCommand implements StaffLensCommand.SubCommand {
    private final StaffLensPlugin plugin;

    public WhoSubCommand(StaffLensPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "who";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            MessageUtil.sendMessage(sender, plugin, "usage-who");
            return;
        }

        int page = parsePage(args, 1);
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            int limit = plugin.getConfig().getInt("log.page-size", 15);
            int offset = (page - 1) * limit;
            List<AuditEntry> entries = plugin.getDatabase().getByTarget(args[0], limit, offset);
            int totalPages = Math.max(1, (int) Math.ceil(plugin.getDatabase().countByTarget(args[0]) / (double) limit));
            SchedulerUtil.runSync(plugin, () -> {
                if (entries.isEmpty()) {
                    MessageUtil.sendMessage(sender, plugin, "no-data");
                    return;
                }

                String safeTarget = MessageUtil.escapeMiniMessage(args[0]);
                sender.sendMessage(MessageUtil.parse("<gray>Actions against <gold>" + safeTarget + "<gray>:"));
                for (AuditEntry e : entries) {
                    sender.sendMessage(MessageUtil.auditEntry(
                            e,
                            "<dark_gray>[" + TimeUtil.format(e.timestamp()) + "] <red>" + MessageUtil.escapeMiniMessage(e.staffName())
                                    + " <gray>: <yellow>" + MessageUtil.escapeMiniMessage(e.action().displayName())
                    ));
                }
                sender.sendMessage(MessageUtil.pageControls("/sl who " + args[0], page, totalPages));
            });
        });
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
}
