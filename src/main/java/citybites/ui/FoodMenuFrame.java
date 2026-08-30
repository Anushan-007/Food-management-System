package citybites.ui;

import citybites.data.DataStore;
import citybites.model.CartItem;
import citybites.model.FoodItem;
import citybites.service.FoodService;
import citybites.util.ImageManager;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.SpinnerNumberModel;

public class FoodMenuFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(FoodMenuFrame.class.getName());

    private List<FoodItem> allItems      = new ArrayList<>();
    private List<FoodItem> displayedItems = new ArrayList<>();

    public FoodMenuFrame() {
        initComponents();
        setTitle("City Bites - Food Menu");
        setMinimumSize(new Dimension(860, 600));
        setSize(1020, 720);
        setLocationRelativeTo(null);
        setResizable(true);
        loadMenu(null);
    }

    private void loadMenu(String filter) {
        try {
            if (allItems.isEmpty()) {
                allItems = FoodService.getAvailableFoodItems();
            }
            displayedItems.clear();
            String q = (filter == null || filter.isBlank()) ? "" : filter.toLowerCase();
            for (FoodItem food : allItems) {
                if (q.isEmpty() || food.getFoodName().toLowerCase().contains(q)) {
                    displayedItems.add(food);
                }
            }
            renderCards();
        } catch (Exception e) {
            AppTheme.showError(this, "Database Error",
                    "Error loading menu: " + e.getMessage());
        }
    }

    private static final int MAX_COLS = 5;

    private void renderCards() {
        cardsPanel.removeAll();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        if (displayedItems.isEmpty()) {
            JPanel empty = AppTheme.emptyStatePanel("", "No items found",
                    "Try a different search term");
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            cardsPanel.add(empty);
        } else {
            for (int i = 0; i < displayedItems.size(); i += MAX_COLS) {
                JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
                row.setBackground(AppTheme.BG_MAIN);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 316));
                for (int j = i; j < Math.min(i + MAX_COLS, displayedItems.size()); j++) {
                    row.add(buildFoodCard(displayedItems.get(j)));
                }
                cardsPanel.add(row);
                cardsPanel.add(Box.createVerticalStrut(16));
            }
        }
        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private JPanel buildFoodCard(FoodItem food) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(AppTheme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER),
            BorderFactory.createEmptyBorder(10, 12, 12, 12)));
        card.setPreferredSize(new Dimension(210, 300));
        card.setMaximumSize(new Dimension(210, 300));

        // Image
        JLabel imgLabel = new JLabel();
        imgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        imgLabel.setIcon(ImageManager.loadScaled(food.getImagePath(), 170, 115));
        imgLabel.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));

        // Name
        JLabel nameLabel = new JLabel(food.getFoodName());
        nameLabel.setFont(AppTheme.FONT_SUBHEAD);
        nameLabel.setForeground(AppTheme.TEXT_PRIMARY);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Price
        JLabel priceLabel = new JLabel("Rs. " + String.format("%.2f", food.getPrice()));
        priceLabel.setFont(AppTheme.FONT_HEADING);
        priceLabel.setForeground(AppTheme.BRAND_ACCENT);
        priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Stock badge
        boolean inStock  = food.getStockQuantity() > 0;
        String stockText = inStock
                ? "In stock: " + food.getStockQuantity()
                : "Out of stock";
        JLabel stockLabel = new JLabel(stockText);
        stockLabel.setFont(AppTheme.FONT_SMALL);
        stockLabel.setForeground(inStock ? AppTheme.SUCCESS : AppTheme.DANGER);
        stockLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Quantity spinner
        JSpinner spn = new JSpinner(
            new SpinnerNumberModel(1, 1, Math.max(1, food.getStockQuantity()), 1));
        spn.setFont(AppTheme.FONT_BODY);
        spn.setMaximumSize(new Dimension(90, 28));
        spn.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add to cart button
        JButton addBtn = AppTheme.primaryBtn("Add to Cart");
        addBtn.setPreferredSize(new Dimension(160, AppTheme.BTN_H));
        addBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.BTN_H));
        addBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        addBtn.setEnabled(inStock);
        addBtn.addActionListener(e -> addToCart(food, (Integer) spn.getValue()));

        card.add(imgLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(priceLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(stockLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(spn);
        card.add(Box.createVerticalStrut(8));
        card.add(addBtn);

        return card;
    }

    private void addToCart(FoodItem food, int quantity) {
        if (food.getStockQuantity() < quantity) {
            AppTheme.showWarning(this, "Insufficient Stock",
                    "Only " + food.getStockQuantity() + " unit(s) available.");
            return;
        }
        CartItem existing = null;
        for (CartItem item : DataStore.cartItems) {
            if (item.getFoodItem().getFoodId() == food.getFoodId()) {
                existing = item; break;
            }
        }
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            DataStore.cartItems.add(new CartItem(food, quantity));
        }
        int total = DataStore.cartItems.stream().mapToInt(CartItem::getQuantity).sum();
        btnViewCart.setText("Cart (" + total + ")");

        // Non-blocking toast — replaces JOptionPane for add-to-cart feedback
        AppTheme.showToast(this,
                food.getFoodName() + " \u00d7" + quantity + " added to cart!");
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        txtSearch   = new javax.swing.JTextField();
        btnSearch   = new javax.swing.JButton();
        btnViewCart = new javax.swing.JButton();
        btnBack     = new javax.swing.JButton();
        cardsPanel  = new JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        // ── Navigation header ────────────────────────────────────────
        int cartQty = DataStore.cartItems.stream().mapToInt(CartItem::getQuantity).sum();
        btnViewCart = AppTheme.primaryBtn(cartQty > 0 ? "Cart (" + cartQty + ")" : "Cart");
        btnViewCart.addActionListener(this::btnViewCartActionPerformed);

        btnBack = AppTheme.ghostBtn("← Back");
        btnBack.setForeground(new Color(150, 170, 190));
        btnBack.addActionListener(this::btnBackActionPerformed);

        JPanel header = AppTheme.navHeader("Food Menu", null, btnViewCart, btnBack);

        // ── Search toolbar ───────────────────────────────────────────
        AppTheme.styleField(txtSearch);
        txtSearch.setPreferredSize(new Dimension(280, AppTheme.BTN_H));
        txtSearch.putClientProperty("JTextField.placeholderText", "Search food items...");

        btnSearch = AppTheme.secondaryBtn("Search");
        btnSearch.addActionListener(e -> {
            allItems.clear();   // force refresh from DB on each search
            loadMenu(txtSearch.getText());
        });
        txtSearch.addActionListener(e -> {
            allItems.clear();
            loadMenu(txtSearch.getText());
        });

        JButton clearSearchBtn = AppTheme.ghostBtn("Clear");
        clearSearchBtn.addActionListener(e -> {
            txtSearch.setText("");
            allItems.clear();
            loadMenu(null);
        });

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        toolbar.setBackground(AppTheme.BG_MAIN);
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER));
        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(AppTheme.FONT_BODY);
        searchLbl.setForeground(AppTheme.TEXT_PRIMARY);
        toolbar.add(searchLbl);
        toolbar.add(txtSearch);
        toolbar.add(btnSearch);
        toolbar.add(clearSearchBtn);

        // ── Cards scroll area ────────────────────────────────────────
        cardsPanel = new JPanel();
        cardsPanel.setBackground(AppTheme.BG_MAIN);

        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.setBackground(AppTheme.BG_MAIN);
        scroll.getViewport().setBackground(AppTheme.BG_MAIN);

        // ── Root layout ──────────────────────────────────────────────
        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(AppTheme.BG_MAIN);
        body.add(toolbar, BorderLayout.NORTH);
        body.add(scroll,  BorderLayout.CENTER);
        getContentPane().add(body, BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void btnViewCartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnViewCartActionPerformed
        new CartFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnViewCartActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new CustomerDashboardFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new FoodMenuFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton    btnSearch;
    private javax.swing.JButton    btnViewCart;
    private javax.swing.JButton    btnBack;
    private javax.swing.JTextField txtSearch;
    private JPanel                 cardsPanel;
    // End of variables declaration//GEN-END:variables

    /**
     * FlowLayout variant that wraps children to new rows as the panel resizes.
     */
    private static class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            return layoutSize(target, false);
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
                int hgap = getHgap(), vgap = getVgap();
                Insets insets  = target.getInsets();
                int maxWidth   = targetWidth - insets.left - insets.right - hgap * 2;
                int width      = 0;
                int height     = insets.top + insets.bottom + vgap * 2;
                int rowWidth   = 0, rowHeight = 0;
                int nmembers   = target.getComponentCount();
                for (int i = 0; i < nmembers; i++) {
                    Component m = target.getComponent(i);
                    if (m.isVisible()) {
                        Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                        if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                            height   += rowHeight + vgap;
                            rowWidth  = 0;
                            rowHeight = 0;
                        }
                        if (rowWidth > 0) rowWidth += hgap;
                        rowWidth  += d.width;
                        rowHeight  = Math.max(rowHeight, d.height);
                        width      = Math.max(width, rowWidth);
                    }
                }
                height += rowHeight;
                return new Dimension(
                    width + insets.left + insets.right + hgap * 2, height);
            }
        }
    }
}
