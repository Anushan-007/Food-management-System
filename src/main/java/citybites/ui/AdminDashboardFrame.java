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
        setMinimumSize(new Dimension(820, 560));
        setSize(960, 640);
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
            cardPanel.add(AppTheme.summaryCard("Total Menu Items",
                    String.valueOf(totalFoods),     AppTheme.BRAND_SECONDARY));
            cardPanel.add(AppTheme.summaryCard("Available Items",
                    String.valueOf(availableFoods), AppTheme.SUCCESS));
            cardPanel.add(AppTheme.summaryCard("Pending Orders",
                    String.valueOf(pendingOrders),  AppTheme.WARNING));
            cardPanel.add(AppTheme.summaryCard("Total Customers",
                    String.valueOf(totalCustomers), AppTheme.BRAND_ACCENT));
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

        // ── Navigation header ────────────────────────────────────────
        btnLogout = AppTheme.ghostBtn("Logout");
        btnLogout.setForeground(new Color(150, 170, 190));
        btnLogout.addActionListener(this::btnLogoutActionPerformed);

        JButton refreshBtn = AppTheme.ghostBtn("Refresh");
        refreshBtn.setForeground(new Color(150, 170, 190));
        refreshBtn.addActionListener(e -> loadSummaryCards());

        JPanel header = AppTheme.navHeader("Admin Portal", "Welcome, Administrator",
                                            refreshBtn, btnLogout);

        // ── Summary cards grid ───────────────────────────────────────
        cardPanel.setBackground(AppTheme.BG_MAIN);
        cardPanel.setLayout(new GridLayout(1, 4, 14, 0));
        cardPanel.setBorder(BorderFactory.createEmptyBorder(
                AppTheme.PAD_LG, AppTheme.PAD_LG, AppTheme.PAD_MD, AppTheme.PAD_LG));

        // ── Section title ────────────────────────────────────────────
        JLabel sectionTitle = new JLabel("Quick Actions");
        sectionTitle.setFont(AppTheme.FONT_HEADING);
        sectionTitle.setForeground(AppTheme.TEXT_PRIMARY);

        // ── Action cards ─────────────────────────────────────────────
        JPanel manageFoodCard = buildActionCard(
            "Manage Food Items",
            "Add, edit or remove menu items and manage stock",
            AppTheme.BRAND_SECONDARY,
            this::btnManageFoodActionPerformed
        );

        JPanel manageCatCard = buildActionCard(
            "Manage Categories",
            "Create, rename or remove food categories",
            new Color(100, 160, 220),
            this::btnManageCategoriesActionPerformed
        );

        JPanel viewOrdersCard = buildActionCard(
            "Customer Orders",
            "View and update status of all customer orders",
            AppTheme.BRAND_ACCENT,
            this::btnViewOrdersActionPerformed
        );

        JPanel actionsRow = new JPanel(new GridLayout(1, 3, 14, 0));
        actionsRow.setBackground(AppTheme.BG_MAIN);
        actionsRow.setBorder(BorderFactory.createEmptyBorder(
                0, AppTheme.PAD_LG, AppTheme.PAD_LG, AppTheme.PAD_LG));
        actionsRow.add(manageFoodCard);
        actionsRow.add(manageCatCard);
        actionsRow.add(viewOrdersCard);

        // ── Section header bar ───────────────────────────────────────
        JPanel sectionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.PAD_LG, 8));
        sectionBar.setBackground(AppTheme.BG_MAIN);
        sectionBar.add(sectionTitle);

        // ── Content area ─────────────────────────────────────────────
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(AppTheme.BG_MAIN);
        content.add(cardPanel,  BorderLayout.NORTH);
        content.add(sectionBar, BorderLayout.CENTER);
        content.add(actionsRow, BorderLayout.SOUTH);

        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header,  BorderLayout.NORTH);
        getContentPane().add(content, BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    /** Builds a clickable action card with accent stripe and description. */
    private JPanel buildActionCard(String title, String description, Color accent,
                                   java.awt.event.ActionListener action) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(AppTheme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 5, 0, 0, accent),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(20, 20, 20, 20))));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(AppTheme.FONT_HEADING);
        titleLbl.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel descLbl = new JLabel(
            "<html><body style='width:200px'>" + description + "</body></html>");
        descLbl.setFont(AppTheme.FONT_BODY);
        descLbl.setForeground(AppTheme.TEXT_MUTED);

        JButton btn = AppTheme.primaryBtn("Open");
        btn.setBackground(accent);
        btn.addActionListener(action);

        JPanel text = new JPanel();
        text.setBackground(AppTheme.BG_CARD);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(titleLbl);
        text.add(Box.createVerticalStrut(8));
        text.add(descLbl);

        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnWrap.setBackground(AppTheme.BG_CARD);
        btnWrap.add(btn);

        card.add(text,    BorderLayout.CENTER);
        card.add(btnWrap, BorderLayout.EAST);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                action.actionPerformed(null);
            }
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBackground(AppTheme.BG_ACCENT_SOFT);
                text.setBackground(AppTheme.BG_ACCENT_SOFT);
                btnWrap.setBackground(AppTheme.BG_ACCENT_SOFT);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBackground(AppTheme.BG_CARD);
                text.setBackground(AppTheme.BG_CARD);
                btnWrap.setBackground(AppTheme.BG_CARD);
            }
        });

        return card;
    }

    private void btnManageFoodActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnManageFoodActionPerformed
        new FoodManagementFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnManageFoodActionPerformed

    private void btnManageCategoriesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnManageCategoriesActionPerformed
        new CategoryManagementFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnManageCategoriesActionPerformed

    private void btnViewOrdersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnViewOrdersActionPerformed
        new AdminOrdersFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnViewOrdersActionPerformed

    private void btnLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogoutActionPerformed
        if (AppTheme.showConfirm(this, "Confirm Logout",
                "Are you sure you want to logout?")) {
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
