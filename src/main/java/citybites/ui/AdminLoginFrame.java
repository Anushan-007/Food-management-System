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
        setMinimumSize(new Dimension(520, 440));
        setSize(580, 490);
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

        // ── Header ──────────────────────────────────────────────
        JPanel header = AppTheme.headerPanel("Admin Portal");

        // ── Card ────────────────────────────────────────────────
        lblTitle.setText("Welcome Back");
        lblTitle.setFont(AppTheme.FONT_TITLE);
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblSubtitle.setText("Sign in to manage City Bites");
        lblSubtitle.setFont(AppTheme.FONT_BODY);
        lblSubtitle.setForeground(AppTheme.TEXT_MUTED);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblUsername.setText("Username");
        lblUsername.setFont(AppTheme.FONT_LABEL);
        lblUsername.setForeground(AppTheme.TEXT_PRIMARY);

        lblPassword.setText("Password");
        lblPassword.setFont(AppTheme.FONT_LABEL);
        lblPassword.setForeground(AppTheme.TEXT_PRIMARY);

        AppTheme.styleField(txtUsername);
        txtPassword.setFont(AppTheme.FONT_BODY);
        txtPassword.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(AppTheme.BORDER),
            javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        txtPassword.setBackground(AppTheme.BG_INPUT);

        btnLogin = AppTheme.primaryBtn("Login");
        btnLogin.setPreferredSize(new Dimension(200, 38));
        btnLogin.setMaximumSize(new Dimension(200, 38));
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.addActionListener(this::btnLoginActionPerformed);

        btnBack = AppTheme.secondaryBtn("Back");
        btnBack.setPreferredSize(new Dimension(200, 38));
        btnBack.setMaximumSize(new Dimension(200, 38));
        btnBack.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnBack.addActionListener(this::btnBackActionPerformed);

        // ── Form panel with GridBagLayout ────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(AppTheme.BG_CARD);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 0, 6, 0);
        c.fill   = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; c.weightx = 1;
        form.add(lblUsername, c);
        c.gridy = 1;
        form.add(txtUsername, c);
        c.gridy = 2;
        form.add(lblPassword, c);
        c.gridy = 3;
        form.add(txtPassword, c);

        // ── Card panel ──────────────────────────────────────────
        JPanel card = new JPanel();
        card.setBackground(AppTheme.BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(javax.swing.BorderFactory.createEmptyBorder(36, 60, 36, 60));
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(6));
        card.add(lblSubtitle);
        card.add(Box.createVerticalStrut(28));
        card.add(form);
        card.add(Box.createVerticalStrut(22));
        card.add(btnLogin);
        card.add(Box.createVerticalStrut(10));
        card.add(btnBack);
        card.add(Box.createVerticalGlue());

        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(AppTheme.BG_MAIN);
        center.setBorder(javax.swing.BorderFactory.createEmptyBorder(30, 60, 30, 60));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1; gbc.weighty = 1;
        center.add(card, gbc);
        getContentPane().add(center, BorderLayout.CENTER);

        getRootPane().setDefaultButton(btnLogin);
    }// </editor-fold>//GEN-END:initComponents

    private void btnLoginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoginActionPerformed
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean ok = AuthService.adminLogin(username, password);
        if (ok) {
            logger.info("Admin login successful: " + username);
            new AdminDashboardFrame().setVisible(true);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Invalid username or password.",
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
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
    private javax.swing.JButton    btnLogin;
    private javax.swing.JButton    btnBack;
    private javax.swing.JTextField txtUsername;
    private javax.swing.JPasswordField txtPassword;
    private javax.swing.JLabel     lblUsername;
    private javax.swing.JLabel     lblPassword;
    private javax.swing.JLabel     lblTitle;
    private javax.swing.JLabel     lblSubtitle;
    // End of variables declaration//GEN-END:variables
}
