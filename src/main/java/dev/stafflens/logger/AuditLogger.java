package dev.stafflens.logger;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.database.Database;
import dev.stafflens.model.AuditEntry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AuditLogger {

    private final StaffLensPlugin plugin;
    private final Database database;
    private final AtomicInteger pendingWrites = new AtomicInteger();
    private final AtomicBoolean acceptingWrites = new AtomicBoolean(true);
    private final Object drainMonitor = new Object();

    public AuditLogger(StaffLensPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    public void log(AuditEntry entry) {
        if (!acceptingWrites.get()) {
            return;
        }

        pendingWrites.incrementAndGet();
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            try {
                database.insert(entry);
            } finally {
                if (pendingWrites.decrementAndGet() == 0) {
                    synchronized (drainMonitor) {
                        drainMonitor.notifyAll();
                    }
                }
            }
        });
    }

    public void shutdownAndDrain(long timeoutMillis) {
        acceptingWrites.set(false);
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        synchronized (drainMonitor) {
            while (pendingWrites.get() > 0) {
                long remainingNanos = deadline - System.nanoTime();
                if (remainingNanos <= 0) {
                    break;
                }

                try {
                    TimeUnit.NANOSECONDS.timedWait(drainMonitor, remainingNanos);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
