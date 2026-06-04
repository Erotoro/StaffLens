package dev.stafflens.command.sub;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.command.StaffLensCommand;
import dev.stafflens.database.Database;
import dev.stafflens.util.MessageUtil;
import dev.stafflens.util.SchedulerUtil;
import org.bukkit.command.CommandSender;

/**
 * {@code /sl verify} — re-walks the tamper-evident hash chain and reports whether the journal has
 * been altered or had rows deleted outside the plugin.
 */
public class VerifySubCommand implements StaffLensCommand.SubCommand {

    private final StaffLensPlugin plugin;

    public VerifySubCommand(StaffLensPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "verify";
    }

    @Override
    public String permission() {
        return "stafflens.admin";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        MessageUtil.sendMessage(sender, plugin, "verify-start");
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            Database.ChainVerification result = plugin.getDatabase().verifyChain();
            SchedulerUtil.runSync(plugin, () -> {
                if (result.intact()) {
                    MessageUtil.sendMessage(sender, plugin, "verify-ok",
                            "<hashed>", String.valueOf(result.hashedRows()),
                            "<legacy>", String.valueOf(result.legacyRows()));
                } else {
                    MessageUtil.sendMessage(sender, plugin, "verify-broken",
                            "<id>", result.firstBrokenId() != null ? String.valueOf(result.firstBrokenId()) : "?",
                            "<hashed>", String.valueOf(result.hashedRows()));
                }
            });
        });
    }
}
