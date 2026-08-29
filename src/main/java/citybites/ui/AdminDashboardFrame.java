package citybites.ui;

import citybites.service.FoodService;
import citybites.service.OrderService;
import citybites.service.AuthService;
import java.awt.*;
import javax.swing.*;

public class AdminDashboardFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(AdminDashboardFrame.class.getName());

    public AdminDashboardFrame() {
        initComponents();
        setTitle("City Bites - Admin Dashboard");
        setMinimumSize(new Dimension(760, 520));
        setSize(860, 580);
        setLocationRelativeTo(null);
        setResizable(true);
        loadSummaryCards();
    }

    private void loadSummaryCards() {
        try {
            int totalFoods     = FoodService.countAllFoodItems();
            int availableFoods = FoodService.countAvailableFoodItems();
            int pendingOrders  = OrderService.countPendingOrders();
            int totalCustomers = AuthService.countCustomers();

            cardPanel.removeAll();
            cardPanel.add(AppTheme.summaryCard("Total Menu Items",   String.valueOf(totalFoods),     AppTheme.BRAND_SECONDARY));
            cardPanel.add(AppTheme.summaryCard("Available Items",    String.valueOf(availableFoods), AppTheme.SUCCESS));
            cardPanel.add(AppTheme.summaryCard("Pending Orders",     String.valueOf(pendingOrders),  AppTheme.WARNING));
            cardPanel.add(AppTheme.summaryCard("Total Customers",    String.valueOf(totalCustomers), AppTheme.BRAND_PRIMARY));
            cardPanel.revalidate();
            cardPanel.repaint();
        } catch (Exception e) {
            logger.warning("Failed to load summary cards: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        lblTitle      = new javax.swing.JLabel();
        lblWelcome    = new javax.swing.JLabel();
        btnManageFood = new javax.swing.JButton();
        btnViewOrders = new javax.swing.JButton();
        btnLogout     = new javax.swing.JButton();
        cardPanel     = new JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        // ── Header ──────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppTheme.BG_HEADER);
        header.setBorder(javax.swing.BorderFactory.createEmptyBorder(AppTheme.PAD_MD, AppTheme.PAD_LG, AppTheme.PAD_MD, AppTheme.PAD_LG));

        lblTitle.setText("CITY BITES");
        lblTitle.setFont(AppTheme.FONT_LOGO);
        lblTitle.setForeground(AppTheme.BRAND_ACCENT);

        lblWelcome.setText("Welcome, Administrator");
        lblWelcome.setFont(AppTheme.FONT_BODY);
        lblWelcome.setForeground(AppTheme.TEXT_WHITE);

        JPanel headerText = new JPanel();
        headerText.setBackground(AppTheme.BG_HEADER);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.add(lblTitle);
        headerText.add(lblWelcome);
        header.add(headerText, BorderLayout.WEST);

        // ── Summary cards ────────────────────────────────────────
        cardPanel.setBackground(AppTheme.BG_MAIN);
        cardPanel.setLayout(new GridLayout(1, 4, 14, 0));
        cardPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(AppTheme.PAD_LG, AppTheme.PAD_LG, AppTheme.PAD_MD, AppTheme.PAD_LG));

        // ── Navigation buttons ───────────────────────────────────
        btnManageFood = AppTheme.wideBtn("Manage Food Items",    AppTheme.BRAND_SECONDARY);
        btnViewOrders = AppTheme.wideBtn("View Customer Orders", AppTheme.BRAND_PRIMARY);
        btnLogout     = AppTheme.secondaryBtn("Logout");

        btnManageFood.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnViewOrders.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogout.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnManageFood.addActionListener(this::btnManageFoodActionPerformed);
        btnViewOrders.addActionListener(this::btnViewOrdersActionPerformed);
        btnLogout.addActionListener(this::btnLogoutActionPerformed);

        JPanel navPanel = new JPanel();
        navPanel.setBackground(AppTheme.BG_CARD);
        navPanel.setBorder(AppTheme.cardBorder());
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));

        JLabel navTitle = new JLabel("Quick Actions");
        navTitle.setFont(AppTheme.FONT_HEADING);
        navTitle.setForeground(AppTheme.TEXT_PRIMARY);
        navTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        navPanel.add(Box.createVerticalStrut(14));
        navPanel.add(navTitle);
        navPanel.add(Box.createVerticalStrut(20));
        navPanel.add(btnManageFood);
        navPanel.add(Box.createVerticalStrut(14));
        navPanel.add(btnViewOrders);
        navPanel.add(Box.createVerticalStrut(30));
        navPanel.add(btnLogout);
        navPanel.add(Box.createVerticalGlue());

        JPanel centerPanel = new JPanel(new BorderLayout(14, 0));
        centerPanel.setBackground(AppTheme.BG_MAIN);
        centerPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, AppTheme.PAD_LG, AppTheme.PAD_LG, AppTheme.PAD_LG));
        centerPanel.add(navPanel, BorderLayout.CENTER);

        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header,      BorderLayout.NORTH);
        getContentPane().add(cardPanel,   BorderLayout.CENTER);
        getContentPane().add(centerPanel, BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    private void btnManageFoodActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnManageFoodActionPerformed
        new FoodManagementFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnManageFoodActionPerformed

    private void btnViewOrdersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnViewOrdersActionPerformed
        new AdminOrdersFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnViewOrdersActionPerformed

    private void btnLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogoutActionPerformed
        int r = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            new AdminLoginFrame().setVisible(true);
            dispose();
        }
    }//GEN-LAST:event_btnLogoutActionPerformed

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new AdminDashboardFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnLogout;
    private javax.swing.JButton btnManageFood;
    private javax.swing.JButton btnViewOrders;
    private javax.swing.JLabel  lblTitle;
    private javax.swing.JLabel  lblWelcome;
    private JPanel              cardPanel;
    // End of variables declaration//GEN-END:variables
}
