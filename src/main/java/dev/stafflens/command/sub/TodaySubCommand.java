package dev.stafflens.command.sub;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.command.CommandArgs;
import dev.stafflens.model.AuditFilter;
import dev.stafflens.util.MessageUtil;
import dev.stafflens.util.TimeUtil;
import org.bukkit.command.CommandSender;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * {@code /sl today [page] [action:..] [staff:..] [flagged]} — everything recorded since midnight.
 */
public class TodaySubCommand extends PagedListSubCommand {

    public TodaySubCommand(StaffLensPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "today";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        CommandArgs parsed = CommandArgs.parse(args);

        long startOfDay = LocalDate.now(ZoneId.systemDefault())
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        AuditFilter filter = parsed.filter().since(startOfDay);

        renderFiltered(sender, "<gray>Actions today:", "/sl today " + parsed.baseArgs(), parsed.page(), filter,
                entry -> MessageUtil.flagPrefix(entry)
                        + "<dark_gray>[" + TimeUtil.format(entry.timestamp()) + "] <red>"
                        + MessageUtil.escapeMiniMessage(entry.staffName())
                        + " <yellow>" + MessageUtil.escapeMiniMessage(entry.action().displayName())
                        + " <white>" + MessageUtil.escapeMiniMessage(entry.targetName()));
    }
}
