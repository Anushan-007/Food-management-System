package citybites.ui;

import java.awt.*;
import javax.swing.*;

public class WelcomeFrame extends javax.swing.JFrame {

    public WelcomeFrame() {
        initComponents();
        setTitle("City Bites - Welcome");
        setMinimumSize(new Dimension(760, 500));
        setSize(920, 580);
        setLocationRelativeTo(null);
        setResizable(true);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        btnAdmin    = new javax.swing.JButton();
        btnCustomer = new javax.swing.JButton();
        btnExit     = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        // ── LEFT PANEL: brand hero ──────────────────────────────────
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(AppTheme.BG_HEADER);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(60, 48, 60, 48));

        JLabel logo = new JLabel("CITY BITES");
        logo.setFont(AppTheme.FONT_LOGO);
        logo.setForeground(AppTheme.BRAND_ACCENT);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tagline = new JLabel("Food Management System");
        tagline.setFont(AppTheme.FONT_HEADING);
        tagline.setForeground(AppTheme.TEXT_WHITE);
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Decorative accent divider
        JPanel divider = new JPanel();
        divider.setBackground(AppTheme.BRAND_ACCENT);
        divider.setMaximumSize(new Dimension(60, 3));
        divider.setPreferredSize(new Dimension(60, 3));
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel desc1 = new JLabel("Fresh ingredients,");
        desc1.setFont(AppTheme.FONT_BODY);
        desc1.setForeground(new Color(150, 170, 190));
        desc1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel desc2 = new JLabel("exceptional taste.");
        desc2.setFont(AppTheme.FONT_BODY);
        desc2.setForeground(new Color(150, 170, 190));
        desc2.setAlignmentX(Component.LEFT_ALIGNMENT);

        leftPanel.add(Box.createVerticalGlue());
        leftPanel.add(logo);
        leftPanel.add(Box.createVerticalStrut(8));
        leftPanel.add(tagline);
        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.add(divider);
        leftPanel.add(Box.createVerticalStrut(24));
        leftPanel.add(desc1);
        leftPanel.add(Box.createVerticalStrut(4));
        leftPanel.add(desc2);
        leftPanel.add(Box.createVerticalGlue());

        // ── RIGHT PANEL: portal selection ───────────────────────────
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(AppTheme.BG_CARD);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(60, 60, 60, 60));

        JLabel welcomeLabel = new JLabel("Welcome!");
        welcomeLabel.setFont(AppTheme.FONT_TITLE);
        welcomeLabel.setForeground(AppTheme.TEXT_PRIMARY);
        welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel promptLabel = new JLabel("Choose your portal to continue");
        promptLabel.setFont(AppTheme.FONT_BODY);
        promptLabel.setForeground(AppTheme.TEXT_MUTED);
        promptLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Admin portal card
        JPanel adminCard = buildPortalCard(
            "Admin Portal",
            "Manage menu, orders and inventory",
            AppTheme.BRAND_SECONDARY
        );
        adminCard.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                btnAdminActionPerformed(null);
            }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                adminCard.setBackground(new Color(236, 240, 241));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                adminCard.setBackground(AppTheme.BG_CARD);
            }
        });

        // Customer portal card
        JPanel customerCard = buildPortalCard(
            "Customer Portal",
            "Browse menu and place orders",
            AppTheme.BRAND_ACCENT
        );
        customerCard.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                btnCustomerActionPerformed(null);
            }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                customerCard.setBackground(AppTheme.BG_ACCENT_SOFT);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                customerCard.setBackground(AppTheme.BG_CARD);
            }
        });

        // Exit — ghost button
        btnExit = AppTheme.ghostBtn("Exit Application");
        btnExit.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnExit.addActionListener(this::btnExitActionPerformed);

        rightPanel.add(Box.createVerticalGlue());
        rightPanel.add(welcomeLabel);
        rightPanel.add(Box.createVerticalStrut(8));
        rightPanel.add(promptLabel);
        rightPanel.add(Box.createVerticalStrut(32));
        rightPanel.add(adminCard);
        rightPanel.add(Box.createVerticalStrut(16));
        rightPanel.add(customerCard);
        rightPanel.add(Box.createVerticalStrut(28));
        rightPanel.add(btnExit);
        rightPanel.add(Box.createVerticalGlue());

        // ── Root: split left/right ───────────────────────────────────
        getContentPane().setLayout(new GridLayout(1, 2));
        getContentPane().add(leftPanel);
        getContentPane().add(rightPanel);
    }// </editor-fold>//GEN-END:initComponents

    /** Builds a clickable portal card with accent border stripe. */
    private JPanel buildPortalCard(String title, String subtitle, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(AppTheme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18))));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(AppTheme.FONT_SUBHEAD);
        titleLbl.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(AppTheme.FONT_SMALL);
        subLbl.setForeground(AppTheme.TEXT_MUTED);

        JPanel text = new JPanel();
        text.setBackground(AppTheme.BG_CARD);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(titleLbl);
        text.add(Box.createVerticalStrut(4));
        text.add(subLbl);

        JLabel arrow = new JLabel(">");
        arrow.setFont(AppTheme.FONT_HEADING);
        arrow.setForeground(accent);

        card.add(text,  BorderLayout.CENTER);
        card.add(arrow, BorderLayout.EAST);
        return card;
    }

    private void btnAdminActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAdminActionPerformed
        new AdminLoginFrame().setVisible(true); dispose();
    }//GEN-LAST:event_btnAdminActionPerformed

    private void btnCustomerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCustomerActionPerformed
        new CustomerLoginFrame().setVisible(true); dispose();
    }//GEN-LAST:event_btnCustomerActionPerformed

    private void btnExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExitActionPerformed
        if (AppTheme.showConfirm(this, "Exit CityBites", "Exit CityBites?")) {
            System.exit(0);
        }
    }//GEN-LAST:event_btnExitActionPerformed

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new WelcomeFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdmin;
    private javax.swing.JButton btnCustomer;
    private javax.swing.JButton btnExit;
    // End of variables declaration//GEN-END:variables
}
