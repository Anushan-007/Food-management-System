package citybites.ui;

import citybites.model.Customer;
import citybites.service.CustomerManagementService;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

public class CustomerManagementFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(CustomerManagementFrame.class.getName());

    // ── Table model: 6 cols — ID (hidden), Full Name, Username, Email, Phone, Registered
    private DefaultTableModel tableModel;

    /**
     * ID of the customer currently loaded into the form for editing.
     * {@code -1} means the form is in Add mode (no customer selected).
     */
    private int selectedCustomerId = -1;

    public CustomerManagementFrame() {
        initComponents();
        setTitle("City Bites - Customer Management");
        setMinimumSize(new Dimension(900, 560));
        setSize(1100, 660);
        setLocationRelativeTo(null);
        setResizable(true);
        setAddMode();
        loadCustomerTable();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        txtFullName        = new javax.swing.JTextField();
        txtUsername        = new javax.swing.JTextField();
        txtPassword        = new javax.swing.JPasswordField();
        txtConfirmPassword = new javax.swing.JPasswordField();
        txtSearch          = new javax.swing.JTextField();
        btnAdd             = new javax.swing.JButton();
        btnUpdate          = new javax.swing.JButton();
        btnDelete          = new javax.swing.JButton();
        btnClear           = new javax.swing.JButton();
        tblCustomer        = new javax.swing.JTable();
        lblFullName        = new javax.swing.JLabel();
        lblUsername        = new javax.swing.JLabel();
        lblPassword        = new javax.swing.JLabel();
        lblConfirmPassword = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        // ── Navigation header ────────────────────────────────────────
        JButton refreshBtn = AppTheme.secondaryBtn("Refresh");
        refreshBtn.addActionListener(e -> { loadCustomerTable(); clearForm(); });

        JButton backBtn = AppTheme.ghostBtn("← Back");
        backBtn.setForeground(new Color(150, 170, 190));
        backBtn.addActionListener(e -> btnBackActionPerformed(null));

        JPanel header = AppTheme.navHeader("Customer Management", null, refreshBtn, backBtn);

        // ── Form labels ──────────────────────────────────────────────
        lblFullName.setText("Full Name");
        lblFullName.setFont(AppTheme.FONT_LABEL);
        lblFullName.setForeground(AppTheme.TEXT_PRIMARY);

        lblUsername.setText("Username");
        lblUsername.setFont(AppTheme.FONT_LABEL);
        lblUsername.setForeground(AppTheme.TEXT_PRIMARY);

        lblPassword.setText("Password");
        lblPassword.setFont(AppTheme.FONT_LABEL);
        lblPassword.setForeground(AppTheme.TEXT_PRIMARY);

        lblPasswordHint = new javax.swing.JLabel("Leave blank to keep current password");
        lblPasswordHint.setFont(AppTheme.FONT_BODY.deriveFont(10f));
        lblPasswordHint.setForeground(AppTheme.TEXT_MUTED);

        lblConfirmPassword.setText("Confirm Password");
        lblConfirmPassword.setFont(AppTheme.FONT_LABEL);
        lblConfirmPassword.setForeground(AppTheme.TEXT_PRIMARY);

        // ── Form fields ──────────────────────────────────────────────
        AppTheme.styleField(txtFullName);
        txtFullName.putClientProperty("JTextField.placeholderText", "Enter full name...");

        AppTheme.styleField(txtUsername);
        txtUsername.putClientProperty("JTextField.placeholderText", "Enter username...");

        // Password field + visibility toggle
        txtPassword.setFont(AppTheme.FONT_BODY);
        txtPassword.setBackground(AppTheme.BG_INPUT);
        txtPassword.setForeground(AppTheme.TEXT_PRIMARY);
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        txtConfirmPassword.setFont(AppTheme.FONT_BODY);
        txtConfirmPassword.setBackground(AppTheme.BG_INPUT);
        txtConfirmPassword.setForeground(AppTheme.TEXT_PRIMARY);
        txtConfirmPassword.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        chkShowPassword = new JCheckBox("Show");
        chkShowPassword.setBackground(AppTheme.BG_CARD);
        chkShowPassword.setForeground(AppTheme.TEXT_MUTED);
        chkShowPassword.setFont(AppTheme.FONT_BODY);
        chkShowPassword.addActionListener(e -> togglePasswordVisibility());

        JPanel pwRow = new JPanel(new BorderLayout(6, 0));
        pwRow.setBackground(AppTheme.BG_CARD);
        pwRow.add(txtPassword, BorderLayout.CENTER);
        pwRow.add(chkShowPassword, BorderLayout.EAST);

        JPanel confirmRow = new JPanel(new BorderLayout());
        confirmRow.setBackground(AppTheme.BG_CARD);
        confirmRow.add(txtConfirmPassword, BorderLayout.CENTER);

        // Helper shown in Edit mode to explain why Full Name is read-only
        lblFullNameHint = new javax.swing.JLabel(
                "Customers manage their full name from My Profile.");
        lblFullNameHint.setFont(AppTheme.FONT_BODY.deriveFont(10f));
        lblFullNameHint.setForeground(AppTheme.TEXT_MUTED);
        lblFullNameHint.setVisible(false); // hidden in Add mode

        // ── Field grid ───────────────────────────────────────────────
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setBackground(AppTheme.BG_CARD);
        GridBagConstraints c = new GridBagConstraints();
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.anchor  = GridBagConstraints.WEST;
        c.weightx = 1;
        c.insets  = new Insets(4, 0, 4, 0);

        c.gridx = 0; c.gridy = 0; fields.add(lblFullName,        c);
        c.gridy = 1;               fields.add(txtFullName,        c);
        c.gridy = 2;               fields.add(lblFullNameHint,    c);
        c.gridy = 3;               fields.add(lblUsername,        c);
        c.gridy = 4;               fields.add(txtUsername,        c);
        c.gridy = 5;               fields.add(lblPassword,        c);
        c.gridy = 6;               fields.add(pwRow,              c);
        c.gridy = 7;               fields.add(lblPasswordHint,    c);
        c.gridy = 8;               fields.add(lblConfirmPassword, c);
        c.gridy = 9;               fields.add(confirmRow,         c);

        // ── Buttons ──────────────────────────────────────────────────
        btnAdd        = AppTheme.primaryBtn("Add Customer");
        btnUpdate     = AppTheme.secondaryBtn("Update Customer");
        btnDelete     = AppTheme.dangerBtn("Delete Customer");
        btnClear      = AppTheme.ghostBtn("Clear Form");
        btnViewDetails = AppTheme.secondaryBtn("View Details");

        btnAdd.setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.BTN_H));
        btnUpdate.setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.BTN_H));
        btnDelete.setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.BTN_H));
        btnClear.setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.BTN_H));
        btnViewDetails.setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.BTN_H));

        btnAdd.addActionListener(e         -> btnAddActionPerformed(null));
        btnUpdate.addActionListener(e      -> btnUpdateActionPerformed(null));
        btnDelete.addActionListener(e      -> btnDeleteActionPerformed(null));
        btnClear.addActionListener(e       -> btnClearActionPerformed(null));
        btnViewDetails.addActionListener(e -> openCustomerDetails());

        // ── Form card ────────────────────────────────────────────────
        JPanel formCard = new JPanel();
        formCard.setBackground(AppTheme.BG_CARD);
        formCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));

        JLabel formTitle = new JLabel("Customer Details");
        formTitle.setFont(AppTheme.FONT_SUBHEAD);
        formTitle.setForeground(AppTheme.TEXT_PRIMARY);
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        fields.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnAdd.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnUpdate.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnDelete.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnClear.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnViewDetails.setAlignmentX(Component.LEFT_ALIGNMENT);

        formCard.add(formTitle);
        formCard.add(Box.createVerticalStrut(14));
        formCard.add(fields);
        formCard.add(Box.createVerticalStrut(18));
        formCard.add(btnAdd);
        formCard.add(Box.createVerticalStrut(6));
        formCard.add(btnUpdate);
        formCard.add(Box.createVerticalStrut(6));
        formCard.add(btnDelete);
        formCard.add(Box.createVerticalStrut(6));
        formCard.add(btnClear);
        formCard.add(Box.createVerticalStrut(14));
        // Thin separator before View Details
        JSeparator viewSep = new JSeparator();
        viewSep.setForeground(AppTheme.BORDER);
        viewSep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        viewSep.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(viewSep);
        formCard.add(Box.createVerticalStrut(10));
        formCard.add(btnViewDetails);
        formCard.add(Box.createVerticalGlue());

        JScrollPane formScroll = new JScrollPane(formCard);
        formScroll.setBorder(null);
        formScroll.setPreferredSize(new Dimension(300, 0));

        // ── Table panel ──────────────────────────────────────────────
        // Model: 6 columns — ID (hidden), Full Name, Username, Email, Phone, Registered
        String[] columns = {"ID", "Full Name", "Username", "Email", "Phone", "Registered"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c2) { return false; }
        };
        tblCustomer = new javax.swing.JTable(tableModel);
        AppTheme.styleTable(tblCustomer);
        tblCustomer.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Remove the ID column from the view; model column 0 stays intact for CRUD
        tblCustomer.getColumnModel().removeColumn(tblCustomer.getColumnModel().getColumn(0));

        // Proportional widths for the 5 visible columns (view col 0–4 = model col 1–5)
        tblCustomer.getColumnModel().getColumn(0).setPreferredWidth(180); // Full Name
        tblCustomer.getColumnModel().getColumn(1).setPreferredWidth(120); // Username
        tblCustomer.getColumnModel().getColumn(2).setPreferredWidth(210); // Email
        tblCustomer.getColumnModel().getColumn(3).setPreferredWidth(130); // Phone
        tblCustomer.getColumnModel().getColumn(4).setPreferredWidth(100); // Registered

        tblCustomer.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) tableRowSelected();
        });

        // Double-click opens View Details
        tblCustomer.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && selectedCustomerId > 0) {
                    openCustomerDetails();
                }
            }
        });

        // Search bar
        AppTheme.styleField(txtSearch);
        txtSearch.putClientProperty("JTextField.placeholderText",
                "Search by name, username, email or phone...");
        txtSearch.setPreferredSize(new Dimension(300, AppTheme.BTN_H));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { filterTable(txtSearch.getText()); }
            @Override public void removeUpdate(DocumentEvent e)  { filterTable(txtSearch.getText()); }
            @Override public void changedUpdate(DocumentEvent e) { filterTable(txtSearch.getText()); }
        });

        JButton clearSearchBtn = AppTheme.ghostBtn("Clear");
        clearSearchBtn.addActionListener(e -> {
            txtSearch.setText("");
            loadCustomerTable();
        });

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBar.setBackground(AppTheme.BG_MAIN);
        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(AppTheme.FONT_BODY);
        searchLbl.setForeground(AppTheme.TEXT_PRIMARY);
        searchBar.add(searchLbl);
        searchBar.add(txtSearch);
        searchBar.add(clearSearchBtn);

        JScrollPane tableScroll = new JScrollPane(tblCustomer);
        tableScroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        tableScroll.getViewport().setBackground(AppTheme.BG_CARD);

        JPanel tablePanel = new JPanel(new BorderLayout(0, 8));
        tablePanel.setBackground(AppTheme.BG_MAIN);
        tablePanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        tablePanel.add(searchBar,   BorderLayout.NORTH);
        tablePanel.add(tableScroll, BorderLayout.CENTER);

        // ── Body ─────────────────────────────────────────────────────
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setBackground(AppTheme.BG_MAIN);
        body.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        body.add(formScroll, BorderLayout.WEST);
        body.add(tablePanel, BorderLayout.CENTER);

        // ── Root layout ──────────────────────────────────────────────
        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header, BorderLayout.NORTH);
        getContentPane().add(body,   BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // ── Form modes ────────────────────────────────────────────────────────

    /** Switches to Add mode: Add enabled, Update/Delete/ViewDetails disabled. Full Name editable. */
    private void setAddMode() {
        selectedCustomerId = -1;
        btnAdd.setEnabled(true);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);
        btnViewDetails.setEnabled(false);
        lblPasswordHint.setVisible(false);
        // Full Name is editable in Add mode (admin enters the new customer's name)
        txtFullName.setEditable(true);
        txtFullName.setBackground(AppTheme.BG_INPUT);
        lblFullNameHint.setVisible(false);
    }

    /** Switches to Edit mode: Add disabled, Update/Delete/ViewDetails enabled. Full Name read-only. */
    private void setEditMode() {
        btnAdd.setEnabled(false);
        btnUpdate.setEnabled(true);
        btnDelete.setEnabled(true);
        btnViewDetails.setEnabled(true);
        lblPasswordHint.setVisible(true);
        // Full Name is read-only in Edit mode — customers manage it themselves
        txtFullName.setEditable(false);
        txtFullName.setBackground(AppTheme.BG_FOOTER);
        lblFullNameHint.setVisible(true);
    }

    private void togglePasswordVisibility() {
        char echo = chkShowPassword.isSelected() ? (char) 0 : '\u2022';
        txtPassword.setEchoChar(chkShowPassword.isSelected() ? (char) 0 : '\u2022');
        txtConfirmPassword.setEchoChar(echo);
    }

    // ── Data loading ──────────────────────────────────────────────────────

    private void loadCustomerTable() {
        try {
            tableModel.setRowCount(0);
            for (Customer c : CustomerManagementService.getAllCustomers()) {
                tableModel.addRow(new Object[]{
                    c.getCustomerId(),
                    c.getFullName(),
                    c.getUsername(),
                    c.getEmail()       != null ? c.getEmail()       : "\u2014",
                    c.getPhoneNumber() != null ? c.getPhoneNumber() : "\u2014",
                    formatDate(c.getCreatedAt())
                });
            }
        } catch (Exception e) {
            AppTheme.showError(this, "Database Error",
                    "Could not load customers: " + e.getMessage());
        }
    }

    private void filterTable(String query) {
        try {
            tableModel.setRowCount(0);
            String q = (query == null) ? "" : query.toLowerCase().trim();
            for (Customer c : CustomerManagementService.getAllCustomers()) {
                String email = c.getEmail()       != null ? c.getEmail().toLowerCase()       : "";
                String phone = c.getPhoneNumber() != null ? c.getPhoneNumber().toLowerCase() : "";
                boolean nameMatch  = q.isEmpty() || c.getFullName().toLowerCase().contains(q);
                boolean userMatch  = !q.isEmpty() && c.getUsername().toLowerCase().contains(q);
                boolean emailMatch = !q.isEmpty() && email.contains(q);
                boolean phoneMatch = !q.isEmpty() && phone.contains(q);
                if (nameMatch || userMatch || emailMatch || phoneMatch) {
                    tableModel.addRow(new Object[]{
                        c.getCustomerId(),
                        c.getFullName(),
                        c.getUsername(),
                        c.getEmail()       != null ? c.getEmail()       : "\u2014",
                        c.getPhoneNumber() != null ? c.getPhoneNumber() : "\u2014",
                        formatDate(c.getCreatedAt())
                    });
                }
            }
        } catch (Exception e) {
            logger.warning("Filter failed: " + e.getMessage());
        }
    }

    private void tableRowSelected() {
        int viewRow = tblCustomer.getSelectedRow();
        if (viewRow < 0) {
            setAddMode();
            return;
        }
        int modelRow = tblCustomer.convertRowIndexToModel(viewRow);
        selectedCustomerId = (int) tableModel.getValueAt(modelRow, 0); // ID from hidden col 0
        txtFullName.setText(tableModel.getValueAt(modelRow, 1).toString()); // Full Name = model col 1
        txtUsername.setText(tableModel.getValueAt(modelRow, 2).toString()); // Username  = model col 2
        txtPassword.setText("");
        txtConfirmPassword.setText("");
        setEditMode();
    }

    // ── View Details ─────────────────────────────────────────────────────

    /** Opens the CustomerDetailsDialog for the currently selected customer. */
    private void openCustomerDetails() {
        if (selectedCustomerId < 1) return;
        new CustomerDetailsDialog(this, selectedCustomerId).setVisible(true);
    }

    // ── CRUD actions ──────────────────────────────────────────────────────

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        if (selectedCustomerId != -1) return;   // defensive guard

        String fullName = txtFullName.getText().trim();
        String username = txtUsername.getText().trim();
        char[] pwChars  = txtPassword.getPassword();
        char[] cfChars  = txtConfirmPassword.getPassword();
        String password = new String(pwChars);
        String confirm  = new String(cfChars);
        Arrays.fill(pwChars, '\0');
        Arrays.fill(cfChars, '\0');

        try {
            int newId = CustomerManagementService.addCustomer(fullName, username, password, confirm);
            AppTheme.showInfo(this, "Success",
                "Customer \"" + username + "\" added (ID: " + newId + ").");
            loadCustomerTable();
            clearForm();
        } catch (IllegalArgumentException ex) {
            AppTheme.showWarning(this, "Validation Error", ex.getMessage());
        } catch (Exception ex) {
            AppTheme.showError(this, "Database Error",
                "Could not add customer: " + ex.getMessage());
        }
    }//GEN-LAST:event_btnAddActionPerformed

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
        if (selectedCustomerId < 1) {
            AppTheme.showWarning(this, "No Selection",
                "Please select a customer to update.");
            return;
        }
        String fullName = txtFullName.getText().trim();
        String username = txtUsername.getText().trim();
        char[] pwChars  = txtPassword.getPassword();
        char[] cfChars  = txtConfirmPassword.getPassword();
        String newPwd   = new String(pwChars);
        String confirm  = new String(cfChars);
        Arrays.fill(pwChars, '\0');
        Arrays.fill(cfChars, '\0');

        // Pass null for password when field is empty to trigger profile-only update
        String pwArg = newPwd.isEmpty() ? null : newPwd;
        String cfArg = confirm.isEmpty() ? null : confirm;

        try {
            CustomerManagementService.updateCustomer(
                selectedCustomerId, fullName, username, pwArg, cfArg);
            AppTheme.showInfo(this, "Updated", "Customer \"" + username + "\" updated.");
            loadCustomerTable();
            clearForm();
        } catch (IllegalArgumentException ex) {
            AppTheme.showWarning(this, "Validation Error", ex.getMessage());
        } catch (Exception ex) {
            AppTheme.showError(this, "Database Error",
                "Could not update customer: " + ex.getMessage());
        }
    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        if (selectedCustomerId < 1) {
            AppTheme.showWarning(this, "No Selection",
                "Please select a customer to delete.");
            return;
        }
        String username = txtUsername.getText().trim();

        // Warn about orders upfront
        boolean hasOrders = CustomerManagementService.customerHasOrders(selectedCustomerId);
        if (hasOrders) {
            AppTheme.showWarning(this, "Cannot Delete",
                "Customer \"" + username + "\" has existing orders and cannot be deleted.");
            return;
        }

        if (!AppTheme.showConfirm(this, "Confirm Delete",
                "Delete customer \"" + username + "\"? This action cannot be undone.")) {
            return;
        }
        try {
            CustomerManagementService.deleteCustomer(selectedCustomerId);
            AppTheme.showInfo(this, "Deleted", "Customer \"" + username + "\" deleted.");
            loadCustomerTable();
            clearForm();
        } catch (IllegalStateException ex) {
            AppTheme.showWarning(this, "Cannot Delete", ex.getMessage());
        } catch (Exception ex) {
            AppTheme.showError(this, "Database Error",
                "Could not delete customer: " + ex.getMessage());
        }
    }//GEN-LAST:event_btnDeleteActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        clearForm();
    }//GEN-LAST:event_btnClearActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new AdminDashboardFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    private void clearForm() {
        tblCustomer.clearSelection();
        txtFullName.setText("");
        txtUsername.setText("");
        txtPassword.setText("");
        txtConfirmPassword.setText("");
        chkShowPassword.setSelected(false);
        txtPassword.setEchoChar('\u2022');
        txtConfirmPassword.setEchoChar('\u2022');
        setAddMode();
        txtFullName.requestFocusInWindow();
    }

    /** Trims timestamp to date-only (first 10 chars: yyyy-MM-dd) for display. */
    private static String formatDate(String timestamp) {
        if (timestamp == null || timestamp.length() < 10) return timestamp;
        return timestamp.substring(0, 10);
    }

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new CustomerManagementFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton        btnAdd;
    private javax.swing.JButton        btnUpdate;
    private javax.swing.JButton        btnDelete;
    private javax.swing.JButton        btnClear;
    private javax.swing.JButton        btnViewDetails;
    private javax.swing.JTextField     txtFullName;
    private javax.swing.JTextField     txtUsername;
    private javax.swing.JPasswordField txtPassword;
    private javax.swing.JPasswordField txtConfirmPassword;
    private javax.swing.JTextField     txtSearch;
    private javax.swing.JTable         tblCustomer;
    private javax.swing.JLabel         lblFullName;
    private javax.swing.JLabel         lblUsername;
    private javax.swing.JLabel         lblPassword;
    private javax.swing.JLabel         lblConfirmPassword;
    private javax.swing.JLabel         lblPasswordHint;
    private javax.swing.JLabel         lblFullNameHint;
    private javax.swing.JCheckBox      chkShowPassword;
    // End of variables declaration//GEN-END:variables
}
