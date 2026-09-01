/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Master;

import Main.PageUI;
import Main.UITheme;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/**
 * Categories master — list + edit-sidebar layout matching the reference.
 */
public class Form_Kategori extends javax.swing.JPanel {

    private JLabel lb_status;
    private JLabel lb_endResults;
    private JLabel lb_selected;
    private JLabel lb_editTitle;
    private JLabel lb_statProducts;
    private JLabel lb_statUnits;
    private JLabel lb_statSold;
    private static final Color PANEL_BG = new Color(0xF3F3F1);
    private static final Color RULE = new Color(0xD0D0CC);

    public Form_Kategori() {
        initComponents();
        txttemp_kode.setVisible(false);
        txt_id.setVisible(false);
        GetData();
        BtnEnabled(false);
        btn_simpan.setText("Save category");
        txt_id.setEditable(false);
        clearStats();
    }

    private void TxtEmpty() {
        txt_id.setText("");
        txt_nama.setText("");
        txt_rak.setText("");
        txttemp_kode.setText("");
        if (lb_selected != null) {
            lb_selected.setText("Selected: —");
        }
        if (lb_editTitle != null) {
            lb_editTitle.setText("NEW CATEGORY");
        }
        clearStats();
    }

    private void BtnEnabled(boolean x) {
        btn_edit.setEnabled(x);
        btn_hapus.setEnabled(x);
    }

    private void clearStats() {
        setStat(lb_statProducts, "0");
        setStat(lb_statUnits, "0");
        setStat(lb_statSold, "0");
    }

    private void setStat(JLabel label, String value) {
        if (label != null) {
            label.setText(value);
        }
    }

    private void refreshStatus(int rows) {
        int products = countAllProducts();
        if (lb_status != null) {
            String unit = rows == 1 ? "category" : "categories";
            String pUnit = products == 1 ? "product" : "products";
            lb_status.setText(rows + " " + unit + " · " + products + " " + pUnit + " classified");
        }
        if (lblcount_rows != null) {
            lblcount_rows.setText("Rows: " + rows);
        }
        if (lb_endResults != null) {
            if (rows == 0) {
                lb_endResults.setText("No categories yet. Use + New to add a category.");
            } else {
                lb_endResults.setText("End of list — " + rows + " of " + rows + " categories.");
            }
        }
    }

    private int countAllProducts() {
        try {
            Connection conn = Koneksi.getConnection();
            java.sql.Statement stm = conn.createStatement();
            java.sql.ResultSet rs = stm.executeQuery("SELECT COUNT(*) FROM produk");
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private void applyTableModel(java.sql.ResultSet sql) throws SQLException {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"ID", "CATEGORY", "SHELF", "PRODUCTS"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        while (sql.next()) {
            model.addRow(new Object[]{
                sql.getString(1),
                sql.getString(2),
                sql.getString(3),
                sql.getString(4)
            });
        }
        tbl_kategori.setModel(model);
        if (tbl_kategori.getColumnCount() >= 4) {
            tbl_kategori.getColumnModel().getColumn(0).setPreferredWidth(50);
            tbl_kategori.getColumnModel().getColumn(1).setPreferredWidth(220);
            tbl_kategori.getColumnModel().getColumn(2).setPreferredWidth(120);
            tbl_kategori.getColumnModel().getColumn(3).setPreferredWidth(80);
        }
        PageUI.styleTable(tbl_kategori);
        refreshStatus(model.getRowCount());
        if (lb_selected != null) {
            lb_selected.setText("Selected: —");
        }
    }

    private void GetData() {
        try {
            Connection conn = Koneksi.getConnection();
            java.sql.Statement stm = conn.createStatement();
            java.sql.ResultSet sql = stm.executeQuery(
                    "SELECT k.kategori_Id, k.nama_kategori, k.no_rak, "
                    + "COUNT(p.kode_produk) "
                    + "FROM kategori k "
                    + "LEFT JOIN produk p ON p.kategori_Id = k.kategori_Id "
                    + "GROUP BY k.kategori_Id, k.nama_kategori, k.no_rak "
                    + "ORDER BY k.kategori_Id");
            applyTableModel(sql);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error " + e);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Form_User.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void GetData_View() {
        int row = tbl_kategori.getSelectedRow();
        if (row < 0) {
            return;
        }
        String row_id = tbl_kategori.getModel().getValueAt(row, 0).toString();
        String name = tbl_kategori.getModel().getValueAt(row, 1).toString();
        txt_id.setText(row_id);
        BtnEnabled(true);
        if (lb_selected != null) {
            lb_selected.setText("Selected: " + name);
        }
        if (lb_editTitle != null) {
            lb_editTitle.setText("EDIT CATEGORY");
        }
        loadCategory(row_id);
    }

    private void loadCategory(String row_id) {
        try {
            Connection conn = Koneksi.getConnection();
            java.sql.Statement stm = conn.createStatement();
            java.sql.ResultSet sql = stm.executeQuery(
                    "SELECT * FROM kategori WHERE kategori_Id='" + row_id + "'");
            if (sql.next()) {
                String kode = sql.getString("kategori_Id");
                txt_id.setText(kode);
                txt_nama.setText(sql.getString("nama_kategori"));
                txt_rak.setText(sql.getString("no_rak"));
                txttemp_kode.setText(kode);
                btn_simpan.setText("Save category");
                loadStats(kode);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error " + e);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Form_User.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loadStats(String kategoriId) {
        int products = 0;
        int units = 0;
        int sold = 0;
        try {
            Connection conn = Koneksi.getConnection();
            java.sql.Statement stm = conn.createStatement();
            java.sql.ResultSet rs = stm.executeQuery(
                    "SELECT COUNT(*), COALESCE(SUM(stok_produk),0) FROM produk WHERE kategori_Id='" + kategoriId + "'");
            if (rs.next()) {
                products = rs.getInt(1);
                units = rs.getInt(2);
            }
            rs = stm.executeQuery(
                    "SELECT COALESCE(SUM(dp.jumlah),0) "
                    + "FROM detail_penjualan dp "
                    + "JOIN produk p ON p.kode_produk = dp.kode_produk "
                    + "JOIN penjualan j ON j.penjualan_Id = dp.penjualan_Id "
                    + "WHERE p.kategori_Id='" + kategoriId + "' "
                    + "AND j.voided = 0 "
                    + "AND j.tanggal_penjualan >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)");
            if (rs.next()) {
                sold = rs.getInt(1);
            }
        } catch (Exception ignored) {
        }
        setStat(lb_statProducts, Integer.toString(products));
        setStat(lb_statUnits, Integer.toString(units));
        setStat(lb_statSold, Integer.toString(sold));
    }

    private int productCountForCategory(String kategoriId) {
        try {
            Connection conn = Koneksi.getConnection();
            java.sql.Statement stm = conn.createStatement();
            java.sql.ResultSet rs = stm.executeQuery(
                    "SELECT COUNT(*) FROM produk WHERE kategori_Id='" + kategoriId + "'");
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private void cariKategori() {
        try {
            String q = txt_cariCategory.getText().trim().replace("'", "''");
            Connection conn = Koneksi.getConnection();
            java.sql.Statement stm = conn.createStatement();
            java.sql.ResultSet sql = stm.executeQuery(
                    "SELECT k.kategori_Id, k.nama_kategori, k.no_rak, "
                    + "COUNT(p.kode_produk) "
                    + "FROM kategori k "
                    + "LEFT JOIN produk p ON p.kategori_Id = k.kategori_Id "
                    + "WHERE k.kategori_Id LIKE '%" + q + "%' "
                    + "OR k.nama_kategori LIKE '%" + q + "%' "
                    + "OR k.no_rak LIKE '%" + q + "%' "
                    + "GROUP BY k.kategori_Id, k.nama_kategori, k.no_rak "
                    + "ORDER BY k.kategori_Id");
            applyTableModel(sql);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error " + e);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Form_User.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private JPanel fieldBlock(String caption, JTextField field) {
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

    private JPanel statRow(String label, JLabel value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, RULE),
                BorderFactory.createEmptyBorder(8, 0, 8, 0)));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lb = new JLabel(label);
        lb.setFont(UITheme.FONT_REGULAR.deriveFont(13f));
        lb.setForeground(UITheme.TEXT_SECONDARY);
        value.setFont(UITheme.FONT_BOLD.deriveFont(13f));
        value.setForeground(PageUI.INK);
        value.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(lb, BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        return row;
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

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        jPanel1 = new JPanel();
        jPanel2 = new JPanel();
        jPanel3 = new JPanel();
        jPanel4 = new JPanel();
        jLabel1 = new JLabel();
        jLabel3 = new JLabel();
        jLabel4 = new JLabel();
        jLabel9 = new JLabel();
        lblcount_rows = new JLabel();
        txt_nama = new JTextField();
        txt_rak = new JTextField();
        txt_cariCategory = new JTextField();
        txttemp_kode = new JTextField();
        txt_id = new JTextField();
        btn_tambah = new JButton();
        btn_hapus = new JButton();
        btn_edit = new JButton();
        btn_simpan = new JButton();
        btn_batal = new JButton();
        btn_cari = new JButton();
        jButton1 = new JButton();
        jScrollPane1 = new JScrollPane();
        jScrollPane2 = new JScrollPane();
        tbl_kategori = new JTable();

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

        JLabel badge = new JLabel("  MASTER / 03  ");
        badge.setOpaque(true);
        badge.setBackground(PageUI.INK);
        badge.setForeground(Color.WHITE);
        badge.setFont(UITheme.FONT_BOLD.deriveFont(11f));
        badge.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 0));
        jLabel1.setText("Categories");
        jLabel1.setFont(UITheme.FONT_HEADING.deriveFont(22f));
        jLabel1.setForeground(PageUI.INK);
        jLabel1.setAlignmentX(Component.LEFT_ALIGNMENT);
        lb_status = new JLabel("0 categories · 0 products classified");
        lb_status.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        lb_status.setForeground(UITheme.TEXT_MUTED);
        lb_status.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleWrap.add(jLabel1);
        titleWrap.add(lb_status);

        headerLeft.add(badge);
        headerLeft.add(titleWrap);
        header.add(headerLeft, BorderLayout.WEST);
        jPanel3.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(UITheme.PAGE_BG);

        // LEFT list
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(UITheme.PAGE_BG);
        left.setBorder(BorderFactory.createEmptyBorder(14, 16, 12, 12));

        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        PageUI.styleField(txt_cariCategory);
        txt_cariCategory.putClientProperty("JTextField.placeholderText", "Search category or shelf number");
        txt_cariCategory.addActionListener(e -> cariKategori());
        btn_cari.setText("Search");
        PageUI.stylePrimaryButton(btn_cari);
        btn_cari.addActionListener(e -> btn_cariActionPerformed(e));
        jButton1.setText("Refresh");
        PageUI.styleGhostButton(jButton1);
        jButton1.setForeground(PageUI.INK);
        jButton1.addActionListener(e -> jButton1ActionPerformed(e));
        JPanel searchBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchBtns.setOpaque(false);
        searchBtns.add(btn_cari);
        searchBtns.add(jButton1);
        searchRow.add(txt_cariCategory, BorderLayout.CENTER);
        searchRow.add(searchBtns, BorderLayout.EAST);

        tbl_kategori.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"ID", "CATEGORY", "SHELF", "PRODUCTS"}));
        tbl_kategori.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tbl_kategoriMouseClicked(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tbl_kategoriMouseReleased(evt);
            }
        });
        tbl_kategori.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tbl_kategoriKeyReleased(evt);
            }
        });
        PageUI.styleTable(tbl_kategori);
        jScrollPane1.setViewportView(tbl_kategori);
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
        lb_selected = new JLabel("Selected: —");
        lb_selected.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lb_selected.setForeground(UITheme.TEXT_MUTED);
        jLabel9.setText("");
        footer.add(lblcount_rows, BorderLayout.WEST);
        footer.add(lb_selected, BorderLayout.EAST);

        JPanel leftCenter = new JPanel(new BorderLayout());
        leftCenter.setOpaque(false);
        leftCenter.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        leftCenter.add(jScrollPane1, BorderLayout.CENTER);
        leftCenter.add(lb_endResults, BorderLayout.SOUTH);

        left.add(searchRow, BorderLayout.NORTH);
        left.add(leftCenter, BorderLayout.CENTER);
        left.add(footer, BorderLayout.SOUTH);

        // RIGHT sidebar
        jPanel4.setBackground(PANEL_BG);
        jPanel4.setPreferredSize(new Dimension(320, 10));
        jPanel4.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, RULE));
        jPanel4.setLayout(new BorderLayout());

        JPanel editHead = new JPanel(new BorderLayout());
        editHead.setOpaque(false);
        editHead.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, RULE),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        lb_editTitle = new JLabel("EDIT CATEGORY");
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

        PageUI.styleField(txt_nama);
        PageUI.styleField(txt_rak);
        txt_nama.putClientProperty("JTextField.placeholderText", "e.g. Fruit");
        txt_rak.putClientProperty("JTextField.placeholderText", "e.g. A-01");

        jLabel3.setVisible(false);
        jLabel4.setVisible(false);
        btn_edit.setVisible(false);

        form.add(fieldBlock("CATEGORY NAME", txt_nama));
        form.add(Box.createVerticalStrut(12));
        form.add(fieldBlock("SHELF", txt_rak));
        form.add(Box.createVerticalStrut(18));

        lb_statProducts = new JLabel("0");
        lb_statUnits = new JLabel("0");
        lb_statSold = new JLabel("0");
        form.add(statRow("Products in this category", lb_statProducts));
        form.add(statRow("Units in stock", lb_statUnits));
        form.add(statRow("Sold this week", lb_statSold));
        form.add(Box.createVerticalStrut(14));

        JLabel lbWarn = new JLabel("<html>Deleting is blocked while products still "
                + "reference this category — reassign them first.</html>");
        lbWarn.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lbWarn.setForeground(UITheme.TEXT_MUTED);
        lbWarn.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lbWarn);
        form.add(Box.createVerticalGlue());

        JPanel actions = new JPanel();
        actions.setBackground(UITheme.ACCENT);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));

        btn_simpan.setText("Save category");
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
        btn_batal = outlineBtn("Cancel");
        btn_batal.addActionListener(e -> btn_batalActionPerformed(e));
        btn_hapus = outlineBtn("Delete");
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

        jScrollPane2.setVisible(false);
        jPanel1.setOpaque(false);
        jPanel1.setLayout(new BorderLayout());
        jPanel2.setOpaque(false);
        jPanel2.setLayout(new BorderLayout());
        jPanel2.add(jPanel3, BorderLayout.CENTER);
        jPanel1.add(jPanel2, BorderLayout.CENTER);
        add(jPanel1, BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void btn_tambahActionPerformed(java.awt.event.ActionEvent evt) {
        tbl_kategori.clearSelection();
        TxtEmpty();
        BtnEnabled(false);
        btn_simpan.setText("Save category");
        txt_nama.requestFocus();
    }

    private void btn_hapusActionPerformed(java.awt.event.ActionEvent evt) {
        String row_id = txt_id.getText();
        if (row_id == null || row_id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select a category first.");
            return;
        }
        int linked = productCountForCategory(row_id);
        if (linked > 0) {
            JOptionPane.showMessageDialog(this,
                    "Deleting is blocked while " + linked + " product"
                    + (linked == 1 ? "" : "s") + " still reference this category — reassign them first.");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(null, "Delete this category?", "Confirm", JOptionPane.OK_CANCEL_OPTION);
        if (ok == 0) {
            try {
                Connection conn = Koneksi.getConnection();
                java.sql.Statement stm = conn.createStatement();
                stm.executeUpdate("DELETE FROM kategori WHERE kategori_Id = '" + row_id + "'");
                JOptionPane.showMessageDialog(null, "Category deleted.");
                btn_tambah.doClick();
                GetData();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "This category is used by products.");
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Form_User.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void btn_editActionPerformed(java.awt.event.ActionEvent evt) {
        String row_id = txt_id.getText();
        if (row_id != null && !row_id.isEmpty()) {
            loadCategory(row_id);
            txt_nama.requestFocus();
        } else {
            JOptionPane.showMessageDialog(null, "Select a category first.");
        }
    }

    private void tbl_kategoriMouseClicked(java.awt.event.MouseEvent evt) {
        GetData_View();
    }

    private void tbl_kategoriMouseReleased(java.awt.event.MouseEvent evt) {
        GetData_View();
    }

    private void tbl_kategoriKeyReleased(java.awt.event.KeyEvent evt) {
        GetData_View();
    }

    private void btn_simpanActionPerformed(java.awt.event.ActionEvent evt) {
        String row_id = txt_id.getText();
        String row_txtnama = txt_nama.getText().trim();
        String row_txtrak = txt_rak.getText().trim();
        int c_kode = 0;

        if (!"".equals(row_txtnama) && !"".equals(row_txtrak)) {
            try {
                Connection conn = Koneksi.getConnection();
                java.sql.Statement stm = conn.createStatement();
                String countSql = "".equals(row_id)
                        ? "SELECT COUNT(kategori_Id) as count FROM kategori WHERE nama_kategori='" + row_txtnama.replace("'", "''") + "'"
                        : "SELECT COUNT(kategori_Id) as count FROM kategori WHERE nama_kategori='" + row_txtnama.replace("'", "''")
                            + "' AND kategori_Id <> '" + row_id + "'";
                java.sql.ResultSet sql = stm.executeQuery(countSql);
                sql.next();
                c_kode = sql.getInt("count");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error " + e);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Form_User.class.getName()).log(Level.SEVERE, null, ex);
            }

            if ("".equals(row_id)) {
                if (c_kode == 0) {
                    try {
                        Connection conn = Koneksi.getConnection();
                        PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO kategori(nama_kategori, no_rak, uuid) VALUES (?,?,?)",
                                java.sql.Statement.RETURN_GENERATED_KEYS);
                        ps.setString(1, row_txtnama);
                        ps.setString(2, row_txtrak);
                        ps.setString(3, Ids.newUuid());
                        ps.executeUpdate();
                        java.sql.ResultSet keys = ps.getGeneratedKeys();
                        if (keys.next()) {
                            SyncOutbox.enqueueCategoryById(keys.getInt(1));
                        }
                        keys.close();
                        ps.close();
                        JOptionPane.showMessageDialog(null, "Category saved.");
                        btn_tambah.doClick();
                        GetData();
                    } catch (SQLException e) {
                        JOptionPane.showMessageDialog(null, "Error " + e);
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(Form_User.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "That name is already used.", "Could not save", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                if (c_kode == 0) {
                    try {
                        Connection conn = Koneksi.getConnection();
                        java.sql.Statement stm = conn.createStatement();
                        stm.executeUpdate("UPDATE kategori SET nama_kategori ='" + row_txtnama.replace("'", "''")
                                + "',no_rak= '" + row_txtrak.replace("'", "''")
                                + "' WHERE kategori_Id = '" + row_id + "'");
                        try {
                            SyncOutbox.enqueueCategoryById(Integer.parseInt(row_id));
                        } catch (Exception ignored) {
                        }
                        JOptionPane.showMessageDialog(null, "Category updated.");
                        btn_tambah.doClick();
                        GetData();
                    } catch (SQLException e) {
                        JOptionPane.showMessageDialog(null, "Error " + e);
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(Form_User.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "That name is already used.", "Could not save", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(null, "Please fill in all fields.");
        }
    }

    private void btn_batalActionPerformed(java.awt.event.ActionEvent evt) {
        btn_tambah.doClick();
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        GetData();
        TxtEmpty();
        BtnEnabled(false);
    }

    private void btn_cariActionPerformed(java.awt.event.ActionEvent evt) {
        cariKategori();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_batal;
    private javax.swing.JButton btn_cari;
    private javax.swing.JButton btn_edit;
    private javax.swing.JButton btn_hapus;
    private javax.swing.JButton btn_simpan;
    private javax.swing.JButton btn_tambah;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblcount_rows;
    private javax.swing.JTable tbl_kategori;
    private javax.swing.JTextField txt_cariCategory;
    private javax.swing.JTextField txt_id;
    private javax.swing.JTextField txt_nama;
    private javax.swing.JTextField txt_rak;
    private javax.swing.JTextField txttemp_kode;
    // End of variables declaration//GEN-END:variables
}
