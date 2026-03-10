package dev.stafflens.command.sub;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.command.StaffLensCommand;
import dev.stafflens.model.AuditEntry;
import dev.stafflens.util.MessageUtil;
import dev.stafflens.util.SchedulerUtil;
import dev.stafflens.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ExportSubCommand implements StaffLensCommand.SubCommand {
    private final StaffLensPlugin plugin;

    public ExportSubCommand(StaffLensPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "export";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("stafflens.export")) {
            MessageUtil.sendMessage(sender, plugin, "no-permission");
            return;
        }
        if (args.length < 1) {
            MessageUtil.sendMessage(sender, plugin, "usage-export");
            return;
        }

        String target = args[0];
        String safeTarget = sanitizeFileSegment(target);
        if (safeTarget.isBlank()) {
            MessageUtil.sendMessage(sender, plugin, "export-fail");
            return;
        }

        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            List<AuditEntry> entries;
            if ("console".equalsIgnoreCase(target)) {
                entries = plugin.getDatabase().getByStaff(null, 1000);
            } else {
                OfflinePlayer off = Bukkit.getOfflinePlayerIfCached(target);
                entries = off != null && off.getUniqueId() != null
                        ? plugin.getDatabase().getByStaff(off.getUniqueId(), 1000)
                        : plugin.getDatabase().getByStaffName(target, 1000);
            }

            if (entries.isEmpty()) {
                SchedulerUtil.runSync(plugin, () -> MessageUtil.sendMessage(sender, plugin, "no-data"));
                return;
            }

            File folder = new File(plugin.getDataFolder(), "exports");
            if (!folder.exists() && !folder.mkdirs()) {
                SchedulerUtil.runSync(plugin, () -> MessageUtil.sendMessage(sender, plugin, "export-fail"));
                return;
            }

            Path folderPath = folder.toPath().toAbsolutePath().normalize();
            Path filePath = folderPath.resolve("log_" + safeTarget + "_" + System.currentTimeMillis() + ".txt").normalize();
            if (!filePath.startsWith(folderPath)) {
                SchedulerUtil.runSync(plugin, () -> MessageUtil.sendMessage(sender, plugin, "export-fail"));
                return;
            }

            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                writer.write("Audit Log for " + target + "\n");
                writer.write("Generated: " + TimeUtil.format(System.currentTimeMillis()) + "\n");
                writer.write("-------------------------------------------------\n");

                for (AuditEntry e : entries) {
                    writer.write(String.format("[%s] Action: %s | Target: %s | Reason: %s | Details: %s%n",
                            TimeUtil.format(e.timestamp()),
                            safeValue(e.action().displayName()),
                            safeValue(e.targetName()),
                            safeValue(e.reason()),
                            safeValue(e.details())
                    ));
                }

                SchedulerUtil.runSync(plugin, () -> MessageUtil.sendMessage(sender, plugin, "export-success", "<file>", filePath.getFileName().toString()));
            } catch (IOException ex) {
                ex.printStackTrace();
                SchedulerUtil.runSync(plugin, () -> MessageUtil.sendMessage(sender, plugin, "export-fail"));
            }
        });
    }

    private String sanitizeFileSegment(String value) {
        return value.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private String safeValue(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
    }
}
