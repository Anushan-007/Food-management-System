package citybites.ui;

import citybites.data.DataStore;
import citybites.model.Customer;
import citybites.model.FoodItem;
import citybites.model.Order;
import citybites.service.FoodService;
import citybites.service.OrderService;
import citybites.util.ImageManager;
import citybites.util.SessionManager;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;

public class CustomerDashboardFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(CustomerDashboardFrame.class.getName());

    public CustomerDashboardFrame() {
        initComponents();
        setTitle("City Bites - Customer Dashboard");
        setMinimumSize(new Dimension(800, 580));
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setResizable(true);
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

        Customer customer = SessionManager.getLoggedInCustomer();
        String firstName  = customer != null ? customer.getFullName().split(" ")[0] : "there";
        String fullWelcome = customer != null
                ? "Welcome back, " + customer.getFullName() + "!"
                : "Welcome!";

        // ── Cart badge count ─────────────────────────────────────────
        int cartCount    = DataStore.cartItems.size();
        double cartTotal = DataStore.cartItems.stream()
                .mapToDouble(ci -> ci.getSubtotal()).sum();
        String cartBadge = cartCount > 0 ? "Cart (" + cartCount + ")" : "Cart";

        // ── Navigation header ────────────────────────────────────────
        btnLogout = AppTheme.ghostBtn("Logout");
        btnLogout.setForeground(new Color(150, 170, 190));
        btnLogout.addActionListener(this::btnLogoutActionPerformed);

        btnViewCart = AppTheme.primaryBtn(cartBadge);
        btnViewCart.addActionListener(this::btnViewCartActionPerformed);

        JPanel header = AppTheme.navHeader("Customer Portal", fullWelcome,
                                            btnViewCart, btnLogout);

        // ── Hero banner ──────────────────────────────────────────────
        JPanel hero = buildHeroBanner(firstName);

        // ── Quick summary cards ──────────────────────────────────────
        JPanel summaryRow = buildSummaryCards(customer, cartCount, cartTotal);

        // ── "Available Now" section ──────────────────────────────────
        JPanel availableSection = buildAvailableNowSection();

        // ── Recent order section ─────────────────────────────────────
        JPanel recentSection = buildRecentOrderSection(customer);

        // ── Navigation buttons row ───────────────────────────────────
        btnViewMenu  = AppTheme.wideBtn("Browse Food Menu", AppTheme.BRAND_ACCENT);
        btnMyOrders  = AppTheme.wideBtn("My Orders",        AppTheme.BRAND_SECONDARY);
        btnMyProfile = AppTheme.wideBtn("My Profile",       new Color(100, 160, 220));
        btnViewMenu.addActionListener(this::btnViewMenuActionPerformed);
        btnMyOrders.addActionListener(this::btnMyOrdersActionPerformed);
        btnMyProfile.addActionListener(this::btnMyProfileActionPerformed);

        JPanel navRow = new JPanel(new GridLayout(1, 3, 14, 0));
        navRow.setBackground(AppTheme.BG_MAIN);
        navRow.setBorder(BorderFactory.createEmptyBorder(
                0, AppTheme.PAD_LG, AppTheme.PAD_LG, AppTheme.PAD_LG));
        navRow.add(btnViewMenu);
        navRow.add(btnMyOrders);
        navRow.add(btnMyProfile);

        // ── Scrollable body ──────────────────────────────────────────
        JPanel body = new JPanel();
        body.setBackground(AppTheme.BG_MAIN);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(hero);
        body.add(summaryRow);
        body.add(availableSection);
        body.add(recentSection);
        body.add(navRow);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBackground(AppTheme.BG_MAIN);
        scroll.getViewport().setBackground(AppTheme.BG_MAIN);

        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header, BorderLayout.NORTH);
        getContentPane().add(scroll, BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // ── Hero banner ────────────────────────────────────────────────────────────

    private JPanel buildHeroBanner(String firstName) {
        JPanel hero = new JPanel(new BorderLayout());
        hero.setBackground(AppTheme.BG_HERO);
        hero.setBorder(BorderFactory.createEmptyBorder(32, AppTheme.PAD_XL, 32, AppTheme.PAD_XL));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JLabel greetLbl = new JLabel("Hello, " + firstName + "!");
        greetLbl.setFont(AppTheme.FONT_TITLE);
        greetLbl.setForeground(AppTheme.TEXT_WHITE);

        JLabel subLbl = new JLabel("What would you like to eat today?");
        subLbl.setFont(AppTheme.FONT_BODY);
        subLbl.setForeground(new Color(150, 170, 190));

        JPanel text = new JPanel();
        text.setBackground(AppTheme.BG_HERO);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(greetLbl);
        text.add(Box.createVerticalStrut(6));
        text.add(subLbl);

        JButton menuBtn = AppTheme.primaryBtn("Browse Menu");
        menuBtn.setPreferredSize(new Dimension(150, AppTheme.BTN_H));
        menuBtn.addActionListener(this::btnViewMenuActionPerformed);

        hero.add(text,    BorderLayout.CENTER);
        hero.add(menuBtn, BorderLayout.EAST);
        return hero;
    }

    // ── Quick summary cards ────────────────────────────────────────────────────

    private JPanel buildSummaryCards(Customer customer, int cartCount, double cartTotal) {
        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setBackground(AppTheme.BG_MAIN);
        row.setBorder(BorderFactory.createEmptyBorder(
                AppTheme.PAD_LG, AppTheme.PAD_LG, AppTheme.PAD_MD, AppTheme.PAD_LG));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        // Cart card
        String cartVal = cartCount > 0
                ? cartCount + " item" + (cartCount == 1 ? "" : "s")
                : "Empty";
        row.add(AppTheme.summaryCard("Your Cart", cartVal, AppTheme.BRAND_ACCENT));

        // Orders count and latest status
        String ordersVal = "—";
        String latestStatus = null;
        if (customer != null) {
            try {
                List<Order> orders = OrderService.getOrdersByCustomer(customer.getCustomerId());
                ordersVal = String.valueOf(orders.size());
                if (!orders.isEmpty()) {
                    latestStatus = orders.get(0).getStatus();
                }
            } catch (Exception ignored) {}
        }
        row.add(AppTheme.summaryCard("Total Orders", ordersVal, AppTheme.BRAND_SECONDARY));

        String statusVal = latestStatus != null ? latestStatus : "No orders yet";
        Color  statusClr = latestStatus != null
                ? AppTheme.statusColor(latestStatus) : AppTheme.TEXT_MUTED;
        row.add(AppTheme.summaryCard("Latest Status", statusVal, statusClr));

        return row;
    }

    // ── Available Now section ──────────────────────────────────────────────────

    private JPanel buildAvailableNowSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(AppTheme.BG_MAIN);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Title bar
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(AppTheme.BG_MAIN);
        titleBar.setBorder(BorderFactory.createEmptyBorder(
                8, AppTheme.PAD_LG, 4, AppTheme.PAD_LG));

        JLabel titleLbl = new JLabel("Available Now");
        titleLbl.setFont(AppTheme.FONT_HEADING);
        titleLbl.setForeground(AppTheme.TEXT_PRIMARY);

        JButton seeAllBtn = AppTheme.ghostBtn("See All >");
        seeAllBtn.setFont(AppTheme.FONT_SMALL);
        seeAllBtn.setForeground(AppTheme.BRAND_ACCENT);
        seeAllBtn.addActionListener(this::btnViewMenuActionPerformed);

        titleBar.add(titleLbl,  BorderLayout.WEST);
        titleBar.add(seeAllBtn, BorderLayout.EAST);

        // Food mini-cards row
        JPanel cardsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 14));
        cardsRow.setBackground(AppTheme.BG_MAIN);

        try {
            List<FoodItem> items = FoodService.getFeaturedFoodItems();
            for (FoodItem food : items) {
                cardsRow.add(buildMiniCard(food));
            }
            if (items.isEmpty()) {
                JLabel empty = new JLabel("No featured items have been configured.");
                empty.setFont(AppTheme.FONT_BODY);
                empty.setForeground(AppTheme.TEXT_MUTED);
                cardsRow.add(empty);
            }
        } catch (Exception e) {
            logger.warning("Could not load featured items: " + e.getMessage());
        }

        section.add(titleBar, BorderLayout.NORTH);
        section.add(cardsRow, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildMiniCard(FoodItem food) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(AppTheme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        card.setPreferredSize(new Dimension(190, 240));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel imgLbl = new JLabel();
        imgLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        imgLbl.setIcon(ImageManager.loadScaled(food.getImagePath(), 150, 100));
        imgLbl.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));

        JLabel nameLbl = new JLabel(food.getFoodName());
        nameLbl.setFont(AppTheme.FONT_SUBHEAD);
        nameLbl.setForeground(AppTheme.TEXT_PRIMARY);
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameLbl.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel priceLbl = new JLabel("Rs. " + String.format("%.2f", food.getPrice()));
        priceLbl.setFont(AppTheme.FONT_HEADING);
        priceLbl.setForeground(AppTheme.BRAND_ACCENT);
        priceLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton addBtn = AppTheme.primaryBtn("Add to Cart");
        addBtn.setPreferredSize(new Dimension(140, 30));
        addBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        addBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        addBtn.setEnabled(food.getStockQuantity() > 0);
        // Navigate to menu so user can set quantity properly
        addBtn.addActionListener(e -> {
            new FoodMenuFrame().setVisible(true);
            dispose();
        });

        card.add(imgLbl);
        card.add(Box.createVerticalStrut(8));
        card.add(nameLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(priceLbl);
        card.add(Box.createVerticalStrut(10));
        card.add(addBtn);
        return card;
    }

    // ── Recent order section ───────────────────────────────────────────────────

    private JPanel buildRecentOrderSection(Customer customer) {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(AppTheme.BG_MAIN);
        section.setBorder(BorderFactory.createEmptyBorder(
                8, AppTheme.PAD_LG, AppTheme.PAD_MD, AppTheme.PAD_LG));

        JLabel titleLbl = new JLabel("Recent Order");
        titleLbl.setFont(AppTheme.FONT_HEADING);
        titleLbl.setForeground(AppTheme.TEXT_PRIMARY);
        section.add(titleLbl, BorderLayout.NORTH);

        if (customer == null) {
            JLabel noOrder = new JLabel("Login to see your orders.");
            noOrder.setFont(AppTheme.FONT_BODY);
            noOrder.setForeground(AppTheme.TEXT_MUTED);
            section.add(noOrder, BorderLayout.CENTER);
            return section;
        }

        try {
            List<Order> orders = OrderService.getOrdersByCustomer(customer.getCustomerId());
            if (orders.isEmpty()) {
                JPanel empty = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
                empty.setBackground(AppTheme.BG_MAIN);
                JLabel noOrder = new JLabel("No orders yet. Start browsing the menu!  ");
                noOrder.setFont(AppTheme.FONT_BODY);
                noOrder.setForeground(AppTheme.TEXT_MUTED);
                JButton menuBtn = AppTheme.primaryBtn("Browse Menu");
                menuBtn.addActionListener(this::btnViewMenuActionPerformed);
                empty.add(noOrder);
                empty.add(menuBtn);
                section.add(empty, BorderLayout.CENTER);
                return section;
            }

            Order latest = orders.get(0);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

            JPanel orderCard = new JPanel(new BorderLayout(0, 6));
            orderCard.setBackground(AppTheme.BG_CARD);
            orderCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0,
                        AppTheme.statusColor(latest.getStatus())),
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.BORDER),
                    BorderFactory.createEmptyBorder(14, 16, 14, 16))));

            JPanel topRow = new JPanel(new BorderLayout());
            topRow.setBackground(AppTheme.BG_CARD);
            JLabel idLbl = new JLabel("Order #" + latest.getOrderId());
            idLbl.setFont(AppTheme.FONT_SUBHEAD);
            idLbl.setForeground(AppTheme.TEXT_PRIMARY);
            JLabel statusLbl = AppTheme.statusBadge(latest.getStatus());
            topRow.add(idLbl,     BorderLayout.WEST);
            topRow.add(statusLbl, BorderLayout.EAST);

            JLabel dateLbl = new JLabel(latest.getOrderDate().format(fmt));
            dateLbl.setFont(AppTheme.FONT_SMALL);
            dateLbl.setForeground(AppTheme.TEXT_MUTED);

            JPanel bottomRow = new JPanel(new BorderLayout());
            bottomRow.setBackground(AppTheme.BG_CARD);
            JLabel totalLbl = new JLabel(
                "Total: Rs. " + String.format("%.2f", latest.getTotalAmount()));
            totalLbl.setFont(AppTheme.FONT_BODY);
            totalLbl.setForeground(AppTheme.SUCCESS);
            JButton viewAllBtn = AppTheme.secondaryBtn("View All Orders");
            viewAllBtn.addActionListener(this::btnMyOrdersActionPerformed);
            bottomRow.add(totalLbl,  BorderLayout.WEST);
            bottomRow.add(viewAllBtn, BorderLayout.EAST);

            orderCard.add(topRow,    BorderLayout.NORTH);
            orderCard.add(dateLbl,   BorderLayout.CENTER);
            orderCard.add(bottomRow, BorderLayout.SOUTH);

            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setBackground(AppTheme.BG_MAIN);
            wrap.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
            wrap.add(orderCard, BorderLayout.CENTER);
            section.add(wrap, BorderLayout.CENTER);

        } catch (Exception e) {
            logger.warning("Could not load recent order: " + e.getMessage());
        }
        return section;
    }

    // ── Action handlers ────────────────────────────────────────────────────────

    private void btnLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogoutActionPerformed
        if (AppTheme.showConfirm(this, "Confirm Logout",
                "Are you sure you want to logout?")) {
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

    private void btnMyProfileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMyProfileActionPerformed
        new CustomerProfileFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnMyProfileActionPerformed

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new CustomerDashboardFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnLogout;
    private javax.swing.JButton btnMyOrders;
    private javax.swing.JButton btnMyProfile;
    private javax.swing.JButton btnViewCart;
    private javax.swing.JButton btnViewMenu;
    private javax.swing.JLabel  lblTitle;
    private javax.swing.JLabel  lblWelcome;
    // End of variables declaration//GEN-END:variables
}
