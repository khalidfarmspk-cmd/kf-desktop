/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Main;

import config.Ids;
import config.Koneksi;
import config.SyncOutbox;
import config.SyncService;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * Owner sales report — range KPIs, day chart, top products, master-detail.
 */
public class Form_ReportPemilik extends javax.swing.JPanel {

    PreparedStatement ps;
    ResultSet rs;
    Date tanggal = new Date();

    private JLabel lb_status;
    private JLabel lb_rangeSubtitle;
    private JLabel lb_linesTitle;
    private JLabel lb_linesMeta;
    private JLabel lb_txTotal;
    private JLabel lb_avgBasket;
    private JLabel lb_revenueCap;
    private JLabel lb_revenueCompare;
    private JLabel lb_txFooter;
    private JLabel lb_costGoods;
    private JLabel lb_profitTicket;
    private JButton btn_print;
    private JButton btn_export;
    private JButton btn_week;
    private JButton btn_today;
    private JButton btn_month;
    private JButton btn_vegetables;
    private JPanel mainReportPanel;
    private JPanel vegetablePanel;
    private CardLayout reportCardLayout;
    private JPanel reportCard;
    private String activeView = "sales";
    private JLabel lb_vegRevenue;
    private JLabel lb_vegExpenses;
    private JLabel lb_vegProfit;
    private JTable tbl_vegetableLines;
    private JTextField txt_vegExpense;
    private com.toedter.calendar.JDateChooser txt_vegExpenseDate;
    private JTable tbl_topProducts;
    private DayChartPanel dayChart;
    private JTextField txt_txSearch;
    private JCheckBox chk_showVoided;
    private TableRowSorter<DefaultTableModel> txSorter;
    private String activePreset = "today";
    private static final Color RULE = new Color(0xD0D0CC);
    private static final Color VOID_FG = new Color(0x8A8A86);

    public Form_ReportPemilik() {
        initComponents();
        formatTanggal();
        applyPreset("today");
        wireSelection();
        runReport();
    }

    private void formatTanggal() {
        txt_tanggaldetail.setDateFormatString("dd/MM/yyyy");
        txt_date.setDateFormatString("dd/MM/yyyy");
    }

    private void wireSelection() {
        tbl_laporan.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tbl_laporan.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                int row = tbl_laporan.getSelectedRow();
                if (row < 0) {
                    clearLinesPanel();
                    return;
                }
                if ("TOTALS".equals(String.valueOf(tbl_laporan.getValueAt(row, 0)))) {
                    clearLinesPanel();
                    return;
                }
                String kode = stripTx(String.valueOf(tbl_laporan.getValueAt(row, 0)));
                String tgl = String.valueOf(tbl_laporan.getValueAt(row, 1));
                String delivery = String.valueOf(tbl_laporan.getValueAt(row, 4));
                String total = String.valueOf(tbl_laporan.getValueAt(row, 6)).replace(UITheme.CURRENCY, "").replace(",", "").trim();
                String kasir = String.valueOf(tbl_laporan.getValueAt(row, 7));
                loadLinesForTransaction(kode, tgl, kasir, total, delivery);
            }
        });
    }

    private String stripTx(String ticket) {
        if (ticket == null) {
            return "";
        }
        String t = ticket.trim();
        if (t.regionMatches(true, 0, "TX-", 0, 3)) {
            t = t.substring(3);
        } else if (t.regionMatches(true, 0, "INV-", 0, 4)) {
            t = t.substring(4);
        }
        // Drop " VOID" / "· VOID" suffix used when Show voided is on
        t = t.replaceAll("(?i)\\s*[·•]?\\s*VOID\\s*$", "").trim();
        return t.replaceFirst("^0+(?!$)", "");
    }

    private static boolean isVoidTicketLabel(String ticket) {
        return ticket != null && ticket.toUpperCase(Locale.ROOT).contains("VOID");
    }

    private void attachTxSorter(DefaultTableModel model) {
        txSorter = new TableRowSorter<DefaultTableModel>(model);
        tbl_laporan.setRowSorter(txSorter);
        applyTxFilter();
    }

    private void applyTxFilter() {
        if (txSorter == null) {
            return;
        }
        String q = txt_txSearch == null ? "" : txt_txSearch.getText().trim();
        if (q.isEmpty()) {
            txSorter.setRowFilter(null);
            return;
        }
        final String needle = q.toLowerCase(Locale.US);
        final String idNeedle = stripTx(q);
        txSorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                Object ticketObj = entry.getValue(0);
                String ticket = ticketObj == null ? "" : String.valueOf(ticketObj);
                if ("TOTALS".equalsIgnoreCase(ticket)) {
                    return false;
                }
                if (!idNeedle.isEmpty() && idNeedle.equals(stripTx(ticket))) {
                    return true;
                }
                for (int c = 0; c < entry.getValueCount(); c++) {
                    Object v = entry.getValue(c);
                    if (v != null && String.valueOf(v).toLowerCase(Locale.US).contains(needle)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void onTxSearchEnter() {
        String q = txt_txSearch == null ? "" : txt_txSearch.getText().trim();
        if (q.isEmpty()) {
            applyTxFilter();
            return;
        }
        // Scanners type the code then Enter — jump straight to that ticket.
        txt_txSearch.setText("");
        applyTxFilter();
        if (selectTicketByQuery(q)) {
            return;
        }
        txt_txSearch.setText(q);
        applyTxFilter();
        if (tbl_laporan.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No transaction matches \"" + q + "\" in this range.");
        } else if (tbl_laporan.getRowCount() == 1) {
            tbl_laporan.setRowSelectionInterval(0, 0);
        }
    }

    private boolean selectTicketByQuery(String q) {
        String id = stripTx(q);
        if (id.isEmpty()) {
            return false;
        }
        // Prefer exact ticket / invoice id match across the unfiltered model.
        DefaultTableModel model = (DefaultTableModel) tbl_laporan.getModel();
        for (int m = 0; m < model.getRowCount(); m++) {
            String ticket = String.valueOf(model.getValueAt(m, 0));
            if ("TOTALS".equalsIgnoreCase(ticket)) {
                continue;
            }
            if (id.equals(stripTx(ticket)) || ticket.equalsIgnoreCase(q.trim())
                    || ("INV-" + String.format("%04d", parseIntSafe(id))).equalsIgnoreCase(q.trim())) {
                if (txSorter != null) {
                    txSorter.setRowFilter(null);
                }
                int view = tbl_laporan.convertRowIndexToView(m);
                if (view < 0) {
                    return false;
                }
                tbl_laporan.setRowSelectionInterval(view, view);
                tbl_laporan.scrollRectToVisible(tbl_laporan.getCellRect(view, 0, true));
                // Keep the caret in the search box. Focusing the table would send
                // the next scan's trailing Enter to JTable's selectNextRow action,
                // walking the selection down instead of re-finding the ticket.
                if (txt_txSearch != null) {
                    txt_txSearch.requestFocusInWindow();
                }
                return true;
            }
        }
        return false;
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private void clearLinesPanel() {
        lb_linesTitle.setText("TX-————");
        lb_linesMeta.setText("Select a transaction");
        lb_txTotal.setText("Total  " + money(0));
        lb_costGoods.setText(money(0));
        lb_profitTicket.setText(money(0));
        tbl_detailLaporan.setModel(newLinesModel());
        PageUI.styleTable(tbl_detailLaporan);
    }

    private DefaultTableModel newLinesModel() {
        return new DefaultTableModel(new Object[][]{}, new String[]{"PRODUCT", "QTY", "SUBTOTAL"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private DefaultTableModel newTxModel() {
        return new DefaultTableModel(
                new Object[][]{},
                new String[]{"TICKET", "DATE", "CUSTOMER", "PAYMENT", "DELIVERY MAN", "DISCOUNT", "TOTAL", "CASHIER"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private String formatDate(Date date) {
        if (date == null) {
            date = tanggal;
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private String money(int amount) {
        return UITheme.CURRENCY + " " + NumberFormat.getIntegerInstance(Locale.US).format(amount);
    }

    private String formatRp(String amount) {
        try {
            return money(Integer.parseInt(amount.trim().replace(",", "")));
        } catch (Exception e) {
            return UITheme.CURRENCY + " " + amount;
        }
    }

    private void applyPreset(String preset) {
        activePreset = preset;
        Calendar cal = Calendar.getInstance();
        Date end = cal.getTime();
        Date start;
        if ("today".equals(preset)) {
            start = end;
        } else if ("month".equals(preset)) {
            cal.set(Calendar.DAY_OF_MONTH, 1);
            start = cal.getTime();
        } else {
            cal.add(Calendar.DAY_OF_MONTH, -6);
            start = cal.getTime();
        }
        txt_date.setDate(start);
        txt_tanggaldetail.setDate(end);
        stylePresetButtons();
        updateRangeSubtitle();
    }

    private void stylePresetButtons() {
        boolean salesView = "sales".equals(activeView);
        stylePreset(btn_week, "week".equals(activePreset) && salesView);
        stylePreset(btn_today, "today".equals(activePreset) && salesView);
        stylePreset(btn_month, "month".equals(activePreset) && salesView);
        stylePreset(btn_vegetables, "vegetables".equals(activeView));
    }

    private void showReportView(String view) {
        activeView = view;
        stylePresetButtons();
        if (reportCardLayout != null && reportCard != null) {
            reportCardLayout.show(reportCard, view);
        }
        if ("vegetables".equals(view)) {
            jLabel1.setText("Vegetable Sales");
        } else {
            jLabel1.setText("Sales");
        }
    }

    private void stylePreset(JButton b, boolean on) {
        if (b == null) {
            return;
        }
        if (on) {
            b.setBackground(PageUI.INK);
            b.setForeground(Color.WHITE);
            b.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        } else {
            b.setBackground(UITheme.SURFACE);
            b.setForeground(PageUI.INK);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(RULE),
                    BorderFactory.createEmptyBorder(7, 13, 7, 13)));
        }
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(UITheme.FONT_BOLD.deriveFont(12f));
    }

    private void updateRangeSubtitle() {
        if (lb_rangeSubtitle == null) {
            return;
        }
        Date from = txt_date.getDate();
        Date to = txt_tanggaldetail.getDate();
        if (from == null || to == null) {
            return;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("d MMM yyyy");
        long days = Math.max(1, (to.getTime() - from.getTime()) / (1000L * 60 * 60 * 24) + 1);
        lb_rangeSubtitle.setText(new SimpleDateFormat("d").format(from) + " — "
                + fmt.format(to) + " · " + days + " day" + (days == 1 ? "" : "s"));
        if (lb_revenueCap != null) {
            lb_revenueCap.setText("REVENUE, " + days + " DAY" + (days == 1 ? "" : "S"));
        }
    }

    private void refreshKpis() {
        String from = formatDate(txt_date.getDate());
        String to = formatDate(txt_tanggaldetail.getDate());
        int tx = 0;
        int revenue = 0;
        int profit = 0;
        try {
            ps = Koneksi.getConnection().prepareStatement(
                    "SELECT COUNT(DISTINCT n.penjualan_Id), COALESCE(SUM(n.Subtotal),0) "
                    + "FROM nota_penjualan n "
                    + "JOIN penjualan j ON j.penjualan_Id = n.penjualan_Id "
                    + "JOIN produk p ON p.kode_produk = n.kode_produk "
                    + "WHERE j.tanggal_penjualan BETWEEN ? AND ? AND j.voided = 0 AND p.is_scale = 0");
            ps.setString(1, from);
            ps.setString(2, to + " 23:59:59");
            rs = ps.executeQuery();
            if (rs.next()) {
                tx = rs.getInt(1);
                revenue = rs.getInt(2);
            }
            // Net profit: line profit − bill discounts − expenses (same as Dashboard)
            ps = Koneksi.getConnection().prepareStatement(
                    "SELECT COALESCE(SUM(n.Subtotal - (n.jumlah * p.harga_beli)),0) "
                    + "FROM nota_penjualan n "
                    + "JOIN penjualan j ON j.penjualan_Id = n.penjualan_Id "
                    + "JOIN produk p ON p.kode_produk = n.kode_produk "
                    + "WHERE j.tanggal_penjualan BETWEEN ? AND ? AND j.voided = 0 AND p.is_scale = 0");
            ps.setString(1, from);
            ps.setString(2, to + " 23:59:59");
            rs = ps.executeQuery();
            if (rs.next()) {
                profit = rs.getInt(1);
            }
            ps = Koneksi.getConnection().prepareStatement(
                    "SELECT COALESCE(SUM(diskon),0) FROM penjualan "
                    + "WHERE tanggal_penjualan BETWEEN ? AND ? AND voided = 0");
            ps.setString(1, from);
            ps.setString(2, to + " 23:59:59");
            rs = ps.executeQuery();
            if (rs.next()) {
                profit -= rs.getInt(1);
            }
            ps = Koneksi.getConnection().prepareStatement(
                    "SELECT COALESCE(SUM(jumlah),0) FROM pengeluaran "
                    + "WHERE tanggal BETWEEN ? AND ?");
            ps.setString(1, from);
            ps.setString(2, to);
            rs = ps.executeQuery();
            if (rs.next()) {
                profit -= rs.getInt(1);
            }
        } catch (Exception e) {
            // Fall back to single-day stored procedures for the end date
            try {
                totalPenjualan();
                totalPendapatan();
                totalKeuntungan();
            } catch (Exception ignored) {
            }
        }
        lb_penjualan.setText(Integer.toString(tx));
        lb_pendapatan.setText(money(revenue));
        lb_keuntungan.setText(money(profit));
        int avg = tx == 0 ? 0 : revenue / tx;
        if (lb_avgBasket != null) {
            lb_avgBasket.setText(money(avg));
        }

        // Compare vs previous equal-length window
        try {
            Date fromD = txt_date.getDate();
            Date toD = txt_tanggaldetail.getDate();
            long span = Math.max(1, (toD.getTime() - fromD.getTime()) / (1000L * 60 * 60 * 24) + 1);
            Calendar c = Calendar.getInstance();
            c.setTime(fromD);
            c.add(Calendar.DAY_OF_MONTH, (int) -span);
            Date prevFrom = c.getTime();
            c.setTime(fromD);
            c.add(Calendar.DAY_OF_MONTH, -1);
            Date prevTo = c.getTime();
            ps = Koneksi.getConnection().prepareStatement(
                    "SELECT COALESCE(SUM(total_pembayaran),0) FROM laporan_penjualan "
                    + "WHERE tanggal_penjualan BETWEEN '" + formatDate(prevFrom) + "' AND '"
                    + formatDate(prevTo) + " 23:59:59'");
            rs = ps.executeQuery();
            int prev = rs.next() ? rs.getInt(1) : 0;
            if (lb_revenueCompare != null) {
                if (prev <= 0) {
                    lb_revenueCompare.setText("vs prior " + span + " days · " + money(prev));
                } else {
                    int pct = (int) Math.round(((revenue - prev) * 100.0) / prev);
                    String sign = pct >= 0 ? "+" : "";
                    lb_revenueCompare.setText(sign + pct + "% against "
                            + new SimpleDateFormat("d").format(prevFrom) + " — "
                            + new SimpleDateFormat("d MMM").format(prevTo));
                }
            }
        } catch (Exception ignored) {
            if (lb_revenueCompare != null) {
                lb_revenueCompare.setText(" ");
            }
        }
    }

    private void totalPenjualan() {
        try {
            String tanggalPenjualan = formatDate(txt_tanggaldetail.getDate());
            ps = Koneksi.getConnection().prepareStatement(
                    "CALL QuantityPenjualan('" + tanggalPenjualan + "', @QuantityPenjualan);");
            ps.execute();
            ps = Koneksi.getConnection().prepareStatement("SELECT @QuantityPenjualan;");
            rs = ps.executeQuery();
            if (rs.next()) {
                lb_penjualan.setText(Integer.toString(rs.getInt(1)));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please check again: " + e.getMessage());
        }
    }

    private void totalPendapatan() {
        try {
            String tanggalPenjualan = formatDate(txt_tanggaldetail.getDate());
            ps = Koneksi.getConnection().prepareStatement(
                    "CALL TotalPendapatan('" + tanggalPenjualan + "', @totalHargaPenjualan)");
            ps.execute();
            ps = Koneksi.getConnection().prepareStatement("SELECT @totalHargaPenjualan;");
            rs = ps.executeQuery();
            if (rs.next()) {
                lb_pendapatan.setText(formatRp(Integer.toString(rs.getInt(1))));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please check again: " + e.getMessage());
        }
    }

    private void totalKeuntungan() {
        // Fallback uses the same net formula as refreshKpis()
        try {
            String day = formatDate(txt_tanggaldetail.getDate());
            int profit = 0;
            ps = Koneksi.getConnection().prepareStatement(
                    "SELECT COALESCE(SUM(n.Subtotal - (n.jumlah * p.harga_beli)),0) "
                    + "FROM nota_penjualan n "
                    + "JOIN penjualan j ON j.penjualan_Id = n.penjualan_Id "
                    + "JOIN produk p ON p.kode_produk = n.kode_produk "
                    + "WHERE j.tanggal_penjualan = ? AND j.voided = 0 AND p.is_scale = 0");
            ps.setString(1, day);
            rs = ps.executeQuery();
            if (rs.next()) {
                profit = rs.getInt(1);
            }
            ps = Koneksi.getConnection().prepareStatement(
                    "SELECT COALESCE(SUM(diskon),0) FROM penjualan "
                    + "WHERE tanggal_penjualan = ? AND voided = 0");
            ps.setString(1, day);
            rs = ps.executeQuery();
            if (rs.next()) {
                profit -= rs.getInt(1);
            }
            ps = Koneksi.getConnection().prepareStatement(
                    "SELECT COALESCE(SUM(jumlah),0) FROM pengeluaran WHERE tanggal = ?");
            ps.setString(1, day);
            rs = ps.executeQuery();
            if (rs.next()) {
                profit -= rs.getInt(1);
            }
            lb_keuntungan.setText(money(profit));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please check again: " + e.getMessage());
        }
    }

    private void runReport() {
        updateRangeSubtitle();
        if ("vegetables".equals(activeView)) {
            refreshVegetablePanel();
            return;
        }
        tampilLaporan();
        loadDayChart();
        loadTopProducts();
        refreshKpis();
        int count = tbl_laporan.getRowCount();
        lb_status.setText(count + " transactions in range");
        if (count > 0) {
            tbl_laporan.setRowSelectionInterval(0, 0);
        } else {
            clearLinesPanel();
        }
    }

    private void loadDayChart() {
        String from = formatDate(txt_date.getDate());
        String to = formatDate(txt_tanggaldetail.getDate());
        List<DayPoint> points = new ArrayList<>();
        try {
            Calendar c = Calendar.getInstance();
            c.setTime(txt_date.getDate());
            Calendar end = Calendar.getInstance();
            end.setTime(txt_tanggaldetail.getDate());
            HashMap<String, Integer> map = new HashMap<>();
            // Same revenue definition as refreshKpis: line subtotals, non-voided,
            // scale items excluded. Vegetables have their own report view.
            ps = Koneksi.getConnection().prepareStatement(
                    "SELECT DATE(j.tanggal_penjualan) AS d, COALESCE(SUM(n.Subtotal),0) AS v "
                    + "FROM nota_penjualan n "
                    + "JOIN penjualan j ON j.penjualan_Id = n.penjualan_Id "
                    + "JOIN produk p ON p.kode_produk = n.kode_produk "
                    + "WHERE j.tanggal_penjualan BETWEEN ? AND ? "
                    + "AND j.voided = 0 AND p.is_scale = 0 "
                    + "GROUP BY DATE(j.tanggal_penjualan)");
            ps.setString(1, from);
            ps.setString(2, to + " 23:59:59");
            rs = ps.executeQuery();
            while (rs.next()) {
                map.put(rs.getString("d"), rs.getInt("v"));
            }
            while (!c.after(end)) {
                String key = formatDate(c.getTime());
                int v = map.containsKey(key) ? map.get(key) : 0;
                points.add(new DayPoint(c.getTime(), v));
                c.add(Calendar.DAY_OF_MONTH, 1);
            }
        } catch (Exception ignored) {
        }
        if (dayChart != null) {
            dayChart.setPoints(points);
        }
    }

    private void loadTopProducts() {
        String from = formatDate(txt_date.getDate());
        String to = formatDate(txt_tanggaldetail.getDate());
        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{}, new String[]{"PRODUCT", "QTY", "REVENUE"}) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        try {
            // Scale items included here on purpose, so weighed goods rank
            // alongside the rest. Quantity is decimal for those (e.g. 0.205 kg).
            ps = Koneksi.getConnection().prepareStatement(
                    "SELECT n.nama_produk, SUM(n.jumlah) AS qty, SUM(n.Subtotal) AS rev "
                    + "FROM nota_penjualan n "
                    + "JOIN penjualan j ON j.penjualan_Id = n.penjualan_Id "
                    + "JOIN produk p ON p.kode_produk = n.kode_produk "
                    + "WHERE j.tanggal_penjualan BETWEEN ? AND ? "
                    + "AND j.voided = 0 "
                    + "GROUP BY n.nama_produk ORDER BY rev DESC LIMIT 8");
            ps.setString(1, from);
            ps.setString(2, to + " 23:59:59");
            rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString(1),
                    QuantityUtil.format(rs.getBigDecimal(2), true),
                    money(rs.getInt(3))
                });
            }
        } catch (Exception e) {
            // ignore empty
        }
        tbl_topProducts.setModel(model);
        PageUI.styleTable(tbl_topProducts);
    }

    private void refreshVegetablePanel() {
        String from = formatDate(txt_date.getDate());
        String to = formatDate(txt_tanggaldetail.getDate());
        int revenue = 0;
        int expenses = 0;
        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{}, new String[]{"DATE", "PRODUCT", "WEIGHT", "AMOUNT"}) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        try {
            ps = Koneksi.getConnection().prepareStatement(
                    "SELECT COALESCE(SUM(n.Subtotal),0) "
                    + "FROM nota_penjualan n "
                    + "JOIN penjualan j ON j.penjualan_Id = n.penjualan_Id "
                    + "JOIN produk p ON p.kode_produk = n.kode_produk "
                    + "WHERE j.tanggal_penjualan BETWEEN ? AND ? AND j.voided = 0 AND p.is_scale = 1");
            ps.setString(1, from);
            ps.setString(2, to + " 23:59:59");
            rs = ps.executeQuery();
            if (rs.next()) {
                revenue = rs.getInt(1);
            }
            ps = Koneksi.getConnection().prepareStatement(
                    "SELECT DATE(j.tanggal_penjualan) AS sale_date, p.nama_produk, "
                    + "n.jumlah AS weight, n.Subtotal AS amount "
                    + "FROM nota_penjualan n "
                    + "JOIN penjualan j ON j.penjualan_Id = n.penjualan_Id "
                    + "JOIN produk p ON p.kode_produk = n.kode_produk "
                    + "WHERE j.tanggal_penjualan BETWEEN ? AND ? AND j.voided = 0 AND p.is_scale = 1 "
                    + "ORDER BY j.tanggal_penjualan DESC");
            ps.setString(1, from);
            ps.setString(2, to + " 23:59:59");
            rs = ps.executeQuery();
            SimpleDateFormat displayDate = new SimpleDateFormat("dd/MM/yyyy");
            while (rs.next()) {
                Date saleDate = rs.getDate("sale_date");
                BigDecimal weight = rs.getBigDecimal("weight");
                if (weight == null) {
                    weight = BigDecimal.ZERO;
                }
                model.addRow(new Object[]{
                    saleDate == null ? "" : displayDate.format(saleDate),
                    rs.getString("nama_produk"),
                    QuantityUtil.format(weight, true),
                    money(rs.getInt("amount"))
                });
            }
            ps = Koneksi.getConnection().prepareStatement(
                    "SELECT COALESCE(SUM(jumlah),0) FROM pengeluaran "
                    + "WHERE tanggal BETWEEN ? AND ? AND keterangan = 'Vegetables'");
            ps.setString(1, from);
            ps.setString(2, to);
            rs = ps.executeQuery();
            if (rs.next()) {
                expenses = rs.getInt(1);
            }
        } catch (Exception ignored) {
        }
        if (tbl_vegetableLines != null) {
            tbl_vegetableLines.setModel(model);
            PageUI.styleTable(tbl_vegetableLines);
        }
        if (lb_vegRevenue != null) {
            lb_vegRevenue.setText(money(revenue));
        }
        if (lb_vegExpenses != null) {
            lb_vegExpenses.setText(money(expenses));
        }
        if (lb_vegProfit != null) {
            lb_vegProfit.setText(money(revenue - expenses));
        }
    }

    private void saveVegetableExpense() {
        Date tanggal = txt_vegExpenseDate == null ? null : txt_vegExpenseDate.getDate();
        if (tanggal == null) {
            JOptionPane.showMessageDialog(this, "Expense date is required.");
            return;
        }
        int jumlah;
        try {
            jumlah = Integer.parseInt(txt_vegExpense.getText().trim().replace(",", ""));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Enter a valid amount in rupees.");
            return;
        }
        if (jumlah <= 0) {
            JOptionPane.showMessageDialog(this, "Amount must be greater than zero.");
            return;
        }
        int userId;
        try {
            userId = Integer.parseInt(user.getId().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Signed-in user is missing.");
            return;
        }
        PreparedStatement insertPs = null;
        try {
            Connection conn = Koneksi.getConnection();
            insertPs = conn.prepareStatement(
                    "INSERT INTO pengeluaran (tanggal, kategori, keterangan, jumlah, user_Id, uuid) "
                    + "VALUES (?,?,?,?,?,?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS);
            insertPs.setString(1, formatDate(tanggal));
            insertPs.setString(2, "Vegetables");
            insertPs.setString(3, "Vegetables");
            insertPs.setInt(4, jumlah);
            insertPs.setInt(5, userId);
            insertPs.setString(6, Ids.newUuid());
            insertPs.executeUpdate();
            ResultSet keys = insertPs.getGeneratedKeys();
            if (keys.next()) {
                SyncOutbox.enqueueExpenseById(keys.getInt(1));
            }
            keys.close();
            txt_vegExpense.setText("");
            refreshVegetablePanel();
            JOptionPane.showMessageDialog(this, "Vegetable expense saved.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not save expense: " + e.getMessage());
        } finally {
            if (insertPs != null) {
                try {
                    insertPs.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private JPanel makeVegKpi(String caption, JLabel value) {
        JPanel cell = new JPanel();
        cell.setOpaque(false);
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, RULE),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)));
        JLabel cap = new JLabel(caption);
        cap.setFont(UITheme.FONT_CAPTION);
        cap.setForeground(UITheme.TEXT_MUTED);
        cap.setAlignmentX(Component.LEFT_ALIGNMENT);
        value.setFont(UITheme.FONT_KPI_VALUE.deriveFont(24f));
        value.setForeground(PageUI.INK);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        cell.add(cap);
        cell.add(Box.createVerticalStrut(6));
        cell.add(value);
        return cell;
    }

    private JPanel cellPanel(boolean right, boolean bottom) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UITheme.SURFACE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, bottom ? 1 : 0, right ? 1 : 0, UITheme.GRID_LINE),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        return p;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txt_date = new com.toedter.calendar.JDateChooser();
        cari_laporan = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tbl_laporan = new javax.swing.JTable();
        btn_segarkan = new javax.swing.JButton();
        txt_tanggaldetail = new com.toedter.calendar.JDateChooser();
        lb_penjualan = new javax.swing.JLabel();
        lb_pendapatan = new javax.swing.JLabel();
        lb_keuntungan = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tbl_detailLaporan = new javax.swing.JTable();
        btn_print = new javax.swing.JButton();
        lb_status = new JLabel("0 transactions in range");
        lb_linesTitle = new JLabel("TX-————");
        lb_linesMeta = new JLabel("Select a transaction");
        lb_txTotal = new JLabel("Total  " + money(0));
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        cari_details = new javax.swing.JButton();
        Rp1 = new javax.swing.JLabel();
        Rp2 = new javax.swing.JLabel();

        setBackground(UITheme.PAGE_BG);
        setLayout(new BorderLayout());

        jPanel1.setBackground(UITheme.PAGE_BG);
        jPanel1.setLayout(new BorderLayout());

        // ---- Header ----
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.PAGE_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, RULE),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        JPanel headerLeft = new JPanel();
        headerLeft.setOpaque(false);
        headerLeft.setLayout(new BoxLayout(headerLeft, BoxLayout.Y_AXIS));
        JLabel crumb = new JLabel("REPORTS / 09");
        crumb.setFont(UITheme.FONT_CAPTION);
        crumb.setForeground(UITheme.TEXT_CAPTION);
        crumb.setAlignmentX(Component.LEFT_ALIGNMENT);
        jLabel1.setText("Sales");
        jLabel1.setFont(UITheme.FONT_HEADING.deriveFont(22f));
        jLabel1.setForeground(PageUI.INK);
        jLabel1.setAlignmentX(Component.LEFT_ALIGNMENT);
        lb_rangeSubtitle = new JLabel(" ");
        lb_rangeSubtitle.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        lb_rangeSubtitle.setForeground(UITheme.TEXT_MUTED);
        lb_rangeSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerLeft.add(crumb);
        headerLeft.add(jLabel1);
        headerLeft.add(lb_rangeSubtitle);
        header.add(headerLeft, BorderLayout.WEST);
        jPanel1.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setBackground(UITheme.PAGE_BG);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(BorderFactory.createEmptyBorder(12, 20, 16, 20));

        // Filter bar
        JPanel filterBar = new JPanel(new BorderLayout());
        filterBar.setOpaque(false);
        filterBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JPanel filterLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filterLeft.setOpaque(false);
        btn_week = new JButton("This week");
        btn_today = new JButton("Today");
        btn_month = new JButton("This month");
        btn_week.addActionListener(e -> { showReportView("sales"); applyPreset("week"); runReport(); });
        btn_today.addActionListener(e -> { showReportView("sales"); applyPreset("today"); runReport(); });
        btn_month.addActionListener(e -> { showReportView("sales"); applyPreset("month"); runReport(); });
        filterLeft.add(btn_week);
        filterLeft.add(btn_today);
        filterLeft.add(btn_month);
        btn_vegetables = new JButton("Vegetable Sales");
        btn_vegetables.addActionListener(e -> { showReportView("vegetables"); runReport(); });
        filterLeft.add(btn_vegetables);
        filterLeft.add(Box.createHorizontalStrut(8));
        txt_date.setPreferredSize(new Dimension(120, 30));
        txt_tanggaldetail.setPreferredSize(new Dimension(120, 30));
        filterLeft.add(txt_date);
        JLabel lbDash = new JLabel("—");
        lbDash.setForeground(UITheme.TEXT_MUTED);
        filterLeft.add(lbDash);
        filterLeft.add(txt_tanggaldetail);
        cari_laporan.setText("Run");
        PageUI.stylePrimaryButton(cari_laporan);
        cari_laporan.addActionListener(e -> {
            activePreset = "custom";
            stylePresetButtons();
            runReport();
        });
        filterLeft.add(cari_laporan);
        filterLeft.add(Box.createHorizontalStrut(8));
        chk_showVoided = new JCheckBox("Show voided");
        chk_showVoided.setOpaque(false);
        chk_showVoided.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        chk_showVoided.setForeground(UITheme.TEXT_MUTED);
        chk_showVoided.setFocusPainted(false);
        chk_showVoided.setSelected(false);
        chk_showVoided.addActionListener(e -> runReport());
        filterLeft.add(chk_showVoided);

        JPanel filterRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        filterRight.setOpaque(false);
        btn_export = new JButton("Export CSV");
        PageUI.styleGhostButton(btn_export);
        btn_export.setForeground(PageUI.INK);
        btn_export.addActionListener(e -> exportCsv());
        btn_print.setText("Print");
        PageUI.styleGhostButton(btn_print);
        btn_print.setForeground(PageUI.INK);
        btn_print.addActionListener(e -> btn_printActionPerformed(e));
        btn_segarkan.setVisible(false);
        cari_details.setVisible(false);
        filterRight.add(btn_export);
        filterRight.add(btn_print);

        filterBar.add(filterLeft, BorderLayout.WEST);
        filterBar.add(filterRight, BorderLayout.EAST);
        center.add(filterBar);
        center.add(Box.createVerticalStrut(12));

        mainReportPanel = new JPanel();
        mainReportPanel.setOpaque(false);
        mainReportPanel.setLayout(new BoxLayout(mainReportPanel, BoxLayout.Y_AXIS));
        mainReportPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Red KPI banner
        JPanel banner = new JPanel(new GridBagLayout());
        banner.setBackground(UITheme.ACCENT);
        banner.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 0));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel rev = new JPanel();
        rev.setOpaque(false);
        rev.setLayout(new BoxLayout(rev, BoxLayout.Y_AXIS));
        rev.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 18));
        lb_revenueCap = new JLabel("REVENUE");
        lb_revenueCap.setFont(UITheme.FONT_CAPTION);
        lb_revenueCap.setForeground(Color.WHITE);
        lb_revenueCap.setAlignmentX(Component.LEFT_ALIGNMENT);
        lb_pendapatan.setFont(UITheme.FONT_KPI_VALUE.deriveFont(36f));
        lb_pendapatan.setForeground(Color.WHITE);
        lb_pendapatan.setText(money(0));
        lb_pendapatan.setAlignmentX(Component.LEFT_ALIGNMENT);
        lb_revenueCompare = new JLabel(" ");
        lb_revenueCompare.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lb_revenueCompare.setForeground(new Color(255, 255, 255, 210));
        lb_revenueCompare.setAlignmentX(Component.LEFT_ALIGNMENT);
        rev.add(lb_revenueCap);
        rev.add(Box.createVerticalStrut(4));
        rev.add(lb_pendapatan);
        rev.add(Box.createVerticalStrut(4));
        rev.add(lb_revenueCompare);

        lb_avgBasket = new JLabel(money(0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0;
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.weightx = 0.4;
        banner.add(rev, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.2;
        banner.add(makeBannerKpi("TRANSACTIONS", lb_penjualan), gbc);
        gbc.gridx = 2;
        banner.add(makeBannerKpi("PROFIT", lb_keuntungan), gbc);
        gbc.gridx = 3;
        banner.add(makeBannerKpi("AVG BASKET", lb_avgBasket), gbc);

        mainReportPanel.add(banner);
        mainReportPanel.add(Box.createVerticalStrut(0));

        // 2×2 grid
        JPanel grid = new JPanel(new GridLayout(2, 2, 0, 0));
        grid.setBackground(UITheme.PAGE_BG);
        grid.setBorder(BorderFactory.createLineBorder(UITheme.GRID_LINE, 1));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setPreferredSize(new Dimension(10, 480));
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Revenue by day
        JPanel chartCell = cellPanel(true, true);
        JPanel chartHead = new JPanel(new BorderLayout());
        chartHead.setOpaque(false);
        JLabel lbChart = new JLabel("Revenue by day");
        lbChart.setFont(UITheme.FONT_BOLD.deriveFont(14f));
        chartHead.add(lbChart, BorderLayout.WEST);
        chartCell.add(chartHead, BorderLayout.NORTH);
        dayChart = new DayChartPanel();
        chartCell.add(dayChart, BorderLayout.CENTER);
        grid.add(chartCell);

        // Top products
        JPanel topCell = cellPanel(false, true);
        JLabel lbTop = new JLabel("Top products");
        lbTop.setFont(UITheme.FONT_BOLD.deriveFont(14f));
        topCell.add(lbTop, BorderLayout.NORTH);
        tbl_topProducts = new JTable(new DefaultTableModel(
                new Object[][]{}, new String[]{"PRODUCT", "QTY", "REVENUE"}));
        PageUI.styleTable(tbl_topProducts);
        JScrollPane spTop = new JScrollPane(tbl_topProducts);
        spTop.setBorder(BorderFactory.createEmptyBorder());
        spTop.getViewport().setBackground(UITheme.SURFACE);
        topCell.add(spTop, BorderLayout.CENTER);
        grid.add(topCell);

        // Transactions
        JPanel txCell = cellPanel(true, false);
        JPanel txNorth = new JPanel();
        txNorth.setOpaque(false);
        txNorth.setLayout(new BoxLayout(txNorth, BoxLayout.Y_AXIS));

        JPanel txHead = new JPanel(new BorderLayout());
        txHead.setOpaque(false);
        txHead.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbTx = new JLabel("Transactions");
        lbTx.setFont(UITheme.FONT_BOLD.deriveFont(14f));
        JLabel lbTxHint = new JLabel("Newest first · click a row for its lines");
        lbTxHint.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lbTxHint.setForeground(UITheme.TEXT_MUTED);
        txHead.add(lbTx, BorderLayout.WEST);
        txHead.add(lbTxHint, BorderLayout.EAST);

        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        searchRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchRow.setBorder(BorderFactory.createEmptyBorder(10, 0, 8, 0));
        searchRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        txt_txSearch = new JTextField();
        PageUI.styleField(txt_txSearch);
        txt_txSearch.putClientProperty("JTextField.placeholderText", "Search ticket, customer, cashier — or scan invoice barcode");
        txt_txSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyTxFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyTxFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyTxFilter();
            }
        });
        txt_txSearch.addActionListener(e -> onTxSearchEnter());
        JButton btnFind = new JButton("Find");
        PageUI.styleGhostButton(btnFind);
        btnFind.setForeground(PageUI.INK);
        btnFind.addActionListener(e -> onTxSearchEnter());
        searchRow.add(txt_txSearch, BorderLayout.CENTER);
        searchRow.add(btnFind, BorderLayout.EAST);

        txNorth.add(txHead);
        txNorth.add(searchRow);
        txCell.add(txNorth, BorderLayout.NORTH);
        tbl_laporan.setModel(newTxModel());
        attachTxSorter((DefaultTableModel) tbl_laporan.getModel());
        PageUI.styleTable(tbl_laporan);
        jScrollPane1.setBorder(BorderFactory.createEmptyBorder());
        jScrollPane1.setViewportView(tbl_laporan);
        txCell.add(jScrollPane1, BorderLayout.CENTER);
        lb_txFooter = new JLabel("0 transactions in range");
        lb_txFooter.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lb_txFooter.setForeground(UITheme.TEXT_MUTED);
        lb_txFooter.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        txCell.add(lb_txFooter, BorderLayout.SOUTH);
        grid.add(txCell);

        // Detail
        JPanel detailCell = cellPanel(false, false);
        JPanel detailHead = new JPanel();
        detailHead.setOpaque(false);
        detailHead.setLayout(new BoxLayout(detailHead, BoxLayout.Y_AXIS));
        lb_linesTitle.setFont(UITheme.FONT_BOLD.deriveFont(14f));
        lb_linesTitle.setForeground(PageUI.INK);
        lb_linesTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        lb_linesMeta.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lb_linesMeta.setForeground(UITheme.TEXT_MUTED);
        lb_linesMeta.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailHead.add(lb_linesTitle);
        detailHead.add(Box.createVerticalStrut(2));
        detailHead.add(lb_linesMeta);
        detailCell.add(detailHead, BorderLayout.NORTH);

        tbl_detailLaporan.setModel(newLinesModel());
        PageUI.styleTable(tbl_detailLaporan);
        jScrollPane2.setBorder(BorderFactory.createEmptyBorder());
        jScrollPane2.setViewportView(tbl_detailLaporan);
        detailCell.add(jScrollPane2, BorderLayout.CENTER);

        JPanel detailSouth = new JPanel();
        detailSouth.setOpaque(false);
        detailSouth.setLayout(new BoxLayout(detailSouth, BoxLayout.Y_AXIS));
        lb_txTotal.setFont(UITheme.FONT_BOLD.deriveFont(13f));
        lb_txTotal.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailSouth.add(lb_txTotal);
        detailSouth.add(Box.createVerticalStrut(6));
        JPanel costRow = new JPanel(new BorderLayout());
        costRow.setOpaque(false);
        costRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        costRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel lbCost = new JLabel("Cost of goods");
        lbCost.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        lbCost.setForeground(UITheme.TEXT_MUTED);
        lb_costGoods = new JLabel(money(0));
        lb_costGoods.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        costRow.add(lbCost, BorderLayout.WEST);
        costRow.add(lb_costGoods, BorderLayout.EAST);
        detailSouth.add(costRow);
        JPanel profitRow = new JPanel(new BorderLayout());
        profitRow.setOpaque(false);
        profitRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        profitRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel lbProf = new JLabel("Profit on ticket");
        lbProf.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        lbProf.setForeground(UITheme.TEXT_MUTED);
        lb_profitTicket = new JLabel(money(0));
        lb_profitTicket.setFont(UITheme.FONT_BOLD.deriveFont(12f));
        lb_profitTicket.setForeground(UITheme.ACCENT);
        profitRow.add(lbProf, BorderLayout.WEST);
        profitRow.add(lb_profitTicket, BorderLayout.EAST);
        detailSouth.add(profitRow);
        detailSouth.add(Box.createVerticalStrut(10));

        JPanel detailActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        detailActions.setOpaque(false);
        detailActions.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton btnReprint = new JButton("Reprint receipt");
        PageUI.styleGhostButton(btnReprint);
        btnReprint.setForeground(PageUI.INK);
        btnReprint.addActionListener(e -> btn_printActionPerformed(e));
        detailActions.add(btnReprint);

        // Void is owner/admin only (this screen is owner Reports).
        if (isOwnerUser()) {
            JButton btnVoid = new JButton("Void sale");
            btnVoid.setFocusPainted(false);
            btnVoid.setBorderPainted(false);
            btnVoid.setContentAreaFilled(false);
            btnVoid.setForeground(UITheme.ACCENT);
            btnVoid.setFont(UITheme.FONT_BOLD.deriveFont(12f));
            btnVoid.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnVoid.addActionListener(e -> voidSelectedSale());
            detailActions.add(btnVoid);
        }
        detailSouth.add(detailActions);
        detailCell.add(detailSouth, BorderLayout.SOUTH);
        grid.add(detailCell);

        mainReportPanel.add(grid);

        vegetablePanel = new JPanel();
        vegetablePanel.setBackground(UITheme.PAGE_BG);
        vegetablePanel.setLayout(new BoxLayout(vegetablePanel, BoxLayout.Y_AXIS));
        vegetablePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel vegKpiRow = new JPanel(new GridLayout(1, 3, 0, 0));
        vegKpiRow.setBackground(UITheme.SURFACE);
        vegKpiRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.GRID_LINE, 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        vegKpiRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        vegKpiRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        lb_vegRevenue = new JLabel(money(0));
        lb_vegExpenses = new JLabel(money(0));
        lb_vegProfit = new JLabel(money(0));
        vegKpiRow.add(makeVegKpi("VEGETABLE REVENUE", lb_vegRevenue));
        vegKpiRow.add(makeVegKpi("VEGETABLE EXPENSES", lb_vegExpenses));
        vegKpiRow.add(makeVegKpi("VEGETABLE PROFIT", lb_vegProfit));
        vegetablePanel.add(vegKpiRow);
        vegetablePanel.add(Box.createVerticalStrut(12));

        JPanel vegTableCell = cellPanel(true, true);
        JLabel lbVegTable = new JLabel("Vegetable sales detail");
        lbVegTable.setFont(UITheme.FONT_BOLD.deriveFont(14f));
        vegTableCell.add(lbVegTable, BorderLayout.NORTH);
        tbl_vegetableLines = new JTable(new DefaultTableModel(
                new Object[][]{}, new String[]{"DATE", "PRODUCT", "WEIGHT", "AMOUNT"}));
        PageUI.styleTable(tbl_vegetableLines);
        JScrollPane spVeg = new JScrollPane(tbl_vegetableLines);
        spVeg.setBorder(BorderFactory.createEmptyBorder());
        spVeg.getViewport().setBackground(UITheme.SURFACE);
        vegTableCell.add(spVeg, BorderLayout.CENTER);
        vegTableCell.setAlignmentX(Component.LEFT_ALIGNMENT);
        vegTableCell.setPreferredSize(new Dimension(10, 360));
        vegTableCell.setMaximumSize(new Dimension(Integer.MAX_VALUE, 420));
        vegetablePanel.add(vegTableCell);
        vegetablePanel.add(Box.createVerticalStrut(12));

        JPanel expensePanel = new JPanel(new BorderLayout());
        expensePanel.setOpaque(false);
        expensePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        expensePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.GRID_LINE, 1),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        expensePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));
        JLabel lbExpense = new JLabel("Daily vegetable expense");
        lbExpense.setFont(UITheme.FONT_BOLD.deriveFont(13f));
        expensePanel.add(lbExpense, BorderLayout.NORTH);
        JPanel expenseRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        expenseRow.setOpaque(false);
        txt_vegExpenseDate = new com.toedter.calendar.JDateChooser();
        txt_vegExpenseDate.setDateFormatString("dd/MM/yyyy");
        txt_vegExpenseDate.setPreferredSize(new Dimension(120, 30));
        txt_vegExpenseDate.setDate(new Date());
        expenseRow.add(txt_vegExpenseDate);
        txt_vegExpense = new JTextField(12);
        PageUI.styleField(txt_vegExpense);
        txt_vegExpense.putClientProperty("JTextField.placeholderText", "Amount (Rs)");
        expenseRow.add(txt_vegExpense);
        JButton btnSaveVegExpense = new JButton("Save expense");
        PageUI.stylePrimaryButton(btnSaveVegExpense);
        btnSaveVegExpense.addActionListener(e -> saveVegetableExpense());
        expenseRow.add(btnSaveVegExpense);
        expensePanel.add(expenseRow, BorderLayout.CENTER);
        vegetablePanel.add(expensePanel);

        reportCardLayout = new CardLayout();
        reportCard = new JPanel(reportCardLayout);
        reportCard.setOpaque(false);
        reportCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        reportCard.add(mainReportPanel, "sales");
        reportCard.add(vegetablePanel, "vegetables");
        center.add(reportCard);
        jPanel1.add(center, BorderLayout.CENTER);
        add(jPanel1, BorderLayout.CENTER);

        jLabel2.setVisible(false);
        jLabel3.setVisible(false);
        jLabel4.setVisible(false);
        jLabel5.setVisible(false);
        jLabel6.setVisible(false);
        Rp1.setVisible(false);
        Rp2.setVisible(false);
        lb_status.setVisible(false);
        stylePresetButtons();
    }// </editor-fold>//GEN-END:initComponents

    private JPanel makeBannerKpi(String caption, JLabel value) {
        JPanel cell = new JPanel();
        cell.setOpaque(false);
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(255, 255, 255, 90)),
                BorderFactory.createEmptyBorder(0, 18, 0, 18)));
        JLabel cap = new JLabel(caption);
        cap.setFont(UITheme.FONT_CAPTION);
        cap.setForeground(Color.WHITE);
        cap.setAlignmentX(Component.LEFT_ALIGNMENT);
        value.setFont(UITheme.FONT_KPI_VALUE.deriveFont(28f));
        value.setForeground(Color.WHITE);
        if (value.getText() == null || value.getText().isEmpty()) {
            value.setText("0");
        }
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        cell.add(cap);
        cell.add(Box.createVerticalStrut(8));
        cell.add(value);
        return cell;
    }

    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("sales-report.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try (FileWriter fw = new FileWriter(fc.getSelectedFile())) {
            fw.write("TICKET,DATE,CUSTOMER,PAYMENT,DELIVERY MAN,DISCOUNT,TOTAL,CASHIER\n");
            for (int i = 0; i < tbl_laporan.getRowCount(); i++) {
                fw.write(tbl_laporan.getValueAt(i, 0) + ","
                        + tbl_laporan.getValueAt(i, 1) + ","
                        + csvCell(tbl_laporan.getValueAt(i, 2)) + ","
                        + csvCell(tbl_laporan.getValueAt(i, 3)) + ","
                        + csvCell(tbl_laporan.getValueAt(i, 4)) + ","
                        + String.valueOf(tbl_laporan.getValueAt(i, 5)).replace(",", "") + ","
                        + String.valueOf(tbl_laporan.getValueAt(i, 6)).replace(",", "") + ","
                        + csvCell(tbl_laporan.getValueAt(i, 7)) + "\n");
            }
            JOptionPane.showMessageDialog(this, "CSV exported.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage());
        }
    }

    private String csvCell(Object value) {
        String s = value == null ? "" : String.valueOf(value);
        return s.replace(",", " ");
    }

    private void btn_printActionPerformed(java.awt.event.ActionEvent evt) {
        int row = tbl_laporan.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a transaction to reprint.");
            return;
        }
        String ticket = String.valueOf(tbl_laporan.getValueAt(row, 0));
        if ("TOTALS".equalsIgnoreCase(ticket)) {
            JOptionPane.showMessageDialog(this, "Select a transaction to reprint.");
            return;
        }
        try {
            int penjualanId = Integer.parseInt(stripTx(ticket).trim());
            ReceiptPrinter.printReceipt(penjualanId);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid ticket: " + ticket);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to reprint: " + e.getMessage());
        }
    }

    private static boolean isOwnerUser() {
        String jenis = user.getJenisUser();
        return "Owner".equalsIgnoreCase(jenis) || "PEMILIK".equalsIgnoreCase(jenis);
    }

    private void voidSelectedSale() {
        if (!isOwnerUser()) {
            JOptionPane.showMessageDialog(this, "Only an owner/admin can void a sale.");
            return;
        }
        int row = tbl_laporan.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a transaction to void.");
            return;
        }
        String ticket = String.valueOf(tbl_laporan.getValueAt(row, 0));
        if ("TOTALS".equalsIgnoreCase(ticket)) {
            JOptionPane.showMessageDialog(this, "Select a transaction to void.");
            return;
        }
        if (isVoidTicketLabel(ticket)) {
            JOptionPane.showMessageDialog(this, "This sale is already voided.");
            return;
        }

        int penjualanId;
        try {
            penjualanId = Integer.parseInt(stripTx(ticket).trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid ticket: " + ticket);
            return;
        }

        int voidedBy;
        try {
            voidedBy = Integer.parseInt(user.getId().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to identify the current user for void.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Void sale " + ticket + "?\nStock will be restored and this sale will be marked void.",
                "Confirm void",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }

        java.sql.Connection conn = null;
        boolean oldAuto = true;
        try {
            conn = Koneksi.getConnection();
            oldAuto = conn.getAutoCommit();
            conn.setAutoCommit(false);

            PreparedStatement restock = conn.prepareStatement(
                    "UPDATE produk p "
                    + "JOIN detail_penjualan d ON d.kode_produk = p.kode_produk "
                    + "SET p.stok_produk = p.stok_produk + d.jumlah "
                    + "WHERE d.penjualan_Id = ?");
            restock.setInt(1, penjualanId);
            restock.executeUpdate();
            restock.close();

            PreparedStatement voidSale = conn.prepareStatement(
                    "UPDATE penjualan SET voided = 1, voided_at = NOW(), voided_by = ? "
                    + "WHERE penjualan_Id = ? AND voided = 0");
            voidSale.setInt(1, voidedBy);
            voidSale.setInt(2, penjualanId);
            int updated = voidSale.executeUpdate();
            voidSale.close();

            if (updated == 0) {
                conn.rollback();
                JOptionPane.showMessageDialog(this, "Sale was not found or already voided.");
                return;
            }

            conn.commit();
            enqueueVoidedSaleForSync(penjualanId);
            JOptionPane.showMessageDialog(this, "Sale voided. Stock restored.");
            clearLinesPanel();
            runReport();
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Exception ignored) {
                }
            }
            JOptionPane.showMessageDialog(this, "Unable to void sale: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(oldAuto);
                } catch (Exception ignored) {
                }
            }
        }
    }

    /** Propagate void to cloud via outbox. Never throws / never rolls back the void. */
    private void enqueueVoidedSaleForSync(int penjualanId) {
        java.sql.Connection c = null;
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
                    + "pl.uuid AS pelanggan_uuid "
                    + "FROM penjualan p "
                    + "LEFT JOIN pelanggan pl ON p.pelanggan_Id = pl.pelanggan_Id "
                    + "WHERE p.penjualan_Id = ?");
            headerPs.setInt(1, penjualanId);
            hrs = headerPs.executeQuery();
            if (!hrs.next()) {
                return;
            }
            String saleUuid = hrs.getString("uuid");
            String tanggal = hrs.getString("tanggal_penjualan");
            int subtotalKotor = hrs.getInt("subtotal_kotor");
            int diskon = hrs.getInt("diskon");
            int totalPembayaran = hrs.getInt("Total_pembayaran");
            int uangDiterima = hrs.getInt("uang_diterima");
            int uangKembalian = hrs.getInt("uang_kembalian");
            int userId = hrs.getInt("user_Id");
            Integer metodeId = (Integer) hrs.getObject("metode_Id");
            String namaKurir = hrs.getString("nama_kurir");
            String pelangganUuid = hrs.getString("pelanggan_uuid");

            StringBuilder linesJson = new StringBuilder();
            linesJson.append('[');
            linesPs = c.prepareStatement(
                    "SELECT uuid, kode_produk, jumlah, Subtotal FROM detail_penjualan "
                    + "WHERE penjualan_Id = ?");
            linesPs.setInt(1, penjualanId);
            lrs = linesPs.executeQuery();
            boolean first = true;
            while (lrs.next()) {
                if (!first) {
                    linesJson.append(',');
                }
                first = false;
                java.math.BigDecimal qty = lrs.getBigDecimal("jumlah");
                String jumlahStr = qty.setScale(3, java.math.RoundingMode.HALF_UP).toPlainString();
                String kodeRaw = lrs.getString("kode_produk");
                linesJson.append('{')
                        .append("\"uuid\":").append(SyncService.jsonString(lrs.getString("uuid"))).append(',')
                        .append("\"kodeProduk\":").append(Long.parseLong(kodeRaw.trim())).append(',')
                        .append("\"jumlah\":").append(SyncService.jsonString(jumlahStr)).append(',')
                        .append("\"subtotal\":").append(lrs.getInt("Subtotal"))
                        .append('}');
            }
            linesJson.append(']');

            StringBuilder payload = new StringBuilder(512);
            payload.append('{')
                    .append("\"uuid\":").append(SyncService.jsonString(saleUuid)).append(',')
                    .append("\"tanggalPenjualan\":").append(SyncService.jsonString(tanggal)).append(',')
                    .append("\"subtotalKotor\":").append(subtotalKotor).append(',')
                    .append("\"diskon\":").append(diskon).append(',')
                    .append("\"totalPembayaran\":").append(totalPembayaran).append(',')
                    .append("\"uangDiterima\":").append(uangDiterima).append(',')
                    .append("\"uangKembalian\":").append(uangKembalian).append(',')
                    .append("\"userId\":").append(userId).append(',')
                    .append("\"pelangganUuid\":")
                    .append(pelangganUuid == null ? "null" : SyncService.jsonString(pelangganUuid))
                    .append(',')
                    .append("\"metodeId\":").append(metodeId == null ? "null" : metodeId.toString()).append(',')
                    .append("\"namaKurir\":")
                    .append(namaKurir == null || namaKurir.isEmpty()
                            ? "null" : SyncService.jsonString(namaKurir))
                    .append(',')
                    .append("\"voided\":1,")
                    .append("\"lines\":").append(linesJson)
                    .append('}');
            SyncService.getInstance().enqueue("sale", saleUuid, payload.toString());
        } catch (Exception e) {
            System.out.println("sync outbox void write failed: " + e);
        } finally {
            try {
                if (lrs != null) {
                    lrs.close();
                }
            } catch (Exception ignored) {
            }
            try {
                if (hrs != null) {
                    hrs.close();
                }
            } catch (Exception ignored) {
            }
            try {
                if (linesPs != null) {
                    linesPs.close();
                }
            } catch (Exception ignored) {
            }
            try {
                if (headerPs != null) {
                    headerPs.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Rp1;
    private javax.swing.JLabel Rp2;
    private javax.swing.JButton btn_segarkan;
    private javax.swing.JButton cari_details;
    private javax.swing.JButton cari_laporan;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lb_keuntungan;
    private javax.swing.JLabel lb_pendapatan;
    private javax.swing.JLabel lb_penjualan;
    private javax.swing.JTable tbl_detailLaporan;
    private javax.swing.JTable tbl_laporan;
    private com.toedter.calendar.JDateChooser txt_date;
    private com.toedter.calendar.JDateChooser txt_tanggaldetail;
    // End of variables declaration//GEN-END:variables

    private void tampilLaporan() {
        try {
            String from = formatDate(txt_date.getDate());
            String to = formatDate(txt_tanggaldetail.getDate());
            boolean showVoided = chk_showVoided != null && chk_showVoided.isSelected();

            String sql = "SELECT p.penjualan_Id, p.tanggal_penjualan, p.Total_pembayaran, "
                    + "COALESCE(p.subtotal_kotor, p.Total_pembayaran) AS subtotal_kotor, "
                    + "COALESCE(p.diskon, 0) AS diskon, p.nama_kurir, u.nama_user, "
                    + "pl.nama_pelanggan, mb.nama_metode, p.voided "
                    + "FROM penjualan p "
                    + "JOIN users u ON p.user_Id = u.user_Id "
                    + "LEFT JOIN pelanggan pl ON p.pelanggan_Id = pl.pelanggan_Id "
                    + "LEFT JOIN metode_bayar mb ON p.metode_Id = mb.metode_Id "
                    + "WHERE p.tanggal_penjualan BETWEEN ? AND ? "
                    + (showVoided ? "" : "AND p.voided = 0 ")
                    + "ORDER BY p.tanggal_penjualan DESC, p.penjualan_Id DESC";
            ps = Koneksi.getConnection().prepareStatement(sql);
            ps.setString(1, from);
            ps.setString(2, to + " 23:59:59");
            rs = ps.executeQuery();

            DefaultTableModel laporan = newTxModel();
            int totalGross = 0;
            int totalDisc = 0;
            int totalNet = 0;
            int activeCount = 0;
            while (rs.next()) {
                int id = rs.getInt("penjualan_Id");
                int gross = rs.getInt("subtotal_kotor");
                int disc = rs.getInt("diskon");
                int tot = rs.getInt("Total_pembayaran");
                boolean voided = rs.getInt("voided") == 1;
                if (!voided) {
                    totalGross += gross;
                    totalDisc += disc;
                    totalNet += tot;
                    activeCount++;
                }
                String customer = rs.getString("nama_pelanggan");
                String metode = rs.getString("nama_metode");
                String kurir = rs.getString("nama_kurir");
                String ticket = "TX-" + String.format("%04d", id);
                if (voided) {
                    ticket = ticket + " VOID";
                }
                laporan.addRow(new Object[]{
                    ticket,
                    rs.getString("tanggal_penjualan"),
                    customer == null || customer.isEmpty() ? "Walk-in" : customer,
                    metode == null ? "" : metode,
                    kurir == null ? "" : kurir,
                    money(disc),
                    money(tot),
                    rs.getString("nama_user")
                });
            }
            if (laporan.getRowCount() > 0) {
                laporan.addRow(new Object[]{
                    "TOTALS",
                    "",
                    "",
                    "Gross " + money(totalGross),
                    "",
                    money(totalDisc),
                    money(totalNet),
                    ""
                });
            }
            tbl_laporan.setModel(laporan);
            attachTxSorter(laporan);
            PageUI.styleTable(tbl_laporan);
            applyVoidRowStyle();
            if (lb_txFooter != null) {
                lb_txFooter.setText(activeCount
                        + " transactions · Gross " + money(totalGross)
                        + " · Discount " + money(totalDisc)
                        + " · Net " + money(totalNet));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please check again: " + e);
        }
    }

    private void applyVoidRowStyle() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                Object ticket = table.getModel().getValueAt(modelRow, 0);
                boolean voided = isVoidTicketLabel(String.valueOf(ticket));
                if (isSelected) {
                    c.setForeground(table.getSelectionForeground());
                } else if (voided) {
                    c.setForeground(VOID_FG);
                } else {
                    c.setForeground(table.getForeground());
                }
                return c;
            }
        };
        for (int c = 0; c < tbl_laporan.getColumnCount(); c++) {
            tbl_laporan.getColumnModel().getColumn(c).setCellRenderer(renderer);
        }
    }

    private void loadLinesForTransaction(String kode, String tgl, String kasir, String total, String deliveryMan) {
        try {
            lb_linesTitle.setText("TX-" + String.format("%04d", Integer.parseInt(kode)));
            String meta = tgl + "  ·  " + kasir;
            if (deliveryMan != null && !deliveryMan.trim().isEmpty() && !"null".equalsIgnoreCase(deliveryMan.trim())) {
                meta = meta + "  ·  Delivery man: " + deliveryMan.trim();
            }
            lb_linesMeta.setText(meta);
            int totalInt = 0;
            try {
                totalInt = Integer.parseInt(total.trim().replace(",", ""));
            } catch (Exception ignored) {
            }
            lb_txTotal.setText("Total  " + money(totalInt));

            String sql = "SELECT pr.nama_produk, dp.jumlah, dp.Subtotal "
                    + "FROM detail_penjualan dp "
                    + "JOIN produk pr ON dp.kode_produk = pr.kode_produk "
                    + "WHERE dp.penjualan_Id = ? ORDER BY dp.kode_produk";
            ps = Koneksi.getConnection().prepareStatement(sql);
            ps.setString(1, kode);
            rs = ps.executeQuery();

            DefaultTableModel laporan = newLinesModel();
            while (rs.next()) {
                laporan.addRow(new Object[]{
                    rs.getString("nama_produk"),
                    rs.getString("jumlah"),
                    formatRp(rs.getString("Subtotal"))
                });
            }
            tbl_detailLaporan.setModel(laporan);
            PageUI.styleTable(tbl_detailLaporan);

            int cost = 0;
            try {
                ps = Koneksi.getConnection().prepareStatement(
                        "SELECT COALESCE(SUM(dp.jumlah * p.harga_beli),0) "
                        + "FROM detail_penjualan dp JOIN produk p ON p.kode_produk = dp.kode_produk "
                        + "WHERE dp.penjualan_Id = ?");
                ps.setString(1, kode);
                rs = ps.executeQuery();
                if (rs.next()) {
                    cost = rs.getInt(1);
                }
            } catch (Exception ignored) {
            }
            lb_costGoods.setText(money(cost));
            lb_profitTicket.setText(money(totalInt - cost));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please check again: " + e);
        }
    }

    private static class DayPoint {
        final Date day;
        final int value;

        DayPoint(Date day, int value) {
            this.day = day;
            this.value = value;
        }
    }

    private static class DayChartPanel extends JPanel {
        private List<DayPoint> points = new ArrayList<>();

        DayChartPanel() {
            setOpaque(true);
            setBackground(UITheme.SURFACE);
            setPreferredSize(new Dimension(10, 180));
        }

        void setPoints(List<DayPoint> points) {
            this.points = points != null ? points : new ArrayList<>();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int padL = 8, padR = 8, padT = 28, padB = 28;
            int chartW = w - padL - padR;
            int chartH = h - padT - padB;
            int baseline = padT + chartH;

            g2.setColor(UITheme.GRID_LINE);
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(padL, baseline, padL + chartW, baseline);

            if (points.isEmpty()) {
                g2.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
                g2.setColor(UITheme.TEXT_MUTED);
                String msg = "No revenue in this range.";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, padT + chartH / 2);
                g2.dispose();
                return;
            }

            int max = 1;
            int peakIdx = 0;
            for (int i = 0; i < points.size(); i++) {
                if (points.get(i).value > max) {
                    max = points.get(i).value;
                    peakIdx = i;
                }
            }

            DayPoint peak = points.get(peakIdx);
            g2.setFont(UITheme.FONT_CAPTION);
            g2.setColor(UITheme.TEXT_MUTED);
            String peakLabel = "PEAK · " + new SimpleDateFormat("EEE").format(peak.day).toUpperCase(Locale.US)
                    + " · " + UITheme.CURRENCY.toUpperCase(Locale.US) + " "
                    + NumberFormat.getIntegerInstance(Locale.US).format(peak.value);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(peakLabel, w - padR - fm.stringWidth(peakLabel), 16);

            int n = points.size();
            float slot = chartW / (float) Math.max(1, n);
            int barW = Math.max(4, Math.min(28, (int) (slot * 0.55f)));
            // Month-scale: day numbers only, thinned. Short ranges keep weekday labels.
            boolean dense = n > 10;
            SimpleDateFormat dayFmt = dense
                    ? new SimpleDateFormat("d")
                    : new SimpleDateFormat("EEE d");
            g2.setFont(UITheme.FONT_CAPTION);
            FontMetrics labelFm = g2.getFontMetrics();
            int sampleW = dense
                    ? labelFm.stringWidth("28")
                    : labelFm.stringWidth("WED 28");
            int labelEvery = Math.max(1, (int) Math.ceil((sampleW + 10) / Math.max(1f, slot)));
            if (n >= 28 && labelEvery < 5) {
                labelEvery = 5;
            } else if (n >= 20 && labelEvery < 3) {
                labelEvery = 3;
            }

            java.util.BitSet labelAt = new java.util.BitSet(n);
            for (int i = 0; i < n; i += labelEvery) {
                labelAt.set(i);
            }
            labelAt.set(0);
            labelAt.set(n - 1);
            // Drop the second-to-last tick if it would crowd the final day label.
            if (n > 2) {
                String lastLb = formatAxisLabel(dayFmt, points.get(n - 1).day, dense);
                int lastTw = labelFm.stringWidth(lastLb);
                int lastCx = padL + (int) ((n - 1) * slot + slot / 2f);
                for (int i = n - 2; i >= 0; i--) {
                    if (!labelAt.get(i)) {
                        continue;
                    }
                    String prevLb = formatAxisLabel(dayFmt, points.get(i).day, dense);
                    int prevTw = labelFm.stringWidth(prevLb);
                    int prevCx = padL + (int) (i * slot + slot / 2f);
                    if (Math.abs(lastCx - prevCx) < (lastTw + prevTw) / 2 + 8) {
                        labelAt.clear(i);
                    }
                    break;
                }
            }

            Calendar today = Calendar.getInstance();
            for (int i = 0; i < n; i++) {
                DayPoint p = points.get(i);
                int barH = (int) ((p.value / (double) max) * (chartH - 8));
                int x = padL + (int) (i * slot + (slot - barW) / 2f);
                int y = baseline - Math.max(barH, p.value > 0 ? 2 : 0);
                boolean isToday = isSameDay(p.day, today.getTime());
                if (i == peakIdx && p.value > 0) {
                    g2.setColor(UITheme.ACCENT);
                } else if (isToday) {
                    g2.setColor(new Color(0xC8C8C4));
                } else {
                    g2.setColor(PageUI.INK);
                }
                g2.fillRect(x, y, barW, Math.max(barH, p.value > 0 ? 2 : 0));

                if (!labelAt.get(i)) {
                    continue;
                }
                String lb = formatAxisLabel(dayFmt, p.day, dense);
                int tw = labelFm.stringWidth(lb);
                g2.setColor(UITheme.TEXT_MUTED);
                g2.drawString(lb, x + barW / 2 - tw / 2, baseline + 16);
            }
            g2.dispose();
        }

        private static String formatAxisLabel(SimpleDateFormat fmt, Date day, boolean dense) {
            String lb = fmt.format(day);
            return dense ? lb : lb.toUpperCase(Locale.US);
        }

        private boolean isSameDay(Date a, Date b) {
            Calendar ca = Calendar.getInstance();
            ca.setTime(a);
            Calendar cb = Calendar.getInstance();
            cb.setTime(b);
            return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
                    && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR);
        }
    }
}
