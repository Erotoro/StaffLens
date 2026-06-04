package dev.stafflens.database;

import com.zaxxer.hikari.HikariDataSource;
import dev.stafflens.StaffLensPlugin;
import dev.stafflens.model.ActionType;
import dev.stafflens.model.AuditEntry;
import dev.stafflens.model.AuditFilter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Shared JDBC logic for the SQLite and MySQL backends. Subclasses only provide a configured pool and
 * a few dialect-specific fragments; everything else (queries, migration, the hash chain) lives here.
 */
public abstract class AbstractSqlDatabase implements Database {

    protected static final String TABLE = "stafflens_logs";
    private static final String LEGACY_CONSOLE_UUID = "CONSOLE";
    private static final String CONSOLE_NAME = "Console";
    private static final char SEP = (char) 1;
    private static final String GENESIS = "";

    /** Context/security columns added after the original release; migrated in on upgrade. */
    private static final String[][] EXTRA_COLUMNS = {
            {"server_name", "VARCHAR(64)"},
            {"world", "VARCHAR(64)"},
            {"x", "INT"},
            {"y", "INT"},
            {"z", "INT"},
            {"ip", "VARCHAR(64)"},
            {"flagged", "INT DEFAULT 0"},
            {"prev_hash", "VARCHAR(64)"},
            {"entry_hash", "VARCHAR(64)"},
    };

    protected final StaffLensPlugin plugin;
    protected HikariDataSource dataSource;

    /** Last entry hash in the chain. Only mutated by the single-threaded audit writer. */
    private final Object hashLock = new Object();
    private String lastHash = GENESIS;

    protected AbstractSqlDatabase(StaffLensPlugin plugin) {
        this.plugin = plugin;
    }

    // Dialect hooks

    protected abstract HikariDataSource createDataSource() throws SQLException;

    protected abstract String idColumnDefinition();

    /** Collation suffix for case-insensitive comparisons (SQLite needs COLLATE NOCASE, MySQL doesn't). */
    protected String caseInsensitive() {
        return "";
    }

    // Lifecycle

    @Override
    public void init() throws SQLException {
        this.dataSource = createDataSource();
        try (Connection conn = dataSource.getConnection()) {
            createTable(conn);
            migrateColumns(conn);
            createIndexes(conn);
            this.lastHash = readLastHash(conn);
        }
    }

    private void createTable(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                "id " + idColumnDefinition() + ", " +
                "staff_uuid VARCHAR(36), " +
                "staff_name VARCHAR(32), " +
                "action VARCHAR(32), " +
                "target_name VARCHAR(32), " +
                "reason TEXT, " +
                "details TEXT, " +
                "timestamp BIGINT, " +
                "server_name VARCHAR(64), " +
                "world VARCHAR(64), " +
                "x INT, " +
                "y INT, " +
                "z INT, " +
                "ip VARCHAR(64), " +
                "flagged INT DEFAULT 0, " +
                "prev_hash VARCHAR(64), " +
                "entry_hash VARCHAR(64))";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void migrateColumns(Connection conn) throws SQLException {
        Set<String> existing = existingColumns(conn);
        for (String[] column : EXTRA_COLUMNS) {
            if (!existing.contains(column[0].toLowerCase(Locale.ROOT))) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE " + TABLE + " ADD COLUMN " + column[0] + " " + column[1]);
                    plugin.getLogger().info("Migrated database: added column '" + column[0] + "'.");
                }
            }
        }
    }

    private Set<String> existingColumns(Connection conn) throws SQLException {
        Set<String> columns = new HashSet<>();
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, TABLE, null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return columns;
    }

    private void createIndexes(Connection conn) throws SQLException {
        String ci = caseInsensitive();
        createIndexIfMissing(conn, "idx_stafflens_staff_uuid_time", "(staff_uuid, timestamp DESC)");
        createIndexIfMissing(conn, "idx_stafflens_staff_name_time", "(staff_name" + ci + ", timestamp DESC)");
        createIndexIfMissing(conn, "idx_stafflens_target_name_time", "(target_name" + ci + ", timestamp DESC)");
        createIndexIfMissing(conn, "idx_stafflens_timestamp", "(timestamp DESC)");
    }

    private void createIndexIfMissing(Connection conn, String indexName, String columns) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getIndexInfo(conn.getCatalog(), null, TABLE, false, false)) {
            while (rs.next()) {
                if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                    return;
                }
            }
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE INDEX " + indexName + " ON " + TABLE + " " + columns);
        }
    }

    private String readLastHash(Connection conn) throws SQLException {
        String sql = "SELECT entry_hash FROM " + TABLE + " WHERE entry_hash IS NOT NULL ORDER BY id DESC LIMIT 1";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                String hash = rs.getString(1);
                if (hash != null && !hash.isBlank()) {
                    return hash;
                }
            }
        }
        return GENESIS;
    }

    // Writes

    @Override
    public void insert(AuditEntry entry) {
        String sql = "INSERT INTO " + TABLE + " (staff_uuid, staff_name, action, target_name, reason, " +
                "details, timestamp, server_name, world, x, y, z, ip, flagged, prev_hash, entry_hash) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        synchronized (hashLock) {
            String prev = lastHash;
            String hash = chainHash(prev, entry);
            try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                setNullableString(ps, 1, entry.staffUuid() != null ? entry.staffUuid().toString() : null);
                ps.setString(2, entry.staffName());
                ps.setString(3, entry.action().name());
                ps.setString(4, entry.targetName());
                ps.setString(5, entry.reason());
                ps.setString(6, entry.details());
                ps.setLong(7, entry.timestamp());
                setNullableString(ps, 8, entry.serverName());
                setNullableString(ps, 9, entry.world());
                setNullableInt(ps, 10, entry.x());
                setNullableInt(ps, 11, entry.y());
                setNullableInt(ps, 12, entry.z());
                setNullableString(ps, 13, entry.ip());
                ps.setInt(14, entry.flagged() ? 1 : 0);
                ps.setString(15, prev);
                ps.setString(16, hash);
                ps.executeUpdate();
                lastHash = hash;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to insert log: " + e.getMessage());
            }
        }
    }

    // Reads

    private List<AuditEntry> query(String sql, Object... args) {
        List<AuditEntry> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditEntry entry = mapRow(rs);
                    if (entry != null) {
                        list.add(entry);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Database query error: " + e.getMessage());
        }
        return list;
    }

    private int count(String sql, Object... args) {
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, args);
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

    private AuditEntry mapRow(ResultSet rs) throws SQLException {
        ActionType actionType;
        try {
            actionType = ActionType.fromSerialized(rs.getString("action"));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Skipping log row with unknown action type: " + rs.getString("action"));
            return null;
        }
        return new AuditEntry(
                parseUuid(rs.getString("staff_uuid")),
                rs.getString("staff_name"),
                actionType,
                rs.getString("target_name"),
                rs.getString("reason"),
                rs.getString("details"),
                rs.getLong("timestamp"),
                rs.getString("server_name"),
                rs.getString("world"),
                getNullableInt(rs, "x"),
                getNullableInt(rs, "y"),
                getNullableInt(rs, "z"),
                rs.getString("ip"),
                rs.getInt("flagged") != 0
        );
    }

    @Override
    public List<AuditEntry> getByStaff(UUID uuid, int limit, int offset) {
        if (uuid == null) {
            return query("SELECT * FROM " + TABLE + " WHERE ((staff_uuid IS NULL AND staff_name = ?) OR staff_uuid = ?) ORDER BY timestamp DESC LIMIT ? OFFSET ?",
                    CONSOLE_NAME, LEGACY_CONSOLE_UUID, limit, offset);
        }
        return query("SELECT * FROM " + TABLE + " WHERE staff_uuid = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?",
                uuid.toString(), limit, offset);
    }

    @Override
    public List<AuditEntry> getByStaff(UUID uuid, int limit) {
        return getByStaff(uuid, limit, 0);
    }

    @Override
    public List<AuditEntry> getByStaffName(String name, int limit, int offset) {
        return query("SELECT * FROM " + TABLE + " WHERE staff_name = ?" + caseInsensitive() + " ORDER BY timestamp DESC LIMIT ? OFFSET ?",
                name, limit, offset);
    }

    @Override
    public List<AuditEntry> getByStaffName(String name, int limit) {
        return getByStaffName(name, limit, 0);
    }

    @Override
    public int countByStaff(UUID uuid) {
        if (uuid == null) {
            return count("SELECT COUNT(*) FROM " + TABLE + " WHERE ((staff_uuid IS NULL AND staff_name = ?) OR staff_uuid = ?)",
                    CONSOLE_NAME, LEGACY_CONSOLE_UUID);
        }
        return count("SELECT COUNT(*) FROM " + TABLE + " WHERE staff_uuid = ?", uuid.toString());
    }

    @Override
    public int countByStaffName(String name) {
        return count("SELECT COUNT(*) FROM " + TABLE + " WHERE staff_name = ?" + caseInsensitive(), name);
    }

    @Override
    public List<AuditEntry> getByTarget(String name, int limit, int offset) {
        return query("SELECT * FROM " + TABLE + " WHERE target_name = ?" + caseInsensitive() + " ORDER BY timestamp DESC LIMIT ? OFFSET ?",
                name, limit, offset);
    }

    @Override
    public List<AuditEntry> getByTarget(String name, int limit) {
        return getByTarget(name, limit, 0);
    }

    @Override
    public int countByTarget(String name) {
        return count("SELECT COUNT(*) FROM " + TABLE + " WHERE target_name = ?" + caseInsensitive(), name);
    }

    @Override
    public List<AuditEntry> search(String queryText, int limit, int offset) {
        String like = "%" + queryText + "%";
        String ci = caseInsensitive();
        return query("SELECT * FROM " + TABLE + " WHERE staff_name LIKE ?" + ci + " OR target_name LIKE ?" + ci
                        + " OR reason LIKE ?" + ci + " OR details LIKE ?" + ci + " ORDER BY timestamp DESC LIMIT ? OFFSET ?",
                like, like, like, like, limit, offset);
    }

    @Override
    public List<AuditEntry> search(String queryText, int limit) {
        return search(queryText, limit, 0);
    }

    @Override
    public int countSearch(String queryText) {
        String like = "%" + queryText + "%";
        String ci = caseInsensitive();
        return count("SELECT COUNT(*) FROM " + TABLE + " WHERE staff_name LIKE ?" + ci + " OR target_name LIKE ?" + ci
                + " OR reason LIKE ?" + ci + " OR details LIKE ?" + ci, like, like, like, like);
    }

    @Override
    public List<AuditEntry> getToday(int limit, int offset) {
        return query("SELECT * FROM " + TABLE + " WHERE timestamp >= ? ORDER BY timestamp DESC LIMIT ? OFFSET ?",
                startOfToday(), limit, offset);
    }

    @Override
    public List<AuditEntry> getToday() {
        return getToday(Integer.MAX_VALUE, 0);
    }

    @Override
    public int countToday() {
        return count("SELECT COUNT(*) FROM " + TABLE + " WHERE timestamp >= ?", startOfToday());
    }

    // Flexible filtered query

    @Override
    public List<AuditEntry> find(AuditFilter filter, int limit, int offset) {
        List<Object> args = new ArrayList<>();
        String where = buildWhere(filter, args);
        args.add(limit);
        args.add(offset);
        return query("SELECT * FROM " + TABLE + where + " ORDER BY timestamp DESC LIMIT ? OFFSET ?", args.toArray());
    }

    @Override
    public int countFind(AuditFilter filter) {
        List<Object> args = new ArrayList<>();
        String where = buildWhere(filter, args);
        return count("SELECT COUNT(*) FROM " + TABLE + where, args.toArray());
    }

    @Override
    public Map<ActionType, Integer> actionBreakdown(AuditFilter filter) {
        List<Object> args = new ArrayList<>();
        String where = buildWhere(filter, args);
        String sql = "SELECT action, COUNT(*) AS total FROM " + TABLE + where
                + " GROUP BY action ORDER BY total DESC";
        Map<ActionType, Integer> breakdown = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, args.toArray());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        breakdown.put(ActionType.fromSerialized(rs.getString("action")), rs.getInt("total"));
                    } catch (IllegalArgumentException ignored) {
                        // Skip rows with action types this build no longer knows about.
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Database breakdown error: " + e.getMessage());
        }
        return breakdown;
    }

    private String buildWhere(AuditFilter filter, List<Object> args) {
        String ci = caseInsensitive();
        List<String> clauses = new ArrayList<>();
        if (filter.staffUuidSet()) {
            if (filter.staffUuid() == null) {
                clauses.add("((staff_uuid IS NULL AND staff_name = ?) OR staff_uuid = ?)");
                args.add(CONSOLE_NAME);
                args.add(LEGACY_CONSOLE_UUID);
            } else {
                clauses.add("staff_uuid = ?");
                args.add(filter.staffUuid().toString());
            }
        }
        if (filter.staffName() != null) {
            clauses.add("staff_name = ?" + ci);
            args.add(filter.staffName());
        }
        if (filter.targetName() != null) {
            clauses.add("target_name = ?" + ci);
            args.add(filter.targetName());
        }
        if (filter.action() != null) {
            clauses.add("action = ?");
            args.add(filter.action().name());
        }
        if (filter.sinceMillis() != null) {
            clauses.add("timestamp >= ?");
            args.add(filter.sinceMillis());
        }
        if (filter.text() != null && !filter.text().isBlank()) {
            String like = "%" + filter.text() + "%";
            clauses.add("(staff_name LIKE ?" + ci + " OR target_name LIKE ?" + ci
                    + " OR reason LIKE ?" + ci + " OR details LIKE ?" + ci + ")");
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (filter.flaggedOnly()) {
            clauses.add("flagged = 1");
        }
        if (clauses.isEmpty()) {
            return "";
        }
        return " WHERE " + String.join(" AND ", clauses);
    }

    // Integrity

    @Override
    public ChainVerification verifyChain() {
        String sql = "SELECT staff_uuid, staff_name, action, target_name, reason, details, timestamp, " +
                "server_name, world, x, y, z, ip, flagged, prev_hash, entry_hash, id FROM " + TABLE + " ORDER BY id ASC";
        long hashed = 0;
        long legacy = 0;
        String previousStored = GENESIS;
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String storedHash = rs.getString("entry_hash");
                if (storedHash == null || storedHash.isBlank()) {
                    legacy++;
                    continue;
                }
                hashed++;
                long id = rs.getLong("id");
                String storedPrev = rs.getString("prev_hash");
                if (storedPrev == null) {
                    storedPrev = GENESIS;
                }
                AuditEntry entry = mapRow(rs);
                boolean linkageOk = storedPrev.equals(previousStored);
                boolean contentOk = entry != null && storedHash.equals(chainHash(storedPrev, entry));
                if (!linkageOk || !contentOk) {
                    return new ChainVerification(false, hashed, legacy, id);
                }
                previousStored = storedHash;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Chain verification error: " + e.getMessage());
            return new ChainVerification(false, hashed, legacy, null);
        }
        return new ChainVerification(true, hashed, legacy, null);
    }

    private String chainHash(String prev, AuditEntry entry) {
        String content = String.join(String.valueOf(SEP),
                ns(entry.staffUuid() != null ? entry.staffUuid().toString() : null),
                ns(entry.staffName()),
                entry.action().name(),
                ns(entry.targetName()),
                ns(entry.reason()),
                ns(entry.details()),
                Long.toString(entry.timestamp()),
                ns(entry.serverName()),
                ns(entry.world()),
                ns(entry.x() != null ? entry.x().toString() : null),
                ns(entry.y() != null ? entry.y().toString() : null),
                ns(entry.z() != null ? entry.z().toString() : null),
                ns(entry.ip()),
                entry.flagged() ? "1" : "0");
        return sha256(prev + SEP + content);
    }

    // Maintenance

    @Override
    public void cleanupOldEntries(int retentionDays) {
        if (retentionDays <= 0) {
            return;
        }
        long cutoff = System.currentTimeMillis() - (retentionDays * 24L * 60L * 60L * 1000L);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM " + TABLE + " WHERE timestamp < ?")) {
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

    // Helpers

    private static long startOfToday() {
        return LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static void bind(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
    }

    private static void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value != null) {
            ps.setString(index, value);
        } else {
            ps.setNull(index, java.sql.Types.VARCHAR);
        }
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value != null) {
            ps.setInt(index, value);
        } else {
            ps.setNull(index, java.sql.Types.INTEGER);
        }
    }

    private static Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static UUID parseUuid(String uuidStr) {
        if (uuidStr == null || uuidStr.isBlank() || LEGACY_CONSOLE_UUID.equals(uuidStr)) {
            return null;
        }
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String ns(String value) {
        return value == null ? "" : value;
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
