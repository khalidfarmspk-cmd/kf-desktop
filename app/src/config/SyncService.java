package config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background cloud sync: local DB is primary; push outbox / pull prices & settings.
 * Daemon threads only — never blocks the till.
 */
public final class SyncService {

    private static final Logger LOG = Logger.getLogger(SyncService.class.getName());
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final long PUSH_INTERVAL_MS = 30_000L;
    private static final long PULL_INTERVAL_MS = 60_000L;
    private static final long AUTH_RETRY_WAIT_MS = 60_000L;
    private static final int MAX_ATTEMPTS = 5;
    private static final int BATCH_SIZE = 20;

    private static final String JDBC_URL = "jdbc:mysql://localhost:3307/pointofsale";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "";

    private static final SyncService INSTANCE = new SyncService();

    private final Object lock = new Object();
    private volatile boolean started;
    private volatile long lastPushSuccessAtMs;
    private volatile long lastPushFailureAtMs;
    private volatile boolean lastPushHadNetworkError;

    private SyncService() {
    }

    public static SyncService getInstance() {
        return INSTANCE;
    }

    /** Start daemon push/pull threads once (idempotent). */
    public void start() {
        synchronized (lock) {
            if (started) {
                return;
            }
            started = true;
        }
        Thread push = new Thread(new PushLoop(), "pos-sync-push");
        push.setDaemon(true);
        push.start();
        Thread pull = new Thread(new PullLoop(), "pos-sync-pull");
        pull.setDaemon(true);
        pull.start();
        LOG.info("SyncService started");
    }

    /** No-op — push/pull re-read the token from sync_state on every cycle. */
    public void notifyTokenRefreshed() {
    }

    // ---- Status for header indicator / settings UI ----

    public enum Indicator {
        SYNCED, PENDING, OFFLINE, DISABLED
    }

    public static final class StatusSnapshot {
        public final Indicator indicator;
        public final int outboxDepth;
        public final String lastPullAt;
        public final String label;

        StatusSnapshot(Indicator indicator, int outboxDepth, String lastPullAt, String label) {
            this.indicator = indicator;
            this.outboxDepth = outboxDepth;
            this.lastPullAt = lastPullAt;
            this.label = label;
        }
    }

    public StatusSnapshot getStatusSnapshot() {
        int depth = 0;
        String lastPull = null;
        try {
            depth = getOutboxDepth();
            lastPull = getSyncState("last_pull_at");
        } catch (Exception e) {
            LOG.log(Level.FINE, "status read failed", e);
        }
        if (!Settings.getBool("sync_enabled", false)) {
            return new StatusSnapshot(Indicator.DISABLED, depth, lastPull, "Sync off");
        }
        long now = System.currentTimeMillis();
        boolean recentOk = lastPushSuccessAtMs > 0 && (now - lastPushSuccessAtMs) <= 120_000L;
        boolean failed = lastPushHadNetworkError
                || (lastPushFailureAtMs > lastPushSuccessAtMs && lastPushFailureAtMs > 0);
        if (failed && !recentOk) {
            return new StatusSnapshot(Indicator.OFFLINE, depth, lastPull, "Offline");
        }
        if (depth > 0) {
            return new StatusSnapshot(Indicator.PENDING, depth, lastPull, "Pending (" + depth + ")");
        }
        if (recentOk) {
            return new StatusSnapshot(Indicator.SYNCED, depth, lastPull, "Synced");
        }
        return new StatusSnapshot(Indicator.OFFLINE, depth, lastPull, "Offline");
    }

    public int getOutboxDepth() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = openConnection();
            ps = conn.prepareStatement("SELECT COUNT(*) FROM sync_outbox");
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "outbox depth failed", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return 0;
    }

    public String getSyncState(String key) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = openConnection();
            ps = conn.prepareStatement("SELECT state_value FROM sync_state WHERE state_key = ?");
            ps.setString(1, key);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "getSyncState failed", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return null;
    }

    public void setSyncState(String key, String value) throws Exception {
        Connection conn = openConnection();
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(
                    "INSERT INTO sync_state (state_key, state_value) VALUES (?,?) "
                    + "ON DUPLICATE KEY UPDATE state_value = VALUES(state_value)");
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    /**
     * Enqueue a payload for cloud push. Never throws to the caller — logs on failure.
     * Safe to call after a committed sale/void; must not roll back till work.
     */
    public void enqueue(String entityType, String entityUuid, String payload) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = openConnection();
            ps = conn.prepareStatement(
                    "INSERT INTO sync_outbox (entity_type, entity_uuid, payload) VALUES (?,?,?) "
                    + "ON DUPLICATE KEY UPDATE payload = VALUES(payload), entity_type = VALUES(entity_type)");
            ps.setString(1, entityType);
            ps.setString(2, entityUuid);
            ps.setString(3, payload);
            ps.executeUpdate();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "sync outbox write failed (sale/void still local): " + e.getMessage(), e);
        } finally {
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    /** Like enqueue but INSERT IGNORE — does not overwrite existing outbox rows. */
    public void enqueueIgnore(String entityType, String entityUuid, String payload) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = openConnection();
            ps = conn.prepareStatement(
                    "INSERT IGNORE INTO sync_outbox (entity_type, entity_uuid, payload) VALUES (?,?,?)");
            ps.setString(1, entityType);
            ps.setString(2, entityUuid);
            ps.setString(3, payload);
            ps.executeUpdate();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "sync outbox ignore write failed: " + e.getMessage(), e);
        } finally {
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    /** GET {base}/api/health — returns null on success, or error message. */
    public String testConnection(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return "API URL is empty";
        }
        String url = trimSlash(baseUrl.trim()) + "/api/health";
        HttpURLConnection http = null;
        try {
            http = openGet(url, null);
            int code = http.getResponseCode();
            String body = readBody(http);
            if (code >= 200 && code < 300) {
                resetFailedOutboxAttempts();
                return null;
            }
            return "HTTP " + code + (body == null || body.isEmpty() ? "" : ": " + truncate(body, 120));
        } catch (Exception e) {
            return e.getMessage() == null ? e.toString() : e.getMessage();
        } finally {
            if (http != null) {
                http.disconnect();
            }
        }
    }

    /** Reset parked outbox rows (attempts >= MAX_ATTEMPTS) so push retries them. */
    public int resetFailedOutboxAttempts() {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = openConnection();
            ps = conn.prepareStatement(
                    "UPDATE sync_outbox SET attempts = 0, last_error = NULL "
                    + "WHERE attempts >= ?");
            ps.setInt(1, MAX_ATTEMPTS);
            return ps.executeUpdate();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "resetFailedOutboxAttempts failed: " + e.getMessage(), e);
            return 0;
        } finally {
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    // ---- JSON helpers (no external library) ----

    public static String jsonString(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(s.length() + 8);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    public static String jsonNumber(Number n) {
        return n == null ? "null" : n.toString();
    }

    // ---- Internals ----

    private Connection openConnection() throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
    }

    private static String trimSlash(String base) {
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    private static String pathForEntity(String entityType) {
        if (entityType == null) {
            return "/api/sync/unknown";
        }
        String t = entityType.trim().toLowerCase();
        if ("sale".equals(t)) {
            return "/api/sync/sales";
        }
        if ("customer".equals(t)) {
            return "/api/sync/customers";
        }
        if ("product".equals(t)) {
            return "/api/sync/products";
        }
        if ("category".equals(t)) {
            return "/api/sync/categories";
        }
        if ("supplier".equals(t)) {
            return "/api/sync/suppliers";
        }
        if ("unit".equals(t)) {
            return "/api/sync/units";
        }
        if ("purchase".equals(t)) {
            return "/api/sync/purchases";
        }
        if ("expense".equals(t)) {
            return "/api/sync/expenses";
        }
        if ("user".equals(t)) {
            return "/api/sync/users";
        }
        return "/api/sync/" + t;
    }

    private boolean isSyncReady(String[] baseAndTokenOut) {
        if (!Settings.getBool("sync_enabled", false)) {
            return false;
        }
        String base = null;
        String token = null;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = openConnection();
            ps = conn.prepareStatement(
                    "SELECT state_key, state_value FROM sync_state "
                    + "WHERE state_key IN ('api_base_url','api_token')");
            rs = ps.executeQuery();
            while (rs.next()) {
                String k = rs.getString(1);
                String v = rs.getString(2);
                if ("api_base_url".equals(k)) {
                    base = v;
                } else if ("api_token".equals(k)) {
                    token = v;
                }
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "sync config read failed", e);
            return false;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        if (base == null || base.trim().isEmpty()) {
            return false;
        }
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        baseAndTokenOut[0] = trimSlash(base.trim());
        baseAndTokenOut[1] = token.trim();
        return true;
    }

    private void authRetryWait() {
        try {
            Thread.sleep(AUTH_RETRY_WAIT_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private final class PushLoop implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(PUSH_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                try {
                    runPushCycle();
                } catch (Throwable t) {
                    lastPushHadNetworkError = true;
                    lastPushFailureAtMs = System.currentTimeMillis();
                    LOG.log(Level.WARNING, "push cycle error: " + t.getMessage(), t);
                }
            }
        }
    }

    private final class PullLoop implements Runnable {
        @Override
        public void run() {
            // Run once immediately so pull logs appear without waiting a full interval.
            try {
                runPullCycle();
            } catch (Throwable t) {
                System.err.println("[sync-pull] cycle exception: " + t);
                t.printStackTrace(System.err);
                LOG.log(Level.WARNING, "pull cycle error: " + t.getMessage(), t);
            }
            while (true) {
                try {
                    Thread.sleep(PULL_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                try {
                    runPullCycle();
                } catch (Throwable t) {
                    System.err.println("[sync-pull] cycle exception: " + t);
                    t.printStackTrace(System.err);
                    LOG.log(Level.WARNING, "pull cycle error: " + t.getMessage(), t);
                }
            }
        }
    }

    private void runPushCycle() {
        String[] cfg = new String[2];
        if (!isSyncReady(cfg)) {
            return;
        }
        String base = cfg[0];
        String token = cfg[1];

        Connection conn = null;
        PreparedStatement selectPs = null;
        ResultSet rs = null;
        try {
            conn = openConnection();
            selectPs = conn.prepareStatement(
                    "SELECT outbox_Id, entity_type, entity_uuid, payload, attempts "
                    + "FROM sync_outbox ORDER BY outbox_Id LIMIT ?");
            selectPs.setInt(1, BATCH_SIZE);
            rs = selectPs.executeQuery();
            List<OutboxRow> rows = new ArrayList<OutboxRow>();
            while (rs.next()) {
                OutboxRow row = new OutboxRow();
                row.id = rs.getInt("outbox_Id");
                row.entityType = rs.getString("entity_type");
                row.entityUuid = rs.getString("entity_uuid");
                row.payload = rs.getString("payload");
                row.attempts = rs.getInt("attempts");
                rows.add(row);
            }
            closeQuietly(rs);
            rs = null;
            closeQuietly(selectPs);
            selectPs = null;

            boolean anyNetworkError = false;
            boolean anySuccess = false;

            for (OutboxRow row : rows) {
                if (row.attempts >= MAX_ATTEMPTS) {
                    continue;
                }
                String[] freshCfg = new String[2];
                if (!isSyncReady(freshCfg)) {
                    break;
                }
                base = freshCfg[0];
                token = freshCfg[1];
                String url = base + pathForEntity(row.entityType);
                HttpURLConnection http = null;
                try {
                    http = openPost(url, token, row.payload);
                    int code = http.getResponseCode();
                    String body = readBody(http);
                    // TEMP debug — keep until we see what the API returns
                    System.out.println("[sync-push] url=" + url
                            + " code=" + code
                            + " body=" + truncate(body == null ? "" : body, 500));
                    if (code == 200 || code == 201 || containsAlreadySynced(body)) {
                        deleteOutbox(conn, row.id);
                        anySuccess = true;
                    } else {
                        recordPushFailure(conn, row.id, url, row.payload, code, body);
                        anyNetworkError = true;
                        if (code == 401) {
                            LOG.warning("push 401 — token rejected, retrying in 60s after re-reading token");
                            authRetryWait();
                            break;
                        }
                    }
                } catch (Exception net) {
                    bumpAttempt(conn, row.id, null);
                    anyNetworkError = true;
                    System.err.println("[sync-push FAIL] outbox_Id=" + row.id
                            + " url=" + url
                            + " EXCEPTION=" + net.getClass().getName()
                            + ": " + net.getMessage());
                    System.err.println("[sync-push FAIL] payload=" + row.payload);
                    LOG.log(Level.FINE, "push network error: " + net.getMessage(), net);
                } finally {
                    if (http != null) {
                        http.disconnect();
                    }
                }
            }

            if (anySuccess) {
                lastPushSuccessAtMs = System.currentTimeMillis();
                lastPushHadNetworkError = false;
            }
            if (anyNetworkError) {
                lastPushFailureAtMs = System.currentTimeMillis();
                lastPushHadNetworkError = true;
            } else if (rows.isEmpty() || anySuccess) {
                // Empty queue and no error counts as a healthy push cycle
                lastPushSuccessAtMs = System.currentTimeMillis();
                lastPushHadNetworkError = false;
            }
        } catch (Exception e) {
            lastPushHadNetworkError = true;
            lastPushFailureAtMs = System.currentTimeMillis();
            LOG.log(Level.WARNING, "push cycle failed: " + e.getMessage(), e);
        } finally {
            closeQuietly(rs);
            closeQuietly(selectPs);
            closeQuietly(conn);
        }
    }

    private void runPullCycle() {
        String[] cfg = new String[2];
        if (!isSyncReady(cfg)) {
            return;
        }
        String base = cfg[0];
        String token = cfg[1];

        String since = null;
        Connection conn = null;
        try {
            conn = openConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT state_value FROM sync_state WHERE state_key = 'last_pull_at'");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                since = rs.getString(1);
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            closeQuietly(conn);
            System.err.println("[sync-pull] exception reading last_pull_at: " + e);
            e.printStackTrace(System.err);
            LOG.log(Level.WARNING, "pull: could not read last_pull_at", e);
            return;
        }

        String url = base + "/api/sync/changes";
        if (since != null && !since.trim().isEmpty()) {
            try {
                url = url + "?since=" + URLEncoder.encode(since.trim(), "UTF-8");
            } catch (Exception e) {
                url = url + "?since=" + since.trim();
            }
        }
        System.err.println("[sync-pull] GET " + url + " (since=" + since + ")");

        HttpURLConnection http = null;
        try {
            http = openGet(url, token);
            int code = http.getResponseCode();
            String body = readBody(http);
            System.err.println("[sync-pull] HTTP " + code + " bodyLen="
                    + (body == null ? 0 : body.length()));
            if (code == 401) {
                LOG.warning("pull 401 — token rejected, retrying in 60s after re-reading token");
                authRetryWait();
                return;
            }
            if (code < 200 || code >= 300) {
                System.err.println("[sync-pull] non-2xx body=" + (body == null ? "" : body));
                LOG.warning("pull HTTP " + code + ": " + truncate(body, 160));
                return;
            }
            applyPullPayload(conn, body);
        } catch (Exception e) {
            System.err.println("[sync-pull] exception: " + e);
            e.printStackTrace(System.err);
            LOG.log(Level.WARNING, "pull failed: " + e.getMessage(), e);
            // do not update last_pull_at
        } finally {
            if (http != null) {
                http.disconnect();
            }
            closeQuietly(conn);
        }
    }

    private void applyPullPayload(Connection conn, String body) throws Exception {
        if (body == null || body.trim().isEmpty()) {
            return;
        }
        String serverTime = extractJsonString(body, "serverTime");
        if (serverTime == null) {
            serverTime = extractJsonString(body, "server_time");
        }

        applyPulledCategories(conn, extractJsonArray(body, "categories"));
        applyPulledSuppliers(conn, extractJsonArray(body, "suppliers"));
        applyPulledCustomers(conn, extractJsonArray(body, "customers"));
        applyPulledProducts(conn, extractJsonArray(body, "products"));
        applyPulledUsers(conn, extractJsonArray(body, "users"));
        applyPulledSettings(conn, extractJsonArray(body, "settings"));

        if (serverTime != null && !serverTime.isEmpty()) {
            String watermark = overlapPullWatermark(serverTime);
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO sync_state (state_key, state_value) VALUES ('last_pull_at',?) "
                    + "ON DUPLICATE KEY UPDATE state_value = VALUES(state_value)");
            try {
                ps.setString(1, watermark);
                ps.executeUpdate();
            } finally {
                ps.close();
            }
        }
    }

    /** Save last_pull_at 5 minutes before serverTime so the next pull overlaps. */
    private static String overlapPullWatermark(String serverTime) {
        try {
            String normalized = serverTime.replace('T', ' ');
            int cut = Math.min(19, normalized.length());
            Timestamp ts = Timestamp.valueOf(normalized.substring(0, cut));
            ts.setTime(ts.getTime() - 5L * 60L * 1000L);
            return ts.toString().substring(0, 19);
        } catch (Exception e) {
            return serverTime;
        }
    }

    private void applyPulledSettings(Connection conn, String block) throws Exception {
        if (block == null) {
            return;
        }
        List<String> objs = splitJsonObjects(block);
        PreparedStatement ups = conn.prepareStatement(
                "UPDATE pengaturan SET setting_value = ? WHERE setting_key = ?");
        try {
            for (String obj : objs) {
                String key = firstNonNull(
                        extractJsonString(obj, "settingKey"),
                        extractJsonString(obj, "setting_key"),
                        extractJsonString(obj, "key"));
                String value = firstNonNull(
                        extractJsonString(obj, "settingValue"),
                        extractJsonString(obj, "setting_value"),
                        extractJsonString(obj, "value"));
                if (key == null) {
                    continue;
                }
                ups.setString(1, value == null ? "" : value);
                ups.setString(2, key);
                ups.executeUpdate();
            }
        } finally {
            ups.close();
        }
    }

    private void applyPulledProducts(Connection conn, String block) throws Exception {
        if (block == null) {
            System.err.println("[sync-pull products] block=null (no products array)");
            return;
        }
        List<String> objs = splitJsonObjects(block);
        System.err.println("[sync-pull products] count=" + objs.size());
        for (String obj : objs) {
            String uuid = extractJsonString(obj, "uuid");
            System.err.println("[sync-pull products] processing uuid=" + uuid);
            try {
                String kode = firstNonNull(
                        extractJsonRawNumber(obj, "kodeProduk"),
                        extractJsonString(obj, "kodeProduk"),
                        extractJsonString(obj, "kode_produk"));
                String nama = firstNonNull(
                        extractJsonString(obj, "namaProduk"),
                        extractJsonString(obj, "nama_produk"));
                String hj = firstNonNull(
                        extractJsonRawNumber(obj, "hargaJual"),
                        extractJsonRawNumber(obj, "harga_jual"));
                String hb = firstNonNull(
                        extractJsonRawNumber(obj, "hargaBeli"),
                        extractJsonRawNumber(obj, "harga_beli"));
                String stok = firstNonNull(
                        extractJsonString(obj, "stokProduk"),
                        extractJsonRawNumber(obj, "stokProduk"),
                        extractJsonString(obj, "stok_produk"));
                String updated = firstNonNull(
                        extractJsonString(obj, "updatedAt"),
                        extractJsonString(obj, "updated_at"));
                String isScaleRaw = firstNonNull(
                        extractJsonRawNumber(obj, "isScale"),
                        extractJsonRawNumber(obj, "is_scale"));
                int isScale = isScaleRaw == null ? 0 : parseIntSafe(isScaleRaw);
                if (uuid == null || kode == null || updated == null) {
                    System.err.println("[sync-pull products] SKIP uuid=" + uuid
                            + " (missing uuid/kode/updatedAt)");
                    continue;
                }
                Integer katId = resolveIdByUuid(conn, "kategori", "kategori_Id",
                        firstNonNull(extractJsonString(obj, "kategoriUuid"), extractJsonString(obj, "kategori_uuid")));
                Integer supId = resolveIdByUuid(conn, "supplier", "supplier_Id",
                        firstNonNull(extractJsonString(obj, "supplierUuid"), extractJsonString(obj, "supplier_uuid")));
                Integer satId = resolveIdByUuid(conn, "satuan", "satuan_Id",
                        firstNonNull(extractJsonString(obj, "satuanUuid"), extractJsonString(obj, "satuan_uuid")));
                String merekRaw = firstNonNull(
                        extractJsonRawNumber(obj, "merekId"),
                        extractJsonRawNumber(obj, "merek_Id"));

                PreparedStatement find = conn.prepareStatement(
                        "SELECT kode_produk, updated_at FROM produk WHERE uuid = ?");
                ResultSet frs = null;
                try {
                    find.setString(1, uuid);
                    frs = find.executeQuery();
                    if (frs.next()) {
                        Timestamp localTs = frs.getTimestamp("updated_at");
                        if (localTs != null && !isIncomingNewer(updated, localTs)) {
                            System.err.println("[sync-pull products] SKIP uuid=" + uuid
                                    + " (local updated_at not older; local=" + localTs
                                    + " incoming=" + updated + ")");
                            continue;
                        }
                        PreparedStatement ups = conn.prepareStatement(
                                "UPDATE produk SET nama_produk = ?, harga_jual = ?, harga_beli = ?, "
                                + "stok_produk = COALESCE(?, stok_produk), "
                                + "kategori_Id = COALESCE(?, kategori_Id), "
                                + "supplier_Id = COALESCE(?, supplier_Id), "
                                + "satuan_Id = COALESCE(?, satuan_Id), "
                                + "merek_Id = COALESCE(?, merek_Id), "
                                + "is_scale = ?, "
                                + "updated_at = ? WHERE uuid = ? AND updated_at < ?");
                        try {
                            ups.setString(1, nama == null ? "" : nama);
                            ups.setInt(2, parseIntSafe(hj));
                            ups.setInt(3, parseIntSafe(hb));
                            if (stok == null) {
                                ups.setNull(4, java.sql.Types.DECIMAL);
                            } else {
                                ups.setBigDecimal(4, new java.math.BigDecimal(stok));
                            }
                            setNullableInt(ups, 5, katId);
                            setNullableInt(ups, 6, supId);
                            setNullableInt(ups, 7, satId);
                            setNullableInt(ups, 8, merekRaw == null ? null : Integer.valueOf(parseIntSafe(merekRaw)));
                            ups.setInt(9, isScale);
                            ups.setString(10, updated);
                            ups.setString(11, uuid);
                            ups.setString(12, updated);
                            int n = ups.executeUpdate();
                            System.err.println("[sync-pull products] UPDATE uuid=" + uuid
                                    + " rows=" + n
                                    + " katId=" + katId + " supId=" + supId + " satId=" + satId);
                        } catch (SQLException upsEx) {
                            System.err.println("[sync-pull products] UPDATE failed uuid=" + uuid
                                    + " exception=" + upsEx);
                            upsEx.printStackTrace(System.err);
                        } finally {
                            ups.close();
                        }
                    } else {
                        // Insert if missing (admin-created product)
                        PreparedStatement ins = conn.prepareStatement(
                                "INSERT INTO produk (kode_produk, nama_produk, harga_beli, harga_jual, stok_produk, "
                                + "kategori_Id, merek_Id, supplier_Id, satuan_Id, is_scale, uuid, updated_at) "
                                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
                        try {
                            ins.setString(1, kode);
                            ins.setString(2, nama == null ? "" : nama);
                            ins.setInt(3, parseIntSafe(hb));
                            ins.setInt(4, parseIntSafe(hj));
                            ins.setBigDecimal(5, stok == null
                                    ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(stok));
                            setNullableInt(ins, 6, katId);
                            setNullableInt(ins, 7, merekRaw == null ? null : Integer.valueOf(parseIntSafe(merekRaw)));
                            setNullableInt(ins, 8, supId);
                            setNullableInt(ins, 9, satId);
                            ins.setInt(10, isScale);
                            ins.setString(11, uuid);
                            ins.setString(12, updated);
                            ins.executeUpdate();
                            System.err.println("[sync-pull products] INSERT uuid=" + uuid
                                    + " kode=" + kode
                                    + " katId=" + katId + " supId=" + supId + " satId=" + satId
                                    + " merekId=" + merekRaw);
                        } catch (SQLException ignoreDup) {
                            System.err.println("[sync-pull products] INSERT failed uuid=" + uuid
                                    + " exception=" + ignoreDup);
                            ignoreDup.printStackTrace(System.err);
                        } finally {
                            ins.close();
                        }
                    }
                } finally {
                    closeQuietly(frs);
                    find.close();
                }
            } catch (Exception ex) {
                System.err.println("[sync-pull products] exception uuid=" + uuid
                        + " message=" + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
    }

    private void applyPulledCategories(Connection conn, String block) throws Exception {
        if (block == null) {
            return;
        }
        for (String obj : splitJsonObjects(block)) {
            String uuid = extractJsonString(obj, "uuid");
            String nama = null;
            try {
                nama = firstNonNull(
                        extractJsonString(obj, "namaKategori"),
                        extractJsonString(obj, "nama_kategori"));
                String noRak = firstNonNull(
                        extractJsonString(obj, "noRak"),
                        extractJsonString(obj, "no_rak"));
                String updated = firstNonNull(
                        extractJsonString(obj, "updatedAt"),
                        extractJsonString(obj, "updated_at"));
                System.err.println("[sync-pull categories] processing uuid=" + uuid
                        + " nama=" + nama);
                if (uuid == null || nama == null || updated == null) {
                    System.err.println("[sync-pull categories] SKIP uuid=" + uuid
                            + " nama=" + nama + " (missing uuid/nama/updated)");
                    continue;
                }
                PreparedStatement find = conn.prepareStatement(
                        "SELECT kategori_Id, updated_at FROM kategori WHERE uuid = ?");
                ResultSet frs = null;
                try {
                    find.setString(1, uuid);
                    frs = find.executeQuery();
                    if (frs.next()) {
                        Timestamp localTs = frs.getTimestamp("updated_at");
                        if (localTs != null && !isIncomingNewer(updated, localTs)) {
                            System.err.println("[sync-pull categories] SKIP uuid=" + uuid
                                    + " nama=" + nama
                                    + " (local updated_at not older; local=" + localTs
                                    + " incoming=" + updated + ")");
                            continue;
                        }
                        PreparedStatement ups = conn.prepareStatement(
                                "UPDATE kategori SET nama_kategori = ?, no_rak = ?, updated_at = ? "
                                + "WHERE uuid = ? AND updated_at < ?");
                        try {
                            ups.setString(1, nama);
                            ups.setString(2, noRak == null ? "0" : noRak);
                            ups.setString(3, updated);
                            ups.setString(4, uuid);
                            ups.setString(5, updated);
                            int n = ups.executeUpdate();
                            System.err.println("[sync-pull categories] UPDATE uuid=" + uuid
                                    + " nama=" + nama + " rows=" + n);
                        } catch (SQLException upsEx) {
                            System.err.println("[sync-pull categories] UPDATE failed uuid=" + uuid
                                    + " nama=" + nama + " exception=" + upsEx);
                            upsEx.printStackTrace(System.err);
                        } finally {
                            ups.close();
                        }
                    } else {
                        PreparedStatement ins = conn.prepareStatement(
                                "INSERT INTO kategori (nama_kategori, no_rak, uuid, updated_at) VALUES (?,?,?,?)");
                        try {
                            ins.setString(1, nama);
                            ins.setString(2, noRak == null ? "0" : noRak);
                            ins.setString(3, uuid);
                            ins.setString(4, updated);
                            ins.executeUpdate();
                            System.err.println("[sync-pull categories] INSERT uuid=" + uuid
                                    + " nama=" + nama);
                        } catch (SQLException insEx) {
                            System.err.println("[sync-pull categories] INSERT failed uuid=" + uuid
                                    + " nama=" + nama + " exception=" + insEx);
                            insEx.printStackTrace(System.err);
                        } finally {
                            ins.close();
                        }
                    }
                } finally {
                    closeQuietly(frs);
                    find.close();
                }
            } catch (Exception ex) {
                System.err.println("[sync-pull categories] FAILED uuid=" + uuid
                        + " nama=" + nama + " exception=" + ex);
                ex.printStackTrace(System.err);
            }
        }
    }

    private void applyPulledSuppliers(Connection conn, String block) throws Exception {
        if (block == null) {
            return;
        }
        for (String obj : splitJsonObjects(block)) {
            String uuid = extractJsonString(obj, "uuid");
            try {
                String nama = firstNonNull(
                        extractJsonString(obj, "namaSupplier"),
                        extractJsonString(obj, "nama_supplier"));
                String alamat = firstNonNull(
                        extractJsonString(obj, "alamatSupplier"),
                        extractJsonString(obj, "alamat_supplier"));
                String telp = firstNonNull(
                        extractJsonString(obj, "telpSupplier"),
                        extractJsonString(obj, "telp_supplier"));
                String updated = firstNonNull(
                        extractJsonString(obj, "updatedAt"),
                        extractJsonString(obj, "updated_at"));
                if (uuid == null || nama == null || updated == null) {
                    System.err.println("[sync-pull suppliers] SKIP uuid=" + uuid);
                    continue;
                }
                PreparedStatement find = conn.prepareStatement(
                        "SELECT supplier_Id, updated_at FROM supplier WHERE uuid = ?");
                ResultSet frs = null;
                try {
                    find.setString(1, uuid);
                    frs = find.executeQuery();
                    if (frs.next()) {
                        Timestamp localTs = frs.getTimestamp("updated_at");
                        if (localTs != null && !isIncomingNewer(updated, localTs)) {
                            continue;
                        }
                        PreparedStatement ups = conn.prepareStatement(
                                "UPDATE supplier SET nama_supplier = ?, alamat_supplier = ?, telp_supplier = ?, "
                                + "updated_at = ? WHERE uuid = ? AND updated_at < ?");
                        try {
                            ups.setString(1, nama);
                            ups.setString(2, alamat == null ? "" : alamat);
                            ups.setString(3, telp == null ? "" : telp);
                            ups.setString(4, updated);
                            ups.setString(5, uuid);
                            ups.setString(6, updated);
                            ups.executeUpdate();
                            System.err.println("[sync-pull suppliers] UPDATE uuid=" + uuid);
                        } catch (SQLException upsEx) {
                            System.err.println("[sync-pull suppliers] UPDATE failed uuid=" + uuid
                                    + " exception=" + upsEx);
                            upsEx.printStackTrace(System.err);
                        } finally {
                            ups.close();
                        }
                    } else {
                        PreparedStatement ins = conn.prepareStatement(
                                "INSERT INTO supplier (nama_supplier, alamat_supplier, telp_supplier, uuid, updated_at) "
                                + "VALUES (?,?,?,?,?)");
                        try {
                            ins.setString(1, nama);
                            ins.setString(2, alamat == null ? "" : alamat);
                            ins.setString(3, telp == null ? "" : telp);
                            ins.setString(4, uuid);
                            ins.setString(5, updated);
                            ins.executeUpdate();
                            System.err.println("[sync-pull suppliers] INSERT uuid=" + uuid);
                        } catch (SQLException insEx) {
                            System.err.println("[sync-pull suppliers] INSERT failed uuid=" + uuid
                                    + " exception=" + insEx);
                            insEx.printStackTrace(System.err);
                        } finally {
                            ins.close();
                        }
                    }
                } finally {
                    closeQuietly(frs);
                    find.close();
                }
            } catch (Exception ex) {
                System.err.println("[sync-pull suppliers] exception uuid=" + uuid
                        + " message=" + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
    }

    private void applyPulledCustomers(Connection conn, String block) throws Exception {
        if (block == null) {
            return;
        }
        for (String obj : splitJsonObjects(block)) {
            String uuid = extractJsonString(obj, "uuid");
            try {
                String nama = firstNonNull(
                        extractJsonString(obj, "namaPelanggan"),
                        extractJsonString(obj, "nama_pelanggan"));
                String telp = firstNonNull(
                        extractJsonString(obj, "telpPelanggan"),
                        extractJsonString(obj, "telp_pelanggan"));
                String alamat = firstNonNull(
                        extractJsonString(obj, "alamatPelanggan"),
                        extractJsonString(obj, "alamat_pelanggan"));
                String updated = firstNonNull(
                        extractJsonString(obj, "updatedAt"),
                        extractJsonString(obj, "updated_at"));
                if (uuid == null || nama == null || updated == null) {
                    System.err.println("[sync-pull customers] SKIP uuid=" + uuid);
                    continue;
                }
                PreparedStatement find = conn.prepareStatement(
                        "SELECT pelanggan_Id, updated_at FROM pelanggan WHERE uuid = ?");
                ResultSet frs = null;
                try {
                    find.setString(1, uuid);
                    frs = find.executeQuery();
                    if (frs.next()) {
                        Timestamp localTs = frs.getTimestamp("updated_at");
                        if (localTs != null && !isIncomingNewer(updated, localTs)) {
                            continue;
                        }
                        PreparedStatement ups = conn.prepareStatement(
                                "UPDATE pelanggan SET nama_pelanggan = ?, telp_pelanggan = ?, alamat_pelanggan = ?, "
                                + "updated_at = ? WHERE uuid = ? AND updated_at < ?");
                        try {
                            ups.setString(1, nama);
                            ups.setString(2, telp);
                            ups.setString(3, alamat);
                            ups.setString(4, updated);
                            ups.setString(5, uuid);
                            ups.setString(6, updated);
                            ups.executeUpdate();
                            System.err.println("[sync-pull customers] UPDATE uuid=" + uuid);
                        } catch (SQLException upsEx) {
                            System.err.println("[sync-pull customers] UPDATE failed uuid=" + uuid
                                    + " exception=" + upsEx);
                            upsEx.printStackTrace(System.err);
                        } finally {
                            ups.close();
                        }
                    } else {
                        PreparedStatement ins = conn.prepareStatement(
                                "INSERT INTO pelanggan (nama_pelanggan, telp_pelanggan, alamat_pelanggan, uuid, updated_at) "
                                + "VALUES (?,?,?,?,?)");
                        try {
                            ins.setString(1, nama);
                            ins.setString(2, telp);
                            ins.setString(3, alamat);
                            ins.setString(4, uuid);
                            ins.setString(5, updated);
                            ins.executeUpdate();
                            System.err.println("[sync-pull customers] INSERT uuid=" + uuid);
                        } catch (SQLException insEx) {
                            System.err.println("[sync-pull customers] INSERT failed uuid=" + uuid
                                    + " exception=" + insEx);
                            insEx.printStackTrace(System.err);
                        } finally {
                            ins.close();
                        }
                    }
                } finally {
                    closeQuietly(frs);
                    find.close();
                }
            } catch (Exception ex) {
                System.err.println("[sync-pull customers] exception uuid=" + uuid
                        + " message=" + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
    }

    private void applyPulledUsers(Connection conn, String block) throws Exception {
        if (block == null) {
            return;
        }
        for (String obj : splitJsonObjects(block)) {
            String uuid = extractJsonString(obj, "uuid");
            try {
                String nama = firstNonNull(
                        extractJsonString(obj, "namaUser"),
                        extractJsonString(obj, "nama_user"));
                String level = firstNonNull(
                        extractJsonString(obj, "levelUser"),
                        extractJsonString(obj, "level_user"));
                String status = firstNonNull(
                        extractJsonString(obj, "statusUser"),
                        extractJsonString(obj, "status_user"));
                String username = firstNonNull(
                        extractJsonString(obj, "usernameUser"),
                        extractJsonString(obj, "username_user"));
                String passwordHash = firstNonNull(
                        extractJsonString(obj, "passwordHash"),
                        extractJsonString(obj, "password_hash"),
                        extractJsonString(obj, "password_user"));
                String updated = firstNonNull(
                        extractJsonString(obj, "updatedAt"),
                        extractJsonString(obj, "updated_at"));
                if (uuid == null || updated == null) {
                    System.err.println("[sync-pull users] SKIP uuid=" + uuid);
                    continue;
                }
                // bcryptjs uses $2b$; jBCrypt verifies $2a$ (compatible prefix)
                if (passwordHash != null && passwordHash.startsWith("$2b$")) {
                    passwordHash = "$2a$" + passwordHash.substring(4);
                }
                boolean hasPassword = passwordHash != null && !passwordHash.trim().isEmpty();
                PreparedStatement find = null;
                ResultSet frs = null;
                try {
                    find = conn.prepareStatement(
                            "SELECT user_Id, updated_at FROM users WHERE uuid = ?");
                    find.setString(1, uuid);
                    frs = find.executeQuery();
                    if (frs.next()) {
                        Timestamp localTs = frs.getTimestamp("updated_at");
                        boolean incomingNewer = localTs == null || isIncomingNewer(updated, localTs);
                        if (!incomingNewer) {
                            // Skip metadata update, but always apply password when present
                            if (hasPassword) {
                                PreparedStatement pwdUps = conn.prepareStatement(
                                        "UPDATE users SET password_user = ? WHERE uuid = ?");
                                try {
                                    pwdUps.setString(1, passwordHash);
                                    pwdUps.setString(2, uuid);
                                    pwdUps.executeUpdate();
                                    System.err.println("[sync-pull users] UPDATE uuid=" + uuid
                                            + " password=set (forced; local updated_at not older)");
                                } catch (SQLException upsEx) {
                                    System.err.println("[sync-pull users] UPDATE failed uuid=" + uuid
                                            + " exception=" + upsEx);
                                    upsEx.printStackTrace(System.err);
                                } finally {
                                    pwdUps.close();
                                }
                            }
                            continue;
                        }
                        PreparedStatement ups;
                        if (hasPassword) {
                            ups = conn.prepareStatement(
                                    "UPDATE users SET nama_user = ?, level_user = ?, status_user = ?, "
                                    + "password_user = ?, updated_at = ? "
                                    + "WHERE uuid = ? AND updated_at < ?");
                        } else {
                            ups = conn.prepareStatement(
                                    "UPDATE users SET nama_user = ?, level_user = ?, status_user = ?, updated_at = ? "
                                    + "WHERE uuid = ? AND updated_at < ?");
                        }
                        try {
                            ups.setString(1, nama == null ? "" : nama);
                            ups.setString(2, level == null ? "KARYAWAN" : level);
                            ups.setString(3, status == null ? "AKTIF" : status);
                            if (hasPassword) {
                                ups.setString(4, passwordHash);
                                ups.setString(5, updated);
                                ups.setString(6, uuid);
                                ups.setString(7, updated);
                            } else {
                                ups.setString(4, updated);
                                ups.setString(5, uuid);
                                ups.setString(6, updated);
                            }
                            ups.executeUpdate();
                            System.err.println("[sync-pull users] UPDATE uuid=" + uuid
                                    + " password=" + (hasPassword ? "set" : "unchanged"));
                        } catch (SQLException upsEx) {
                            System.err.println("[sync-pull users] UPDATE failed uuid=" + uuid
                                    + " exception=" + upsEx);
                            upsEx.printStackTrace(System.err);
                        } finally {
                            ups.close();
                        }
                    } else {
                        String uname = username == null || username.isEmpty()
                                ? ("u_" + uuid.substring(0, 8)) : username;
                        String pwd = hasPassword
                                ? passwordHash : SyncOutbox.placeholderPasswordHash();
                        PreparedStatement ins = conn.prepareStatement(
                                "INSERT INTO users (nama_user, alamat_user, telp_user, username_user, "
                                + "password_user, level_user, status_user, uuid, updated_at) "
                                + "VALUES (?,?,?,?,?,?,?,?,?)");
                        try {
                            ins.setString(1, nama == null ? uname : nama);
                            ins.setString(2, "");
                            ins.setString(3, "");
                            ins.setString(4, uname);
                            ins.setString(5, pwd);
                            ins.setString(6, level == null ? "KARYAWAN" : level);
                            ins.setString(7, status == null ? "AKTIF" : status);
                            ins.setString(8, uuid);
                            ins.setString(9, updated);
                            ins.executeUpdate();
                            System.err.println("[sync-pull users] INSERT uuid=" + uuid
                                    + " password=" + (hasPassword ? "from-payload" : "placeholder"));
                        } catch (SQLException insEx) {
                            System.err.println("[sync-pull users] INSERT failed uuid=" + uuid
                                    + " exception=" + insEx);
                            insEx.printStackTrace(System.err);
                        } finally {
                            ins.close();
                        }
                    }
                } catch (SQLException e) {
                    // users.uuid may be missing until migration_010
                    System.err.println("[sync-pull users] exception uuid=" + uuid
                            + " message=" + e.getMessage());
                    e.printStackTrace(System.err);
                } finally {
                    closeQuietly(frs);
                    closeQuietly(find);
                }
            } catch (Exception ex) {
                System.err.println("[sync-pull users] exception uuid=" + uuid
                        + " message=" + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
    }

    private static boolean isIncomingNewer(String incoming, Timestamp local) {
        if (incoming == null || local == null) {
            return true;
        }
        try {
            Timestamp in = Timestamp.valueOf(incoming.replace('T', ' ').substring(0,
                    Math.min(19, incoming.replace('T', ' ').length())));
            return in.after(local);
        } catch (Exception e) {
            return true;
        }
    }

    private static Integer resolveIdByUuid(Connection conn, String table, String idCol, String uuid)
            throws SQLException {
        if (uuid == null || uuid.trim().isEmpty()) {
            return null;
        }
        PreparedStatement ps = conn.prepareStatement(
                "SELECT " + idCol + " FROM " + table + " WHERE uuid = ? LIMIT 1");
        ResultSet rs = null;
        try {
            ps.setString(1, uuid.trim());
            rs = ps.executeQuery();
            if (rs.next()) {
                return Integer.valueOf(rs.getInt(1));
            }
        } finally {
            closeQuietly(rs);
            ps.close();
        }
        return null;
    }

    private static void setNullableInt(PreparedStatement ps, int idx, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(idx, java.sql.Types.INTEGER);
        } else {
            ps.setInt(idx, value.intValue());
        }
    }

    private void deleteOutbox(Connection conn, int id) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("DELETE FROM sync_outbox WHERE outbox_Id = ?");
        try {
            ps.setInt(1, id);
            ps.executeUpdate();
        } finally {
            ps.close();
        }
    }

    private void recordPushFailure(Connection conn, int outboxId, String url,
            String payload, int code, String body) {
        String bodyStr = body == null ? "" : body;
        System.err.println("[sync-push FAIL] outbox_Id=" + outboxId
                + " url=" + url
                + " code=" + code
                + " body=" + bodyStr);
        System.err.println("[sync-push FAIL] payload=" + (payload == null ? "" : payload));
        LOG.warning("push rejected HTTP " + code + " for outbox_Id=" + outboxId
                + ": " + truncate(bodyStr, 160));
        bumpAttempt(conn, outboxId, "HTTP " + code + "\n" + bodyStr);
    }

    private void bumpAttempt(Connection conn, int id, String lastError) {
        PreparedStatement ps = null;
        try {
            if (lastError != null) {
                ps = conn.prepareStatement(
                        "UPDATE sync_outbox SET attempts = attempts + 1, "
                        + "last_attempt = CURRENT_TIMESTAMP, last_error = ? "
                        + "WHERE outbox_Id = ?");
                ps.setString(1, lastError);
                ps.setInt(2, id);
            } else {
                ps = conn.prepareStatement(
                        "UPDATE sync_outbox SET attempts = attempts + 1, "
                        + "last_attempt = CURRENT_TIMESTAMP "
                        + "WHERE outbox_Id = ?");
                ps.setInt(1, id);
            }
            ps.executeUpdate();
        } catch (Exception e) {
            LOG.log(Level.FINE, "bumpAttempt failed", e);
        } finally {
            closeQuietly(ps);
        }
    }

    private static boolean containsAlreadySynced(String body) {
        if (body == null) {
            return false;
        }
        String lower = body.toLowerCase();
        return lower.contains("already_synced") || lower.contains("\"alreadySynced\":true");
    }

    private HttpURLConnection openPost(String urlStr, String bearer, String jsonBody) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setConnectTimeout(CONNECT_TIMEOUT_MS);
        http.setReadTimeout(READ_TIMEOUT_MS);
        http.setRequestMethod("POST");
        http.setDoOutput(true);
        http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        http.setRequestProperty("Accept", "application/json");
        if (bearer != null && !bearer.isEmpty()) {
            http.setRequestProperty("Authorization", "Bearer " + bearer);
        }
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        http.setFixedLengthStreamingMode(bytes.length);
        OutputStream os = http.getOutputStream();
        try {
            os.write(bytes);
            os.flush();
        } finally {
            os.close();
        }
        return http;
    }

    private HttpURLConnection openGet(String urlStr, String bearer) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setConnectTimeout(CONNECT_TIMEOUT_MS);
        http.setReadTimeout(READ_TIMEOUT_MS);
        http.setRequestMethod("GET");
        http.setRequestProperty("Accept", "application/json");
        if (bearer != null && !bearer.isEmpty()) {
            http.setRequestProperty("Authorization", "Bearer " + bearer);
        }
        return http;
    }

    private static String readBody(HttpURLConnection http) {
        InputStream in = null;
        try {
            int code = http.getResponseCode();
            in = code >= 400 ? http.getErrorStream() : http.getInputStream();
            if (in == null) {
                return "";
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            return sb.toString();
        } catch (Exception e) {
            return "";
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static int parseIntSafe(String s) {
        try {
            if (s.indexOf('.') >= 0) {
                return (int) Double.parseDouble(s);
            }
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static String firstNonNull(String... vals) {
        if (vals == null) {
            return null;
        }
        for (String v : vals) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    /** Extract a JSON string value for key (handles escaped quotes simply). */
    static String extractJsonString(String json, String key) {
        if (json == null) {
            return null;
        }
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) {
            return null;
        }
        int colon = json.indexOf(':', idx + pattern.length());
        if (colon < 0) {
            return null;
        }
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        if (i >= json.length()) {
            return null;
        }
        if (json.charAt(i) == 'n'
                && json.regionMatches(i, "null", 0, 4)) {
            return null;
        }
        if (json.charAt(i) != '"') {
            return null;
        }
        i++;
        StringBuilder sb = new StringBuilder();
        while (i < json.length()) {
            char c = json.charAt(i++);
            if (c == '\\' && i < json.length()) {
                char n = json.charAt(i++);
                switch (n) {
                    case '"':
                    case '\\':
                    case '/':
                        sb.append(n);
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        if (i + 3 < json.length()) {
                            try {
                                sb.append((char) Integer.parseInt(json.substring(i, i + 4), 16));
                            } catch (Exception ignored) {
                            }
                            i += 4;
                        }
                        break;
                    default:
                        sb.append(n);
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    static String extractJsonRawNumber(String json, String key) {
        if (json == null) {
            return null;
        }
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) {
            return null;
        }
        int colon = json.indexOf(':', idx + pattern.length());
        if (colon < 0) {
            return null;
        }
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        if (i < json.length() && json.charAt(i) == '"') {
            return extractJsonString(json, key);
        }
        int start = i;
        while (i < json.length()) {
            char c = json.charAt(i);
            if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                i++;
            } else {
                break;
            }
        }
        if (start == i) {
            return null;
        }
        return json.substring(start, i);
    }

    static String extractJsonArray(String json, String key) {
        if (json == null) {
            return null;
        }
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) {
            return null;
        }
        int colon = json.indexOf(':', idx + pattern.length());
        if (colon < 0) {
            return null;
        }
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        if (i >= json.length() || json.charAt(i) != '[') {
            return null;
        }
        int depth = 0;
        int start = i;
        for (; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') {
                i++;
                while (i < json.length()) {
                    char d = json.charAt(i);
                    if (d == '\\') {
                        i += 2;
                        continue;
                    }
                    if (d == '"') {
                        break;
                    }
                    i++;
                }
                continue;
            }
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return json.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    static List<String> splitJsonObjects(String arrayJson) {
        List<String> list = new ArrayList<String>();
        if (arrayJson == null || arrayJson.length() < 2) {
            return list;
        }
        String inner = arrayJson.trim();
        if (inner.startsWith("[")) {
            inner = inner.substring(1);
        }
        if (inner.endsWith("]")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        int depth = 0;
        int start = -1;
        boolean inStr = false;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (inStr) {
                if (c == '\\') {
                    i++;
                    continue;
                }
                if (c == '"') {
                    inStr = false;
                }
                continue;
            }
            if (c == '"') {
                inStr = true;
                continue;
            }
            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    list.add(inner.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return list;
    }

    private static void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static void closeQuietly(PreparedStatement ps) {
        if (ps != null) {
            try {
                ps.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static final class OutboxRow {
        int id;
        String entityType;
        String entityUuid;
        String payload;
        int attempts;
    }

    /** One-shot pull for console debugging (no Swing). */
    /**
     * Debug one-shot. No args pulls; "push" drains the outbox once. Useful because
     * the app launches under javaw, which discards the [sync-push] diagnostics.
     */
    public static void main(String[] args) {
        boolean push = args != null && args.length > 0 && "push".equalsIgnoreCase(args[0]);
        String leg = push ? "sync-push" : "sync-pull";
        System.err.println("[" + leg + "] debug one-shot starting…");
        if (push) {
            getInstance().runPushCycle();
        } else {
            getInstance().runPullCycle();
        }
        System.err.println("[" + leg + "] debug one-shot done.");
    }
}
