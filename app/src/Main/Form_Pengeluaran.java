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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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
 * Shop expenses. Discounts given are shown as a separate, non-expense figure.
 */
public class Form_Pengeluaran extends javax.swing.JPanel {

    private static final Color PANEL_BG = new Color(0xF3F3F1);
    private static final Color RULE = new Color(0xD0D0CC);
    private static final String[] CATEGORIES = {
        "Rent", "Utilities", "Salaries", "Supplies", "Transport", "Maintenance", "Other"
    };

    private JLabel lb_status;
    private JLabel lb_editTitle;
    private JLabel lb_totalExpenses;
    private JLabel lb_discountsGiven;
    private JLabel lblcount_rows;
    private JLabel lb_selected;
    private com.toedter.calendar.JDateChooser txt_from;
    private com.toedter.calendar.JDateChooser txt_to;
    private com.toedter.calendar.JDateChooser txt_tanggal;
    private JComboBox<String> cb_kategori;
    private JTextField txt_id;
    private JTextField txt_keterangan;
    private JTextField txt_jumlah;
    private JButton btn_run;
    private JButton btn_tambah;
    private JButton btn_simpan;
    private JButton btn_batal;
    private JButton btn_hapus;
    private JTable tbl_pengeluaran;
    private JScrollPane jScrollPane1;
    private final boolean canEdit;

    public Form_Pengeluaran() {
        String jenis = user.getJenisUser();
        canEdit = "Owner".equals(jenis) || "PEMILIK".equals(jenis);
        initComponents();
        applyDefaultRange();
        loadData();
        applyRole();
    }

    private void applyRole() {
        if (canEdit) {
            return;
        }
        txt_tanggal.setEnabled(false);
        cb_kategori.setEnabled(false);
        txt_keterangan.setEnabled(false);
        txt_jumlah.setEnabled(false);
        btn_simpan.setEnabled(false);
        btn_tambah.setEnabled(false);
        btn_batal.setEnabled(false);
        if (btn_hapus != null) {
            btn_hapus.setEnabled(false);
        }
        btn_simpan.setText("View only");
    }

    private void applyDefaultRange() {
        Calendar cal = Calendar.getInstance();
        Date end = cal.getTime();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        txt_from.setDate(cal.getTime());
        txt_to.setDate(end);
        txt_tanggal.setDate(end);
    }

    private String formatDate(Date date) {
        if (date == null) {
            date = new Date();
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private String money(int amount) {
        return UITheme.CURRENCY + " " + NumberFormat.getIntegerInstance(Locale.US).format(amount);
    }

    private void TxtEmpty() {
        txt_id.setText("");
        txt_keterangan.setText("");
        txt_jumlah.setText("");
        txt_tanggal.setDate(new Date());
        if (cb_kategori.getItemCount() > 0) {
            cb_kategori.setSelectedIndex(0);
        }
        if (lb_editTitle != null) {
            lb_editTitle.setText("NEW EXPENSE");
        }
        if (lb_selected != null) {
            lb_selected.setText("Selected: —");
        }
        btn_simpan.setText(canEdit ? "Save expense" : "View only");
    }

    private void loadData() {
        String from = formatDate(txt_from.getDate());
        String to = formatDate(txt_to.getDate());
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection conn = Koneksi.getConnection();
            ps = conn.prepareStatement(
                    "SELECT e.pengeluaran_Id, e.tanggal, e.kategori, e.keterangan, e.jumlah, u.nama_user "
                    + "FROM pengeluaran e "
                    + "JOIN users u ON e.user_Id = u.user_Id "
                    + "WHERE e.tanggal BETWEEN ? AND ? "
                    + "ORDER BY e.tanggal DESC, e.pengeluaran_Id DESC");
            ps.setString(1, from);
            ps.setString(2, to);
            rs = ps.executeQuery();
            DefaultTableModel model = new DefaultTableModel(
                    new Object[]{"ID", "DATE", "CATEGORY", "DESCRIPTION", "AMOUNT", "RECORDED BY"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            int total = 0;
            while (rs.next()) {
                int amount = rs.getInt("jumlah");
                total += amount;
                String ket = rs.getString("keterangan");
                model.addRow(new Object[]{
                    rs.getInt("pengeluaran_Id"),
                    rs.getString("tanggal"),
                    rs.getString("kategori"),
                    ket == null ? "" : ket,
                    money(amount),
                    rs.getString("nama_user")
                });
            }
            tbl_pengeluaran.setModel(model);
            if (tbl_pengeluaran.getColumnCount() >= 6) {
                tbl_pengeluaran.getColumnModel().getColumn(0).setPreferredWidth(50);
                tbl_pengeluaran.getColumnModel().getColumn(1).setPreferredWidth(90);
                tbl_pengeluaran.getColumnModel().getColumn(2).setPreferredWidth(110);
                tbl_pengeluaran.getColumnModel().getColumn(3).setPreferredWidth(220);
                tbl_pengeluaran.getColumnModel().getColumn(4).setPreferredWidth(100);
                tbl_pengeluaran.getColumnModel().getColumn(5).setPreferredWidth(120);
            }
            PageUI.styleTable(tbl_pengeluaran);
            lb_totalExpenses.setText(money(total));
            if (lb_status != null) {
                lb_status.setText(model.getRowCount() + " expenses in range · " + money(total));
            }
            if (lblcount_rows != null) {
                lblcount_rows.setText("Rows: " + model.getRowCount());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not load expenses: " + e.getMessage());
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
        loadDiscountsGiven(from, to);
    }

    private void loadDiscountsGiven(String from, String to) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection conn = Koneksi.getConnection();
            ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(diskon), 0) FROM penjualan "
                    + "WHERE tanggal_penjualan BETWEEN ? AND ? AND voided = 0");
            ps.setString(1, from);
            ps.setString(2, to);
            rs = ps.executeQuery();
            int discounts = rs.next() ? rs.getInt(1) : 0;
            lb_discountsGiven.setText(money(discounts));
        } catch (Exception e) {
            lb_discountsGiven.setText(money(0));
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    private void loadRow() {
        int row = tbl_pengeluaran.getSelectedRow();
        if (row < 0) {
            return;
        }
        txt_id.setText(String.valueOf(tbl_pengeluaran.getValueAt(row, 0)));
        try {
            txt_tanggal.setDate(new SimpleDateFormat("yyyy-MM-dd")
                    .parse(String.valueOf(tbl_pengeluaran.getValueAt(row, 1))));
        } catch (Exception ignored) {
        }
        cb_kategori.setSelectedItem(String.valueOf(tbl_pengeluaran.getValueAt(row, 2)));
        txt_keterangan.setText(String.valueOf(tbl_pengeluaran.getValueAt(row, 3)));
        String amount = String.valueOf(tbl_pengeluaran.getValueAt(row, 4))
                .replace(UITheme.CURRENCY, "").replace(",", "").trim();
        txt_jumlah.setText(amount);
        if (lb_editTitle != null) {
            lb_editTitle.setText("EDIT EXPENSE");
        }
        if (lb_selected != null) {
            lb_selected.setText("Selected: 1 row");
        }
        if (canEdit) {
            btn_simpan.setText("Update expense");
        }
    }

    private void saveExpense() {
        if (!canEdit) {
            return;
        }
        Date tanggal = txt_tanggal.getDate();
        if (tanggal == null) {
            JOptionPane.showMessageDialog(this, "Date is required.");
            return;
        }
        Object kat = cb_kategori.getSelectedItem();
        if (kat == null) {
            JOptionPane.showMessageDialog(this, "Category is required.");
            return;
        }
        int jumlah;
        try {
            jumlah = Integer.parseInt(txt_jumlah.getText().trim().replace(",", ""));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Enter a valid amount in rupees.");
            return;
        }
        if (jumlah <= 0) {
            JOptionPane.showMessageDialog(this, "Amount must be greater than zero.");
            return;
        }
        String ket = txt_keterangan.getText().trim();
        String id = txt_id.getText().trim();
        int userId;
        try {
            userId = Integer.parseInt(user.getId());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Signed-in user is missing.");
            return;
        }
        PreparedStatement ps = null;
        try {
            Connection conn = Koneksi.getConnection();
            if (id.isEmpty()) {
                ps = conn.prepareStatement(
                        "INSERT INTO pengeluaran (tanggal, kategori, keterangan, jumlah, user_Id, uuid) "
                        + "VALUES (?,?,?,?,?,?)",
                        java.sql.Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, formatDate(tanggal));
                ps.setString(2, kat.toString());
                if (ket.isEmpty()) {
                    ps.setNull(3, java.sql.Types.VARCHAR);
                } else {
                    ps.setString(3, ket);
                }
                ps.setInt(4, jumlah);
                ps.setInt(5, userId);
                ps.setString(6, Ids.newUuid());
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    SyncOutbox.enqueueExpenseById(keys.getInt(1));
                }
                keys.close();
                JOptionPane.showMessageDialog(this, "Expense saved.");
            } else {
                ps = conn.prepareStatement(
                        "UPDATE pengeluaran SET tanggal = ?, kategori = ?, keterangan = ?, jumlah = ? "
                        + "WHERE pengeluaran_Id = ?");
                ps.setString(1, formatDate(tanggal));
                ps.setString(2, kat.toString());
                if (ket.isEmpty()) {
                    ps.setNull(3, java.sql.Types.VARCHAR);
                } else {
                    ps.setString(3, ket);
                }
                ps.setInt(4, jumlah);
                ps.setInt(5, Integer.parseInt(id));
                ps.executeUpdate();
                SyncOutbox.enqueueExpenseById(Integer.parseInt(id));
                JOptionPane.showMessageDialog(this, "Expense updated.");
            }
            TxtEmpty();
            loadData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not save expense: " + e.getMessage());
        } finally {
            closeQuietly(ps);
        }
    }

    private void deleteExpense() {
        if (!canEdit) {
            return;
        }
        String id = txt_id.getText().trim();
        if (id.isEmpty()) {
            int row = tbl_pengeluaran.getSelectedRow();
            if (row >= 0) {
                id = String.valueOf(tbl_pengeluaran.getValueAt(row, 0));
            }
        }
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select an expense to delete.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete this expense permanently?",
                "Delete expense",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        PreparedStatement ps = null;
        try {
            ps = Koneksi.getConnection().prepareStatement(
                    "DELETE FROM pengeluaran WHERE pengeluaran_Id = ?");
            ps.setInt(1, Integer.parseInt(id));
            int n = ps.executeUpdate();
            if (n > 0) {
                JOptionPane.showMessageDialog(this, "Expense deleted.");
            } else {
                JOptionPane.showMessageDialog(this, "Expense not found.");
            }
            TxtEmpty();
            loadData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not delete expense: " + e.getMessage());
        } finally {
            closeQuietly(ps);
        }
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

    private void initComponents() {
        txt_id = new JTextField();
        txt_id.setVisible(false);
        txt_from = new com.toedter.calendar.JDateChooser();
        txt_to = new com.toedter.calendar.JDateChooser();
        txt_tanggal = new com.toedter.calendar.JDateChooser();
        txt_from.setDateFormatString("yyyy-MM-dd");
        txt_to.setDateFormatString("yyyy-MM-dd");
        txt_tanggal.setDateFormatString("yyyy-MM-dd");
        cb_kategori = new JComboBox<String>(CATEGORIES);
        txt_keterangan = new JTextField();
        txt_jumlah = new JTextField();
        btn_run = new JButton("Run");
        btn_tambah = new JButton();
        btn_simpan = new JButton();
        btn_batal = new JButton();
        tbl_pengeluaran = new JTable();
        jScrollPane1 = new JScrollPane();
        lblcount_rows = new JLabel("Rows: 0");
        lb_totalExpenses = new JLabel(money(0));
        lb_discountsGiven = new JLabel(money(0));

        setLayout(new BorderLayout());
        PageUI.paintPage(this);

        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UITheme.PAGE_BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.PAGE_BG);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, RULE));
        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headerLeft.setOpaque(false);
        JLabel badge = new JLabel("  TRANSACTIONS / EXPENSES  ");
        badge.setOpaque(true);
        badge.setBackground(PageUI.INK);
        badge.setForeground(Color.WHITE);
        badge.setFont(UITheme.FONT_BOLD.deriveFont(11f));
        badge.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 0));
        JLabel title = new JLabel("Expenses");
        title.setFont(UITheme.FONT_HEADING.deriveFont(22f));
        title.setForeground(PageUI.INK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        lb_status = new JLabel("0 expenses in range");
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

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterRow.setOpaque(false);
        JLabel lbFrom = new JLabel("From");
        PageUI.styleLabel(lbFrom);
        JLabel lbTo = new JLabel("To");
        PageUI.styleLabel(lbTo);
        txt_from.setPreferredSize(new Dimension(130, 30));
        txt_to.setPreferredSize(new Dimension(130, 30));
        PageUI.stylePrimaryButton(btn_run);
        btn_run.addActionListener(e -> loadData());
        filterRow.add(lbFrom);
        filterRow.add(txt_from);
        filterRow.add(lbTo);
        filterRow.add(txt_to);
        filterRow.add(btn_run);
        if (canEdit) {
            JButton btnDeleteSelected = new JButton("Delete selected");
            PageUI.styleGhostButton(btnDeleteSelected);
            btnDeleteSelected.setForeground(UITheme.ACCENT);
            btnDeleteSelected.addActionListener(e -> deleteExpense());
            filterRow.add(btnDeleteSelected);
        }

        JPanel kpiRow = new JPanel(new GridLayout(1, 2, 0, 0));
        kpiRow.setOpaque(false);
        kpiRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.GRID_LINE),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        kpiRow.add(kpiCell("EXPENSES IN RANGE", lb_totalExpenses, false));
        kpiRow.add(kpiCell("DISCOUNTS GIVEN (NOT AN EXPENSE)", lb_discountsGiven, true));

        tbl_pengeluaran.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"ID", "DATE", "CATEGORY", "DESCRIPTION", "AMOUNT", "RECORDED BY"}));
        tbl_pengeluaran.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                loadRow();
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                loadRow();
            }
        });
        PageUI.styleTable(tbl_pengeluaran);
        jScrollPane1.setViewportView(tbl_pengeluaran);
        PageUI.styleScroll(jScrollPane1);

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

        JPanel leftNorth = new JPanel();
        leftNorth.setOpaque(false);
        leftNorth.setLayout(new BoxLayout(leftNorth, BoxLayout.Y_AXIS));
        filterRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        kpiRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftNorth.add(filterRow);
        leftNorth.add(Box.createVerticalStrut(12));
        leftNorth.add(kpiRow);
        leftNorth.add(Box.createVerticalStrut(12));

        left.add(leftNorth, BorderLayout.NORTH);
        left.add(jScrollPane1, BorderLayout.CENTER);
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
        lb_editTitle = new JLabel("NEW EXPENSE");
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
            tbl_pengeluaran.clearSelection();
            TxtEmpty();
        });
        editHead.add(lb_editTitle, BorderLayout.WEST);
        editHead.add(btn_tambah, BorderLayout.EAST);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        PageUI.styleField(txt_keterangan);
        PageUI.styleField(txt_jumlah);
        txt_jumlah.setHorizontalAlignment(SwingConstants.RIGHT);
        txt_jumlah.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(KeyEvent evt) {
                char c = evt.getKeyChar();
                if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE) {
                    evt.consume();
                }
            }
        });
        txt_keterangan.putClientProperty("JTextField.placeholderText", "Optional note");
        txt_tanggal.setPreferredSize(new Dimension(10, 30));
        form.add(fieldBlock("DATE", txt_tanggal));
        form.add(Box.createVerticalStrut(12));
        form.add(fieldBlock("CATEGORY", cb_kategori));
        form.add(Box.createVerticalStrut(12));
        form.add(fieldBlock("DESCRIPTION", txt_keterangan));
        form.add(Box.createVerticalStrut(12));
        form.add(fieldBlock("AMOUNT (Rs)", txt_jumlah));
        form.add(Box.createVerticalStrut(16));
        JLabel lbHint = new JLabel("<html>Bill discounts are revenue forgone, not money spent. "
                + "They are shown on the left as “Discounts given (not an expense)” "
                + "and are never inserted here.</html>");
        lbHint.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lbHint.setForeground(UITheme.TEXT_MUTED);
        lbHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lbHint);
        form.add(Box.createVerticalGlue());

        JPanel actions = new JPanel();
        actions.setBackground(UITheme.ACCENT);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));
        btn_simpan.setText("Save expense");
        btn_simpan.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn_simpan.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        PageUI.stylePrimaryButton(btn_simpan);
        btn_simpan.setBackground(Color.WHITE);
        btn_simpan.setForeground(UITheme.ACCENT);
        btn_simpan.addActionListener(e -> saveExpense());
        JPanel secondary = new JPanel(new GridLayout(1, 2, 8, 0));
        secondary.setOpaque(false);
        secondary.setAlignmentX(Component.LEFT_ALIGNMENT);
        secondary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn_batal = outlineBtn("Cancel");
        btn_batal.addActionListener(e -> {
            tbl_pengeluaran.clearSelection();
            TxtEmpty();
        });
        btn_hapus = outlineBtn("Delete");
        btn_hapus.addActionListener(e -> deleteExpense());
        secondary.add(btn_batal);
        secondary.add(btn_hapus);
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

    private JPanel kpiCell(String caption, JLabel value, boolean muted) {
        JPanel cell = new JPanel();
        cell.setOpaque(true);
        cell.setBackground(muted ? PANEL_BG : UITheme.SURFACE);
        cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, muted ? 0 : 1, RULE),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        JLabel cap = new JLabel(caption);
        PageUI.styleCaption(cap);
        cap.setAlignmentX(Component.LEFT_ALIGNMENT);
        value.setFont(UITheme.FONT_BOLD.deriveFont(18f));
        value.setForeground(PageUI.INK);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        cell.add(cap);
        cell.add(Box.createVerticalStrut(4));
        cell.add(value);
        return cell;
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
