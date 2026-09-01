package Main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Shared editorial chrome for every content page: page padding, breadcrumbs,
 * accent buttons, hairline panels/tables. Forms call these helpers so the
 * dashboard / products look stays consistent app-wide.
 */
public final class PageUI {

    public static final int PAGE_PAD = 24;
    public static final Color INK = new Color(0x1A1A1A);

    private PageUI() {}

    public static void paintPage(JComponent root) {
        root.setOpaque(true);
        root.setBackground(UITheme.PAGE_BG);
    }

    /** Standard page header: caption crumb + large title. */
    public static JPanel pageHeader(String breadcrumb, JLabel titleLabel) {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel crumb = new JLabel(breadcrumb);
        crumb.setFont(UITheme.FONT_CAPTION);
        crumb.setForeground(UITheme.TEXT_CAPTION);
        crumb.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(crumb);

        titleLabel.setFont(UITheme.FONT_HEADING.deriveFont(28f));
        titleLabel.setForeground(INK);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(titleLabel);
        header.add(Box.createVerticalStrut(14));
        return header;
    }

    public static Border pagePadding() {
        return BorderFactory.createEmptyBorder(PAGE_PAD, PAGE_PAD, PAGE_PAD, PAGE_PAD);
    }

    public static Border gridBox() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.GRID_LINE, 1),
            BorderFactory.createEmptyBorder(14, 16, 14, 16));
    }

    public static Border hairline() {
        return BorderFactory.createLineBorder(UITheme.GRID_LINE, 1);
    }

    public static void styleBox(JPanel panel) {
        panel.setOpaque(true);
        panel.setBackground(UITheme.SURFACE);
        panel.setBorder(gridBox());
    }

    public static void stylePrimaryButton(JButton button) {
        button.setBackground(UITheme.ACCENT);
        button.setForeground(Color.WHITE);
        button.setFont(UITheme.FONT_BOLD.deriveFont(12f));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    }

    public static void styleDangerButton(JButton button) {
        stylePrimaryButton(button);
        button.setBackground(UITheme.DANGER);
    }

    public static void styleGhostButton(JButton button) {
        button.setBackground(UITheme.SURFACE);
        button.setForeground(UITheme.TEXT_SECONDARY);
        button.setFont(UITheme.FONT_BOLD.deriveFont(12f));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.GRID_LINE),
            BorderFactory.createEmptyBorder(7, 14, 7, 14)));
    }

    public static void styleField(JTextField field) {
        field.setFont(UITheme.FONT_REGULAR.deriveFont(13f));
        field.setForeground(INK);
        field.setBackground(UITheme.SURFACE);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.GRID_LINE),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)));
    }

    /** Toggle echo on one or more password fields (show = plain text). */
    public static void setPasswordVisible(boolean visible, JPasswordField... fields) {
        if (fields == null) {
            return;
        }
        char echo = visible ? (char) 0 : '\u2022';
        for (JPasswordField f : fields) {
            if (f != null) {
                f.setEchoChar(echo);
            }
        }
    }

    /** Checkbox that shows/hides the given password fields. */
    public static JCheckBox createShowPasswordCheck(String label, JPasswordField... fields) {
        JCheckBox cb = new JCheckBox(label == null ? "Show password" : label);
        cb.setOpaque(false);
        cb.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        cb.setForeground(UITheme.TEXT_SECONDARY);
        cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cb.setFocusPainted(false);
        cb.addActionListener(e -> setPasswordVisible(cb.isSelected(), fields));
        return cb;
    }

    /** Password field with a "Show" checkbox on the right. */
    public static JPanel wrapPasswordField(JPasswordField field) {
        return wrapPasswordField(field, "Show");
    }

    public static JPanel wrapPasswordField(JPasswordField field, String checkLabel) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        if (field != null) {
            styleField(field);
            row.add(field, BorderLayout.CENTER);
            row.add(createShowPasswordCheck(checkLabel, field), BorderLayout.EAST);
        }
        return row;
    }

    public static void styleLabel(JLabel label) {
        label.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        label.setForeground(UITheme.TEXT_SECONDARY);
    }

    public static void styleCaption(JLabel label) {
        label.setFont(UITheme.FONT_CAPTION);
        label.setForeground(UITheme.TEXT_CAPTION);
    }

    public static void styleTable(JTable table) {
        table.setFont(UITheme.FONT_REGULAR.deriveFont(12f));
        table.setForeground(INK);
        table.setBackground(UITheme.SURFACE);
        table.setGridColor(UITheme.NAV_RULE);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(UITheme.ACCENT_LIGHT);
        table.setSelectionForeground(INK);
        table.getTableHeader().setFont(UITheme.FONT_CAPTION);
        table.getTableHeader().setForeground(UITheme.TEXT_CAPTION);
        table.getTableHeader().setBackground(UITheme.SURFACE);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.GRID_LINE));
        ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer())
            .setHorizontalAlignment(SwingConstants.LEFT);
    }

    public static void styleScroll(JScrollPane scroll) {
        scroll.setBorder(hairline());
        scroll.getViewport().setBackground(UITheme.SURFACE);
    }

    /**
     * Recursively restyles a generated AbsoluteLayout form: page bg, accent
     * primary buttons, editorial tables/fields. Call after initComponents().
     */
    public static void restyleTree(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JPanel) {
                JPanel p = (JPanel) child;
                Color bg = p.getBackground();
                if (bg != null && (bg.equals(UITheme.BACKGROUND) || bg.equals(UITheme.PRIMARY_LIGHT)
                        || isNearBlueSidebar(bg))) {
                    p.setBackground(UITheme.PAGE_BG);
                }
            } else if (child instanceof JButton) {
                restyleButton((JButton) child);
            } else if (child instanceof JTable) {
                styleTable((JTable) child);
            } else if (child instanceof JScrollPane) {
                JScrollPane sp = (JScrollPane) child;
                styleScroll(sp);
                Component view = sp.getViewport().getView();
                if (view instanceof JTable) {
                    styleTable((JTable) view);
                }
            } else if (child instanceof JTextField) {
                styleField((JTextField) child);
            } else if (child instanceof JLabel) {
                JLabel lb = (JLabel) child;
                Font f = lb.getFont();
                if (f != null && f.getSize() >= 20) {
                    lb.setFont(UITheme.FONT_HEADING.deriveFont(28f));
                    lb.setForeground(INK);
                } else if (lb.getForeground() != null
                        && (lb.getForeground().equals(UITheme.TEXT_SECONDARY)
                        || lb.getForeground().equals(UITheme.TEXT_PRIMARY))) {
                    // keep secondary labels muted; titles already handled
                    if (f != null && f.isBold() && f.getSize() <= 14) {
                        lb.setForeground(UITheme.TEXT_SECONDARY);
                    }
                }
            }
            if (child instanceof Container) {
                restyleTree((Container) child);
            }
        }
    }

    private static boolean isNearBlueSidebar(Color bg) {
        return bg.equals(UITheme.SIDEBAR_BG);
    }

    private static void restyleButton(JButton button) {
        String text = button.getText() == null ? "" : button.getText().toLowerCase();
        Color bg = button.getBackground();
        boolean isDanger = text.contains("hapus") || text.contains("delete")
                || (bg != null && bg.equals(UITheme.DANGER));
        boolean isGhost = text.contains("batal") || text.contains("cancel")
                || text.contains("segarkan") || text.contains("refresh");
        boolean isPrimary = (bg != null && (bg.equals(UITheme.PRIMARY) || bg.equals(UITheme.PRIMARY_DARK)
                || bg.equals(UITheme.SUCCESS) || bg.equals(UITheme.ACCENT)))
                || text.contains("simpan") || text.contains("save") || text.contains("tambah")
                || text.contains("add") || text.contains("edit") || text.contains("cari")
                || text.contains("search") || text.contains("login") || text.contains("cetak")
                || text.contains("print") || text.contains("apply");

        if (isDanger) {
            styleDangerButton(button);
        } else if (isGhost) {
            styleGhostButton(button);
        } else if (isPrimary || (bg != null && !bg.equals(UITheme.SURFACE))) {
            stylePrimaryButton(button);
        }
    }
}
