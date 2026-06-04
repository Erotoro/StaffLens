package dev.stafflens.command.sub;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.command.CommandArgs;
import dev.stafflens.model.AuditFilter;
import dev.stafflens.util.MessageUtil;
import dev.stafflens.util.TimeUtil;
import org.bukkit.command.CommandSender;

/**
 * {@code /sl who <target> [page] [action:..] [time:..]} — actions taken against a given target.
 */
public class WhoSubCommand extends PagedListSubCommand {

    public WhoSubCommand(StaffLensPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "who";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        CommandArgs parsed = CommandArgs.parse(args);
        String target = parsed.firstPositional();
        if (target == null) {
            MessageUtil.sendMessage(sender, plugin, "usage-who");
            return;
        }

        AuditFilter filter = parsed.filter().targetName(target);
        String header = "<gray>Actions against <gold>" + MessageUtil.escapeMiniMessage(target) + "<gray>:";
        renderFiltered(sender, header, "/sl who " + parsed.baseArgs(), parsed.page(), filter,
                entry -> MessageUtil.flagPrefix(entry)
                        + "<dark_gray>[" + TimeUtil.format(entry.timestamp()) + "] <red>"
                        + MessageUtil.escapeMiniMessage(entry.staffName())
                        + " <gray>: <yellow>" + MessageUtil.escapeMiniMessage(entry.action().displayName()));
    }
}
