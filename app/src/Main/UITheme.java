package Main;

import java.awt.Color;
import java.awt.Font;
import javax.swing.UIManager;

/**
 * Central place for the app's modern light theme: colors, fonts, and the
 * FlatLaf setup. Forms reference these constants instead of hardcoding
 * their own colors so the look stays consistent app-wide.
 */
public class UITheme {

    public static final Color PRIMARY = new Color(0x2563EB);
    public static final Color PRIMARY_DARK = new Color(0x1D4ED8);
    public static final Color PRIMARY_LIGHT = new Color(0xDBEAFE);

    public static final Color SIDEBAR_BG = new Color(0x1E1B3A);
    public static final Color SIDEBAR_TEXT = new Color(0xE2E0F5);
    public static final Color SIDEBAR_TEXT_MUTED = new Color(0x9C97C4);

    public static final Color BACKGROUND = new Color(0xF4F6FB);
    public static final Color SURFACE = Color.WHITE;
    public static final Color BORDER = new Color(0xE2E8F0);

    public static final Color TEXT_PRIMARY = new Color(0x1E293B);
    public static final Color TEXT_SECONDARY = new Color(0x64748B);

    public static final Color SUCCESS = new Color(0x22C55E);
    public static final Color SUCCESS_DARK = new Color(0x16A34A);
    public static final Color DANGER = new Color(0xEF4444);
    public static final Color WARNING = new Color(0xF59E0B);

    /** Editorial/admin-dashboard accent palette — used by Menu_Utama (shared chrome),
     *  Form_DasbordPemilik, and Form_Barang to match the coral-red reference design.
     *  Kept separate from PRIMARY so the rest of the app's blue theme is untouched. */
    public static final Color ACCENT = new Color(0xEC3013);
    public static final Color ACCENT_DARK = new Color(0xC92810);
    public static final Color ACCENT_LIGHT = new Color(0xFDE8E4);
    /** Editorial dashboard: white page + hairline black grid (matches reference mock). */
    public static final Color PAGE_BG = Color.WHITE;
    public static final Color NAV_BG = new Color(0xF5F5F3);
    public static final Color NAV_RULE = new Color(0xC8C8C4);
    public static final Color DIVIDER = new Color(0xD8D8D4);
    public static final Color GRID_LINE = new Color(0x1A1A1A);
    public static final Color TEXT_MUTED = new Color(0x9B9B98);
    public static final Color TEXT_CAPTION = new Color(0x8A8A87);

    /** Display currency symbol used across dashboards, reports, and totals. */
    public static final String CURRENCY = "Rs";

    public static final String FONT_FAMILY = "Segoe UI";
    public static final Font FONT_REGULAR = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font FONT_SUBHEADING = new Font(FONT_FAMILY, Font.BOLD, 16);
    public static final Font FONT_HEADING = new Font(FONT_FAMILY, Font.BOLD, 22);
    public static final Font FONT_CAPTION = new Font(FONT_FAMILY, Font.BOLD, 10);
    public static final Font FONT_KPI_VALUE = new Font(FONT_FAMILY, Font.BOLD, 32);

    private static boolean applied = false;

    public static void apply() {
        if (applied) {
            return;
        }
        applied = true;
        try {
            com.formdev.flatlaf.FlatLightLaf.setup();
        } catch (Throwable ex) {
            // Fall back to whatever default look and feel is active.
        }

        UIManager.put("Component.accentColor", PRIMARY);
        UIManager.put("Component.focusColor", PRIMARY_LIGHT);
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ProgressBar.arc", 10);
        UIManager.put("CheckBox.arc", 4);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumbArc", 8);
        UIManager.put("TabbedPane.selectedBackground", SURFACE);
        UIManager.put("Table.rowHeight", 30);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 1));
        UIManager.put("TableHeader.height", 34);
        UIManager.put("defaultFont", FONT_REGULAR);
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("Button.default.background", PRIMARY);
        UIManager.put("Button.default.foreground", Color.WHITE);
    }
}
