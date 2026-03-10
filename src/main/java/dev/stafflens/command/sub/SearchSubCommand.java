package dev.stafflens.command.sub;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.command.StaffLensCommand;
import dev.stafflens.model.AuditEntry;
import dev.stafflens.util.MessageUtil;
import dev.stafflens.util.SchedulerUtil;
import dev.stafflens.util.TimeUtil;
import org.bukkit.command.CommandSender;

import java.util.List;

public class SearchSubCommand implements StaffLensCommand.SubCommand {
    private final StaffLensPlugin plugin;

    public SearchSubCommand(StaffLensPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "search";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            MessageUtil.sendMessage(sender, plugin, "usage-search");
            return;
        }
        int page = parseTrailingPage(args);
        int queryLength = isLastArgPage(args) ? args.length - 1 : args.length;
        String query = String.join(" ", java.util.Arrays.copyOfRange(args, 0, queryLength));

        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            int limit = plugin.getConfig().getInt("log.page-size", 15);
            int offset = (page - 1) * limit;
            List<AuditEntry> entries = plugin.getDatabase().search(query, limit, offset);
            int totalPages = Math.max(1, (int) Math.ceil(plugin.getDatabase().countSearch(query) / (double) limit));
            SchedulerUtil.runSync(plugin, () -> {
                if (entries.isEmpty()) {
                    MessageUtil.sendMessage(sender, plugin, "no-data");
                    return;
                }

                String safeQuery = MessageUtil.escapeMiniMessage(query);
                sender.sendMessage(MessageUtil.parse("<gray>Search results for '<gold>" + safeQuery + "<gray>':"));
                for (AuditEntry e : entries) {
                    sender.sendMessage(MessageUtil.auditEntry(
                            e,
                            "<dark_gray>[" + TimeUtil.format(e.timestamp()) + "] <red>" + MessageUtil.escapeMiniMessage(e.staffName())
                                    + " <gray>> <yellow>" + MessageUtil.escapeMiniMessage(e.action().displayName())
                                    + " <gray>> <white>" + MessageUtil.escapeMiniMessage(e.targetName())
                    ));
                }
                sender.sendMessage(MessageUtil.pageControls("/sl search " + query, page, totalPages));
            });
        });
    }

    private int parseTrailingPage(String[] args) {
        if (!isLastArgPage(args)) {
            return 1;
        }
        return Math.max(1, Integer.parseInt(args[args.length - 1]));
    }

    private boolean isLastArgPage(String[] args) {
        if (args.length < 2) {
            return false;
        }
        try {
            Integer.parseInt(args[args.length - 1]);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
