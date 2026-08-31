package citybites.ui;

import citybites.model.Customer;
import citybites.service.CustomerProfileService;
import citybites.util.ImageManager;
import citybites.util.ProfileImageManager;
import citybites.util.SessionManager;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.optionalusertools.DateVetoPolicy;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Customer self-service profile editor with explicit View / Edit modes.
 *
 * <h2>Authorization</h2>
 * <p>The customer ID is read <em>exclusively</em> from {@link SessionManager}
 * via {@link CustomerProfileService#getCurrentCustomerProfile()} and
 * {@link CustomerProfileService#updateCurrentCustomerProfile}.
 * No ID is ever derived from a text field or table.
 *
 * <h2>Image lifecycle</h2>
 * <ul>
 *   <li><b>File chosen</b>: source file is previewed with
 *       {@link ImageManager#loadScaledPreview} — nothing is copied yet.</li>
 *   <li><b>Save</b>: source file is imported to
 *       {@code ~/.citybites/profile-images/} via
 *       {@link ProfileImageManager#importImage}; only the relative filename
 *       is stored in MySQL.</li>
 *   <li><b>DB fails after import</b>: newly imported file is deleted (rollback).</li>
 *   <li><b>DB succeeds</b>: old managed profile image (if any) is deleted.</li>
 *   <li><b>Remove Photo → Save</b>: DB path set to NULL; old file deleted.</li>
 *   <li><b>Cancel</b>: pending source file is discarded; nothing is imported.</li>
 * </ul>
 *
 * <h2>Mode states</h2>
 * <ul>
 *   <li><b>View</b>: all editable fields are read-only; only "Edit Profile" is active.</li>
 *   <li><b>Edit</b>: fields editable; "Save" and "Cancel" are active.</li>
 * </ul>
 */
public class CustomerProfileFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(CustomerProfileFrame.class.getName());

    // ── Current persisted profile ─────────────────────────────────────────────
    private Customer profile;

    // ── Edit-mode image state (never persisted until Save) ────────────────────
    /**
     * Source file selected by the user via JFileChooser.
     * {@code null} when no new photo has been chosen in this edit session.
     */
    private File    pendingSourceFile  = null;
    /**
     * {@code true} when the user clicked "Remove Photo" in this edit session.
     * Reset to {@code false} on Cancel or after a successful Save.
     */
    private boolean pendingRemoveImage = false;

    // ── Form controls ─────────────────────────────────────────────────────────
    private JTextField fldFullName;
    private JLabel     lblUsername;
    private JTextField fldEmail;
    private JTextField fldPhone;
    private DatePicker datePickerDob;
    private JLabel     lblAge;
    private JTextArea  fldAddress;
    private JLabel     imgPreview;

    // ── Mode-toggle controls ──────────────────────────────────────────────────
    private JButton btnEdit;
    private JButton btnSave;
    private JButton btnCancel;
    private JButton btnChangePhoto;
    private JButton btnRemovePhoto;

    public CustomerProfileFrame() {
        profile = CustomerProfileService.getCurrentCustomerProfile();
        initComponents();
        setTitle("City Bites - My Profile");
        setMinimumSize(new Dimension(720, 560));
        setSize(840, 700);
        setLocationRelativeTo(null);
        setResizable(true);
        applyViewMode();
    }

    // ── UI construction ────────────────────────────────────────────────────────

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        // ── Nav header ────────────────────────────────────────────────────
        JButton btnBack = AppTheme.ghostBtn("\u2190 Dashboard");
        btnBack.setForeground(new Color(150, 170, 190));
        btnBack.addActionListener(e -> { new CustomerDashboardFrame().setVisible(true); dispose(); });

        JButton btnLogout = AppTheme.ghostBtn("Logout");
        btnLogout.setForeground(new Color(150, 170, 190));
        btnLogout.addActionListener(e -> {
            if (AppTheme.showConfirm(this, "Confirm Logout", "Are you sure you want to logout?")) {
                SessionManager.logout();
                new CustomerLoginFrame().setVisible(true);
                dispose();
            }
        });

        JPanel header = AppTheme.navHeader("My Profile",
                "View and update your account details", btnBack, btnLogout);

        // ── Photo card ─────────────────────────────────────────────────────
        JPanel photoCard = buildPhotoCard();

        // ── Info card ──────────────────────────────────────────────────────
        JPanel infoCard = buildInfoCard();

        // ── Bottom action row ──────────────────────────────────────────────
        btnEdit   = AppTheme.primaryBtn("Edit Profile");
        btnSave   = AppTheme.primaryBtn("Save");
        btnCancel = AppTheme.secondaryBtn("Cancel");

        btnEdit.setPreferredSize(new Dimension(160, AppTheme.BTN_H));
        btnSave.setPreferredSize(new Dimension(120, AppTheme.BTN_H));
        btnCancel.setPreferredSize(new Dimension(100, AppTheme.BTN_H));

        btnEdit.addActionListener(e -> applyEditMode());
        btnSave.addActionListener(e -> saveProfile());
        btnCancel.addActionListener(e -> cancelEdit());

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT,
                AppTheme.PAD_SM, AppTheme.PAD_MD));
        actionRow.setBackground(AppTheme.BG_MAIN);
        actionRow.add(btnCancel);
        actionRow.add(btnSave);
        actionRow.add(btnEdit);

        // ── Scrollable body ────────────────────────────────────────────────
        JPanel body = new JPanel();
        body.setBackground(AppTheme.BG_MAIN);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(photoCard);
        body.add(infoCard);
        body.add(actionRow);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBackground(AppTheme.BG_MAIN);
        scroll.getViewport().setBackground(AppTheme.BG_MAIN);

        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header, BorderLayout.NORTH);
        getContentPane().add(scroll, BorderLayout.CENTER);
    }

    // ── Photo card ─────────────────────────────────────────────────────────────

    private JPanel buildPhotoCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(AppTheme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 5, 0, 0, AppTheme.BRAND_ACCENT),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(20, 20, 20, 20))));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        imgPreview = new JLabel("No Photo", SwingConstants.CENTER);
        imgPreview.setFont(AppTheme.FONT_SMALL);
        imgPreview.setForeground(AppTheme.TEXT_MUTED);
        imgPreview.setPreferredSize(new Dimension(120, 120));
        imgPreview.setMinimumSize(new Dimension(120, 120));
        imgPreview.setMaximumSize(new Dimension(120, 120));
        imgPreview.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 2));
        imgPreview.setBackground(AppTheme.BG_ACCENT_SOFT);
        imgPreview.setOpaque(true);
        refreshPersistedImagePreview();

        btnChangePhoto = AppTheme.secondaryBtn("Change Photo");
        btnChangePhoto.addActionListener(e -> chooseProfilePhoto());

        btnRemovePhoto = AppTheme.ghostBtn("Remove");
        btnRemovePhoto.setForeground(AppTheme.DANGER);
        btnRemovePhoto.addActionListener(e -> {
            pendingSourceFile  = null;
            pendingRemoveImage = true;
            imgPreview.setIcon(null);
            imgPreview.setText("No Photo");
        });

        JPanel photoActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        photoActions.setBackground(AppTheme.BG_CARD);
        photoActions.add(btnChangePhoto);
        photoActions.add(btnRemovePhoto);

        JLabel hint = new JLabel("JPG or PNG only");
        hint.setFont(AppTheme.FONT_SMALL);
        hint.setForeground(AppTheme.TEXT_MUTED);

        JPanel photoRight = new JPanel();
        photoRight.setBackground(AppTheme.BG_CARD);
        photoRight.setLayout(new BoxLayout(photoRight, BoxLayout.Y_AXIS));
        photoRight.setBorder(BorderFactory.createEmptyBorder(16, 20, 0, 0));

        JLabel photoTitle = new JLabel("Profile Photo");
        photoTitle.setFont(AppTheme.FONT_HEADING);
        photoTitle.setForeground(AppTheme.TEXT_PRIMARY);
        photoRight.add(photoTitle);
        photoRight.add(Box.createVerticalStrut(12));
        photoRight.add(photoActions);
        photoRight.add(Box.createVerticalStrut(8));
        photoRight.add(hint);

        card.add(imgPreview, BorderLayout.WEST);
        card.add(photoRight, BorderLayout.CENTER);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(AppTheme.BG_MAIN);
        wrap.setBorder(BorderFactory.createEmptyBorder(
                AppTheme.PAD_LG, AppTheme.PAD_LG, AppTheme.PAD_MD, AppTheme.PAD_LG));
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    // ── Info card ──────────────────────────────────────────────────────────────

    private JPanel buildInfoCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(AppTheme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 5, 0, 0, AppTheme.BRAND_SECONDARY),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(20, 20, 20, 20))));

        JLabel cardTitle = new JLabel("Personal Information");
        cardTitle.setFont(AppTheme.FONT_HEADING);
        cardTitle.setForeground(AppTheme.TEXT_PRIMARY);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(AppTheme.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        fldFullName = new JTextField(profile != null ? profile.getFullName() : "");
        AppTheme.styleField(fldFullName);
        addFormRow(form, "Full Name *", 0, fldFullName);

        // Username — always read-only
        lblUsername = new JLabel(profile != null ? profile.getUsername() : "");
        lblUsername.setFont(AppTheme.FONT_BODY);
        lblUsername.setForeground(AppTheme.TEXT_MUTED);
        addFormRow(form, "Username", 1, lblUsername);

        fldEmail = new JTextField(
            profile != null && profile.getEmail() != null ? profile.getEmail() : "");
        AppTheme.styleField(fldEmail);
        addFormRow(form, "Email", 2, fldEmail);

        fldPhone = new JTextField(
            profile != null && profile.getPhoneNumber() != null ? profile.getPhoneNumber() : "");
        AppTheme.styleField(fldPhone);
        addFormRow(form, "Phone Number", 3, fldPhone);

        // ── Date of Birth — calendar date picker ───────────────────────────
        DatePickerSettings dobSettings = new DatePickerSettings();
        // Safe to set before construction: format and null-date allowance
        dobSettings.setAllowEmptyDates(true);
        dobSettings.setFormatForDatesCommonEra("MMM d, yyyy");

        // DatePicker must be constructed BEFORE setVetoPolicy() is called
        datePickerDob = new DatePicker(dobSettings);

        // Veto policy: restrict selectable range to today − 120 years … today
        // Must be set after DatePicker construction (LGoodDatePicker requirement).
        dobSettings.setVetoPolicy(new DateVetoPolicy() {
            @Override
            public boolean isDateAllowed(LocalDate date) {
                if (date == null) return true;          // null = no date selected, always allowed
                LocalDate today   = LocalDate.now();
                LocalDate minDate = today.minusYears(120);
                return !date.isAfter(today) && !date.isBefore(minDate);
            }
        });

        // Set initial value
        if (profile != null && profile.getDateOfBirth() != null) {
            datePickerDob.setDate(profile.getDateOfBirth());
        }

        // Placeholder text (FlatLaf client property on the inner text field)
        JTextField dobTextField = datePickerDob.getComponentDateTextField();
        dobTextField.putClientProperty("JTextField.placeholderText", "Select date of birth");

        // Refresh the Age label whenever the date changes
        datePickerDob.addDateChangeListener(event -> refreshAge());

        addFormRow(form, "Date of Birth", 4, datePickerDob);
        // ───────────────────────────────────────────────────────────────────

        lblAge = new JLabel();
        lblAge.setFont(AppTheme.FONT_BODY);
        lblAge.setForeground(AppTheme.TEXT_MUTED);
        addFormRow(form, "Age", 5, lblAge);
        refreshAge();

        fldAddress = new JTextArea(3, 30);
        fldAddress.setFont(AppTheme.FONT_BODY);
        fldAddress.setForeground(AppTheme.TEXT_PRIMARY);
        fldAddress.setBackground(AppTheme.BG_INPUT);
        fldAddress.setLineWrap(true);
        fldAddress.setWrapStyleWord(true);
        fldAddress.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        if (profile != null && profile.getDeliveryAddress() != null) {
            fldAddress.setText(profile.getDeliveryAddress());
        }
        JScrollPane addrScroll = new JScrollPane(fldAddress);
        addrScroll.setBorder(null);
        addFormRow(form, "Delivery Address", 6, addrScroll);

        card.add(cardTitle, BorderLayout.NORTH);
        card.add(form,      BorderLayout.CENTER);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(AppTheme.BG_MAIN);
        wrap.setBorder(BorderFactory.createEmptyBorder(
                0, AppTheme.PAD_LG, AppTheme.PAD_MD, AppTheme.PAD_LG));
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    private static void addFormRow(JPanel form, String labelText, int row, Component field) {
        GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = 0; lc.gridy = row;
        lc.anchor = GridBagConstraints.NORTHWEST;
        lc.insets = new Insets(9, 0, 9, 20);
        JLabel lbl = new JLabel(labelText + ":");
        lbl.setFont(AppTheme.FONT_BODY);
        lbl.setForeground(AppTheme.TEXT_MUTED);
        form.add(lbl, lc);

        GridBagConstraints fc = new GridBagConstraints();
        fc.gridx = 1; fc.gridy = row;
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.insets = new Insets(6, 0, 6, 0);
        form.add(field, fc);
    }

    // ── View / Edit mode ──────────────────────────────────────────────────────

    private void applyViewMode() {
        // Text fields read-only
        fldFullName.setEditable(false);
        fldEmail.setEditable(false);
        fldPhone.setEditable(false);
        fldAddress.setEditable(false);

        // Muted background signals read-only
        Color ro = AppTheme.BG_MAIN;
        fldFullName.setBackground(ro);
        fldEmail.setBackground(ro);
        fldPhone.setBackground(ro);
        fldAddress.setBackground(ro);

        // Date picker: disabled — shows value but calendar popup cannot be opened
        datePickerDob.setEnabled(false);

        // Photo buttons disabled
        btnChangePhoto.setEnabled(false);
        btnRemovePhoto.setEnabled(false);

        // Action buttons
        btnEdit.setVisible(true);
        btnSave.setVisible(false);
        btnCancel.setVisible(false);
    }

    private void applyEditMode() {
        // Text fields editable
        fldFullName.setEditable(true);
        fldEmail.setEditable(true);
        fldPhone.setEditable(true);
        fldAddress.setEditable(true);

        Color rw = AppTheme.BG_INPUT;
        fldFullName.setBackground(rw);
        fldEmail.setBackground(rw);
        fldPhone.setBackground(rw);
        fldAddress.setBackground(rw);

        // Date picker: enabled — calendar popup available, date is clearable
        datePickerDob.setEnabled(true);

        btnChangePhoto.setEnabled(true);
        btnRemovePhoto.setEnabled(true);

        btnEdit.setVisible(false);
        btnSave.setVisible(true);
        btnCancel.setVisible(true);

        fldFullName.requestFocusInWindow();
    }

    private void cancelEdit() {
        // Discard any pending image selection — nothing was imported yet, nothing to delete
        pendingSourceFile  = null;
        pendingRemoveImage = false;

        // Reload persisted values into all form fields
        reloadFieldsFromProfile();

        // Restore persisted photo preview
        refreshPersistedImagePreview();

        applyViewMode();
    }

    // ── Reload fields ─────────────────────────────────────────────────────────

    private void reloadFieldsFromProfile() {
        fldFullName.setText(profile != null ? profile.getFullName() : "");
        lblUsername.setText(profile != null ? profile.getUsername() : "");
        fldEmail.setText(profile != null && profile.getEmail() != null ? profile.getEmail() : "");
        fldPhone.setText(profile != null && profile.getPhoneNumber() != null
                         ? profile.getPhoneNumber() : "");
        datePickerDob.setDate(profile != null ? profile.getDateOfBirth() : null);
        fldAddress.setText(profile != null && profile.getDeliveryAddress() != null
                           ? profile.getDeliveryAddress() : "");
        refreshAge();
    }

    // ── Image helpers ─────────────────────────────────────────────────────────

    /** Shows the persisted profile image (from DB path) or the placeholder. */
    private void refreshPersistedImagePreview() {
        String path = (profile != null) ? profile.getProfileImagePath() : null;
        if (path != null && !path.isBlank()) {
            ImageIcon icon = ProfileImageManager.loadScaled(path, 120, 120);
            if (icon != null) {
                imgPreview.setIcon(icon);
                imgPreview.setText(null);
                return;
            }
        }
        imgPreview.setIcon(null);
        imgPreview.setText("No Photo");
    }

    private void refreshAge() {
        if (lblAge == null || datePickerDob == null) return;
        LocalDate dob = datePickerDob.getDate();
        if (dob == null) { lblAge.setText("\u2014"); return; }
        int age = CustomerProfileService.calculateAge(dob);
        lblAge.setText(age >= 0 ? age + " years" : "\u2014");
    }

    private void chooseProfilePhoto() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose Profile Photo");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Image files (JPG, PNG)", "jpg", "jpeg", "png"));
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File chosen = chooser.getSelectedFile();

        // Preview source directly — do NOT import until Save
        ImageIcon preview = ImageManager.loadScaledPreview(chosen, 120, 120);
        imgPreview.setIcon(preview);
        imgPreview.setText(null);

        pendingSourceFile  = chosen;
        pendingRemoveImage = false;
    }

    // ── Save ───────────────────────────────────────────────────────────────────

    private void saveProfile() {
        String fullName = fldFullName.getText().trim();
        String email    = fldEmail.getText().trim();
        String phone    = fldPhone.getText().trim();
        String address  = fldAddress.getText().trim();

        // Date comes directly from the calendar picker — no parsing needed
        LocalDate dob = datePickerDob.getDate();

        // ── Determine final image path and import if needed ────────────────
        String oldImgPath   = (profile != null) ? profile.getProfileImagePath() : null;
        String newImgPath   = null;     // the path we will write to DB
        String importedFile = null;     // tracks a newly imported file for rollback

        if (pendingRemoveImage) {
            newImgPath = null;          // remove: set DB to NULL
        } else if (pendingSourceFile != null) {
            // Import source file now (only on Save, never on file selection)
            try {
                importedFile = ProfileImageManager.importImage(pendingSourceFile.toPath());
                newImgPath = importedFile;
            } catch (Exception ex) {
                AppTheme.showError(this, "Photo Import Failed",
                        "Could not import photo: " + ex.getMessage());
                return;
            }
        } else {
            newImgPath = oldImgPath;    // unchanged
        }

        // ── Persist via session-aware service ──────────────────────────────
        String error = CustomerProfileService.updateCurrentCustomerProfile(
                fullName,
                email.isEmpty()   ? null : email,
                phone.isEmpty()   ? null : phone,
                dob, newImgPath,
                address.isEmpty() ? null : address);

        if (error != null) {
            // Rollback: if we imported a new file, delete it
            if (importedFile != null) {
                ProfileImageManager.deleteProfileImage(importedFile);
            }
            AppTheme.showError(this, "Save Failed", error);
            return;
        }

        // ── DB succeeded: clean up old managed profile image ───────────────
        boolean imageChanged = pendingRemoveImage || (pendingSourceFile != null);
        if (imageChanged && oldImgPath != null && !oldImgPath.equals(newImgPath)) {
            ProfileImageManager.deleteProfileImage(oldImgPath);
        }

        // ── Refresh session and reload profile ─────────────────────────────
        Customer updated = CustomerProfileService.getCurrentCustomerProfile();
        if (updated != null) {
            SessionManager.setLoggedInCustomer(updated);
            profile = updated;
        }

        // Reset pending image state
        pendingSourceFile  = null;
        pendingRemoveImage = false;

        // Reload form fields from saved data and enter view mode
        reloadFieldsFromProfile();
        refreshPersistedImagePreview();
        applyViewMode();

        AppTheme.showToast(this, "Profile saved successfully.");
    }
}
