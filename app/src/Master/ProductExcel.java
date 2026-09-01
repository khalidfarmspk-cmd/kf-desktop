package Master;

import config.Ids;
import config.Koneksi;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Import / export products as Excel (.xlsx). Columns:
 * CODE, PRODUCT, CATEGORY, BUY, SELL, STOCK, UNIT, SUPPLIER
 */
public final class ProductExcel {

    public static final String[] HEADERS = {
        "CODE", "PRODUCT", "CATEGORY", "BUY", "SELL", "STOCK", "UNIT", "SUPPLIER"
    };

    private ProductExcel() {
    }

    public static void exportToFile(File file) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Products");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }

            Connection conn = Koneksi.getConnection();
            Statement stm = conn.createStatement();
            ResultSet rs = stm.executeQuery(
                    "SELECT p.kode_produk, p.nama_produk, k.nama_kategori, "
                    + "p.harga_beli, p.harga_jual, p.stok_produk, "
                    + "u.nama_satuan, s.nama_supplier "
                    + "FROM produk p "
                    + "LEFT JOIN kategori k ON p.kategori_Id = k.kategori_Id "
                    + "LEFT JOIN satuan u ON p.satuan_Id = u.satuan_Id "
                    + "LEFT JOIN supplier s ON p.supplier_Id = s.supplier_Id "
                    + "ORDER BY p.nama_produk");

            int rowIdx = 1;
            while (rs.next()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(rs.getString(1) == null ? "" : rs.getString(1));
                row.createCell(1).setCellValue(rs.getString(2) == null ? "" : rs.getString(2));
                row.createCell(2).setCellValue(rs.getString(3) == null ? "" : rs.getString(3));
                row.createCell(3).setCellValue(rs.getDouble(4));
                row.createCell(4).setCellValue(rs.getDouble(5));
                row.createCell(5).setCellValue(rs.getDouble(6));
                row.createCell(6).setCellValue(rs.getString(7) == null ? "" : rs.getString(7));
                row.createCell(7).setCellValue(rs.getString(8) == null ? "" : rs.getString(8));
            }
            rs.close();
            stm.close();

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream out = new FileOutputStream(file)) {
                wb.write(out);
            }
        }
    }

    /** @return summary message for the user */
    public static String importFromFile(File file) throws Exception {
        DataFormatter fmt = new DataFormatter(Locale.US);
        List<String> errors = new ArrayList<>();
        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        Connection conn = Koneksi.getConnection();
        int merekId = resolveMerekId(conn);
        Map<String, Integer> kategoriCache = new HashMap<>();
        Map<String, Integer> satuanCache = new HashMap<>();
        Map<String, Integer> supplierCache = new HashMap<>();

        try (FileInputStream in = new FileInputStream(file);
                Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("The Excel file has no sheets.");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Missing header row.");
            }
            Map<String, Integer> col = mapHeaders(headerRow, fmt);
            requireColumn(col, "CODE");
            requireColumn(col, "PRODUCT");

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String code = codeCell(row, col.get("CODE"), fmt).trim();
                String name = cell(row, col.get("PRODUCT"), fmt).trim();
                if (code.isEmpty() && name.isEmpty()) {
                    continue;
                }
                if (code.isEmpty() || name.isEmpty()) {
                    skipped++;
                    errors.add("Row " + (r + 1) + ": CODE and PRODUCT are required.");
                    continue;
                }

                String category = cell(row, col.get("CATEGORY"), fmt).trim();
                String unit = cell(row, col.get("UNIT"), fmt).trim();
                String supplier = cell(row, col.get("SUPPLIER"), fmt).trim();
                String buyStr = cell(row, col.get("BUY"), fmt).trim();
                String sellStr = cell(row, col.get("SELL"), fmt).trim();
                String stockStr = cell(row, col.get("STOCK"), fmt).trim();

                if (category.isEmpty()) {
                    category = "General";
                }
                if (unit.isEmpty()) {
                    unit = "Piece";
                }
                if (supplier.isEmpty()) {
                    supplier = "General";
                }

                try {
                    int buy = parseIntMoney(buyStr);
                    int sell = parseIntMoney(sellStr);
                    BigDecimal stock = parseStock(stockStr);

                    int kategoriId = resolveKategoriId(conn, category, kategoriCache);
                    int satuanId = resolveSatuanId(conn, unit, satuanCache);
                    int supplierId = resolveSupplierId(conn, supplier, supplierCache);
                    int isScale = isScaleForCategory(category);

                    if (productExists(conn, code)) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE produk SET nama_produk=?, harga_beli=?, harga_jual=?, stok_produk=?, "
                                + "kategori_Id=?, merek_Id=?, supplier_Id=?, satuan_Id=?, is_scale=? "
                                + "WHERE kode_produk=?")) {
                            ps.setString(1, truncate(name, 30));
                            ps.setInt(2, buy);
                            ps.setInt(3, sell);
                            ps.setBigDecimal(4, stock);
                            ps.setInt(5, kategoriId);
                            ps.setInt(6, merekId);
                            ps.setInt(7, supplierId);
                            ps.setInt(8, satuanId);
                            ps.setInt(9, isScale);
                            ps.setString(10, code);
                            ps.executeUpdate();
                        }
                        updated++;
                    } else {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO produk(kode_produk, nama_produk, harga_beli, harga_jual, stok_produk, "
                                + "kategori_Id, merek_Id, supplier_Id, satuan_Id, is_scale, uuid) "
                                + "VALUES (?,?,?,?,?,?,?,?,?,?,?)")) {
                            ps.setString(1, truncate(code, 30));
                            ps.setString(2, truncate(name, 30));
                            ps.setInt(3, buy);
                            ps.setInt(4, sell);
                            ps.setBigDecimal(5, stock);
                            ps.setInt(6, kategoriId);
                            ps.setInt(7, merekId);
                            ps.setInt(8, supplierId);
                            ps.setInt(9, satuanId);
                            ps.setInt(10, isScale);
                            ps.setString(11, Ids.newUuid());
                            ps.executeUpdate();
                        }
                        inserted++;
                    }
                } catch (Exception ex) {
                    skipped++;
                    errors.add("Row " + (r + 1) + " (" + code + "): " + ex.getMessage());
                }
            }
        }

        StringBuilder msg = new StringBuilder();
        msg.append("Import finished.\n")
                .append("Added: ").append(inserted)
                .append("\nUpdated: ").append(updated)
                .append("\nSkipped: ").append(skipped);
        if (!errors.isEmpty()) {
            msg.append("\n\nNotes:\n");
            int limit = Math.min(8, errors.size());
            for (int i = 0; i < limit; i++) {
                msg.append("• ").append(errors.get(i)).append("\n");
            }
            if (errors.size() > limit) {
                msg.append("• …and ").append(errors.size() - limit).append(" more.");
            }
        }
        return msg.toString();
    }

    private static Map<String, Integer> mapHeaders(Row headerRow, DataFormatter fmt) {
        Map<String, Integer> map = new HashMap<>();
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            String raw = cell(headerRow, c, fmt).trim();
            if (raw.isEmpty()) {
                continue;
            }
            String key = normalizeHeader(raw);
            if (!key.isEmpty()) {
                map.put(key, c);
            }
        }
        // aliases
        if (!map.containsKey("PRODUCT") && map.containsKey("NAME")) {
            map.put("PRODUCT", map.get("NAME"));
        }
        if (!map.containsKey("CODE") && map.containsKey("KODE")) {
            map.put("CODE", map.get("KODE"));
        }
        if (!map.containsKey("BUY") && map.containsKey("BUY PRICE")) {
            map.put("BUY", map.get("BUY PRICE"));
        }
        if (!map.containsKey("SELL") && map.containsKey("SELL PRICE")) {
            map.put("SELL", map.get("SELL PRICE"));
        }
        return map;
    }

    private static String normalizeHeader(String raw) {
        return raw.trim().toUpperCase(Locale.ROOT).replace('_', ' ');
    }

    private static void requireColumn(Map<String, Integer> col, String name) {
        if (!col.containsKey(name)) {
            throw new IllegalArgumentException(
                    "Missing column \"" + name + "\". Expected headers: CODE, PRODUCT, CATEGORY, BUY, SELL, STOCK, UNIT, SUPPLIER");
        }
    }

    private static String codeCell(Row row, Integer idx, DataFormatter fmt) {
        if (idx == null || row == null) {
            return "";
        }
        Cell cell = row.getCell(idx);
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.format("%06d", (long) cell.getNumericCellValue());
        }
        if (cell.getCellType() == CellType.FORMULA) {
            try {
                return String.format("%06d", (long) cell.getNumericCellValue());
            } catch (IllegalStateException ignored) {
                return fmt.formatCellValue(cell).trim();
            }
        }
        return fmt.formatCellValue(cell).trim();
    }

    private static String cell(Row row, Integer idx, DataFormatter fmt) {
        if (idx == null || row == null) {
            return "";
        }
        Cell cell = row.getCell(idx);
        if (cell == null) {
            return "";
        }
        return fmt.formatCellValue(cell);
    }

    private static int parseIntMoney(String s) {
        if (s == null || s.trim().isEmpty()) {
            return 0;
        }
        String cleaned = s.trim().replace(",", "").replace("Rs", "").replace("RS", "").trim();
        if (cleaned.contains(".")) {
            return (int) Math.round(Double.parseDouble(cleaned));
        }
        return Integer.parseInt(cleaned);
    }

    private static BigDecimal parseStock(String s) {
        if (s == null || s.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(s.trim().replace(",", ""));
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static int isScaleForCategory(String category) {
        return "vegetables".equalsIgnoreCase(category == null ? "" : category.trim()) ? 1 : 0;
    }

    private static boolean productExists(Connection conn, String code) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM produk WHERE kode_produk=? LIMIT 1")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static int resolveMerekId(Connection conn) throws SQLException {
        try (Statement stm = conn.createStatement();
                ResultSet rs = stm.executeQuery(
                        "SELECT merek_Id FROM merek WHERE nama_merek='General' LIMIT 1")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO merek(nama_merek, uuid) VALUES ('General', ?)")) {
            ps.setString(1, Ids.newUuid());
            ps.executeUpdate();
        } catch (SQLException ignore) {
            try (Statement stm = conn.createStatement()) {
                stm.executeUpdate("INSERT INTO merek(nama_merek) VALUES ('General')");
            }
        }
        try (Statement stm = conn.createStatement();
                ResultSet rs = stm.executeQuery(
                        "SELECT merek_Id FROM merek WHERE nama_merek='General' LIMIT 1")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        try (Statement stm = conn.createStatement();
                ResultSet rs = stm.executeQuery("SELECT merek_Id FROM merek ORDER BY merek_Id LIMIT 1")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        throw new SQLException("No brand available for product import.");
    }

    private static int resolveKategoriId(Connection conn, String name, Map<String, Integer> cache)
            throws SQLException {
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT kategori_Id FROM kategori WHERE LOWER(nama_kategori)=? LIMIT 1")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    cache.put(key, id);
                    return id;
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO kategori(nama_kategori, no_rak, uuid) VALUES (?,?,?)")) {
            ps.setString(1, truncate(name.trim(), 30));
            ps.setInt(2, 0);
            ps.setString(3, Ids.newUuid());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT kategori_Id FROM kategori WHERE LOWER(nama_kategori)=? LIMIT 1")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    cache.put(key, id);
                    return id;
                }
            }
        }
        throw new SQLException("Could not create category: " + name);
    }

    private static int resolveSatuanId(Connection conn, String name, Map<String, Integer> cache)
            throws SQLException {
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT satuan_Id FROM satuan WHERE LOWER(nama_satuan)=? LIMIT 1")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    cache.put(key, id);
                    return id;
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO satuan(nama_satuan, allow_decimal, uuid) VALUES (?,?,?)")) {
            ps.setString(1, truncate(name.trim(), 20));
            ps.setInt(2, 1);
            ps.setString(3, Ids.newUuid());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT satuan_Id FROM satuan WHERE LOWER(nama_satuan)=? LIMIT 1")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    cache.put(key, id);
                    return id;
                }
            }
        }
        throw new SQLException("Could not create unit: " + name);
    }

    private static int resolveSupplierId(Connection conn, String name, Map<String, Integer> cache)
            throws SQLException {
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT supplier_Id FROM supplier WHERE LOWER(nama_supplier)=? LIMIT 1")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    cache.put(key, id);
                    return id;
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO supplier(nama_supplier, alamat_supplier, telp_supplier, uuid) VALUES (?,?,?,?)")) {
            ps.setString(1, truncate(name.trim(), 30));
            ps.setString(2, "-");
            ps.setString(3, "0");
            ps.setString(4, Ids.newUuid());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT supplier_Id FROM supplier WHERE LOWER(nama_supplier)=? LIMIT 1")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    cache.put(key, id);
                    return id;
                }
            }
        }
        throw new SQLException("Could not create supplier: " + name);
    }
}
