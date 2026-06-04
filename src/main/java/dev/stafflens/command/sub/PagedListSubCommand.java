package dev.stafflens.command.sub;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.command.StaffLensCommand;
import dev.stafflens.model.AuditEntry;
import dev.stafflens.model.AuditFilter;
import dev.stafflens.util.MessageUtil;
import dev.stafflens.util.SchedulerUtil;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Base for subcommands that print a paginated list of entries. Handles the async-fetch / sync-render
 * / pagination boilerplate so each command only says what to fetch and how to render a line.
 */
public abstract class PagedListSubCommand implements StaffLensCommand.SubCommand {

    protected final StaffLensPlugin plugin;

    protected PagedListSubCommand(StaffLensPlugin plugin) {
        this.plugin = plugin;
    }

    public interface PageQuery {
        List<AuditEntry> fetch(int limit, int offset);

        int count();
    }

    public interface LineRenderer {
        String line(AuditEntry entry);
    }

    protected int pageSize() {
        return plugin.getConfig().getInt("log.page-size", 15);
    }

    /** Shortcut over {@link #renderPage} when the page comes straight from an {@link AuditFilter}. */
    protected void renderFiltered(CommandSender sender, String header, String baseCommand, int page,
                                  AuditFilter filter, LineRenderer renderer) {
        renderPage(sender, header, baseCommand, page, new PageQuery() {
            @Override
            public List<AuditEntry> fetch(int limit, int offset) {
                return plugin.getDatabase().find(filter, limit, offset);
            }

            @Override
            public int count() {
                return plugin.getDatabase().countFind(filter);
            }
        }, renderer);
    }

    /** Runs the query off-thread, then renders the page back on the region thread. */
    protected void renderPage(CommandSender sender, String header, String baseCommand, int page,
                              PageQuery query, LineRenderer renderer) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            int limit = pageSize();
            int offset = (page - 1) * limit;
            List<AuditEntry> entries = query.fetch(limit, offset);
            int total = query.count();
            int totalPages = Math.max(1, (int) Math.ceil(total / (double) limit));

            SchedulerUtil.runSync(plugin, () -> {
                if (entries.isEmpty()) {
                    MessageUtil.sendMessage(sender, plugin, "no-data");
                    return;
                }
                sender.sendMessage(MessageUtil.parse(header));
                for (AuditEntry entry : entries) {
                    sender.sendMessage(MessageUtil.auditEntry(entry, renderer.line(entry)));
                }
                sender.sendMessage(MessageUtil.pageControls(baseCommand, page, totalPages));
            });
        });
    }
}
