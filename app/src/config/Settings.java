package config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Key/value shop settings stored in {@code pengaturan}.
 */
public final class Settings {

    private Settings() {
    }

    public static String get(String key, String fallback) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection conn = Koneksi.getConnection();
            ps = conn.prepareStatement(
                    "SELECT setting_value FROM pengaturan WHERE setting_key = ?");
            ps.setString(1, key);
            rs = ps.executeQuery();
            if (rs.next()) {
                String value = rs.getString(1);
                return value != null ? value : fallback;
            }
        } catch (Exception ignored) {
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
        return fallback;
    }

    public static int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(get(key, Integer.toString(fallback)).trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    public static boolean getBool(String key, boolean fallback) {
        String raw = get(key, fallback ? "1" : "0");
        if (raw == null) {
            return fallback;
        }
        raw = raw.trim();
        return "1".equals(raw) || "true".equalsIgnoreCase(raw) || "yes".equalsIgnoreCase(raw);
    }

    public static void set(String key, String value) throws Exception {
        Connection conn = Koneksi.getConnection();
        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO pengaturan (setting_key, setting_value) VALUES (?,?) "
                + "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)");
        try {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
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
}
