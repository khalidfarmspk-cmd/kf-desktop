/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Main;

import config.Koneksi;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author Lenovo
 */
public class Form_ReportKaryawan extends javax.swing.JPanel {

    PreparedStatement ps;
    ResultSet rs;
    Date tanggal = new Date();

    private JLabel lb_status;
    private JLabel lb_linesTitle;
    private JLabel lb_linesMeta;
    private JLabel lb_txTotal;
    private javax.swing.JButton btn_print;
    private JTextField txt_txSearch;
    private TableRowSorter<DefaultTableModel> txSorter;

    public Form_ReportKaryawan() {
        initComponents();
        Main.PageUI.paintPage(this);
        Main.PageUI.stylePrimaryButton(cari_laporan);
        Main.PageUI.styleGhostButton(btn_segarkan);
        Main.PageUI.styleGhostButton(btn_print);
        Main.PageUI.styleTable(tbl_laporan);
        Main.PageUI.styleTable(tbl_detailLaporan);
        Main.PageUI.styleScroll(jScrollPane1);
        Main.PageUI.styleScroll(jScrollPane2);

        formatTanggal();
        txt_date.setDate(tanggal);
        txt_tanggaldetail.setDate(tanggal);
        wireSelection();
        runReport();
    }

    private void formatTanggal() {
        txt_tanggaldetail.setDateFormatString("yyyy-MM-dd");
        txt_date.setDateFormatString("yyyy-MM-dd");
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
                String kode = String.valueOf(tbl_laporan.getValueAt(row, 0));
                String tgl = String.valueOf(tbl_laporan.getValueAt(row, 1));
                String delivery = String.valueOf(tbl_laporan.getValueAt(row, 4));
                String total = String.valueOf(tbl_laporan.getValueAt(row, 6));
                String kasir = String.valueOf(tbl_laporan.getValueAt(row, 7));
                loadLinesForTransaction(kode, tgl, kasir, total, delivery);
            }
        });
    }

    private void clearLinesPanel() {
        lb_linesTitle.setText("LINES — select a transaction");
        lb_linesMeta.setText(" ");
        lb_txTotal.setText("Total  " + Main.UITheme.CURRENCY + " 0");
        DefaultTableModel empty = newLinesModel();
        tbl_detailLaporan.setModel(empty);
        Main.PageUI.styleTable(tbl_detailLaporan);
    }

    private DefaultTableModel newLinesModel() {
        return new DefaultTableModel(
            new Object[][]{},
            new String[]{"CODE", "PRODUCT", "QTY", "SUBTOTAL"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private DefaultTableModel newTxModel() {
        return new DefaultTableModel(
            new Object[][]{},
            new String[]{"CODE", "DATE", "CUSTOMER", "PAYMENT", "DELIVERY MAN", "DISCOUNT", "TOTAL", "CASHIER"}
        ) {
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

    private String formatRp(String amount) {
        try {
            String raw = amount.trim().replace(Main.UITheme.CURRENCY, "").replace(",", "").trim();
            int n = Integer.parseInt(raw);
            return Main.UITheme.CURRENCY + " " + NumberFormat.getIntegerInstance(Locale.US).format(n);
        } catch (Exception e) {
            return Main.UITheme.CURRENCY + " " + amount;
        }
    }

    private void runReport() {
        tampilLaporan();
        int count = tbl_laporan.getRowCount();
        if (count > 0 && "TOTALS".equals(String.valueOf(tbl_laporan.getValueAt(count - 1, 0)))) {
            count--;
        }
        if (count > 0) {
            tbl_laporan.setRowSelectionInterval(0, 0);
        } else {
            clearLinesPanel();
        }
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
        jLabel2 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        btn_segarkan = new javax.swing.JButton();
        txt_tanggaldetail = new com.toedter.calendar.JDateChooser();
        cari_details = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        tbl_detailLaporan = new javax.swing.JTable();
        btn_print = new javax.swing.JButton();
        lb_status = new JLabel("0 transactions in range");
        lb_linesTitle = new JLabel("LINES — select a transaction");
        lb_linesMeta = new JLabel(" ");
        lb_txTotal = new JLabel("Total  " + Main.UITheme.CURRENCY + " 0");

        setPreferredSize(new Dimension(1030, 590));
        setBackground(UITheme.PAGE_BG);
        setLayout(new BorderLayout());

        jPanel1.setBackground(UITheme.PAGE_BG);
        jPanel1.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));
        jPanel1.setLayout(new BoxLayout(jPanel1, BoxLayout.Y_AXIS));

        // ---- Header ----
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

        JPanel headerLeft = new JPanel();
        headerLeft.setOpaque(false);
        headerLeft.setLayout(new BoxLayout(headerLeft, BoxLayout.Y_AXIS));

        JLabel crumb = new JLabel("REPORTS / 09");
        crumb.setFont(UITheme.FONT_CAPTION);
        crumb.setForeground(UITheme.TEXT_CAPTION);
        crumb.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerLeft.add(crumb);

        jLabel1.setFont(UITheme.FONT_HEADING.deriveFont(28f));
        jLabel1.setForeground(PageUI.INK);
        jLabel1.setText("Today's sales");
        jLabel1.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerLeft.add(jLabel1);

        lb_status.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lb_status.setForeground(UITheme.TEXT_MUTED);
        lb_status.setHorizontalAlignment(SwingConstants.RIGHT);
        lb_status.setVerticalAlignment(SwingConstants.BOTTOM);

        header.add(headerLeft, BorderLayout.WEST);
        header.add(lb_status, BorderLayout.EAST);
        jPanel1.add(header);
        jPanel1.add(Box.createVerticalStrut(14));

        // ---- Filter bar (staff: today only — their own tickets) ----
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterBar.setBackground(UITheme.PAGE_BG);
        filterBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel lbToday = new JLabel("Today only · all cashiers");
        lbToday.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        lbToday.setForeground(UITheme.TEXT_MUTED);
        filterBar.add(lbToday);

        // Keep date choosers in sync for any legacy handlers, but hide them.
        Date now = new Date();
        txt_date.setDate(now);
        txt_tanggaldetail.setDate(now);
        txt_date.setVisible(false);
        txt_tanggaldetail.setVisible(false);

        cari_laporan.setText("Refresh");
        cari_laporan.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cari_laporan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cari_laporanActionPerformed(evt);
            }
        });
        filterBar.add(cari_laporan);

        btn_segarkan.setText("Refresh");
        btn_segarkan.setVisible(false);

        btn_print.setText("Reprint");
        btn_print.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn_print.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_printActionPerformed(evt);
            }
        });
        filterBar.add(btn_print);

        // Legacy unused controls
        jLabel2.setVisible(false);
        jLabel4.setVisible(false);
        cari_details.setVisible(false);

        jPanel1.add(filterBar);
        jPanel1.add(Box.createVerticalStrut(14));

        // ---- Master-detail (no owner KPI row) ----
        JPanel masterDetail = new JPanel(new GridLayout(1, 2, 0, 0));
        masterDetail.setBackground(UITheme.PAGE_BG);
        masterDetail.setBorder(BorderFactory.createLineBorder(UITheme.GRID_LINE, 1));
        masterDetail.setAlignmentX(Component.LEFT_ALIGNMENT);
        masterDetail.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel txPanel = new JPanel(new BorderLayout());
        txPanel.setBackground(UITheme.SURFACE);
        txPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UITheme.GRID_LINE));

        JPanel txHeader = new JPanel();
        txHeader.setBackground(UITheme.SURFACE);
        txHeader.setLayout(new BoxLayout(txHeader, BoxLayout.Y_AXIS));
        txHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.GRID_LINE),
            BorderFactory.createEmptyBorder(12, 14, 10, 14)));

        JPanel txTitleRow = new JPanel(new BorderLayout());
        txTitleRow.setOpaque(false);
        txTitleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbTxTitle = new JLabel("TRANSACTIONS");
        lbTxTitle.setFont(UITheme.FONT_CAPTION);
        lbTxTitle.setForeground(UITheme.TEXT_CAPTION);
        txTitleRow.add(lbTxTitle, BorderLayout.WEST);

        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        searchRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        searchRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txt_txSearch = new JTextField();
        Main.PageUI.styleField(txt_txSearch);
        txt_txSearch.putClientProperty("JTextField.placeholderText", "Search ticket, customer — or scan invoice barcode");
        txt_txSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyTxFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyTxFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyTxFilter(); }
        });
        txt_txSearch.addActionListener(e -> onTxSearchEnter());
        JButton btnFind = new JButton("Find");
        Main.PageUI.styleGhostButton(btnFind);
        btnFind.addActionListener(e -> onTxSearchEnter());
        searchRow.add(txt_txSearch, BorderLayout.CENTER);
        searchRow.add(btnFind, BorderLayout.EAST);

        txHeader.add(txTitleRow);
        txHeader.add(searchRow);
        txPanel.add(txHeader, BorderLayout.NORTH);

        tbl_laporan.setModel(newTxModel());
        attachTxSorter((DefaultTableModel) tbl_laporan.getModel());
        jScrollPane1.setBorder(BorderFactory.createEmptyBorder());
        jScrollPane1.setViewportView(tbl_laporan);
        txPanel.add(jScrollPane1, BorderLayout.CENTER);
        masterDetail.add(txPanel);

        JPanel linesPanel = new JPanel(new BorderLayout());
        linesPanel.setBackground(UITheme.SURFACE);

        JPanel linesHeader = new JPanel();
        linesHeader.setOpaque(false);
        linesHeader.setLayout(new BoxLayout(linesHeader, BoxLayout.Y_AXIS));
        linesHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.GRID_LINE),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        lb_linesTitle.setFont(UITheme.FONT_CAPTION);
        lb_linesTitle.setForeground(UITheme.TEXT_CAPTION);
        lb_linesTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        linesHeader.add(lb_linesTitle);
        linesHeader.add(Box.createVerticalStrut(4));

        lb_linesMeta.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lb_linesMeta.setForeground(UITheme.TEXT_MUTED);
        lb_linesMeta.setAlignmentX(Component.LEFT_ALIGNMENT);
        linesHeader.add(lb_linesMeta);
        linesPanel.add(linesHeader, BorderLayout.NORTH);

        tbl_detailLaporan.setModel(newLinesModel());
        jScrollPane2.setBorder(BorderFactory.createEmptyBorder());
        jScrollPane2.setViewportView(tbl_detailLaporan);
        linesPanel.add(jScrollPane2, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UITheme.SURFACE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.GRID_LINE),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        lb_txTotal.setFont(UITheme.FONT_BOLD.deriveFont(13f));
        lb_txTotal.setForeground(PageUI.INK);
        lb_txTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        footer.add(lb_txTotal, BorderLayout.EAST);
        linesPanel.add(footer, BorderLayout.SOUTH);

        masterDetail.add(linesPanel);
        jPanel1.add(masterDetail);

        add(jPanel1, BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void cari_laporanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cari_laporanActionPerformed
        runReport();
    }//GEN-LAST:event_cari_laporanActionPerformed

    private void cari_detailsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cari_detailsActionPerformed
        runReport();
    }//GEN-LAST:event_cari_detailsActionPerformed

    private void btn_segarkanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_segarkanActionPerformed
        txt_date.setDate(tanggal);
        txt_tanggaldetail.setDate(tanggal);
        runReport();
    }//GEN-LAST:event_btn_segarkanActionPerformed

    private void btn_printActionPerformed(java.awt.event.ActionEvent evt) {
        int row = tbl_laporan.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a transaction to reprint.");
            return;
        }
        String kode = String.valueOf(tbl_laporan.getValueAt(row, 0));
        if ("TOTALS".equals(kode)) {
            JOptionPane.showMessageDialog(this, "Select a transaction to reprint.");
            return;
        }
        try {
            int penjualanId = Integer.parseInt(kode.trim());
            ReceiptPrinter.printReceipt(penjualanId);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid ticket: " + kode);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to reprint: " + e.getMessage());
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_segarkan;
    private javax.swing.JButton cari_details;
    private javax.swing.JButton cari_laporan;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable tbl_detailLaporan;
    private javax.swing.JTable tbl_laporan;
    private com.toedter.calendar.JDateChooser txt_date;
    private com.toedter.calendar.JDateChooser txt_tanggaldetail;
    // End of variables declaration//GEN-END:variables

    private void tampilLaporan() {
        try {
            String sql = "SELECT p.penjualan_Id, p.tanggal_penjualan, p.Total_pembayaran, "
                + "COALESCE(p.subtotal_kotor, p.Total_pembayaran) AS subtotal_kotor, "
                + "COALESCE(p.diskon, 0) AS diskon, p.nama_kurir, u.nama_user, "
                + "pl.nama_pelanggan, mb.nama_metode "
                + "FROM penjualan p "
                + "JOIN users u ON p.user_Id = u.user_Id "
                + "LEFT JOIN pelanggan pl ON p.pelanggan_Id = pl.pelanggan_Id "
                + "LEFT JOIN metode_bayar mb ON p.metode_Id = mb.metode_Id "
                + "WHERE DATE(p.tanggal_penjualan) = CURDATE() "
                + "AND p.voided = 0 "
                + "ORDER BY p.tanggal_penjualan DESC, p.penjualan_Id DESC";
            ps = Koneksi.getConnection().prepareStatement(sql);
            rs = ps.executeQuery();

            DefaultTableModel laporan = newTxModel();
            int totalGross = 0;
            int totalDisc = 0;
            int totalNet = 0;
            while (rs.next()) {
                int gross = rs.getInt("subtotal_kotor");
                int disc = rs.getInt("diskon");
                int tot = rs.getInt("Total_pembayaran");
                totalGross += gross;
                totalDisc += disc;
                totalNet += tot;
                String customer = rs.getString("nama_pelanggan");
                String metode = rs.getString("nama_metode");
                String kurir = rs.getString("nama_kurir");
                laporan.addRow(new Object[]{
                    rs.getString("penjualan_Id"),
                    rs.getString("tanggal_penjualan"),
                    customer == null || customer.isEmpty() ? "Walk-in" : customer,
                    metode == null ? "" : metode,
                    kurir == null ? "" : kurir,
                    formatRp(Integer.toString(disc)),
                    formatRp(Integer.toString(tot)),
                    rs.getString("nama_user")
                });
            }
            if (laporan.getRowCount() > 0) {
                laporan.addRow(new Object[]{
                    "TOTALS",
                    "",
                    "",
                    "Gross " + formatRp(Integer.toString(totalGross)),
                    "",
                    formatRp(Integer.toString(totalDisc)),
                    formatRp(Integer.toString(totalNet)),
                    ""
                });
            }
            tbl_laporan.setModel(laporan);
            attachTxSorter(laporan);
            Main.PageUI.styleTable(tbl_laporan);
            if (lb_status != null) {
                int txCount = laporan.getRowCount() > 0 ? laporan.getRowCount() - 1 : 0;
                lb_status.setText(txCount + " transactions · Gross " + formatRp(Integer.toString(totalGross))
                        + " · Discount " + formatRp(Integer.toString(totalDisc))
                        + " · Net " + formatRp(Integer.toString(totalNet)));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please check again: " + e);
        }
    }

    private void loadLinesForTransaction(String kode, String tgl, String kasir, String total, String deliveryMan) {
        try {
            lb_linesTitle.setText("LINES — TX-" + kode);
            String meta = tgl + "  ·  " + kasir;
            if (deliveryMan != null && !deliveryMan.trim().isEmpty() && !"null".equalsIgnoreCase(deliveryMan.trim())) {
                meta = meta + "  ·  Delivery man: " + deliveryMan.trim();
            }
            lb_linesMeta.setText(meta);
            lb_txTotal.setText("Total  " + formatRp(total));

            String sql = "SELECT kode_produk, nama_produk, jumlah, Subtotal FROM nota_penjualan WHERE penjualan_Id = ? ORDER BY kode_produk";
            ps = Koneksi.getConnection().prepareStatement(sql);
            ps.setString(1, kode);
            rs = ps.executeQuery();

            DefaultTableModel laporan = newLinesModel();
            while (rs.next()) {
                laporan.addRow(new Object[]{
                    rs.getString("kode_produk"),
                    rs.getString("nama_produk"),
                    rs.getString("jumlah"),
                    rs.getString("Subtotal")
                });
            }
            tbl_detailLaporan.setModel(laporan);
            Main.PageUI.styleTable(tbl_detailLaporan);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please check again: " + e);
        }
    }

    private void attachTxSorter(DefaultTableModel model) {
        txSorter = new TableRowSorter<DefaultTableModel>(model);
        tbl_laporan.setRowSorter(txSorter);
        applyTxFilter();
    }

    private String stripTicket(String ticket) {
        if (ticket == null) {
            return "";
        }
        String t = ticket.trim();
        if (t.regionMatches(true, 0, "TX-", 0, 3)) {
            t = t.substring(3);
        } else if (t.regionMatches(true, 0, "INV-", 0, 4)) {
            t = t.substring(4);
        }
        return t.replaceFirst("^0+(?!$)", "");
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
        final String idNeedle = stripTicket(q);
        txSorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                Object ticketObj = entry.getValue(0);
                String ticket = ticketObj == null ? "" : String.valueOf(ticketObj);
                if ("TOTALS".equalsIgnoreCase(ticket)) {
                    return false;
                }
                if (!idNeedle.isEmpty() && idNeedle.equals(stripTicket(ticket))) {
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
        String id = stripTicket(q);
        if (id.isEmpty()) {
            return false;
        }
        DefaultTableModel model = (DefaultTableModel) tbl_laporan.getModel();
        for (int m = 0; m < model.getRowCount(); m++) {
            String ticket = String.valueOf(model.getValueAt(m, 0));
            if ("TOTALS".equalsIgnoreCase(ticket)) {
                continue;
            }
            if (id.equals(stripTicket(ticket)) || ticket.equalsIgnoreCase(q.trim())) {
                if (txSorter != null) {
                    txSorter.setRowFilter(null);
                }
                int view = tbl_laporan.convertRowIndexToView(m);
                if (view < 0) {
                    return false;
                }
                tbl_laporan.setRowSelectionInterval(view, view);
                tbl_laporan.scrollRectToVisible(tbl_laporan.getCellRect(view, 0, true));
                return true;
            }
        }
        return false;
    }

    private void tampilDetailLaporan() {
        int row = tbl_laporan.getSelectedRow();
        if (row >= 0) {
            String kode = String.valueOf(tbl_laporan.getValueAt(row, 0));
            if ("TOTALS".equals(kode)) {
                clearLinesPanel();
                return;
            }
            String tgl = String.valueOf(tbl_laporan.getValueAt(row, 1));
            String delivery = String.valueOf(tbl_laporan.getValueAt(row, 4));
            String total = String.valueOf(tbl_laporan.getValueAt(row, 6));
            String kasir = String.valueOf(tbl_laporan.getValueAt(row, 7));
            loadLinesForTransaction(kode, tgl, kasir, total, delivery);
        } else {
            clearLinesPanel();
        }
    }

}
