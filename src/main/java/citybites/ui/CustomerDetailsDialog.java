package citybites.ui;

import citybites.model.Customer;
import citybites.service.CustomerManagementService;
import citybites.service.CustomerProfileService;
import citybites.util.ProfileImageManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.border.Border;

/**
 * Modal dialog that shows the full read-only profile of a customer.
 * Opened by the admin from Customer Management via the "View Details" button
 * or by double-clicking a table row.
 *
 * <p>Security guarantees:
 * <ul>
 *   <li>Never displays customer_id, password, or password_hash.</li>
 *   <li>Never displays profile_image_path as a string.</li>
 *   <li>Profile image is loaded only through {@link ProfileImageManager}.</li>
 *   <li>No edit or import capability — purely read-only.</li>
 * </ul>
 */
public class CustomerDetailsDialog extends JDialog {

    private static final DateTimeFormatter DOB_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy");

    /**
     * Constructs and displays the dialog.
     *
     * @param owner      the parent frame (CustomerManagementFrame)
     * @param customerId the ID of the customer to display — fetched via service
     */
    public CustomerDetailsDialog(Frame owner, int customerId) {
        super(owner, "Customer Profile", true);

        Customer customer;
        try {
            customer = CustomerManagementService.getCustomerDetails(customerId);
        } catch (IllegalStateException ex) {
            AppTheme.showWarning(owner, "Not Found", ex.getMessage());
            dispose();
            return;
        } catch (Exception ex) {
            AppTheme.showError(owner, "Error", "Could not load customer: " + ex.getMessage());
            dispose();
            return;
        }

        buildContent(customer);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setMinimumSize(new Dimension(540, 460));
        setResizable(true);
        setLocationRelativeTo(owner);
    }

    // ── Layout builder ─────────────────────────────────────────────────────────

    private void buildContent(Customer c) {
        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());

        getContentPane().add(buildHeader(c), BorderLayout.NORTH);
        getContentPane().add(buildScrollPane(c), BorderLayout.CENTER);
        getContentPane().add(buildFooter(), BorderLayout.SOUTH);

        // Escape key closes dialog
        KeyStroke esc = KeyStroke.getKeyStroke("ESCAPE");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });
    }

    /** Dark nav-style header with title, customer name, and a close button. */
    private JPanel buildHeader(Customer c) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppTheme.BG_HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JPanel left = new JPanel();
        left.setBackground(AppTheme.BG_HEADER);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel titleLbl = new JLabel("Customer Profile");
        titleLbl.setFont(AppTheme.FONT_HEADING);
        titleLbl.setForeground(AppTheme.BRAND_ACCENT);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel nameLbl = new JLabel(c.getFullName());
        nameLbl.setFont(AppTheme.FONT_BODY);
        nameLbl.setForeground(new Color(180, 200, 220));
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(titleLbl);
        left.add(Box.createVerticalStrut(2));
        left.add(nameLbl);

        JButton closeBtn = AppTheme.ghostBtn("✕  Close");
        closeBtn.setForeground(new Color(180, 200, 220));
        closeBtn.addActionListener(e -> dispose());

        header.add(left,     BorderLayout.WEST);
        header.add(closeBtn, BorderLayout.EAST);
        return header;
    }

    /** Builds the scrollable content area with photo, account, contact, personal sections. */
    private JScrollPane buildScrollPane(Customer c) {
        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(AppTheme.BG_MAIN);
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx    = 0;
        gc.fill     = GridBagConstraints.HORIZONTAL;
        gc.weightx  = 1.0;
        gc.weighty  = 0.0;
        gc.anchor   = GridBagConstraints.NORTHWEST;
        gc.insets   = new Insets(0, 0, 12, 0);

        // Row 0: profile photo  +  account details side-by-side
        gc.gridy = 0;
        content.add(buildTopSection(c), gc);

        // Row 1: contact details (full width)
        gc.gridy = 1;
        content.add(buildSectionCard("Contact Details", new String[][]{
            {"Email",            orNotProvided(c.getEmail())},
            {"Phone Number",     orNotProvided(c.getPhoneNumber())},
            {"Delivery Address", orNotProvided(c.getDeliveryAddress())}
        }), gc);

        // Row 2: personal details (full width)
        String dobStr = (c.getDateOfBirth() != null)
                ? c.getDateOfBirth().format(DOB_FMT) : "Not provided";
        int age = CustomerProfileService.calculateAge(c.getDateOfBirth());
        String ageStr = (age >= 0) ? age + " years" : "Not provided";

        gc.gridy = 2;
        content.add(buildSectionCard("Personal Details", new String[][]{
            {"Date of Birth", dobStr},
            {"Age",           ageStr}
        }), gc);

        // Row 3: vertical filler — pushes content to top when dialog is tall
        gc.gridy   = 3;
        gc.weighty = 1.0;
        gc.fill    = GridBagConstraints.BOTH;
        content.add(new JPanel() { { setOpaque(false); } }, gc);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(AppTheme.BG_MAIN);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    /**
     * Builds the top section: profile photo (left) and account details card (right).
     */
    private JPanel buildTopSection(Customer c) {
        JPanel top = new JPanel(new BorderLayout(16, 0));
        top.setBackground(AppTheme.BG_MAIN);

        // Profile photo — loaded via ProfileImageManager; placeholder if missing
        ImageIcon photo = ProfileImageManager.loadScaled(c.getProfileImagePath(), 140, 140);
        JLabel photoLbl = new JLabel(photo);
        photoLbl.setPreferredSize(new Dimension(140, 140));
        photoLbl.setMinimumSize(new Dimension(140, 140));
        photoLbl.setMaximumSize(new Dimension(140, 140));
        photoLbl.setHorizontalAlignment(SwingConstants.CENTER);
        photoLbl.setVerticalAlignment(SwingConstants.CENTER);
        photoLbl.setOpaque(true);
        photoLbl.setBackground(AppTheme.BG_FOOTER);
        photoLbl.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 2));

        JPanel photoWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        photoWrapper.setBackground(AppTheme.BG_MAIN);
        photoWrapper.add(photoLbl);

        JPanel accountCard = buildSectionCard("Account Details", new String[][]{
            {"Full Name",       c.getFullName()},
            {"Username",        c.getUsername()},
            {"Registered Date", formatDate(c.getCreatedAt())}
        });

        top.add(photoWrapper,  BorderLayout.WEST);
        top.add(accountCard,   BorderLayout.CENTER);
        return top;
    }

    /**
     * Builds a white card panel with a coloured title and labeled field rows.
     *
     * @param title section heading
     * @param rows  each element is {labelText, valueText}
     */
    private JPanel buildSectionCard(String title, String[][] rows) {
        JPanel card = new JPanel();
        card.setBackground(AppTheme.BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(AppTheme.FONT_SUBHEAD);
        titleLbl.setForeground(AppTheme.BRAND_ACCENT);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(titleLbl);
        card.add(Box.createVerticalStrut(8));

        // Thin accent separator under the title
        JSeparator sep = new JSeparator();
        sep.setForeground(AppTheme.BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(sep);
        card.add(Box.createVerticalStrut(8));

        for (String[] row : rows) {
            card.add(fieldRow(row[0], row[1]));
            card.add(Box.createVerticalStrut(5));
        }
        return card;
    }

    /**
     * Returns a panel containing a bold label and a wrapping value component.
     * Long values (e.g. delivery address) wrap cleanly.
     */
    private JPanel fieldRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(10, 2));
        row.setBackground(AppTheme.BG_CARD);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(AppTheme.FONT_LABEL.deriveFont(Font.BOLD));
        lbl.setForeground(AppTheme.TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(140, 22));
        lbl.setMinimumSize(new Dimension(140, 22));
        lbl.setVerticalAlignment(SwingConstants.TOP);

        // JTextArea wraps long values; styled to look like a label
        JTextArea val = new JTextArea(value != null ? value : "Not provided");
        val.setFont(AppTheme.FONT_BODY);
        val.setForeground(AppTheme.TEXT_PRIMARY);
        val.setBackground(AppTheme.BG_CARD);
        val.setEditable(false);
        val.setFocusable(false);
        val.setLineWrap(true);
        val.setWrapStyleWord(true);
        val.setBorder(null);
        val.setOpaque(false);
        val.setCursor(Cursor.getDefaultCursor());

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }

    /** Builds the footer panel with a Close button. */
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 10));
        footer.setBackground(AppTheme.BG_FOOTER);
        footer.setBorder(AppTheme.footerBorder());

        JButton closeBtn = AppTheme.secondaryBtn("Close");
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);
        return footer;
    }

    // ── Display helpers ────────────────────────────────────────────────────────

    /** Returns {@code "Not provided"} when value is null or blank. */
    private static String orNotProvided(String value) {
        return (value == null || value.isBlank()) ? "Not provided" : value;
    }

    /** Trims a timestamp to date-only (first 10 chars). Returns {@code "Not provided"} on null. */
    private static String formatDate(String timestamp) {
        if (timestamp == null || timestamp.length() < 10) return "Not provided";
        return timestamp.substring(0, 10);
    }
}
