package dev.stafflens.database;

import dev.stafflens.model.ActionType;
import dev.stafflens.model.AuditEntry;
import dev.stafflens.model.AuditFilter;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface Database {
    void init() throws SQLException;

    void insert(AuditEntry entry);

    List<AuditEntry> getByStaff(UUID uuid, int limit, int offset);

    List<AuditEntry> getByStaff(UUID uuid, int limit);

    List<AuditEntry> getByStaffName(String name, int limit, int offset);

    List<AuditEntry> getByStaffName(String name, int limit);

    int countByStaff(UUID uuid);

    int countByStaffName(String name);

    List<AuditEntry> getByTarget(String name, int limit, int offset);

    List<AuditEntry> getByTarget(String name, int limit);

    int countByTarget(String name);

    List<AuditEntry> search(String query, int limit, int offset);

    List<AuditEntry> search(String query, int limit);

    int countSearch(String query);

    List<AuditEntry> getToday(int limit, int offset);

    List<AuditEntry> getToday();

    int countToday();

    /** Flexible, composable query used by the {@code key:value} command filters. */
    List<AuditEntry> find(AuditFilter filter, int limit, int offset);

    /** Total number of rows matching the given filter. */
    int countFind(AuditFilter filter);

    /** Counts matching rows grouped by action type, ordered by descending frequency. */
    Map<ActionType, Integer> actionBreakdown(AuditFilter filter);

    /** Re-walks the tamper-evident hash chain and reports the first inconsistency, if any. */
    ChainVerification verifyChain();

    void cleanupOldEntries(int retentionDays);

    void close();

    /**
     * Result of a hash-chain integrity check.
     *
     * @param intact        whether every hashed row links to and matches the previous one
     * @param hashedRows    number of rows that participate in the chain
     * @param legacyRows    rows predating the chain (no hash) that cannot be verified
     * @param firstBrokenId id of the first row whose hash/linkage failed, or {@code null} if intact
     */
    record ChainVerification(boolean intact, long hashedRows, long legacyRows, Long firstBrokenId) {
    }
}
