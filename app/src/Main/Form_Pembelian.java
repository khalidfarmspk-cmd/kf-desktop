/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Main;

import config.Ids;
import config.Koneksi;
import config.SyncOutbox;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/**
 * Restock — goods-in layout (scan lines + delivery sidebar).
 */
public class Form_Pembelian extends javax.swing.JPanel {

    PreparedStatement ps;
    ResultSet rs;

    private JLabel lb_cartStatus;
    private JLabel lb_purchaseTotal;
    private JLabel lb_officerMeta;
    private JLabel lb_poMeta;
    private JLabel lb_receivedMeta;
    private JLabel lb_selected;
    private JLabel lb_tableFooter;
    private JLabel lb_unitsReceived;
    private JLabel lb_stockValue;
    private JLabel lb_selectedLineTitle;
    private JComboBox<String> cb_supplier;
    private JTextField txt_invoice;
    private JTextField txt_deliveryDate;
    private final List<ProductRow> catalog = new ArrayList<>();

    private static final Color PANEL_BG = new Color(0xF3F3F1);
    private static final Color RULE = new Color(0xD0D0CC);
    private static final Color SEARCH_BG = new Color(0xEFEFEA);

    private static class ProductRow {
        final String code;
        final String name;
        final String category;
        final BigDecimal stock;
        final int buy;

        ProductRow(String code, String name, String category, BigDecimal stock, int buy) {
            this.code = code;
            this.name = name;
            this.category = category;
            this.stock = stock != null ? stock : BigDecimal.ZERO;
            this.buy = buy;
        }
    }

    public Form_Pembelian() {
        initComponents();

        txt_idKasir.setText(user.getId());
        txt_namaKasir.setText(user.getNama());
        tampilBarang();
        loadSuppliers();
        setEditableFalse();
        id();
        date();
        refreshMeta();
        updateCartStatus();
        updatePurchaseTotal();
        clearSelectedLine();
    }

    void tampilBarang() {
        catalog.clear();
        try {
            String tampilBarang = "SELECT produk.kode_produk, produk.nama_produk, kategori.nama_kategori, "
                    + "produk.stok_produk, produk.harga_beli "
                    + "FROM produk JOIN kategori ON produk.kategori_Id = kategori.kategori_Id "
                    + "ORDER BY produk.nama_produk;";
            ps = Koneksi.getConnection().prepareStatement(tampilBarang);
            rs = ps.executeQuery();

            DefaultTableModel barang = new DefaultTableModel(
                    new Object[]{"CODE", "PRODUCT", "CATEGORY", "STOCK", "BUY"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            while (rs.next()) {
                ProductRow p = new ProductRow(
                        rs.getString("produk.kode_produk"),
                        rs.getString("produk.nama_produk"),
                        rs.getString("kategori.nama_kategori"),
                        rs.getBigDecimal("produk.stok_produk"),
                        rs.getInt("produk.harga_beli"));
                catalog.add(p);
                barang.addRow(new Object[]{p.code, p.name, p.category, p.stock.toPlainString(), p.buy});
            }
            tbl_barang.setModel(barang);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please check again: " + e);
        }
    }

    void cariBarang() {
        String q = txt_cariBarang.getText().trim();
        DefaultTableModel Barang = new DefaultTableModel(
                new Object[]{"CODE", "PRODUCT", "CATEGORY", "STOCK", "BUY"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        catalog.clear();
        try {
            String cari = "SELECT produk.kode_produk, produk.nama_produk, kategori.nama_kategori, "
                    + "produk.stok_produk, produk.harga_beli "
                    + "FROM produk JOIN kategori ON produk.kategori_Id = kategori.kategori_Id "
                    + "WHERE produk.kode_produk LIKE '%" + q + "%' || produk.nama_produk LIKE '%"
                    + q + "%' || kategori.nama_kategori LIKE '%" + q + "%'";
            ps = Koneksi.getConnection().prepareStatement(cari);
            rs = ps.executeQuery();
            while (rs.next()) {
                ProductRow p = new ProductRow(
                        rs.getString("produk.kode_produk"),
                        rs.getString("produk.nama_produk"),
                        rs.getString("kategori.nama_kategori"),
                        rs.getBigDecimal("produk.stok_produk"),
                        rs.getInt("produk.harga_beli"));
                catalog.add(p);
                Barang.addRow(new Object[]{p.code, p.name, p.category, p.stock.toPlainString(), p.buy});
            }
            tbl_barang.setModel(Barang);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please check again: " + e);
        }
    }

    private void loadSuppliers() {
        try {
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("Select supplier");
            ps = Koneksi.getConnection().prepareStatement(
                    "SELECT nama_supplier FROM supplier ORDER BY nama_supplier");
            rs = ps.executeQuery();
            while (rs.next()) {
                model.addElement(rs.getString(1));
            }
            cb_supplier.setModel(model);
        } catch (Exception e) {
            cb_supplier.setModel(new DefaultComboBoxModel<>(new String[]{"Select supplier"}));
        }
    }

    private ProductRow findProduct(String code) {
        for (ProductRow p : catalog) {
            if (p.code.equals(code)) {
                return p;
            }
        }
        return null;
    }

    void setEditableFalse() {
        txt_kodeBarang.setEditable(false);
        txt_kategori.setEditable(false);
        txt_namaBarang.setEditable(false);
        txt_namaKasir.setEditable(false);
        txt_idKasir.setEditable(false);
        txt_kodeTransaksi.setEditable(false);
        txt_tanggalTransaksi.setEditable(false);
    }

    void id() {
        try {
            String idPenjualan = "SELECT MAX(pembelian.pembelian_Id) FROM pembelian;";
            ps = Koneksi.getConnection().prepareStatement(idPenjualan);
            rs = ps.executeQuery();
            if (rs.next()) {
                int idJual = rs.getInt(1);
                txt_kodeTransaksi.setText(Integer.toString(idJual + 1));
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
        if (txt_deliveryDate != null) {
            txt_deliveryDate.setText(new SimpleDateFormat("dd/MM/yyyy").format(tanggal));
        }
        refreshMeta();
    }

    void bersihInput() {
        txt_kategori.setText("");
        txt_kodeBarang.setText("");
        txt_namaBarang.setText("");
        txt_jumlahBarang.setText("");
        txt_hargaJual.setText("");
    }

    private void refreshMeta() {
        if (lb_officerMeta != null) {
            lb_officerMeta.setText(txt_namaKasir.getText());
        }
        if (lb_poMeta != null) {
            try {
                int n = Integer.parseInt(txt_kodeTransaksi.getText().trim());
                lb_poMeta.setText("PO-" + String.format("%04d", n));
            } catch (Exception e) {
                lb_poMeta.setText("PO-" + txt_kodeTransaksi.getText());
            }
        }
        if (lb_receivedMeta != null) {
            lb_receivedMeta.setText(new SimpleDateFormat("d MMM · h:mm a").format(new Date()));
        }
    }

    private String money(int amount) {
        return UITheme.CURRENCY + " " + NumberFormat.getIntegerInstance(Locale.US).format(amount);
    }

    private String num(int amount) {
        return NumberFormat.getIntegerInstance(Locale.US).format(amount);
    }

    private void updateCartStatus() {
        int lines = tbl_detailBarang.getRowCount();
        BigDecimal units = BigDecimal.ZERO;
        int total = 0;
        for (int i = 0; i < lines; i++) {
            try {
                units = units.add(QuantityUtil.parse(tbl_detailBarang.getValueAt(i, 2).toString()));
                total += Integer.parseInt(tbl_detailBarang.getValueAt(i, 4).toString());
            } catch (Exception ignored) {
            }
        }
        String unitsText = QuantityUtil.format(units, true);
        if (lb_cartStatus != null) {
            lb_cartStatus.setText("Lines: " + lines + " · Units: " + unitsText);
        }
        if (lb_tableFooter != null) {
            lb_tableFooter.setText(lines + " lines received          "
                    + unitsText + "          " + num(total));
        }
        if (lb_unitsReceived != null) {
            lb_unitsReceived.setText(unitsText);
        }
        if (lb_stockValue != null) {
            lb_stockValue.setText(money(total));
        }
    }

    private void updatePurchaseTotal() {
        int total = 0;
        int rows = tbl_detailBarang.getRowCount();
        for (int i = 0; i < rows; i++) {
            try {
                total += Integer.parseInt(tbl_detailBarang.getValueAt(i, 4).toString());
            } catch (Exception ignored) {
            }
        }
        if (lb_purchaseTotal != null) {
            lb_purchaseTotal.setText(money(total));
        }
        updateCartStatus();
    }

    private void tryAddFromScan() {
        String q = txt_cariBarang.getText().trim();
        if (q.isEmpty()) {
            return;
        }
        for (ProductRow p : catalog) {
            if (p.code.equalsIgnoreCase(q)) {
                selectProduct(p);
                txt_jumlahBarang.setText("1");
                txt_hargaJual.setText(Integer.toString(p.buy));
                addCurrentLine();
                txt_cariBarang.setText("");
                return;
            }
        }
        cariBarang();
        if (catalog.size() == 1) {
            ProductRow p = catalog.get(0);
            selectProduct(p);
            txt_jumlahBarang.setText("1");
            txt_hargaJual.setText(Integer.toString(p.buy));
            addCurrentLine();
            txt_cariBarang.setText("");
            tampilBarang();
        } else if (catalog.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No product found.");
            tampilBarang();
        }
    }

    private void selectProduct(ProductRow p) {
        txt_kodeBarang.setText(p.code);
        txt_namaBarang.setText(p.name);
        txt_kategori.setText(p.category);
        txt_hargaJual.setText(Integer.toString(p.buy));
        if (lb_selectedLineTitle != null) {
            lb_selectedLineTitle.setText("SELECTED LINE — " + p.name.toUpperCase(Locale.ROOT));
        }
        if (lb_selected != null) {
            lb_selected.setText("Selected: " + p.name);
        }
    }

    private void addCurrentLine() {
        try {
            String code = txt_kodeBarang.getText().trim();
            String name = txt_namaBarang.getText().trim();
            BigDecimal qty = QuantityUtil.parse(txt_jumlahBarang.getText());
            int buy = Integer.parseInt(txt_hargaJual.getText().trim().replace(",", ""));
            if (code.isEmpty() || name.isEmpty() || qty.compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(this, "Select a product and enter quantity.");
                return;
            }
            ProductRow p = findProduct(code);
            BigDecimal stock = p != null ? p.stock : BigDecimal.ZERO;
            for (int i = 0; i < tbl_detailBarang.getRowCount(); i++) {
                if (code.equals(tbl_detailBarang.getValueAt(i, 0).toString())) {
                    BigDecimal existing = QuantityUtil.parse(tbl_detailBarang.getValueAt(i, 2).toString());
                    BigDecimal next = existing.add(qty);
                    int subtotal = QuantityUtil.moneySubtotal(next, buy);
                    tbl_detailBarang.setValueAt(next.toPlainString(), i, 2);
                    tbl_detailBarang.setValueAt(buy, i, 3);
                    tbl_detailBarang.setValueAt(subtotal, i, 4);
                    tbl_detailBarang.setValueAt(stock.add(next).toPlainString(), i, 5);
                    tbl_detailBarang.setRowSelectionInterval(i, i);
                    loadSelectedLine(i);
                    updatePurchaseTotal();
                    bersihInput();
                    return;
                }
            }
            DefaultTableModel model = (DefaultTableModel) tbl_detailBarang.getModel();
            model.addRow(new Object[]{
                code, name, qty.toPlainString(), buy,
                QuantityUtil.moneySubtotal(qty, buy),
                stock.add(qty).toPlainString()
            });
            int last = model.getRowCount() - 1;
            tbl_detailBarang.setRowSelectionInterval(last, last);
            loadSelectedLine(last);
            bersihInput();
            updatePurchaseTotal();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Check quantity and buy price.");
        }
    }

    private void loadSelectedLine(int row) {
        if (row < 0 || row >= tbl_detailBarang.getRowCount()) {
            clearSelectedLine();
            return;
        }
        String code = tbl_detailBarang.getValueAt(row, 0).toString();
        String name = tbl_detailBarang.getValueAt(row, 1).toString();
        txt_kodeBarang.setText(code);
        txt_namaBarang.setText(name);
        txt_jumlahBarang.setText(tbl_detailBarang.getValueAt(row, 2).toString());
        txt_hargaJual.setText(tbl_detailBarang.getValueAt(row, 3).toString());
        if (lb_selectedLineTitle != null) {
            lb_selectedLineTitle.setText("SELECTED LINE — " + name.toUpperCase(Locale.ROOT));
        }
        if (lb_selected != null) {
            lb_selected.setText("Selected: " + name);
        }
    }

    private void clearSelectedLine() {
        if (lb_selectedLineTitle != null) {
            lb_selectedLineTitle.setText("SELECTED LINE — —");
        }
        if (lb_selected != null) {
            lb_selected.setText("Selected: —");
        }
        txt_jumlahBarang.setText("");
        txt_hargaJual.setText("");
    }

    private JPanel metaCell(String caption, JLabel value) {
        JPanel cell = new JPanel();
        cell.setBackground(PANEL_BG);
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

    private JPanel fieldBlock(String caption, Component field) {
        JPanel wrap = new JPanel(new BorderLayout(0, 4));
        wrap.setOpaque(false);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        JLabel lb = new JLabel(caption);
        PageUI.styleCaption(lb);
        wrap.add(lb, BorderLayout.NORTH);
        wrap.add(field, BorderLayout.CENTER);
        return wrap;
    }

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

    private void styleCombo(JComboBox<String> cb) {
        cb.setFont(UITheme.FONT_REGULAR.deriveFont(13f));
        cb.setBackground(UITheme.SURFACE);
        cb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.GRID_LINE),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        jPanel1 = new JPanel();
        jLabel1 = new JLabel();
        jLabel2 = new JLabel();
        jLabel3 = new JLabel();
        jLabel4 = new JLabel();
        jLabel5 = new JLabel();
        jLabel6 = new JLabel();
        jLabel7 = new JLabel();
        jLabel8 = new JLabel();
        jLabel9 = new JLabel();
        jLabel10 = new JLabel();
        jLabel11 = new JLabel();
        jLabel12 = new JLabel();
        jLabel13 = new JLabel();
        jScrollPane1 = new JScrollPane();
        jScrollPane2 = new JScrollPane();
        tbl_barang = new JTable();
        tbl_detailBarang = new JTable();
        btn_hapus = new JButton();
        btn_simpan = new JButton();
        btn_segarkan = new JButton();
        txt_idKasir = new JTextField();
        txt_namaKasir = new JTextField();
        txt_kodeTransaksi = new JTextField();
        txt_tanggalTransaksi = new JTextField();
        btn_cari = new JButton();
        txt_cariBarang = new JTextField();
        txt_namaBarang = new JTextField();
        txt_kodeBarang = new JTextField();
        txt_hargaJual = new JTextField();
        txt_merek = new JTextField();
        txt_merek.setVisible(false);
        txt_kategori = new JTextField();
        txt_jumlahBarang = new JTextField();
        btn_batal = new JButton();
        btn_tambah = new JButton();
        btn_perbarui = new JButton();

        setLayout(new BorderLayout());
        PageUI.paintPage(this);

        // Hidden compatibility fields
        tbl_barang.setVisible(false);
        jScrollPane1.setVisible(false);
        txt_kodeBarang.setVisible(false);
        txt_namaBarang.setVisible(false);
        txt_kategori.setVisible(false);
        txt_merek.setVisible(false);
        txt_idKasir.setVisible(false);
        txt_namaKasir.setVisible(false);
        txt_kodeTransaksi.setVisible(false);
        txt_tanggalTransaksi.setVisible(false);
        btn_cari.setVisible(false);
        btn_batal.setVisible(false);
        btn_segarkan.setVisible(false);
        jLabel2.setVisible(false);
        jLabel3.setVisible(false);
        jLabel4.setVisible(false);
        jLabel5.setVisible(false);
        jLabel6.setVisible(false);
        jLabel7.setVisible(false);
        jLabel8.setVisible(false);
        jLabel9.setVisible(false);
        jLabel10.setVisible(false);
        jLabel11.setVisible(false);
        jLabel12.setVisible(false);
        jLabel13.setVisible(false);

        jPanel1.setBackground(UITheme.PAGE_BG);
        jPanel1.setLayout(new BorderLayout());

        // ---- Header ----
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(UITheme.PAGE_BG);
        top.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, RULE));

        JPanel topLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        topLeft.setOpaque(false);

        JLabel badge = new JLabel("  GOODS IN  ");
        badge.setOpaque(true);
        badge.setBackground(PageUI.INK);
        badge.setForeground(Color.WHITE);
        badge.setFont(UITheme.FONT_BOLD.deriveFont(11f));
        badge.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 0));
        jLabel1.setText("Restock");
        jLabel1.setFont(UITheme.FONT_HEADING.deriveFont(22f));
        jLabel1.setForeground(PageUI.INK);
        jLabel1.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbHint = new JLabel("Scan each carton as it comes off the van");
        lbHint.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        lbHint.setForeground(UITheme.TEXT_MUTED);
        lbHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleWrap.add(jLabel1);
        titleWrap.add(lbHint);
        topLeft.add(badge);
        topLeft.add(titleWrap);

        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topRight.setOpaque(false);
        lb_officerMeta = new JLabel("—");
        lb_poMeta = new JLabel("—");
        lb_receivedMeta = new JLabel("—");
        topRight.add(metaCell("OFFICER", lb_officerMeta));
        topRight.add(metaCell("PURCHASE", lb_poMeta));
        topRight.add(metaCell("RECEIVED", lb_receivedMeta));

        top.add(topLeft, BorderLayout.WEST);
        top.add(topRight, BorderLayout.EAST);
        jPanel1.add(top, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(UITheme.PAGE_BG);

        // LEFT
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(UITheme.PAGE_BG);
        left.setBorder(BorderFactory.createEmptyBorder(14, 16, 12, 12));

        JPanel searchRow = new JPanel(new BorderLayout(10, 0));
        searchRow.setOpaque(false);
        txt_cariBarang.setFont(UITheme.FONT_REGULAR.deriveFont(14f));
        txt_cariBarang.setBackground(SEARCH_BG);
        txt_cariBarang.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RULE),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        txt_cariBarang.putClientProperty("JTextField.placeholderText", "Scan barcode or search product to receive");
        txt_cariBarang.addActionListener(e -> tryAddFromScan());
        btn_tambah.setText("Add line");
        PageUI.stylePrimaryButton(btn_tambah);
        btn_tambah.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        btn_tambah.addActionListener(e -> {
            if (txt_kodeBarang.getText().trim().isEmpty()) {
                tryAddFromScan();
            } else {
                if (txt_jumlahBarang.getText().trim().isEmpty()) {
                    txt_jumlahBarang.setText("1");
                }
                addCurrentLine();
            }
        });
        searchRow.add(txt_cariBarang, BorderLayout.CENTER);
        searchRow.add(btn_tambah, BorderLayout.EAST);

        tbl_detailBarang.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"CODE", "PRODUCT", "QTY", "BUY", "LINE COST", "STOCK AFTER"}) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        });
        tbl_detailBarang.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tbl_detailBarangMouseClicked(evt);
            }
        });
        PageUI.styleTable(tbl_detailBarang);
        jScrollPane2.setViewportView(tbl_detailBarang);
        PageUI.styleScroll(jScrollPane2);

        lb_tableFooter = new JLabel("0 lines received          0          0");
        lb_tableFooter.setFont(UITheme.FONT_BOLD.deriveFont(12f));
        lb_tableFooter.setForeground(PageUI.INK);
        lb_tableFooter.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.GRID_LINE),
                BorderFactory.createEmptyBorder(10, 8, 10, 8)));

        JLabel lbNote = new JLabel("Lines post to stock only when the purchase is saved — check quantities against the delivery note first.");
        lbNote.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lbNote.setForeground(UITheme.TEXT_MUTED);
        lbNote.setBorder(BorderFactory.createEmptyBorder(10, 0, 8, 0));

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(PANEL_BG);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, RULE),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        lb_cartStatus = new JLabel("Lines: 0 · Units: 0");
        lb_cartStatus.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lb_cartStatus.setForeground(UITheme.TEXT_MUTED);
        lb_selected = new JLabel("Selected: —");
        lb_selected.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lb_selected.setForeground(UITheme.TEXT_MUTED);
        footer.add(lb_cartStatus, BorderLayout.WEST);
        footer.add(lb_selected, BorderLayout.EAST);

        JPanel leftCenter = new JPanel(new BorderLayout());
        leftCenter.setOpaque(false);
        leftCenter.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        leftCenter.add(jScrollPane2, BorderLayout.CENTER);
        JPanel underTable = new JPanel(new BorderLayout());
        underTable.setOpaque(false);
        underTable.add(lb_tableFooter, BorderLayout.NORTH);
        underTable.add(lbNote, BorderLayout.SOUTH);
        leftCenter.add(underTable, BorderLayout.SOUTH);

        left.add(searchRow, BorderLayout.NORTH);
        left.add(leftCenter, BorderLayout.CENTER);
        left.add(footer, BorderLayout.SOUTH);

        // RIGHT sidebar
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(PANEL_BG);
        sidebar.setPreferredSize(new Dimension(320, 10));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, RULE));

        JPanel sideHead = new JPanel(new BorderLayout());
        sideHead.setOpaque(false);
        sideHead.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, RULE),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JLabel lbDelivery = new JLabel("DELIVERY");
        lbDelivery.setFont(UITheme.FONT_CAPTION);
        lbDelivery.setForeground(UITheme.TEXT_CAPTION);
        JButton btnNewPo = new JButton("+ New PO");
        btnNewPo.setFocusPainted(false);
        btnNewPo.setBorderPainted(false);
        btnNewPo.setContentAreaFilled(false);
        btnNewPo.setForeground(UITheme.ACCENT);
        btnNewPo.setFont(UITheme.FONT_BOLD.deriveFont(12f));
        btnNewPo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnNewPo.addActionListener(e -> btn_segarkanActionPerformed(null));
        sideHead.add(lbDelivery, BorderLayout.WEST);
        sideHead.add(btnNewPo, BorderLayout.EAST);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        cb_supplier = new JComboBox<>();
        styleCombo(cb_supplier);
        txt_invoice = new JTextField();
        txt_deliveryDate = new JTextField();
        PageUI.styleField(txt_invoice);
        PageUI.styleField(txt_deliveryDate);
        PageUI.styleField(txt_jumlahBarang);
        PageUI.styleField(txt_hargaJual);
        txt_invoice.putClientProperty("JTextField.placeholderText", "INV-…");
        txt_jumlahBarang.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(KeyEvent evt) {
                txt_jumlahBarangKeyTyped(evt);
            }
        });
        txt_hargaJual.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(KeyEvent evt) {
                char c = evt.getKeyChar();
                if (!(Character.isDigit(c)) && !(c == KeyEvent.VK_BACK_SPACE)) {
                    evt.consume();
                }
            }
        });

        form.add(fieldBlock("SUPPLIER", cb_supplier));
        form.add(Box.createVerticalStrut(10));
        JPanel invDate = new JPanel(new GridLayout(1, 2, 10, 0));
        invDate.setOpaque(false);
        invDate.setAlignmentX(Component.LEFT_ALIGNMENT);
        invDate.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        invDate.add(fieldBlock("INVOICE NO.", txt_invoice));
        invDate.add(fieldBlock("DELIVERY DATE", txt_deliveryDate));
        form.add(invDate);
        form.add(Box.createVerticalStrut(16));

        lb_selectedLineTitle = new JLabel("SELECTED LINE — —");
        lb_selectedLineTitle.setFont(UITheme.FONT_CAPTION);
        lb_selectedLineTitle.setForeground(UITheme.TEXT_CAPTION);
        lb_selectedLineTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lb_selectedLineTitle);
        form.add(Box.createVerticalStrut(8));

        JPanel qtyBuy = new JPanel(new GridLayout(1, 2, 10, 0));
        qtyBuy.setOpaque(false);
        qtyBuy.setAlignmentX(Component.LEFT_ALIGNMENT);
        qtyBuy.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        qtyBuy.add(fieldBlock("QUANTITY", txt_jumlahBarang));
        qtyBuy.add(fieldBlock("BUY PRICE", txt_hargaJual));
        form.add(qtyBuy);
        form.add(Box.createVerticalStrut(10));

        JPanel lineActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        lineActions.setOpaque(false);
        lineActions.setAlignmentX(Component.LEFT_ALIGNMENT);
        lineActions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn_perbarui.setText("Update line");
        PageUI.styleGhostButton(btn_perbarui);
        btn_perbarui.setForeground(PageUI.INK);
        btn_perbarui.addActionListener(e -> btn_perbaruiActionPerformed(e));
        btn_hapus.setText("Remove line");
        btn_hapus.setFocusPainted(false);
        btn_hapus.setBorderPainted(false);
        btn_hapus.setContentAreaFilled(false);
        btn_hapus.setForeground(UITheme.ACCENT);
        btn_hapus.setFont(UITheme.FONT_BOLD.deriveFont(12f));
        btn_hapus.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn_hapus.addActionListener(e -> btn_hapusActionPerformed(e));
        lineActions.add(btn_perbarui);
        lineActions.add(btn_hapus);
        form.add(lineActions);
        form.add(Box.createVerticalStrut(16));

        lb_unitsReceived = new JLabel("0");
        lb_stockValue = new JLabel(money(0));
        JPanel unitsRow = new JPanel(new BorderLayout());
        unitsRow.setOpaque(false);
        unitsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        unitsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel lbU = new JLabel("Units received");
        lbU.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        lbU.setForeground(UITheme.TEXT_MUTED);
        lb_unitsReceived.setFont(UITheme.FONT_BOLD.deriveFont(13f));
        unitsRow.add(lbU, BorderLayout.WEST);
        unitsRow.add(lb_unitsReceived, BorderLayout.EAST);
        JPanel valueRow = new JPanel(new BorderLayout());
        valueRow.setOpaque(false);
        valueRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel lbV = new JLabel("Stock value added");
        lbV.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        lbV.setForeground(UITheme.TEXT_MUTED);
        lb_stockValue.setFont(UITheme.FONT_BOLD.deriveFont(13f));
        valueRow.add(lbV, BorderLayout.WEST);
        valueRow.add(lb_stockValue, BorderLayout.EAST);
        form.add(unitsRow);
        form.add(valueRow);
        form.add(Box.createVerticalGlue());

        JPanel actions = new JPanel();
        actions.setBackground(UITheme.ACCENT);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));

        JLabel lbTotalCap = new JLabel("PURCHASE TOTAL");
        lbTotalCap.setFont(UITheme.FONT_CAPTION);
        lbTotalCap.setForeground(Color.WHITE);
        lbTotalCap.setAlignmentX(Component.LEFT_ALIGNMENT);
        lb_purchaseTotal = new JLabel(money(0));
        lb_purchaseTotal.setFont(UITheme.FONT_KPI_VALUE.deriveFont(28f));
        lb_purchaseTotal.setForeground(Color.WHITE);
        lb_purchaseTotal.setAlignmentX(Component.LEFT_ALIGNMENT);

        btn_simpan.setText("Post to stock");
        btn_simpan.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn_simpan.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        PageUI.stylePrimaryButton(btn_simpan);
        btn_simpan.setBackground(Color.WHITE);
        btn_simpan.setForeground(UITheme.ACCENT);
        btn_simpan.addActionListener(e -> btn_simpanActionPerformed(e));

        JPanel secondary = new JPanel(new GridLayout(1, 2, 8, 0));
        secondary.setOpaque(false);
        secondary.setAlignmentX(Component.LEFT_ALIGNMENT);
        secondary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JButton btnDraft = outlineBtn("Save draft");
        btnDraft.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Draft kept on this screen. Post to stock when the delivery note is checked."));
        JButton btnDiscard = outlineBtn("Discard");
        btnDiscard.addActionListener(e -> btn_segarkanActionPerformed(null));
        secondary.add(btnDraft);
        secondary.add(btnDiscard);

        actions.add(lbTotalCap);
        actions.add(Box.createVerticalStrut(4));
        actions.add(lb_purchaseTotal);
        actions.add(Box.createVerticalStrut(12));
        actions.add(btn_simpan);
        actions.add(Box.createVerticalStrut(8));
        actions.add(secondary);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.getViewport().setBackground(PANEL_BG);
        formScroll.setOpaque(false);

        sidebar.add(sideHead, BorderLayout.NORTH);
        sidebar.add(formScroll, BorderLayout.CENTER);
        sidebar.add(actions, BorderLayout.SOUTH);

        body.add(left, BorderLayout.CENTER);
        body.add(sidebar, BorderLayout.EAST);
        jPanel1.add(body, BorderLayout.CENTER);
        add(jPanel1, BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void tbl_detailBarangMouseClicked(java.awt.event.MouseEvent evt) {
        int row = tbl_detailBarang.getSelectedRow();
        loadSelectedLine(row);
    }

    private void txt_jumlahBarangKeyTyped(java.awt.event.KeyEvent evt) {
        char c = evt.getKeyChar();
        if (c == KeyEvent.VK_BACK_SPACE || c == KeyEvent.VK_DELETE) {
            return;
        }
        if (Character.isDigit(c)) {
            return;
        }
        if (c == '.' && txt_jumlahBarang.getText().indexOf('.') < 0) {
            return;
        }
        JOptionPane.showMessageDialog(null, "Numbers only", "Invalid input", JOptionPane.ERROR_MESSAGE);
        evt.consume();
    }

    private void btn_hapusActionPerformed(java.awt.event.ActionEvent evt) {
        DefaultTableModel detail = (DefaultTableModel) tbl_detailBarang.getModel();
        int row = tbl_detailBarang.getSelectedRow();
        if (row >= 0) {
            detail.removeRow(row);
            bersihInput();
            clearSelectedLine();
            updateCartStatus();
            updatePurchaseTotal();
        } else {
            JOptionPane.showMessageDialog(null, "Select a line to remove.");
        }
    }

    private void btn_perbaruiActionPerformed(java.awt.event.ActionEvent evt) {
        int row = tbl_detailBarang.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a line to update.");
            return;
        }
        try {
            String code = tbl_detailBarang.getValueAt(row, 0).toString();
            String name = tbl_detailBarang.getValueAt(row, 1).toString();
            BigDecimal qty = QuantityUtil.parse(txt_jumlahBarang.getText());
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(this, "Enter a valid quantity.");
                return;
            }
            int buy = Integer.parseInt(txt_hargaJual.getText().trim().replace(",", ""));
            ProductRow p = findProduct(code);
            BigDecimal stock = p != null ? p.stock : BigDecimal.ZERO;
            tbl_detailBarang.setValueAt(name, row, 1);
            tbl_detailBarang.setValueAt(qty.toPlainString(), row, 2);
            tbl_detailBarang.setValueAt(buy, row, 3);
            tbl_detailBarang.setValueAt(QuantityUtil.moneySubtotal(qty, buy), row, 4);
            tbl_detailBarang.setValueAt(stock.add(qty).toPlainString(), row, 5);
            loadSelectedLine(row);
            updatePurchaseTotal();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Check quantity and buy price.");
        }
    }

    private void btn_simpanActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            if (tbl_detailBarang.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "Add at least one line before posting.");
                return;
            }
            int idTransaksi = Integer.valueOf(txt_kodeTransaksi.getText());
            String tanggal = txt_tanggalTransaksi.getText();
            int idUser = Integer.valueOf(txt_idKasir.getText());

            String purchaseUuid = Ids.newUuid();
            String penjualan = "INSERT INTO `pembelian`(`pembelian_Id`, `tanggal_pembelian`, `user_Id`, `uuid`) "
                    + "VALUES (?,?,?,?)";
            ps = Koneksi.getConnection().prepareStatement(penjualan);
            ps.setInt(1, idTransaksi);
            ps.setString(2, tanggal);
            ps.setInt(3, idUser);
            ps.setString(4, purchaseUuid);
            ps.executeUpdate();

            int jumlahBaris = tbl_detailBarang.getRowCount();
            for (int i = 0; i < jumlahBaris; i++) {
                String detail = "INSERT INTO `detail_pembelian`(`pembelian_Id`, `kode_produk`, `jumlah`, `uuid`) "
                        + "VALUES (?,?,?,?)";
                ps = Koneksi.getConnection().prepareStatement(detail);
                ps.setInt(1, idTransaksi);
                ps.setString(2, tbl_detailBarang.getValueAt(i, 0).toString());
                ps.setBigDecimal(3, QuantityUtil.parse(tbl_detailBarang.getValueAt(i, 2).toString()));
                ps.setString(4, Ids.newUuid());
                ps.executeUpdate();

                // Apply buy price if edited on this PO line.
                try {
                    int buy = Integer.parseInt(tbl_detailBarang.getValueAt(i, 3).toString());
                    ps = Koneksi.getConnection().prepareStatement(
                            "UPDATE produk SET harga_beli=? WHERE kode_produk=?");
                    ps.setInt(1, buy);
                    ps.setString(2, tbl_detailBarang.getValueAt(i, 0).toString());
                    ps.executeUpdate();
                } catch (Exception ignored) {
                }
            }

            SyncOutbox.enqueuePurchaseById(idTransaksi);
            JOptionPane.showMessageDialog(this, "Purchase posted to stock.");
            txt_idKasir.setText(user.getId());
            txt_namaKasir.setText(user.getNama());
            tampilBarang();
            id();
            date();
            bersihInput();
            txt_invoice.setText("");
            DefaultTableModel model = (DefaultTableModel) tbl_detailBarang.getModel();
            model.setRowCount(0);
            clearSelectedLine();
            updateCartStatus();
            updatePurchaseTotal();
            refreshMeta();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please check again: " + e);
        }
    }

    private void btn_segarkanActionPerformed(java.awt.event.ActionEvent evt) {
        txt_idKasir.setText(user.getId());
        txt_namaKasir.setText(user.getNama());
        tampilBarang();
        id();
        date();
        bersihInput();
        txt_invoice.setText("");
        if (cb_supplier.getItemCount() > 0) {
            cb_supplier.setSelectedIndex(0);
        }
        DefaultTableModel model = (DefaultTableModel) tbl_detailBarang.getModel();
        model.setRowCount(0);
        clearSelectedLine();
        updateCartStatus();
        updatePurchaseTotal();
        refreshMeta();
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
    private javax.swing.JTextField txt_tanggalTransaksi;
    // End of variables declaration//GEN-END:variables
}
