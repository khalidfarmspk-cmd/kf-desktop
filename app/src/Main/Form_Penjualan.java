/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Main;

import Master.Form_Pelanggan;
import config.Ids;
import config.Koneksi;
import config.Settings;
import config.SyncService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Sell — register layout (scan + quick keys + ticket sidebar).
 */
public class Form_Penjualan extends javax.swing.JPanel {

    PreparedStatement ps;
    ResultSet rs;

    private JLabel lb_cartStatus;
    private JLabel lb_cashierMeta;
    private JLabel lb_ticketMeta;
    private JLabel lb_timeMeta;
    private JLabel lb_subtotalVal;
    private JLabel lb_discountVal;
    private JLabel lb_changeVal;
    private JLabel lb_totalDue;
    private JLabel lb_ticketCustomer;
    private JPanel pn_quickKeys;
    private JPanel pn_ticketLines;
    private JPanel pn_categoryTabs;
    private JTextField txt_customer;
    private JTextField txt_discount;
    private JButton btn_newCustomer;
    private JButton btn_clearCustomer;
    private JComboBox<MetodeItem> cb_metode;
    private JComboBox<String> cb_discountMode;
    private JTextField txt_kurir;
    private JLabel lb_kurirCap;
    private JPanel pn_kurirRow;
    private JPanel pn_cashReceivedRow;
    private JPanel pn_changeDueRow;
    private Integer selectedPelangganId;
    private String selectedPelangganName;
    private JPopupMenu customerSuggestPopup;
    private JList<CustomerMatch> customerSuggestList;
    private boolean suppressCustomerSuggest;
    private Timer customerSuggestTimer;
    private Timer rongtaPollTimer;
    private String lastAutoRongtaText = "";
    private int maxDiscountPercent = 10;
    private String activeCategory = "All items";
    private final List<ProductKey> catalog = new ArrayList<>();
    private final List<JButton> categoryButtons = new ArrayList<>();

    private static final Color TICKET_BG = new Color(0xF3F3F1);
    private static final Color SEARCH_BG = new Color(0xEFEFEA);
    private static final Color RULE = new Color(0xD0D0CC);

    private static class ProductKey {
        final String code;
        final String name;
        final String category;
        final String unitName;
        final BigDecimal stock;
        final int price;
        final boolean allowDecimal;

        ProductKey(String code, String name, String category,
                BigDecimal stock, int price, String unitName, boolean allowDecimal) {
            this.code = code;
            this.name = name;
            this.category = category;
            this.unitName = unitName;
            this.stock = stock;
            this.price = price;
            this.allowDecimal = allowDecimal;
        }
    }

    private static class MetodeItem {
        final int id;
        final String nama;

        MetodeItem(int id, String nama) {
            this.id = id;
            this.nama = nama;
        }

        @Override
        public String toString() {
            return nama;
        }
    }

    private static class CustomerMatch {
        final int id;
        final String nama;
        final String telp;

        CustomerMatch(int id, String nama, String telp) {
            this.id = id;
            this.nama = nama;
            this.telp = telp;
        }

        @Override
        public String toString() {
            if (telp == null || telp.isEmpty()) {
                return nama;
            }
            return nama + "  ·  " + telp;
        }
    }

    public Form_Penjualan() {
        initComponents();

        txt_idKasir.setText(user.getId());
        txt_namaKasir.setText(user.getNama());
        tampilBarang();
        setEditableFalse();
        id();
        date();
        refreshMeta();
        loadCategories();
        loadQuickKeys();
        rebuildTicketLines();
        updateCartStatus();
        loadPaymentMethods();
        loadMaxDiscount();
        updateMoneyLabels();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        startRongtaPollTimer();
    }

    @Override
    public void removeNotify() {
        stopRongtaPollTimer();
        super.removeNotify();
    }

    private void startRongtaPollTimer() {
        if (rongtaPollTimer != null) {
            return;
        }
        rongtaPollTimer = new Timer(150, e -> pollRongtaScan());
        rongtaPollTimer.start();
    }

    private void stopRongtaPollTimer() {
        if (rongtaPollTimer != null) {
            rongtaPollTimer.stop();
            rongtaPollTimer = null;
        }
        lastAutoRongtaText = "";
    }

    private void pollRongtaScan() {
        if (txt_cariBarang == null) {
            return;
        }
        String q = txt_cariBarang.getText();
        System.out.println("[timer tick] text=" + q);
        if (!isRongtaScanText(q)) {
            lastAutoRongtaText = "";
            return;
        }
        if (q.equals(lastAutoRongtaText)) {
            return;
        }
        lastAutoRongtaText = q;
        tryScanOrAdd();
    }

    void tampilBarang() {
        catalog.clear();
        try {
            String tampilBarang = "SELECT produk.kode_produk, produk.nama_produk, kategori.nama_kategori, "
                    + "produk.stok_produk, produk.harga_jual, satuan.nama_satuan, satuan.allow_decimal "
                    + "FROM produk JOIN kategori ON produk.kategori_Id = kategori.kategori_Id "
                    + "JOIN satuan ON produk.satuan_Id = satuan.satuan_Id "
                    + "ORDER BY produk.nama_produk;";
            ps = Koneksi.getConnection().prepareStatement(tampilBarang);
            rs = ps.executeQuery();

            DefaultTableModel barang = new DefaultTableModel(
                    new Object[]{"CODE", "PRODUCT", "CATEGORY", "STOCK", "PRICE"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            while (rs.next()) {
                BigDecimal stock = rs.getBigDecimal("produk.stok_produk");
                if (stock == null) {
                    stock = BigDecimal.ZERO;
                }
                ProductKey p = new ProductKey(
                        rs.getString("produk.kode_produk"),
                        rs.getString("produk.nama_produk"),
                        rs.getString("kategori.nama_kategori"),
                        stock,
                        rs.getInt("produk.harga_jual"),
                        rs.getString("satuan.nama_satuan"),
                        rs.getInt("satuan.allow_decimal") == 1);
                catalog.add(p);
                barang.addRow(new Object[]{p.code, p.name, p.category, p.stock, p.price});
            }
            tbl_barang.setModel(barang);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please check again: " + e);
        }
    }

    void cariBarang() {
        String q = txt_cariBarang.getText().trim();
        if (q.isEmpty()) {
            tampilBarang();
            loadQuickKeys();
            return;
        }
        DefaultTableModel Barang = new DefaultTableModel(
                new Object[]{"CODE", "PRODUCT", "CATEGORY", "STOCK", "PRICE"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        try {
            String cari = "SELECT produk.kode_produk, produk.nama_produk, kategori.nama_kategori, "
                    + "produk.stok_produk, produk.harga_jual, satuan.nama_satuan, satuan.allow_decimal "
                    + "FROM produk JOIN kategori ON produk.kategori_Id = kategori.kategori_Id "
                    + "JOIN satuan ON produk.satuan_Id = satuan.satuan_Id "
                    + "WHERE produk.kode_produk LIKE '%" + q + "%' || produk.nama_produk LIKE '%"
                    + q + "%' || kategori.nama_kategori LIKE '%" + q + "%'";
            ps = Koneksi.getConnection().prepareStatement(cari);
            rs = ps.executeQuery();
            catalog.clear();
            while (rs.next()) {
                BigDecimal stock = rs.getBigDecimal("produk.stok_produk");
                if (stock == null) {
                    stock = BigDecimal.ZERO;
                }
                ProductKey p = new ProductKey(
                        rs.getString("produk.kode_produk"),
                        rs.getString("produk.nama_produk"),
                        rs.getString("kategori.nama_kategori"),
                        stock,
                        rs.getInt("produk.harga_jual"),
                        rs.getString("satuan.nama_satuan"),
                        rs.getInt("satuan.allow_decimal") == 1);
                catalog.add(p);
                Barang.addRow(new Object[]{p.code, p.name, p.category, p.stock, p.price});
            }
            tbl_barang.setModel(Barang);
            renderQuickKeysFromCatalog();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please check again: " + e);
        }
    }

    void setEditableFalse() {
        txt_kodeBarang.setEditable(false);
        txt_kategori.setEditable(false);
        txt_namaBarang.setEditable(false);
        txt_hargaJual.setEditable(false);
        txt_subTotal.setEditable(false);
        txt_namaKasir.setEditable(false);
        txt_idKasir.setEditable(false);
        txt_uangKembalian.setEditable(false);
        txt_totalPembayaran.setEditable(false);
    }

    void id() {
        try {
            String idPenjualan = "SELECT MAX(penjualan.penjualan_Id) FROM penjualan;";
            ps = Koneksi.getConnection().prepareStatement(idPenjualan);
            rs = ps.executeQuery();
            if (rs.next()) {
                int idJual = rs.getInt(1);
                txt_kodeTransaksi.setText(Integer.toString(idJual + 1));
                txt_kodeTransaksi.setEditable(false);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please check again: " + e);
        }
        refreshMeta();
    }

    void date() {
        Date tanggal = new Date();
        SimpleDateFormat formatTanggal = new SimpleDateFormat("yyyy-MM-dd");
        txt_tanggalTransaksi.setText(formatTanggal.format(tanggal));
        txt_tanggalTransaksi.setEditable(false);
        refreshMeta();
    }

    void bersihInput() {
        txt_kategori.setText("");
        txt_kodeBarang.setText("");
        txt_namaBarang.setText("");
        txt_jumlahBarang.setText("");
        txt_hargaJual.setText("");
        txt_subTotal.setText("");
    }

    void totalPembayaran() {
        int jumlahBaris = tbl_detailBarang.getRowCount();
        int totalPembayaran = 0;
        for (int i = 0; i < jumlahBaris; i++) {
            totalPembayaran += Integer.parseInt(tbl_detailBarang.getValueAt(i, 4).toString());
        }
        txt_totalPembayaran.setText(Integer.toString(totalPembayaran));
        recalcChange();
        updateCartStatus();
        updateMoneyLabels();
        rebuildTicketLines();
    }

    private int getSubtotalKotor() {
        try {
            return Integer.parseInt(nullToZero(txt_totalPembayaran.getText()));
        } catch (Exception e) {
            return 0;
        }
    }

    private int discountCapAmount(int subtotal) {
        if (subtotal <= 0 || maxDiscountPercent <= 0) {
            return 0;
        }
        return (int) ((long) subtotal * maxDiscountPercent / 100);
    }

    /**
     * Discount in rupees from the field (Rs amount or % of subtotal).
     * Already limited to the admin max percent of subtotal.
     */
    private int parsedDiscountAmount() {
        int subtotal = getSubtotalKotor();
        if (subtotal <= 0 || txt_discount == null) {
            return 0;
        }
        String raw = txt_discount.getText().trim();
        if (raw.isEmpty()) {
            return 0;
        }
        int entered;
        try {
            entered = Integer.parseInt(raw.replace(",", ""));
        } catch (Exception e) {
            return 0;
        }
        if (entered < 0) {
            entered = 0;
        }
        loadMaxDiscount();
        int amount;
        boolean percent = cb_discountMode != null && "%".equals(String.valueOf(cb_discountMode.getSelectedItem()));
        if (percent) {
            if (entered > maxDiscountPercent) {
                entered = maxDiscountPercent;
            }
            amount = (int) ((long) subtotal * entered / 100);
        } else {
            amount = entered;
        }
        int cap = discountCapAmount(subtotal);
        if (amount > cap) {
            amount = cap;
        }
        if (amount > subtotal) {
            amount = subtotal;
        }
        return amount;
    }

    /** Discount actually charged — never above admin max_discount_percent of subtotal. */
    private int appliedDiscount() {
        return parsedDiscountAmount();
    }

    /**
     * Clamp the discount field to the admin max and notify when the cashier goes over.
     */
    private void enforceDiscountLimit() {
        if (txt_discount == null) {
            return;
        }
        loadMaxDiscount();
        int subtotal = getSubtotalKotor();
        String raw = txt_discount.getText().trim();
        if (raw.isEmpty() || subtotal <= 0) {
            return;
        }
        int entered;
        try {
            entered = Integer.parseInt(raw.replace(",", ""));
        } catch (Exception e) {
            return;
        }
        if (entered < 0) {
            entered = 0;
        }
        boolean percent = cb_discountMode != null && "%".equals(String.valueOf(cb_discountMode.getSelectedItem()));
        int capRs = discountCapAmount(subtotal);
        if (percent) {
            if (entered > maxDiscountPercent) {
                txt_discount.setText(Integer.toString(maxDiscountPercent));
                JOptionPane.showMessageDialog(this,
                        "Discount cannot exceed " + maxDiscountPercent + "% "
                        + "(admin limit · max " + money(capRs) + ").");
            }
        } else if (entered > capRs) {
            txt_discount.setText(Integer.toString(capRs));
            JOptionPane.showMessageDialog(this,
                    "Discount cannot exceed " + maxDiscountPercent + "% of the subtotal "
                    + "(max " + money(capRs) + ").");
        }
    }

    private int getNetTotal() {
        int net = getSubtotalKotor() - appliedDiscount();
        return net < 0 ? 0 : net;
    }

    private void loadMaxDiscount() {
        maxDiscountPercent = Settings.getInt("max_discount_percent", 10);
        if (maxDiscountPercent < 0) {
            maxDiscountPercent = 0;
        }
        if (maxDiscountPercent > 100) {
            maxDiscountPercent = 100;
        }
    }

    private void loadPaymentMethods() {
        if (cb_metode == null) {
            return;
        }
        DefaultComboBoxModel<MetodeItem> model = new DefaultComboBoxModel<MetodeItem>();
        PreparedStatement localPs = null;
        ResultSet localRs = null;
        try {
            localPs = Koneksi.getConnection().prepareStatement(
                    "SELECT metode_Id, nama_metode FROM metode_bayar WHERE aktif = 1 ORDER BY nama_metode");
            localRs = localPs.executeQuery();
            int cashIndex = 0;
            int i = 0;
            while (localRs.next()) {
                String nama = localRs.getString("nama_metode");
                model.addElement(new MetodeItem(localRs.getInt("metode_Id"), nama));
                if ("Cash".equalsIgnoreCase(nama)) {
                    cashIndex = i;
                }
                i++;
            }
            cb_metode.setModel(model);
            if (model.getSize() > 0) {
                cb_metode.setSelectedIndex(cashIndex);
            }
            updateDeliveryManVisibility();
        } catch (Exception e) {
            cb_metode.setModel(model);
        } finally {
            if (localRs != null) {
                try {
                    localRs.close();
                } catch (Exception ignored) {
                }
            }
            if (localPs != null) {
                try {
                    localPs.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private boolean isDeliveryPayment(MetodeItem metode) {
        if (metode == null || metode.nama == null) {
            return false;
        }
        String n = metode.nama.trim().toLowerCase(Locale.ROOT);
        return n.contains("on delivery");
    }

    private boolean isCashPayment(MetodeItem metode) {
        return metode != null && metode.nama != null
                && "Cash".equalsIgnoreCase(metode.nama.trim());
    }

    private void updateDeliveryManVisibility() {
        MetodeItem metode = cb_metode == null ? null : (MetodeItem) cb_metode.getSelectedItem();
        boolean show = isDeliveryPayment(metode);
        if (pn_kurirRow != null) {
            pn_kurirRow.setVisible(show);
        }
        if (lb_kurirCap != null) {
            lb_kurirCap.setVisible(show);
        }
        if (!show && txt_kurir != null) {
            txt_kurir.setText("");
        }
        if (pn_kurirRow != null && pn_kurirRow.getParent() != null) {
            pn_kurirRow.getParent().revalidate();
            pn_kurirRow.getParent().repaint();
        }
        updateCashFieldsVisibility();
    }

    private void updateCashFieldsVisibility() {
        MetodeItem metode = cb_metode == null ? null : (MetodeItem) cb_metode.getSelectedItem();
        boolean show = isCashPayment(metode);
        if (pn_cashReceivedRow != null) {
            pn_cashReceivedRow.setVisible(show);
        }
        if (pn_changeDueRow != null) {
            pn_changeDueRow.setVisible(show);
        }
        if (!show) {
            if (txt_uangDiterima != null) {
                txt_uangDiterima.setText("");
            }
            if (txt_uangKembalian != null) {
                txt_uangKembalian.setText("0");
            }
            if (lb_changeVal != null) {
                lb_changeVal.setText(money(0));
            }
        } else {
            recalcChange();
            updateMoneyLabels();
        }
        if (pn_cashReceivedRow != null && pn_cashReceivedRow.getParent() != null) {
            pn_cashReceivedRow.getParent().revalidate();
            pn_cashReceivedRow.getParent().repaint();
        }
    }

    private void selectCustomer(int id, String nama) {
        selectedPelangganId = Integer.valueOf(id);
        selectedPelangganName = nama;
        hideCustomerSuggestions();
        if (txt_customer != null) {
            suppressCustomerSuggest = true;
            txt_customer.setText(nama);
            suppressCustomerSuggest = false;
        }
        refreshTicketCustomer();
    }

    private void clearCustomer() {
        selectedPelangganId = null;
        selectedPelangganName = null;
        hideCustomerSuggestions();
        if (txt_customer != null && !txt_customer.hasFocus()) {
            suppressCustomerSuggest = true;
            txt_customer.setText("");
            suppressCustomerSuggest = false;
        }
        refreshTicketCustomer();
    }

    private String lookupPelangganUuid(int pelangganId) {
        PreparedStatement localPs = null;
        ResultSet localRs = null;
        try {
            localPs = Koneksi.getConnection().prepareStatement(
                    "SELECT uuid FROM pelanggan WHERE pelanggan_Id = ?");
            localPs.setInt(1, pelangganId);
            localRs = localPs.executeQuery();
            if (localRs.next()) {
                return localRs.getString(1);
            }
        } catch (Exception e) {
            System.out.println("lookupPelangganUuid: " + e);
        } finally {
            try {
                if (localRs != null) {
                    localRs.close();
                }
            } catch (Exception ignored) {
            }
            try {
                if (localPs != null) {
                    localPs.close();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private void refreshTicketCustomer() {
        if (lb_ticketCustomer == null) {
            return;
        }
        if (selectedPelangganName != null && !selectedPelangganName.isEmpty()) {
            lb_ticketCustomer.setText(selectedPelangganName);
        } else {
            lb_ticketCustomer.setText("Walk-in");
        }
    }

    private void ensureCustomerSuggestUi() {
        if (customerSuggestPopup != null) {
            return;
        }
        customerSuggestList = new JList<CustomerMatch>();
        customerSuggestList.setFont(UITheme.FONT_REGULAR.deriveFont(13f));
        customerSuggestList.setVisibleRowCount(6);
        customerSuggestList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        customerSuggestList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 1) {
                    acceptCustomerSuggestion();
                }
            }
        });
        JScrollPane sp = new JScrollPane(customerSuggestList);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setPreferredSize(new Dimension(280, 140));
        customerSuggestPopup = new JPopupMenu();
        customerSuggestPopup.setBorder(BorderFactory.createLineBorder(RULE));
        customerSuggestPopup.add(sp);
        customerSuggestTimer = new Timer(180, e -> showCustomerSuggestions());
        customerSuggestTimer.setRepeats(false);
    }

    private void scheduleCustomerSuggestions() {
        if (suppressCustomerSuggest || txt_customer == null) {
            return;
        }
        ensureCustomerSuggestUi();
        // Typing away from a selected customer clears the selection until they pick again.
        if (selectedPelangganName != null
                && !selectedPelangganName.equals(txt_customer.getText().trim())) {
            selectedPelangganId = null;
            selectedPelangganName = null;
            refreshTicketCustomer();
        }
        customerSuggestTimer.restart();
    }

    private void showCustomerSuggestions() {
        if (txt_customer == null || !txt_customer.isShowing()) {
            return;
        }
        String q = txt_customer.getText().trim();
        if (q.isEmpty()) {
            hideCustomerSuggestions();
            selectedPelangganId = null;
            selectedPelangganName = null;
            refreshTicketCustomer();
            return;
        }
        List<CustomerMatch> matches = queryCustomers(q, 12);
        if (matches.isEmpty()) {
            hideCustomerSuggestions();
            return;
        }
        ensureCustomerSuggestUi();
        customerSuggestList.setListData(matches.toArray(new CustomerMatch[0]));
        customerSuggestList.setSelectedIndex(0);
        customerSuggestPopup.setPopupSize(Math.max(txt_customer.getWidth(), 240),
                Math.min(160, 28 + matches.size() * 22));
        if (!customerSuggestPopup.isVisible()) {
            customerSuggestPopup.show(txt_customer, 0, txt_customer.getHeight());
        } else {
            customerSuggestPopup.pack();
            customerSuggestPopup.setVisible(true);
        }
        txt_customer.requestFocusInWindow();
    }

    private void hideCustomerSuggestions() {
        if (customerSuggestPopup != null && customerSuggestPopup.isVisible()) {
            customerSuggestPopup.setVisible(false);
        }
    }

    private void acceptCustomerSuggestion() {
        if (customerSuggestList == null) {
            return;
        }
        CustomerMatch m = customerSuggestList.getSelectedValue();
        if (m == null) {
            hideCustomerSuggestions();
            return;
        }
        selectCustomer(m.id, m.nama);
    }

    private List<CustomerMatch> queryCustomers(String q, int limit) {
        List<CustomerMatch> matches = new ArrayList<CustomerMatch>();
        PreparedStatement localPs = null;
        ResultSet localRs = null;
        try {
            localPs = Koneksi.getConnection().prepareStatement(
                    "SELECT pelanggan_Id, nama_pelanggan, telp_pelanggan FROM pelanggan "
                    + "WHERE nama_pelanggan LIKE ? OR telp_pelanggan LIKE ? "
                    + "ORDER BY "
                    + "CASE WHEN nama_pelanggan LIKE ? THEN 0 ELSE 1 END, "
                    + "nama_pelanggan LIMIT ?");
            String like = "%" + q + "%";
            String prefix = q + "%";
            localPs.setString(1, like);
            localPs.setString(2, like);
            localPs.setString(3, prefix);
            localPs.setInt(4, limit);
            localRs = localPs.executeQuery();
            while (localRs.next()) {
                String telp = localRs.getString("telp_pelanggan");
                matches.add(new CustomerMatch(
                        localRs.getInt("pelanggan_Id"),
                        localRs.getString("nama_pelanggan"),
                        telp == null ? "" : telp));
            }
        } catch (Exception e) {
            // leave empty — caller shows no popup
        } finally {
            if (localRs != null) {
                try {
                    localRs.close();
                } catch (Exception ignored) {
                }
            }
            if (localPs != null) {
                try {
                    localPs.close();
                } catch (Exception ignored) {
                }
            }
        }
        return matches;
    }

    private void searchCustomer() {
        String q = txt_customer.getText().trim();
        if (q.isEmpty()) {
            clearCustomer();
            if (txt_customer != null) {
                suppressCustomerSuggest = true;
                txt_customer.setText("");
                suppressCustomerSuggest = false;
            }
            return;
        }
        if (customerSuggestPopup != null && customerSuggestPopup.isVisible()
                && customerSuggestList != null && customerSuggestList.getSelectedValue() != null) {
            acceptCustomerSuggestion();
            return;
        }
        List<CustomerMatch> matches = queryCustomers(q, 20);
        if (matches.isEmpty()) {
            hideCustomerSuggestions();
            JOptionPane.showMessageDialog(this,
                    "No customer found. Use + New customer to add one.");
            return;
        }
        if (matches.size() == 1) {
            CustomerMatch m = matches.get(0);
            selectCustomer(m.id, m.nama);
            return;
        }
        ensureCustomerSuggestUi();
        customerSuggestList.setListData(matches.toArray(new CustomerMatch[0]));
        customerSuggestList.setSelectedIndex(0);
        customerSuggestPopup.setPopupSize(Math.max(txt_customer.getWidth(), 240), 160);
        customerSuggestPopup.show(txt_customer, 0, txt_customer.getHeight());
        txt_customer.requestFocusInWindow();
    }

    private void newCustomer() {
        Integer id = Form_Pelanggan.promptNew(this);
        if (id == null) {
            return;
        }
        PreparedStatement localPs = null;
        ResultSet localRs = null;
        try {
            localPs = Koneksi.getConnection().prepareStatement(
                    "SELECT nama_pelanggan FROM pelanggan WHERE pelanggan_Id = ?");
            localPs.setInt(1, id.intValue());
            localRs = localPs.executeQuery();
            if (localRs.next()) {
                selectCustomer(id.intValue(), localRs.getString(1));
            }
        } catch (Exception e) {
            selectCustomer(id.intValue(), txt_customer.getText().trim());
        } finally {
            if (localRs != null) {
                try {
                    localRs.close();
                } catch (Exception ignored) {
                }
            }
            if (localPs != null) {
                try {
                    localPs.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void recalcChange() {
        try {
            int total = getNetTotal();
            int cash = Integer.parseInt(nullToZero(txt_uangDiterima.getText()));
            if (cash >= total && total > 0) {
                txt_uangKembalian.setText(Integer.toString(cash - total));
            } else {
                txt_uangKembalian.setText("0");
            }
        } catch (Exception e) {
            txt_uangKembalian.setText("0");
        }
    }

    private String nullToZero(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "0";
        }
        return s.trim().replace(",", "");
    }

    private void updateCartStatus() {
        if (lb_cartStatus == null) {
            return;
        }
        int lines = tbl_detailBarang.getRowCount();
        BigDecimal units = BigDecimal.ZERO;
        for (int i = 0; i < lines; i++) {
            try {
                units = units.add(QuantityUtil.parse(tbl_detailBarang.getValueAt(i, 2).toString()));
            } catch (Exception ignored) {
            }
        }
        lb_cartStatus.setText(lines + " lines · " + QuantityUtil.format(units, true) + " units");
    }

    private void updateMoneyLabels() {
        int subtotal = getSubtotalKotor();
        int discount = appliedDiscount();
        int net = getNetTotal();
        if (lb_subtotalVal != null) {
            lb_subtotalVal.setText(money(subtotal));
        }
        if (lb_discountVal != null) {
            lb_discountVal.setText(money(discount));
        }
        if (lb_changeVal != null) {
            lb_changeVal.setText(money(Integer.parseInt(nullToZero(txt_uangKembalian.getText()))));
        }
        if (lb_totalDue != null) {
            lb_totalDue.setText(money(net));
        }
        refreshTicketCustomer();
    }

    private void refreshMeta() {
        if (lb_cashierMeta != null) {
            lb_cashierMeta.setText(txt_namaKasir.getText());
        }
        if (lb_ticketMeta != null) {
            try {
                int n = Integer.parseInt(txt_kodeTransaksi.getText().trim());
                lb_ticketMeta.setText("TX-" + String.format("%04d", n));
            } catch (Exception e) {
                lb_ticketMeta.setText("TX-" + txt_kodeTransaksi.getText());
            }
        }
        if (lb_timeMeta != null) {
            lb_timeMeta.setText(new SimpleDateFormat("d MMM · HH:mm").format(new Date()));
        }
    }

    private String money(int amount) {
        return UITheme.CURRENCY + " " + NumberFormat.getIntegerInstance(Locale.US).format(amount);
    }

    private void loadCategories() {
        pn_categoryTabs.removeAll();
        categoryButtons.clear();
        addCategoryTab("All items");
        try {
            ps = Koneksi.getConnection().prepareStatement(
                    "SELECT nama_kategori FROM kategori ORDER BY nama_kategori");
            rs = ps.executeQuery();
            while (rs.next()) {
                addCategoryTab(rs.getString(1));
            }
        } catch (Exception ignored) {
        }
        selectCategory("All items");
        pn_categoryTabs.revalidate();
        pn_categoryTabs.repaint();
    }

    private void addCategoryTab(String name) {
        JButton tab = new JButton(name);
        tab.setFocusPainted(false);
        tab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tab.setFont(UITheme.FONT_BOLD.deriveFont(12f));
        tab.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RULE),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        tab.addActionListener(e -> selectCategory(name));
        categoryButtons.add(tab);
        pn_categoryTabs.add(tab);
        pn_categoryTabs.add(Box.createHorizontalStrut(6));
    }

    private void selectCategory(String name) {
        activeCategory = name;
        for (JButton b : categoryButtons) {
            boolean on = name.equals(b.getText());
            if (on) {
                b.setBackground(PageUI.INK);
                b.setForeground(Color.WHITE);
                b.setOpaque(true);
                b.setBorder(BorderFactory.createEmptyBorder(9, 15, 9, 15));
            } else {
                b.setBackground(UITheme.SURFACE);
                b.setForeground(PageUI.INK);
                b.setOpaque(true);
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(RULE),
                        BorderFactory.createEmptyBorder(8, 14, 8, 14)));
            }
        }
        loadQuickKeys();
    }

    private void loadQuickKeys() {
        List<ProductKey> keys = new ArrayList<>();
        try {
            ps = Koneksi.getConnection().prepareStatement("CALL TopProduct('8');");
            rs = ps.executeQuery();
            while (rs.next()) {
                String code = rs.getString("kode_produk");
                ProductKey fromCat = findInCatalog(code);
                if (fromCat != null) {
                    keys.add(fromCat);
                } else {
                    keys.add(new ProductKey(
                            code,
                            rs.getString("nama_produk"),
                            "",
                            BigDecimal.valueOf(999),
                            rs.getInt("harga_jual"),
                            "Piece",
                            false));
                }
            }
        } catch (Exception e) {
            // Fall back to first catalog items.
        }
        if (keys.isEmpty()) {
            for (ProductKey p : catalog) {
                if (keys.size() >= 8) {
                    break;
                }
                keys.add(p);
            }
        }
        if (!"All items".equals(activeCategory)) {
            List<ProductKey> filtered = new ArrayList<>();
            for (ProductKey p : keys) {
                if (activeCategory.equalsIgnoreCase(p.category)) {
                    filtered.add(p);
                }
            }
            if (filtered.isEmpty()) {
                for (ProductKey p : catalog) {
                    if (activeCategory.equalsIgnoreCase(p.category) && filtered.size() < 8) {
                        filtered.add(p);
                    }
                }
            }
            keys = filtered;
        }
        renderQuickKeys(keys);
    }

    private void renderQuickKeysFromCatalog() {
        List<ProductKey> keys = new ArrayList<>();
        for (ProductKey p : catalog) {
            if (keys.size() >= 8) {
                break;
            }
            if ("All items".equals(activeCategory) || activeCategory.equalsIgnoreCase(p.category)) {
                keys.add(p);
            }
        }
        renderQuickKeys(keys);
    }

    private ProductKey findInCatalog(String code) {
        for (ProductKey p : catalog) {
            if (p.code.equals(code)) {
                return p;
            }
        }
        return null;
    }

    private void renderQuickKeys(List<ProductKey> keys) {
        pn_quickKeys.removeAll();
        int slots = Math.max(8, ((keys.size() + 3) / 4) * 4);
        for (int i = 0; i < slots; i++) {
            int top = i < 4 ? 0 : 1;
            int left = (i % 4 == 0) ? 0 : 1;
            if (i < keys.size()) {
                JPanel card = quickKeyCard(keys.get(i));
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(top, left, 0, 0, UITheme.GRID_LINE),
                        BorderFactory.createEmptyBorder(16, 14, 16, 14)));
                pn_quickKeys.add(card);
            } else {
                JPanel empty = new JPanel();
                empty.setOpaque(true);
                empty.setBackground(UITheme.SURFACE);
                empty.setBorder(BorderFactory.createMatteBorder(top, left, 0, 0, UITheme.GRID_LINE));
                pn_quickKeys.add(empty);
            }
        }
        pn_quickKeys.revalidate();
        pn_quickKeys.repaint();
    }

    private JPanel quickKeyCard(ProductKey p) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UITheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 0, UITheme.GRID_LINE),
                BorderFactory.createEmptyBorder(16, 14, 16, 14)));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setOpaque(true);

        JLabel cat = new JLabel(p.category == null || p.category.isEmpty()
                ? "ITEM" : p.category.toUpperCase(Locale.ROOT));
        cat.setFont(UITheme.FONT_CAPTION);
        cat.setForeground(UITheme.TEXT_CAPTION);
        cat.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel name = new JLabel(p.name);
        name.setFont(UITheme.FONT_BOLD.deriveFont(15f));
        name.setForeground(PageUI.INK);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel price = new JLabel(money(p.price));
        price.setFont(UITheme.FONT_REGULAR.deriveFont(13f));
        price.setForeground(PageUI.INK);
        price.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(cat);
        card.add(Box.createVerticalStrut(8));
        card.add(name);
        card.add(Box.createVerticalGlue());
        card.add(Box.createVerticalStrut(10));
        card.add(price);

        final Timer[] holdTimer = new Timer[1];
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                holdTimer[0] = new Timer(550, ev -> {
                    holdTimer[0].stop();
                    promptQtyAndAdd(p);
                });
                holdTimer[0].setRepeats(false);
                holdTimer[0].start();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (holdTimer[0] != null && holdTimer[0].isRunning()) {
                    holdTimer[0].stop();
                    addProductToCart(p, BigDecimal.ONE);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (holdTimer[0] != null && holdTimer[0].isRunning()) {
                    holdTimer[0].stop();
                }
            }
        });
        return card;
    }

    private void promptQtyAndAdd(ProductKey p) {
        String input = JOptionPane.showInputDialog(this, "Quantity for " + p.name + ":", "1");
        if (input == null) {
            return;
        }
        try {
            BigDecimal qty = QuantityUtil.parse(input);
            String err = QuantityUtil.validate(qty, p.allowDecimal);
            if (err != null) {
                JOptionPane.showMessageDialog(this, err);
                return;
            }
            addProductToCart(p, qty);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Enter a valid quantity.");
        }
    }

    private static boolean hasFractionalQty(BigDecimal qty) {
        return qty != null && qty.stripTrailingZeros().scale() > 0;
    }

    private void addProductToCart(ProductKey p, BigDecimal qty) {
        addProductToCart(p, qty, BigDecimal.valueOf(p.price), true);
    }

    private void addProductToCart(ProductKey p, BigDecimal qty, BigDecimal priceOverride) {
        addProductToCart(p, qty, priceOverride, false);
    }

    private void addProductToCart(ProductKey p, BigDecimal qty, BigDecimal priceOverride, boolean checkStock) {
        boolean scaleWeighted = hasFractionalQty(qty);
        String err;
        if (scaleWeighted) {
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                err = "Enter a valid quantity.";
            } else if (qty.scale() > 3) {
                err = "Quantity allows up to 3 decimal places.";
            } else {
                err = null;
            }
        } else {
            err = QuantityUtil.validate(qty, p.allowDecimal);
        }
        if (err != null) {
            JOptionPane.showMessageDialog(this, err);
            return;
        }
        if (checkStock && p.stock.compareTo(qty) < 0) {
            JOptionPane.showMessageDialog(this,
                    "Quantity exceeds stock (" + QuantityUtil.formatWithUnit(p.stock, p.unitName, p.allowDecimal) + ").");
            return;
        }
        boolean scaleTotalLine = !checkStock;
        int lineSubtotal;
        int unitPrice;
        if (scaleTotalLine) {
            lineSubtotal = priceOverride.setScale(0, RoundingMode.HALF_UP).intValue();
            unitPrice = priceOverride.divide(qty, 0, RoundingMode.HALF_UP).intValue();
        } else {
            unitPrice = priceOverride.setScale(0, RoundingMode.HALF_UP).intValue();
            lineSubtotal = QuantityUtil.moneySubtotal(qty, unitPrice);
        }
        DefaultTableModel model = (DefaultTableModel) tbl_detailBarang.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            if (p.code.equals(model.getValueAt(i, 0).toString())) {
                BigDecimal existing = QuantityUtil.parse(model.getValueAt(i, 2).toString());
                BigDecimal next = existing.add(qty);
                if (checkStock && p.stock.compareTo(next) < 0) {
                    JOptionPane.showMessageDialog(this,
                            "Quantity exceeds stock (" + QuantityUtil.formatWithUnit(p.stock, p.unitName, p.allowDecimal) + ").");
                    return;
                }
                int combinedSubtotal = Integer.parseInt(model.getValueAt(i, 4).toString()) + lineSubtotal;
                int mergedUnitPrice = scaleTotalLine
                        ? priceOverride.divide(qty, 0, RoundingMode.HALF_UP).intValue()
                        : unitPrice;
                model.setValueAt(QuantityUtil.format(next, true), i, 2);
                model.setValueAt(mergedUnitPrice, i, 3);
                model.setValueAt(combinedSubtotal, i, 4);
                totalPembayaran();
                return;
            }
        }
        model.addRow(new Object[]{
            p.code, p.name, QuantityUtil.format(qty, true), unitPrice, lineSubtotal});
        totalPembayaran();
    }

    private static boolean isRongtaScanText(String text) {
        if (text == null) {
            return false;
        }
        return text.contains("code=")
                && text.contains("weight=")
                && text.contains("price=")
                && text.trim().endsWith("/kg");
    }

    private void tryScanOrAdd() {
        String q = txt_cariBarang.getText().trim();
        if (q.isEmpty()) {
            return;
        }
        if (q.length() == 18 && q.startsWith("21")) {
            String pluStr = q.substring(2, 6);
            String priceRawStr = q.substring(6, 12);
            String weightRawStr = q.substring(12, 17);
            try {
                BigDecimal price = BigDecimal.valueOf(Long.parseLong(priceRawStr))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal weight = BigDecimal.valueOf(Long.parseLong(weightRawStr))
                        .divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
                BigDecimal unitPrice = price.divide(weight, 2, RoundingMode.HALF_UP);
                ProductKey match = null;
                // Try exact match first
                for (ProductKey p : catalog) {
                    if (p.code.equals(pluStr)) {
                        match = p;
                        break;
                    }
                }
                // Try stripping leading zeros from both
                if (match == null) {
                    String pluStripped = String.valueOf(Long.parseLong(pluStr));
                    for (ProductKey p : catalog) {
                        try {
                            String codeStripped = String.valueOf(Long.parseLong(p.code));
                            if (codeStripped.equals(pluStripped)) {
                                match = p;
                                break;
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                if (match != null) {
                    addProductToCart(match, weight, price);
                    txt_cariBarang.setText("");
                } else {
                    JOptionPane.showMessageDialog(this, "PLU [" + pluStr + "] not found.");
                }
            } catch (NumberFormatException e) {
                // Not a valid 18-digit scale barcode, fall through to normal lookup
            }
            return;
        }
        if (isRongtaScanText(q)) {
            System.out.println("[rongta] triggered, text=" + q);
            try {
                // Parse code
                String code = q.replaceAll(".*code=([^;]+);.*", "$1").trim();

                // Parse weight
                String weightStr = q.replaceAll(".*weight=\\s*([0-9.]+)\\s*kg.*", "$1").trim();
                BigDecimal weight = new BigDecimal(weightStr);

                // Parse total price
                String priceStr = q.replaceAll(".*price=\\s*([0-9.]+)\\s*;.*", "$1").trim();
                BigDecimal totalPrice = new BigDecimal(priceStr);

                // Parse unit price (per kg)
                String unitPriceStr = q.replaceAll(".*unit=\\s*([0-9.]+)\\s*/kg.*", "$1").trim();
                BigDecimal unitPrice = new BigDecimal(unitPriceStr);

                // Find product — exact match first, then ends-with match
                long codeNum = Long.parseLong(code.replaceAll("^0+", "0").replaceAll("^0+([1-9])", "$1"));
                ProductKey match = null;
                for (ProductKey p : catalog) {
                    try {
                        if (Long.parseLong(p.code) == codeNum) {
                            match = p;
                            break;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (match == null) {
                    for (ProductKey p : catalog) {
                        try {
                            if (Long.parseLong(p.code) % 10000 == codeNum % 10000) {
                                match = p;
                                break;
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                if (match != null) {
                    addProductToCart(match, weight, totalPrice);
                    txt_cariBarang.setText("");
                } else {
                    JOptionPane.showMessageDialog(this, "Product code " + code + " not found.");
                }
            } catch (Exception ex) {
                // Not a valid Rongta text — fall through to normal lookup
            }
            return;
        }
        if (q.length() == 13 && q.startsWith("21")) {
            // Scale label from Rongta
            String pluStr = q.substring(2, 6);       // 4-digit PLU
            String priceStr = q.substring(6, 12);    // 6-digit price
            try {
                int plu = Integer.parseInt(pluStr);
                BigDecimal price = new BigDecimal(Long.parseLong(priceStr))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                // Find product where kode_produk % 10000 == plu
                ProductKey match = null;
                for (ProductKey p : catalog) {
                    try {
                        long kode = Long.parseLong(p.code);
                        if (kode % 10000 == plu) {
                            match = p;
                            break;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (match != null) {
                    addProductToCart(match, BigDecimal.ONE, price);
                    txt_cariBarang.setText("");
                } else {
                    JOptionPane.showMessageDialog(this,
                            "PLU " + pluStr + " not found.");
                }
                return;
            } catch (NumberFormatException e) {
                // Not a valid scale barcode, fall through to normal lookup
            }
        }
        // Exact barcode match first
        for (ProductKey p : catalog) {
            if (p.code.equalsIgnoreCase(q)) {
                addProductToCart(p, BigDecimal.ONE);
                txt_cariBarang.setText("");
                return;
            }
        }
        cariBarang();
        if (catalog.size() == 1) {
            addProductToCart(catalog.get(0), BigDecimal.ONE);
            txt_cariBarang.setText("");
            tampilBarang();
            loadQuickKeys();
        } else if (catalog.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No product found.");
            tampilBarang();
            loadQuickKeys();
        }
    }

    private void rebuildTicketLines() {
        if (pn_ticketLines == null) {
            return;
        }
        pn_ticketLines.removeAll();
        DefaultTableModel model = (DefaultTableModel) tbl_detailBarang.getModel();
        if (model.getRowCount() == 0) {
            JLabel empty = new JLabel("Ticket is empty — scan or tap a quick key.");
            empty.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
            empty.setForeground(UITheme.TEXT_MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(18, 4, 18, 4));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            pn_ticketLines.add(empty);
        } else {
            for (int i = 0; i < model.getRowCount(); i++) {
                final int row = i;
                String code = model.getValueAt(i, 0).toString();
                String name = model.getValueAt(i, 1).toString();
                BigDecimal qty = QuantityUtil.parse(model.getValueAt(i, 2).toString());
                int price = Integer.parseInt(model.getValueAt(i, 3).toString());
                int sub = Integer.parseInt(model.getValueAt(i, 4).toString());
                ProductKey p = findInCatalog(code);
                String unitName = p != null ? p.unitName : "";
                boolean allowDecimal = p != null && p.allowDecimal;
                if (hasFractionalQty(qty)) {
                    allowDecimal = true;
                }
                pn_ticketLines.add(ticketLineRow(row, code, name, qty, unitName, allowDecimal, price, sub));
            }
        }
        pn_ticketLines.add(Box.createVerticalGlue());
        pn_ticketLines.revalidate();
        pn_ticketLines.repaint();
    }

    private JPanel ticketLineRow(int row, String code, String name, BigDecimal qty,
            String unitName, boolean allowDecimal, int price, int sub) {
        JPanel rowPanel = new JPanel(new BorderLayout(10, 0));
        rowPanel.setOpaque(false);
        rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, RULE),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setMinimumSize(new Dimension(48, 40));
        JLabel lbName = new JLabel(name);
        lbName.setFont(UITheme.FONT_BOLD.deriveFont(13f));
        lbName.setForeground(PageUI.INK);
        lbName.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbMeta = new JLabel(code + " · " + money(price) + " each");
        lbMeta.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lbMeta.setForeground(UITheme.TEXT_MUTED);
        lbMeta.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(lbName);
        left.add(Box.createVerticalStrut(2));
        left.add(lbMeta);

        JPanel qtyBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        qtyBox.setBackground(Color.WHITE);
        qtyBox.setBorder(BorderFactory.createLineBorder(RULE));
        JButton minus = qtyBtn("−");
        JLabel lbQty = new JLabel(QuantityUtil.formatWithUnit(qty, unitName, allowDecimal), SwingConstants.CENTER);
        lbQty.setFont(UITheme.FONT_BOLD.deriveFont(12f));
        // Grow with long unit labels; keep a usable minimum.
        Dimension qtyPref = lbQty.getPreferredSize();
        lbQty.setPreferredSize(new Dimension(Math.max(56, qtyPref.width + 8), 26));
        JButton plus = qtyBtn("+");
        minus.addActionListener(e -> changeQty(row, -1));
        plus.addActionListener(e -> changeQty(row, 1));
        qtyBox.add(minus);
        qtyBox.add(lbQty);
        qtyBox.add(plus);

        JLabel lbLine = new JLabel(money(sub), SwingConstants.RIGHT);
        lbLine.setFont(UITheme.FONT_BOLD.deriveFont(13f));
        lbLine.setForeground(PageUI.INK);
        // Never clip the line total — size to the full money string.
        Dimension moneyPref = lbLine.getPreferredSize();
        int moneyW = Math.max(moneyPref.width + 2, 72);
        lbLine.setPreferredSize(new Dimension(moneyW, moneyPref.height));
        lbLine.setMinimumSize(new Dimension(moneyW, moneyPref.height));
        lbLine.setMaximumSize(new Dimension(moneyW, moneyPref.height));

        JPanel right = new JPanel(new BorderLayout(10, 0));
        right.setOpaque(false);
        right.add(qtyBox, BorderLayout.CENTER);
        right.add(lbLine, BorderLayout.EAST);
        // Right block must not shrink below qty + full amount.
        Dimension rightPref = right.getPreferredSize();
        right.setMinimumSize(rightPref);
        right.setPreferredSize(rightPref);

        rowPanel.add(left, BorderLayout.CENTER);
        rowPanel.add(right, BorderLayout.EAST);

        // Preferred width 10 → BoxLayout expands to viewport; avoids clipping past the scroll edge.
        int rowH = Math.max(56, rightPref.height + 20);
        rowPanel.setPreferredSize(new Dimension(10, rowH));
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowH));
        rowPanel.setMinimumSize(new Dimension(120, rowH));

        rowPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) || e.getClickCount() == 2) {
                    removeLine(row);
                }
            }
        });
        return rowPanel;
    }

    private JButton qtyBtn(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setPreferredSize(new Dimension(28, 26));
        b.setFont(UITheme.FONT_BOLD.deriveFont(14f));
        b.setForeground(PageUI.INK);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void changeQty(int row, int delta) {
        DefaultTableModel model = (DefaultTableModel) tbl_detailBarang.getModel();
        if (row < 0 || row >= model.getRowCount()) {
            return;
        }
        BigDecimal qty = QuantityUtil.parse(model.getValueAt(row, 2).toString()).add(BigDecimal.valueOf(delta));
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            removeLine(row);
            return;
        }
        String code = model.getValueAt(row, 0).toString();
        ProductKey p = findInCatalog(code);
        if (p != null) {
            String err = QuantityUtil.validate(qty, p.allowDecimal);
            if (err != null) {
                JOptionPane.showMessageDialog(this, err);
                return;
            }
            if (p.stock.compareTo(qty) < 0) {
                JOptionPane.showMessageDialog(this,
                        "Quantity exceeds stock (" + QuantityUtil.formatWithUnit(p.stock, p.unitName, p.allowDecimal) + ").");
                return;
            }
        }
        int price = Integer.parseInt(model.getValueAt(row, 3).toString());
        model.setValueAt(qty.toPlainString(), row, 2);
        model.setValueAt(QuantityUtil.moneySubtotal(qty, price), row, 4);
        totalPembayaran();
    }

    private void removeLine(int row) {
        DefaultTableModel model = (DefaultTableModel) tbl_detailBarang.getModel();
        if (row >= 0 && row < model.getRowCount()) {
            model.removeRow(row);
            if (model.getRowCount() == 0) {
                txt_totalPembayaran.setText("0");
                txt_uangDiterima.setText("");
                txt_uangKembalian.setText("0");
            }
            totalPembayaran();
        }
    }

    private JPanel metaCell(String caption, JLabel value) {
        JPanel cell = new JPanel();
        cell.setBackground(TICKET_BG);
        cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, RULE),
                BorderFactory.createEmptyBorder(10, 14, 10, 16)));
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        JLabel cap = new JLabel(caption);
        cap.setFont(UITheme.FONT_CAPTION);
        cap.setForeground(UITheme.TEXT_CAPTION);
        cap.setAlignmentX(Component.LEFT_ALIGNMENT);
        value.setFont(UITheme.FONT_BOLD.deriveFont(13f));
        value.setForeground(PageUI.INK);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        cell.add(cap);
        cell.add(Box.createVerticalStrut(2));
        cell.add(value);
        return cell;
    }

    private JPanel summaryRow(String label, Component value, boolean inputRow) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        JLabel lb = new JLabel(label);
        lb.setFont(UITheme.FONT_REGULAR.deriveFont(13f));
        lb.setForeground(UITheme.TEXT_SECONDARY);
        row.add(lb, BorderLayout.WEST);
        if (value instanceof JLabel) {
            ((JLabel) value).setHorizontalAlignment(SwingConstants.RIGHT);
        }
        row.add(value, BorderLayout.EAST);
        return row;
    }

    /** Ticket totals row: label | optional controls | money — shared column widths. */
    private JPanel moneyRow(String label, Component controls, JLabel amount) {
        final int controlsW = 128;
        final int rowH = 32;
        // Wide enough for large rupee totals (e.g. Rs 99,999,999)
        final int amountW = amount != null
                ? Math.max(110, amount.getFontMetrics(UITheme.FONT_REGULAR.deriveFont(13f))
                        .stringWidth("Rs 99,999,999") + 8)
                : 110;

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowH + 8));
        row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lb = new JLabel(label);
        lb.setFont(UITheme.FONT_REGULAR.deriveFont(13f));
        lb.setForeground(UITheme.TEXT_SECONDARY);

        JPanel mid = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        mid.setOpaque(false);
        mid.setPreferredSize(new Dimension(controlsW, rowH));
        mid.setMinimumSize(new Dimension(controlsW, rowH));
        mid.setMaximumSize(new Dimension(controlsW, rowH));
        if (controls != null) {
            mid.add(controls);
        }

        if (amount != null) {
            amount.setHorizontalAlignment(SwingConstants.RIGHT);
            amount.setPreferredSize(new Dimension(amountW, rowH));
            amount.setMinimumSize(new Dimension(amountW, rowH));
            // No maximum clip — label may grow if needed
        } else {
            amount = new JLabel("");
            amount.setPreferredSize(new Dimension(amountW, rowH));
        }

        JPanel right = new JPanel(new BorderLayout(0, 0));
        right.setOpaque(false);
        right.add(mid, BorderLayout.CENTER);
        right.add(amount, BorderLayout.EAST);

        row.add(lb, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private JTextField moneyField(int width) {
        JTextField f = new JTextField();
        f.setFont(UITheme.FONT_BOLD.deriveFont(13f));
        f.setForeground(PageUI.INK);
        f.setBackground(UITheme.SURFACE);
        f.setHorizontalAlignment(SwingConstants.RIGHT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.GRID_LINE),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        Dimension d = new Dimension(width, 28);
        f.setPreferredSize(d);
        f.setMinimumSize(d);
        f.setMaximumSize(d);
        return f;
    }

    /** Single bordered control: amount field + Rs/% toggle, same height. */
    private JPanel discountControlGroup() {
        final int h = 28;
        JPanel group = new JPanel(new BorderLayout(0, 0));
        group.setOpaque(true);
        group.setBackground(UITheme.SURFACE);
        group.setBorder(BorderFactory.createLineBorder(UITheme.GRID_LINE));
        Dimension outer = new Dimension(124, h);
        group.setPreferredSize(outer);
        group.setMinimumSize(outer);
        group.setMaximumSize(outer);

        txt_discount = new JTextField();
        txt_discount.setFont(UITheme.FONT_BOLD.deriveFont(13f));
        txt_discount.setForeground(PageUI.INK);
        txt_discount.setBackground(UITheme.SURFACE);
        txt_discount.setHorizontalAlignment(SwingConstants.RIGHT);
        txt_discount.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 4));
        txt_discount.setOpaque(true);

        cb_discountMode = new JComboBox<String>(new String[]{"Rs", "%"});
        cb_discountMode.setFont(UITheme.FONT_BOLD.deriveFont(12f));
        cb_discountMode.setBackground(UITheme.SURFACE);
        cb_discountMode.setForeground(PageUI.INK);
        cb_discountMode.setFocusable(false);
        cb_discountMode.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, UITheme.GRID_LINE));
        Dimension modeSize = new Dimension(48, h - 2);
        cb_discountMode.setPreferredSize(modeSize);
        cb_discountMode.setMinimumSize(modeSize);
        cb_discountMode.setMaximumSize(modeSize);

        group.add(txt_discount, BorderLayout.CENTER);
        group.add(cb_discountMode, BorderLayout.EAST);
        return group;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        jPanel1 = new JPanel();
        jLabel1 = new JLabel();
        jLabel2 = new JLabel();
        jLabel3 = new JLabel();
        txt_namaKasir = new JTextField();
        txt_idKasir = new JTextField();
        jLabel4 = new JLabel();
        txt_cariBarang = new JTextField();
        btn_cari = new JButton();
        jScrollPane1 = new JScrollPane();
        tbl_barang = new JTable();
        jLabel5 = new JLabel();
        txt_kodeTransaksi = new JTextField();
        jLabel6 = new JLabel();
        txt_namaBarang = new JTextField();
        jLabel7 = new JLabel();
        txt_tanggalTransaksi = new JTextField();
        jLabel8 = new JLabel();
        txt_merek = new JTextField();
        txt_merek.setVisible(false);
        txt_kodeBarang = new JTextField();
        jLabel9 = new JLabel();
        txt_hargaJual = new JTextField();
        jLabel10 = new JLabel();
        jLabel11 = new JLabel();
        txt_kategori = new JTextField();
        txt_jumlahBarang = new JTextField();
        jLabel12 = new JLabel();
        btn_tambah = new JButton();
        jScrollPane2 = new JScrollPane();
        tbl_detailBarang = new JTable();
        jLabel13 = new JLabel();
        txt_subTotal = new JTextField();
        btn_batal = new JButton();
        jLabel14 = new JLabel();
        jLabel15 = new JLabel();
        jLabel16 = new JLabel();
        txt_totalPembayaran = new JTextField();
        txt_uangDiterima = new JTextField();
        txt_uangKembalian = new JTextField();
        btn_hapus = new JButton();
        btn_perbarui = new JButton();
        btn_simpan = new JButton();
        btn_segarkan = new JButton();

        setLayout(new BorderLayout());
        PageUI.paintPage(this);

        // Hidden compatibility fields / table (logic still uses them).
        tbl_barang.setVisible(false);
        jScrollPane1.setVisible(false);
        tbl_detailBarang.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"CODE", "PRODUCT", "QTY", "PRICE", "SUBTOTAL"}) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        });
        tbl_detailBarang.setVisible(false);
        jScrollPane2.setVisible(false);
        txt_kodeBarang.setVisible(false);
        txt_namaBarang.setVisible(false);
        txt_kategori.setVisible(false);
        txt_merek.setVisible(false);
        txt_hargaJual.setVisible(false);
        txt_jumlahBarang.setVisible(false);
        txt_subTotal.setVisible(false);
        txt_idKasir.setVisible(false);
        txt_namaKasir.setVisible(false);
        txt_kodeTransaksi.setVisible(false);
        txt_tanggalTransaksi.setVisible(false);
        txt_totalPembayaran.setVisible(false);
        txt_uangKembalian.setVisible(false);
        btn_batal.setVisible(false);
        btn_hapus.setVisible(false);
        btn_perbarui.setVisible(false);
        btn_segarkan.setVisible(false);
        btn_cari.setVisible(false);

        jPanel1.setBackground(UITheme.PAGE_BG);
        jPanel1.setLayout(new BorderLayout());

        // ---- Top chrome ----
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(UITheme.PAGE_BG);
        top.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, RULE));

        JPanel topLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        topLeft.setOpaque(false);
        topLeft.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JLabel lbRegister = new JLabel("  REGISTER 1  ");
        lbRegister.setOpaque(true);
        lbRegister.setBackground(UITheme.ACCENT);
        lbRegister.setForeground(Color.WHITE);
        lbRegister.setFont(UITheme.FONT_BOLD.deriveFont(11f));
        lbRegister.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 0));
        jLabel1.setText("Sell");
        jLabel1.setFont(UITheme.FONT_HEADING.deriveFont(22f));
        jLabel1.setForeground(PageUI.INK);
        jLabel1.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbHint = new JLabel("Scan a barcode to add — no mouse needed");
        lbHint.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        lbHint.setForeground(UITheme.TEXT_MUTED);
        lbHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleWrap.add(jLabel1);
        titleWrap.add(lbHint);

        topLeft.add(lbRegister);
        topLeft.add(titleWrap);

        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topRight.setOpaque(false);
        lb_cashierMeta = new JLabel("—");
        lb_ticketMeta = new JLabel("—");
        lb_timeMeta = new JLabel("—");
        jLabel3.setText("CASHIER");
        jLabel6.setText("TICKET");
        jLabel5.setText("TIME");
        topRight.add(metaCell("CASHIER", lb_cashierMeta));
        topRight.add(metaCell("TICKET", lb_ticketMeta));
        topRight.add(metaCell("TIME", lb_timeMeta));

        top.add(topLeft, BorderLayout.WEST);
        top.add(topRight, BorderLayout.EAST);
        jPanel1.add(top, BorderLayout.NORTH);

        // ---- Body: left register + right ticket ----
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(UITheme.PAGE_BG);

        // LEFT
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(UITheme.PAGE_BG);
        left.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 16));

        JPanel searchRow = new JPanel(new BorderLayout(10, 0));
        searchRow.setOpaque(false);
        searchRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        txt_cariBarang.setFont(UITheme.FONT_REGULAR.deriveFont(14f));
        txt_cariBarang.setBackground(SEARCH_BG);
        txt_cariBarang.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RULE),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        txt_cariBarang.putClientProperty("JTextField.placeholderText", "Scan barcode, or search by name");
        txt_cariBarang.addActionListener(e -> tryScanOrAdd());
        txt_cariBarang.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    tryScanOrAdd();
                }
            }
        });
        btn_tambah.setText("Add");
        PageUI.stylePrimaryButton(btn_tambah);
        btn_tambah.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
        btn_tambah.addActionListener(e -> tryScanOrAdd());
        searchRow.add(txt_cariBarang, BorderLayout.CENTER);
        searchRow.add(btn_tambah, BorderLayout.EAST);

        pn_categoryTabs = new JPanel();
        pn_categoryTabs.setOpaque(false);
        pn_categoryTabs.setLayout(new BoxLayout(pn_categoryTabs, BoxLayout.X_AXIS));
        pn_categoryTabs.setBorder(BorderFactory.createEmptyBorder(14, 0, 14, 0));

        JPanel quickHead = new JPanel(new BorderLayout());
        quickHead.setOpaque(false);
        quickHead.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JLabel lbQk = new JLabel("QUICK KEYS");
        lbQk.setFont(UITheme.FONT_CAPTION);
        lbQk.setForeground(UITheme.TEXT_CAPTION);
        JLabel lbQkHint = new JLabel("Tap to add one unit · long-press to set quantity");
        lbQkHint.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lbQkHint.setForeground(UITheme.TEXT_MUTED);
        quickHead.add(lbQk, BorderLayout.WEST);
        quickHead.add(lbQkHint, BorderLayout.EAST);

        pn_quickKeys = new JPanel(new GridLayout(2, 4, 0, 0));
        pn_quickKeys.setBackground(UITheme.SURFACE);
        pn_quickKeys.setBorder(BorderFactory.createLineBorder(UITheme.GRID_LINE));

        JLabel lbFooter = new JLabel("Quick keys are the eight products sold most this week. Anything else: scan it, or search above.");
        lbFooter.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lbFooter.setForeground(UITheme.TEXT_MUTED);
        lbFooter.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JPanel leftStack = new JPanel();
        leftStack.setOpaque(false);
        leftStack.setLayout(new BoxLayout(leftStack, BoxLayout.Y_AXIS));
        searchRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        pn_categoryTabs.setAlignmentX(Component.LEFT_ALIGNMENT);
        quickHead.setAlignmentX(Component.LEFT_ALIGNMENT);
        pn_quickKeys.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbFooter.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftStack.add(searchRow);
        leftStack.add(pn_categoryTabs);
        leftStack.add(quickHead);
        leftStack.add(pn_quickKeys);
        leftStack.add(lbFooter);
        left.add(leftStack, BorderLayout.NORTH);

        // RIGHT ticket — wide enough for large line totals
        JPanel ticket = new JPanel(new BorderLayout());
        ticket.setPreferredSize(new Dimension(440, 10));
        ticket.setMinimumSize(new Dimension(400, 10));
        ticket.setBackground(TICKET_BG);
        ticket.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, RULE));

        JPanel ticketHead = new JPanel(new BorderLayout());
        ticketHead.setOpaque(false);
        ticketHead.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, RULE),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JLabel lbTicket = new JLabel("TICKET");
        lbTicket.setFont(UITheme.FONT_CAPTION);
        lbTicket.setForeground(UITheme.TEXT_CAPTION);
        lb_cartStatus = new JLabel("0 lines · 0 units");
        lb_cartStatus.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        lb_cartStatus.setForeground(UITheme.TEXT_MUTED);
        ticketHead.add(lbTicket, BorderLayout.WEST);
        ticketHead.add(lb_cartStatus, BorderLayout.EAST);

        JPanel ticketMeta = new JPanel();
        ticketMeta.setOpaque(false);
        ticketMeta.setLayout(new BoxLayout(ticketMeta, BoxLayout.Y_AXIS));
        ticketMeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, RULE),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        JLabel lbCustCap = new JLabel("CUSTOMER");
        PageUI.styleCaption(lbCustCap);
        lbCustCap.setAlignmentX(Component.LEFT_ALIGNMENT);
        lb_ticketCustomer = new JLabel("Walk-in");
        lb_ticketCustomer.setFont(UITheme.FONT_BOLD.deriveFont(13f));
        lb_ticketCustomer.setForeground(PageUI.INK);
        lb_ticketCustomer.setAlignmentX(Component.LEFT_ALIGNMENT);

        txt_customer = new JTextField();
        PageUI.styleField(txt_customer);
        txt_customer.putClientProperty("JTextField.placeholderText", "Name or phone");
        txt_customer.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleCustomerSuggestions();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleCustomerSuggestions();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                scheduleCustomerSuggestions();
            }
        });
        txt_customer.addActionListener(e -> searchCustomer());
        txt_customer.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (customerSuggestPopup == null || !customerSuggestPopup.isVisible()
                        || customerSuggestList == null) {
                    return;
                }
                int idx = customerSuggestList.getSelectedIndex();
                int size = customerSuggestList.getModel().getSize();
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (size > 0) {
                        customerSuggestList.setSelectedIndex(Math.min(idx + 1, size - 1));
                    }
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (size > 0) {
                        customerSuggestList.setSelectedIndex(Math.max(idx - 1, 0));
                    }
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    hideCustomerSuggestions();
                    e.consume();
                }
            }
        });
        txt_customer.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // Delay so a click on a suggestion can register first.
                Timer t = new Timer(180, ev -> {
                    if (customerSuggestPopup != null && customerSuggestPopup.isShowing()
                            && customerSuggestList != null
                            && customerSuggestList.isFocusOwner()) {
                        return;
                    }
                    hideCustomerSuggestions();
                });
                t.setRepeats(false);
                t.start();
            }
        });
        btn_newCustomer = new JButton("+ New");
        PageUI.styleGhostButton(btn_newCustomer);
        btn_newCustomer.setForeground(UITheme.ACCENT);
        btn_newCustomer.addActionListener(e -> newCustomer());
        btn_clearCustomer = new JButton("Clear");
        PageUI.styleGhostButton(btn_clearCustomer);
        btn_clearCustomer.setForeground(PageUI.INK);
        btn_clearCustomer.addActionListener(e -> {
            suppressCustomerSuggest = true;
            txt_customer.setText("");
            suppressCustomerSuggest = false;
            clearCustomer();
        });
        JPanel custBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        custBtns.setOpaque(false);
        custBtns.add(btn_newCustomer);
        custBtns.add(btn_clearCustomer);
        JPanel custSearch = new JPanel(new BorderLayout(6, 0));
        custSearch.setOpaque(false);
        custSearch.setAlignmentX(Component.LEFT_ALIGNMENT);
        custSearch.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        custSearch.add(txt_customer, BorderLayout.CENTER);
        custSearch.add(custBtns, BorderLayout.EAST);

        JLabel lbPayCap = new JLabel("PAYMENT");
        PageUI.styleCaption(lbPayCap);
        lbPayCap.setAlignmentX(Component.LEFT_ALIGNMENT);
        cb_metode = new JComboBox<MetodeItem>();
        cb_metode.setAlignmentX(Component.LEFT_ALIGNMENT);
        cb_metode.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        cb_metode.addActionListener(e -> updateDeliveryManVisibility());

        lb_kurirCap = new JLabel("DELIVERY MAN");
        PageUI.styleCaption(lb_kurirCap);
        lb_kurirCap.setAlignmentX(Component.LEFT_ALIGNMENT);
        txt_kurir = new JTextField();
        PageUI.styleField(txt_kurir);
        txt_kurir.putClientProperty("JTextField.placeholderText", "Delivery man name");
        pn_kurirRow = new JPanel(new BorderLayout());
        pn_kurirRow.setOpaque(false);
        pn_kurirRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        pn_kurirRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        pn_kurirRow.add(txt_kurir, BorderLayout.CENTER);
        lb_kurirCap.setVisible(false);
        pn_kurirRow.setVisible(false);

        ticketMeta.add(lbCustCap);
        ticketMeta.add(Box.createVerticalStrut(2));
        ticketMeta.add(lb_ticketCustomer);
        ticketMeta.add(Box.createVerticalStrut(6));
        ticketMeta.add(custSearch);
        ticketMeta.add(Box.createVerticalStrut(10));
        ticketMeta.add(lbPayCap);
        ticketMeta.add(Box.createVerticalStrut(4));
        ticketMeta.add(cb_metode);
        ticketMeta.add(Box.createVerticalStrut(8));
        ticketMeta.add(lb_kurirCap);
        ticketMeta.add(Box.createVerticalStrut(4));
        ticketMeta.add(pn_kurirRow);

        JPanel ticketNorth = new JPanel(new BorderLayout());
        ticketNorth.setOpaque(false);
        ticketNorth.add(ticketHead, BorderLayout.NORTH);
        ticketNorth.add(ticketMeta, BorderLayout.SOUTH);

        pn_ticketLines = new JPanel();
        pn_ticketLines.setOpaque(false);
        pn_ticketLines.setLayout(new BoxLayout(pn_ticketLines, BoxLayout.Y_AXIS));
        JScrollPane ticketScroll = new JScrollPane(pn_ticketLines);
        ticketScroll.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        ticketScroll.getViewport().setBackground(TICKET_BG);
        ticketScroll.setOpaque(false);
        ticketScroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        ticketScroll.getVerticalScrollBar().setUnitIncrement(16);

        JLabel lbVoidHint = new JLabel("Double-click a line to void it.");
        lbVoidHint.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lbVoidHint.setForeground(UITheme.TEXT_MUTED);
        lbVoidHint.setBorder(BorderFactory.createEmptyBorder(4, 16, 8, 16));

        JPanel summary = new JPanel();
        summary.setOpaque(false);
        summary.setLayout(new BoxLayout(summary, BoxLayout.Y_AXIS));
        summary.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, RULE),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));

        lb_subtotalVal = new JLabel(money(0));
        lb_subtotalVal.setFont(UITheme.FONT_REGULAR.deriveFont(13f));
        lb_subtotalVal.setForeground(PageUI.INK);
        lb_discountVal = new JLabel(money(0));
        lb_discountVal.setFont(UITheme.FONT_REGULAR.deriveFont(13f));
        lb_discountVal.setForeground(PageUI.INK);
        lb_changeVal = new JLabel(money(0));
        lb_changeVal.setFont(UITheme.FONT_REGULAR.deriveFont(13f));
        lb_changeVal.setForeground(PageUI.INK);

        summary.add(moneyRow("Subtotal", null, lb_subtotalVal));

        JPanel discControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        discControls.setOpaque(false);
        discControls.add(discountControlGroup());
        txt_discount.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                enforceDiscountLimit();
                recalcChange();
                updateMoneyLabels();
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                char c = evt.getKeyChar();
                if (!(Character.isDigit(c)) && !(c == KeyEvent.VK_BACK_SPACE)) {
                    evt.consume();
                }
            }
        });
        cb_discountMode.addActionListener(e -> {
            enforceDiscountLimit();
            recalcChange();
            updateMoneyLabels();
        });
        summary.add(moneyRow("Discount", discControls, lb_discountVal));

        txt_uangDiterima = moneyField(124);
        txt_uangDiterima.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txt_uangDiterimaKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txt_uangDiterimaKeyTyped(evt);
            }
        });
        JPanel cashControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        cashControls.setOpaque(false);
        cashControls.add(txt_uangDiterima);
        pn_cashReceivedRow = moneyRow("Cash received", cashControls, null);
        pn_changeDueRow = moneyRow("Change due", null, lb_changeVal);
        summary.add(pn_cashReceivedRow);
        summary.add(pn_changeDueRow);

        JPanel checkout = new JPanel();
        checkout.setBackground(UITheme.ACCENT);
        checkout.setLayout(new BoxLayout(checkout, BoxLayout.Y_AXIS));
        checkout.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel lbDueCap = new JLabel("TOTAL DUE");
        lbDueCap.setFont(UITheme.FONT_CAPTION);
        lbDueCap.setForeground(Color.WHITE);
        lbDueCap.setAlignmentX(Component.LEFT_ALIGNMENT);
        lb_totalDue = new JLabel(money(0));
        lb_totalDue.setFont(UITheme.FONT_KPI_VALUE.deriveFont(34f));
        lb_totalDue.setForeground(Color.WHITE);
        lb_totalDue.setAlignmentX(Component.LEFT_ALIGNMENT);

        btn_simpan.setText("Charge & print receipt");
        btn_simpan.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn_simpan.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        PageUI.stylePrimaryButton(btn_simpan);
        btn_simpan.setBackground(Color.WHITE);
        btn_simpan.setForeground(UITheme.ACCENT);
        btn_simpan.setFont(UITheme.FONT_BOLD.deriveFont(13f));
        btn_simpan.addActionListener(evt -> btn_simpanActionPerformed(evt));

        JPanel secondary = new JPanel(new BorderLayout());
        secondary.setOpaque(false);
        secondary.setAlignmentX(Component.LEFT_ALIGNMENT);
        secondary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JButton btnVoid = outlineBtn("Void ticket");
        btnVoid.addActionListener(e -> btn_segarkanActionPerformed(null));
        secondary.add(btnVoid, BorderLayout.CENTER);

        checkout.add(lbDueCap);
        checkout.add(Box.createVerticalStrut(4));
        checkout.add(lb_totalDue);
        checkout.add(Box.createVerticalStrut(12));
        checkout.add(btn_simpan);
        checkout.add(Box.createVerticalStrut(8));
        checkout.add(secondary);

        JPanel ticketSouth = new JPanel(new BorderLayout());
        ticketSouth.setOpaque(false);
        ticketSouth.add(lbVoidHint, BorderLayout.NORTH);
        ticketSouth.add(summary, BorderLayout.CENTER);
        ticketSouth.add(checkout, BorderLayout.SOUTH);

        ticket.add(ticketNorth, BorderLayout.NORTH);
        ticket.add(ticketScroll, BorderLayout.CENTER);
        ticket.add(ticketSouth, BorderLayout.SOUTH);

        body.add(left, BorderLayout.CENTER);
        body.add(ticket, BorderLayout.EAST);
        jPanel1.add(body, BorderLayout.CENTER);

        // Keep unused labels referenced so NetBeans fields stay valid.
        jLabel2.setVisible(false);
        jLabel4.setVisible(false);
        jLabel7.setVisible(false);
        jLabel8.setVisible(false);
        jLabel9.setVisible(false);
        jLabel10.setVisible(false);
        jLabel11.setVisible(false);
        jLabel12.setVisible(false);
        jLabel13.setVisible(false);
        jLabel14.setVisible(false);
        jLabel15.setVisible(false);
        jLabel16.setVisible(false);

        add(jPanel1, BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private JButton outlineBtn(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setForeground(Color.WHITE);
        b.setFont(UITheme.FONT_BOLD.deriveFont(12f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return b;
    }

    private void txt_uangDiterimaKeyReleased(java.awt.event.KeyEvent evt) {
        recalcChange();
        updateMoneyLabels();
    }

    private void btn_simpanActionPerformed(java.awt.event.ActionEvent evt) {
        Connection conn = null;
        boolean oldAuto = true;
        try {
            if (tbl_detailBarang.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "Ticket is empty.");
                return;
            }
            MetodeItem metode = cb_metode == null ? null : (MetodeItem) cb_metode.getSelectedItem();
            if (metode == null) {
                JOptionPane.showMessageDialog(this, "Select a payment method.");
                return;
            }
            String namaKurir = "";
            if (isDeliveryPayment(metode)) {
                namaKurir = txt_kurir == null ? "" : txt_kurir.getText().trim();
                if (namaKurir.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Enter the delivery man's name for delivery sales.");
                    if (txt_kurir != null) {
                        txt_kurir.requestFocusInWindow();
                    }
                    return;
                }
            }
            loadMaxDiscount();
            int idTransaksi = Integer.valueOf(txt_kodeTransaksi.getText());
            String tanggal = txt_tanggalTransaksi.getText();
            int subtotalKotor = getSubtotalKotor();
            int diskon = appliedDiscount();
            int cap = discountCapAmount(subtotalKotor);
            if (diskon > cap) {
                JOptionPane.showMessageDialog(this,
                        "Discount cannot exceed " + maxDiscountPercent + "% of the subtotal.");
                return;
            }
            int totalPembayaran = getNetTotal();
            int uangDiterima;
            int uangKembalian;
            if (isCashPayment(metode)) {
                uangDiterima = Integer.valueOf(nullToZero(txt_uangDiterima.getText()));
                if (uangDiterima < totalPembayaran) {
                    JOptionPane.showMessageDialog(this, "Cash received must cover total due.");
                    txt_uangDiterima.requestFocusInWindow();
                    return;
                }
                uangKembalian = Integer.valueOf(nullToZero(txt_uangKembalian.getText()));
            } else {
                // Non-cash: treat as paid in full — no tender / change.
                uangDiterima = totalPembayaran;
                uangKembalian = 0;
            }
            int idUser = Integer.valueOf(txt_idKasir.getText());
            String saleUuid = Ids.newUuid();
            String pelangganUuid = null;
            if (selectedPelangganId != null) {
                pelangganUuid = lookupPelangganUuid(selectedPelangganId.intValue());
            }
            StringBuilder linesJson = new StringBuilder();
            linesJson.append('[');

            conn = Koneksi.getConnection();
            oldAuto = conn.getAutoCommit();
            conn.setAutoCommit(false);

            PreparedStatement headerPs = conn.prepareStatement(
                    // waktu_penjualan is NOW() rather than a parameter: tanggal_penjualan
                    // is DATE-only, so this is what carries the clock time for reporting.
                    "INSERT INTO `penjualan`(`penjualan_Id`, `tanggal_penjualan`, `total_Pembayaran`, "
                    + "`uang_diterima`, `uang_kembalian`, `user_Id`, `uuid`, "
                    + "`pelanggan_Id`, `metode_Id`, `nama_kurir`, `subtotal_kotor`, `diskon`, "
                    + "`waktu_penjualan`) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,NOW())");
            try {
                headerPs.setInt(1, idTransaksi);
                headerPs.setString(2, tanggal);
                headerPs.setInt(3, totalPembayaran);
                headerPs.setInt(4, uangDiterima);
                headerPs.setInt(5, uangKembalian);
                headerPs.setInt(6, idUser);
                headerPs.setString(7, saleUuid);
                if (selectedPelangganId == null) {
                    headerPs.setNull(8, Types.INTEGER);
                } else {
                    headerPs.setInt(8, selectedPelangganId.intValue());
                }
                headerPs.setInt(9, metode.id);
                if (namaKurir.isEmpty()) {
                    headerPs.setNull(10, Types.VARCHAR);
                } else {
                    headerPs.setString(10, namaKurir);
                }
                headerPs.setInt(11, subtotalKotor);
                headerPs.setInt(12, diskon);
                headerPs.executeUpdate();
            } finally {
                headerPs.close();
            }

            int jumlahBaris = tbl_detailBarang.getRowCount();
            for (int i = 0; i < jumlahBaris; i++) {
                String lineUuid = Ids.newUuid();
                String kodeProduk = tbl_detailBarang.getValueAt(i, 0).toString();
                BigDecimal jumlah = QuantityUtil.parse(tbl_detailBarang.getValueAt(i, 2).toString());
                int lineSubtotal = Integer.parseInt(tbl_detailBarang.getValueAt(i, 4).toString());
                PreparedStatement linePs = conn.prepareStatement(
                        "INSERT INTO `detail_penjualan`(`penjualan_Id`, `kode_produk`, `jumlah`, `Subtotal`, `uuid`) "
                        + "VALUES (?,?,?,?,?)");
                try {
                    linePs.setInt(1, idTransaksi);
                    linePs.setString(2, kodeProduk);
                    linePs.setBigDecimal(3, jumlah);
                    linePs.setInt(4, lineSubtotal);
                    linePs.setString(5, lineUuid);
                    linePs.executeUpdate();
                } finally {
                    linePs.close();
                }
                if (i > 0) {
                    linesJson.append(',');
                }
                String jumlahStr = jumlah.setScale(3, RoundingMode.HALF_UP).toPlainString();
                linesJson.append('{')
                        .append("\"uuid\":").append(SyncService.jsonString(lineUuid)).append(',')
                        .append("\"kodeProduk\":").append(Long.parseLong(kodeProduk.trim())).append(',')
                        .append("\"jumlah\":").append(SyncService.jsonString(jumlahStr)).append(',')
                        .append("\"subtotal\":").append(lineSubtotal)
                        .append('}');
            }
            linesJson.append(']');
            conn.commit();

            // Outbox after commit — never rolls back a completed sale
            try {
                StringBuilder payload = new StringBuilder(512);
                payload.append('{')
                        .append("\"uuid\":").append(SyncService.jsonString(saleUuid)).append(',')
                        .append("\"tanggalPenjualan\":").append(SyncService.jsonString(tanggal)).append(',')
                        .append("\"subtotalKotor\":").append(subtotalKotor).append(',')
                        .append("\"diskon\":").append(diskon).append(',')
                        .append("\"totalPembayaran\":").append(totalPembayaran).append(',')
                        .append("\"uangDiterima\":").append(uangDiterima).append(',')
                        .append("\"uangKembalian\":").append(uangKembalian).append(',')
                        .append("\"userId\":").append(idUser).append(',')
                        .append("\"pelangganUuid\":")
                        .append(pelangganUuid == null ? "null" : SyncService.jsonString(pelangganUuid))
                        .append(',')
                        .append("\"metodeId\":").append(metode.id).append(',')
                        .append("\"namaKurir\":")
                        .append(namaKurir.isEmpty() ? "null" : SyncService.jsonString(namaKurir))
                        .append(',')
                        .append("\"voided\":0,")
                        .append("\"lines\":").append(linesJson)
                        .append('}');
                SyncService.getInstance().enqueue("sale", saleUuid, payload.toString());
                // Drop old stuck rows that still carry the pre-fix payload shape
                try {
                    PreparedStatement clearStuck = Koneksi.getConnection().prepareStatement(
                            "DELETE FROM sync_outbox WHERE attempts >= 3");
                    try {
                        clearStuck.executeUpdate();
                    } finally {
                        clearStuck.close();
                    }
                } catch (Exception clearEx) {
                    System.out.println("sync outbox stuck cleanup failed: " + clearEx);
                }
            } catch (Exception outboxEx) {
                System.out.println("sync outbox write failed: " + outboxEx);
            }

            try {
                ReceiptPrinter.printReceipt(idTransaksi);
            } catch (Exception e) {
                System.out.println("Unable to show receipt: " + e);
            }
            resetTicket();
            JOptionPane.showMessageDialog(this, "Sale completed.");

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Exception ignored) {
                }
            }
            JOptionPane.showMessageDialog(null, "Please check again: " + e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(oldAuto);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void resetTicket() {
        txt_idKasir.setText(user.getId());
        txt_namaKasir.setText(user.getNama());
        tampilBarang();
        id();
        bersihInput();
        txt_totalPembayaran.setText("0");
        txt_uangDiterima.setText("");
        txt_uangKembalian.setText("0");
        if (txt_discount != null) {
            txt_discount.setText("");
        }
        if (cb_discountMode != null) {
            cb_discountMode.setSelectedIndex(0);
        }
        if (txt_customer != null) {
            txt_customer.setText("");
        }
        if (txt_kurir != null) {
            txt_kurir.setText("");
        }
        clearCustomer();
        loadPaymentMethods();
        updateDeliveryManVisibility();
        DefaultTableModel model = (DefaultTableModel) tbl_detailBarang.getModel();
        model.setRowCount(0);
        loadQuickKeys();
        totalPembayaran();
        refreshMeta();
    }

    private void btn_segarkanActionPerformed(java.awt.event.ActionEvent evt) {
        resetTicket();
    }

    private void txt_uangDiterimaKeyTyped(java.awt.event.KeyEvent evt) {
        char c = evt.getKeyChar();
        if (!(Character.isDigit(c)) && !(c == KeyEvent.VK_BACK_SPACE)) {
            JOptionPane.showMessageDialog(null, "Numbers only", "Invalid input", JOptionPane.ERROR_MESSAGE);
            evt.consume();
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_batal;
    private javax.swing.JButton btn_cari;
    private javax.swing.JButton btn_hapus;
    private javax.swing.JButton btn_perbarui;
    private javax.swing.JButton btn_segarkan;
    private javax.swing.JButton btn_simpan;
    private javax.swing.JButton btn_tambah;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable tbl_barang;
    private javax.swing.JTable tbl_detailBarang;
    private javax.swing.JTextField txt_cariBarang;
    private javax.swing.JTextField txt_hargaJual;
    private javax.swing.JTextField txt_idKasir;
    private javax.swing.JTextField txt_jumlahBarang;
    private javax.swing.JTextField txt_kategori;
    private javax.swing.JTextField txt_kodeBarang;
    private javax.swing.JTextField txt_kodeTransaksi;
    private javax.swing.JTextField txt_merek;
    private javax.swing.JTextField txt_namaBarang;
    private javax.swing.JTextField txt_namaKasir;
    private javax.swing.JTextField txt_subTotal;
    private javax.swing.JTextField txt_tanggalTransaksi;
    private javax.swing.JTextField txt_totalPembayaran;
    private javax.swing.JTextField txt_uangDiterima;
    private javax.swing.JTextField txt_uangKembalian;
    // End of variables declaration//GEN-END:variables
}
