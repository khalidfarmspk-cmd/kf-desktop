/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Master;

import Main.PageUI;
import Main.UITheme;
import Main.UserSession;
import config.Ids;
import config.Koneksi;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Users master — list + edit-sidebar layout matching the reference.
 */
public class Form_User extends javax.swing.JPanel {

    private JLabel lb_status;
    private JLabel lb_endResults;
    private JLabel lb_selected;
    private JLabel lb_editTitle;
    private JLabel lb_passwordMeta;
    private JLabel lb_salesRecorded;
    private JPanel pn_passwordField;
    private JButton btn_resetPassword;
    private static final Color PANEL_BG = new Color(0xF3F3F1);
    private static final Color RULE = new Color(0xD0D0CC);

    UserSession UserSession = new UserSession();

    public Form_User() throws ClassNotFoundException {
        initComponents();
        txttemp_username.setVisible(false);
        txt_id.setVisible(false);
        GetData();
        BtnEnabled(false);
        showPasswordForNew(true);
        btn_simpan.setText("Save user");
        txt_id.setEditable(false);
        clearSales();
    }

    private void TxtEmpty() {
        txt_id.setText("");
        txt_nama.setText("");
        txt_alamat.setText("");
        txt_telp.setText("");
        txt_username.setText("");
        txt_password.setText("");
        cb_status.setSelectedItem("Active");
        cb_level.setSelectedItem("Staff");
        txttemp_username.setText("");
        if (lb_selected != null) {
            lb_selected.setText("Selected: —");
        }
        if (lb_editTitle != null) {
            lb_editTitle.setText("NEW USER");
        }
        if (lb_passwordMeta != null) {
            lb_passwordMeta.setText("Set a password for this account");
        }
        clearSales();
        showPasswordForNew(true);
    }

    private void BtnEnabled(boolean x) {
        btn_edit.setEnabled(x);
        btn_hapus.setEnabled(x);
        if (btn_resetPassword != null) {
            btn_resetPassword.setEnabled(x);
        }
    }

    private void showPasswordForNew(boolean isNew) {
        if (pn_passwordField != null) {
            pn_passwordField.setVisible(isNew);
        }
        if (btn_resetPassword != null) {
            btn_resetPassword.setVisible(!isNew);
        }
        jLabel7.setVisible(isNew);
        txt_password.setVisible(isNew);
    }

    private void clearSales() {
        if (lb_salesRecorded != null) {
            lb_salesRecorded.setText("0 · " + UITheme.CURRENCY + " 0");
        }
    }

    private void refreshStatus(int rows, int active) {
        if (lb_status != null) {
            String accounts = rows == 1 ? "account" : "accounts";
            lb_status.setText(rows + " " + accounts + " · " + active + " active");
        }
        if (lblcount_rows != null) {
            lblcount_rows.setText("Rows: " + rows);
        }
        if (lb_endResults != null) {
            if (rows == 0) {
                lb_endResults.setText("No accounts yet. Use + New to add a user.");
            } else {
                lb_endResults.setText("End of list — " + rows + " of " + rows + " accounts.");
            }
        }
    }

    private static String roleLabel(String level) {
        if ("PEMILIK".equalsIgnoreCase(level) || "Owner".equalsIgnoreCase(level)) {
            return "Owner";
        }
        if ("KARYAWAN".equalsIgnoreCase(level) || "Staff".equalsIgnoreCase(level) || "Employee".equalsIgnoreCase(level)) {
            return "Staff";
        }
        return level == null ? "" : level;
    }

    private static String statusLabel(String status) {
        if ("AKTIF".equalsIgnoreCase(status) || "Active".equalsIgnoreCase(status)) {
            return "Active";
        }
        if ("NON-AKTIF".equalsIgnoreCase(status) || "Suspended".equalsIgnoreCase(status)
                || "Inactive".equalsIgnoreCase(status)) {
            return "Suspended";
        }
        return status == null ? "" : status;
    }

    private static String roleDb(String label) {
        return "Owner".equalsIgnoreCase(label) ? "PEMILIK" : "KARYAWAN";
    }

    private static String statusDb(String label) {
        return "Active".equalsIgnoreCase(label) ? "AKTIF" : "NON-AKTIF";
    }

    private void applyTableModel(java.sql.ResultSet sql) throws SQLException {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"ID", "NAME", "USERNAME", "PHONE", "ROLE", "STATUS"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        int active = 0;
        while (sql.next()) {
            String status = sql.getString("status_user");
            if ("AKTIF".equalsIgnoreCase(status)) {
                active++;
            }
            model.addRow(new Object[]{
                sql.getString("user_Id"),
                sql.getString("nama_user"),
                sql.getString("username_user"),
                sql.getString("telp_user"),
                roleLabel(sql.getString("level_user")),
                statusLabel(status)
            });
        }
        tbl_user.setModel(model);
        if (tbl_user.getColumnCount() >= 6) {
            tbl_user.getColumnModel().getColumn(0).setPreferredWidth(40);
            tbl_user.getColumnModel().getColumn(1).setPreferredWidth(120);
            tbl_user.getColumnModel().getColumn(2).setPreferredWidth(100);
            tbl_user.getColumnModel().getColumn(3).setPreferredWidth(100);
            tbl_user.getColumnModel().getColumn(4).setPreferredWidth(70);
            tbl_user.getColumnModel().getColumn(5).setPreferredWidth(80);
        }
        PageUI.styleTable(tbl_user);
        styleStatusColumn();
        refreshStatus(model.getRowCount(), active);
        if (lb_selected != null) {
            lb_selected.setText("Selected: —");
        }
    }

    private void styleStatusColumn() {
        if (tbl_user.getColumnCount() < 6) {
            return;
        }
        tbl_user.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String s = value == null ? "" : value.toString();
                if (!isSelected && "Suspended".equalsIgnoreCase(s)) {
                    c.setForeground(UITheme.ACCENT);
                } else if (isSelected) {
                    c.setForeground(PageUI.INK);
                } else {
                    c.setForeground(PageUI.INK);
                }
                setFont(UITheme.FONT_REGULAR.deriveFont(12f));
                return c;
            }
        });
    }

    private void GetData() {
        try {
            Connection conn = Koneksi.getConnection();
            java.sql.Statement stm = conn.createStatement();
            java.sql.ResultSet sql = stm.executeQuery(
                    "SELECT user_Id, nama_user, alamat_user, telp_user, username_user, "
                    + "level_user, status_user FROM tableusers ORDER BY user_Id");
            applyTableModel(sql);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error " + e);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Form_User.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void GetData_View() {
        int row = tbl_user.getSelectedRow();
        if (row < 0) {
            return;
        }
        String row_id = tbl_user.getModel().getValueAt(row, 0).toString();
        String name = tbl_user.getModel().getValueAt(row, 1).toString();
        txt_id.setText(row_id);
        BtnEnabled(true);
        if (lb_selected != null) {
            lb_selected.setText("Selected: " + name);
        }
        if (lb_editTitle != null) {
            lb_editTitle.setText("EDIT USER");
        }
        if (row_id.equals(Integer.toString(UserSession.getU_id()))) {
            btn_hapus.setEnabled(false);
        }
        loadUser(row_id);
    }

    private void loadUser(String row_id) {
        try {
            showPasswordForNew(false);
            Connection conn = Koneksi.getConnection();
            java.sql.Statement stm = conn.createStatement();
            java.sql.ResultSet sql = stm.executeQuery("SELECT * FROM users WHERE user_Id='" + row_id + "'");
            if (sql.next()) {
                txt_id.setText(sql.getString("user_Id"));
                String kode = sql.getString("username_user");
                txt_nama.setText(sql.getString("nama_user"));
                txt_username.setText(sql.getString("username_user"));
                txt_alamat.setText(sql.getString("alamat_user"));
                txt_telp.setText(sql.getString("telp_user"));
                cb_status.setSelectedItem(statusLabel(sql.getString("status_user")));
                cb_level.setSelectedItem(roleLabel(sql.getString("level_user")));
                txttemp_username.setText(kode);
                txt_password.setText("");
                btn_simpan.setText("Save user");
                if (lb_passwordMeta != null) {
                    lb_passwordMeta.setText("Last changed "
                            + new SimpleDateFormat("d MMM yyyy").format(new Date()));
                }
                loadSales(row_id);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error " + e);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Form_User.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loadSales(String userId) {
        int count = 0;
        int total = 0;
        try {
            Connection conn = Koneksi.getConnection();
            java.sql.Statement stm = conn.createStatement();
            java.sql.ResultSet rs = stm.executeQuery(
                    "SELECT COUNT(*), COALESCE(SUM(total_Pembayaran),0) FROM penjualan "
                    + "WHERE user_Id='" + userId + "' AND voided = 0");
            if (rs.next()) {
                count = rs.getInt(1);
                total = rs.getInt(2);
            }
        } catch (Exception ignored) {
        }
        if (lb_salesRecorded != null) {
            lb_salesRecorded.setText(count + " · " + UITheme.CURRENCY + " "
                    + NumberFormat.getIntegerInstance(Locale.US).format(total));
        }
    }

    private void cariBarang() {
        try {
            String q = txt_cariUser.getText().trim().replace("'", "''");
            Connection conn = Koneksi.getConnection();
            java.sql.Statement stm = conn.createStatement();
            java.sql.ResultSet sql = stm.executeQuery(
                    "SELECT user_Id, nama_user, alamat_user, telp_user, username_user, "
                    + "level_user, status_user FROM tableusers "
                    + "WHERE user_Id LIKE '%" + q + "%' "
                    + "OR nama_user LIKE '%" + q + "%' "
                    + "OR alamat_user LIKE '%" + q + "%' "
                    + "OR username_user LIKE '%" + q + "%' "
                    + "OR level_user LIKE '%" + q + "%' "
                    + "OR telp_user LIKE '%" + q + "%' "
                    + "OR status_user LIKE '%" + q + "%' "
                    + "ORDER BY user_Id");
            applyTableModel(sql);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error " + e);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Form_User.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setFont(UITheme.FONT_REGULAR.deriveFont(13f));
        combo.setBackground(UITheme.SURFACE);
        combo.setForeground(PageUI.INK);
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.GRID_LINE),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
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

    private void resetPassword() {
        String row_id = txt_id.getText();
        if (row_id == null || row_id.isEmpty()) {
            return;
        }
        JPasswordField pf = new JPasswordField(18);
        JPanel panel = PageUI.wrapPasswordField(pf);
        int ok = JOptionPane.showConfirmDialog(this, panel, "Enter new password", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        String pwd = new String(pf.getPassword()).trim();
        if (pwd.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password cannot be empty.");
            return;
        }
        try {
            Connection conn = Koneksi.getConnection();
            String hash = BCrypt.hashpw(pwd, BCrypt.gensalt(10));
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE users SET password_user = ? WHERE user_Id = ?");
            ps.setString(1, hash);
            ps.setString(2, row_id);
            ps.executeUpdate();
            ps.close();
            if (lb_passwordMeta != null) {
                lb_passwordMeta.setText("Last changed "
                        + new SimpleDateFormat("d MMM yyyy").format(new Date()));
            }
            JOptionPane.showMessageDialog(this, "Password updated.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        jPanel1 = new JPanel();
        jPanel2 = new JPanel();
        jPanel3 = new JPanel();
        jLabel1 = new JLabel();
        jLabel2 = new JLabel();
        jLabel3 = new JLabel();
        jLabel4 = new JLabel();
        jLabel5 = new JLabel();
        jLabel6 = new JLabel();
        jLabel7 = new JLabel();
        jLabel8 = new JLabel();
        jLabel9 = new JLabel();
        lblcount_rows = new JLabel();
        txt_nama = new JTextField();
        txt_alamat = new JTextField();
        txt_telp = new JTextField();
        txt_username = new JTextField();
        txt_password = new JPasswordField();
        txt_cariUser = new JTextField();
        txttemp_username = new JTextField();
        txt_id = new JTextField();
        cb_level = new JComboBox<>();
        cb_status = new JComboBox<>();
        btn_tambah = new JButton();
        btn_hapus = new JButton();
        btn_edit = new JButton();
        btn_simpan = new JButton();
        btn_batal = new JButton();
        btn_cari = new JButton();
        jButton1 = new JButton();
        jScrollPane1 = new JScrollPane();
        jScrollPane2 = new JScrollPane();
        tbl_user = new JTable();

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

        JLabel badge = new JLabel("  MASTER / 06  ");
        badge.setOpaque(true);
        badge.setBackground(PageUI.INK);
        badge.setForeground(Color.WHITE);
        badge.setFont(UITheme.FONT_BOLD.deriveFont(11f));
        badge.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 0));
        jLabel1.setText("Users");
        jLabel1.setFont(UITheme.FONT_HEADING.deriveFont(22f));
        jLabel1.setForeground(PageUI.INK);
        jLabel1.setAlignmentX(Component.LEFT_ALIGNMENT);
        lb_status = new JLabel("0 accounts · 0 active");
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

        // LEFT
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(UITheme.PAGE_BG);
        left.setBorder(BorderFactory.createEmptyBorder(14, 16, 12, 12));

        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        PageUI.styleField(txt_cariUser);
        txt_cariUser.putClientProperty("JTextField.placeholderText", "Search name or username");
        txt_cariUser.addActionListener(e -> cariBarang());
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
        searchRow.add(txt_cariUser, BorderLayout.CENTER);
        searchRow.add(searchBtns, BorderLayout.EAST);

        tbl_user.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"ID", "NAME", "USERNAME", "PHONE", "ROLE", "STATUS"}));
        tbl_user.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tbl_userMouseClicked(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tbl_userMouseReleased(evt);
            }
        });
        tbl_user.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tbl_userKeyReleased(evt);
            }
        });
        PageUI.styleTable(tbl_user);
        jScrollPane1.setViewportView(tbl_user);
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
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(PANEL_BG);
        sidebar.setPreferredSize(new Dimension(340, 10));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, RULE));

        JPanel editHead = new JPanel(new BorderLayout());
        editHead.setOpaque(false);
        editHead.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, RULE),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        lb_editTitle = new JLabel("EDIT USER");
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
        PageUI.styleField(txt_alamat);
        PageUI.styleField(txt_telp);
        PageUI.styleField(txt_username);
        PageUI.styleField(txt_password);
        txt_password.setEchoChar('\u2022');
        txt_alamat.putClientProperty("JTextField.placeholderText", "Street, city");
        txt_telp.putClientProperty("JTextField.placeholderText", "+92 ...");
        txt_password.putClientProperty("JTextField.placeholderText", "Password");
        txt_username.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(KeyEvent evt) {
                txt_usernameKeyTyped(evt);
            }
        });
        txt_telp.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(KeyEvent evt) {
                txt_telpKeyTyped(evt);
            }
        });

        cb_level.setModel(new DefaultComboBoxModel<>(new String[]{"Staff", "Owner"}));
        cb_status.setModel(new DefaultComboBoxModel<>(new String[]{"Active", "Suspended"}));
        styleCombo(cb_level);
        styleCombo(cb_status);

        form.add(fieldBlock("FULL NAME", txt_nama));
        form.add(Box.createVerticalStrut(10));
        form.add(fieldBlock("ADDRESS", txt_alamat));
        form.add(Box.createVerticalStrut(10));

        JPanel phoneUser = new JPanel(new GridLayout(1, 2, 10, 0));
        phoneUser.setOpaque(false);
        phoneUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        phoneUser.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        phoneUser.add(fieldBlock("PHONE", txt_telp));
        phoneUser.add(fieldBlock("USERNAME", txt_username));
        form.add(phoneUser);
        form.add(Box.createVerticalStrut(10));

        JPanel roleStatus = new JPanel(new GridLayout(1, 2, 10, 0));
        roleStatus.setOpaque(false);
        roleStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        roleStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        roleStatus.add(fieldBlock("ROLE", cb_level));
        roleStatus.add(fieldBlock("STATUS", cb_status));
        form.add(roleStatus);
        form.add(Box.createVerticalStrut(14));

        JPanel passwordRow = new JPanel(new BorderLayout(8, 0));
        passwordRow.setOpaque(false);
        passwordRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        lb_passwordMeta = new JLabel("Set a password for this account");
        lb_passwordMeta.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lb_passwordMeta.setForeground(UITheme.TEXT_MUTED);
        btn_resetPassword = new JButton("Reset password");
        PageUI.styleGhostButton(btn_resetPassword);
        btn_resetPassword.setForeground(PageUI.INK);
        btn_resetPassword.addActionListener(e -> resetPassword());
        passwordRow.add(lb_passwordMeta, BorderLayout.CENTER);
        passwordRow.add(btn_resetPassword, BorderLayout.EAST);
        form.add(passwordRow);

        pn_passwordField = fieldBlock("PASSWORD", PageUI.wrapPasswordField(txt_password));
        form.add(Box.createVerticalStrut(8));
        form.add(pn_passwordField);
        form.add(Box.createVerticalStrut(14));

        JPanel salesRow = new JPanel(new BorderLayout());
        salesRow.setOpaque(false);
        salesRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        salesRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel lbSalesCap = new JLabel("Sales recorded");
        lbSalesCap.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        lbSalesCap.setForeground(UITheme.TEXT_MUTED);
        lb_salesRecorded = new JLabel("0 · " + UITheme.CURRENCY + " 0");
        lb_salesRecorded.setFont(UITheme.FONT_BOLD.deriveFont(13f));
        lb_salesRecorded.setForeground(PageUI.INK);
        salesRow.add(lbSalesCap, BorderLayout.WEST);
        salesRow.add(lb_salesRecorded, BorderLayout.EAST);
        form.add(salesRow);
        form.add(Box.createVerticalGlue());

        jLabel2.setVisible(false);
        jLabel3.setVisible(false);
        jLabel4.setVisible(false);
        jLabel5.setVisible(false);
        jLabel6.setVisible(false);
        jLabel7.setVisible(false);
        jLabel8.setVisible(false);
        btn_edit.setVisible(false);

        JPanel actions = new JPanel();
        actions.setBackground(UITheme.ACCENT);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));

        btn_simpan.setText("Save user");
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

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.getViewport().setBackground(PANEL_BG);
        formScroll.setOpaque(false);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);

        sidebar.add(editHead, BorderLayout.NORTH);
        sidebar.add(formScroll, BorderLayout.CENTER);
        sidebar.add(actions, BorderLayout.SOUTH);

        body.add(left, BorderLayout.CENTER);
        body.add(sidebar, BorderLayout.EAST);
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
        tbl_user.clearSelection();
        TxtEmpty();
        BtnEnabled(false);
        btn_simpan.setText("Save user");
        txt_nama.requestFocus();
    }

    private void btn_editActionPerformed(java.awt.event.ActionEvent evt) {
        String row_id = txt_id.getText();
        if (row_id != null && !row_id.isEmpty()) {
            loadUser(row_id);
            txt_nama.requestFocus();
        } else {
            JOptionPane.showMessageDialog(null, "Select a user first.");
        }
    }

    private void btn_simpanActionPerformed(java.awt.event.ActionEvent evt) {
        String row_id = txt_id.getText();
        String row_txtnama = txt_nama.getText().trim();
        String row_txtalamat = txt_alamat.getText().trim();
        String row_txttelp = txt_telp.getText().trim();
        String row_txtusername = txt_username.getText().trim();
        String row_txtpassword = new String(txt_password.getPassword());
        String row_txtstatus = statusDb(cb_status.getSelectedItem().toString());
        String row_txtlevel = roleDb(cb_level.getSelectedItem().toString());
        int c_kode = 0;

        if (!"".equals(row_txtnama) && !"".equals(row_txtusername)) {
            if ("".equals(row_id) && "".equals(row_txtpassword.trim())) {
                JOptionPane.showMessageDialog(null, "Please set a password for the new user.");
                return;
            }
            try {
                Connection conn = Koneksi.getConnection();
                java.sql.Statement stm = conn.createStatement();
                String countSql = "".equals(row_id)
                        ? "SELECT COUNT(user_Id) as count FROM users WHERE username_user='" + row_txtusername.replace("'", "''") + "'"
                        : "SELECT COUNT(user_Id) as count FROM users WHERE username_user='" + row_txtusername.replace("'", "''")
                            + "' AND user_Id <> '" + row_id + "'";
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
                        String hash = BCrypt.hashpw(row_txtpassword, BCrypt.gensalt(10));
                        PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO users(nama_user, alamat_user, telp_user, username_user, "
                                + "password_user, level_user, status_user, uuid) VALUES (?,?,?,?,?,?,?,?)");
                        ps.setString(1, row_txtnama);
                        ps.setString(2, row_txtalamat);
                        ps.setString(3, row_txttelp);
                        ps.setString(4, row_txtusername);
                        ps.setString(5, hash);
                        ps.setString(6, row_txtlevel);
                        ps.setString(7, row_txtstatus);
                        ps.setString(8, Ids.newUuid());
                        ps.executeUpdate();
                        ps.close();
                        JOptionPane.showMessageDialog(null, "User saved.");
                        btn_tambah.doClick();
                        GetData();
                    } catch (SQLException e) {
                        JOptionPane.showMessageDialog(null, "Error " + e);
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(Form_User.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "That username is already used.", "Could not save", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                if (c_kode == 0) {
                    try {
                        Connection conn = Koneksi.getConnection();
                        java.sql.Statement stm = conn.createStatement();
                        stm.executeUpdate("UPDATE users SET nama_user ='" + row_txtnama.replace("'", "''")
                                + "',alamat_user= '" + row_txtalamat.replace("'", "''")
                                + "',telp_user= '" + row_txttelp.replace("'", "''")
                                + "',username_user= '" + row_txtusername.replace("'", "''")
                                + "',level_user= '" + row_txtlevel + "',status_user='"
                                + row_txtstatus + "' WHERE user_Id = '" + row_id + "'");
                        JOptionPane.showMessageDialog(null, "User updated.");
                        btn_tambah.doClick();
                        GetData();
                    } catch (SQLException e) {
                        JOptionPane.showMessageDialog(null, "Error " + e);
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(Form_User.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "That username is already used.", "Could not save", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(null, "Please fill in name and username.");
        }
    }

    private void btn_hapusActionPerformed(java.awt.event.ActionEvent evt) {
        int ok = JOptionPane.showConfirmDialog(null, "Delete this user?", "Confirm", JOptionPane.OK_CANCEL_OPTION);
        if (ok == 0) {
            try {
                String row_id = txt_id.getText();
                Connection conn = Koneksi.getConnection();
                java.sql.Statement stm = conn.createStatement();
                stm.executeUpdate("DELETE FROM users WHERE user_Id = '" + row_id + "'");
                JOptionPane.showMessageDialog(null, "User deleted.");
                btn_tambah.doClick();
                GetData();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error " + e);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Form_User.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void txt_usernameKeyTyped(java.awt.event.KeyEvent evt) {
        char c = evt.getKeyChar();
        if (c == KeyEvent.VK_SPACE) {
            JOptionPane.showMessageDialog(null, "Username cannot contain spaces", "Invalid input", JOptionPane.ERROR_MESSAGE);
            evt.consume();
        }
    }

    private void tbl_userMouseClicked(java.awt.event.MouseEvent evt) {
        GetData_View();
    }

    private void tbl_userKeyReleased(java.awt.event.KeyEvent evt) {
        GetData_View();
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        GetData();
        TxtEmpty();
        BtnEnabled(false);
    }

    private void btn_batalActionPerformed(java.awt.event.ActionEvent evt) {
        btn_tambah.doClick();
    }

    private void tbl_userMouseReleased(java.awt.event.MouseEvent evt) {
        GetData_View();
    }

    private void txt_telpKeyTyped(java.awt.event.KeyEvent evt) {
        char c = evt.getKeyChar();
        if (!(Character.isDigit(c)) && !(c == KeyEvent.VK_BACK_SPACE) && c != ' ' && c != '+') {
            JOptionPane.showMessageDialog(null, "Numbers only", "Invalid input", JOptionPane.ERROR_MESSAGE);
            evt.consume();
        }
    }

    private void btn_cariActionPerformed(java.awt.event.ActionEvent evt) {
        cariBarang();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_batal;
    private javax.swing.JButton btn_cari;
    private javax.swing.JButton btn_edit;
    private javax.swing.JButton btn_hapus;
    private javax.swing.JButton btn_simpan;
    private javax.swing.JButton btn_tambah;
    private javax.swing.JComboBox<String> cb_level;
    private javax.swing.JComboBox<String> cb_status;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblcount_rows;
    private javax.swing.JTable tbl_user;
    private javax.swing.JTextField txt_alamat;
    private javax.swing.JTextField txt_cariUser;
    private javax.swing.JTextField txt_id;
    private javax.swing.JTextField txt_nama;
    private javax.swing.JPasswordField txt_password;
    private javax.swing.JTextField txt_telp;
    private javax.swing.JTextField txt_username;
    private javax.swing.JTextField txttemp_username;
    // End of variables declaration//GEN-END:variables
}
