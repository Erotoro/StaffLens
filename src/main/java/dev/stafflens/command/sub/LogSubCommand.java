package dev.stafflens.command.sub;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.command.CommandArgs;
import dev.stafflens.model.AuditFilter;
import dev.stafflens.util.MessageUtil;
import dev.stafflens.util.TimeUtil;
import org.bukkit.command.CommandSender;

/**
 * {@code /sl log <staff> [page] [action:..] [time:..] [flagged]} — history of actions performed by a
 * staff member, with optional filters.
 */
public class LogSubCommand extends PagedListSubCommand {

    public LogSubCommand(StaffLensPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "log";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        CommandArgs parsed = CommandArgs.parse(args);
        String staff = parsed.firstPositional();
        if (staff == null) {
            MessageUtil.sendMessage(sender, plugin, "usage-log");
            return;
        }

        AuditFilter filter = parsed.filter();
        if ("console".equalsIgnoreCase(staff)) {
            filter.staffUuid(null);
        } else {
            // Match by name (case-insensitive): catches both UUID-stamped and name-only rows.
            filter.staffName(staff);
        }

        String header = "<gray>Log for <gold>" + MessageUtil.escapeMiniMessage(staff) + "<gray>:";
        renderFiltered(sender, header, "/sl log " + parsed.baseArgs(), parsed.page(), filter,
                entry -> MessageUtil.flagPrefix(entry)
                        + "<dark_gray>[" + TimeUtil.format(entry.timestamp()) + "] <yellow>"
                        + MessageUtil.escapeMiniMessage(entry.action().displayName())
                        + " <gray>-> <white>" + MessageUtil.escapeMiniMessage(entry.targetName())
                        + " <gray>(" + MessageUtil.escapeMiniMessage(entry.reason()) + ")");
    }
}
