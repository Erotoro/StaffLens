package dev.stafflens.logger;

import dev.stafflens.StaffLensPlugin;
import dev.stafflens.database.Database;
import dev.stafflens.model.AuditEntry;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Off-thread audit writer with a single consumer thread. One worker keeps rows in submission order
 * (so the hash chain stays deterministic), avoids connection-pool thrash, and drains cleanly on stop.
 */
public class AuditLogger {

    private final StaffLensPlugin plugin;
    private final Database database;
    private final BlockingQueue<AuditEntry> queue = new LinkedBlockingQueue<>();
    private final AtomicBoolean accepting = new AtomicBoolean(true);
    private final Thread worker;
    private volatile boolean running = true;

    public AuditLogger(StaffLensPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        this.worker = new Thread(this::processLoop, "StaffLens-Audit-Writer");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    public void log(AuditEntry entry) {
        if (!accepting.get()) {
            return;
        }
        queue.offer(entry);
    }

    private void processLoop() {
        while (running || !queue.isEmpty()) {
            AuditEntry entry;
            try {
                entry = queue.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (entry == null) {
                continue;
            }
            try {
                database.insert(entry);
            } catch (Throwable t) {
                plugin.getLogger().severe("Audit writer failed to persist an entry: " + t.getMessage());
            }
        }
    }

    /** Stops accepting entries and waits up to {@code timeoutMillis} for the queue to drain. */
    public void shutdownAndDrain(long timeoutMillis) {
        accepting.set(false);
        running = false;
        try {
            worker.join(timeoutMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (worker.isAlive()) {
            worker.interrupt();
            plugin.getLogger().warning("Audit writer did not drain within " + timeoutMillis
                    + "ms; " + queue.size() + " entr" + (queue.size() == 1 ? "y" : "ies") + " may be lost.");
        }
    }
}
