package dev.stafflens.command;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.command.sub.*;
import dev.stafflens.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class StaffLensCommand implements CommandExecutor {

    private final StaffLensPlugin plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public StaffLensCommand(StaffLensPlugin plugin) {
        this.plugin = plugin;
        registerSub(new LogSubCommand(plugin));
        registerSub(new WhoSubCommand(plugin));
        registerSub(new SearchSubCommand(plugin));
        registerSub(new TodaySubCommand(plugin));
        registerSub(new ExportSubCommand(plugin));
    }

    private void registerSub(SubCommand cmd) {
        subCommands.put(cmd.getName(), cmd);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            MessageUtil.sendMessage(sender, plugin, "help");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("stafflens.admin")) {
                MessageUtil.sendMessage(sender, plugin, "no-permission");
                return true;
            }
            try {
                plugin.reloadRuntime();
                MessageUtil.sendMessage(sender, plugin, "reload-success");
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to reload StaffLens: " + e.getMessage());
                sender.sendMessage(MessageUtil.parse("<red>Reload failed. Check console."));
            }
            return true;
        }

        SubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub != null) {
            if (!sender.hasPermission("stafflens.use")) {
                MessageUtil.sendMessage(sender, plugin, "no-permission");
                return true;
            }
            sub.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        } else {
            MessageUtil.sendMessage(sender, plugin, "unknown-command");
        }
        return true;
    }
    
    public interface SubCommand {
        String getName();
        void execute(CommandSender sender, String[] args);
    }
}
