/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Main;

import java.awt.event.ActionListener;

/**
 * A single row in the sidebar. Two flavors:
 *  - section header ("OVERVIEW", "MASTER DATA", ...): static, non-clickable caption
 *  - nav item ("01  Dashboard"): numbered, clickable, highlights red when selected
 *
 * Spacing is tuned to the editorial reference: generous row height, fixed
 * index column, and a clear left safe-zone.
 */
public class MenuItem extends javax.swing.JPanel {

    /** Left margin from sidebar edge to content. */
    private static final int PAD_X = 24;
    /** Fixed width for the "01" index so labels share one vertical edge. */
    private static final int INDEX_W = 26;
    /** Gutter between index and label. */
    private static final int INDEX_GAP = 14;
    /** Clickable nav row height (includes vertical padding). */
    private static final int ROW_H = 44;

    private final boolean header;
    private ActionListener act;
    private boolean selected = false;

    /** Section header row, e.g. "OVERVIEW". */
    public MenuItem(String sectionTitle) {
        this(sectionTitle, false);
    }

    /** Section header; set {@code withTopRule} for a hairline above (Master Data / Transactions). */
    public MenuItem(String sectionTitle, boolean withTopRule) {
        this.header = true;
        initComponents();
        remove(lb_index);
        lb_index.setVisible(false);
        lb_menuName.setText(sectionTitle.toUpperCase());
        lb_menuName.setFont(Main.UITheme.FONT_CAPTION.deriveFont(10f));
        lb_menuName.setForeground(Main.UITheme.TEXT_CAPTION);
        setBackground(Main.UITheme.NAV_BG);
        setCursor(java.awt.Cursor.getDefaultCursor());
        setLayout(new java.awt.BorderLayout());
        add(lb_menuName, java.awt.BorderLayout.WEST);

        // Top rule + breathing room above the caption; tight gap below into first item.
        int padTop = withTopRule ? 18 : 16;
        int padBottom = 8;
        int height = padTop + padBottom + 14;
        setBorder(javax.swing.BorderFactory.createCompoundBorder(
            withTopRule
                ? javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, Main.UITheme.NAV_RULE)
                : javax.swing.BorderFactory.createEmptyBorder(),
            javax.swing.BorderFactory.createEmptyBorder(padTop, PAD_X, padBottom, 16)));

        java.awt.Dimension d = new java.awt.Dimension(Integer.MAX_VALUE, height + (withTopRule ? 1 : 0));
        setPreferredSize(d);
        setMaximumSize(d);
        setMinimumSize(d);
    }

    /** Clickable, numbered nav row, e.g. "01  Dashboard". */
    public MenuItem(int index, String menuName, ActionListener act) {
        this.header = false;
        this.act = act;
        initComponents();
        lb_index.setText(String.format("%02d", index));
        lb_menuName.setText(menuName);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(0, PAD_X, 0, 16));

        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (MenuItem.this.act != null) {
                    MenuItem.this.act.actionPerformed(null);
                }
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (!selected) {
                    setBackground(Main.UITheme.DIVIDER);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (!selected) {
                    setBackground(Main.UITheme.NAV_BG);
                }
            }
        });

        java.awt.Dimension d = new java.awt.Dimension(Integer.MAX_VALUE, ROW_H);
        setPreferredSize(d);
        setMaximumSize(d);
        setMinimumSize(d);
    }

    public boolean isHeader() {
        return header;
    }

    public String getMenuName() {
        return lb_menuName != null ? lb_menuName.getText() : "";
    }

    /** Programmatically fire the same action as a sidebar click. */
    public void activate() {
        if (!header && act != null) {
            act.actionPerformed(null);
        }
    }

    public void setSelected(boolean value) {
        this.selected = value;
        if (value) {
            setBackground(Main.UITheme.ACCENT);
            lb_index.setForeground(java.awt.Color.WHITE);
            lb_menuName.setForeground(java.awt.Color.WHITE);
            lb_menuName.setFont(Main.UITheme.FONT_BOLD.deriveFont(13f));
        } else {
            setBackground(Main.UITheme.NAV_BG);
            lb_index.setForeground(Main.UITheme.TEXT_MUTED);
            lb_menuName.setForeground(new java.awt.Color(0x1A1A1A));
            lb_menuName.setFont(Main.UITheme.FONT_REGULAR.deriveFont(13f));
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        lb_index = new javax.swing.JLabel();
        lb_menuName = new javax.swing.JLabel();

        setBackground(Main.UITheme.NAV_BG);
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        setOpaque(true);

        lb_index.setFont(Main.UITheme.FONT_REGULAR.deriveFont(11f));
        lb_index.setForeground(Main.UITheme.TEXT_MUTED);
        lb_index.setText("00");
        lb_index.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);

        lb_menuName.setFont(Main.UITheme.FONT_REGULAR.deriveFont(13f));
        lb_menuName.setForeground(new java.awt.Color(0x1A1A1A));
        lb_menuName.setText("Menu Item");

        setLayout(new java.awt.BorderLayout(INDEX_GAP, 0));
        lb_index.setPreferredSize(new java.awt.Dimension(INDEX_W, ROW_H));
        add(lb_index, java.awt.BorderLayout.WEST);
        add(lb_menuName, java.awt.BorderLayout.CENTER);
    }

    private javax.swing.JLabel lb_index;
    private javax.swing.JLabel lb_menuName;
}
