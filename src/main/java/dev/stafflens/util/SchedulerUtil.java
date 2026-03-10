package dev.stafflens.util;

import dev.stafflens.StaffLensPlugin;

public final class SchedulerUtil {

    private SchedulerUtil() {
    }

    public static void runSync(StaffLensPlugin plugin, Runnable task) {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, task);
    }
}
