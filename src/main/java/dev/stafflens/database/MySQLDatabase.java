package dev.stafflens.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.stafflens.StaffLensPlugin;
import dev.stafflens.model.ActionType;
import dev.stafflens.model.AuditEntry;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MySQLDatabase implements Database {

    private static final String LEGACY_CONSOLE_UUID = "CONSOLE";
    private static final String CONSOLE_NAME = "Console";

    private final StaffLensPlugin plugin;
    private HikariDataSource dataSource;

    public MySQLDatabase(StaffLensPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() throws SQLException {
        FileConfiguration cfg = plugin.getConfig();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + cfg.getString("database.mysql.host") + ":" + cfg.getString("database.mysql.port") + "/" + cfg.getString("database.mysql.database"));
        config.setUsername(cfg.getString("database.mysql.username"));
        config.setPassword(cfg.getString("database.mysql.password"));
        config.setPoolName("StaffLens-MySQL");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS stafflens_logs (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "staff_uuid VARCHAR(36) NULL, " +
                    "staff_name VARCHAR(32), " +
                    "action VARCHAR(32), " +
                    "target_name VARCHAR(32), " +
                    "reason TEXT, " +
                    "details TEXT, " +
                    "timestamp BIGINT)");
            createIndexIfMissing(conn, "idx_stafflens_staff_uuid_time", "CREATE INDEX idx_stafflens_staff_uuid_time ON stafflens_logs (staff_uuid, timestamp DESC)");
            createIndexIfMissing(conn, "idx_stafflens_staff_name_time", "CREATE INDEX idx_stafflens_staff_name_time ON stafflens_logs (staff_name, timestamp DESC)");
            createIndexIfMissing(conn, "idx_stafflens_target_name_time", "CREATE INDEX idx_stafflens_target_name_time ON stafflens_logs (target_name, timestamp DESC)");
            createIndexIfMissing(conn, "idx_stafflens_timestamp", "CREATE INDEX idx_stafflens_timestamp ON stafflens_logs (timestamp DESC)");
        }
    }

    @Override
    public void insert(AuditEntry entry) {
        String sql = "INSERT INTO stafflens_logs (staff_uuid, staff_name, action, target_name, reason, details, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (entry.staffUuid() != null) {
                ps.setString(1, entry.staffUuid().toString());
            } else {
                ps.setNull(1, java.sql.Types.VARCHAR);
            }
            ps.setString(2, entry.staffName());
            ps.setString(3, entry.action().name());
            ps.setString(4, entry.targetName());
            ps.setString(5, entry.reason());
            ps.setString(6, entry.details());
            ps.setLong(7, entry.timestamp());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to insert log: " + e.getMessage());
        }
    }

    private List<AuditEntry> query(String sql, Object... args) {
        List<AuditEntry> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = parseUuid(rs.getString("staff_uuid"));
                    ActionType actionType;
                    try {
                        actionType = ActionType.fromSerialized(rs.getString("action"));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Skipping log row with unknown action type: " + rs.getString("action"));
                        continue;
                    }
                    list.add(new AuditEntry(
                            uuid,
                            rs.getString("staff_name"),
                            actionType,
                            rs.getString("target_name"),
                            rs.getString("reason"),
                            rs.getString("details"),
                            rs.getLong("timestamp")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Database query error: " + e.getMessage());
        }
        return list;
    }

    private int count(String sql, Object... args) {
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Database count error: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public List<AuditEntry> getByStaff(UUID uuid, int limit, int offset) {
        if (uuid == null) {
            return query("SELECT * FROM stafflens_logs WHERE ((staff_uuid IS NULL AND staff_name = ?) OR staff_uuid = ?) ORDER BY timestamp DESC LIMIT ? OFFSET ?",
                    CONSOLE_NAME, LEGACY_CONSOLE_UUID, limit, offset);
        }
        return query("SELECT * FROM stafflens_logs WHERE staff_uuid = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?",
                uuid.toString(), limit, offset);
    }

    @Override
    public List<AuditEntry> getByStaff(UUID uuid, int limit) {
        return getByStaff(uuid, limit, 0);
    }

    @Override
    public List<AuditEntry> getByStaffName(String name, int limit, int offset) {
        return query("SELECT * FROM stafflens_logs WHERE staff_name = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?",
                name, limit, offset);
    }

    @Override
    public List<AuditEntry> getByStaffName(String name, int limit) {
        return getByStaffName(name, limit, 0);
    }

    @Override
    public int countByStaff(UUID uuid) {
        if (uuid == null) {
            return count("SELECT COUNT(*) FROM stafflens_logs WHERE ((staff_uuid IS NULL AND staff_name = ?) OR staff_uuid = ?)",
                    CONSOLE_NAME, LEGACY_CONSOLE_UUID);
        }
        return count("SELECT COUNT(*) FROM stafflens_logs WHERE staff_uuid = ?", uuid.toString());
    }

    @Override
    public int countByStaffName(String name) {
        return count("SELECT COUNT(*) FROM stafflens_logs WHERE staff_name = ?", name);
    }

    @Override
    public List<AuditEntry> getByTarget(String name, int limit, int offset) {
        return query("SELECT * FROM stafflens_logs WHERE target_name = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?",
                name, limit, offset);
    }

    @Override
    public List<AuditEntry> getByTarget(String name, int limit) {
        return getByTarget(name, limit, 0);
    }

    @Override
    public int countByTarget(String name) {
        return count("SELECT COUNT(*) FROM stafflens_logs WHERE target_name = ?", name);
    }

    @Override
    public List<AuditEntry> search(String query, int limit, int offset) {
        String q = "%" + query + "%";
        return query("SELECT * FROM stafflens_logs WHERE staff_name LIKE ? OR target_name LIKE ? OR reason LIKE ? OR details LIKE ? ORDER BY timestamp DESC LIMIT ? OFFSET ?",
                q, q, q, q, limit, offset);
    }

    @Override
    public List<AuditEntry> search(String query, int limit) {
        return search(query, limit, 0);
    }

    @Override
    public int countSearch(String query) {
        String q = "%" + query + "%";
        return count("SELECT COUNT(*) FROM stafflens_logs WHERE staff_name LIKE ? OR target_name LIKE ? OR reason LIKE ? OR details LIKE ?",
                q, q, q, q);
    }

    @Override
    public List<AuditEntry> getToday(int limit, int offset) {
        long startOfDay = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return query("SELECT * FROM stafflens_logs WHERE timestamp >= ? ORDER BY timestamp DESC LIMIT ? OFFSET ?",
                startOfDay, limit, offset);
    }

    @Override
    public List<AuditEntry> getToday() {
        return getToday(Integer.MAX_VALUE, 0);
    }

    @Override
    public int countToday() {
        long startOfDay = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return count("SELECT COUNT(*) FROM stafflens_logs WHERE timestamp >= ?", startOfDay);
    }

    @Override
    public void cleanupOldEntries(int retentionDays) {
        if (retentionDays <= 0) {
            return;
        }

        long cutoff = System.currentTimeMillis() - (retentionDays * 24L * 60L * 60L * 1000L);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM stafflens_logs WHERE timestamp < ?")) {
            ps.setLong(1, cutoff);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to cleanup old logs: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    private void createIndexIfMissing(Connection conn, String indexName, String createSql) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getIndexInfo(conn.getCatalog(), null, "stafflens_logs", false, false)) {
            while (rs.next()) {
                String existing = rs.getString("INDEX_NAME");
                if (indexName.equalsIgnoreCase(existing)) {
                    return;
                }
            }
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createSql);
        }
    }

    private UUID parseUuid(String uuidStr) {
        if (uuidStr == null || uuidStr.isBlank() || LEGACY_CONSOLE_UUID.equals(uuidStr)) {
            return null;
        }
        return UUID.fromString(uuidStr);
    }
}
