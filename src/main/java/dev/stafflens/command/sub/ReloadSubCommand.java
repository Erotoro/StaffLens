package dev.stafflens.command.sub;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.command.StaffLensCommand;
import dev.stafflens.util.MessageUtil;
import org.bukkit.command.CommandSender;

/**
 * {@code /sl reload} — reloads configuration, locales and integrations without a restart.
 */
public class ReloadSubCommand implements StaffLensCommand.SubCommand {

    private final StaffLensPlugin plugin;

    public ReloadSubCommand(StaffLensPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String permission() {
        return "stafflens.admin";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        try {
            plugin.reloadRuntime();
            MessageUtil.sendMessage(sender, plugin, "reload-success");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reload StaffLens: " + e.getMessage());
            sender.sendMessage(MessageUtil.parse("<red>Reload failed. Check console."));
        }
    }
}
