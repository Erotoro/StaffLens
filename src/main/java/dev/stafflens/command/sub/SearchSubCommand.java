package dev.stafflens.command.sub;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.command.CommandArgs;
import dev.stafflens.model.AuditFilter;
import dev.stafflens.util.MessageUtil;
import dev.stafflens.util.TimeUtil;
import org.bukkit.command.CommandSender;

/**
 * {@code /sl search <text> [page] [action:..] [time:..] [target:..] [flagged]} — free-text search
 * across the journal, combinable with structured filters.
 */
public class SearchSubCommand extends PagedListSubCommand {

    public SearchSubCommand(StaffLensPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "search";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        CommandArgs parsed = CommandArgs.parse(args);
        String text = parsed.joinedPositional();

        if (text.isBlank() && !parsed.hasFilters()) {
            MessageUtil.sendMessage(sender, plugin, "usage-search");
            return;
        }

        AuditFilter filter = parsed.filter();
        if (!text.isBlank()) {
            filter.text(text);
        }

        String label = text.isBlank() ? "filters" : text;
        String header = "<gray>Search results for '<gold>" + MessageUtil.escapeMiniMessage(label) + "<gray>':";
        renderFiltered(sender, header, "/sl search " + parsed.baseArgs(), parsed.page(), filter,
                entry -> MessageUtil.flagPrefix(entry)
                        + "<dark_gray>[" + TimeUtil.format(entry.timestamp()) + "] <red>"
                        + MessageUtil.escapeMiniMessage(entry.staffName())
                        + " <gray>> <yellow>" + MessageUtil.escapeMiniMessage(entry.action().displayName())
                        + " <gray>> <white>" + MessageUtil.escapeMiniMessage(entry.targetName()));
    }
}
