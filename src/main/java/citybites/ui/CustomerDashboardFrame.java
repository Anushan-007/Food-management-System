package citybites.ui;

import citybites.data.DataStore;
import citybites.model.Customer;
import citybites.util.SessionManager;
import java.awt.*;
import javax.swing.*;

public class CustomerDashboardFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(CustomerDashboardFrame.class.getName());

    public CustomerDashboardFrame() {
        initComponents();
        setTitle("City Bites - Customer Dashboard");
        setMinimumSize(new Dimension(680, 480));
        setSize(760, 540);
        setLocationRelativeTo(null);
        setResizable(true);

        Customer customer = SessionManager.getLoggedInCustomer();
        if (customer != null) {
            lblWelcome.setText("Welcome back, " + customer.getFullName() + "!");
        }

        int cartCount = DataStore.cartItems.size();
        String cartLabel = cartCount > 0
                ? "View Cart  (" + cartCount + " item" + (cartCount == 1 ? "" : "s") + ")"
                : "View Cart";
        btnViewCart.setText(cartLabel);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        lblTitle    = new javax.swing.JLabel();
        lblWelcome  = new javax.swing.JLabel();
        btnViewMenu = new javax.swing.JButton();
        btnViewCart = new javax.swing.JButton();
        btnLogout   = new javax.swing.JButton();
        btnMyOrders = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        // ── Header ──────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppTheme.BG_HEADER);
        header.setBorder(javax.swing.BorderFactory.createEmptyBorder(AppTheme.PAD_MD, AppTheme.PAD_LG, AppTheme.PAD_MD, AppTheme.PAD_LG));

        lblTitle.setText("CITY BITES");
        lblTitle.setFont(AppTheme.FONT_LOGO);
        lblTitle.setForeground(AppTheme.BRAND_ACCENT);

        lblWelcome.setText("Welcome!");
        lblWelcome.setFont(AppTheme.FONT_BODY);
        lblWelcome.setForeground(AppTheme.TEXT_WHITE);

        JPanel headerText = new JPanel();
        headerText.setBackground(AppTheme.BG_HEADER);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.add(lblTitle);
        headerText.add(lblWelcome);
        header.add(headerText, BorderLayout.WEST);

        // ── Navigation card ──────────────────────────────────────
        btnViewMenu = AppTheme.wideBtn("Browse Food Menu",  AppTheme.BRAND_PRIMARY);
        btnMyOrders = AppTheme.wideBtn("My Orders",         AppTheme.BRAND_SECONDARY);
        btnViewCart = AppTheme.wideBtn("View Cart",         AppTheme.BRAND_ACCENT);
        btnLogout   = AppTheme.secondaryBtn("Logout");

        btnViewMenu.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnMyOrders.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnViewCart.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogout.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnViewMenu.addActionListener(this::btnViewMenuActionPerformed);
        btnMyOrders.addActionListener(this::btnMyOrdersActionPerformed);
        btnViewCart.addActionListener(this::btnViewCartActionPerformed);
        btnLogout.addActionListener(this::btnLogoutActionPerformed);

        JPanel card = new JPanel();
        card.setBackground(AppTheme.BG_CARD);
        card.setBorder(AppTheme.cardBorder());
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel cardTitle = new JLabel("What would you like to do?");
        cardTitle.setFont(AppTheme.FONT_HEADING);
        cardTitle.setForeground(AppTheme.TEXT_PRIMARY);
        cardTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(Box.createVerticalStrut(14));
        card.add(cardTitle);
        card.add(Box.createVerticalStrut(24));
        card.add(btnViewMenu);
        card.add(Box.createVerticalStrut(14));
        card.add(btnMyOrders);
        card.add(Box.createVerticalStrut(14));
        card.add(btnViewCart);
        card.add(Box.createVerticalStrut(30));
        card.add(btnLogout);
        card.add(Box.createVerticalGlue());

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(AppTheme.BG_MAIN);
        center.setBorder(javax.swing.BorderFactory.createEmptyBorder(24, 80, 24, 80));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1; gbc.weighty = 1;
        center.add(card, gbc);

        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header, BorderLayout.NORTH);
        getContentPane().add(center, BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void btnLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogoutActionPerformed
        int r = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            SessionManager.logout();
            new CustomerLoginFrame().setVisible(true);
            dispose();
        }
    }//GEN-LAST:event_btnLogoutActionPerformed

    private void btnViewMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnViewMenuActionPerformed
        new FoodMenuFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnViewMenuActionPerformed

    private void btnViewCartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnViewCartActionPerformed
        new CartFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnViewCartActionPerformed

    private void btnMyOrdersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMyOrdersActionPerformed
        new CustomerOrdersFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnMyOrdersActionPerformed

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new CustomerDashboardFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnLogout;
    private javax.swing.JButton btnMyOrders;
    private javax.swing.JButton btnViewCart;
    private javax.swing.JButton btnViewMenu;
    private javax.swing.JLabel  lblTitle;
    private javax.swing.JLabel  lblWelcome;
    // End of variables declaration//GEN-END:variables
}
