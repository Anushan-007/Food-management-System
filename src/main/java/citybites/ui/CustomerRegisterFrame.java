package citybites.ui;

import citybites.service.AuthService;
import java.awt.*;
import javax.swing.*;

public class CustomerRegisterFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(CustomerRegisterFrame.class.getName());

    public CustomerRegisterFrame() {
        initComponents();
        setTitle("City Bites - Customer Registration");
        setSize(700, 600);
        setLocationRelativeTo(null);
        setResizable(false);
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

        lblTitle.setText("Customer Registration");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(44, 62, 80));
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);

        Font labelFont = new Font("Segoe UI", Font.PLAIN, 13);
        lblFullName.setText("Full Name:");        lblFullName.setFont(labelFont);
        lblUsername.setText("Username:");         lblUsername.setFont(labelFont);
        lblPassword.setText("Password:");         lblPassword.setFont(labelFont);
        jLabel1.setText("Confirm Password:");     jLabel1.setFont(labelFont);

        Font fieldFont   = new Font("Segoe UI", Font.PLAIN, 13);
        Dimension fieldSize = new Dimension(250, 28);
        txtFullName.setFont(fieldFont);        txtFullName.setPreferredSize(fieldSize);
        txtUsername.setFont(fieldFont);        txtUsername.setPreferredSize(fieldSize);
        txtPassword.setFont(fieldFont);        txtPassword.setPreferredSize(fieldSize);
        txtConfirmPassword.setFont(fieldFont); txtConfirmPassword.setPreferredSize(fieldSize);

        Dimension btnSize = new Dimension(110, 32);
        btnRegister.setText("Register");
        btnRegister.setFont(new Font("Segoe UI", Font.BOLD, 13));
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
        headerPanel.setBorder(BorderFactory.createEmptyBorder(35, 20, 20, 20));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblTitle);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 80, 20, 80));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 8, 12, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.35; formPanel.add(lblFullName, gbc);
        gbc.gridx = 1; gbc.weightx = 0.65;                 formPanel.add(txtFullName, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.35; formPanel.add(lblUsername, gbc);
        gbc.gridx = 1; gbc.weightx = 0.65;                 formPanel.add(txtUsername, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.35; formPanel.add(lblPassword, gbc);
        gbc.gridx = 1; gbc.weightx = 0.65;                 formPanel.add(txtPassword, gbc);
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.35; formPanel.add(jLabel1, gbc);
        gbc.gridx = 1; gbc.weightx = 0.65;                 formPanel.add(txtConfirmPassword, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 25));
        btnPanel.setBackground(Color.WHITE);
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

    private void btnRegisterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegisterActionPerformed
        String fullName        = txtFullName.getText().trim();
        String username        = txtUsername.getText().trim();
        String password        = new String(txtPassword.getPassword()).trim();
        String confirmPassword = new String(txtConfirmPassword.getPassword()).trim();

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please complete all fields.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (fullName.length() < 3) {
            JOptionPane.showMessageDialog(this,
                    "Full name must contain at least 3 characters.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtFullName.requestFocus();
            return;
        }
        if (username.length() < 4) {
            JOptionPane.showMessageDialog(this,
                    "Username must contain at least 4 characters.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtUsername.requestFocus();
            return;
        }
        if (password.length() < 4) {
            JOptionPane.showMessageDialog(this,
                    "Password must contain at least 4 characters.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtConfirmPassword.setText("");
            txtConfirmPassword.requestFocus();
            return;
        }

        try {
            AuthService.register(fullName, username, password);
            JOptionPane.showMessageDialog(this,
                    "Registration successful!\nYou can now login.",
                    "Registration Successful", JOptionPane.INFORMATION_MESSAGE);
            new CustomerLoginFrame().setVisible(true);
            dispose();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Registration Failed", JOptionPane.WARNING_MESSAGE);
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

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new CustomerRegisterFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnClear;
    private javax.swing.JButton btnRegister;
    private javax.swing.JLabel  jLabel1;
    private javax.swing.JLabel  lblFullName;
    private javax.swing.JLabel  lblPassword;
    private javax.swing.JLabel  lblTitle;
    private javax.swing.JLabel  lblUsername;
    private javax.swing.JPasswordField txtConfirmPassword;
    private javax.swing.JTextField     txtFullName;
    private javax.swing.JPasswordField txtPassword;
    private javax.swing.JTextField     txtUsername;
    // End of variables declaration//GEN-END:variables
}
