package citybites.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Centralised design system for CityBites.
 * All colours, fonts, spacing and component factories live here.
 */
public final class AppTheme {

    // Brand colours
    public static final Color BRAND_PRIMARY   = new Color(192,  57,  43);  // deep red
    public static final Color BRAND_SECONDARY = new Color( 44,  62,  80);  // slate navy
    public static final Color BRAND_ACCENT    = new Color(230, 126,  34);  // warm orange

    // Backgrounds
    public static final Color BG_MAIN    = new Color(245, 246, 248);
    public static final Color BG_CARD    = Color.WHITE;
    public static final Color BG_HEADER  = new Color( 44,  62,  80);
    public static final Color BG_FOOTER  = new Color(236, 240, 241);
    public static final Color BG_INPUT   = Color.WHITE;

    // Text
    public static final Color TEXT_PRIMARY = new Color( 44,  62,  80);
    public static final Color TEXT_MUTED   = new Color(100, 116, 139);
    public static final Color TEXT_WHITE   = Color.WHITE;
    public static final Color TEXT_LINK    = new Color( 41, 128, 185);

    // Semantic colours
    public static final Color SUCCESS = new Color( 39, 174,  96);
    public static final Color WARNING = new Color(241, 196,  15);
    public static final Color DANGER  = new Color(192,  57,  43);
    public static final Color INFO    = new Color( 41, 128, 185);

    // Borders / dividers
    public static final Color BORDER = new Color(203, 213, 225);

    // Fonts
    public static final Font FONT_LOGO    = new Font("Segoe UI", Font.BOLD,  30);
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD,  22);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD,  16);
    public static final Font FONT_SUBHEAD = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FONT_BODY    = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_LABEL   = new Font("Segoe UI", Font.PLAIN, 13);

    // Spacing
    public static final int PAD_SM = 8;
    public static final int PAD_MD = 14;
    public static final int PAD_LG = 20;

    private AppTheme() {}

    // ── Button factories ────────────────────────────────────────────────────

    public static JButton primaryBtn(String text) {
        JButton b = baseBtn(text, BRAND_PRIMARY, TEXT_WHITE, FONT_SUBHEAD);
        b.setPreferredSize(new Dimension(120, 34));
        return b;
    }

    public static JButton secondaryBtn(String text) {
        JButton b = baseBtn(text, BG_FOOTER, TEXT_PRIMARY, FONT_BODY);
        b.setBorder(BorderFactory.createLineBorder(BORDER));
        b.setPreferredSize(new Dimension(100, 34));
        return b;
    }

    public static JButton dangerBtn(String text) {
        JButton b = baseBtn(text, DANGER, TEXT_WHITE, FONT_SUBHEAD);
        b.setPreferredSize(new Dimension(100, 34));
        return b;
    }

    public static JButton successBtn(String text) {
        JButton b = baseBtn(text, SUCCESS, TEXT_WHITE, FONT_SUBHEAD);
        b.setPreferredSize(new Dimension(130, 34));
        return b;
    }

    public static JButton wideBtn(String text, Color bg) {
        JButton b = baseBtn(text, bg, TEXT_WHITE, FONT_SUBHEAD);
        b.setPreferredSize(new Dimension(220, 40));
        b.setMaximumSize(new Dimension(220, 40));
        return b;
    }

    private static JButton baseBtn(String text, Color bg, Color fg, Font f) {
        JButton b = new JButton(text);
        b.setFont(f);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ── Table styling ───────────────────────────────────────────────────────

    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setRowHeight(26);
        table.setGridColor(BORDER);
        table.setSelectionBackground(new Color(220, 232, 250));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());
    }

    // ── Input field styling ─────────────────────────────────────────────────

    public static void styleField(JTextField f) {
        f.setFont(FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        f.setBackground(BG_INPUT);
    }

    // ── Panel / border helpers ──────────────────────────────────────────────

    public static JPanel headerPanel(String title) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, PAD_LG, PAD_MD));
        p.setBackground(BG_HEADER);
        JLabel l = new JLabel(title);
        l.setFont(FONT_HEADING);
        l.setForeground(TEXT_WHITE);
        p.add(l);
        return p;
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(PAD_MD, PAD_MD, PAD_MD, PAD_MD));
    }

    public static Border footerBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
            BorderFactory.createEmptyBorder(PAD_SM, PAD_MD, PAD_SM, PAD_MD));
    }

    // ── Summary card factory ────────────────────────────────────────────────

    public static JPanel summaryCard(String label, String value, Color accent) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(16, 16, 16, 16))));

        JLabel valLabel = new JLabel(value);
        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valLabel.setForeground(accent);
        valLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(FONT_SMALL);
        lblLabel.setForeground(TEXT_MUTED);
        lblLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(valLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(lblLabel);
        return card;
    }

    // ── Status badge ────────────────────────────────────────────────────────

    public static Color statusColor(String status) {
        if (status == null) return TEXT_MUTED;
        return switch (status) {
            case "Pending"   -> WARNING;
            case "Preparing" -> INFO;
            case "Ready"     -> BRAND_ACCENT;
            case "Completed" -> SUCCESS;
            case "Cancelled" -> DANGER;
            default          -> TEXT_MUTED;
        };
    }

    // ── Inner header renderer ────────────────────────────────────────────────

    public static class HeaderRenderer extends DefaultTableCellRenderer {
        public HeaderRenderer() {
            setOpaque(true);
            setBackground(BG_HEADER);
            setForeground(TEXT_WHITE);
            setFont(FONT_SUBHEAD);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(60, 80, 100)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            setText(v != null ? v.toString() : "");
            return this;
        }
    }
}
