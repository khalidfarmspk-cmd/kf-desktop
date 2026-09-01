package Main;

import config.Settings;
import config.SyncOutbox;
import config.SyncService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Invoice / receipt settings. Owner only. Not a drag-and-drop designer.
 */
public class Form_Pengaturan extends javax.swing.JPanel {

    private static final Color RULE = new Color(0xD0D0CC);
    private static final int PREVIEW_COLS = 48;
    /** On-screen preview scale of print logo (576 dots ≈ receipt width). */
    private static final int LOGO_PREVIEW_MAX_W = 220;

    private JTextField txt_shopName;
    private JTextField txt_shopAddress;
    private JTextField txt_shopPhone;
    private JTextArea txt_footer;
    private JTextField txt_maxDiscount;
    private JTextField txt_logoPath;
    private JLabel lb_logoPreview;
    private JCheckBox chk_showLogo;
    private JCheckBox chk_showCustomer;
    private JCheckBox chk_showCashier;
    private JCheckBox chk_showPayment;
    private JCheckBox chk_showBarcode;
    private JButton btn_browseLogo;
    private JButton btn_removeLogo;
    private JButton btn_save;
    private JButton btn_testPrint;
    private JTextArea txt_preview;
    private String pendingLogoPath = "";

    private JTextField txt_apiUrl;
    private JPasswordField txt_apiToken;
    private JCheckBox chk_syncEnabled;
    private JButton btn_testSync;
    private JButton btn_fullSync;
    private JButton btn_retryFailed;
    private JLabel lb_syncStatus;
    private Timer syncStatusTimer;

    public Form_Pengaturan() {
        initComponents();
        loadSettings();
        refreshLogoPreview();
        refreshPreview();
        refreshSyncStatus();
        syncStatusTimer = new Timer(30_000, e -> refreshSyncStatus());
        syncStatusTimer.setRepeats(true);
        syncStatusTimer.start();
    }

    private File dataDir() {
        File dir = new File("data");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private void loadSettings() {
        txt_shopName.setText(Settings.get("shop_name", ""));
        txt_shopAddress.setText(Settings.get("shop_address", ""));
        txt_shopPhone.setText(Settings.get("shop_phone", ""));
        String footer = Settings.get("receipt_footer", "Thank you");
        txt_footer.setText(footer == null ? "" : footer.replace("\r\n", "\n").replace('\r', '\n'));
        txt_maxDiscount.setText(Integer.toString(Settings.getInt("max_discount_percent", 10)));
        pendingLogoPath = Settings.get("logo_path", ShopBranding.DEFAULT_LOGO_FILE);
        txt_logoPath.setText(pendingLogoPath);
        chk_showLogo.setSelected(Settings.getBool("show_logo", true));
        chk_showCustomer.setSelected(Settings.getBool("show_customer", true));
        chk_showCashier.setSelected(Settings.getBool("show_cashier", true));
        chk_showPayment.setSelected(Settings.getBool("show_payment_method", true));
        chk_showBarcode.setSelected(Settings.getBool("show_invoice_barcode", true));

        String apiUrl = SyncService.getInstance().getSyncState("api_base_url");
        if (apiUrl == null || apiUrl.isEmpty()) {
            apiUrl = "https://pos-api-production-91dc.up.railway.app";
        }
        if (txt_apiUrl != null) {
            txt_apiUrl.setText(apiUrl);
        }
        String token = SyncService.getInstance().getSyncState("api_token");
        if (txt_apiToken != null) {
            txt_apiToken.setText(token == null ? "" : token);
        }
        if (chk_syncEnabled != null) {
            chk_syncEnabled.setSelected(Settings.getBool("sync_enabled", false));
        }
    }

    private boolean persistSettings() {
        int maxPct;
        try {
            maxPct = Integer.parseInt(txt_maxDiscount.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Max discount percent must be a whole number.");
            return false;
        }
        if (maxPct < 0 || maxPct > 100) {
            JOptionPane.showMessageDialog(this, "Max discount percent must be between 0 and 100.");
            return false;
        }
        try {
            Settings.set("shop_name", txt_shopName.getText().trim());
            Settings.set("shop_address", txt_shopAddress.getText().trim());
            Settings.set("shop_phone", txt_shopPhone.getText().trim());
            // Preserve line breaks exactly; normalize CRLF → LF
            String footer = txt_footer.getText().replace("\r\n", "\n").replace('\r', '\n');
            Settings.set("receipt_footer", footer);
            Settings.set("max_discount_percent", Integer.toString(maxPct));
            Settings.set("logo_path", pendingLogoPath == null ? "" : pendingLogoPath);
            Settings.set("show_logo", chk_showLogo.isSelected() ? "1" : "0");
            Settings.set("show_customer", chk_showCustomer.isSelected() ? "1" : "0");
            Settings.set("show_cashier", chk_showCashier.isSelected() ? "1" : "0");
            Settings.set("show_payment_method", chk_showPayment.isSelected() ? "1" : "0");
            Settings.set("show_invoice_barcode", chk_showBarcode.isSelected() ? "1" : "0");

            SyncService.getInstance().setSyncState("api_base_url", txt_apiUrl.getText().trim());
            char[] tokenChars = txt_apiToken.getPassword();
            String token = tokenChars == null ? "" : new String(tokenChars).trim();
            SyncService.getInstance().setSyncState("api_token", token.isEmpty() ? null : token);
            boolean wasEnabled = Settings.getBool("sync_enabled", false);
            boolean nowEnabled = chk_syncEnabled.isSelected();
            Settings.set("sync_enabled", nowEnabled ? "1" : "0");
            SyncService.getInstance().notifyTokenRefreshed();
            SyncService.getInstance().start();
            if (!wasEnabled && nowEnabled) {
                int queued = SyncOutbox.queueFullSync();
                refreshSyncStatus();
                JOptionPane.showMessageDialog(this,
                        "Sync enabled. Queued " + queued + " records for upload.");
            } else {
                refreshSyncStatus();
            }
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not save settings: " + e.getMessage());
            return false;
        }
    }

    private void testSyncConnection() {
        final String base = txt_apiUrl.getText().trim();
        btn_testSync.setEnabled(false);
        btn_testSync.setText("Testing…");
        new Thread(() -> {
            final String err = SyncService.getInstance().testConnection(base);
            SwingUtilities.invokeLater(() -> {
                btn_testSync.setEnabled(true);
                btn_testSync.setText("Test connection");
                if (err == null) {
                    JOptionPane.showMessageDialog(Form_Pengaturan.this,
                            "OK — API is reachable.\n"
                            + "Parked outbox rows were reset for retry.");
                    refreshSyncStatus();
                } else {
                    JOptionPane.showMessageDialog(Form_Pengaturan.this, "Connection failed:\n" + err);
                }
            });
        }, "pos-sync-test").start();
    }

    private void retryFailedOutbox() {
        btn_retryFailed.setEnabled(false);
        new Thread(() -> {
            final int reset = SyncService.getInstance().resetFailedOutboxAttempts();
            SwingUtilities.invokeLater(() -> {
                btn_retryFailed.setEnabled(true);
                JOptionPane.showMessageDialog(Form_Pengaturan.this,
                        reset + " failed outbox row(s) reset — push will retry them.");
                refreshSyncStatus();
            });
        }, "pos-retry-failed").start();
    }

    private void runFullSyncNow() {
        btn_fullSync.setEnabled(false);
        lb_syncStatus.setText("Queuing records…");
        new Thread(() -> {
            final int queued = SyncOutbox.queueFullSync();
            final int depth = SyncService.getInstance().getOutboxDepth();
            SwingUtilities.invokeLater(() -> {
                btn_fullSync.setEnabled(true);
                lb_syncStatus.setText("Done — " + depth + " items in outbox (queued " + queued + ")");
                JOptionPane.showMessageDialog(Form_Pengaturan.this,
                        "Done — " + depth + " items in outbox.");
                refreshSyncStatus();
            });
        }, "pos-full-sync").start();
    }

    private void refreshSyncStatus() {
        if (lb_syncStatus == null) {
            return;
        }
        try {
            SyncService.StatusSnapshot snap = SyncService.getInstance().getStatusSnapshot();
            String lastPull = snap.lastPullAt;
            if (lastPull == null || lastPull.trim().isEmpty()) {
                lastPull = "never";
            }
            lb_syncStatus.setText("Last pull: " + lastPull
                    + "   ·   Outbox: " + snap.outboxDepth
                    + "   ·   " + snap.label);
        } catch (Exception e) {
            lb_syncStatus.setText("Sync status unavailable");
        }
    }

    private void browseLogo() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose shop logo");
        fc.setFileFilter(new FileNameExtensionFilter("Images (PNG, JPG, BMP)", "png", "jpg", "jpeg", "bmp"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File chosen = fc.getSelectedFile();
        if (chosen == null || !chosen.isFile()) {
            return;
        }
        String name = chosen.getName();
        String lower = name.toLowerCase();
        String ext = ".png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            ext = ".jpg";
        } else if (lower.endsWith(".bmp")) {
            ext = ".bmp";
        } else if (lower.endsWith(".png")) {
            ext = ".png";
        }
        File dest = new File(dataDir(), "shop_logo" + ext);
        try {
            Files.copy(chosen.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            pendingLogoPath = dest.getAbsolutePath();
            txt_logoPath.setText(pendingLogoPath);
            refreshLogoPreview();
            refreshPreview();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not copy logo: " + e.getMessage());
        }
    }

    private void removeLogo() {
        if (pendingLogoPath != null && !pendingLogoPath.isEmpty()) {
            File f = new File(pendingLogoPath);
            if (f.isFile() && f.getParentFile() != null
                    && f.getParentFile().getAbsolutePath().equals(dataDir().getAbsolutePath())) {
                // Best-effort delete of the copied file only
                f.delete();
            }
        }
        pendingLogoPath = "";
        txt_logoPath.setText("");
        refreshLogoPreview();
        refreshPreview();
    }

    private void refreshLogoPreview() {
        if (lb_logoPreview == null) {
            return;
        }
        if (pendingLogoPath == null || pendingLogoPath.trim().isEmpty()) {
            lb_logoPreview.setIcon(null);
            lb_logoPreview.setText("No logo");
            return;
        }
        File f = new File(pendingLogoPath);
        if (!f.isFile()) {
            lb_logoPreview.setIcon(null);
            lb_logoPreview.setText("Logo file missing");
            return;
        }
        try {
            BufferedImage img = ImageIO.read(f);
            if (img == null) {
                lb_logoPreview.setIcon(null);
                lb_logoPreview.setText("Unable to load");
                return;
            }
            int w = img.getWidth();
            int h = img.getHeight();
            if (w > LOGO_PREVIEW_MAX_W) {
                h = Math.max(1, (int) Math.round(h * (LOGO_PREVIEW_MAX_W / (double) w)));
                w = LOGO_PREVIEW_MAX_W;
            }
            Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            lb_logoPreview.setIcon(new ImageIcon(scaled));
            lb_logoPreview.setText("");
        } catch (Exception e) {
            lb_logoPreview.setIcon(null);
            lb_logoPreview.setText("Unable to load");
        }
    }

    private void refreshPreview() {
        if (txt_preview == null) {
            return;
        }
        // Adaptive columns (same idea as the printer) so large prices are not clipped
        String sampleAPrice = "Rs 600";
        String sampleATotal = "Rs 600";
        String sampleBPrice = "Rs 23,432";
        String sampleBTotal = "Rs 23,432";
        int qtyW = Math.max("Qty".length(), 1);
        int priceW = Math.max("Price".length(),
                Math.max(sampleAPrice.length(), sampleBPrice.length()));
        int totalW = Math.max("Total".length(),
                Math.max(sampleATotal.length(), sampleBTotal.length()));
        int descW = PREVIEW_COLS - qtyW - priceW - totalW - 3;
        if (descW < 8) {
            sampleAPrice = "600";
            sampleATotal = "600";
            sampleBPrice = "23,432";
            sampleBTotal = "23,432";
            priceW = Math.max("Price".length(),
                    Math.max(sampleAPrice.length(), sampleBPrice.length()));
            totalW = Math.max("Total".length(),
                    Math.max(sampleATotal.length(), sampleBTotal.length()));
            descW = PREVIEW_COLS - qtyW - priceW - totalW - 3;
        }
        if (descW < 1) {
            descW = 1;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(centerLine("[ approx. preview — not exact ]")).append('\n');
        sb.append('\n');

        if (chk_showLogo.isSelected() && pendingLogoPath != null && !pendingLogoPath.trim().isEmpty()) {
            sb.append(centerLine("+------------------------------+")).append('\n');
            sb.append(centerLine("|          [ LOGO ]            |")).append('\n');
            sb.append(centerLine("+------------------------------+")).append('\n');
            sb.append('\n');
        }

        String shop = txt_shopName.getText().trim();
        if (!shop.isEmpty()) {
            sb.append(centerLine(truncate(shop, PREVIEW_COLS))).append('\n');
        }
        appendPreviewMultiline(sb, txt_shopAddress.getText());
        String phone = txt_shopPhone.getText().trim();
        if (!phone.isEmpty()) {
            sb.append(centerLine(truncate(phone, PREVIEW_COLS))).append('\n');
        }

        sb.append(repeat('-', PREVIEW_COLS)).append('\n');
        sb.append("Invoice #: ").append(ReceiptPrinter.formatInvoiceNumber(14)).append('\n');
        sb.append("Transaction Date: ")
                .append(new SimpleDateFormat("dd/MM/yyyy hh:mm a").format(new Date())).append('\n');
        if (chk_showCashier.isSelected()) {
            String cashier = user.getNama() != null ? user.getNama() : "Cashier";
            sb.append("Cashier: ").append(cashier).append('\n');
        }
        if (chk_showCustomer.isSelected()) {
            sb.append("Customer: Sample Customer").append('\n');
        }

        sb.append(repeat('-', PREVIEW_COLS)).append('\n');
        sb.append(centerLine("Sales Item")).append('\n');
        sb.append(repeat('-', PREVIEW_COLS)).append('\n');
        sb.append(padRight("Product", descW))
                .append(' ').append(padLeft("Qty", qtyW))
                .append(' ').append(padLeft("Price", priceW))
                .append(' ').append(padLeft("Total", totalW)).append('\n');
        sb.append(repeat('-', PREVIEW_COLS)).append('\n');
        sb.append(padRight("Sample item A", descW))
                .append(' ').append(padLeft("1", qtyW))
                .append(' ').append(padLeft(sampleAPrice, priceW))
                .append(' ').append(padLeft(sampleATotal, totalW)).append('\n');
        sb.append(padRight("Sample item B", descW))
                .append(' ').append(padLeft("1", qtyW))
                .append(' ').append(padLeft(sampleBPrice, priceW))
                .append(' ').append(padLeft(sampleBTotal, totalW)).append('\n');

        sb.append(repeat('-', PREVIEW_COLS)).append('\n');
        sb.append(padLabelValue("Total Items / Qty", "2 / 3")).append('\n');
        sb.append(padLabelValue("Discount", "Rs 100")).append('\n');
        sb.append(padLabelValue("Invoice Value", "Rs 900")).append('\n');
        sb.append(repeat('-', PREVIEW_COLS)).append('\n');
        sb.append(centerLine("Payment")).append('\n');
        sb.append(repeat('-', PREVIEW_COLS)).append('\n');
        String pay = chk_showPayment.isSelected() ? "Cash" : "Cash";
        sb.append(padLabelValue(pay, "Rs 1,000")).append('\n');
        sb.append(padLabelValue("Change Due", "Rs 100")).append('\n');

        if (chk_showBarcode.isSelected()) {
            sb.append(repeat('-', PREVIEW_COLS)).append('\n');
            sb.append(centerLine(ReceiptPrinter.formatInvoiceNumber(14))).append('\n');
            sb.append(centerLine("+------------------------------+")).append('\n');
            sb.append(centerLine("||||||||||||||||||||||||||||||")).append('\n');
            sb.append(centerLine("|       [ CODE 128 ]          |")).append('\n');
            sb.append(centerLine("||||||||||||||||||||||||||||||")).append('\n');
            sb.append(centerLine("+------------------------------+")).append('\n');
            sb.append(centerLine("14")).append('\n');
        }

        sb.append('\n');
        String footer = txt_footer.getText().replace("\r\n", "\n").replace('\r', '\n');
        if (footer.isEmpty()) {
            sb.append(centerLine("Thank you")).append('\n');
        } else {
            String[] lines = footer.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                appendWrappedCentered(sb, lines[i]);
            }
        }

        txt_preview.setText(sb.toString());
        txt_preview.setCaretPosition(0);
    }

    /** Word-wrap then centre — full footer visible in preview. */
    private void appendWrappedCentered(StringBuilder sb, String line) {
        if (line == null || line.isEmpty()) {
            sb.append('\n');
            return;
        }
        String remaining = line;
        while (!remaining.isEmpty()) {
            if (remaining.length() <= PREVIEW_COLS) {
                sb.append(centerLine(remaining)).append('\n');
                return;
            }
            int breakAt = remaining.lastIndexOf(' ', PREVIEW_COLS);
            if (breakAt < PREVIEW_COLS / 3) {
                breakAt = PREVIEW_COLS;
            }
            sb.append(centerLine(remaining.substring(0, breakAt).trim())).append('\n');
            remaining = remaining.substring(breakAt).trim();
        }
    }

    private void appendPreviewMultiline(StringBuilder sb, String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                sb.append(centerLine(truncate(line, PREVIEW_COLS))).append('\n');
            }
        }
    }

    private static String padLeft(String s, int width) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= width) {
            return s;
        }
        return repeat(' ', width - s.length()) + s;
    }

    private static String padRight(String s, int width) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= width) {
            return s.substring(0, width);
        }
        return s + repeat(' ', width - s.length());
    }

    private static String padLabelValue(String label, String value) {
        String left = label + ":";
        int spaces = PREVIEW_COLS - left.length() - value.length();
        if (spaces < 1) {
            spaces = 1;
        }
        return left + repeat(' ', spaces) + value;
    }

    private static String padLeftRight(String left, String right) {
        int spaces = PREVIEW_COLS - left.length() - right.length();
        if (spaces < 1) {
            spaces = 1;
        }
        return left + repeat(' ', spaces) + right;
    }

    private static String centerLine(String s) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= PREVIEW_COLS) {
            return s.substring(0, PREVIEW_COLS);
        }
        int pad = (PREVIEW_COLS - s.length()) / 2;
        return repeat(' ', pad) + s;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private DocumentListener previewListener() {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                refreshPreview();
            }
            public void removeUpdate(DocumentEvent e) {
                refreshPreview();
            }
            public void changedUpdate(DocumentEvent e) {
                refreshPreview();
            }
        };
    }

    private JPanel fieldBlock(String caption, Component field) {
        JPanel wrap = new JPanel(new BorderLayout(0, 4));
        wrap.setOpaque(false);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.setMaximumSize(new Dimension(520, field instanceof JTextArea ? 120 : 58));
        JLabel lb = new JLabel(caption);
        PageUI.styleCaption(lb);
        wrap.add(lb, BorderLayout.NORTH);
        wrap.add(field, BorderLayout.CENTER);
        return wrap;
    }

    private void initComponents() {
        txt_shopName = new JTextField();
        txt_shopAddress = new JTextField();
        txt_shopPhone = new JTextField();
        txt_footer = new JTextArea(4, 40);
        txt_maxDiscount = new JTextField();
        txt_logoPath = new JTextField();
        txt_logoPath.setEditable(false);
        lb_logoPreview = new JLabel("No logo", SwingConstants.CENTER);
        chk_showLogo = new JCheckBox("Show logo on receipt");
        chk_showCustomer = new JCheckBox("Show customer name");
        chk_showCashier = new JCheckBox("Show cashier");
        chk_showPayment = new JCheckBox("Show payment method");
        chk_showBarcode = new JCheckBox("Show invoice barcode");
        btn_browseLogo = new JButton("Browse…");
        btn_removeLogo = new JButton("Remove logo");
        btn_save = new JButton("Save settings");
        btn_testPrint = new JButton("Print test receipt");
        txt_apiUrl = new JTextField();
        txt_apiToken = new JPasswordField();
        chk_syncEnabled = new JCheckBox("Enable background sync");
        btn_testSync = new JButton("Test connection");
        btn_fullSync = new JButton("Full sync now");
        btn_retryFailed = new JButton("Retry failed");
        lb_syncStatus = new JLabel("Sync status: —");
        txt_preview = new JTextArea();
        txt_preview.setEditable(false);
        txt_preview.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        txt_preview.setBackground(Color.WHITE);
        txt_preview.setForeground(PageUI.INK);
        txt_preview.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        txt_preview.setLineWrap(false);
        txt_preview.setColumns(PREVIEW_COLS);
        txt_preview.setRows(36);

        setLayout(new BorderLayout());
        PageUI.paintPage(this);

        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UITheme.PAGE_BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.PAGE_BG);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, RULE));
        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headerLeft.setOpaque(false);
        JLabel badge = new JLabel("  SETTINGS / INVOICE  ");
        badge.setOpaque(true);
        badge.setBackground(PageUI.INK);
        badge.setForeground(Color.WHITE);
        badge.setFont(UITheme.FONT_BOLD.deriveFont(11f));
        badge.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 0));
        JLabel title = new JLabel("Invoice settings");
        title.setFont(UITheme.FONT_HEADING.deriveFont(22f));
        title.setForeground(PageUI.INK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sub = new JLabel("Shop logo, receipt layout, footer, discount cap, and cloud sync.");
        sub.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        sub.setForeground(UITheme.TEXT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleWrap.add(title);
        titleWrap.add(sub);
        headerLeft.add(badge);
        headerLeft.add(titleWrap);
        header.add(headerLeft, BorderLayout.WEST);
        page.add(header, BorderLayout.NORTH);

        // ---- Left form ----
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 12));

        PageUI.styleField(txt_shopName);
        PageUI.styleField(txt_shopAddress);
        PageUI.styleField(txt_shopPhone);
        PageUI.styleField(txt_maxDiscount);
        PageUI.styleField(txt_logoPath);
        txt_footer.setFont(UITheme.FONT_REGULAR.deriveFont(13f));
        txt_footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.GRID_LINE),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        txt_footer.setLineWrap(true);
        txt_footer.setWrapStyleWord(true);
        JScrollPane footerScroll = new JScrollPane(txt_footer);
        footerScroll.setPreferredSize(new Dimension(480, 88));
        footerScroll.setMaximumSize(new Dimension(520, 100));
        footerScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        txt_maxDiscount.setMaximumSize(new Dimension(120, 32));
        txt_maxDiscount.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(KeyEvent evt) {
                char c = evt.getKeyChar();
                if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE) {
                    evt.consume();
                }
            }
        });

        DocumentListener dl = previewListener();
        txt_shopName.getDocument().addDocumentListener(dl);
        txt_shopAddress.getDocument().addDocumentListener(dl);
        txt_shopPhone.getDocument().addDocumentListener(dl);
        txt_footer.getDocument().addDocumentListener(dl);

        java.awt.event.ItemListener il = e -> {
            if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
                refreshPreview();
            }
        };
        chk_showLogo.addItemListener(il);
        chk_showCustomer.addItemListener(il);
        chk_showCashier.addItemListener(il);
        chk_showPayment.addItemListener(il);
        chk_showBarcode.addItemListener(il);

        // Shop logo section
        JLabel lbLogoCap = new JLabel("SHOP LOGO");
        PageUI.styleCaption(lbLogoCap);
        lbLogoCap.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lbLogoCap);
        form.add(Box.createVerticalStrut(6));

        lb_logoPreview.setOpaque(true);
        lb_logoPreview.setBackground(new Color(0xF7F7F5));
        lb_logoPreview.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RULE),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        lb_logoPreview.setPreferredSize(new Dimension(240, 100));
        lb_logoPreview.setMaximumSize(new Dimension(520, 120));
        lb_logoPreview.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lb_logoPreview);
        form.add(Box.createVerticalStrut(8));

        JPanel logoRow = new JPanel(new BorderLayout(8, 0));
        logoRow.setOpaque(false);
        logoRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoRow.setMaximumSize(new Dimension(520, 32));
        logoRow.add(txt_logoPath, BorderLayout.CENTER);
        JPanel logoBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        logoBtns.setOpaque(false);
        PageUI.stylePrimaryButton(btn_browseLogo);
        PageUI.styleGhostButton(btn_removeLogo);
        btn_removeLogo.setForeground(PageUI.INK);
        btn_browseLogo.addActionListener(e -> browseLogo());
        btn_removeLogo.addActionListener(e -> removeLogo());
        logoBtns.add(btn_browseLogo);
        logoBtns.add(btn_removeLogo);
        logoRow.add(logoBtns, BorderLayout.EAST);
        form.add(logoRow);
        form.add(Box.createVerticalStrut(6));

        chk_showLogo.setOpaque(false);
        chk_showLogo.setFont(UITheme.FONT_REGULAR.deriveFont(13f));
        chk_showLogo.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(chk_showLogo);
        form.add(Box.createVerticalStrut(4));
        JLabel lbLogoWarn = new JLabel("<html>The printer is black and white only. "
                + "Simple line-art logos work best — photographs will look poor.</html>");
        lbLogoWarn.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lbLogoWarn.setForeground(UITheme.TEXT_MUTED);
        lbLogoWarn.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lbLogoWarn);
        form.add(Box.createVerticalStrut(16));

        form.add(fieldBlock("SHOP NAME", txt_shopName));
        form.add(Box.createVerticalStrut(12));
        form.add(fieldBlock("ADDRESS", txt_shopAddress));
        form.add(Box.createVerticalStrut(12));
        form.add(fieldBlock("PHONE", txt_shopPhone));
        form.add(Box.createVerticalStrut(12));
        form.add(fieldBlock("RECEIPT FOOTER (multi-line)", footerScroll));
        form.add(Box.createVerticalStrut(12));
        form.add(fieldBlock("MAX DISCOUNT PERCENT", txt_maxDiscount));
        form.add(Box.createVerticalStrut(18));

        JLabel lbToggles = new JLabel("RECEIPT LINES");
        PageUI.styleCaption(lbToggles);
        lbToggles.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lbToggles);
        form.add(Box.createVerticalStrut(6));
        for (JCheckBox c : new JCheckBox[]{chk_showCustomer, chk_showCashier, chk_showPayment, chk_showBarcode}) {
            c.setOpaque(false);
            c.setFont(UITheme.FONT_REGULAR.deriveFont(13f));
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(c);
        }
        form.add(Box.createVerticalStrut(20));

        JLabel lbSync = new JLabel("CLOUD SYNC");
        PageUI.styleCaption(lbSync);
        lbSync.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lbSync);
        form.add(Box.createVerticalStrut(6));
        JLabel lbSyncHelp = new JLabel("<html>Local till stays primary. Sales queue offline and "
                + "upload when the network is up. Prices/settings pull every 60s.</html>");
        lbSyncHelp.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lbSyncHelp.setForeground(UITheme.TEXT_MUTED);
        lbSyncHelp.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lbSyncHelp);
        form.add(Box.createVerticalStrut(10));

        PageUI.styleField(txt_apiUrl);
        PageUI.styleField(txt_apiToken);
        form.add(fieldBlock("API URL", txt_apiUrl));
        form.add(Box.createVerticalStrut(12));
        form.add(fieldBlock("API TOKEN", txt_apiToken));
        form.add(Box.createVerticalStrut(8));
        chk_syncEnabled.setOpaque(false);
        chk_syncEnabled.setFont(UITheme.FONT_REGULAR.deriveFont(13f));
        chk_syncEnabled.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(chk_syncEnabled);
        form.add(Box.createVerticalStrut(10));

        JPanel syncActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        syncActions.setOpaque(false);
        syncActions.setAlignmentX(Component.LEFT_ALIGNMENT);
        PageUI.styleGhostButton(btn_testSync);
        btn_testSync.setForeground(PageUI.INK);
        btn_testSync.addActionListener(e -> testSyncConnection());
        PageUI.styleGhostButton(btn_fullSync);
        btn_fullSync.setForeground(PageUI.INK);
        btn_fullSync.addActionListener(e -> runFullSyncNow());
        PageUI.styleGhostButton(btn_retryFailed);
        btn_retryFailed.setForeground(PageUI.INK);
        btn_retryFailed.addActionListener(e -> retryFailedOutbox());
        syncActions.add(btn_testSync);
        syncActions.add(btn_fullSync);
        syncActions.add(btn_retryFailed);
        form.add(syncActions);
        form.add(Box.createVerticalStrut(8));
        lb_syncStatus.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        lb_syncStatus.setForeground(UITheme.TEXT_MUTED);
        lb_syncStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lb_syncStatus);
        form.add(Box.createVerticalStrut(20));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        PageUI.stylePrimaryButton(btn_save);
        PageUI.styleGhostButton(btn_testPrint);
        btn_testPrint.setForeground(PageUI.INK);
        btn_save.addActionListener(e -> {
            if (persistSettings()) {
                JOptionPane.showMessageDialog(this, "Invoice settings saved.");
            }
        });
        btn_testPrint.addActionListener(e -> {
            if (!persistSettings()) {
                return;
            }
            ReceiptPrinter.printTestReceipt();
        });
        actions.add(btn_save);
        actions.add(btn_testPrint);
        form.add(actions);
        form.add(Box.createVerticalStrut(12));
        JLabel note = new JLabel("<html>This is a settings form, not a layout designer. "
                + "The receipt stays 48 characters wide for the 80mm printer.</html>");
        note.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        note.setForeground(UITheme.TEXT_MUTED);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(note);
        form.add(Box.createVerticalGlue());

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.getViewport().setBackground(UITheme.PAGE_BG);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        formScroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // ---- Right live preview: centred paper + scroll for long footers ----
        JPanel previewShell = new JPanel(new BorderLayout());
        previewShell.setBackground(new Color(0xF3F3F1));
        previewShell.setPreferredSize(new Dimension(460, 10));
        previewShell.setMinimumSize(new Dimension(400, 10));
        previewShell.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, RULE));

        JPanel previewHead = new JPanel(new BorderLayout());
        previewHead.setOpaque(false);
        previewHead.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, RULE),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JLabel lbPrev = new JLabel("LIVE PREVIEW");
        PageUI.styleCaption(lbPrev);
        JLabel lbApprox = new JLabel("48-col paper · scroll for full receipt");
        lbApprox.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lbApprox.setForeground(UITheme.TEXT_MUTED);
        previewHead.add(lbPrev, BorderLayout.WEST);
        previewHead.add(lbApprox, BorderLayout.EAST);

        JPanel paperStage = new JPanel(new java.awt.GridBagLayout());
        paperStage.setBackground(new Color(0xE8E8E4));
        paperStage.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JScrollPane previewScroll = new JScrollPane(txt_preview);
        previewScroll.setBorder(BorderFactory.createLineBorder(new Color(0xC8C8C4)));
        previewScroll.getViewport().setBackground(Color.WHITE);
        previewScroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        previewScroll.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        previewScroll.getVerticalScrollBar().setUnitIncrement(16);

        // Preferred width ≈ 48 monospace columns so text is not clipped
        FontMetrics fm = txt_preview.getFontMetrics(txt_preview.getFont());
        int paperW = Math.max(360, fm.charWidth('M') * PREVIEW_COLS + 40);
        previewScroll.setPreferredSize(new Dimension(paperW, 520));

        JPanel paperCard = new JPanel(new BorderLayout());
        paperCard.setOpaque(false);
        paperCard.add(previewScroll, BorderLayout.CENTER);
        paperStage.add(paperCard); // GridBagLayout centres by default

        JLabel lbFoot = new JLabel("<html>Receipt is centred in this panel. "
                + "Scroll to see the full footer. Print a test for real ESC/POS output.</html>");
        lbFoot.setFont(UITheme.FONT_REGULAR.deriveFont(11f));
        lbFoot.setForeground(UITheme.TEXT_MUTED);
        lbFoot.setBorder(BorderFactory.createEmptyBorder(10, 16, 14, 16));

        previewShell.add(previewHead, BorderLayout.NORTH);
        previewShell.add(paperStage, BorderLayout.CENTER);
        previewShell.add(lbFoot, BorderLayout.SOUTH);

        // Form + preview share width; preview keeps room for the paper
        JPanel body = new JPanel(new java.awt.GridBagLayout());
        body.setBackground(UITheme.PAGE_BG);
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.fill = java.awt.GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.weightx = 0.55;
        body.add(formScroll, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.45;
        body.add(previewShell, gbc);

        page.add(body, BorderLayout.CENTER);
        add(page, BorderLayout.CENTER);
    }
}
