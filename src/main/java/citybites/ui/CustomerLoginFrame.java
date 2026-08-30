package citybites.ui;

import citybites.model.Customer;
import citybites.service.AuthService;
import citybites.util.SessionManager;
import java.awt.*;
import java.util.Optional;
import javax.swing.*;

public class CustomerLoginFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(CustomerLoginFrame.class.getName());

    public CustomerLoginFrame() {
        initComponents();
        setTitle("City Bites - Customer Login");
        setMinimumSize(new Dimension(520, 540));
        setSize(600, 600);
        setLocationRelativeTo(null);
        setResizable(true);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        txtUsername = new javax.swing.JTextField();
        txtPassword = new javax.swing.JPasswordField();
        btnLogin    = new javax.swing.JButton();
        btnRegister = new javax.swing.JButton();
        btnClear    = new javax.swing.JButton();
        btnBack     = new javax.swing.JButton();
        lblUsername = new javax.swing.JLabel();
        lblPassword = new javax.swing.JLabel();
        lblTitle    = new javax.swing.JLabel();
        lblSubtitle = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        // ── Navigation header ────────────────────────────────────────
        JButton backBtn = AppTheme.ghostBtn("← Back");
        backBtn.setForeground(new Color(150, 170, 190));
        backBtn.addActionListener(this::btnBackActionPerformed);
        JPanel header = AppTheme.navHeader("Customer Portal", null, backBtn);

        // ── Card: title & subtitle ───────────────────────────────────
        lblTitle.setText("Customer Login");
        lblTitle.setFont(AppTheme.FONT_TITLE);
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblSubtitle.setText("Sign in to browse and order food");
        lblSubtitle.setFont(AppTheme.FONT_BODY);
        lblSubtitle.setForeground(AppTheme.TEXT_MUTED);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Field labels ─────────────────────────────────────────────
        lblUsername.setText("Username");
        lblUsername.setFont(AppTheme.FONT_LABEL);
        lblUsername.setForeground(AppTheme.TEXT_PRIMARY);

        lblPassword.setText("Password");
        lblPassword.setFont(AppTheme.FONT_LABEL);
        lblPassword.setForeground(AppTheme.TEXT_PRIMARY);

        // ── Fields ───────────────────────────────────────────────────
        AppTheme.styleField(txtUsername);
        AppTheme.styleField(txtPassword);
        txtPassword.addActionListener(this::btnLoginActionPerformed);

        // Password visibility toggle
        JToggleButton togglePwd = new JToggleButton("Show");
        togglePwd.setFont(AppTheme.FONT_SMALL);
        togglePwd.setPreferredSize(new Dimension(58, 30));
        togglePwd.setFocusPainted(false);
        togglePwd.addActionListener(e -> {
            if (togglePwd.isSelected()) {
                txtPassword.setEchoChar((char) 0);
                togglePwd.setText("Hide");
            } else {
                txtPassword.setEchoChar('\u2022');
                togglePwd.setText("Show");
            }
        });

        JPanel pwdRow = new JPanel(new BorderLayout(6, 0));
        pwdRow.setBackground(AppTheme.BG_CARD);
        pwdRow.add(txtPassword, BorderLayout.CENTER);
        pwdRow.add(togglePwd,  BorderLayout.EAST);

        // ── Buttons ──────────────────────────────────────────────────
        btnLogin    = AppTheme.primaryBtn("Login");
        btnRegister = AppTheme.successBtn("Create Account");
        btnClear    = AppTheme.secondaryBtn("Clear");
        btnBack     = AppTheme.secondaryBtn("Back");

        btnLogin.setPreferredSize(new Dimension(220, AppTheme.BTN_H));
        btnLogin.setMaximumSize(new Dimension(220, AppTheme.BTN_H));
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRegister.setPreferredSize(new Dimension(220, AppTheme.BTN_H));
        btnRegister.setMaximumSize(new Dimension(220, AppTheme.BTN_H));
        btnRegister.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnLogin.addActionListener(this::btnLoginActionPerformed);
        btnRegister.addActionListener(this::btnRegisterActionPerformed);
        btnClear.addActionListener(this::btnClearActionPerformed);
        btnBack.addActionListener(this::btnBackActionPerformed);

        // ── Form grid ────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(AppTheme.BG_CARD);
        GridBagConstraints c = new GridBagConstraints();
        c.insets  = new Insets(6, 0, 6, 0);
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.anchor  = GridBagConstraints.WEST;
        c.weightx = 1;

        c.gridx = 0; c.gridy = 0; form.add(lblUsername, c);
        c.gridy = 1;              form.add(txtUsername,  c);
        c.gridy = 2;              form.add(lblPassword,  c);
        c.gridy = 3;              form.add(pwdRow,       c);

        // Utility row (Clear + Back)
        JPanel utilRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        utilRow.setBackground(AppTheme.BG_CARD);
        utilRow.add(btnClear);
        utilRow.add(btnBack);

        // "New here?" divider
        JLabel dividerLabel = new JLabel("New to City Bites?");
        dividerLabel.setFont(AppTheme.FONT_SMALL);
        dividerLabel.setForeground(AppTheme.TEXT_MUTED);
        dividerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Card panel ───────────────────────────────────────────────
        JPanel card = new JPanel();
        card.setBackground(AppTheme.BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(32, 60, 32, 60));
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(6));
        card.add(lblSubtitle);
        card.add(Box.createVerticalStrut(26));
        card.add(form);
        card.add(Box.createVerticalStrut(6));
        card.add(utilRow);
        card.add(Box.createVerticalStrut(16));
        card.add(btnLogin);
        card.add(Box.createVerticalStrut(20));
        card.add(dividerLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(btnRegister);
        card.add(Box.createVerticalGlue());

        // ── Root layout ──────────────────────────────────────────────
        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(AppTheme.BG_MAIN);
        center.setBorder(BorderFactory.createEmptyBorder(20, 60, 20, 60));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        center.add(card, gbc);
        getContentPane().add(center, BorderLayout.CENTER);

        getRootPane().setDefaultButton(btnLogin);
    }// </editor-fold>//GEN-END:initComponents

    private void btnLoginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoginActionPerformed
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            AppTheme.showWarning(this, "Validation Error",
                    "Please enter username and password.");
            return;
        }
        try {
            Optional<Customer> result = AuthService.customerLogin(username, password);
            if (result.isPresent()) {
                SessionManager.setLoggedInCustomer(result.get());
                logger.info("Customer login: " + username);
                new CustomerDashboardFrame().setVisible(true);
                dispose();
            } else {
                AppTheme.showError(this, "Login Failed",
                        "Invalid username or password.");
                txtPassword.setText("");
                txtPassword.requestFocus();
            }
        } catch (Exception e) {
            AppTheme.showError(this, "Error", "Database error: " + e.getMessage());
        }
    }//GEN-LAST:event_btnLoginActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        txtUsername.setText("");
        txtPassword.setText("");
        txtUsername.requestFocus();
    }//GEN-LAST:event_btnClearActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new WelcomeFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    private void btnRegisterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegisterActionPerformed
        new CustomerRegisterFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnRegisterActionPerformed

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new CustomerLoginFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton         btnLogin;
    private javax.swing.JButton         btnRegister;
    private javax.swing.JButton         btnClear;
    private javax.swing.JButton         btnBack;
    private javax.swing.JTextField      txtUsername;
    private javax.swing.JPasswordField  txtPassword;
    private javax.swing.JLabel          lblUsername;
    private javax.swing.JLabel          lblPassword;
    private javax.swing.JLabel          lblTitle;
    private javax.swing.JLabel          lblSubtitle;
    // End of variables declaration//GEN-END:variables
}
