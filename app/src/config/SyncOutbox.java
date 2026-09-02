package config;

import dao.Encrypt;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Build sync_outbox payloads after local saves. Failures are logged only.
 */
public final class SyncOutbox {

    private static final SimpleDateFormat TS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private SyncOutbox() {
    }

    public static void enqueueProductByKode(String kodeProduk) {
        if (kodeProduk == null || kodeProduk.trim().isEmpty()) {
            return;
        }
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Koneksi.getConnection();
            ps = conn.prepareStatement(
                    "SELECT p.uuid, p.kode_produk, p.nama_produk, p.harga_beli, p.harga_jual, "
                    + "p.stok_produk, p.merek_Id, p.is_scale, p.updated_at, "
                    + "k.uuid AS kategori_uuid, s.uuid AS supplier_uuid, u.uuid AS satuan_uuid "
                    + "FROM produk p "
                    + "LEFT JOIN kategori k ON p.kategori_Id = k.kategori_Id "
                    + "LEFT JOIN supplier s ON p.supplier_Id = s.supplier_Id "
                    + "LEFT JOIN satuan u ON p.satuan_Id = u.satuan_Id "
                    + "WHERE p.kode_produk = ?");
            ps.setString(1, kodeProduk.trim());
            rs = ps.executeQuery();
            if (!rs.next()) {
                return;
            }
            String uuid = rs.getString("uuid");
            StringBuilder sb = new StringBuilder(384);
            sb.append('{')
                    .append("\"uuid\":").append(SyncService.jsonString(uuid)).append(',')
                    .append("\"kodeProduk\":").append(jsonKode(rs.getString("kode_produk"))).append(',')
                    .append("\"namaProduk\":").append(SyncService.jsonString(rs.getString("nama_produk"))).append(',')
                    .append("\"hargaBeli\":").append(rs.getInt("harga_beli")).append(',')
                    .append("\"hargaJual\":").append(rs.getInt("harga_jual")).append(',')
                    .append("\"stokProduk\":").append(SyncService.jsonString(qty3(rs.getBigDecimal("stok_produk")))).append(',')
                    .append("\"isScale\":").append(rs.getInt("is_scale")).append(',')
                    .append("\"kategoriUuid\":").append(jsonUuidOrNull(rs.getString("kategori_uuid"))).append(',')
                    .append("\"merekId\":").append(rs.getObject("merek_Id") == null ? "null" : Integer.toString(rs.getInt("merek_Id"))).append(',')
                    .append("\"supplierUuid\":").append(jsonUuidOrNull(rs.getString("supplier_uuid"))).append(',')
                    .append("\"satuanUuid\":").append(jsonUuidOrNull(rs.getString("satuan_uuid"))).append(',')
                    .append("\"updatedAt\":").append(SyncService.jsonString(formatTs(rs.getTimestamp("updated_at"))))
                    .append('}');
            SyncService.getInstance().enqueue("product", uuid, sb.toString());
        } catch (Exception e) {
            System.out.println("sync outbox product failed: " + e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    public static void enqueueCategoryById(int kategoriId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Koneksi.getConnection();
            ps = conn.prepareStatement(
                    "SELECT uuid, nama_kategori, no_rak, updated_at FROM kategori WHERE kategori_Id = ?");
            ps.setInt(1, kategoriId);
            rs = ps.executeQuery();
            if (!rs.next()) {
                return;
            }
            String uuid = rs.getString("uuid");
            StringBuilder sb = new StringBuilder(192);
            sb.append('{')
                    .append("\"uuid\":").append(SyncService.jsonString(uuid)).append(',')
                    .append("\"namaKategori\":").append(SyncService.jsonString(rs.getString("nama_kategori"))).append(',')
                    .append("\"noRak\":").append(SyncService.jsonString(rs.getString("no_rak"))).append(',')
                    .append("\"updatedAt\":").append(SyncService.jsonString(formatTs(rs.getTimestamp("updated_at"))))
                    .append('}');
            SyncService.getInstance().enqueue("category", uuid, sb.toString());
        } catch (Exception e) {
            System.out.println("sync outbox category failed: " + e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    public static void enqueueSupplierById(int supplierId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Koneksi.getConnection();
            ps = conn.prepareStatement(
                    "SELECT uuid, nama_supplier, alamat_supplier, telp_supplier, updated_at "
                    + "FROM supplier WHERE supplier_Id = ?");
            ps.setInt(1, supplierId);
            rs = ps.executeQuery();
            if (!rs.next()) {
                return;
            }
            String uuid = rs.getString("uuid");
            StringBuilder sb = new StringBuilder(256);
            sb.append('{')
                    .append("\"uuid\":").append(SyncService.jsonString(uuid)).append(',')
                    .append("\"namaSupplier\":").append(SyncService.jsonString(rs.getString("nama_supplier"))).append(',')
                    .append("\"alamatSupplier\":").append(SyncService.jsonString(rs.getString("alamat_supplier"))).append(',')
                    .append("\"telpSupplier\":").append(SyncService.jsonString(rs.getString("telp_supplier"))).append(',')
                    .append("\"updatedAt\":").append(SyncService.jsonString(formatTs(rs.getTimestamp("updated_at"))))
                    .append('}');
            SyncService.getInstance().enqueue("supplier", uuid, sb.toString());
        } catch (Exception e) {
            System.out.println("sync outbox supplier failed: " + e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    public static void enqueueCustomerById(int pelangganId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Koneksi.getConnection();
            ps = conn.prepareStatement(
                    "SELECT uuid, nama_pelanggan, telp_pelanggan, alamat_pelanggan, updated_at "
                    + "FROM pelanggan WHERE pelanggan_Id = ?");
            ps.setInt(1, pelangganId);
            rs = ps.executeQuery();
            if (!rs.next()) {
                return;
            }
            String uuid = rs.getString("uuid");
            StringBuilder sb = new StringBuilder(256);
            sb.append('{')
                    .append("\"uuid\":").append(SyncService.jsonString(uuid)).append(',')
                    .append("\"namaPelanggan\":").append(SyncService.jsonString(rs.getString("nama_pelanggan"))).append(',')
                    .append("\"telpPelanggan\":").append(jsonNullableString(rs.getString("telp_pelanggan"))).append(',')
                    .append("\"alamatPelanggan\":").append(jsonNullableString(rs.getString("alamat_pelanggan"))).append(',')
                    .append("\"updatedAt\":").append(SyncService.jsonString(formatTs(rs.getTimestamp("updated_at"))))
                    .append('}');
            SyncService.getInstance().enqueue("customer", uuid, sb.toString());
        } catch (Exception e) {
            System.out.println("sync outbox customer failed: " + e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /**
     * Queue a customer removal. Call before the local DELETE, while the uuid is
     * still readable. The cloud row must go too, otherwise applyPulledCustomers
     * re-inserts it on the next pull.
     */
    public static void enqueueCustomerDelete(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return;
        }
        String id = uuid.trim();
        StringBuilder sb = new StringBuilder(64);
        sb.append('{')
                .append("\"uuid\":").append(SyncService.jsonString(id))
                .append('}');
        SyncService.getInstance().enqueue("customer_delete", id, sb.toString());
    }

    /** Columns the user payload needs; shared by the single and backfill enqueues. */
    private static final String USER_SELECT =
            "SELECT uuid, nama_user, alamat_user, telp_user, username_user, "
            + "password_user, level_user, status_user, updated_at "
            + "FROM users WHERE user_Id = ?";

    private static String userPayload(ResultSet rs) throws Exception {
        StringBuilder sb = new StringBuilder(320);
        sb.append('{')
                .append("\"uuid\":").append(SyncService.jsonString(rs.getString("uuid"))).append(',')
                .append("\"namaUser\":").append(SyncService.jsonString(rs.getString("nama_user"))).append(',')
                .append("\"alamatUser\":").append(jsonNullableString(rs.getString("alamat_user"))).append(',')
                .append("\"telpUser\":").append(jsonNullableString(rs.getString("telp_user"))).append(',')
                .append("\"usernameUser\":").append(SyncService.jsonString(rs.getString("username_user"))).append(',')
                .append("\"passwordHash\":").append(SyncService.jsonString(rs.getString("password_user"))).append(',')
                .append("\"levelUser\":").append(SyncService.jsonString(rs.getString("level_user"))).append(',')
                .append("\"statusUser\":").append(SyncService.jsonString(rs.getString("status_user"))).append(',')
                .append("\"updatedAt\":").append(SyncService.jsonString(formatTs(rs.getTimestamp("updated_at"))))
                .append('}');
        return sb.toString();
    }

    public static void enqueueUserById(int userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Koneksi.getConnection();
            ps = conn.prepareStatement(USER_SELECT);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            if (!rs.next()) {
                return;
            }
            SyncService.getInstance().enqueue("user", rs.getString("uuid"), userPayload(rs));
        } catch (Exception e) {
            System.out.println("sync outbox user failed: " + e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /**
     * Queue a user removal. Call before the local DELETE, while the uuid is still
     * readable. Shares the outbox uuid key with enqueueUserById, so a create that
     * has not shipped yet is replaced by the delete rather than racing it.
     */
    public static void enqueueUserDelete(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return;
        }
        String id = uuid.trim();
        StringBuilder sb = new StringBuilder(64);
        sb.append('{')
                .append("\"uuid\":").append(SyncService.jsonString(id))
                .append('}');
        SyncService.getInstance().enqueue("user_delete", id, sb.toString());
    }

    public static void enqueuePurchaseById(int pembelianId) {
        Connection conn = null;
        PreparedStatement headerPs = null;
        PreparedStatement linesPs = null;
        ResultSet hrs = null;
        ResultSet lrs = null;
        try {
            conn = Koneksi.getConnection();
            headerPs = conn.prepareStatement(
                    "SELECT uuid, tanggal_pembelian, user_Id FROM pembelian WHERE pembelian_Id = ?");
            headerPs.setInt(1, pembelianId);
            hrs = headerPs.executeQuery();
            if (!hrs.next()) {
                return;
            }
            String uuid = hrs.getString("uuid");
            String tanggal = hrs.getString("tanggal_pembelian");
            int userId = hrs.getInt("user_Id");

            StringBuilder lines = new StringBuilder();
            lines.append('[');
            linesPs = conn.prepareStatement(
                    "SELECT uuid, kode_produk, jumlah FROM detail_pembelian WHERE pembelian_Id = ?");
            linesPs.setInt(1, pembelianId);
            lrs = linesPs.executeQuery();
            boolean first = true;
            while (lrs.next()) {
                if (!first) {
                    lines.append(',');
                }
                first = false;
                lines.append('{')
                        .append("\"uuid\":").append(SyncService.jsonString(lrs.getString("uuid"))).append(',')
                        .append("\"kodeProduk\":").append(jsonKode(lrs.getString("kode_produk"))).append(',')
                        .append("\"jumlah\":").append(SyncService.jsonString(qty3(lrs.getBigDecimal("jumlah"))))
                        .append('}');
            }
            lines.append(']');

            StringBuilder sb = new StringBuilder(384);
            sb.append('{')
                    .append("\"uuid\":").append(SyncService.jsonString(uuid)).append(',')
                    .append("\"tanggalPembelian\":").append(SyncService.jsonString(tanggal)).append(',')
                    .append("\"userId\":").append(userId).append(',')
                    .append("\"lines\":").append(lines)
                    .append('}');
            SyncService.getInstance().enqueue("purchase", uuid, sb.toString());
        } catch (Exception e) {
            System.out.println("sync outbox purchase failed: " + e);
        } finally {
            closeQuietly(lrs);
            closeQuietly(hrs);
            closeQuietly(linesPs);
            closeQuietly(headerPs);
        }
    }

    public static void enqueueExpenseById(int pengeluaranId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Koneksi.getConnection();
            ps = conn.prepareStatement(
                    "SELECT uuid, tanggal, kategori, keterangan, jumlah, user_Id, updated_at "
                    + "FROM pengeluaran WHERE pengeluaran_Id = ?");
            ps.setInt(1, pengeluaranId);
            rs = ps.executeQuery();
            if (!rs.next()) {
                return;
            }
            String uuid = rs.getString("uuid");
            StringBuilder sb = new StringBuilder(256);
            sb.append('{')
                    .append("\"uuid\":").append(SyncService.jsonString(uuid)).append(',')
                    .append("\"tanggal\":").append(SyncService.jsonString(rs.getString("tanggal"))).append(',')
                    .append("\"kategori\":").append(SyncService.jsonString(rs.getString("kategori"))).append(',')
                    .append("\"keterangan\":").append(jsonNullableString(rs.getString("keterangan"))).append(',')
                    .append("\"jumlah\":").append(rs.getInt("jumlah")).append(',')
                    .append("\"userId\":").append(rs.getInt("user_Id")).append(',')
                    .append("\"updatedAt\":").append(SyncService.jsonString(formatTs(rs.getTimestamp("updated_at"))))
                    .append('}');
            SyncService.getInstance().enqueue("expense", uuid, sb.toString());
        } catch (Exception e) {
            System.out.println("sync outbox expense failed: " + e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /**
     * Enqueue all local master + transaction rows for initial sync.
     * Uses INSERT IGNORE so existing outbox UUIDs are not duplicated.
     * @return number of rows attempted
     */
    public static int queueFullSync() {
        int count = 0;
        Connection conn = null;
        try {
            conn = Koneksi.getConnection();
            count += enqueueAllCategories(conn);
            count += enqueueAllSuppliers(conn);
            count += enqueueAllCustomers(conn);
            count += enqueueAllProducts(conn);
            count += enqueueAllPurchases(conn);
            count += enqueueAllExpenses(conn);
            count += enqueueAllSales(conn);
            count += enqueueAllUsers(conn);
        } catch (Exception e) {
            System.out.println("full sync queue failed: " + e);
        }
        return count;
    }

    private static int enqueueAllUsers(Connection conn) throws Exception {
        PreparedStatement ps = conn.prepareStatement("SELECT user_Id FROM users");
        ResultSet rs = ps.executeQuery();
        List<Integer> ids = new ArrayList<Integer>();
        while (rs.next()) {
            ids.add(Integer.valueOf(rs.getInt(1)));
        }
        rs.close();
        ps.close();
        for (Integer id : ids) {
            Connection c2 = null;
            PreparedStatement p2 = null;
            ResultSet r2 = null;
            try {
                c2 = Koneksi.getConnection();
                p2 = c2.prepareStatement(USER_SELECT);
                p2.setInt(1, id.intValue());
                r2 = p2.executeQuery();
                if (!r2.next()) {
                    continue;
                }
                SyncService.getInstance().enqueueIgnore("user", r2.getString("uuid"), userPayload(r2));
            } catch (Exception e) {
                System.out.println("full sync user failed: " + e);
            } finally {
                closeQuietly(r2);
                closeQuietly(p2);
            }
        }
        return ids.size();
    }

    private static int enqueueAllProducts(Connection conn) throws Exception {
        PreparedStatement ps = conn.prepareStatement("SELECT kode_produk FROM produk");
        ResultSet rs = ps.executeQuery();
        int n = 0;
        List<String> codes = new ArrayList<String>();
        while (rs.next()) {
            codes.add(rs.getString(1));
        }
        rs.close();
        ps.close();
        for (String kode : codes) {
            enqueueProductIgnore(conn, kode);
            n++;
        }
        return n;
    }

    private static void enqueueProductIgnore(Connection conn, String kode) {
        // Reuse builder then INSERT IGNORE
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(
                    "SELECT p.uuid, p.kode_produk, p.nama_produk, p.harga_beli, p.harga_jual, "
                    + "p.stok_produk, p.merek_Id, p.is_scale, p.updated_at, "
                    + "k.uuid AS kategori_uuid, s.uuid AS supplier_uuid, u.uuid AS satuan_uuid "
                    + "FROM produk p "
                    + "LEFT JOIN kategori k ON p.kategori_Id = k.kategori_Id "
                    + "LEFT JOIN supplier s ON p.supplier_Id = s.supplier_Id "
                    + "LEFT JOIN satuan u ON p.satuan_Id = u.satuan_Id "
                    + "WHERE p.kode_produk = ?");
            ps.setString(1, kode);
            rs = ps.executeQuery();
            if (!rs.next()) {
                return;
            }
            String uuid = rs.getString("uuid");
            StringBuilder sb = new StringBuilder(384);
            sb.append('{')
                    .append("\"uuid\":").append(SyncService.jsonString(uuid)).append(',')
                    .append("\"kodeProduk\":").append(jsonKode(rs.getString("kode_produk"))).append(',')
                    .append("\"namaProduk\":").append(SyncService.jsonString(rs.getString("nama_produk"))).append(',')
                    .append("\"hargaBeli\":").append(rs.getInt("harga_beli")).append(',')
                    .append("\"hargaJual\":").append(rs.getInt("harga_jual")).append(',')
                    .append("\"stokProduk\":").append(SyncService.jsonString(qty3(rs.getBigDecimal("stok_produk")))).append(',')
                    .append("\"isScale\":").append(rs.getInt("is_scale")).append(',')
                    .append("\"kategoriUuid\":").append(jsonUuidOrNull(rs.getString("kategori_uuid"))).append(',')
                    .append("\"merekId\":").append(rs.getObject("merek_Id") == null ? "null" : Integer.toString(rs.getInt("merek_Id"))).append(',')
                    .append("\"supplierUuid\":").append(jsonUuidOrNull(rs.getString("supplier_uuid"))).append(',')
                    .append("\"satuanUuid\":").append(jsonUuidOrNull(rs.getString("satuan_uuid"))).append(',')
                    .append("\"updatedAt\":").append(SyncService.jsonString(formatTs(rs.getTimestamp("updated_at"))))
                    .append('}');
            SyncService.getInstance().enqueueIgnore("product", uuid, sb.toString());
        } catch (Exception e) {
            System.out.println("full sync product failed: " + e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    private static int enqueueAllCategories(Connection conn) throws Exception {
        PreparedStatement ps = conn.prepareStatement("SELECT kategori_Id FROM kategori");
        ResultSet rs = ps.executeQuery();
        List<Integer> ids = new ArrayList<Integer>();
        while (rs.next()) {
            ids.add(Integer.valueOf(rs.getInt(1)));
        }
        rs.close();
        ps.close();
        for (Integer id : ids) {
            enqueueCategoryByIdIgnore(id.intValue());
        }
        return ids.size();
    }

    private static void enqueueCategoryByIdIgnore(int id) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Koneksi.getConnection();
            ps = conn.prepareStatement(
                    "SELECT uuid, nama_kategori, no_rak, updated_at FROM kategori WHERE kategori_Id = ?");
            ps.setInt(1, id);
            rs = ps.executeQuery();
            if (!rs.next()) {
                return;
            }
            String uuid = rs.getString("uuid");
            StringBuilder sb = new StringBuilder(192);
            sb.append('{')
                    .append("\"uuid\":").append(SyncService.jsonString(uuid)).append(',')
                    .append("\"namaKategori\":").append(SyncService.jsonString(rs.getString("nama_kategori"))).append(',')
                    .append("\"noRak\":").append(SyncService.jsonString(rs.getString("no_rak"))).append(',')
                    .append("\"updatedAt\":").append(SyncService.jsonString(formatTs(rs.getTimestamp("updated_at"))))
                    .append('}');
            SyncService.getInstance().enqueueIgnore("category", uuid, sb.toString());
        } catch (Exception e) {
            System.out.println("full sync category failed: " + e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    private static int enqueueAllSuppliers(Connection conn) throws Exception {
        PreparedStatement ps = conn.prepareStatement("SELECT supplier_Id FROM supplier");
        ResultSet rs = ps.executeQuery();
        List<Integer> ids = new ArrayList<Integer>();
        while (rs.next()) {
            ids.add(Integer.valueOf(rs.getInt(1)));
        }
        rs.close();
        ps.close();
        for (Integer id : ids) {
            Connection c2 = null;
            PreparedStatement p2 = null;
            ResultSet r2 = null;
            try {
                c2 = Koneksi.getConnection();
                p2 = c2.prepareStatement(
                        "SELECT uuid, nama_supplier, alamat_supplier, telp_supplier, updated_at "
                        + "FROM supplier WHERE supplier_Id = ?");
                p2.setInt(1, id.intValue());
                r2 = p2.executeQuery();
                if (!r2.next()) {
                    continue;
                }
                String uuid = r2.getString("uuid");
                StringBuilder sb = new StringBuilder(256);
                sb.append('{')
                        .append("\"uuid\":").append(SyncService.jsonString(uuid)).append(',')
                        .append("\"namaSupplier\":").append(SyncService.jsonString(r2.getString("nama_supplier"))).append(',')
                        .append("\"alamatSupplier\":").append(SyncService.jsonString(r2.getString("alamat_supplier"))).append(',')
                        .append("\"telpSupplier\":").append(SyncService.jsonString(r2.getString("telp_supplier"))).append(',')
                        .append("\"updatedAt\":").append(SyncService.jsonString(formatTs(r2.getTimestamp("updated_at"))))
                        .append('}');
                SyncService.getInstance().enqueueIgnore("supplier", uuid, sb.toString());
            } catch (Exception e) {
                System.out.println("full sync supplier failed: " + e);
            } finally {
                closeQuietly(r2);
                closeQuietly(p2);
            }
        }
        return ids.size();
    }

    private static int enqueueAllCustomers(Connection conn) throws Exception {
        PreparedStatement ps = conn.prepareStatement("SELECT pelanggan_Id FROM pelanggan");
        ResultSet rs = ps.executeQuery();
        List<Integer> ids = new ArrayList<Integer>();
        while (rs.next()) {
            ids.add(Integer.valueOf(rs.getInt(1)));
        }
        rs.close();
        ps.close();
        for (Integer id : ids) {
            Connection c2 = null;
            PreparedStatement p2 = null;
            ResultSet r2 = null;
            try {
                c2 = Koneksi.getConnection();
                p2 = c2.prepareStatement(
                        "SELECT uuid, nama_pelanggan, telp_pelanggan, alamat_pelanggan, updated_at "
                        + "FROM pelanggan WHERE pelanggan_Id = ?");
                p2.setInt(1, id.intValue());
                r2 = p2.executeQuery();
                if (!r2.next()) {
                    continue;
                }
                String uuid = r2.getString("uuid");
                StringBuilder sb = new StringBuilder(256);
                sb.append('{')
                        .append("\"uuid\":").append(SyncService.jsonString(uuid)).append(',')
                        .append("\"namaPelanggan\":").append(SyncService.jsonString(r2.getString("nama_pelanggan"))).append(',')
                        .append("\"telpPelanggan\":").append(jsonNullableString(r2.getString("telp_pelanggan"))).append(',')
                        .append("\"alamatPelanggan\":").append(jsonNullableString(r2.getString("alamat_pelanggan"))).append(',')
                        .append("\"updatedAt\":").append(SyncService.jsonString(formatTs(r2.getTimestamp("updated_at"))))
                        .append('}');
                SyncService.getInstance().enqueueIgnore("customer", uuid, sb.toString());
            } catch (Exception e) {
                System.out.println("full sync customer failed: " + e);
            } finally {
                closeQuietly(r2);
                closeQuietly(p2);
            }
        }
        return ids.size();
    }

    private static int enqueueAllPurchases(Connection conn) throws Exception {
        PreparedStatement ps = conn.prepareStatement("SELECT pembelian_Id FROM pembelian");
        ResultSet rs = ps.executeQuery();
        List<Integer> ids = new ArrayList<Integer>();
        while (rs.next()) {
            ids.add(Integer.valueOf(rs.getInt(1)));
        }
        rs.close();
        ps.close();
        for (Integer id : ids) {
            enqueuePurchaseIgnore(id.intValue());
        }
        return ids.size();
    }

    private static void enqueuePurchaseIgnore(int pembelianId) {
        // same as enqueuePurchaseById but enqueueIgnore
        Connection conn = null;
        PreparedStatement headerPs = null;
        PreparedStatement linesPs = null;
        ResultSet hrs = null;
        ResultSet lrs = null;
        try {
            conn = Koneksi.getConnection();
            headerPs = conn.prepareStatement(
                    "SELECT uuid, tanggal_pembelian, user_Id FROM pembelian WHERE pembelian_Id = ?");
            headerPs.setInt(1, pembelianId);
            hrs = headerPs.executeQuery();
            if (!hrs.next()) {
                return;
            }
            String uuid = hrs.getString("uuid");
            StringBuilder lines = new StringBuilder();
            lines.append('[');
            linesPs = conn.prepareStatement(
                    "SELECT uuid, kode_produk, jumlah FROM detail_pembelian WHERE pembelian_Id = ?");
            linesPs.setInt(1, pembelianId);
            lrs = linesPs.executeQuery();
            boolean first = true;
            while (lrs.next()) {
                if (!first) {
                    lines.append(',');
                }
                first = false;
                lines.append('{')
                        .append("\"uuid\":").append(SyncService.jsonString(lrs.getString("uuid"))).append(',')
                        .append("\"kodeProduk\":").append(jsonKode(lrs.getString("kode_produk"))).append(',')
                        .append("\"jumlah\":").append(SyncService.jsonString(qty3(lrs.getBigDecimal("jumlah"))))
                        .append('}');
            }
            lines.append(']');
            StringBuilder sb = new StringBuilder(384);
            sb.append('{')
                    .append("\"uuid\":").append(SyncService.jsonString(uuid)).append(',')
                    .append("\"tanggalPembelian\":").append(SyncService.jsonString(hrs.getString("tanggal_pembelian"))).append(',')
                    .append("\"userId\":").append(hrs.getInt("user_Id")).append(',')
                    .append("\"lines\":").append(lines)
                    .append('}');
            SyncService.getInstance().enqueueIgnore("purchase", uuid, sb.toString());
        } catch (Exception e) {
            System.out.println("full sync purchase failed: " + e);
        } finally {
            closeQuietly(lrs);
            closeQuietly(hrs);
            closeQuietly(linesPs);
            closeQuietly(headerPs);
        }
    }

    private static int enqueueAllExpenses(Connection conn) throws Exception {
        PreparedStatement ps = conn.prepareStatement("SELECT pengeluaran_Id FROM pengeluaran");
        ResultSet rs = ps.executeQuery();
        List<Integer> ids = new ArrayList<Integer>();
        while (rs.next()) {
            ids.add(Integer.valueOf(rs.getInt(1)));
        }
        rs.close();
        ps.close();
        for (Integer id : ids) {
            Connection c2 = null;
            PreparedStatement p2 = null;
            ResultSet r2 = null;
            try {
                c2 = Koneksi.getConnection();
                p2 = c2.prepareStatement(
                        "SELECT uuid, tanggal, kategori, keterangan, jumlah, user_Id, updated_at "
                        + "FROM pengeluaran WHERE pengeluaran_Id = ?");
                p2.setInt(1, id.intValue());
                r2 = p2.executeQuery();
                if (!r2.next()) {
                    continue;
                }
                String uuid = r2.getString("uuid");
                StringBuilder sb = new StringBuilder(256);
                sb.append('{')
                        .append("\"uuid\":").append(SyncService.jsonString(uuid)).append(',')
                        .append("\"tanggal\":").append(SyncService.jsonString(r2.getString("tanggal"))).append(',')
                        .append("\"kategori\":").append(SyncService.jsonString(r2.getString("kategori"))).append(',')
                        .append("\"keterangan\":").append(jsonNullableString(r2.getString("keterangan"))).append(',')
                        .append("\"jumlah\":").append(r2.getInt("jumlah")).append(',')
                        .append("\"userId\":").append(r2.getInt("user_Id")).append(',')
                        .append("\"updatedAt\":").append(SyncService.jsonString(formatTs(r2.getTimestamp("updated_at"))))
                        .append('}');
                SyncService.getInstance().enqueueIgnore("expense", uuid, sb.toString());
            } catch (Exception e) {
                System.out.println("full sync expense failed: " + e);
            } finally {
                closeQuietly(r2);
                closeQuietly(p2);
            }
        }
        return ids.size();
    }

    private static int enqueueAllSales(Connection conn) throws Exception {
        PreparedStatement ps = conn.prepareStatement("SELECT penjualan_Id FROM penjualan");
        ResultSet rs = ps.executeQuery();
        List<Integer> ids = new ArrayList<Integer>();
        while (rs.next()) {
            ids.add(Integer.valueOf(rs.getInt(1)));
        }
        rs.close();
        ps.close();
        for (Integer id : ids) {
            enqueueSaleIgnore(id.intValue());
        }
        return ids.size();
    }

    private static void enqueueSaleIgnore(int penjualanId) {
        Connection c = null;
        PreparedStatement headerPs = null;
        PreparedStatement linesPs = null;
        ResultSet hrs = null;
        ResultSet lrs = null;
        try {
            c = Koneksi.getConnection();
            headerPs = c.prepareStatement(
                    "SELECT p.uuid, p.tanggal_penjualan, "
                    + "COALESCE(p.subtotal_kotor, p.Total_pembayaran) AS subtotal_kotor, "
                    + "COALESCE(p.diskon, 0) AS diskon, p.Total_pembayaran, "
                    + "p.uang_diterima, p.uang_kembalian, p.user_Id, p.metode_Id, p.nama_kurir, "
                    + "COALESCE(p.voided, 0) AS voided, pl.uuid AS pelanggan_uuid "
                    + "FROM penjualan p "
                    + "LEFT JOIN pelanggan pl ON p.pelanggan_Id = pl.pelanggan_Id "
                    + "WHERE p.penjualan_Id = ?");
            headerPs.setInt(1, penjualanId);
            hrs = headerPs.executeQuery();
            if (!hrs.next()) {
                return;
            }
            String saleUuid = hrs.getString("uuid");
            Integer metodeId = (Integer) hrs.getObject("metode_Id");
            String namaKurir = hrs.getString("nama_kurir");
            String pelangganUuid = hrs.getString("pelanggan_uuid");
            int voided = hrs.getInt("voided");

            StringBuilder linesJson = new StringBuilder();
            linesJson.append('[');
            linesPs = c.prepareStatement(
                    "SELECT uuid, kode_produk, jumlah, Subtotal FROM detail_penjualan WHERE penjualan_Id = ?");
            linesPs.setInt(1, penjualanId);
            lrs = linesPs.executeQuery();
            boolean first = true;
            while (lrs.next()) {
                if (!first) {
                    linesJson.append(',');
                }
                first = false;
                linesJson.append('{')
                        .append("\"uuid\":").append(SyncService.jsonString(lrs.getString("uuid"))).append(',')
                        .append("\"kodeProduk\":").append(jsonKode(lrs.getString("kode_produk"))).append(',')
                        .append("\"jumlah\":").append(SyncService.jsonString(qty3(lrs.getBigDecimal("jumlah")))).append(',')
                        .append("\"subtotal\":").append(lrs.getInt("Subtotal"))
                        .append('}');
            }
            linesJson.append(']');

            StringBuilder payload = new StringBuilder(512);
            payload.append('{')
                    .append("\"uuid\":").append(SyncService.jsonString(saleUuid)).append(',')
                    .append("\"tanggalPenjualan\":").append(SyncService.jsonString(hrs.getString("tanggal_penjualan"))).append(',')
                    .append("\"subtotalKotor\":").append(hrs.getInt("subtotal_kotor")).append(',')
                    .append("\"diskon\":").append(hrs.getInt("diskon")).append(',')
                    .append("\"totalPembayaran\":").append(hrs.getInt("Total_pembayaran")).append(',')
                    .append("\"uangDiterima\":").append(hrs.getInt("uang_diterima")).append(',')
                    .append("\"uangKembalian\":").append(hrs.getInt("uang_kembalian")).append(',')
                    .append("\"userId\":").append(hrs.getInt("user_Id")).append(',')
                    .append("\"pelangganUuid\":")
                    .append(pelangganUuid == null ? "null" : SyncService.jsonString(pelangganUuid))
                    .append(',')
                    .append("\"metodeId\":").append(metodeId == null ? "null" : metodeId.toString()).append(',')
                    .append("\"namaKurir\":")
                    .append(namaKurir == null || namaKurir.isEmpty()
                            ? "null" : SyncService.jsonString(namaKurir))
                    .append(',')
                    .append("\"voided\":").append(voided).append(',')
                    .append("\"lines\":").append(linesJson)
                    .append('}');
            SyncService.getInstance().enqueueIgnore("sale", saleUuid, payload.toString());
        } catch (Exception e) {
            System.out.println("full sync sale failed: " + e);
        } finally {
            closeQuietly(lrs);
            closeQuietly(hrs);
            closeQuietly(linesPs);
            closeQuietly(headerPs);
        }
    }

    /** Lookup last insert id helper for callers that need it. */
    public static int lookupIdByUuid(String table, String idCol, String uuidCol, String uuid) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Koneksi.getConnection();
            ps = conn.prepareStatement("SELECT " + idCol + " FROM " + table + " WHERE " + uuidCol + " = ?");
            ps.setString(1, uuid);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            System.out.println("lookupIdByUuid failed: " + e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
        return -1;
    }

    public static String placeholderPasswordHash() {
        return Encrypt.getmd5java("RESET-" + Ids.newUuid());
    }

    private static String qty3(BigDecimal q) {
        if (q == null) {
            return "0.000";
        }
        return q.setScale(3, RoundingMode.HALF_UP).toPlainString();
    }

    private static String jsonKode(String kode) {
        if (kode == null) {
            return "null";
        }
        try {
            return Long.toString(Long.parseLong(kode.trim()));
        } catch (Exception e) {
            return SyncService.jsonString(kode.trim());
        }
    }

    private static String jsonUuidOrNull(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return "null";
        }
        return SyncService.jsonString(uuid.trim());
    }

    private static String jsonNullableString(String s) {
        if (s == null) {
            return "null";
        }
        return SyncService.jsonString(s);
    }

    private static String formatTs(Timestamp ts) {
        if (ts == null) {
            return TS.format(new java.util.Date());
        }
        synchronized (TS) {
            return TS.format(ts);
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
