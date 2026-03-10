package dev.stafflens.database;

import dev.stafflens.model.AuditEntry;
import java.sql.SQLException;
import java.util.List;
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
    void cleanupOldEntries(int retentionDays);
    void close();
}
