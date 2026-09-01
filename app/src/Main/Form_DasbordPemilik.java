/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Main;

import config.Koneksi;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * Owner dashboard — editorial "Today at a glance" layout matching the
 * coral KPI banner + 2×2 attention grid reference.
 */
public class Form_DasbordPemilik extends javax.swing.JPanel {

    PreparedStatement ps;
    ResultSet rs;
    Date tanggal = new Date();

    private javax.swing.JLabel lb_attnLow;
    private javax.swing.JLabel lb_attnPrice;
    private javax.swing.JLabel lb_attnSupplier;
    private javax.swing.JLabel lb_attnRestock;
    private javax.swing.JLabel lb_favoritFooter;
    private javax.swing.JLabel lb_laporanEmpty;
    private javax.swing.JLabel lb_bannerMeta;
    private HourChartPanel hourChart;
    private javax.swing.JPanel pn_favoritCards;
    private javax.swing.JPanel pn_laporanCards;

    public Form_DasbordPemilik() {
        initComponents();

        totalPenjualan();
        totalKeuntungan();
        totalProduk();
        totalPendapatan();
        needsAttention();
        terlaris();
        tampilLaporan();
        loadHourlySales();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        lb_totalPenjualan = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        lb_totalKeuntungan = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        lb_totalProduk = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        lb_pendapatan = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        txt_stok = new javax.swing.JTextField();
        btn_caristok = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        txt_terlaris = new javax.swing.JTextField();
        btn_cariterlaris = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        tb_favorit = new javax.swing.JTable();
        tb_laporan = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        jScrollPane3 = new javax.swing.JScrollPane();
        // Kept for field compatibility with older NetBeans form bindings.
        totalBarang = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        p_notifikasi = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tb_stok = new javax.swing.JTable();

        totalBarang.setVisible(false);
        jLabel7.setVisible(false);
        p_notifikasi.setVisible(false);
        jScrollPane1.setVisible(false);
        tb_stok.setVisible(false);

        setLayout(new java.awt.BorderLayout());
        setBackground(UITheme.PAGE_BG);

        jPanel1.setBackground(UITheme.PAGE_BG);
        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(18, 0, 0, 0));
        jPanel1.setLayout(new java.awt.BorderLayout());

        // ---- Header ----
        javax.swing.JPanel header = new javax.swing.JPanel(new java.awt.BorderLayout());
        header.setBackground(UITheme.PAGE_BG);
        header.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 24, 16, 24));

        javax.swing.JPanel headerLeft = new javax.swing.JPanel();
        headerLeft.setOpaque(false);
        headerLeft.setLayout(new javax.swing.BoxLayout(headerLeft, javax.swing.BoxLayout.Y_AXIS));

        javax.swing.JLabel lb_breadcrumb = new javax.swing.JLabel("OVERVIEW / 01");
        lb_breadcrumb.setFont(UITheme.FONT_CAPTION);
        lb_breadcrumb.setForeground(UITheme.TEXT_CAPTION);
        lb_breadcrumb.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        headerLeft.add(lb_breadcrumb);

        jLabel1.setFont(UITheme.FONT_HEADING.deriveFont(34f));
        jLabel1.setForeground(new Color(0x1A1A1A));
        jLabel1.setText("Today at a glance");
        jLabel1.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        headerLeft.add(jLabel1);

        javax.swing.JPanel headerActions = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);
        headerActions.setBorder(javax.swing.BorderFactory.createEmptyBorder(18, 0, 0, 0));

        javax.swing.JButton btnNewSale = new javax.swing.JButton("New sale");
        PageUI.stylePrimaryButton(btnNewSale);
        btnNewSale.addActionListener(e -> openShellPage("Sell"));
        headerActions.add(btnNewSale);

        javax.swing.JButton btnRestock = new javax.swing.JButton("Restock");
        PageUI.styleGhostButton(btnRestock);
        btnRestock.setForeground(new Color(0x1A1A1A));
        btnRestock.addActionListener(e -> openShellPage("Restock"));
        headerActions.add(btnRestock);

        javax.swing.JButton btnReports = new javax.swing.JButton("Reports");
        PageUI.styleGhostButton(btnReports);
        btnReports.setForeground(new Color(0x1A1A1A));
        btnReports.addActionListener(e -> openShellPage("Reports"));
        headerActions.add(btnReports);

        header.add(headerLeft, java.awt.BorderLayout.WEST);
        header.add(headerActions, java.awt.BorderLayout.EAST);
        jPanel1.add(header, java.awt.BorderLayout.NORTH);

        // ---- Body ----
        javax.swing.JPanel body = new javax.swing.JPanel();
        body.setBackground(UITheme.PAGE_BG);
        body.setLayout(new javax.swing.BoxLayout(body, javax.swing.BoxLayout.Y_AXIS));
        body.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 24, 24, 24));

        // Coral KPI banner
        jPanel5.setBackground(UITheme.ACCENT);
        jPanel5.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 24, 22, 0));
        jPanel5.setLayout(new java.awt.GridBagLayout());
        jPanel5.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        jPanel5.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.fill = java.awt.GridBagConstraints.BOTH;
        gbc.gridy = 0;
        gbc.weighty = 1;

        javax.swing.JPanel revenueCell = bannerCell(false);
        jLabel12.setFont(UITheme.FONT_CAPTION);
        jLabel12.setForeground(Color.WHITE);
        jLabel12.setText("REVENUE TODAY");
        jLabel12.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        revenueCell.add(jLabel12);
        revenueCell.add(javax.swing.Box.createVerticalStrut(6));

        javax.swing.JPanel revenueValueRow = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
        revenueValueRow.setOpaque(false);
        revenueValueRow.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        javax.swing.JLabel lb_rpPrefix = new javax.swing.JLabel(UITheme.CURRENCY);
        lb_rpPrefix.setFont(UITheme.FONT_KPI_VALUE.deriveFont(42f));
        lb_rpPrefix.setForeground(Color.WHITE);
        revenueValueRow.add(lb_rpPrefix);
        lb_pendapatan.setFont(UITheme.FONT_KPI_VALUE.deriveFont(42f));
        lb_pendapatan.setForeground(Color.WHITE);
        lb_pendapatan.setText("0");
        revenueValueRow.add(lb_pendapatan);
        revenueCell.add(revenueValueRow);

        lb_bannerMeta = new javax.swing.JLabel("Shift 1 · store closes 21:00 · updated "
                + new SimpleDateFormat("HH:mm").format(new Date()));
        lb_bannerMeta.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lb_bannerMeta.setForeground(new Color(255, 255, 255, 210));
        lb_bannerMeta.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        revenueCell.add(javax.swing.Box.createVerticalStrut(4));
        revenueCell.add(lb_bannerMeta);

        gbc.gridx = 0;
        gbc.weightx = 0.42;
        jPanel5.add(revenueCell, gbc);

        jPanel2.setOpaque(false);
        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.Y_AXIS));
        jPanel2.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(255, 255, 255, 90)),
                javax.swing.BorderFactory.createEmptyBorder(0, 22, 0, 18)));
        jLabel8.setFont(UITheme.FONT_CAPTION);
        jLabel8.setForeground(Color.WHITE);
        jLabel8.setText("TRANSACTIONS");
        jLabel8.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        jPanel2.add(jLabel8);
        jPanel2.add(javax.swing.Box.createVerticalStrut(8));
        lb_totalPenjualan.setFont(UITheme.FONT_KPI_VALUE.deriveFont(36f));
        lb_totalPenjualan.setForeground(Color.WHITE);
        lb_totalPenjualan.setText("0");
        lb_totalPenjualan.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        jPanel2.add(lb_totalPenjualan);
        gbc.gridx = 1;
        gbc.weightx = 0.18;
        jPanel5.add(jPanel2, gbc);

        jPanel3.setOpaque(false);
        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.Y_AXIS));
        jPanel3.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(255, 255, 255, 90)),
                javax.swing.BorderFactory.createEmptyBorder(0, 22, 0, 18)));
        jLabel9.setFont(UITheme.FONT_CAPTION);
        jLabel9.setForeground(Color.WHITE);
        jLabel9.setText("PROFIT");
        jLabel9.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        jPanel3.add(jLabel9);
        jPanel3.add(javax.swing.Box.createVerticalStrut(8));
        javax.swing.JPanel profitRow = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        profitRow.setOpaque(false);
        profitRow.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        jLabel2.setFont(UITheme.FONT_KPI_VALUE.deriveFont(36f));
        jLabel2.setForeground(Color.WHITE);
        jLabel2.setText(UITheme.CURRENCY);
        lb_totalKeuntungan.setFont(UITheme.FONT_KPI_VALUE.deriveFont(36f));
        lb_totalKeuntungan.setForeground(Color.WHITE);
        lb_totalKeuntungan.setText("0");
        profitRow.add(jLabel2);
        profitRow.add(lb_totalKeuntungan);
        jPanel3.add(profitRow);
        gbc.gridx = 2;
        gbc.weightx = 0.22;
        jPanel5.add(jPanel3, gbc);

        jPanel4.setOpaque(false);
        jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.Y_AXIS));
        jPanel4.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(255, 255, 255, 90)),
                javax.swing.BorderFactory.createEmptyBorder(0, 22, 0, 22)));
        jLabel10.setFont(UITheme.FONT_CAPTION);
        jLabel10.setForeground(Color.WHITE);
        jLabel10.setText("SELLABLE ITEMS");
        jLabel10.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        jPanel4.add(jLabel10);
        jPanel4.add(javax.swing.Box.createVerticalStrut(8));
        lb_totalProduk.setFont(UITheme.FONT_KPI_VALUE.deriveFont(36f));
        lb_totalProduk.setForeground(Color.WHITE);
        lb_totalProduk.setText("0");
        lb_totalProduk.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        jPanel4.add(lb_totalProduk);
        gbc.gridx = 3;
        gbc.weightx = 0.18;
        jPanel5.add(jPanel4, gbc);

        body.add(jPanel5);
        body.add(javax.swing.Box.createVerticalStrut(0));

        // 2×2 editorial grid
        javax.swing.JPanel gridShell = new javax.swing.JPanel(new java.awt.GridLayout(2, 2, 0, 0));
        gridShell.setBackground(UITheme.PAGE_BG);
        gridShell.setBorder(javax.swing.BorderFactory.createLineBorder(UITheme.GRID_LINE, 1));
        gridShell.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        gridShell.setPreferredSize(new Dimension(10, 460));
        gridShell.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // --- Sales by hour ---
        javax.swing.JPanel hourPanel = listCell(true, true);
        javax.swing.JPanel hourHeader = sectionHeader("Sales by hour", null);
        javax.swing.JLabel lbHours = new javax.swing.JLabel("08:00 — 21:00");
        lbHours.setFont(UITheme.FONT_CAPTION);
        lbHours.setForeground(UITheme.TEXT_MUTED);
        hourHeader.add(lbHours, java.awt.BorderLayout.EAST);
        hourPanel.add(hourHeader, java.awt.BorderLayout.NORTH);
        hourChart = new HourChartPanel();
        hourPanel.add(hourChart, java.awt.BorderLayout.CENTER);
        gridShell.add(hourPanel);

        // --- Needs attention ---
        javax.swing.JPanel attnPanel = listCell(false, true);
        javax.swing.JPanel attnHeader = sectionHeader("Needs attention", null);
        javax.swing.JPanel attnFilter = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
        attnFilter.setOpaque(false);
        jLabel4.setFont(UITheme.FONT_CAPTION);
        jLabel4.setForeground(UITheme.TEXT_CAPTION);
        jLabel4.setText("STOCK BELOW");
        attnFilter.add(jLabel4);
        styleFilterField(txt_stok, "3");
        attnFilter.add(txt_stok);
        styleApplyButton(btn_caristok);
        btn_caristok.addActionListener(evt -> needsAttention());
        attnFilter.add(btn_caristok);
        attnHeader.add(attnFilter, java.awt.BorderLayout.EAST);
        attnPanel.add(attnHeader, java.awt.BorderLayout.NORTH);

        javax.swing.JPanel attnList = new javax.swing.JPanel();
        attnList.setOpaque(false);
        attnList.setLayout(new javax.swing.BoxLayout(attnList, javax.swing.BoxLayout.Y_AXIS));
        lb_attnLow = attentionRow(attnList, "Products under threshold", false);
        lb_attnPrice = attentionRow(attnList, "Products with no price set", false);
        lb_attnSupplier = attentionRow(attnList, "Products missing a supplier", true);
        lb_attnRestock = attentionRow(attnList, "Open restock orders", false);

        javax.swing.JLabel lbFixRestock = linkLabel("Fix in Restock →");
        lbFixRestock.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                openShellPage("Restock");
            }
        });
        lbFixRestock.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        attnList.add(javax.swing.Box.createVerticalStrut(12));
        attnList.add(lbFixRestock);

        attnPanel.add(attnList, java.awt.BorderLayout.CENTER);
        gridShell.add(attnPanel);

        // --- Best sellers ---
        javax.swing.JPanel favoritPanel = listCell(true, false);
        javax.swing.JPanel favoritHeader = sectionHeader("Best sellers", null);
        javax.swing.JPanel favoritFilter = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
        favoritFilter.setOpaque(false);
        jLabel11.setFont(UITheme.FONT_CAPTION);
        jLabel11.setForeground(UITheme.TEXT_CAPTION);
        jLabel11.setText("ROWS");
        favoritFilter.add(jLabel11);
        styleFilterField(txt_terlaris, "5");
        favoritFilter.add(txt_terlaris);
        styleApplyButton(btn_cariterlaris);
        btn_cariterlaris.addActionListener(evt -> terlaris());
        favoritFilter.add(btn_cariterlaris);
        favoritHeader.add(favoritFilter, java.awt.BorderLayout.EAST);
        jLabel5.setVisible(false);
        favoritPanel.add(favoritHeader, java.awt.BorderLayout.NORTH);

        tb_favorit.setModel(new DefaultTableModel(new Object[][]{}, new String[]{"CODE", "PRODUCT", "PRICE"}));
        styleEditorialTable(tb_favorit);
        jScrollPane2.setBorder(null);
        jScrollPane2.setViewportView(tb_favorit);
        jScrollPane2.getViewport().setBackground(UITheme.SURFACE);

        pn_favoritCards = buildListBody(jScrollPane2, "No sales data yet.",
                new String[]{"CODE", "PRODUCT", "PRICE"});
        favoritPanel.add(pn_favoritCards, java.awt.BorderLayout.CENTER);

        lb_favoritFooter = new javax.swing.JLabel(" ");
        lb_favoritFooter.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lb_favoritFooter.setForeground(UITheme.TEXT_MUTED);
        lb_favoritFooter.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 0, 0));
        favoritPanel.add(lb_favoritFooter, java.awt.BorderLayout.SOUTH);
        gridShell.add(favoritPanel);

        // --- Latest sales ---
        javax.swing.JPanel laporanPanel = listCell(false, false);
        javax.swing.JPanel laporanHeader = sectionHeader("Latest sales", null);
        javax.swing.JLabel lbLive = new javax.swing.JLabel("LIVE");
        lbLive.setFont(UITheme.FONT_CAPTION);
        lbLive.setForeground(UITheme.ACCENT);
        laporanHeader.add(lbLive, java.awt.BorderLayout.EAST);
        jLabel6.setVisible(false);
        laporanPanel.add(laporanHeader, java.awt.BorderLayout.NORTH);

        tb_laporan.setModel(new DefaultTableModel(
                new Object[][]{}, new String[]{"TIME", "TXN", "CASHIER", "TOTAL"}));
        styleEditorialTable(tb_laporan);
        jScrollPane3.setBorder(null);
        jScrollPane3.setViewportView(tb_laporan);
        jScrollPane3.getViewport().setBackground(UITheme.SURFACE);

        pn_laporanCards = new javax.swing.JPanel(new java.awt.CardLayout());
        pn_laporanCards.setOpaque(false);
        pn_laporanCards.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.GRID_LINE));
        pn_laporanCards.add(jScrollPane3, "table");
        javax.swing.JPanel laporanEmpty = new javax.swing.JPanel(new java.awt.BorderLayout());
        laporanEmpty.setBackground(UITheme.SURFACE);
        laporanEmpty.add(columnHeaderBar(new String[]{"TIME", "TXN", "CASHIER", "TOTAL"}),
                java.awt.BorderLayout.NORTH);
        lb_laporanEmpty = emptyStateLabel("Nothing sold yet today.");
        laporanEmpty.add(lb_laporanEmpty, java.awt.BorderLayout.CENTER);
        pn_laporanCards.add(laporanEmpty, "empty");
        laporanPanel.add(pn_laporanCards, java.awt.BorderLayout.CENTER);

        javax.swing.JLabel lbOpenReport = linkLabel("Open sales report →");
        lbOpenReport.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                openShellPage("Reports");
            }
        });
        lbOpenReport.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 0, 0));
        laporanPanel.add(lbOpenReport, java.awt.BorderLayout.SOUTH);
        gridShell.add(laporanPanel);

        body.add(gridShell);
        jPanel1.add(body, java.awt.BorderLayout.CENTER);
        add(jPanel1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_caristok;
    private javax.swing.JButton btn_cariterlaris;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
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
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel lb_pendapatan;
    private javax.swing.JLabel lb_totalKeuntungan;
    private javax.swing.JLabel lb_totalPenjualan;
    private javax.swing.JLabel lb_totalProduk;
    private javax.swing.JPanel p_notifikasi;
    private javax.swing.JTable tb_favorit;
    private javax.swing.JTable tb_laporan;
    private javax.swing.JTable tb_stok;
    private javax.swing.JLabel totalBarang;
    private javax.swing.JTextField txt_stok;
    private javax.swing.JTextField txt_terlaris;
    // End of variables declaration//GEN-END:variables

    private javax.swing.JPanel bannerCell(boolean withLeftRule) {
        javax.swing.JPanel cell = new javax.swing.JPanel();
        cell.setOpaque(false);
        cell.setLayout(new javax.swing.BoxLayout(cell, javax.swing.BoxLayout.Y_AXIS));
        cell.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, withLeftRule ? 22 : 0, 0, 18));
        return cell;
    }

    private javax.swing.border.Border cellBorder(boolean right, boolean bottom) {
        return javax.swing.BorderFactory.createMatteBorder(
                0, 0, bottom ? 1 : 0, right ? 1 : 0, UITheme.GRID_LINE);
    }

    private javax.swing.JPanel listCell(boolean right, boolean bottom) {
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout(0, 0));
        panel.setBackground(UITheme.SURFACE);
        panel.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                cellBorder(right, bottom),
                javax.swing.BorderFactory.createEmptyBorder(14, 16, 12, 16)));
        return panel;
    }

    private javax.swing.JPanel sectionHeader(String title, javax.swing.JLabel unused) {
        javax.swing.JPanel header = new javax.swing.JPanel(new java.awt.BorderLayout());
        header.setOpaque(false);
        header.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 10, 0));
        javax.swing.JLabel lb = new javax.swing.JLabel(title);
        lb.setFont(UITheme.FONT_BOLD.deriveFont(14f));
        lb.setForeground(UITheme.TEXT_PRIMARY);
        header.add(lb, java.awt.BorderLayout.WEST);
        return header;
    }

    private javax.swing.JLabel attentionRow(javax.swing.JPanel parent, String label, boolean accentZeroOk) {
        javax.swing.JPanel row = new javax.swing.JPanel(new java.awt.BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.NAV_RULE),
                javax.swing.BorderFactory.createEmptyBorder(9, 0, 9, 0)));
        javax.swing.JLabel name = new javax.swing.JLabel(label);
        name.setFont(UITheme.FONT_REGULAR.deriveFont(13f));
        name.setForeground(UITheme.TEXT_PRIMARY);
        javax.swing.JLabel value = new javax.swing.JLabel("0");
        value.setFont(UITheme.FONT_BOLD.deriveFont(13f));
        value.setForeground(UITheme.TEXT_PRIMARY);
        value.putClientProperty("accentWhenPositive", Boolean.TRUE);
        row.add(name, java.awt.BorderLayout.WEST);
        row.add(value, java.awt.BorderLayout.EAST);
        row.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        parent.add(row);
        return value;
    }

    private void setAttentionValue(javax.swing.JLabel label, int count) {
        label.setText(Integer.toString(count));
        label.setForeground(count > 0 ? UITheme.ACCENT : UITheme.TEXT_PRIMARY);
    }

    private javax.swing.JLabel linkLabel(String text) {
        javax.swing.JLabel lb = new javax.swing.JLabel(text);
        lb.setFont(UITheme.FONT_BOLD.deriveFont(12f));
        lb.setForeground(UITheme.ACCENT);
        lb.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        return lb;
    }

    private javax.swing.JPanel buildListBody(javax.swing.JScrollPane tableScroll, String emptyText, String[] columns) {
        javax.swing.JPanel cards = new javax.swing.JPanel(new java.awt.CardLayout());
        cards.setOpaque(false);
        cards.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.GRID_LINE));
        cards.add(tableScroll, "table");

        javax.swing.JPanel empty = new javax.swing.JPanel(new java.awt.BorderLayout());
        empty.setBackground(UITheme.SURFACE);
        empty.add(columnHeaderBar(columns), java.awt.BorderLayout.NORTH);
        empty.add(emptyStateLabel(emptyText), java.awt.BorderLayout.CENTER);
        cards.add(empty, "empty");
        return cards;
    }

    private javax.swing.JPanel columnHeaderBar(String[] columns) {
        javax.swing.JPanel bar = new javax.swing.JPanel(new java.awt.GridLayout(1, columns.length, 0, 0));
        bar.setBackground(UITheme.SURFACE);
        bar.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.GRID_LINE));
        for (String col : columns) {
            javax.swing.JLabel lb = new javax.swing.JLabel(col);
            lb.setFont(UITheme.FONT_CAPTION);
            lb.setForeground(UITheme.TEXT_CAPTION);
            lb.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 4, 8, 4));
            bar.add(lb);
        }
        return bar;
    }

    private void styleFilterField(javax.swing.JTextField field, String value) {
        field.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        field.setText(value);
        field.setColumns(3);
        field.setFont(UITheme.FONT_BOLD.deriveFont(12f));
        field.setForeground(UITheme.TEXT_PRIMARY);
        field.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(UITheme.GRID_LINE),
                javax.swing.BorderFactory.createEmptyBorder(3, 6, 3, 6)));
    }

    private void styleApplyButton(javax.swing.JButton button) {
        button.setText("Apply");
        button.setBackground(UITheme.SURFACE);
        button.setForeground(UITheme.TEXT_PRIMARY);
        button.setFont(UITheme.FONT_BOLD.deriveFont(11f));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(UITheme.GRID_LINE),
                javax.swing.BorderFactory.createEmptyBorder(4, 10, 4, 10)));
    }

    private void styleEditorialTable(javax.swing.JTable table) {
        table.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        table.setForeground(UITheme.TEXT_PRIMARY);
        table.setBackground(UITheme.SURFACE);
        table.setGridColor(UITheme.NAV_RULE);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(UITheme.ACCENT_LIGHT);
        table.setSelectionForeground(UITheme.TEXT_PRIMARY);
        table.getTableHeader().setFont(UITheme.FONT_CAPTION);
        table.getTableHeader().setForeground(UITheme.TEXT_CAPTION);
        table.getTableHeader().setBackground(UITheme.SURFACE);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBorder(javax.swing.BorderFactory.createMatteBorder(
                0, 0, 1, 0, UITheme.GRID_LINE));
        ((javax.swing.table.DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer())
                .setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    }

    private String formatRp(int amount) {
        return UITheme.CURRENCY + " " + java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(amount);
    }

    private String formatRp(String amount) {
        try {
            return formatRp(Integer.parseInt(amount.trim()));
        } catch (Exception e) {
            return UITheme.CURRENCY + " " + amount;
        }
    }

    private String titleCase(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private javax.swing.JLabel emptyStateLabel(String text) {
        javax.swing.JLabel label = new javax.swing.JLabel("<html><div style='text-align:center;padding:18px 12px;'>"
                + text + "</div></html>");
        label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        label.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        label.setForeground(UITheme.TEXT_MUTED);
        return label;
    }

    private void showCard(javax.swing.JPanel cardPanel, boolean hasData) {
        ((java.awt.CardLayout) cardPanel.getLayout()).show(cardPanel, hasData ? "table" : "empty");
    }

    private void openShellPage(String menuName) {
        java.awt.Window w = SwingUtilities.getWindowAncestor(this);
        if (w instanceof Menu_Utama) {
            ((Menu_Utama) w).openPage(menuName);
        }
    }

    private void totalPenjualan() {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String tanggalPenjualan = dateFormat.format(tanggal);

            String jumlahPenjualan = "CALL QuantityPenjualan('" + tanggalPenjualan + "', @QuantityPenjualan);";
            String selectJumlahPenjualan = "SELECT @QuantityPenjualan;";

            ps = Koneksi.getConnection().prepareStatement(jumlahPenjualan);
            ps.execute();
            ps = Koneksi.getConnection().prepareStatement(selectJumlahPenjualan);
            rs = ps.executeQuery();
            if (rs.next()) {
                lb_totalPenjualan.setText(Integer.toString(rs.getInt(1)));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please check again: " + e.getMessage());
        }
    }

    private void totalKeuntungan() {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String day = dateFormat.format(tanggal);

            // Same net formula as Reports: line profit − bill discounts − expenses
            int profit = 0;
            ps = Koneksi.getConnection().prepareStatement(
                    "SELECT COALESCE(SUM(n.Subtotal - (n.jumlah * p.harga_beli)),0) "
                    + "FROM nota_penjualan n "
                    + "JOIN penjualan j ON j.penjualan_Id = n.penjualan_Id "
                    + "JOIN produk p ON p.kode_produk = n.kode_produk "
                    + "WHERE j.tanggal_penjualan = ? AND j.voided = 0");
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

            lb_totalKeuntungan.setText(java.text.NumberFormat.getIntegerInstance(java.util.Locale.US)
                    .format(profit));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please check again: " + e.getMessage());
        }
    }

    private void totalPendapatan() {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String tanggalPenjualan = dateFormat.format(tanggal);

            String totalPendapatan = "CALL TotalPendapatan('" + tanggalPenjualan + "', @totalHargaPenjualan)";
            String selectTotalPendapatan = "SELECT @totalHargaPenjualan;";

            ps = Koneksi.getConnection().prepareStatement(totalPendapatan);
            ps.execute();
            ps = Koneksi.getConnection().prepareStatement(selectTotalPendapatan);
            rs = ps.executeQuery();
            if (rs.next()) {
                lb_pendapatan.setText(java.text.NumberFormat.getIntegerInstance(java.util.Locale.US)
                        .format(rs.getInt(1)));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please check again: " + e.getMessage());
        }
    }

    private void totalProduk() {
        try {
            String Produk = "SELECT COUNT(*) AS JumlahProduk FROM produk WHERE stok_produk > 0;";
            ps = Koneksi.getConnection().prepareStatement(Produk);
            rs = ps.executeQuery();
            if (rs.next()) {
                lb_totalProduk.setText(Integer.toString(rs.getInt(1)));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please check again: " + e);
        }
    }

    private void needsAttention() {
        int threshold = 3;
        try {
            threshold = Integer.parseInt(txt_stok.getText().trim());
        } catch (Exception ignored) {
            txt_stok.setText("3");
            threshold = 3;
        }

        try {
            ps = Koneksi.getConnection().prepareStatement(
                    "SELECT COUNT(*) FROM produk WHERE stok_produk < " + threshold);
            rs = ps.executeQuery();
            int low = rs.next() ? rs.getInt(1) : 0;
            setAttentionValue(lb_attnLow, low);

            ps = Koneksi.getConnection().prepareStatement(
                    "SELECT COUNT(*) FROM produk WHERE harga_jual IS NULL OR harga_jual <= 0");
            rs = ps.executeQuery();
            int noPrice = rs.next() ? rs.getInt(1) : 0;
            setAttentionValue(lb_attnPrice, noPrice);

            ps = Koneksi.getConnection().prepareStatement(
                    "SELECT COUNT(*) FROM produk WHERE supplier_Id IS NULL OR supplier_Id = 0 OR supplier_Id = ''");
            rs = ps.executeQuery();
            int noSupplier = rs.next() ? rs.getInt(1) : 0;
            setAttentionValue(lb_attnSupplier, noSupplier);

            // Schema has no open/draft restock status — surface 0.
            setAttentionValue(lb_attnRestock, 0);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please check again: " + e.getMessage());
        }
    }

    private void terlaris() {
        String favorit = txt_terlaris.getText();

        DefaultTableModel LaporanLaris = new DefaultTableModel();
        LaporanLaris.addColumn("CODE");
        LaporanLaris.addColumn("PRODUCT");
        LaporanLaris.addColumn("PRICE");

        try {
            String cari = "CALL TopProduct('" + favorit + "');";
            ps = Koneksi.getConnection().prepareStatement(cari);
            rs = ps.executeQuery();

            boolean dataFound = false;
            int ranked = 0;
            while (rs.next()) {
                dataFound = true;
                ranked++;
                LaporanLaris.addRow(new Object[]{
                    rs.getString("kode_produk"),
                    titleCase(rs.getString("nama_produk")),
                    formatRp(rs.getString("harga_jual"))
                });
            }

            tb_favorit.setModel(LaporanLaris);
            styleEditorialTable(tb_favorit);
            showCard(pn_favoritCards, dataFound);
            if (dataFound) {
                lb_favoritFooter.setText(ranked + " of " + ranked + " product"
                        + (ranked == 1 ? "" : "s") + " ranked.");
            } else {
                lb_favoritFooter.setText("No products ranked yet.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please check again: " + e);
        }
    }

    private void tampilLaporan() {
        try {
            String tampilBarang = "SELECT * FROM laporan_penjualan WHERE tanggal_penjualan = CURRENT_DATE "
                    + "ORDER BY penjualan_Id DESC;";
            ps = Koneksi.getConnection().prepareStatement(tampilBarang);
            rs = ps.executeQuery();

            DefaultTableModel laporan = new DefaultTableModel();
            laporan.addColumn("TIME");
            laporan.addColumn("TXN");
            laporan.addColumn("CASHIER");
            laporan.addColumn("TOTAL");

            while (rs.next()) {
                laporan.addRow(new Object[]{
                    "—",
                    "TX-" + String.format("%04d", rs.getInt("penjualan_Id")),
                    rs.getString("nama_user"),
                    formatRp(rs.getString("total_pembayaran"))
                });
            }
            tb_laporan.setModel(laporan);
            styleEditorialTable(tb_laporan);
            boolean hasToday = laporan.getRowCount() > 0;
            showCard(pn_laporanCards, hasToday);

            if (!hasToday && lb_laporanEmpty != null) {
                String hint = "Nothing sold yet today.";
                try {
                    ps = Koneksi.getConnection().prepareStatement(
                            "SELECT penjualan_Id, tanggal_penjualan, total_pembayaran FROM laporan_penjualan "
                            + "ORDER BY tanggal_penjualan DESC, penjualan_Id DESC LIMIT 1");
                    rs = ps.executeQuery();
                    if (rs.next()) {
                        String day = new SimpleDateFormat("d MMM").format(rs.getDate("tanggal_penjualan"));
                        hint = "Nothing sold yet today. The last sale was TX-"
                                + String.format("%04d", rs.getInt("penjualan_Id"))
                                + " on " + day + ", " + formatRp(rs.getString("total_pembayaran")) + ".";
                    }
                } catch (Exception ignored) {
                }
                lb_laporanEmpty.setText("<html><div style='text-align:center;padding:18px 12px;'>"
                        + hint + "</div></html>");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please check again: " + e);
        }
    }

    /** Attempt hourly buckets from penjualan datetime; otherwise empty chart. */
    private void loadHourlySales() {
        int[] hours = new int[14]; // 08..21 inclusive → 14 slots
        boolean any = false;
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String day = dateFormat.format(tanggal);
            ps = Koneksi.getConnection().prepareStatement(
                    "SELECT HOUR(tanggal_penjualan) AS jam, COUNT(*) AS n "
                    + "FROM penjualan WHERE DATE(tanggal_penjualan) = '" + day + "' "
                    + "AND voided = 0 "
                    + "GROUP BY HOUR(tanggal_penjualan)");
            rs = ps.executeQuery();
            while (rs.next()) {
                int h = rs.getInt("jam");
                if (h >= 8 && h <= 21) {
                    hours[h - 8] = rs.getInt("n");
                    if (hours[h - 8] > 0) {
                        any = true;
                    }
                }
            }
        } catch (Exception ignored) {
            // DATE-only columns yield empty chart — matches reference empty state.
        }
        hourChart.setValues(hours, any);
    }

    /** Minimal bar chart for sales-by-hour with editorial empty state. */
    private static class HourChartPanel extends javax.swing.JPanel {
        private int[] values = new int[14];
        private boolean hasData = false;
        private static final String[] LABELS = {"08", "10", "12", "14", "16", "18", "20"};

        HourChartPanel() {
            setOpaque(true);
            setBackground(UITheme.SURFACE);
        }

        void setValues(int[] values, boolean hasData) {
            this.values = values != null ? values : new int[14];
            this.hasData = hasData;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int padL = 8;
            int padR = 8;
            int padT = 12;
            int padB = 28;
            int chartW = w - padL - padR;
            int chartH = h - padT - padB;
            int baseline = padT + chartH;

            g2.setColor(UITheme.GRID_LINE);
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(padL, baseline, padL + chartW, baseline);

            if (hasData) {
                int max = 1;
                for (int v : values) {
                    max = Math.max(max, v);
                }
                int n = values.length;
                float slot = chartW / (float) n;
                int barW = Math.max(6, (int) (slot * 0.55f));
                for (int i = 0; i < n; i++) {
                    int barH = (int) ((values[i] / (double) max) * (chartH - 8));
                    int x = padL + (int) (i * slot + (slot - barW) / 2f);
                    int y = baseline - barH;
                    g2.setColor(UITheme.ACCENT);
                    g2.fillRect(x, y, barW, Math.max(barH, values[i] > 0 ? 2 : 0));
                }
            } else {
                String msg = "No sales logged yet — bars fill as transactions are saved.";
                g2.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
                g2.setColor(UITheme.TEXT_MUTED);
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(msg);
                g2.drawString(msg, Math.max(padL, (w - tw) / 2), padT + chartH / 2);
            }

            g2.setFont(UITheme.FONT_CAPTION);
            g2.setColor(UITheme.TEXT_MUTED);
            FontMetrics fm = g2.getFontMetrics();
            for (int i = 0; i < LABELS.length; i++) {
                float x = padL + (i / (float) (LABELS.length - 1)) * chartW;
                String lb = LABELS[i];
                int tw = fm.stringWidth(lb);
                g2.drawString(lb, (int) x - tw / 2, baseline + 16);
            }
            g2.dispose();
        }
    }
}
