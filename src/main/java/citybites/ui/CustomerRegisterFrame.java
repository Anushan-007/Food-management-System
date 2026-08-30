package citybites.ui;

import citybites.service.AuthService;
import citybites.util.PasswordValidator;
import java.awt.*;
import javax.swing.*;

public class CustomerRegisterFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(CustomerRegisterFrame.class.getName());

    public CustomerRegisterFrame() {
        initComponents();
        setTitle("City Bites - Customer Registration");
        setMinimumSize(new Dimension(560, 600));
        setSize(640, 660);
        setLocationRelativeTo(null);
        setResizable(true);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        lblTitle           = new javax.swing.JLabel();
        lblFullName        = new javax.swing.JLabel();
        txtFullName        = new javax.swing.JTextField();
        lblUsername        = new javax.swing.JLabel();
        txtUsername        = new javax.swing.JTextField();
        lblPassword        = new javax.swing.JLabel();
        txtPassword        = new javax.swing.JPasswordField();
        jLabel1            = new javax.swing.JLabel();
        txtConfirmPassword = new javax.swing.JPasswordField();
        btnRegister        = new javax.swing.JButton();
        btnClear           = new javax.swing.JButton();
        btnBack            = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        // ── Navigation header ────────────────────────────────────────
        JButton backBtn = AppTheme.ghostBtn("← Back");
        backBtn.setForeground(new Color(150, 170, 190));
        backBtn.addActionListener(this::btnBackActionPerformed);
        JPanel header = AppTheme.navHeader("Customer Portal", null, backBtn);

        // ── Title & subtitle ─────────────────────────────────────────
        lblTitle.setText("Create Account");
        lblTitle.setFont(AppTheme.FONT_TITLE);
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitle = new JLabel("Fill in your details to get started");
        lblSubtitle.setFont(AppTheme.FONT_BODY);
        lblSubtitle.setForeground(AppTheme.TEXT_MUTED);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Field labels ─────────────────────────────────────────────
        lblFullName.setText("Full Name");
        lblFullName.setFont(AppTheme.FONT_LABEL);
        lblFullName.setForeground(AppTheme.TEXT_PRIMARY);

        lblUsername.setText("Username");
        lblUsername.setFont(AppTheme.FONT_LABEL);
        lblUsername.setForeground(AppTheme.TEXT_PRIMARY);

        lblPassword.setText("Password");
        lblPassword.setFont(AppTheme.FONT_LABEL);
        lblPassword.setForeground(AppTheme.TEXT_PRIMARY);

        jLabel1.setText("Confirm Password");
        jLabel1.setFont(AppTheme.FONT_LABEL);
        jLabel1.setForeground(AppTheme.TEXT_PRIMARY);

        // ── Fields ───────────────────────────────────────────────────
        AppTheme.styleField(txtFullName);
        AppTheme.styleField(txtUsername);
        AppTheme.styleField(txtPassword);
        AppTheme.styleField(txtConfirmPassword);

        // Password visibility toggle (shared for both password fields)
        JToggleButton togglePwd1 = new JToggleButton("Show");
        togglePwd1.setFont(AppTheme.FONT_SMALL);
        togglePwd1.setPreferredSize(new Dimension(58, 30));
        togglePwd1.setFocusPainted(false);
        togglePwd1.addActionListener(e -> {
            if (togglePwd1.isSelected()) {
                txtPassword.setEchoChar((char) 0);
                togglePwd1.setText("Hide");
            } else {
                txtPassword.setEchoChar('\u2022');
                togglePwd1.setText("Show");
            }
        });

        JToggleButton togglePwd2 = new JToggleButton("Show");
        togglePwd2.setFont(AppTheme.FONT_SMALL);
        togglePwd2.setPreferredSize(new Dimension(58, 30));
        togglePwd2.setFocusPainted(false);
        togglePwd2.addActionListener(e -> {
            if (togglePwd2.isSelected()) {
                txtConfirmPassword.setEchoChar((char) 0);
                togglePwd2.setText("Hide");
            } else {
                txtConfirmPassword.setEchoChar('\u2022');
                togglePwd2.setText("Show");
            }
        });

        JPanel pwdRow1 = new JPanel(new BorderLayout(6, 0));
        pwdRow1.setBackground(AppTheme.BG_CARD);
        pwdRow1.add(txtPassword, BorderLayout.CENTER);
        pwdRow1.add(togglePwd1, BorderLayout.EAST);

        JPanel pwdRow2 = new JPanel(new BorderLayout(6, 0));
        pwdRow2.setBackground(AppTheme.BG_CARD);
        pwdRow2.add(txtConfirmPassword, BorderLayout.CENTER);
        pwdRow2.add(togglePwd2,        BorderLayout.EAST);

        // Password policy hint
        JLabel policyHint = new JLabel(
            "<html><span style='color:#6b7280;font-size:9pt'>" +
            PasswordValidator.POLICY_MESSAGE + "</span></html>");
        policyHint.setFont(AppTheme.FONT_SMALL);
        policyHint.setForeground(AppTheme.TEXT_MUTED);

        // ── Form grid ────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(AppTheme.BG_CARD);
        GridBagConstraints c = new GridBagConstraints();
        c.insets  = new Insets(5, 0, 5, 0);
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.anchor  = GridBagConstraints.WEST;
        c.weightx = 1;

        c.gridx = 0; c.gridy = 0; form.add(lblFullName,   c);
        c.gridy = 1;               form.add(txtFullName,   c);
        c.gridy = 2;               form.add(lblUsername,   c);
        c.gridy = 3;               form.add(txtUsername,   c);
        c.gridy = 4;               form.add(lblPassword,   c);
        c.gridy = 5;               form.add(pwdRow1,       c);
        c.gridy = 6;               form.add(policyHint,    c);
        c.gridy = 7;               form.add(jLabel1,       c);
        c.gridy = 8;               form.add(pwdRow2,       c);

        // ── Buttons ──────────────────────────────────────────────────
        btnRegister = AppTheme.primaryBtn("Register");
        btnRegister.setPreferredSize(new Dimension(220, AppTheme.BTN_H));
        btnRegister.setMaximumSize(new Dimension(220, AppTheme.BTN_H));
        btnRegister.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRegister.addActionListener(this::btnRegisterActionPerformed);

        btnClear = AppTheme.secondaryBtn("Clear");
        btnBack  = AppTheme.secondaryBtn("Back");
        btnClear.addActionListener(this::btnClearActionPerformed);
        btnBack.addActionListener(this::btnBackActionPerformed);

        JPanel utilRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        utilRow.setBackground(AppTheme.BG_CARD);
        utilRow.add(btnClear);
        utilRow.add(btnBack);

        // ── Card panel ───────────────────────────────────────────────
        JPanel card = new JPanel();
        card.setBackground(AppTheme.BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(28, 60, 28, 60));
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(6));
        card.add(lblSubtitle);
        card.add(Box.createVerticalStrut(22));
        card.add(form);
        card.add(Box.createVerticalStrut(6));
        card.add(utilRow);
        card.add(Box.createVerticalStrut(16));
        card.add(btnRegister);
        card.add(Box.createVerticalGlue());

        // ── Root layout ──────────────────────────────────────────────
        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(AppTheme.BG_MAIN);
        center.setBorder(BorderFactory.createEmptyBorder(16, 60, 16, 60));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        center.add(card, gbc);
        getContentPane().add(center, BorderLayout.CENTER);

        getRootPane().setDefaultButton(btnRegister);
    }// </editor-fold>//GEN-END:initComponents

    private void btnRegisterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegisterActionPerformed
        String fullName        = txtFullName.getText().trim();
        String username        = txtUsername.getText().trim();
        String password        = new String(txtPassword.getPassword()).trim();
        String confirmPassword = new String(txtConfirmPassword.getPassword()).trim();

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            AppTheme.showWarning(this, "Validation Error", "Please complete all fields.");
            return;
        }
        if (fullName.length() < 3) {
            AppTheme.showWarning(this, "Validation Error",
                    "Full name must be at least 3 characters.");
            txtFullName.requestFocus(); return;
        }
        if (username.length() < 4) {
            AppTheme.showWarning(this, "Validation Error",
                    "Username must be at least 4 characters.");
            txtUsername.requestFocus(); return;
        }
        if (!PasswordValidator.isCompliant(password)) {
            AppTheme.showWarning(this, "Validation Error", PasswordValidator.POLICY_MESSAGE);
            txtPassword.requestFocus(); return;
        }
        if (!password.equals(confirmPassword)) {
            AppTheme.showWarning(this, "Validation Error", "Passwords do not match.");
            txtConfirmPassword.setText("");
            txtConfirmPassword.requestFocus(); return;
        }

        try {
            AuthService.register(fullName, username, password);
            AppTheme.showInfo(this, "Registration Successful",
                    "Registration successful!\nYou can now login.");
            new CustomerLoginFrame().setVisible(true);
            dispose();
        } catch (RuntimeException e) {
            AppTheme.showError(this, "Registration Failed", e.getMessage());
            txtUsername.requestFocus();
        }
    }//GEN-LAST:event_btnRegisterActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        txtFullName.setText("");
        txtUsername.setText("");
        txtPassword.setText("");
        txtConfirmPassword.setText("");
        txtFullName.requestFocus();
    }//GEN-LAST:event_btnClearActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new CustomerLoginFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new CustomerRegisterFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton         btnRegister;
    private javax.swing.JButton         btnClear;
    private javax.swing.JButton         btnBack;
    private javax.swing.JLabel          jLabel1;
    private javax.swing.JLabel          lblFullName;
    private javax.swing.JLabel          lblPassword;
    private javax.swing.JLabel          lblTitle;
    private javax.swing.JLabel          lblUsername;
    private javax.swing.JPasswordField  txtConfirmPassword;
    private javax.swing.JTextField      txtFullName;
    private javax.swing.JPasswordField  txtPassword;
    private javax.swing.JTextField      txtUsername;
    // End of variables declaration//GEN-END:variables
}
