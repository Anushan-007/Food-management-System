package citybites.ui;

import citybites.service.AuthService;
import java.awt.*;
import javax.swing.*;

public class AdminLoginFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(AdminLoginFrame.class.getName());

    public AdminLoginFrame() {
        initComponents();
        setTitle("City Bites - Admin Login");
        setMinimumSize(new Dimension(520, 460));
        setSize(600, 520);
        setLocationRelativeTo(null);
        setResizable(true);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        txtUsername = new javax.swing.JTextField();
        txtPassword = new javax.swing.JPasswordField();
        btnLogin    = new javax.swing.JButton();
        btnBack     = new javax.swing.JButton();
        lblUsername = new javax.swing.JLabel();
        lblPassword = new javax.swing.JLabel();
        lblTitle    = new javax.swing.JLabel();
        lblSubtitle = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        // ── Navigation header ────────────────────────────────────────
        JButton logoutBtn = AppTheme.ghostBtn("← Back");
        logoutBtn.setForeground(new Color(150, 170, 190));
        logoutBtn.addActionListener(this::btnBackActionPerformed);
        JPanel header = AppTheme.navHeader("Admin Portal", null, logoutBtn);

        // ── Card: title & subtitle ───────────────────────────────────
        lblTitle.setText("Welcome Back");
        lblTitle.setFont(AppTheme.FONT_TITLE);
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblSubtitle.setText("Sign in to manage City Bites");
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
        pwdRow.add(txtPassword,  BorderLayout.CENTER);
        pwdRow.add(togglePwd,    BorderLayout.EAST);

        // ── Form grid ────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(AppTheme.BG_CARD);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 0, 6, 0);
        c.fill   = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;

        c.gridx = 0; c.gridy = 0; form.add(lblUsername, c);
        c.gridy = 1;               form.add(txtUsername,  c);
        c.gridy = 2;               form.add(lblPassword,  c);
        c.gridy = 3;               form.add(pwdRow,       c);

        // ── Buttons ──────────────────────────────────────────────────
        btnLogin = AppTheme.primaryBtn("Login");
        btnLogin.setPreferredSize(new Dimension(220, AppTheme.BTN_H));
        btnLogin.setMaximumSize(new Dimension(220, AppTheme.BTN_H));
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.addActionListener(this::btnLoginActionPerformed);

        btnBack = AppTheme.secondaryBtn("Back");
        btnBack.setPreferredSize(new Dimension(220, AppTheme.BTN_H));
        btnBack.setMaximumSize(new Dimension(220, AppTheme.BTN_H));
        btnBack.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnBack.addActionListener(this::btnBackActionPerformed);

        // ── Card panel ───────────────────────────────────────────────
        JPanel card = new JPanel();
        card.setBackground(AppTheme.BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(36, 60, 36, 60));
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(6));
        card.add(lblSubtitle);
        card.add(Box.createVerticalStrut(30));
        card.add(form);
        card.add(Box.createVerticalStrut(24));
        card.add(btnLogin);
        card.add(Box.createVerticalStrut(10));
        card.add(btnBack);
        card.add(Box.createVerticalGlue());

        // ── Root layout ──────────────────────────────────────────────
        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(AppTheme.BG_MAIN);
        center.setBorder(BorderFactory.createEmptyBorder(24, 60, 24, 60));
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
        boolean ok = AuthService.adminLogin(username, password);
        if (ok) {
            logger.info("Admin login successful: " + username);
            new AdminDashboardFrame().setVisible(true);
            dispose();
        } else {
            AppTheme.showError(this, "Login Failed",
                    "Invalid username or password.");
            txtPassword.setText("");
            txtPassword.requestFocus();
        }
    }//GEN-LAST:event_btnLoginActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new WelcomeFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new AdminLoginFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton         btnLogin;
    private javax.swing.JButton         btnBack;
    private javax.swing.JTextField      txtUsername;
    private javax.swing.JPasswordField  txtPassword;
    private javax.swing.JLabel          lblUsername;
    private javax.swing.JLabel          lblPassword;
    private javax.swing.JLabel          lblTitle;
    private javax.swing.JLabel          lblSubtitle;
    // End of variables declaration//GEN-END:variables
}
