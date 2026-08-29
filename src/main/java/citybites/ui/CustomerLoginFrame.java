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
        setSize(600, 450);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblTitle    = new javax.swing.JLabel();
        lblSubtitle = new javax.swing.JLabel();
        lblUsername = new javax.swing.JLabel();
        txtUsername = new javax.swing.JTextField();
        lblPassword = new javax.swing.JLabel();
        txtPassword = new javax.swing.JPasswordField();
        btnLogin    = new javax.swing.JButton();
        btnRegister = new javax.swing.JButton();
        btnClear    = new javax.swing.JButton();
        btnBack     = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lblTitle.setText("CITY BITES");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(new Color(41, 128, 185));
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);

        lblSubtitle.setText("Customer Login");
        lblSubtitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblSubtitle.setForeground(new Color(44, 62, 80));
        lblSubtitle.setHorizontalAlignment(SwingConstants.CENTER);

        Font labelFont = new Font("Segoe UI", Font.PLAIN, 13);
        lblUsername.setText("Username:");
        lblUsername.setFont(labelFont);
        lblPassword.setText("Password:");
        lblPassword.setFont(labelFont);

        txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtUsername.setPreferredSize(new Dimension(220, 28));

        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtPassword.setPreferredSize(new Dimension(220, 28));
        txtPassword.addActionListener(evt -> btnLoginActionPerformed(evt));

        Dimension btnSize = new Dimension(100, 30);

        btnLogin.setText("Login");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnLogin.setPreferredSize(btnSize);
        btnLogin.addActionListener(this::btnLoginActionPerformed);

        btnRegister.setText("Register");
        btnRegister.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnRegister.setPreferredSize(btnSize);
        btnRegister.addActionListener(this::btnRegisterActionPerformed);

        btnClear.setText("Clear");
        btnClear.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnClear.setPreferredSize(btnSize);
        btnClear.addActionListener(this::btnClearActionPerformed);

        btnBack.setText("Back");
        btnBack.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnBack.setPreferredSize(btnSize);
        btnBack.addActionListener(this::btnBackActionPerformed);

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(30, 20, 20, 20));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblTitle);
        headerPanel.add(Box.createVerticalStrut(6));
        headerPanel.add(lblSubtitle);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 60, 10, 60));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 8, 10, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        formPanel.add(lblUsername, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        formPanel.add(txtUsername, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        formPanel.add(lblPassword, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        formPanel.add(txtPassword, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 20));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.add(btnLogin);
        btnPanel.add(btnRegister);
        btnPanel.add(btnClear);
        btnPanel.add(btnBack);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(formPanel, BorderLayout.CENTER);
        centerPanel.add(btnPanel,  BorderLayout.SOUTH);

        getContentPane().setBackground(Color.WHITE);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(headerPanel, BorderLayout.NORTH);
        getContentPane().add(centerPanel, BorderLayout.CENTER);

    }// </editor-fold>//GEN-END:initComponents

    private void btnLoginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoginActionPerformed
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter username and password.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Optional<Customer> result = AuthService.customerLogin(username, password);
            if (result.isPresent()) {
                SessionManager.setLoggedInCustomer(result.get());
                JOptionPane.showMessageDialog(this,
                        "Login successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
                new CustomerDashboardFrame().setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Invalid username or password.",
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
                txtPassword.setText("");
                txtPassword.requestFocus();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Database error: " + e.getMessage(),
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

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new CustomerLoginFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnClear;
    private javax.swing.JButton btnLogin;
    private javax.swing.JButton btnRegister;
    private javax.swing.JLabel  lblPassword;
    private javax.swing.JLabel  lblSubtitle;
    private javax.swing.JLabel  lblTitle;
    private javax.swing.JLabel  lblUsername;
    private javax.swing.JPasswordField txtPassword;
    private javax.swing.JTextField     txtUsername;
    // End of variables declaration//GEN-END:variables
}
