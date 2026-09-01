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
import java.awt.Window;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/**
 * Customers master — walk-in shoppers with no login.
 */
public class Form_Pelanggan extends javax.swing.JPanel {

    private static final Color PANEL_BG = new Color(0xF3F3F1);
    private static final Color RULE = new Color(0xD0D0CC);

    private JLabel lb_status;
    private JLabel lb_endResults;
    private JLabel lb_selected;
    private JLabel lb_editTitle;
    private JLabel lb_statSales;
    private JLabel lb_statSpent;
    private JLabel lblcount_rows;
    private JTextField txt_id;
    private JTextField txt_nama;
    private JTextField txt_telp;
    private JTextField txt_alamat;
    private JTextField txt_cari;
    private JButton btn_tambah;
    private JButton btn_simpan;
    private JButton btn_batal;
    private JButton btn_cari;
    private JButton btn_refresh;
    private JTable tbl_pelanggan;
    private JScrollPane jScrollPane1;

    public Form_Pelanggan() {
        initComponents();
        GetData();
        btn_simpan.setText("Save customer");
    }

    /**
     * Small dialog used from the sell screen. Returns the new pelanggan_Id, or
     * {@code null} if cancelled.
     */
    public static Integer promptNew(Component parent) {
        Window owner = parent == null ? null : javax.swing.SwingUtilities.getWindowAncestor(parent);
        final JDialog dialog = new JDialog(owner, "New customer", JDialog.DEFAULT_MODALITY_TYPE);
        dialog.setResizable(false);

        JTextField nama = new JTextField();
        JTextField telp = new JTextField();
        JTextField alamat = new JTextField();
        PageUI.styleField(nama);
        PageUI.styleField(telp);
        PageUI.styleField(alamat);
        nama.putClientProperty("JTextField.placeholderText", "Required");
        telp.putClientProperty("JTextField.placeholderText", "Optional");
        alamat.putClientProperty("JTextField.placeholderText", "Optional");

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(16, 18, 8, 18));
        form.setBackground(UITheme.SURFACE);
        form.add(labeled("NAME", nama));
        form.add(Box.createVerticalStrut(10));
        form.add(labeled("PHONE", telp));
        form.add(Box.createVerticalStrut(10));
        form.add(labeled("ADDRESS", alamat));

        final Integer[] createdId = new Integer[1];

        JButton save = new JButton("Save customer");
        PageUI.stylePrimaryButton(save);
        save.addActionListener(e -> {
            Integer id = insertCustomer(dialog, nama.getText(), telp.getText(), alamat.getText());
            if (id != null) {
                createdId[0] = id;
                dialog.dispose();
            }
        });
        JButton cancel = new JButton("Cancel");
        PageUI.styleGhostButton(cancel);
        cancel.setForeground(PageUI.INK);
        cancel.addActionListener(e -> dialog.dispose());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.setBorder(BorderFactory.createEmptyBorder(8, 18, 16, 18));
        actions.add(cancel);
        actions.add(save);

        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(form, BorderLayout.CENTER);
        dialog.getContentPane().add(actions, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setSize(Math.max(360, dialog.getWidth()), dialog.getHeight());
        dialog.setLocationRelativeTo(parent);
        nama.requestFocusInWindow();
        dialog.setVisible(true);
        return createdId[0];
    }

    public static Integer insertCustomer(Component parent, String name, String phone, String address) {
        String nama = name == null ? "" : name.trim();
        if (nama.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Customer name is required.");
            return null;
        }
        String telp = phone == null ? "" : phone.trim();
        String alamat = address == null ? "" : address.trim();
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            Connection conn = Koneksi.getConnection();
            ps = conn.prepareStatement(
                    "INSERT INTO pelanggan (nama_pelanggan, telp_pelanggan, alamat_pelanggan, uuid) "
                    + "VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, nama);
            if (telp.isEmpty()) {
                ps.setNull(2, java.sql.Types.VARCHAR);
            } else {
                ps.setString(2, telp);
            }
            if (alamat.isEmpty()) {
                ps.setNull(3, java.sql.Types.VARCHAR);
            } else {
                ps.setString(3, alamat);
            }
            ps.setString(4, Ids.newUuid());
            ps.executeUpdate();
            keys = ps.getGeneratedKeys();
            if (keys.next()) {
                Integer newId = Integer.valueOf(keys.getInt(1));
                SyncOutbox.enqueueCustomerById(newId.intValue());
                return newId;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, "Could not save customer: " + e.getMessage());
        } finally {
            closeQuietly(keys);
            closeQuietly(ps);
        }
        return null;
    }

    private static JPanel labeled(String caption, JTextField field) {
        JPanel wrap = new JPanel(new BorderLayout(0, 4));
        wrap.setOpaque(false);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lb = new JLabel(caption);
        PageUI.styleCaption(lb);
        wrap.add(lb, BorderLayout.NORTH);
        wrap.add(field, BorderLayout.CENTER);
        return wrap;
    }

    private void TxtEmpty() {
        txt_id.setText("");
        txt_nama.setText("");
        txt_telp.setText("");
        txt_alamat.setText("");
        if (lb_selected != null) {
            lb_selected.setText("Selected: —");
        }
        if (lb_editTitle != null) {
            lb_editTitle.setText("NEW CUSTOMER");
        }
        setStat(lb_statSales, "0");
        setStat(lb_statSpent, money(0));
    }

    private void setStat(JLabel label, String value) {
        if (label != null) {
            label.setText(value);
        }
    }

    private String money(int amount) {
        return UITheme.CURRENCY + " " + NumberFormat.getIntegerInstance(Locale.US).format(amount);
    }

    private void refreshStatus(int rows) {
        if (lb_status != null) {
            lb_status.setText(rows + (rows == 1 ? " customer" : " customers") + " on file");
        }
        if (lblcount_rows != null) {
            lblcount_rows.setText("Rows: " + rows);
        }
        if (lb_endResults != null) {
            if (rows == 0) {
                lb_endResults.setText("No customers yet. Use + New to add a customer.");
            } else {
                lb_endResults.setText("End of list — " + rows + " of " + rows + " customers.");
            }
        }
    }

    private void GetData() {
        loadTable(null);
    }

    private void loadTable(String query) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection conn = Koneksi.getConnection();
            String sql = "SELECT p.pelanggan_Id, p.nama_pelanggan, p.telp_pelanggan, p.alamat_pelanggan, "
                    + "COUNT(j.penjualan_Id) AS sales_count, "
                    + "COALESCE(SUM(j.Total_pembayaran), 0) AS total_spent "
                    + "FROM pelanggan p "
                    + "LEFT JOIN penjualan j ON j.pelanggan_Id = p.pelanggan_Id AND j.voided = 0 ";
            if (query != null && !query.isEmpty()) {
                sql += "WHERE p.nama_pelanggan LIKE ? OR p.telp_pelanggan LIKE ? ";
            }
            sql += "GROUP BY p.pelanggan_Id, p.nama_pelanggan, p.telp_pelanggan, p.alamat_pelanggan "
                    + "ORDER BY p.nama_pelanggan";
            ps = conn.prepareStatement(sql);
            if (query != null && !query.isEmpty()) {
                String like = "%" + query + "%";
                ps.setString(1, like);
                ps.setString(2, like);
            }
            rs = ps.executeQuery();
            DefaultTableModel model = new DefaultTableModel(
                    new Object[]{"ID", "NAME", "PHONE", "ADDRESS", "SALES", "TOTAL SPENT"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            while (rs.next()) {
                String phone = rs.getString("telp_pelanggan");
                String addr = rs.getString("alamat_pelanggan");
                model.addRow(new Object[]{
                    rs.getInt("pelanggan_Id"),
                    rs.getString("nama_pelanggan"),
                    phone == null ? "" : phone,
                    addr == null ? "" : addr,
                    rs.getInt("sales_count"),
                    money(rs.getInt("total_spent"))
                });
            }
            tbl_pelanggan.setModel(model);
            if (tbl_pelanggan.getColumnCount() >= 6) {
                tbl_pelanggan.getColumnModel().getColumn(0).setPreferredWidth(50);
                tbl_pelanggan.getColumnModel().getColumn(1).setPreferredWidth(180);
                tbl_pelanggan.getColumnModel().getColumn(2).setPreferredWidth(110);
                tbl_pelanggan.getColumnModel().getColumn(3).setPreferredWidth(220);
                tbl_pelanggan.getColumnModel().getColumn(4).setPreferredWidth(60);
                tbl_pelanggan.getColumnModel().getColumn(5).setPreferredWidth(110);
            }
            PageUI.styleTable(tbl_pelanggan);
            refreshStatus(model.getRowCount());
            if (lb_selected != null) {
                lb_selected.setText("Selected: —");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not load customers: " + e.getMessage());
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    private void loadRow() {
        int row = tbl_pelanggan.getSelectedRow();
        if (row < 0) {
            return;
        }
        txt_id.setText(String.valueOf(tbl_pelanggan.getValueAt(row, 0)));
        txt_nama.setText(String.valueOf(tbl_pelanggan.getValueAt(row, 1)));
        txt_telp.setText(String.valueOf(tbl_pelanggan.getValueAt(row, 2)));
        txt_alamat.setText(String.valueOf(tbl_pelanggan.getValueAt(row, 3)));
        setStat(lb_statSales, String.valueOf(tbl_pelanggan.getValueAt(row, 4)));
        setStat(lb_statSpent, String.valueOf(tbl_pelanggan.getValueAt(row, 5)));
        if (lb_selected != null) {
            lb_selected.setText("Selected: 1 row");
        }
        if (lb_editTitle != null) {
            lb_editTitle.setText("EDIT CUSTOMER");
        }
        btn_simpan.setText("Update customer");
    }

    private void saveCustomer() {
        String nama = txt_nama.getText().trim();
        if (nama.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Customer name is required.");
            return;
        }
        String telp = txt_telp.getText().trim();
        String alamat = txt_alamat.getText().trim();
        String id = txt_id.getText().trim();
        PreparedStatement ps = null;
        try {
            Connection conn = Koneksi.getConnection();
            if (id.isEmpty()) {
                Integer newId = insertCustomer(this, nama, telp, alamat);
                if (newId == null) {
                    return;
                }
                JOptionPane.showMessageDialog(this, "Customer saved.");
            } else {
                ps = conn.prepareStatement(
                        "UPDATE pelanggan SET nama_pelanggan = ?, telp_pelanggan = ?, alamat_pelanggan = ? "
                        + "WHERE pelanggan_Id = ?");
                ps.setString(1, nama);
                if (telp.isEmpty()) {
                    ps.setNull(2, java.sql.Types.VARCHAR);
                } else {
                    ps.setString(2, telp);
                }
                if (alamat.isEmpty()) {
                    ps.setNull(3, java.sql.Types.VARCHAR);
                } else {
                    ps.setString(3, alamat);
                }
                ps.setInt(4, Integer.parseInt(id));
                ps.executeUpdate();
                SyncOutbox.enqueueCustomerById(Integer.parseInt(id));
                JOptionPane.showMessageDialog(this, "Customer updated.");
            }
            TxtEmpty();
            btn_simpan.setText("Save customer");
            GetData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not save customer: " + e.getMessage());
        } finally {
            closeQuietly(ps);
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

    private void initComponents() {
        txt_id = new JTextField();
        txt_nama = new JTextField();
        txt_telp = new JTextField();
        txt_alamat = new JTextField();
        txt_cari = new JTextField();
        btn_tambah = new JButton();
        btn_simpan = new JButton();
        btn_batal = new JButton();
        btn_cari = new JButton();
        btn_refresh = new JButton();
        tbl_pelanggan = new JTable();
        jScrollPane1 = new JScrollPane();
        lblcount_rows = new JLabel("Rows: 0");

        txt_id.setVisible(false);

        setLayout(new BorderLayout());
        PageUI.paintPage(this);

        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UITheme.PAGE_BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.PAGE_BG);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, RULE));

        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headerLeft.setOpaque(false);
        JLabel badge = new JLabel("  MASTER / CUSTOMERS  ");
        badge.setOpaque(true);
        badge.setBackground(PageUI.INK);
        badge.setForeground(Color.WHITE);
        badge.setFont(UITheme.FONT_BOLD.deriveFont(11f));
        badge.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 0));
        JLabel title = new JLabel("Customers");
        title.setFont(UITheme.FONT_HEADING.deriveFont(22f));
        title.setForeground(PageUI.INK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        lb_status = new JLabel("0 customers on file");
        lb_status.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        lb_status.setForeground(UITheme.TEXT_MUTED);
        lb_status.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleWrap.add(title);
        titleWrap.add(lb_status);
        headerLeft.add(badge);
        headerLeft.add(titleWrap);
        header.add(headerLeft, BorderLayout.WEST);
        page.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(UITheme.PAGE_BG);

        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(UITheme.PAGE_BG);
        left.setBorder(BorderFactory.createEmptyBorder(14, 16, 12, 12));

        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        PageUI.styleField(txt_cari);
        txt_cari.putClientProperty("JTextField.placeholderText", "Search by name or phone");
        txt_cari.addActionListener(e -> loadTable(txt_cari.getText().trim()));
        btn_cari.setText("Search");
        PageUI.stylePrimaryButton(btn_cari);
        btn_cari.addActionListener(e -> loadTable(txt_cari.getText().trim()));
        btn_refresh.setText("Refresh");
        PageUI.styleGhostButton(btn_refresh);
        btn_refresh.setForeground(PageUI.INK);
        btn_refresh.addActionListener(e -> {
            txt_cari.setText("");
            TxtEmpty();
            GetData();
        });
        JPanel searchBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchBtns.setOpaque(false);
        searchBtns.add(btn_cari);
        searchBtns.add(btn_refresh);
        searchRow.add(txt_cari, BorderLayout.CENTER);
        searchRow.add(searchBtns, BorderLayout.EAST);

        tbl_pelanggan.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"ID", "NAME", "PHONE", "ADDRESS", "SALES", "TOTAL SPENT"}));
        tbl_pelanggan.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                loadRow();
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                loadRow();
            }
        });
        PageUI.styleTable(tbl_pelanggan);
        jScrollPane1.setViewportView(tbl_pelanggan);
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
        lb_selected = new JLabel("Selected: —");
        lb_selected.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lb_selected.setForeground(UITheme.TEXT_MUTED);
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

        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(PANEL_BG);
        sidebar.setPreferredSize(new Dimension(320, 10));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, RULE));

        JPanel editHead = new JPanel(new BorderLayout());
        editHead.setOpaque(false);
        editHead.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, RULE),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        lb_editTitle = new JLabel("NEW CUSTOMER");
        lb_editTitle.setFont(UITheme.FONT_CAPTION);
        lb_editTitle.setForeground(UITheme.TEXT_CAPTION);
        btn_tambah.setText("+ New");
        btn_tambah.setFocusPainted(false);
        btn_tambah.setBorderPainted(false);
        btn_tambah.setContentAreaFilled(false);
        btn_tambah.setForeground(UITheme.ACCENT);
        btn_tambah.setFont(UITheme.FONT_BOLD.deriveFont(12f));
        btn_tambah.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn_tambah.addActionListener(e -> {
            tbl_pelanggan.clearSelection();
            TxtEmpty();
            btn_simpan.setText("Save customer");
            txt_nama.requestFocus();
        });
        editHead.add(lb_editTitle, BorderLayout.WEST);
        editHead.add(btn_tambah, BorderLayout.EAST);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        PageUI.styleField(txt_nama);
        PageUI.styleField(txt_telp);
        PageUI.styleField(txt_alamat);
        txt_nama.putClientProperty("JTextField.placeholderText", "Customer name");
        txt_telp.putClientProperty("JTextField.placeholderText", "Phone (optional)");
        txt_alamat.putClientProperty("JTextField.placeholderText", "Address (optional)");
        form.add(fieldBlock("NAME", txt_nama));
        form.add(Box.createVerticalStrut(12));
        form.add(fieldBlock("PHONE", txt_telp));
        form.add(Box.createVerticalStrut(12));
        form.add(fieldBlock("ADDRESS", txt_alamat));
        form.add(Box.createVerticalStrut(18));
        lb_statSales = new JLabel("0");
        lb_statSpent = new JLabel(money(0));
        form.add(statRow("Number of sales", lb_statSales));
        form.add(statRow("Total spent", lb_statSpent));
        form.add(Box.createVerticalStrut(14));
        JLabel lbNote = new JLabel("<html>Customers cannot be deleted — past sales still reference them.</html>");
        lbNote.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lbNote.setForeground(UITheme.TEXT_MUTED);
        lbNote.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lbNote);
        form.add(Box.createVerticalGlue());

        JPanel actions = new JPanel();
        actions.setBackground(UITheme.ACCENT);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));
        btn_simpan.setText("Save customer");
        btn_simpan.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn_simpan.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        PageUI.stylePrimaryButton(btn_simpan);
        btn_simpan.setBackground(Color.WHITE);
        btn_simpan.setForeground(UITheme.ACCENT);
        btn_simpan.addActionListener(e -> saveCustomer());
        JPanel secondary = new JPanel(new GridLayout(1, 1, 8, 0));
        secondary.setOpaque(false);
        secondary.setAlignmentX(Component.LEFT_ALIGNMENT);
        secondary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn_batal = outlineBtn("Cancel");
        btn_batal.addActionListener(e -> {
            tbl_pelanggan.clearSelection();
            TxtEmpty();
            btn_simpan.setText("Save customer");
        });
        secondary.add(btn_batal);
        actions.add(btn_simpan);
        actions.add(Box.createVerticalStrut(8));
        actions.add(secondary);

        sidebar.add(editHead, BorderLayout.NORTH);
        sidebar.add(form, BorderLayout.CENTER);
        sidebar.add(actions, BorderLayout.SOUTH);

        body.add(left, BorderLayout.CENTER);
        body.add(sidebar, BorderLayout.EAST);
        page.add(body, BorderLayout.CENTER);
        add(page, BorderLayout.CENTER);
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
