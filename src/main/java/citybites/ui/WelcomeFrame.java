package citybites.ui;

import java.awt.*;
import javax.swing.*;

public class WelcomeFrame extends javax.swing.JFrame {

    public WelcomeFrame() {
        initComponents();
        setTitle("City Bites - Welcome");
        setMinimumSize(new Dimension(680, 500));
        setSize(760, 560);
        setLocationRelativeTo(null);
        setResizable(true);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        lblLogo     = new javax.swing.JLabel();
        lblTagline  = new javax.swing.JLabel();
        lblPrompt   = new javax.swing.JLabel();
        btnAdmin    = new javax.swing.JButton();
        btnCustomer = new javax.swing.JButton();
        btnExit     = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lblLogo.setText("CITY BITES");
        lblLogo.setFont(AppTheme.FONT_LOGO);
        lblLogo.setForeground(AppTheme.BRAND_PRIMARY);
        lblLogo.setHorizontalAlignment(SwingConstants.CENTER);
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblTagline.setText("Food Management System");
        lblTagline.setFont(AppTheme.FONT_HEADING);
        lblTagline.setForeground(AppTheme.BRAND_SECONDARY);
        lblTagline.setHorizontalAlignment(SwingConstants.CENTER);
        lblTagline.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblPrompt.setText("Choose your portal to continue");
        lblPrompt.setFont(AppTheme.FONT_BODY);
        lblPrompt.setForeground(AppTheme.TEXT_MUTED);
        lblPrompt.setHorizontalAlignment(SwingConstants.CENTER);
        lblPrompt.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnAdmin    = AppTheme.wideBtn("Admin Portal",    AppTheme.BRAND_SECONDARY);
        btnCustomer = AppTheme.wideBtn("Customer Portal", AppTheme.BRAND_PRIMARY);
        btnAdmin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCustomer.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnAdmin.addActionListener(this::btnAdminActionPerformed);
        btnCustomer.addActionListener(this::btnCustomerActionPerformed);

        btnExit = new JButton("Exit");
        btnExit.setFont(AppTheme.FONT_BODY);
        btnExit.setForeground(AppTheme.TEXT_MUTED);
        btnExit.setBorderPainted(false);
        btnExit.setContentAreaFilled(false);
        btnExit.setFocusPainted(false);
        btnExit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExit.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnExit.addActionListener(this::btnExitActionPerformed);

        JPanel divider = new JPanel();
        divider.setBackground(AppTheme.BRAND_PRIMARY);
        divider.setMaximumSize(new Dimension(60, 3));
        divider.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel center = new JPanel();
        center.setBackground(AppTheme.BG_CARD);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(BorderFactory.createEmptyBorder(50, 80, 40, 80));
        center.add(lblLogo);
        center.add(Box.createVerticalStrut(6));
        center.add(lblTagline);
        center.add(Box.createVerticalStrut(12));
        center.add(divider);
        center.add(Box.createVerticalStrut(30));
        center.add(lblPrompt);
        center.add(Box.createVerticalStrut(28));
        center.add(btnAdmin);
        center.add(Box.createVerticalStrut(14));
        center.add(btnCustomer);
        center.add(Box.createVerticalStrut(30));
        center.add(btnExit);
        center.add(Box.createVerticalGlue());

        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1; gbc.weighty = 1;
        getContentPane().add(center, gbc);
    }// </editor-fold>//GEN-END:initComponents

    private void btnAdminActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAdminActionPerformed
        new AdminLoginFrame().setVisible(true); dispose();
    }//GEN-LAST:event_btnAdminActionPerformed

    private void btnCustomerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCustomerActionPerformed
        new CustomerLoginFrame().setVisible(true); dispose();
    }//GEN-LAST:event_btnCustomerActionPerformed

    private void btnExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExitActionPerformed
        int r = JOptionPane.showConfirmDialog(this, "Exit CityBites?",
                "Confirm Exit", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) System.exit(0);
    }//GEN-LAST:event_btnExitActionPerformed

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new WelcomeFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdmin;
    private javax.swing.JButton btnCustomer;
    private javax.swing.JButton btnExit;
    private javax.swing.JLabel  lblLogo;
    private javax.swing.JLabel  lblTagline;
    private javax.swing.JLabel  lblPrompt;
    // End of variables declaration//GEN-END:variables
}
