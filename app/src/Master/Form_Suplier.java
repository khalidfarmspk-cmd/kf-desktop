/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Master;

import config.Ids;
import config.Koneksi;
import config.SyncOutbox;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
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
 * Suppliers master — editorial grid layout matching Categories.
 */
public class Form_Suplier extends javax.swing.JPanel {

    private JLabel lb_status;
    private JLabel lb_endResults;
    private JLabel lb_selected;

    public Form_Suplier() {
        initComponents();
        txttemp_kode.setVisible(false);
        txt_id.setVisible(false);
        GetData();
        BtnEnabled(false);
        btn_simpan.setText("Save supplier");
        txt_id.setEditable(false);
    }

    private void TxtEmpty() {
        txt_id.setText("");
        txt_nama.setText("");
        txt_alamat.setText("");
        txt_telp.setText("");
        txttemp_kode.setText("");
        updateSelectedLabel(0);
    }

    private void BtnEnabled(boolean x) {
        btn_edit.setEnabled(x);
        btn_hapus.setEnabled(x);
    }

    private void updateSelectedLabel(int n) {
        if (lb_selected != null) {
            lb_selected.setText(n <= 0 ? "Selected: —" : "Selected: " + n + " row");
        }
    }

    private void refreshStatus(int rows) {
        if (lb_status != null) {
            String unit = rows == 1 ? "supplier" : "suppliers";
            lb_status.setText(rows + " " + unit + " on file");
        }
        if (lblcount_rows != null) {
            lblcount_rows.setText("Rows: " + rows);
        }
        if (lb_endResults != null) {
            if (rows == 0) {
                lb_endResults.setText("No suppliers yet. Use New to add a supplier.");
            } else {
                lb_endResults.setText("End of results — " + rows + " of " + rows
                        + " row" + (rows == 1 ? "" : "s") + " shown. Use New to add a supplier.");
            }
        }
    }

    private void applyTableModel(java.sql.ResultSet sql) throws SQLException {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"ID", "SUPPLIER", "ADDRESS", "PHONE"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        while (sql.next()) {
            model.addRow(new Object[]{
                sql.getString("supplier_Id"),
                sql.getString("nama_supplier"),
                sql.getString("alamat_supplier"),
                sql.getString("telp_supplier")
            });
        }
        tbl_supplier.setModel(model);
        if (tbl_supplier.getColumnCount() >= 4) {
            tbl_supplier.getColumnModel().getColumn(0).setPreferredWidth(50);
            tbl_supplier.getColumnModel().getColumn(1).setPreferredWidth(180);
            tbl_supplier.getColumnModel().getColumn(2).setPreferredWidth(280);
            tbl_supplier.getColumnModel().getColumn(3).setPreferredWidth(120);
        }
        Main.PageUI.styleTable(tbl_supplier);
        refreshStatus(model.getRowCount());
        updateSelectedLabel(0);
    }

    private void GetData() {
        try {
            Connection conn = Koneksi.getConnection();
            java.sql.Statement stm = conn.createStatement();
            java.sql.ResultSet sql = stm.executeQuery(
                    "SELECT supplier_Id, nama_supplier, alamat_supplier, telp_supplier "
                    + "FROM supplier ORDER BY supplier_Id");
            applyTableModel(sql);
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
        txt_id.setText(row_id);
        BtnEnabled(true);
        updateSelectedLabel(1);
    }

    private void cariSupplier() {
        try {
            String q = txt_cariSupplier.getText().trim().replace("'", "''");
            Connection conn = Koneksi.getConnection();
            java.sql.Statement stm = conn.createStatement();
            java.sql.ResultSet sql = stm.executeQuery(
                    "SELECT supplier_Id, nama_supplier, alamat_supplier, telp_supplier "
                    + "FROM supplier "
                    + "WHERE supplier_Id LIKE '%" + q + "%' "
                    + "OR nama_supplier LIKE '%" + q + "%' "
                    + "OR alamat_supplier LIKE '%" + q + "%' "
                    + "OR telp_supplier LIKE '%" + q + "%' "
                    + "ORDER BY supplier_Id");
            applyTableModel(sql);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error " + e);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Form_User.class.getName()).log(Level.SEVERE, null, ex);
        }
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
        jLabel5 = new JLabel();
        jLabel9 = new JLabel();
        lblcount_rows = new JLabel();
        txt_nama = new JTextField();
        txt_alamat = new JTextField();
        txt_telp = new JTextField();
        txt_cariSupplier = new JTextField();
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
        tbl_supplier = new JTable();

        setLayout(new BorderLayout());
        Main.PageUI.paintPage(this);

        jPanel3.setBackground(Main.UITheme.PAGE_BG);
        jPanel3.setBorder(Main.PageUI.pagePadding());
        jPanel3.setLayout(new BoxLayout(jPanel3, BoxLayout.Y_AXIS));

        // ---- Header ----
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        JPanel headerLeft = new JPanel();
        headerLeft.setOpaque(false);
        headerLeft.setLayout(new BoxLayout(headerLeft, BoxLayout.Y_AXIS));

        JLabel crumb = new JLabel("MASTER DATA / 05");
        crumb.setFont(Main.UITheme.FONT_CAPTION);
        crumb.setForeground(Main.UITheme.TEXT_CAPTION);
        crumb.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerLeft.add(crumb);

        jLabel1.setText("Suppliers");
        jLabel1.setFont(Main.UITheme.FONT_HEADING.deriveFont(28f));
        jLabel1.setForeground(Main.PageUI.INK);
        jLabel1.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerLeft.add(jLabel1);

        lb_status = new JLabel("0 suppliers on file");
        lb_status.setFont(Main.UITheme.FONT_REGULAR.deriveFont(11f));
        lb_status.setForeground(Main.UITheme.TEXT_MUTED);
        lb_status.setHorizontalAlignment(SwingConstants.RIGHT);
        lb_status.setVerticalAlignment(SwingConstants.BOTTOM);

        header.add(headerLeft, BorderLayout.WEST);
        header.add(lb_status, BorderLayout.EAST);
        jPanel3.add(header);
        jPanel3.add(Box.createVerticalStrut(12));

        // ---- Details | Note ----
        JPanel detailsShell = new JPanel(new GridLayout(1, 2, 0, 0));
        detailsShell.setBackground(Main.UITheme.SURFACE);
        detailsShell.setBorder(BorderFactory.createLineBorder(Main.UITheme.GRID_LINE, 1));
        detailsShell.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsShell.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        detailsShell.setPreferredSize(new Dimension(10, 180));

        jPanel4.setBackground(Main.UITheme.SURFACE);
        jPanel4.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Main.UITheme.GRID_LINE),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        jPanel4.setLayout(new GridBagLayout());

        JLabel lb_details = new JLabel("SUPPLIER DETAILS");
        Main.PageUI.styleCaption(lb_details);

        jLabel3.setText("Supplier name");
        Main.PageUI.styleLabel(jLabel3);
        jLabel5.setText("Address");
        Main.PageUI.styleLabel(jLabel5);
        jLabel4.setText("Phone");
        Main.PageUI.styleLabel(jLabel4);

        Main.PageUI.styleField(txt_nama);
        txt_nama.putClientProperty("JTextField.placeholderText", "Company or person");
        txt_nama.addActionListener(evt -> txt_namaActionPerformed(evt));

        Main.PageUI.styleField(txt_alamat);
        txt_alamat.putClientProperty("JTextField.placeholderText", "Street, city");
        txt_alamat.addActionListener(evt -> txt_alamatActionPerformed(evt));

        Main.PageUI.styleField(txt_telp);
        txt_telp.putClientProperty("JTextField.placeholderText", "+62 ...");
        txt_telp.addActionListener(evt -> txt_telpActionPerformed(evt));
        txt_telp.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txt_telpKeyTyped(evt);
            }
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 12, 0);
        gbc.weightx = 1;
        jPanel4.add(lb_details, gbc);

        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 10, 14);
        jPanel4.add(jLabel3, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        jPanel4.add(txt_nama, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 10, 14);
        jPanel4.add(jLabel5, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        jPanel4.add(txt_alamat, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 0, 14);
        jPanel4.add(jLabel4, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        jPanel4.add(txt_telp, gbc);

        detailsShell.add(jPanel4);

        JPanel notePanel = new JPanel();
        notePanel.setBackground(Main.UITheme.SURFACE);
        notePanel.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        notePanel.setLayout(new BoxLayout(notePanel, BoxLayout.Y_AXIS));

        JLabel lb_note = new JLabel("NOTE");
        Main.PageUI.styleCaption(lb_note);
        lb_note.setAlignmentX(Component.LEFT_ALIGNMENT);
        notePanel.add(lb_note);
        notePanel.add(Box.createVerticalStrut(10));

        JLabel lb_noteBody = new JLabel("<html>Suppliers appear in the restock form and on "
                + "purchase records. Phone is the number the restock officer calls from "
                + "the floor — keep it current.</html>");
        lb_noteBody.setFont(Main.UITheme.FONT_REGULAR.deriveFont(12f));
        lb_noteBody.setForeground(Main.UITheme.TEXT_MUTED);
        lb_noteBody.setAlignmentX(Component.LEFT_ALIGNMENT);
        notePanel.add(lb_noteBody);
        detailsShell.add(notePanel);

        jPanel3.add(detailsShell);

        // ---- Action bar ----
        JPanel actionBar = new JPanel(new BorderLayout());
        actionBar.setBackground(Main.UITheme.SURFACE);
        actionBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 1, 1, Main.UITheme.GRID_LINE),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        actionBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftActions.setOpaque(false);

        btn_simpan.setText("Save supplier");
        Main.PageUI.stylePrimaryButton(btn_simpan);
        btn_simpan.addActionListener(evt -> btn_simpanActionPerformed(evt));
        leftActions.add(btn_simpan);

        btn_batal.setText("Cancel");
        btn_batal.setFocusPainted(false);
        btn_batal.setBorderPainted(false);
        btn_batal.setContentAreaFilled(false);
        btn_batal.setOpaque(false);
        btn_batal.setForeground(Main.UITheme.ACCENT);
        btn_batal.setFont(Main.UITheme.FONT_BOLD.deriveFont(12f));
        btn_batal.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn_batal.addActionListener(evt -> btn_batalActionPerformed(evt));
        leftActions.add(btn_batal);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);

        btn_tambah.setText("New");
        Main.PageUI.styleGhostButton(btn_tambah);
        btn_tambah.addActionListener(evt -> btn_tambahActionPerformed(evt));
        rightActions.add(btn_tambah);

        btn_edit.setText("Edit");
        Main.PageUI.styleGhostButton(btn_edit);
        btn_edit.addActionListener(evt -> btn_editActionPerformed(evt));
        rightActions.add(btn_edit);

        btn_hapus.setText("Delete");
        btn_hapus.setFocusPainted(false);
        btn_hapus.setBorderPainted(false);
        btn_hapus.setContentAreaFilled(false);
        btn_hapus.setOpaque(false);
        btn_hapus.setForeground(Main.UITheme.ACCENT);
        btn_hapus.setFont(Main.UITheme.FONT_BOLD.deriveFont(12f));
        btn_hapus.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn_hapus.addActionListener(evt -> btn_hapusActionPerformed(evt));
        rightActions.add(btn_hapus);

        Main.PageUI.styleField(txt_cariSupplier);
        txt_cariSupplier.putClientProperty("JTextField.placeholderText", "Search supplier...");
        txt_cariSupplier.setColumns(14);
        txt_cariSupplier.addActionListener(evt -> txt_cariSupplierActionPerformed(evt));
        rightActions.add(txt_cariSupplier);

        btn_cari.setText("Search");
        Main.PageUI.styleGhostButton(btn_cari);
        btn_cari.setIcon(null);
        btn_cari.addActionListener(evt -> btn_cariActionPerformed(evt));
        rightActions.add(btn_cari);

        jButton1.setText("Refresh");
        Main.PageUI.styleGhostButton(jButton1);
        jButton1.addActionListener(evt -> jButton1ActionPerformed(evt));
        rightActions.add(jButton1);

        actionBar.add(leftActions, BorderLayout.WEST);
        actionBar.add(rightActions, BorderLayout.EAST);
        jPanel3.add(actionBar);

        // ---- Table ----
        tbl_supplier.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"ID", "SUPPLIER", "ADDRESS", "PHONE"}));
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
        Main.PageUI.styleTable(tbl_supplier);

        jScrollPane1.setViewportView(tbl_supplier);
        Main.PageUI.styleScroll(jScrollPane1);
        jScrollPane1.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 1, Main.UITheme.GRID_LINE));
        jScrollPane1.setAlignmentX(Component.LEFT_ALIGNMENT);
        jScrollPane1.setPreferredSize(new Dimension(10, 260));
        jPanel3.add(jScrollPane1);

        lb_endResults = new JLabel(" ");
        lb_endResults.setFont(Main.UITheme.FONT_REGULAR.deriveFont(11f));
        lb_endResults.setForeground(Main.UITheme.TEXT_MUTED);
        lb_endResults.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 1, 1, Main.UITheme.GRID_LINE),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        lb_endResults.setAlignmentX(Component.LEFT_ALIGNMENT);
        lb_endResults.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        jPanel3.add(lb_endResults);

        // ---- Footer ----
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        lblcount_rows.setText("Rows: 0");
        lblcount_rows.setFont(Main.UITheme.FONT_REGULAR.deriveFont(11f));
        lblcount_rows.setForeground(Main.UITheme.TEXT_MUTED);

        lb_selected = new JLabel("Selected: —");
        lb_selected.setFont(Main.UITheme.FONT_REGULAR.deriveFont(11f));
        lb_selected.setForeground(Main.UITheme.TEXT_MUTED);
        jLabel9.setText("");

        footer.add(lblcount_rows, BorderLayout.WEST);
        footer.add(lb_selected, BorderLayout.EAST);
        jPanel3.add(footer);

        txt_id.setColumns(4);
        txttemp_kode.setColumns(4);
        jScrollPane2.setViewportView(txt_id);
        jScrollPane2.setPreferredSize(new Dimension(0, 0));
        jScrollPane2.setMinimumSize(new Dimension(0, 0));
        jScrollPane2.setMaximumSize(new Dimension(0, 0));
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
        tbl_supplier.clearSelection();
        TxtEmpty();
        BtnEnabled(false);
        btn_simpan.setText("Save supplier");
        txt_nama.requestFocus();
    }

    private void btn_hapusActionPerformed(java.awt.event.ActionEvent evt) {
        int ok = JOptionPane.showConfirmDialog(null, "Delete this supplier?", "Confirm", JOptionPane.OK_CANCEL_OPTION);
        if (ok == 0) {
            try {
                String row_id = txt_id.getText();
                Connection conn = Koneksi.getConnection();
                java.sql.Statement stm = conn.createStatement();
                stm.executeUpdate("DELETE FROM supplier WHERE supplier_Id = '" + row_id + "'");
                JOptionPane.showMessageDialog(null, "Supplier deleted.");
                btn_tambah.doClick();
                GetData();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "This supplier is used by products.");
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Form_User.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void btn_editActionPerformed(java.awt.event.ActionEvent evt) {
        String row_id = txt_id.getText();
        if (!"0".equals(row_id) && row_id != null && !row_id.isEmpty()) {
            try {
                btn_simpan.setText("Save changes");
                Connection conn = Koneksi.getConnection();
                java.sql.Statement stm = conn.createStatement();
                java.sql.ResultSet sql = stm.executeQuery("SELECT * FROM supplier WHERE supplier_Id='" + row_id + "'");
                if (sql.next()) {
                    String kode = sql.getString("supplier_Id");
                    txt_id.setText(sql.getString("supplier_Id"));
                    txt_nama.setText(sql.getString("nama_supplier"));
                    txt_alamat.setText(sql.getString("alamat_supplier"));
                    txt_telp.setText(sql.getString("telp_supplier"));
                    txttemp_kode.setText(kode);
                    txt_nama.requestFocus();
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error " + e);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Form_User.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            JOptionPane.showMessageDialog(null, "Select a supplier first.");
        }
    }

    private void tbl_supplierMouseClicked(java.awt.event.MouseEvent evt) {
        GetData_View();
    }

    private void tbl_supplierMouseReleased(java.awt.event.MouseEvent evt) {
        GetData_View();
    }

    private void tbl_supplierKeyReleased(java.awt.event.KeyEvent evt) {
        GetData_View();
    }

    private void txt_telpActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void txt_namaActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void txt_alamatActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void btn_simpanActionPerformed(java.awt.event.ActionEvent evt) {
        String row_id = txt_id.getText();
        String row_txtnama = txt_nama.getText();
        String row_txtalamat = txt_alamat.getText();
        String row_txttelp = txt_telp.getText();
        int c_kode = 0;

        if (!"".equals(row_txtnama) && !"".equals(row_txtalamat)) {
            try {
                Connection conn = Koneksi.getConnection();
                java.sql.Statement stm = conn.createStatement();
                String countSql = "".equals(row_id)
                        ? "SELECT COUNT(supplier_Id) as count FROM supplier WHERE nama_supplier='" + row_txtnama + "'"
                        : "SELECT COUNT(supplier_Id) as count FROM supplier WHERE nama_supplier='" + row_txtnama
                            + "' AND supplier_Id <> '" + row_id + "'";
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
                                "INSERT INTO supplier(nama_supplier, alamat_supplier, telp_supplier, uuid) "
                                + "VALUES (?,?,?,?)",
                                java.sql.Statement.RETURN_GENERATED_KEYS);
                        ps.setString(1, row_txtnama);
                        ps.setString(2, row_txtalamat);
                        ps.setString(3, row_txttelp);
                        ps.setString(4, Ids.newUuid());
                        ps.executeUpdate();
                        java.sql.ResultSet keys = ps.getGeneratedKeys();
                        if (keys.next()) {
                            SyncOutbox.enqueueSupplierById(keys.getInt(1));
                        }
                        keys.close();
                        ps.close();
                        JOptionPane.showMessageDialog(null, "Supplier saved.");
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
                        stm.executeUpdate("UPDATE supplier SET nama_supplier ='" + row_txtnama + "',alamat_supplier= '"
                                + row_txtalamat + "',telp_supplier= '" + row_txttelp + "' WHERE supplier_Id = '" + row_id + "'");
                        try {
                            SyncOutbox.enqueueSupplierById(Integer.parseInt(row_id));
                        } catch (Exception ignored) {
                        }
                        JOptionPane.showMessageDialog(null, "Supplier updated.");
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
    }

    private void txt_telpKeyTyped(java.awt.event.KeyEvent evt) {
        char c = evt.getKeyChar();
        if (!(Character.isDigit(c)) && !(c == KeyEvent.VK_BACK_SPACE)) {
            JOptionPane.showMessageDialog(null, "Numbers only", "Invalid input", JOptionPane.ERROR_MESSAGE);
            evt.consume();
        }
    }

    private void txt_cariSupplierActionPerformed(java.awt.event.ActionEvent evt) {
        cariSupplier();
    }

    private void btn_cariActionPerformed(java.awt.event.ActionEvent evt) {
        cariSupplier();
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
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblcount_rows;
    private javax.swing.JTable tbl_supplier;
    private javax.swing.JTextField txt_alamat;
    private javax.swing.JTextField txt_cariSupplier;
    private javax.swing.JTextField txt_id;
    private javax.swing.JTextField txt_nama;
    private javax.swing.JTextField txt_telp;
    private javax.swing.JTextField txttemp_kode;
    // End of variables declaration//GEN-END:variables
}
