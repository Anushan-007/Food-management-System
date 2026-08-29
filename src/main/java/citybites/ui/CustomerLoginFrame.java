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
        setMinimumSize(new Dimension(520, 500));
        setSize(580, 540);
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

        // ── Header ──────────────────────────────────────────────
        JPanel header = AppTheme.headerPanel("Customer Portal");

        // ── Labels ──────────────────────────────────────────────
        lblTitle.setText("Customer Login");
        lblTitle.setFont(AppTheme.FONT_TITLE);
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblSubtitle.setText("Sign in to browse and order food");
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
        txtPassword.addActionListener(this::btnLoginActionPerformed);

        btnLogin    = AppTheme.primaryBtn("Login");
        btnRegister = AppTheme.successBtn("Create Account");
        btnClear    = AppTheme.secondaryBtn("Clear");
        btnBack     = AppTheme.secondaryBtn("Back");

        btnLogin.setPreferredSize(new Dimension(200, 38));
        btnLogin.setMaximumSize(new Dimension(200, 38));
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRegister.setPreferredSize(new Dimension(200, 38));
        btnRegister.setMaximumSize(new Dimension(200, 38));
        btnRegister.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnLogin.addActionListener(this::btnLoginActionPerformed);
        btnRegister.addActionListener(this::btnRegisterActionPerformed);
        btnClear.addActionListener(this::btnClearActionPerformed);
        btnBack.addActionListener(this::btnBackActionPerformed);

        JLabel lblOr = new JLabel("New to City Bites?");
        lblOr.setFont(AppTheme.FONT_SMALL);
        lblOr.setForeground(AppTheme.TEXT_MUTED);
        lblOr.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Form ─────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(AppTheme.BG_CARD);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 0, 6, 0);
        c.fill   = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;

        c.gridx = 0; c.gridy = 0; form.add(lblUsername, c);
        c.gridy = 1;              form.add(txtUsername,  c);
        c.gridy = 2;              form.add(lblPassword,  c);
        c.gridy = 3;              form.add(txtPassword,  c);

        JPanel utilRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        utilRow.setBackground(AppTheme.BG_CARD);
        utilRow.add(btnClear);
        utilRow.add(btnBack);

        // ── Card ─────────────────────────────────────────────────
        JPanel card = new JPanel();
        card.setBackground(AppTheme.BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(javax.swing.BorderFactory.createEmptyBorder(32, 60, 32, 60));
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(6));
        card.add(lblSubtitle);
        card.add(Box.createVerticalStrut(26));
        card.add(form);
        card.add(Box.createVerticalStrut(6));
        card.add(utilRow);
        card.add(Box.createVerticalStrut(14));
        card.add(btnLogin);
        card.add(Box.createVerticalStrut(18));
        card.add(lblOr);
        card.add(Box.createVerticalStrut(8));
        card.add(btnRegister);
        card.add(Box.createVerticalGlue());

        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(AppTheme.BG_MAIN);
        center.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 60, 20, 60));
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
        try {
            Optional<Customer> result = AuthService.customerLogin(username, password);
            if (result.isPresent()) {
                SessionManager.setLoggedInCustomer(result.get());
                logger.info("Customer login: " + username);
                new CustomerDashboardFrame().setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password.",
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
                txtPassword.setText("");
                txtPassword.requestFocus();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
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
    private javax.swing.JButton    btnLogin;
    private javax.swing.JButton    btnRegister;
    private javax.swing.JButton    btnClear;
    private javax.swing.JButton    btnBack;
    private javax.swing.JTextField txtUsername;
    private javax.swing.JPasswordField txtPassword;
    private javax.swing.JLabel     lblUsername;
    private javax.swing.JLabel     lblPassword;
    private javax.swing.JLabel     lblTitle;
    private javax.swing.JLabel     lblSubtitle;
    // End of variables declaration//GEN-END:variables
}
