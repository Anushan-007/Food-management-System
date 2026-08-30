package citybites.ui;

import citybites.data.DataStore;
import citybites.model.CartItem;
import citybites.model.Customer;
import citybites.model.Order;
import citybites.service.OrderService;
import citybites.util.ImageManager;
import citybites.util.SessionManager;
import java.awt.*;
import javax.swing.*;

/**
 * Shopping cart — two-column layout.
 * Left: scrollable item cards (image, name, price, qty stepper, subtotal, remove).
 * Right: sticky order summary (total, Confirm Order CTA, Continue Shopping, Clear Cart).
 */
public class CartFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(CartFrame.class.getName());

    private JPanel   itemsPanel;     // left column — holds CartItemCard rows
    private JLabel   lblTotal;       // right summary — grand total
    private JButton  btnConfirmOrder;
    private JButton  btnContinue;
    private JButton  btnClearCart;
    private JButton  btnBack;

    public CartFrame() {
        initComponents();
        setTitle("City Bites - Shopping Cart");
        setMinimumSize(new Dimension(900, 600));
        setSize(1080, 700);
        setLocationRelativeTo(null);
        setResizable(true);
        renderCart();
    }

    // ── Component init ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        // dummy declarations to satisfy GEN-BEGIN/END tags
        lblTotal        = new javax.swing.JLabel();
        btnConfirmOrder = new javax.swing.JButton();
        btnContinue     = new javax.swing.JButton();
        btnClearCart    = new javax.swing.JButton();
        btnBack         = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        // ── Navigation header ────────────────────────────────────────
        btnBack = AppTheme.ghostBtn("← Back");
        btnBack.setForeground(new Color(150, 170, 190));
        btnBack.addActionListener(this::btnBackActionPerformed);
        JPanel header = AppTheme.navHeader("Shopping Cart", null, btnBack);

        // ── LEFT: scrollable items panel ─────────────────────────────
        itemsPanel = new JPanel();
        itemsPanel.setBackground(AppTheme.BG_MAIN);
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setBorder(BorderFactory.createEmptyBorder(
                AppTheme.PAD_MD, AppTheme.PAD_MD, AppTheme.PAD_MD, AppTheme.PAD_MD));

        JScrollPane leftScroll = new JScrollPane(itemsPanel);
        leftScroll.setBorder(null);
        leftScroll.getVerticalScrollBar().setUnitIncrement(16);
        leftScroll.setBackground(AppTheme.BG_MAIN);
        leftScroll.getViewport().setBackground(AppTheme.BG_MAIN);

        // ── RIGHT: sticky order summary ──────────────────────────────
        JPanel summaryCard = buildSummaryPanel();

        // ── Two-column split ─────────────────────────────────────────
        JPanel bodyPanel = new JPanel(new BorderLayout(0, 0));
        bodyPanel.setBackground(AppTheme.BG_MAIN);
        bodyPanel.add(leftScroll,   BorderLayout.CENTER);
        bodyPanel.add(summaryCard,  BorderLayout.EAST);

        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header,    BorderLayout.NORTH);
        getContentPane().add(bodyPanel, BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // ── Summary panel (right column) ───────────────────────────────────────────

    private JPanel buildSummaryPanel() {
        JPanel card = new JPanel();
        card.setBackground(AppTheme.BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, AppTheme.BORDER),
            BorderFactory.createEmptyBorder(AppTheme.PAD_LG, AppTheme.PAD_LG,
                    AppTheme.PAD_LG, AppTheme.PAD_LG)));
        card.setPreferredSize(new Dimension(300, Integer.MAX_VALUE));

        JLabel titleLbl = new JLabel("Order Summary");
        titleLbl.setFont(AppTheme.FONT_HEADING);
        titleLbl.setForeground(AppTheme.TEXT_PRIMARY);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel divider = new JPanel();
        divider.setBackground(AppTheme.BORDER);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel totalTitleLbl = new JLabel("Grand Total");
        totalTitleLbl.setFont(AppTheme.FONT_LABEL);
        totalTitleLbl.setForeground(AppTheme.TEXT_MUTED);
        totalTitleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblTotal = new JLabel("Rs. 0.00");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTotal.setForeground(AppTheme.SUCCESS);
        lblTotal.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel divider2 = new JPanel();
        divider2.setBackground(AppTheme.BORDER);
        divider2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider2.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Confirm Order — full-width orange CTA
        btnConfirmOrder = AppTheme.primaryBtn("Confirm Order");
        btnConfirmOrder.setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.BTN_H));
        btnConfirmOrder.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnConfirmOrder.addActionListener(this::btnConfirmOrderActionPerformed);

        // Continue Shopping
        btnContinue = AppTheme.secondaryBtn("Continue Shopping");
        btnContinue.setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.BTN_H));
        btnContinue.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnContinue.addActionListener(this::btnBackActionPerformed);

        // Clear Cart — danger outline
        btnClearCart = AppTheme.dangerOutlineBtn("Clear Cart");
        btnClearCart.setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.BTN_H));
        btnClearCart.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnClearCart.addActionListener(this::btnClearCartActionPerformed);

        card.add(titleLbl);
        card.add(Box.createVerticalStrut(12));
        card.add(divider);
        card.add(Box.createVerticalStrut(16));
        card.add(totalTitleLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(lblTotal);
        card.add(Box.createVerticalStrut(20));
        card.add(divider2);
        card.add(Box.createVerticalStrut(20));
        card.add(btnConfirmOrder);
        card.add(Box.createVerticalStrut(10));
        card.add(btnContinue);
        card.add(Box.createVerticalStrut(10));
        card.add(btnClearCart);
        card.add(Box.createVerticalGlue());
        return card;
    }

    // ── Cart rendering ─────────────────────────────────────────────────────────

    private void renderCart() {
        itemsPanel.removeAll();

        if (DataStore.cartItems.isEmpty()) {
            // Empty state
            JPanel empty = AppTheme.emptyStatePanel("[ ]",
                    "Your cart is empty",
                    "Browse the menu and add some items!");
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            JButton browseBtn = AppTheme.primaryBtn("Browse Food Menu");
            browseBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            browseBtn.addActionListener(e -> {
                new FoodMenuFrame().setVisible(true);
                dispose();
            });
            JPanel emptyWrap = new JPanel();
            emptyWrap.setBackground(AppTheme.BG_MAIN);
            emptyWrap.setLayout(new BoxLayout(emptyWrap, BoxLayout.Y_AXIS));
            emptyWrap.add(empty);
            emptyWrap.add(browseBtn);
            emptyWrap.add(Box.createVerticalGlue());
            itemsPanel.add(emptyWrap);
            lblTotal.setText("Rs. 0.00");
            btnConfirmOrder.setEnabled(false);
            btnClearCart.setEnabled(false);
        } else {
            // Item cards
            JLabel itemsTitle = new JLabel("Cart Items (" + DataStore.cartItems.size() + ")");
            itemsTitle.setFont(AppTheme.FONT_HEADING);
            itemsTitle.setForeground(AppTheme.TEXT_PRIMARY);
            itemsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            itemsPanel.add(itemsTitle);
            itemsPanel.add(Box.createVerticalStrut(12));

            double grandTotal = 0;
            for (CartItem item : DataStore.cartItems) {
                itemsPanel.add(buildItemCard(item));
                itemsPanel.add(Box.createVerticalStrut(10));
                grandTotal += item.getSubtotal();
            }
            lblTotal.setText("Rs. " + String.format("%.2f", grandTotal));
            btnConfirmOrder.setEnabled(true);
            btnClearCart.setEnabled(true);
        }

        itemsPanel.revalidate();
        itemsPanel.repaint();
    }

    /** Builds a single CartItemCard row. */
    private JPanel buildItemCard(CartItem item) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(AppTheme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Image thumbnail
        JLabel imgLbl = new JLabel();
        imgLbl.setIcon(ImageManager.loadScaled(
                item.getFoodItem().getImagePath(), 64, 64));
        imgLbl.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        imgLbl.setPreferredSize(new Dimension(64, 64));

        // Center: name + price info
        JPanel info = new JPanel();
        info.setBackground(AppTheme.BG_CARD);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JLabel nameLbl = new JLabel(item.getFoodItem().getFoodName());
        nameLbl.setFont(AppTheme.FONT_SUBHEAD);
        nameLbl.setForeground(AppTheme.TEXT_PRIMARY);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel priceLbl = new JLabel("Rs. " + String.format("%.2f", item.getFoodItem().getPrice()) + " each");
        priceLbl.setFont(AppTheme.FONT_SMALL);
        priceLbl.setForeground(AppTheme.TEXT_MUTED);
        priceLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtotalLbl = new JLabel("Subtotal: Rs. " + String.format("%.2f", item.getSubtotal()));
        subtotalLbl.setFont(AppTheme.FONT_BODY);
        subtotalLbl.setForeground(AppTheme.BRAND_ACCENT);
        subtotalLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        info.add(nameLbl);
        info.add(Box.createVerticalStrut(4));
        info.add(priceLbl);
        info.add(Box.createVerticalStrut(4));
        info.add(subtotalLbl);

        // Right: qty stepper + remove
        JPanel right = new JPanel();
        right.setBackground(AppTheme.BG_CARD);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        // Qty stepper: [−] [qty] [+]
        JButton minusBtn = new JButton("−");
        JLabel  qtyLbl   = new JLabel(String.valueOf(item.getQuantity()),
                                       SwingConstants.CENTER);
        JButton plusBtn  = new JButton("+");

        for (JButton b : new JButton[]{minusBtn, plusBtn}) {
            b.setFont(AppTheme.FONT_SUBHEAD);
            b.setPreferredSize(new Dimension(32, 28));
            b.setMargin(new Insets(0, 0, 0, 0));
            b.setFocusPainted(false);
            b.setBackground(AppTheme.BG_FOOTER);
            b.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        }
        qtyLbl.setFont(AppTheme.FONT_SUBHEAD);
        qtyLbl.setPreferredSize(new Dimension(36, 28));
        qtyLbl.setForeground(AppTheme.TEXT_PRIMARY);

        minusBtn.addActionListener(e -> {
            int q = item.getQuantity();
            if (q > 1) {
                item.setQuantity(q - 1);
                qtyLbl.setText(String.valueOf(item.getQuantity()));
                updateSubtotal(subtotalLbl, item);
                updateTotal();
            }
        });
        plusBtn.addActionListener(e -> {
            int maxStock = item.getFoodItem().getStockQuantity();
            int q = item.getQuantity();
            if (q < maxStock) {
                item.setQuantity(q + 1);
                qtyLbl.setText(String.valueOf(item.getQuantity()));
                updateSubtotal(subtotalLbl, item);
                updateTotal();
            } else {
                AppTheme.showWarning(CartFrame.this, "Stock Limit",
                        "Only " + maxStock + " unit(s) in stock.");
            }
        });

        JPanel stepperRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        stepperRow.setBackground(AppTheme.BG_CARD);
        stepperRow.add(minusBtn);
        stepperRow.add(qtyLbl);
        stepperRow.add(plusBtn);
        stepperRow.setAlignmentX(Component.RIGHT_ALIGNMENT);

        // Remove button
        JButton removeBtn = AppTheme.dangerOutlineBtn("Remove");
        removeBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.BTN_H));
        removeBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
        removeBtn.addActionListener(e -> {
            if (AppTheme.showConfirm(CartFrame.this, "Remove Item",
                    "Remove \"" + item.getFoodItem().getFoodName() + "\" from cart?")) {
                DataStore.cartItems.remove(item);
                renderCart();
            }
        });

        right.add(stepperRow);
        right.add(Box.createVerticalStrut(6));
        right.add(removeBtn);

        card.add(imgLbl, BorderLayout.WEST);
        card.add(info,   BorderLayout.CENTER);
        card.add(right,  BorderLayout.EAST);
        return card;
    }

    private void updateSubtotal(JLabel lbl, CartItem item) {
        lbl.setText("Subtotal: Rs. " + String.format("%.2f", item.getSubtotal()));
    }

    private void updateTotal() {
        double total = DataStore.cartItems.stream().mapToDouble(CartItem::getSubtotal).sum();
        lblTotal.setText("Rs. " + String.format("%.2f", total));
    }

    // ── Action handlers ────────────────────────────────────────────────────────

    private void btnConfirmOrderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConfirmOrderActionPerformed
        if (DataStore.cartItems.isEmpty()) {
            AppTheme.showWarning(this, "Empty Cart", "Your cart is empty.");
            return;
        }
        Customer customer = SessionManager.getLoggedInCustomer();
        if (customer == null) {
            AppTheme.showWarning(this, "Login Required",
                    "Please login before confirming the order.");
            new CustomerLoginFrame().setVisible(true);
            dispose();
            return;
        }
        double total = DataStore.cartItems.stream()
                .mapToDouble(CartItem::getSubtotal).sum();
        if (!AppTheme.showConfirm(this, "Confirm Order",
                "Confirm this order?\nTotal Amount: Rs. " + String.format("%.2f", total))) {
            return;
        }

        btnConfirmOrder.setEnabled(false);
        btnConfirmOrder.setText("Placing order...");
        try {
            Order order = OrderService.placeOrder(customer, DataStore.cartItems);
            DataStore.cartItems.clear();
            AppTheme.showInfo(this, "Order Successful",
                    "Order placed successfully!\nOrder ID: " + order.getOrderId() +
                    "\nTotal: Rs. " + String.format("%.2f", order.getTotalAmount()));
            new CustomerDashboardFrame().setVisible(true);
            dispose();
        } catch (Exception e) {
            AppTheme.showError(this, "Order Failed",
                    "Failed to place order: " + e.getMessage());
            btnConfirmOrder.setEnabled(true);
            btnConfirmOrder.setText("Confirm Order");
        }
    }//GEN-LAST:event_btnConfirmOrderActionPerformed

    private void btnClearCartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearCartActionPerformed
        if (DataStore.cartItems.isEmpty()) {
            AppTheme.showInfo(this, "Cart Empty", "The cart is already empty.");
            return;
        }
        if (AppTheme.showConfirm(this, "Clear Cart",
                "Remove all items from the cart?")) {
            DataStore.cartItems.clear();
            renderCart();
        }
    }//GEN-LAST:event_btnClearCartActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new FoodMenuFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new CartFrame().setVisible(true));
    }
}
