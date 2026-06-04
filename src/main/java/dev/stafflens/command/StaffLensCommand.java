package dev.stafflens.command;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.command.sub.ExportSubCommand;
import dev.stafflens.command.sub.LogSubCommand;
import dev.stafflens.command.sub.ReloadSubCommand;
import dev.stafflens.command.sub.SearchSubCommand;
import dev.stafflens.command.sub.StatsSubCommand;
import dev.stafflens.command.sub.TodaySubCommand;
import dev.stafflens.command.sub.VerifySubCommand;
import dev.stafflens.command.sub.WhoSubCommand;
import dev.stafflens.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class StaffLensCommand implements CommandExecutor {

    private final StaffLensPlugin plugin;
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public StaffLensCommand(StaffLensPlugin plugin) {
        this.plugin = plugin;
        registerSub(new LogSubCommand(plugin));
        registerSub(new WhoSubCommand(plugin));
        registerSub(new SearchSubCommand(plugin));
        registerSub(new TodaySubCommand(plugin));
        registerSub(new StatsSubCommand(plugin));
        registerSub(new ExportSubCommand(plugin));
        registerSub(new VerifySubCommand(plugin));
        registerSub(new ReloadSubCommand(plugin));
    }

    private void registerSub(SubCommand cmd) {
        subCommands.put(cmd.getName(), cmd);
    }

    /** @return the registered subcommands, keyed by name (insertion order preserved). */
    public Map<String, SubCommand> getSubCommands() {
        return subCommands;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            MessageUtil.sendMessage(sender, plugin, "help");
            return true;
        }

        SubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub == null) {
            MessageUtil.sendMessage(sender, plugin, "unknown-command");
            return true;
        }

        if (!sender.hasPermission(sub.permission())) {
            MessageUtil.sendMessage(sender, plugin, "no-permission");
            return true;
        }

        sub.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        return true;
    }

    public interface SubCommand {
        String getName();

        void execute(CommandSender sender, String[] args);

        default String permission() {
            return "stafflens.use";
        }
    }
}
