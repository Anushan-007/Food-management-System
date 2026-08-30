package citybites.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Centralised design system for CityBites.
 * All colours, fonts, spacing and component factories live here.
 *
 * Button hierarchy:
 *   primaryBtn     – filled orange, white text   (primary CTA)
 *   secondaryBtn   – white bg, visible border    (secondary actions)
 *   ghostBtn       – transparent bg, muted text  (low-priority actions)
 *   dangerBtn      – filled red, white text      (destructive, e.g. Delete)
 *   dangerOutlineBtn – red border/text, light bg (less alarming danger)
 *   successBtn     – filled green, white text    (confirm/complete)
 *   wideBtn(color) – full-width variant for dashboards
 */
public final class AppTheme {

    // ── Brand colours ────────────────────────────────────────────────────────
    public static final Color BRAND_PRIMARY   = new Color(192,  57,  43);  // deep red  (danger only)
    public static final Color BRAND_SECONDARY = new Color( 44,  62,  80);  // slate navy
    public static final Color BRAND_ACCENT    = new Color(230, 126,  34);  // warm orange — primary CTA

    // ── Backgrounds ──────────────────────────────────────────────────────────
    public static final Color BG_MAIN    = new Color(245, 246, 248);
    public static final Color BG_CARD    = Color.WHITE;
    public static final Color BG_HEADER  = new Color( 44,  62,  80);  // slate navy
    public static final Color BG_FOOTER  = new Color(236, 240, 241);
    public static final Color BG_INPUT   = Color.WHITE;
    public static final Color BG_HERO    = new Color( 36,  52,  68);  // slightly lighter navy for hero
    public static final Color BG_ACCENT_SOFT = new Color(255, 244, 230); // soft orange tint

    // ── Text ─────────────────────────────────────────────────────────────────
    public static final Color TEXT_PRIMARY = new Color( 44,  62,  80);
    public static final Color TEXT_MUTED   = new Color(100, 116, 139);
    public static final Color TEXT_WHITE   = Color.WHITE;
    public static final Color TEXT_LINK    = new Color( 41, 128, 185);
    public static final Color TEXT_ACCENT  = BRAND_ACCENT;

    // ── Semantic colours ──────────────────────────────────────────────────────
    public static final Color SUCCESS = new Color( 39, 174,  96);
    public static final Color WARNING = new Color(180, 130,   0);  // darker yellow for readability
    public static final Color DANGER  = new Color(192,  57,  43);
    public static final Color INFO    = new Color( 41, 128, 185);

    // ── Status badge backgrounds (light tints) ────────────────────────────────
    private static final Color STATUS_BG_PENDING   = new Color(255, 249, 219);
    private static final Color STATUS_BG_PREPARING = new Color(219, 234, 254);
    private static final Color STATUS_BG_READY     = new Color(255, 237, 213);
    private static final Color STATUS_BG_COMPLETED = new Color(220, 252, 231);
    private static final Color STATUS_BG_CANCELLED = new Color(254, 226, 226);

    // ── Borders / dividers ────────────────────────────────────────────────────
    public static final Color BORDER       = new Color(203, 213, 225);
    public static final Color BORDER_FOCUS = BRAND_ACCENT;

    // ── Fonts ─────────────────────────────────────────────────────────────────
    public static final Font FONT_LOGO    = new Font("Segoe UI", Font.BOLD,  30);
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD,  22);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD,  16);
    public static final Font FONT_SUBHEAD = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FONT_BODY    = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_LABEL   = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BADGE   = new Font("Segoe UI", Font.BOLD,  11);

    // ── Spacing ───────────────────────────────────────────────────────────────
    public static final int PAD_SM = 8;
    public static final int PAD_MD = 14;
    public static final int PAD_LG = 20;
    public static final int PAD_XL = 32;

    // ── Button height ─────────────────────────────────────────────────────────
    public static final int BTN_H = 38;

    private AppTheme() {}

    // ── Button factories ──────────────────────────────────────────────────────

    /** Primary CTA — filled orange, white text. */
    public static JButton primaryBtn(String text) {
        JButton b = baseBtn(text, BRAND_ACCENT, TEXT_WHITE, FONT_SUBHEAD);
        b.setPreferredSize(new Dimension(120, BTN_H));
        return b;
    }

    /** Secondary — white background, visible border. */
    public static JButton secondaryBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(FONT_BODY);
        b.setBackground(BG_CARD);
        b.setForeground(TEXT_PRIMARY);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(100, BTN_H));
        return b;
    }

    /** Ghost — transparent background, muted text. */
    public static JButton ghostBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(FONT_BODY);
        b.setForeground(TEXT_MUTED);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(100, BTN_H));
        return b;
    }

    /** Danger — filled red background, white text (e.g. Delete). */
    public static JButton dangerBtn(String text) {
        JButton b = baseBtn(text, DANGER, TEXT_WHITE, FONT_SUBHEAD);
        b.setPreferredSize(new Dimension(100, BTN_H));
        return b;
    }

    /** Danger outline — red border and text, very light red background. */
    public static JButton dangerOutlineBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(FONT_BODY);
        b.setForeground(DANGER);
        b.setBackground(STATUS_BG_CANCELLED);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DANGER, 1),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(100, BTN_H));
        return b;
    }

    /** Success — green background, white text. */
    public static JButton successBtn(String text) {
        JButton b = baseBtn(text, SUCCESS, TEXT_WHITE, FONT_SUBHEAD);
        b.setPreferredSize(new Dimension(130, BTN_H));
        return b;
    }

    /** Wide button for dashboard navigation cards. */
    public static JButton wideBtn(String text, Color bg) {
        JButton b = baseBtn(text, bg, TEXT_WHITE, FONT_SUBHEAD);
        b.setPreferredSize(new Dimension(220, BTN_H));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, BTN_H));
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

    // ── Header / navigation panels ────────────────────────────────────────────

    /**
     * Simple section header strip (used by screens that don't need a full nav bar).
     */
    public static JPanel headerPanel(String title) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, PAD_LG, PAD_MD));
        p.setBackground(BG_HEADER);
        JLabel l = new JLabel(title);
        l.setFont(FONT_HEADING);
        l.setForeground(TEXT_WHITE);
        p.add(l);
        return p;
    }

    /**
     * Full application navigation header with logo (left) and right-side controls.
     *
     * @param portalName  small subtitle under the logo, e.g. "Admin Portal"
     * @param userLabel   user display string, e.g. "Welcome, Admin" — may be null
     * @param rightButtons  any action buttons to show right-aligned
     */
    public static JPanel navHeader(String portalName, String userLabel,
                                   JButton... rightButtons) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_HEADER);
        p.setBorder(BorderFactory.createEmptyBorder(PAD_MD, PAD_LG, PAD_MD, PAD_LG));

        // Left: CITY BITES logo + portal name
        JLabel logo = new JLabel("CITY BITES");
        logo.setFont(FONT_LOGO);
        logo.setForeground(BRAND_ACCENT);

        JLabel portal = new JLabel(portalName != null ? portalName : "");
        portal.setFont(FONT_SMALL);
        portal.setForeground(new Color(150, 170, 190));

        JPanel left = new JPanel();
        left.setBackground(BG_HEADER);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(logo);
        if (portalName != null && !portalName.isBlank()) left.add(portal);

        // Right: user label + buttons
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setBackground(BG_HEADER);

        if (userLabel != null && !userLabel.isBlank()) {
            JLabel user = new JLabel(userLabel);
            user.setFont(FONT_SMALL);
            user.setForeground(new Color(150, 170, 190));
            right.add(user);
        }
        for (JButton btn : rightButtons) right.add(btn);

        p.add(left,  BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    // ── Table styling ─────────────────────────────────────────────────────────

    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setRowHeight(28);
        table.setGridColor(BORDER);
        table.setSelectionBackground(BG_ACCENT_SOFT);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());
    }

    // ── Input field styling ───────────────────────────────────────────────────

    public static void styleField(JTextField f) {
        f.setFont(FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        f.setBackground(BG_INPUT);
    }

    public static void styleField(JPasswordField f) {
        f.setFont(FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        f.setBackground(BG_INPUT);
    }

    // ── Panel / border helpers ────────────────────────────────────────────────

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

    // ── Stat / summary card factory ───────────────────────────────────────────

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

    // ── Status badge ──────────────────────────────────────────────────────────

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

    /** Creates a colored pill-style status badge JLabel. */
    public static JLabel statusBadge(String status) {
        Color fg = statusColor(status);
        Color bg = switch (status == null ? "" : status) {
            case "Pending"   -> STATUS_BG_PENDING;
            case "Preparing" -> STATUS_BG_PREPARING;
            case "Ready"     -> STATUS_BG_READY;
            case "Completed" -> STATUS_BG_COMPLETED;
            case "Cancelled" -> STATUS_BG_CANCELLED;
            default          -> BG_FOOTER;
        };
        JLabel badge = new JLabel(status != null ? status : "—");
        badge.setFont(FONT_BADGE);
        badge.setForeground(fg);
        badge.setOpaque(true);
        badge.setBackground(bg);
        badge.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(fg, 1),
            BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        return badge;
    }

    // ── Empty state panel ─────────────────────────────────────────────────────

    /**
     * A centered panel for "no data" states.
     * @param iconText  large text symbol or empty string
     * @param title     e.g. "Your cart is empty"
     * @param subtitle  e.g. "Add some items from the menu"
     */
    public static JPanel emptyStatePanel(String iconText, String title, String subtitle) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_MAIN);

        if (iconText != null && !iconText.isBlank()) {
            JLabel icon = new JLabel(iconText);
            icon.setFont(new Font("Segoe UI", Font.PLAIN, 48));
            icon.setForeground(new Color(200, 210, 220));
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(Box.createVerticalStrut(40));
            p.add(icon);
            p.add(Box.createVerticalStrut(16));
        } else {
            p.add(Box.createVerticalStrut(60));
        }

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(FONT_HEADING);
        titleLbl.setForeground(TEXT_MUTED);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(FONT_BODY);
        subLbl.setForeground(new Color(160, 175, 190));
        subLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(titleLbl);
        p.add(Box.createVerticalStrut(8));
        p.add(subLbl);
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ── Toast notification ────────────────────────────────────────────────────

    private static JWindow  currentToast;
    private static Timer    toastTimer;

    /**
     * Shows a non-blocking toast notification near the bottom-right of {@code owner}.
     * Auto-closes after 2.5 seconds. Replaces any previous toast immediately.
     */
    public static void showToast(Window owner, String message) {
        // Dismiss any active toast
        if (toastTimer  != null) { toastTimer.stop();  toastTimer  = null; }
        if (currentToast != null) { currentToast.dispose(); currentToast = null; }

        JWindow toast = new JWindow(owner);
        currentToast = toast;

        JLabel lbl = new JLabel(message);
        lbl.setFont(FONT_BODY);
        lbl.setForeground(TEXT_WHITE);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BRAND_SECONDARY);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BRAND_ACCENT, 2),
            BorderFactory.createEmptyBorder(10, 18, 10, 18)));
        panel.add(lbl, BorderLayout.CENTER);

        toast.getContentPane().add(panel);
        toast.pack();

        // Position: bottom-right of owner window
        if (owner != null) {
            Point      loc  = owner.getLocationOnScreen();
            Dimension  own  = owner.getSize();
            Dimension  tsz  = toast.getPreferredSize();
            toast.setLocation(loc.x + own.width - tsz.width - 20,
                              loc.y + own.height - tsz.height - 56);
        }

        toast.setVisible(true);

        toastTimer = new Timer(2500, e -> {
            toast.dispose();
            if (currentToast == toast) currentToast = null;
        });
        toastTimer.setRepeats(false);
        toastTimer.start();
    }

    // ── Styled dialog factories ───────────────────────────────────────────────

    /** Styled information/success dialog. */
    public static void showInfo(Component parent, String title, String message) {
        buildMessageDialog(parent, title, message, SUCCESS, "OK");
    }

    /** Styled warning dialog. */
    public static void showWarning(Component parent, String title, String message) {
        buildMessageDialog(parent, title, message, WARNING, "OK");
    }

    /** Styled error dialog. */
    public static void showError(Component parent, String title, String message) {
        buildMessageDialog(parent, title, message, DANGER, "OK");
    }

    /**
     * Styled Yes/No confirmation dialog.
     * @return true if the user clicked Yes.
     */
    public static boolean showConfirm(Component parent, String title, String message) {
        return buildConfirmDialog(parent, title, message);
    }

    // ── Private dialog builders ───────────────────────────────────────────────

    private static void buildMessageDialog(Component parent, String title,
                                           String message, Color accent, String btnText) {
        Window owner = resolveOwner(parent);
        JDialog dlg  = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setResizable(false);

        JPanel content = new JPanel();
        content.setBackground(BG_CARD);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(0, 28, 24, 28));

        // Accent top strip
        JPanel strip = new JPanel();
        strip.setBackground(accent);
        strip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        strip.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(strip);
        content.add(Box.createVerticalStrut(18));

        // Message
        JLabel msg = new JLabel(
            "<html><body style='width:260px;font-family:Segoe UI;font-size:9pt'>"
            + message.replace("\n", "<br>") + "</body></html>");
        msg.setFont(FONT_BODY);
        msg.setForeground(TEXT_PRIMARY);
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(msg);
        content.add(Box.createVerticalStrut(20));

        // Button
        JButton btn = primaryBtn(btnText);
        btn.setPreferredSize(new Dimension(110, BTN_H));
        btn.setMaximumSize(new Dimension(160, BTN_H));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.addActionListener(e -> dlg.dispose());
        content.add(btn);

        dlg.getContentPane().add(content);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(340, 0));
        dlg.setLocationRelativeTo(parent);
        dlg.getRootPane().setDefaultButton(btn);
        dlg.setVisible(true);
    }

    private static boolean buildConfirmDialog(Component parent, String title, String message) {
        Window owner = resolveOwner(parent);
        final boolean[] result = {false};

        JDialog dlg = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setResizable(false);

        JPanel content = new JPanel();
        content.setBackground(BG_CARD);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(0, 28, 24, 28));

        // Accent strip
        JPanel strip = new JPanel();
        strip.setBackground(BRAND_SECONDARY);
        strip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        strip.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(strip);
        content.add(Box.createVerticalStrut(18));

        // Message
        JLabel msg = new JLabel(
            "<html><body style='width:260px;font-family:Segoe UI;font-size:9pt'>"
            + message.replace("\n", "<br>") + "</body></html>");
        msg.setFont(FONT_BODY);
        msg.setForeground(TEXT_PRIMARY);
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(msg);
        content.add(Box.createVerticalStrut(20));

        // Buttons row
        JButton btnYes = primaryBtn("Yes");
        JButton btnNo  = secondaryBtn("No");
        btnYes.setPreferredSize(new Dimension(100, BTN_H));
        btnNo.setPreferredSize(new Dimension(100, BTN_H));
        btnYes.addActionListener(e -> { result[0] = true; dlg.dispose(); });
        btnNo.addActionListener(e  -> dlg.dispose());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setBackground(BG_CARD);
        btnRow.add(btnYes);
        btnRow.add(btnNo);
        btnRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(btnRow);

        dlg.getContentPane().add(content);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(340, 0));
        dlg.setLocationRelativeTo(parent);
        dlg.getRootPane().setDefaultButton(btnYes);

        // Escape = cancel
        KeyStroke esc = KeyStroke.getKeyStroke("ESCAPE");
        dlg.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "escape");
        dlg.getRootPane().getActionMap().put("escape", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dlg.dispose(); }
        });

        dlg.setVisible(true);
        return result[0];
    }

    private static Window resolveOwner(Component parent) {
        if (parent instanceof Window) return (Window) parent;
        return SwingUtilities.getWindowAncestor(parent);
    }

    // ── Inner header renderer ─────────────────────────────────────────────────

    public static class HeaderRenderer extends DefaultTableCellRenderer {
        public HeaderRenderer() {
            setOpaque(true);
            setBackground(BG_HEADER);
            setForeground(TEXT_WHITE);
            setFont(FONT_SUBHEAD);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(60, 80, 100)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean s, boolean f, int r, int c) {
            setText(v != null ? v.toString() : "");
            return this;
        }
    }

    // ── Inner status-cell renderer ────────────────────────────────────────────

    /** Applies coloured status text in table cells. */
    public static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected && value != null) {
                setForeground(AppTheme.statusColor(value.toString()));
                setFont(AppTheme.FONT_SUBHEAD);
            }
            return this;
        }
    }
}
