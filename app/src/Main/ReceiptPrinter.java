package Main;

import config.Koneksi;
import config.Settings;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.swing.JOptionPane;

/**
 * ESC/POS receipt printer for Black Copper BC-86AC (Windows: "Black Copper 80").
 * 80mm paper, 72mm printable = 48 characters at Font A.
 */
public final class ReceiptPrinter {

    private static final String PRINTER_NAME = "Black Copper 80";
    private static final int WIDTH = 48;
    /** Max logo width in dots for an 80mm / 203dpi class printer. */
    private static final int LOGO_MAX_WIDTH = 576;
    private static final Charset CHARSET = Charset.forName("UTF-8");

    private ReceiptPrinter() {
    }

    public static void printReceipt(int penjualanId) {
        try {
            Header header = loadHeader(penjualanId);
            if (header == null) {
                JOptionPane.showMessageDialog(null, "Sale #" + penjualanId + " not found for receipt.");
                return;
            }
            applySettings(header);
            List<LineItem> lines = loadLines(penjualanId);

            byte[] data = buildEscPos(header, lines);
            sendToPrinter(data);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Receipt print failed: " + e.getMessage());
        }
    }

    public static void printTestReceipt() {
        try {
            Header header = new Header();
            header.penjualanId = 14;
            header.tanggal = new Date();
            header.subtotal = 1000;
            header.discount = 100;
            header.total = 900;
            header.cash = 1000;
            header.change = 100;
            header.cashier = user.getNama() != null ? user.getNama() : "Cashier";
            header.customer = "Sample Customer";
            header.paymentMethod = "Cash";
            applySettings(header);

            List<LineItem> lines = new ArrayList<LineItem>();
            LineItem a = new LineItem();
            a.name = "Sample item A";
            a.qty = BigDecimal.ONE;
            a.unitName = "Piece";
            a.allowDecimal = false;
            a.unitPrice = 600;
            a.subtotal = 600;
            lines.add(a);
            LineItem b = new LineItem();
            b.name = "Sample item B";
            b.qty = BigDecimal.valueOf(2);
            b.unitName = "Piece";
            b.allowDecimal = false;
            b.unitPrice = 200;
            b.subtotal = 400;
            lines.add(b);

            sendToPrinter(buildEscPos(header, lines));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Test receipt failed: " + e.getMessage());
        }
    }

    private static void applySettings(Header header) {
        header.shopName = Settings.get("shop_name", "Khalid Farms");
        header.shopAddress = Settings.get("shop_address", "Bahria town Lahore");
        header.shopPhone = Settings.get("shop_phone", "");
        header.footer = Settings.get("receipt_footer", "Thank you");
        header.logoPath = Settings.get("logo_path", ShopBranding.DEFAULT_LOGO_FILE);
        header.showLogo = Settings.getBool("show_logo", true);
        header.showCustomer = Settings.getBool("show_customer", true);
        header.showCashier = Settings.getBool("show_cashier", true);
        header.showPaymentMethod = Settings.getBool("show_payment_method", true);
        header.showInvoiceBarcode = Settings.getBool("show_invoice_barcode", true);
    }

    private static Header loadHeader(int penjualanId) throws Exception {
        String sql = "SELECT p.penjualan_Id, p.tanggal_penjualan, p.updated_at, p.Total_pembayaran, "
                + "p.uang_diterima, p.uang_kembalian, "
                + "COALESCE(p.subtotal_kotor, p.Total_pembayaran) AS subtotal_kotor, "
                + "COALESCE(p.diskon, 0) AS diskon, p.nama_kurir, "
                + "COALESCE(p.voided, 0) AS voided, "
                + "u.nama_user, pl.nama_pelanggan, mb.nama_metode "
                + "FROM penjualan p "
                + "JOIN users u ON p.user_Id = u.user_Id "
                + "LEFT JOIN pelanggan pl ON p.pelanggan_Id = pl.pelanggan_Id "
                + "LEFT JOIN metode_bayar mb ON p.metode_Id = mb.metode_Id "
                + "WHERE p.penjualan_Id = ?";
        PreparedStatement ps = Koneksi.getConnection().prepareStatement(sql);
        try {
            ps.setInt(1, penjualanId);
            ResultSet rs = ps.executeQuery();
            try {
                if (!rs.next()) {
                    return null;
                }
                Header h = new Header();
                h.penjualanId = rs.getInt("penjualan_Id");
                java.sql.Timestamp soldAt = null;
                try {
                    soldAt = rs.getTimestamp("updated_at");
                } catch (Exception ignored) {
                }
                if (soldAt != null) {
                    h.tanggal = soldAt;
                } else {
                    h.tanggal = rs.getDate("tanggal_penjualan");
                }
                h.total = rs.getInt("Total_pembayaran");
                h.cash = rs.getInt("uang_diterima");
                h.change = rs.getInt("uang_kembalian");
                h.subtotal = rs.getInt("subtotal_kotor");
                h.discount = rs.getInt("diskon");
                h.cashier = rs.getString("nama_user");
                h.customer = rs.getString("nama_pelanggan");
                h.paymentMethod = rs.getString("nama_metode");
                h.deliveryMan = rs.getString("nama_kurir");
                h.voided = rs.getInt("voided") == 1;
                if (h.subtotal <= 0) {
                    h.subtotal = h.total + h.discount;
                }
                return h;
            } finally {
                rs.close();
            }
        } finally {
            ps.close();
        }
    }

    private static List<LineItem> loadLines(int penjualanId) throws Exception {
        String sql = "SELECT pr.nama_produk, dp.jumlah, pr.harga_jual, dp.Subtotal, s.nama_satuan, s.allow_decimal "
                + "FROM detail_penjualan dp "
                + "JOIN produk pr ON dp.kode_produk = pr.kode_produk "
                + "JOIN satuan s ON pr.satuan_Id = s.satuan_Id "
                + "WHERE dp.penjualan_Id = ?";
        PreparedStatement ps = Koneksi.getConnection().prepareStatement(sql);
        try {
            ps.setInt(1, penjualanId);
            ResultSet rs = ps.executeQuery();
            try {
                List<LineItem> list = new ArrayList<LineItem>();
                while (rs.next()) {
                    LineItem line = new LineItem();
                    line.name = rs.getString("nama_produk");
                    BigDecimal qty = rs.getBigDecimal("jumlah");
                    line.qty = qty != null ? qty : BigDecimal.ZERO;
                    BigDecimal subtotal = rs.getBigDecimal("Subtotal");
                    qty = line.qty;
                    if (qty != null && qty.compareTo(BigDecimal.ZERO) > 0
                            && subtotal != null && subtotal.compareTo(BigDecimal.ZERO) > 0) {
                        line.unitPrice = subtotal.divide(qty, 0, RoundingMode.HALF_UP).intValue();
                    } else {
                        line.unitPrice = rs.getInt("harga_jual");
                    }
                    line.subtotal = rs.getInt("Subtotal");
                    line.unitName = rs.getString("nama_satuan");
                    line.allowDecimal = rs.getBoolean("allow_decimal");
                    list.add(line);
                }
                return list;
            } finally {
                rs.close();
            }
        } finally {
            ps.close();
        }
    }

    private static byte[] buildEscPos(Header header, List<LineItem> lines) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // ESC @ - initialise
        out.write(new byte[]{0x1B, 0x40});

        // ---- 1. Header: logo + shop ----
        try {
            writeLogo(out, header);
        } catch (Exception ignored) {
        }

        String shopName = nullSafe(header.shopName).trim();
        if (!shopName.isEmpty()) {
            out.write(new byte[]{0x1B, 0x61, 0x01});
            out.write(new byte[]{0x1B, 0x21, 0x30}); // double width+height
            writeLine(out, shopName);
            out.write(new byte[]{0x1B, 0x21, 0x00});
        }

        writeCenteredLines(out, header.shopAddress);
        if (header.shopPhone != null && !header.shopPhone.trim().isEmpty()) {
            writeCenteredLine(out, truncate(header.shopPhone.trim(), WIDTH));
        }

        if (header.voided) {
            out.write(new byte[]{0x1B, 0x61, 0x01}); // center
            out.write(new byte[]{0x1B, 0x21, 0x30}); // double width+height
            writeLine(out, "*** VOIDED ***");
            out.write(new byte[]{0x1B, 0x21, 0x00});
            out.write(new byte[]{0x1B, 0x61, 0x00});
        }

        // ---- 2. Transaction meta (left) ----
        out.write(new byte[]{0x1B, 0x61, 0x00});
        writeLine(out, dashedLine());
        writeLine(out, "Invoice #: " + formatInvoiceNumber(header.penjualanId));
        Date when = header.tanggal != null ? header.tanggal : new Date();
        writeLine(out, "Transaction Date: " + new SimpleDateFormat("dd/MM/yyyy hh:mm a").format(when));
        if (header.showCashier) {
            writeLine(out, "Cashier: " + nullSafe(header.cashier));
        }
        if (header.showCustomer && header.customer != null && !header.customer.trim().isEmpty()) {
            writeLine(out, "Customer: " + header.customer.trim());
        }
        if (header.deliveryMan != null && !header.deliveryMan.trim().isEmpty()) {
            writeLine(out, "Delivery man: " + header.deliveryMan.trim());
        }

        // ---- 3. Sales Item title ----
        writeLine(out, dashedLine());
        writeCenteredLine(out, "Sales Item");
        out.write(new byte[]{0x1B, 0x61, 0x00});
        writeLine(out, dashedLine());

        // ---- 4. Items: headers (no per-line discount) then rows ----
        ItemCols cols = layoutItemCols(lines);
        writeLine(out, itemHeaderRow(cols));
        writeLine(out, dashedLine());

        BigDecimal totalQty = BigDecimal.ZERO;
        for (int i = 0; i < lines.size(); i++) {
            LineItem line = lines.get(i);
            totalQty = totalQty.add(line.qty != null ? line.qty : BigDecimal.ZERO);
            writeLine(out, itemInlineRow(cols,
                    nullSafe(line.name),
                    QuantityUtil.format(line.qty, line.allowDecimal),
                    formatMoney(line.unitPrice, cols.compactMoney),
                    formatMoney(line.subtotal, cols.compactMoney)));
        }

        // ---- 5. Totals (bill discount only here) ----
        writeLine(out, dashedLine());
        writeLine(out, padLabelValue("Total Items / Qty",
                lines.size() + " / " + QuantityUtil.format(totalQty, true)));
        writeLine(out, padLabelValue("Discount", money(header.discount)));
        // Emphasized invoice value
        out.write(new byte[]{0x1B, 0x21, 0x08}); // emphasized
        writeLine(out, padLabelValue("Invoice Value", money(header.total)));
        out.write(new byte[]{0x1B, 0x21, 0x00});

        // ---- 6. Payments ----
        writeLine(out, dashedLine());
        writeCenteredLine(out, "Payment");
        out.write(new byte[]{0x1B, 0x61, 0x00});
        writeLine(out, dashedLine());
        String payLabel = "Cash";
        if (header.showPaymentMethod && header.paymentMethod != null
                && !header.paymentMethod.trim().isEmpty()) {
            payLabel = header.paymentMethod.trim();
        }
        boolean cashPay = "Cash".equalsIgnoreCase(payLabel);
        if (cashPay) {
            writeLine(out, padLabelValue("Cash", money(header.cash > 0 ? header.cash : header.total)));
            writeLine(out, padLabelValue("Change Due", money(header.change)));
        } else {
            writeLine(out, padLabelValue(payLabel, money(header.total)));
        }

        // ---- 7. Barcode ----
        writeLine(out, dashedLine());
        try {
            writeInvoiceBarcode(out, header);
        } catch (Exception ignored) {
        }

        // ---- 8. Footer ----
        out.write(new byte[]{0x1B, 0x61, 0x01});
        writeFooterLines(out, header.footer);
        out.write(new byte[]{0x1B, 0x61, 0x00});

        if (cashPay) {
            // Open cash drawer - pulse on pin 2
            out.write(0x1B);
            out.write(0x70);
            out.write(0x00);
            out.write(0x19);
            out.write(0xFA);
        }

        // Feed past cutter
        out.write(0x0A);
        out.write(0x0A);
        out.write(0x0A);
        out.write(0x0A);
        out.write(0x0A);

        // GS V 0 - full cut
        out.write(new byte[]{0x1D, 0x56, 0x00});

        return out.toByteArray();
    }

    /**
     * Adaptive Product | Qty | Price | Total widths so large amounts are never cut off.
     * Shrinks the product name column first; if still tight, drops the "Rs " prefix.
     */
    private static final class ItemCols {
        final int desc;
        final int qty;
        final int price;
        final int total;
        final boolean compactMoney;

        ItemCols(int desc, int qty, int price, int total, boolean compactMoney) {
            this.desc = desc;
            this.qty = qty;
            this.price = price;
            this.total = total;
            this.compactMoney = compactMoney;
        }
    }

    private static final int MIN_DESC = 8;

    private static ItemCols layoutItemCols(List<LineItem> lines) {
        ItemCols full = measureItemCols(lines, false);
        if (full.desc >= MIN_DESC) {
            return full;
        }
        ItemCols compact = measureItemCols(lines, true);
        if (compact.desc >= MIN_DESC) {
            return compact;
        }
        // Extreme amounts: keep money columns exact, give leftover to name (may be tiny).
        return compact;
    }

    private static ItemCols measureItemCols(List<LineItem> lines, boolean compactMoney) {
        int qtyW = "Qty".length();
        int priceW = "Price".length();
        int totalW = "Total".length();
        if (lines != null) {
            for (int i = 0; i < lines.size(); i++) {
                LineItem line = lines.get(i);
                String q = QuantityUtil.format(line.qty, line.allowDecimal);
                String p = formatMoney(line.unitPrice, compactMoney);
                String t = formatMoney(line.subtotal, compactMoney);
                if (q.length() > qtyW) {
                    qtyW = q.length();
                }
                if (p.length() > priceW) {
                    priceW = p.length();
                }
                if (t.length() > totalW) {
                    totalW = t.length();
                }
            }
        }
        int gaps = 3;
        int descW = WIDTH - qtyW - priceW - totalW - gaps;
        if (descW < 1) {
            descW = 1;
        }
        return new ItemCols(descW, qtyW, priceW, totalW, compactMoney);
    }

    private static String formatMoney(int amount, boolean compact) {
        if (compact) {
            return NumberFormat.getIntegerInstance(Locale.US).format(amount);
        }
        return money(amount);
    }

    private static String dashedLine() {
        return repeat('-', WIDTH);
    }

    private static String itemHeaderRow(ItemCols cols) {
        return padRight("Product", cols.desc)
                + " " + padLeft("Qty", cols.qty)
                + " " + padLeft("Price", cols.price)
                + " " + padLeft("Total", cols.total);
    }

    private static String itemInlineRow(ItemCols cols, String name, String qty, String price, String total) {
        return padRight(truncate(nullSafe(name), cols.desc), cols.desc)
                + " " + padLeft(qty, cols.qty)
                + " " + padLeft(price, cols.price)
                + " " + padLeft(total, cols.total);
    }

    private static void writeLogo(ByteArrayOutputStream out, Header header) throws Exception {
        if (!header.showLogo) {
            return;
        }
        BufferedImage src = loadLogoImage(header);
        if (src == null) {
            return;
        }
        BufferedImage scaled = scaleToMaxWidth(src, LOGO_MAX_WIDTH);
        byte[] raster = toEscPosRaster(scaled);

        out.write(new byte[]{0x1B, 0x61, 0x01}); // centre
        out.write(raster);
        out.write(0x0A); // one blank line after logo
        out.write(new byte[]{0x1B, 0x61, 0x00});
    }

    private static BufferedImage loadLogoImage(Header header) throws Exception {
        String path = header.logoPath == null ? "" : header.logoPath.trim();
        if (!path.isEmpty()) {
            File file = new File(path);
            if (file.isFile()) {
                BufferedImage img = ImageIO.read(file);
                if (img != null) {
                    return img;
                }
            }
        }
        java.net.URL url = ShopBranding.logoUrl();
        if (url == null) {
            return null;
        }
        return ImageIO.read(url);
    }

    /**
     * Scale preserving aspect ratio so width ? maxWidth dots.
     */
    static BufferedImage scaleToMaxWidth(BufferedImage src, int maxWidth) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= 0 || h <= 0) {
            return src;
        }
        if (w > maxWidth) {
            h = Math.max(1, (int) Math.round(h * (maxWidth / (double) w)));
            w = maxWidth;
        }
        // Width must be a multiple of 8 for packed raster
        int paddedW = ((w + 7) / 8) * 8;
        BufferedImage dest = new BufferedImage(paddedW, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dest.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, paddedW, h);
            g.drawImage(src, (paddedW - w) / 2, 0, w, h, null);
        } finally {
            g.dispose();
        }
        return dest;
    }

    /**
        // GS V 0 - full cut
     * Uses Floyd-Steinberg dithering unless the image is already pure B&amp;W.
     */
    static byte[] toEscPosRaster(BufferedImage image) throws Exception {
        int width = image.getWidth();
        int height = image.getHeight();
        int bytesPerRow = width / 8;
        if (bytesPerRow * 8 != width) {
            throw new IllegalArgumentException("Logo width must be a multiple of 8");
        }

        double[][] gray = new double[height][width];
        boolean pureBw = true;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int a = (rgb >>> 24) & 0xFF;
                int r = (rgb >>> 16) & 0xFF;
                int g = (rgb >>> 8) & 0xFF;
                int b = rgb & 0xFF;
                if (a < 128) {
                    // Transparent ? white (no ink)
                    gray[y][x] = 255.0;
                } else {
                    gray[y][x] = 0.299 * r + 0.587 * g + 0.114 * b;
                    if (r != g || g != b || (r != 0 && r != 255)) {
                        pureBw = false;
                    }
                }
            }
        }

        boolean[][] ink = new boolean[height][width];
        if (pureBw) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    ink[y][x] = gray[y][x] < 128.0;
                }
            }
        } else {
            floydSteinberg(gray, ink, width, height);
        }

        ByteArrayOutputStream raster = new ByteArrayOutputStream();
        // GS V 0 - full cut
        raster.write(0x1D);
        raster.write(0x76);
        raster.write(0x30);
        raster.write(0x00);
        raster.write(bytesPerRow & 0xFF);          // xL
        raster.write((bytesPerRow >> 8) & 0xFF);   // xH
        raster.write(height & 0xFF);               // yL
        raster.write((height >> 8) & 0xFF);        // yH

        for (int y = 0; y < height; y++) {
            for (int bx = 0; bx < bytesPerRow; bx++) {
                int b = 0;
                for (int bit = 0; bit < 8; bit++) {
                    if (ink[y][bx * 8 + bit]) {
                        b |= (0x80 >> bit);
                    }
                }
                raster.write(b);
            }
        }
        return raster.toByteArray();
    }

    private static void floydSteinberg(double[][] gray, boolean[][] ink, int width, int height) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double old = gray[y][x];
                double neu = old < 128.0 ? 0.0 : 255.0;
                ink[y][x] = neu == 0.0;
                double err = old - neu;
                if (x + 1 < width) {
                    gray[y][x + 1] += err * 7.0 / 16.0;
                }
                if (y + 1 < height) {
                    if (x > 0) {
                        gray[y + 1][x - 1] += err * 3.0 / 16.0;
                    }
                    gray[y + 1][x] += err * 5.0 / 16.0;
                    if (x + 1 < width) {
                        gray[y + 1][x + 1] += err * 1.0 / 16.0;
                    }
                }
            }
        }
    }

    private static void writeInvoiceBarcode(ByteArrayOutputStream out, Header header) throws Exception {
        if (!header.showInvoiceBarcode) {
            return;
        }
        int id = header.penjualanId;
        if (id <= 0) {
            id = 0;
        }
        String invLabel = formatInvoiceNumber(id);
        String payload = String.valueOf(id);

        out.write(new byte[]{0x1B, 0x61, 0x01}); // centre
        writeLine(out, invLabel);

        // GS h 80 - barcode height
        out.write(new byte[]{0x1D, 0x68, 0x50});
        // GS w 2 - module width
        out.write(new byte[]{0x1D, 0x77, 0x02});
        // GS H 2 - HRI below
        out.write(new byte[]{0x1D, 0x48, 0x02});

        // GS k 73 n {B d1..dn - Code 128, code set B
        byte[] dataBytes = payload.getBytes(Charset.forName("US-ASCII"));
        int n = 2 + dataBytes.length; // {B + data
        out.write(0x1D);
        out.write(0x6B);
        out.write(0x49);
        out.write(n & 0xFF);
        out.write(0x7B); // {
        out.write(0x42); // B
        out.write(dataBytes);

        out.write(0x0A);
        out.write(new byte[]{0x1B, 0x61, 0x00});
    }

    static String formatInvoiceNumber(int penjualanId) {
        return "INV-" + String.format("%04d", Math.max(0, penjualanId));
    }

    private static void writeFooterLines(ByteArrayOutputStream out, String footer) throws Exception {
        String text = footer == null ? "" : footer.replace("\r\n", "\n").replace('\r', '\n');
        if (text.isEmpty()) {
            writeCenteredLine(out, "Thank you");
            return;
        }
        String[] lines = text.split("\n", -1);
        boolean any = false;
        for (int i = 0; i < lines.length; i++) {
            writeWrappedCentered(out, lines[i]);
            any = true;
        }
        if (!any) {
            writeCenteredLine(out, "Thank you");
        }
    }

    /** Word-wrap then centre each physical line (keeps long footer text). */
    private static void writeWrappedCentered(ByteArrayOutputStream out, String line) throws Exception {
        if (line == null || line.isEmpty()) {
            writeCenteredLine(out, "");
            return;
        }
        String remaining = line;
        while (!remaining.isEmpty()) {
            if (remaining.length() <= WIDTH) {
                writeCenteredLine(out, remaining);
                return;
            }
            int breakAt = remaining.lastIndexOf(' ', WIDTH);
            if (breakAt < WIDTH / 3) {
                breakAt = WIDTH;
            }
            writeCenteredLine(out, remaining.substring(0, breakAt).trim());
            remaining = remaining.substring(breakAt).trim();
        }
    }

    private static void writeCenteredLines(ByteArrayOutputStream out, String text) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                writeCenteredLine(out, truncate(line, WIDTH));
            }
        }
    }

    private static void writeCenteredLine(ByteArrayOutputStream out, String text) throws Exception {
        out.write(new byte[]{0x1B, 0x61, 0x01});
        writeLine(out, text == null ? "" : text);
    }

    private static void sendToPrinter(byte[] data) throws Exception {
        PrintService target = null;
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (int i = 0; i < services.length; i++) {
            if (services[i].getName().contains(PRINTER_NAME)) {
                target = services[i];
                break;
            }
        }
        if (target == null) {
            JOptionPane.showMessageDialog(null, "Printer \"" + PRINTER_NAME + "\" not found.");
            return;
        }
        DocPrintJob job = target.createPrintJob();
        job.print(new SimpleDoc(data, DocFlavor.BYTE_ARRAY.AUTOSENSE, null), null);
    }

    private static String money(int amount) {
        return "Rs " + NumberFormat.getIntegerInstance(Locale.US).format(amount);
    }

    private static void writeLine(ByteArrayOutputStream out, String text) throws Exception {
        out.write(text.getBytes(CHARSET));
        out.write(0x0A);
    }

    private static String padLabelValue(String label, String value) {
        String left = label + ":";
        int spaces = WIDTH - left.length() - value.length();
        if (spaces < 1) {
            spaces = 1;
        }
        return left + repeat(' ', spaces) + value;
    }

    private static String padLeft(String s, int width) {
        if (s == null) {
            s = "";
        }
        // Never clip amounts - column layout must size to fit.
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

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static final class Header {
        int penjualanId;
        Date tanggal;
        int subtotal;
        int discount;
        int total;
        int cash;
        int change;
        String cashier;
        String customer;
        String paymentMethod;
        String deliveryMan;
        boolean voided;
        String shopName;
        String shopAddress;
        String shopPhone;
        String footer;
        String logoPath;
        boolean showLogo;
        boolean showCustomer;
        boolean showCashier;
        boolean showPaymentMethod;
        boolean showInvoiceBarcode;
    }

    private static final class LineItem {
        String name;
        BigDecimal qty;
        String unitName;
        boolean allowDecimal;
        int unitPrice;
        int subtotal;
    }
}
