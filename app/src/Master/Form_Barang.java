/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Master;

import Main.PageUI;
import Main.UITheme;
import com.barcodelib.barcode.Linear;
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
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.print.*;
import javax.print.attribute.*;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.view.JasperViewer;

/**
 * Products master — list + edit-sidebar layout matching the reference.
 */
public class Form_Barang extends javax.swing.JPanel {

    private JLabel lb_status;
    private JLabel lb_endResults;
    private JLabel lb_margin;
    private JLabel lb_printed;
    private JLabel lb_editTitle;
    private static final Color PANEL_BG = new Color(0xF3F3F1);
    private static final Color RULE = new Color(0xD0D0CC);
    private static final String PLACEHOLDER_CAT = "Select category";
    private static final String PLACEHOLDER_SUP = "Select supplier";
    private static final String PLACEHOLDER_UNIT = "Select unit";

    public Form_Barang() {
        initComponents();

        SelectKategori();
        SelectSupplier();
        SelectSatuan();
        txttemp_kode.setVisible(false);
        txttemp_IDkategori.setVisible(false);
        txttemp_IDsupplier.setVisible(false);
        txttemp_IDsatuan.setVisible(false);
        txt_id.setVisible(false);
        GetData();
        BtnEnabled(false);
        btn_simpan.setText("Save product");
        txt_id.setEditable(false);
        btn_dapatKode.setVisible(true);
        updateMargin();
    }


    /** Hidden default brand so produk.merek_Id FK stays satisfied without brand UI. */
    private int ensureDefaultMerekId() throws SQLException, ClassNotFoundException {
        Connection conn = Koneksi.getConnection();
        java.sql.Statement stm = conn.createStatement();
        java.sql.ResultSet rs = stm.executeQuery(
                "SELECT merek_Id FROM merek WHERE nama_merek='General' LIMIT 1");
        if (rs.next()) {
            return rs.getInt(1);
        }
        try {
            stm.executeUpdate(
                    "INSERT INTO merek(nama_merek, uuid) VALUES ('General', UUID())");
        } catch (SQLException ignore) {
            // uuid column may be missing on older DBs
            try {
                stm.executeUpdate("INSERT INTO merek(nama_merek) VALUES ('General')");
            } catch (SQLException ignore2) {
            }
        }
        rs = stm.executeQuery("SELECT merek_Id FROM merek WHERE nama_merek='General' LIMIT 1");
        if (rs.next()) {
            return rs.getInt(1);
        }
        rs = stm.executeQuery("SELECT merek_Id FROM merek ORDER BY merek_Id LIMIT 1");
        if (rs.next()) {
            return rs.getInt(1);
        }
        throw new SQLException("No brand row available for product FK.");
    }

    private void SelectKategori() {
        try {
            Connection conn = Koneksi.getConnection();
            java.sql.Statement stm = conn.createStatement();
            java.sql.ResultSet rs = stm.executeQuery("SELECT * FROM kategori ORDER BY nama_kategori");
            cb_kategori.removeAllItems();
            cb_kategori.addItem(PLACEHOLDER_CAT);
            while (rs.next()) {
                cb_kategori.addItem(rs.getString("nama_kategori"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error " + e);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Form_Barang.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    private void SelectSupplier() {
        try {
            Connection conn = Koneksi.getConnection();
            java.sql.Statement stm = conn.createStatement();
            java.sql.ResultSet rs = stm.executeQuery("SELECT * FROM supplier ORDER BY nama_supplier");
            cb_supplier.removeAllItems();
            cb_supplier.addItem(PLACEHOLDER_SUP);
            while (rs.next()) {
                cb_supplier.addItem(rs.getString("nama_supplier"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error " + e);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Form_Barang.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void SelectSatuan() {
        try {
            Connection conn = Koneksi.getConnection();
            if (conn == null) {
                JOptionPane.showMessageDialog(null, "Database connection is not available.");
                return;
            }
            java.sql.Statement stm = conn.createStatement();
            java.sql.ResultSet rs = stm.executeQuery(
                    "SELECT nama_satuan FROM satuan ORDER BY nama_satuan");
            javax.swing.DefaultComboBoxModel<String> model =
                    new javax.swing.DefaultComboBoxModel<String>();
            model.addElement(PLACEHOLDER_UNIT);
            while (rs.next()) {
                String name = rs.getString("nama_satuan");
                if (name != null && !name.trim().isEmpty()) {
                    model.addElement(name);
                }
            }
            rs.close();
            stm.close();
            cb_satuan.setModel(model);
            cb_satuan.setSelectedIndex(0);
            if (model.getSize() <= 1) {
                JOptionPane.showMessageDialog(null,
                        "No units found in satuan table. Run the satuan migration SQL.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error loading units: " + e.getMessage());
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Form_Barang.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void TxtEmpty() {
        if (cb_satuan.getItemCount() <= 1) {
            SelectSatuan();
        }
        txt_id.setText("");
        txt_kode.setText("");
        txt_nama.setText("");
        txt_beli.setText("");
        txt_jual.setText("");
        txt_stok.setText("");
        txttemp_IDkategori.setText("");
        txttemp_IDsupplier.setText("");
        txttemp_IDsatuan.setText("");
        txttemp_kode.setText("");
        cb_kategori.setSelectedItem(PLACEHOLDER_CAT);
        cb_supplier.setSelectedItem(PLACEHOLDER_SUP);
        cb_satuan.setSelectedItem(PLACEHOLDER_UNIT);
        jLabel9.setText("Selected: —");
        showBarcodePreview("");
        txt_kode.setEditable(true);
        btn_dapatKode.setVisible(true);
        if (lb_editTitle != null) {
            lb_editTitle.setText("NEW PRODUCT");
        }
        updateMargin();
    }

    private void BtnEnabled(boolean x) {
        btn_hapus.setEnabled(x);
    }

    private void applyListModel(java.sql.ResultSet sql) throws SQLException {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"CODE", "PRODUCT", "CATEGORY", "BUY", "SELL", "STOCK"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        while (sql.next()) {
            model.addRow(new Object[]{
                sql.getString("CODE"),
                sql.getString("PRODUCT"),
                sql.getString("CATEGORY"),
                sql.getString("BUY"),
                sql.getString("SELL"),
                sql.getString("STOCK")
            });
        }
        tbl_supplier.setModel(model);
        if (tbl_supplier.getColumnCount() >= 6) {
            tbl_supplier.getColumnModel().getColumn(0).setPreferredWidth(90);
            tbl_supplier.getColumnModel().getColumn(1).setPreferredWidth(180);
            tbl_supplier.getColumnModel().getColumn(2).setPreferredWidth(120);
            tbl_supplier.getColumnModel().getColumn(3).setPreferredWidth(80);
            tbl_supplier.getColumnModel().getColumn(4).setPreferredWidth(80);
            tbl_supplier.getColumnModel().getColumn(5).setPreferredWidth(60);
        }
        PageUI.styleTable(tbl_supplier);
        refreshStatus(model.getRowCount());
    }

    private void refreshStatus(int rows) {
        int missingSupplier = countMissingSupplier();
        if (lb_status != null) {
            String unit = rows == 1 ? "product" : "products";
            String need = missingSupplier == 1 ? "1 needs a supplier"
                    : missingSupplier + " need a supplier";
            lb_status.setText(rows + " " + unit + (missingSupplier > 0 ? " · " + need : ""));
        }
        lblcount_rows.setText("Rows: " + rows);
        if (lb_endResults != null) {
            if (rows == 0) {
                lb_endResults.setText("No products yet. Use + New to add a product.");
            } else {
                lb_endResults.setText("End of list — " + rows + " of " + rows + " products.");
            }
        }
    }

    private int countMissingSupplier() {
        try {
            Connection conn = Koneksi.getConnection();
            java.sql.Statement stm = conn.createStatement();
            java.sql.ResultSet rs = stm.executeQuery(
                    "SELECT COUNT(*) FROM produk WHERE supplier_Id IS NULL OR supplier_Id = 0 OR supplier_Id = ''");
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private void GetData() {
        try {
            Connection conn = Koneksi.getConnection();
            java.sql.Statement stm = conn.createStatement();
            java.sql.ResultSet sql = stm.executeQuery(
                    "SELECT kode_produk AS CODE, nama_produk AS PRODUCT, "
                    + "nama_kategori AS CATEGORY, "
                    + "harga_beli AS BUY, harga_jual AS SELL, stok_produk AS STOCK FROM tableproduk");
            applyListModel(sql);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error " + e);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Form_User.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void GetData_View() {
        int row = tbl_supplier.getSelectedRow();
        if (row < 0) {
            return;
        }
        String row_id = tbl_supplier.getModel().getValueAt(row, 0).toString();
        String name = tbl_supplier.getModel().getValueAt(row, 1).toString();
        txt_id.setText(row_id);
        jLabel9.setText("Selected: " + name);
        BtnEnabled(true);
        if (lb_editTitle != null) {
            lb_editTitle.setText("EDIT PRODUCT");
        }
    }

    private void updateMargin() {
        if (lb_margin == null) {
            return;
        }
        try {
            int buy = Integer.parseInt(txt_beli.getText().trim().replace(",", ""));
            int sell = Integer.parseInt(txt_jual.getText().trim().replace(",", ""));
            int margin = sell - buy;
            int pct = buy == 0 ? 0 : (int) Math.round((margin * 100.0) / buy);
            lb_margin.setText(UITheme.CURRENCY + " "
                    + NumberFormat.getIntegerInstance(Locale.US).format(margin)
                    + " · " + pct + "%");
            lb_margin.setForeground(margin >= 0 ? UITheme.ACCENT : UITheme.DANGER);
        } catch (Exception e) {
            lb_margin.setText(UITheme.CURRENCY + " 0 · —");
            lb_margin.setForeground(UITheme.TEXT_MUTED);
        }
    }

    private JLabel fieldCaption(String text) {
        JLabel lb = new JLabel(text);
        PageUI.styleCaption(lb);
        return lb;
    }

    private JPanel fieldBlock(String caption, Component field) {
        JPanel wrap = new JPanel(new BorderLayout(0, 4));
        wrap.setOpaque(false);
        wrap.add(fieldCaption(caption), BorderLayout.NORTH);
        wrap.add(field, BorderLayout.CENTER);
        return wrap;
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
        jPanel3 = new JPanel();
        jLabel1 = new JLabel();
        btn_tambah = new JButton();
        btn_hapus = new JButton();
        jScrollPane1 = new JScrollPane();
        tbl_supplier = new JTable();
        lblcount_rows = new JLabel();
        jLabel9 = new JLabel();
        jPanel4 = new JPanel();
        txt_jual = new JTextField();
        txt_nama = new JTextField();
        txt_beli = new JTextField();
        jLabel3 = new JLabel();
        jLabel5 = new JLabel();
        jLabel4 = new JLabel();
        btn_simpan = new JButton();
        btn_batal = new JButton();
        txt_kode = new JTextField();
        jLabel6 = new JLabel();
        jLabel7 = new JLabel();
        txt_stok = new JTextField();
        jLabel8 = new JLabel();
        jLabel10 = new JLabel();
        jLabel11 = new JLabel();
        jLabel12 = new JLabel();
        cb_kategori = new JComboBox<>();
        cb_merek = new JComboBox<>();
        cb_merek.setVisible(false);
        cb_supplier = new JComboBox<>();
        cb_satuan = new JComboBox<>();
        barcode = new JLabel();
        btn_dapatKode = new JButton();
        btn_segarkan = new JButton();
        txttemp_kode = new JTextField();
        txttemp_IDsupplier = new JTextField();
        txttemp_IDmerek = new JTextField();
        txttemp_IDmerek.setVisible(false);
        txttemp_IDkategori = new JTextField();
        txttemp_IDsatuan = new JTextField();
        txt_id = new JTextField();
        txt_cariBarang = new JTextField();
        btn_cari = new JButton();
        btn_cetak = new JButton();
        btn_printLabel = new JButton();

        setLayout(new BorderLayout());
        PageUI.paintPage(this);

        jPanel3.setBackground(UITheme.PAGE_BG);
        jPanel3.setLayout(new BorderLayout());

        // ---- Header ----
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.PAGE_BG);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, RULE));

        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headerLeft.setOpaque(false);

        JLabel badge = new JLabel("  MASTER / 02  ");
        badge.setOpaque(true);
        badge.setBackground(PageUI.INK);
        badge.setForeground(Color.WHITE);
        badge.setFont(UITheme.FONT_BOLD.deriveFont(11f));
        badge.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 0));
        jLabel1.setText("Products");
        jLabel1.setFont(UITheme.FONT_HEADING.deriveFont(22f));
        jLabel1.setForeground(PageUI.INK);
        jLabel1.setAlignmentX(Component.LEFT_ALIGNMENT);
        lb_status = new JLabel("0 products");
        lb_status.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        lb_status.setForeground(UITheme.TEXT_MUTED);
        lb_status.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleWrap.add(jLabel1);
        titleWrap.add(lb_status);

        headerLeft.add(badge);
        headerLeft.add(titleWrap);
        header.add(headerLeft, BorderLayout.WEST);
        jPanel3.add(header, BorderLayout.NORTH);

        // ---- Body: list | edit ----
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(UITheme.PAGE_BG);

        // LEFT list
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(UITheme.PAGE_BG);
        left.setBorder(BorderFactory.createEmptyBorder(14, 16, 12, 12));

        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        PageUI.styleField(txt_cariBarang);
        txt_cariBarang.putClientProperty("JTextField.placeholderText", "Search code, name or category.");
        txt_cariBarang.addActionListener(e -> cariBarang());
        btn_cari.setText("Search");
        PageUI.stylePrimaryButton(btn_cari);
        btn_cari.addActionListener(e -> btn_cariActionPerformed(e));
        btn_segarkan.setText("Refresh");
        PageUI.styleGhostButton(btn_segarkan);
        btn_segarkan.setForeground(PageUI.INK);
        btn_segarkan.addActionListener(e -> btn_segarkanActionPerformed(e));
        btn_cetak.setText("Print list");
        PageUI.styleGhostButton(btn_cetak);
        btn_cetak.setForeground(PageUI.INK);
        btn_cetak.addActionListener(e -> btn_cetakActionPerformed(e));
        btn_printLabel.setText("Print Label");
        PageUI.styleGhostButton(btn_printLabel);
        btn_printLabel.setForeground(PageUI.INK);
        btn_printLabel.addActionListener(e -> btn_printLabelActionPerformed(e));
        btn_importExcel = new JButton("Import Excel");
        PageUI.styleGhostButton(btn_importExcel);
        btn_importExcel.setForeground(PageUI.INK);
        btn_importExcel.addActionListener(e -> btn_importExcelActionPerformed(e));
        btn_exportExcel = new JButton("Export Excel");
        PageUI.styleGhostButton(btn_exportExcel);
        btn_exportExcel.setForeground(PageUI.INK);
        btn_exportExcel.addActionListener(e -> btn_exportExcelActionPerformed(e));
        btn_deleteAll = new JButton("Delete all");
        PageUI.styleGhostButton(btn_deleteAll);
        btn_deleteAll.setForeground(new Color(0xB00020));
        btn_deleteAll.addActionListener(e -> btn_deleteAllActionPerformed(e));

        JPanel searchBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchBtns.setOpaque(false);
        searchBtns.add(btn_cari);
        searchBtns.add(btn_segarkan);
        searchBtns.add(btn_importExcel);
        searchBtns.add(btn_exportExcel);
        searchBtns.add(btn_deleteAll);
        searchBtns.add(btn_cetak);
        searchBtns.add(btn_printLabel);
        searchRow.add(txt_cariBarang, BorderLayout.CENTER);
        searchRow.add(searchBtns, BorderLayout.EAST);

        tbl_supplier.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"CODE", "PRODUCT", "CATEGORY", "BUY", "SELL", "STOCK"}));
        tbl_supplier.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tbl_supplierMouseClicked(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tbl_supplierMouseReleased(evt);
            }
        });
        tbl_supplier.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tbl_supplierKeyReleased(evt);
            }
        });
        PageUI.styleTable(tbl_supplier);
        jScrollPane1.setViewportView(tbl_supplier);
        PageUI.styleScroll(jScrollPane1);

        lb_endResults = new JLabel(" ");
        lb_endResults.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lb_endResults.setForeground(UITheme.TEXT_MUTED);
        lb_endResults.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(PANEL_BG);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, RULE),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        lblcount_rows.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lblcount_rows.setForeground(UITheme.TEXT_MUTED);
        lblcount_rows.setText("Rows: 0");
        jLabel9.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        jLabel9.setForeground(UITheme.TEXT_MUTED);
        jLabel9.setText("Selected: —");
        footer.add(lblcount_rows, BorderLayout.WEST);
        footer.add(jLabel9, BorderLayout.EAST);

        JPanel leftCenter = new JPanel(new BorderLayout());
        leftCenter.setOpaque(false);
        leftCenter.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        leftCenter.add(jScrollPane1, BorderLayout.CENTER);
        leftCenter.add(lb_endResults, BorderLayout.SOUTH);

        left.add(searchRow, BorderLayout.NORTH);
        left.add(leftCenter, BorderLayout.CENTER);
        left.add(footer, BorderLayout.SOUTH);

        // RIGHT edit sidebar
        jPanel4.setBackground(PANEL_BG);
        jPanel4.setPreferredSize(new Dimension(360, 10));
        jPanel4.setMinimumSize(new Dimension(320, 10));
        jPanel4.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, RULE));
        jPanel4.setLayout(new BorderLayout());

        JPanel editHead = new JPanel(new BorderLayout());
        editHead.setOpaque(false);
        editHead.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, RULE),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        lb_editTitle = new JLabel("EDIT PRODUCT");
        lb_editTitle.setFont(UITheme.FONT_CAPTION);
        lb_editTitle.setForeground(UITheme.TEXT_CAPTION);
        btn_tambah.setText("+ New");
        btn_tambah.setFocusPainted(false);
        btn_tambah.setBorderPainted(false);
        btn_tambah.setContentAreaFilled(false);
        btn_tambah.setForeground(UITheme.ACCENT);
        btn_tambah.setFont(UITheme.FONT_BOLD.deriveFont(12f));
        btn_tambah.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn_tambah.addActionListener(e -> btn_tambahActionPerformed(e));
        editHead.add(lb_editTitle, BorderLayout.WEST);
        editHead.add(btn_tambah, BorderLayout.EAST);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        PageUI.styleField(txt_kode);
        PageUI.styleField(txt_nama);
        PageUI.styleField(txt_beli);
        PageUI.styleField(txt_jual);
        PageUI.styleField(txt_stok);
        styleCombo(cb_kategori);
        styleCombo(cb_supplier);
        styleCombo(cb_satuan);
        cb_satuan.setForeground(PageUI.INK);
        cb_satuan.setOpaque(true);

        txt_beli.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(KeyEvent evt) { txt_beliKeyTyped(evt); }
            public void keyReleased(KeyEvent evt) { updateMargin(); }
        });
        txt_jual.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(KeyEvent evt) { txt_jualKeyTyped(evt); }
            public void keyReleased(KeyEvent evt) { updateMargin(); }
        });
        txt_stok.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(KeyEvent evt) { txt_stokKeyTyped(evt); }
        });
        cb_kategori.addItemListener(evt -> cb_kategoriItemStateChanged(evt));
        cb_supplier.addItemListener(evt -> cb_supplierItemStateChanged(evt));
        cb_satuan.addItemListener(evt -> cb_satuanItemStateChanged(evt));

        btn_dapatKode.setText("Fetch");
        PageUI.styleGhostButton(btn_dapatKode);
        btn_dapatKode.setForeground(PageUI.INK);
        btn_dapatKode.addActionListener(e -> btn_dapatKodeActionPerformed(e));

        JPanel kodeRow = new JPanel(new BorderLayout(6, 0));
        kodeRow.setOpaque(false);
        kodeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        kodeRow.add(txt_kode, BorderLayout.CENTER);
        kodeRow.add(btn_dapatKode, BorderLayout.EAST);

        JPanel priceRow = new JPanel(new GridLayout(1, 2, 10, 0));
        priceRow.setOpaque(false);
        priceRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        jLabel4.setText("BUY PRICE");
        jLabel5.setText("SELL PRICE");
        priceRow.add(fieldBlock("BUY PRICE", txt_beli));
        priceRow.add(fieldBlock("SELL PRICE", txt_jual));

        lb_margin = new JLabel(UITheme.CURRENCY + " 0 · —");
        lb_margin.setFont(UITheme.FONT_BOLD.deriveFont(13f));
        lb_margin.setForeground(UITheme.ACCENT);
        lb_margin.setBorder(BorderFactory.createEmptyBorder(4, 0, 10, 0));
        lb_margin.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel grid = new JPanel(new GridLayout(3, 2, 10, 10));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        grid.add(fieldBlock("STOCK", txt_stok));
        grid.add(fieldBlock("UNIT", cb_satuan));
        grid.add(fieldBlock("CATEGORY", cb_kategori));
        grid.add(fieldBlock("SUPPLIER", cb_supplier));
        JPanel emptyCell = new JPanel();
        emptyCell.setOpaque(false);
        grid.add(emptyCell);

        jLabel3.setVisible(false);
        jLabel6.setVisible(false);
        jLabel7.setVisible(false);
        jLabel8.setVisible(false);
        jLabel10.setVisible(false);
        jLabel11.setVisible(false);
        jLabel12.setVisible(false);

        JPanel kodeBlock = fieldBlock("PRODUCT CODE", kodeRow);
        kodeBlock.setAlignmentX(Component.LEFT_ALIGNMENT);
        kodeBlock.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        JPanel namaBlock = fieldBlock("NAME", txt_nama);
        namaBlock.setAlignmentX(Component.LEFT_ALIGNMENT);
        namaBlock.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        priceRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(kodeBlock);
        form.add(Box.createVerticalStrut(10));
        form.add(namaBlock);
        form.add(Box.createVerticalStrut(10));
        form.add(priceRow);
        form.add(lb_margin);
        form.add(grid);
        form.add(Box.createVerticalStrut(14));

        JPanel barcodeMeta = new JPanel(new BorderLayout(0, 4));
        barcodeMeta.setOpaque(false);
        barcodeMeta.setAlignmentX(Component.LEFT_ALIGNMENT);
        barcodeMeta.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        JLabel lbBarcode = new JLabel("Barcode");
        PageUI.styleCaption(lbBarcode);
        lb_printed = new JLabel("Printed " + new SimpleDateFormat("d MMM yyyy").format(new Date()));
        lb_printed.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lb_printed.setForeground(UITheme.TEXT_MUTED);
        JPanel barcodeTop = new JPanel(new BorderLayout());
        barcodeTop.setOpaque(false);
        barcodeTop.add(lbBarcode, BorderLayout.WEST);
        barcodeTop.add(lb_printed, BorderLayout.EAST);
        barcode.setHorizontalAlignment(SwingConstants.LEFT);
        barcode.setAlignmentX(Component.LEFT_ALIGNMENT);
        barcodeMeta.add(barcodeTop, BorderLayout.NORTH);
        barcodeMeta.add(barcode, BorderLayout.CENTER);
        form.add(barcodeMeta);
        form.add(Box.createVerticalGlue());

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, RULE),
                BorderFactory.createEmptyBorder(12, 16, 16, 16)));

        btn_simpan.setText("Save product");
        PageUI.stylePrimaryButton(btn_simpan);
        btn_simpan.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn_simpan.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn_simpan.addActionListener(e -> btn_simpanActionPerformed(e));

        JPanel secondary = new JPanel(new GridLayout(1, 2, 8, 0));
        secondary.setOpaque(false);
        secondary.setAlignmentX(Component.LEFT_ALIGNMENT);
        secondary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn_batal.setText("Cancel");
        PageUI.stylePrimaryButton(btn_batal);
        btn_batal.addActionListener(e -> btn_batalActionPerformed(e));
        btn_hapus.setText("Delete");
        PageUI.stylePrimaryButton(btn_hapus);
        btn_hapus.addActionListener(e -> btn_hapusActionPerformed(e));
        secondary.add(btn_batal);
        secondary.add(btn_hapus);

        actions.add(btn_simpan);
        actions.add(Box.createVerticalStrut(8));
        actions.add(secondary);

        jPanel4.add(editHead, BorderLayout.NORTH);
        jPanel4.add(form, BorderLayout.CENTER);
        jPanel4.add(actions, BorderLayout.SOUTH);

        body.add(left, BorderLayout.CENTER);
        body.add(jPanel4, BorderLayout.EAST);
        jPanel3.add(body, BorderLayout.CENTER);

        add(jPanel3, BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void btn_tambahActionPerformed(java.awt.event.ActionEvent evt) {
        tbl_supplier.clearSelection();
        TxtEmpty();
        BtnEnabled(false);
        btn_simpan.setText("Save product");
        txt_nama.requestFocus();
    }

    private void btn_hapusActionPerformed(java.awt.event.ActionEvent evt) {
        int ok = JOptionPane.showConfirmDialog(null, "Delete this product?", "Confirm", JOptionPane.OK_CANCEL_OPTION);
        if (ok == 0) {
            try {
                String row_id = txt_id.getText();
                Connection conn = Koneksi.getConnection();
                java.sql.Statement stm = conn.createStatement();
                stm.executeUpdate("DELETE FROM produk WHERE kode_produk = '" + row_id + "'");
                JOptionPane.showMessageDialog(null, "Product deleted.");
                btn_tambah.doClick();
                GetData();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error " + e);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Form_Barang.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void tbl_supplierMouseClicked(java.awt.event.MouseEvent evt) {
        GetData_View();
        Mouseklik();
    }

    private void tbl_supplierMouseReleased(java.awt.event.MouseEvent evt) {
        GetData_View();
        Mouseklik();
    }

    private void tbl_supplierKeyReleased(java.awt.event.KeyEvent evt) {
        GetData_View();
        Mouseklik();
    }

    private void txt_jualKeyTyped(java.awt.event.KeyEvent evt) {
        char c = evt.getKeyChar();
        if (!(Character.isDigit(c)) && !(c == KeyEvent.VK_BACK_SPACE)) {
            JOptionPane.showMessageDialog(null, "Numbers only", "Invalid input", JOptionPane.ERROR_MESSAGE);
            evt.consume();
        }
    }

    private void btn_simpanActionPerformed(java.awt.event.ActionEvent evt) {
        String row_id = txt_id.getText();
        String row_txtkode = txt_kode.getText();
        String row_txtnama = txt_nama.getText();
        String row_txtbeli = txt_beli.getText();
        String row_txtjual = txt_jual.getText();
        String row_txtstok = txt_stok.getText();
        String row_txttemp_kode = txttemp_kode.getText();
        String row_txtkategori = txttemp_IDkategori.getText();
        String row_txtsupplier = txttemp_IDsupplier.getText();
        String row_txtsatuan = txttemp_IDsatuan.getText();
        int c_kode = 0;

        if (!"".equals(row_txtkode) && !"".equals(row_txtnama) && !"".equals(row_txtbeli) && !"".equals(row_txtjual)
                && !"".equals(row_txtstok) && !"".equals(row_txtkategori)
                && !"".equals(row_txtsupplier) && !"".equals(row_txtsatuan)) {
            int row_txtmerek;
            try {
                row_txtmerek = ensureDefaultMerekId();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Error " + ex);
                return;
            }
            try {
                Connection conn = Koneksi.getConnection();
                java.sql.Statement stm = conn.createStatement();
                java.sql.ResultSet sql = stm.executeQuery("SELECT COUNT(kode_produk) as count FROM produk WHERE kode_produk='" + row_txtkode + "'");
                sql.next();
                c_kode = sql.getInt("count");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error " + e);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Form_Barang.class.getName()).log(Level.SEVERE, null, ex);
            }

            if ("".equals(row_id)) {
                if (c_kode == 0) {
                    try {
                        Connection conn = Koneksi.getConnection();
                        PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO produk(kode_produk, nama_produk, harga_beli, harga_jual, stok_produk, "
                                + "kategori_Id, merek_Id, supplier_Id, satuan_Id, uuid) "
                                + "VALUES (?,?,?,?,?,?,?,?,?,?)");
                        ps.setString(1, row_txtkode);
                        ps.setString(2, row_txtnama);
                        ps.setInt(3, Integer.parseInt(row_txtbeli.trim()));
                        ps.setInt(4, Integer.parseInt(row_txtjual.trim()));
                        ps.setBigDecimal(5, new BigDecimal(row_txtstok.trim()));
                        ps.setInt(6, Integer.parseInt(row_txtkategori.trim()));
                        ps.setInt(7, row_txtmerek);
                        ps.setInt(8, Integer.parseInt(row_txtsupplier.trim()));
                        ps.setInt(9, Integer.parseInt(row_txtsatuan.trim()));
                        ps.setString(10, Ids.newUuid());
                        ps.executeUpdate();
                        ps.close();
                        SyncOutbox.enqueueProductByKode(row_txtkode);
                        JOptionPane.showMessageDialog(null, "Product saved.");
                        btn_tambah.doClick();
                        GetData();
                        generate(row_txtkode);
                    } catch (SQLException e) {
                        JOptionPane.showMessageDialog(null, "Error " + e);
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(Form_Barang.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (Exception ex) {
                        Logger.getLogger(Form_Barang.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "This product code already exists.", "Could not save", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                if (c_kode == 0 || row_txtkode.equals(row_txttemp_kode)) {
                    try {
                        Connection conn = Koneksi.getConnection();
                        java.sql.Statement stm = conn.createStatement();
                        stm.executeUpdate("UPDATE produk SET nama_produk ='" + row_txtnama + "',harga_beli= '" + row_txtbeli
                                + "',harga_jual= '" + row_txtjual + "',stok_produk= '" + row_txtstok + "',kategori_Id= '"
                                + row_txtkategori + "',merek_Id= '" + row_txtmerek + "',supplier_Id= '" + row_txtsupplier
                                + "',satuan_Id= '" + row_txtsatuan
                                + "' WHERE kode_produk = '" + row_id + "'");
                        SyncOutbox.enqueueProductByKode(row_txtkode);
                        JOptionPane.showMessageDialog(null, "Product updated.");
                        btn_tambah.doClick();
                        GetData();
                        btn_dapatKode.setVisible(true);
                    } catch (SQLException e) {
                        JOptionPane.showMessageDialog(null, "Error " + e);
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(Form_Barang.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "This product code already exists.", "Could not save", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(null, "Please fill in all required fields.");
        }
    }

    private void btn_batalActionPerformed(java.awt.event.ActionEvent evt) {
        btn_tambah.doClick();
    }

    private void btn_segarkanActionPerformed(java.awt.event.ActionEvent evt) {
        SelectKategori();
        SelectSupplier();
        SelectSatuan();
        GetData();
        showBarcodePreview("");
        btn_dapatKode.setVisible(true);
        txt_id.setText("");
        txt_cariBarang.setText("");
        jLabel9.setText("Selected: —");
    }

    private void txt_beliKeyTyped(java.awt.event.KeyEvent evt) {
        char c = evt.getKeyChar();
        if (!(Character.isDigit(c)) && !(c == KeyEvent.VK_BACK_SPACE)) {
            JOptionPane.showMessageDialog(null, "Numbers only", "Invalid input", JOptionPane.ERROR_MESSAGE);
            evt.consume();
        }
    }

    private void txt_stokKeyTyped(java.awt.event.KeyEvent evt) {
        char c = evt.getKeyChar();
        if (c == KeyEvent.VK_BACK_SPACE) {
            return;
        }
        if (Character.isDigit(c)) {
            return;
        }
        if (c == '.' && txt_stok.getText().indexOf('.') < 0) {
            return;
        }
        JOptionPane.showMessageDialog(null, "Numbers only", "Invalid input", JOptionPane.ERROR_MESSAGE);
        evt.consume();
    }


    private void cb_kategoriItemStateChanged(java.awt.event.ItemEvent evt) {
        Object sel = cb_kategori.getSelectedItem();
        if (sel == null) {
            return;
        }
        String nm_kategori = sel.toString();
        if (!nm_kategori.equals("") && !nm_kategori.equals(PLACEHOLDER_CAT)) {
            try {
                Connection conn = Koneksi.getConnection();
                java.sql.Statement stm = conn.createStatement();
                java.sql.ResultSet sql = stm.executeQuery("SELECT kategori_Id FROM kategori WHERE nama_kategori='" + nm_kategori + "'");
                if (sql.next()) {
                    txttemp_IDkategori.setText(sql.getString("kategori_Id"));
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error " + e);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Form_Barang.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            txttemp_IDkategori.setText("");
        }
    }

    private void cb_supplierItemStateChanged(java.awt.event.ItemEvent evt) {
        Object sel = cb_supplier.getSelectedItem();
        if (sel == null) {
            return;
        }
        String nm_supplier = sel.toString();
        if (!nm_supplier.equals("") && !nm_supplier.equals(PLACEHOLDER_SUP)) {
            try {
                Connection conn = Koneksi.getConnection();
                java.sql.Statement stm = conn.createStatement();
                java.sql.ResultSet sql = stm.executeQuery("SELECT supplier_Id FROM supplier WHERE nama_supplier='" + nm_supplier + "'");
                if (sql.next()) {
                    txttemp_IDsupplier.setText(sql.getString("supplier_Id"));
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error " + e);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Form_Barang.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            txttemp_IDsupplier.setText("");
        }
    }

    private void cb_satuanItemStateChanged(java.awt.event.ItemEvent evt) {
        Object sel = cb_satuan.getSelectedItem();
        if (sel == null) {
            return;
        }
        String nm_satuan = sel.toString();
        if (!nm_satuan.equals("") && !nm_satuan.equals(PLACEHOLDER_UNIT)) {
            try {
                Connection conn = Koneksi.getConnection();
                java.sql.Statement stm = conn.createStatement();
                java.sql.ResultSet sql = stm.executeQuery("SELECT satuan_Id FROM satuan WHERE nama_satuan='" + nm_satuan + "'");
                if (sql.next()) {
                    txttemp_IDsatuan.setText(sql.getString("satuan_Id"));
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error " + e);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Form_Barang.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            txttemp_IDsatuan.setText("");
        }
    }

    private void btn_dapatKodeActionPerformed(java.awt.event.ActionEvent evt) {
        txt_kode.setText(getRandomNumberString());
    }

    private void btn_cariActionPerformed(java.awt.event.ActionEvent evt) {
        cariBarang();
    }

    private void btn_cetakActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            String report = ("src/report/produk.jrxml");
            HashMap hash = new HashMap();
            hash.put("", 0);
            JasperReport JRpt = JasperCompileManager.compileReport(report);
            JasperPrint JPrint = JasperFillManager.fillReport(JRpt, hash, Koneksi.getConnection());
            JasperViewer.viewReport(JPrint, false);
        } catch (Exception e) {
            System.out.println("Unable to show report: " + e);
        }
    }

    private void btn_printLabelActionPerformed(java.awt.event.ActionEvent evt) {
        String code = txt_kode.getText().trim();
        String name = txt_nama.getText().trim();
        String price = txt_jual.getText().trim();
        Object unitSel = cb_satuan.getSelectedItem();
        String unit = (unitSel == null || PLACEHOLDER_UNIT.equals(unitSel.toString()))
                ? "" : unitSel.toString();

        if (code.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select a product first.");
            return;
        }

        String qty = JOptionPane.showInputDialog(this, "How many labels?", "1");
        if (qty == null) {
            return;
        }
        qty = qty.trim();
        if (qty.isEmpty()) {
            qty = "1";
        }

        String priceLine = unit.isEmpty()
                ? ("Rs. " + price)
                : ("Rs. " + price + " / " + unit);

        // LP 2824 Plus @ 203dpi — typical 2" x 1" label (406 x 203 dots)
        String zpl =
            "^XA\n"
            + "^PW406\n"
            + "^LL203\n"
            + "^LH0,0\n"
            + "^FO16,16^A0N,24,24^FD" + zplSafe(name) + "^FS\n"
            + "^FO16,48^BY2^BCN,72,Y,N,N^FD" + zplSafe(code) + "^FS\n"
            + "^FO16,150^A0N,22,22^FD" + zplSafe(priceLine) + "^FS\n"
            + "^PQ" + zplSafe(qty) + "\n"
            + "^XZ\n";

        try {
            PrintService target = null;
            for (PrintService p : PrintServiceLookup.lookupPrintServices(null, null)) {
                if (p.getName() != null && p.getName().contains("LP 2824")) {
                    target = p;
                    break;
                }
            }
            if (target == null) {
                JOptionPane.showMessageDialog(this, "Zebra printer not found.");
                return;
            }
            DocPrintJob job = target.createPrintJob();
            job.print(new SimpleDoc(zpl.getBytes("US-ASCII"),
                    DocFlavor.BYTE_ARRAY.AUTOSENSE, null), null);
            JOptionPane.showMessageDialog(this, "Label sent to " + target.getName());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Print failed: " + e.getMessage());
        }
    }

    /** Strip ZPL control chars from user-entered fields. */
    private String zplSafe(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("^", " ").replace("~", " ").replace("\r", " ").replace("\n", " ");
    }

    private void btn_exportExcelActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export products to Excel");
        fc.setSelectedFile(new java.io.File("products.xlsx"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Excel workbook (*.xlsx)", "xlsx"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        java.io.File file = fc.getSelectedFile();
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            file = new java.io.File(file.getAbsolutePath() + ".xlsx");
        }
        try {
            ProductExcel.exportToFile(file);
            JOptionPane.showMessageDialog(this,
                    "Exported products to:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Export failed: " + ex.getMessage(),
                    "Export Excel",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void btn_importExcelActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Import products from Excel");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Excel workbook (*.xlsx, *.xls)", "xlsx", "xls"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            String summary = ProductExcel.importFromFile(fc.getSelectedFile());
            SelectKategori();
            SelectSupplier();
            SelectSatuan();
            GetData();
            JOptionPane.showMessageDialog(this, summary, "Import Excel",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Import failed: " + ex.getMessage(),
                    "Import Excel",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void btn_deleteAllActionPerformed(java.awt.event.ActionEvent evt) {
        int count = 0;
        try {
            Connection conn = Koneksi.getConnection();
            java.sql.Statement stm = conn.createStatement();
            java.sql.ResultSet rs = stm.executeQuery("SELECT COUNT(*) FROM produk");
            if (rs.next()) {
                count = rs.getInt(1);
            }
            rs.close();
            stm.close();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not count products: " + ex.getMessage(),
                    "Delete all",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (count == 0) {
            JOptionPane.showMessageDialog(this, "No products to delete.");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "Delete ALL " + count + " products?\n\n"
                + "Purchase and sale line items for those products will also be removed.\n"
                + "This cannot be undone.",
                "Delete all products",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        Connection conn = null;
        try {
            conn = Koneksi.getConnection();
            conn.setAutoCommit(false);
            java.sql.Statement stm = conn.createStatement();
            stm.executeUpdate("DELETE FROM detail_penjualan");
            stm.executeUpdate("DELETE FROM detail_pembelian");
            int deleted = stm.executeUpdate("DELETE FROM produk");
            conn.commit();
            JOptionPane.showMessageDialog(this, "Deleted " + deleted + " products.");
            btn_tambah.doClick();
            GetData();
        } catch (Exception ex) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignore) {
                }
            }
            JOptionPane.showMessageDialog(this,
                    "Delete failed: " + ex.getMessage(),
                    "Delete all",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel barcode;
    private javax.swing.JButton btn_batal;
    private javax.swing.JButton btn_cari;
    private javax.swing.JButton btn_cetak;
    private javax.swing.JButton btn_dapatKode;
    private javax.swing.JButton btn_printLabel;
    private javax.swing.JButton btn_importExcel;
    private javax.swing.JButton btn_exportExcel;
    private javax.swing.JButton btn_deleteAll;
    private javax.swing.JButton btn_hapus;
    private javax.swing.JButton btn_segarkan;
    private javax.swing.JButton btn_simpan;
    private javax.swing.JButton btn_tambah;
    private javax.swing.JComboBox<String> cb_kategori;
    private javax.swing.JComboBox<String> cb_merek;
    private javax.swing.JComboBox<String> cb_satuan;
    private javax.swing.JComboBox<String> cb_supplier;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblcount_rows;
    private javax.swing.JTable tbl_supplier;
    private javax.swing.JTextField txt_beli;
    private javax.swing.JTextField txt_cariBarang;
    private javax.swing.JTextField txt_id;
    private javax.swing.JTextField txt_jual;
    private javax.swing.JTextField txt_kode;
    private javax.swing.JTextField txt_nama;
    private javax.swing.JTextField txt_stok;
    private javax.swing.JTextField txttemp_IDkategori;
    private javax.swing.JTextField txttemp_IDmerek;
    private javax.swing.JTextField txttemp_IDsatuan;
    private javax.swing.JTextField txttemp_IDsupplier;
    private javax.swing.JTextField txttemp_kode;
    // End of variables declaration//GEN-END:variables

    private String getRandomNumberString() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        sb.append("123456789".charAt(random.nextInt(9)));
        for (int i = 0; i < 8; i++) {
            sb.append("0123456789".charAt(random.nextInt(10)));
        }
        return sb.toString();
    }

    private void generate(String row_txtkode) throws Exception {
        Linear barcode = new Linear();
        barcode.setType(Linear.CODE128B);
        barcode.setData(row_txtkode);
        barcode.setI(11.0f);
        String fname = row_txtkode;
        barcode.renderBarcode("src" + "/" + "img" + "/barcode/" + fname + ".png");
        showBarcodePreview(row_txtkode);
    }

    /** Scale barcode PNG to fit the edit sidebar (preview only — labels use ZPL). */
    private void showBarcodePreview(String code) {
        if (code == null || code.trim().isEmpty()) {
            barcode.setIcon(null);
            barcode.setText("No product code");
            barcode.setForeground(UITheme.TEXT_MUTED);
            return;
        }
        java.io.File png = new java.io.File("src" + "/" + "img" + "/barcode/" + code.trim() + ".png");
        if (!png.isFile()) {
            barcode.setIcon(null);
            barcode.setText("No barcode image yet — save the product to generate a preview");
            barcode.setForeground(UITheme.TEXT_MUTED);
            return;
        }
        ImageIcon raw = new ImageIcon(png.getPath());
        if (raw.getIconWidth() <= 0 || raw.getIconHeight() <= 0) {
            barcode.setIcon(null);
            barcode.setText("Barcode image is unreadable — try saving the product again");
            barcode.setForeground(UITheme.TEXT_MUTED);
            return;
        }
        barcode.setText(null);
        barcode.setForeground(PageUI.INK);
        final int maxW = 300;
        final int maxH = 70;
        int w = raw.getIconWidth();
        int h = raw.getIconHeight();
        double scale = Math.min((double) maxW / w, (double) maxH / h);
        if (scale >= 1.0) {
            barcode.setIcon(raw);
        } else {
            int nw = Math.max(1, (int) Math.round(w * scale));
            int nh = Math.max(1, (int) Math.round(h * scale));
            Image scaled = raw.getImage().getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
            barcode.setIcon(new ImageIcon(scaled));
        }
    }

    private void Mouseklik() {
        if (tbl_supplier.getSelectedRow() < 0) {
            return;
        }
        String row_id = txt_id.getText();
        if (row_id == null || row_id.isEmpty() || "0".equals(row_id)) {
            return;
        }
        try {
            btn_simpan.setText("Save product");
            Connection conn = Koneksi.getConnection();
            java.sql.Statement stm = conn.createStatement();
            java.sql.ResultSet sql = stm.executeQuery(
                    "SELECT p.kode_produk, p.nama_produk, p.harga_beli, p.harga_jual, p.stok_produk, "
                    + "p.satuan_Id, s.supplier_Id, s.nama_supplier, k.kategori_Id, k.nama_kategori, "
                    + "u.nama_satuan "
                    + "FROM produk p "
                    + "LEFT JOIN supplier s ON p.supplier_Id = s.supplier_Id "
                    + "LEFT JOIN kategori k ON p.kategori_Id = k.kategori_Id "
                    + "LEFT JOIN satuan u ON p.satuan_Id = u.satuan_Id "
                    + "WHERE p.kode_produk='" + row_id + "'");
            if (sql.next()) {
                String kode = sql.getString("kode_produk");
                txt_id.setText(sql.getString("kode_produk"));
                txt_kode.setText(kode);
                txt_nama.setText(sql.getString("nama_produk"));
                txt_beli.setText(sql.getString("harga_beli"));
                txt_jual.setText(sql.getString("harga_jual"));
                txt_stok.setText(sql.getString("stok_produk"));
                String cat = sql.getString("nama_kategori");
                String sup = sql.getString("nama_supplier");
                String unit = sql.getString("nama_satuan");
                cb_kategori.setSelectedItem(cat != null ? cat : PLACEHOLDER_CAT);
                txttemp_IDkategori.setText(sql.getString("kategori_Id") != null ? sql.getString("kategori_Id") : "");
                cb_supplier.setSelectedItem(sup != null ? sup : PLACEHOLDER_SUP);
                txttemp_IDsupplier.setText(sql.getString("supplier_Id") != null ? sql.getString("supplier_Id") : "");
                cb_satuan.setSelectedItem(unit != null ? unit : PLACEHOLDER_UNIT);
                txttemp_IDsatuan.setText(sql.getString("satuan_Id") != null ? sql.getString("satuan_Id") : "");
                txttemp_kode.setText(kode);
                showBarcodePreview(txt_kode.getText());
                txt_kode.requestFocus();
                updateMargin();
            }
            txt_kode.setEditable(false);
            btn_dapatKode.setVisible(false);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error " + e);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Form_Barang.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void cariBarang() {
        try {
            Connection conn = Koneksi.getConnection();
            java.sql.Statement stm = conn.createStatement();
            String q = txt_cariBarang.getText();
            java.sql.ResultSet sql = stm.executeQuery(
                    "SELECT kode_produk AS CODE, nama_produk AS PRODUCT, "
                    + "nama_kategori AS CATEGORY, "
                    + "harga_beli AS BUY, harga_jual AS SELL, stok_produk AS STOCK FROM tableproduk"
                    + " WHERE tableproduk.kode_produk LIKE '%" + q
                    + "%' || tableproduk.nama_produk LIKE '%" + q
                    + "%' || tableproduk.nama_kategori LIKE '%" + q
                    + "%' || tableproduk.harga_beli LIKE '%" + q
                    + "%' || tableproduk.harga_jual LIKE '%" + q
                    + "%' || tableproduk.stok_produk LIKE '%" + q
                    + "%' || tableproduk.nama_supplier LIKE '%" + q + "%'");
            applyListModel(sql);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error " + e);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Form_User.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
