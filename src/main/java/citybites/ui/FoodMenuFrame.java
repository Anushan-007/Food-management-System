package citybites.ui;

import citybites.data.DataStore;
import citybites.model.CartItem;
import citybites.model.FoodItem;
import citybites.service.FoodService;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

public class FoodMenuFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(FoodMenuFrame.class.getName());

    /** Cached list of available food items for this session view. */
    private List<FoodItem> availableItems;

    public FoodMenuFrame() {
        initComponents();
        setTitle("City Bites - Food Menu");
        setSize(950, 650);
        setLocationRelativeTo(null);
        setResizable(false);

        spnQuantity.setModel(new SpinnerNumberModel(1, 1, 100, 1));
        applyTableStyle(tblFoodMenu);

        tblFoodMenu.getColumnModel().getColumn(0).setPreferredWidth(60);
        tblFoodMenu.getColumnModel().getColumn(1).setPreferredWidth(430);
        tblFoodMenu.getColumnModel().getColumn(2).setPreferredWidth(130);

        loadFoodMenu();
    }

    private void applyTableStyle(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(24);
        table.setGridColor(new Color(220, 225, 230));
        table.setSelectionBackground(new Color(210, 228, 248));
        table.setSelectionForeground(new Color(30, 30, 30));
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getTableHeader().setDefaultRenderer(
                new javax.swing.table.DefaultTableCellRenderer() {
            {
                setOpaque(true);
                setBackground(new Color(52, 73, 94));
                setForeground(Color.WHITE);
                setFont(new Font("Segoe UI", Font.BOLD, 13));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(80, 100, 120)),
                        BorderFactory.createEmptyBorder(4, 8, 4, 8)));
            }
            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                setText(value != null ? value.toString() : "");
                return this;
            }
        });
    }

    private void loadFoodMenu() {
        DefaultTableModel model = (DefaultTableModel) tblFoodMenu.getModel();
        model.setRowCount(0);
        try {
            availableItems = FoodService.getAvailableFoodItems();
            for (FoodItem food : availableItems) {
                model.addRow(new Object[]{
                    food.getFoodId(),
                    food.getFoodName(),
                    String.format("%.2f", food.getPrice())
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading menu: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblTitle     = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblFoodMenu  = new javax.swing.JTable();
        lblQuantity  = new javax.swing.JLabel();
        spnQuantity  = new javax.swing.JSpinner();
        btnAddToCart = new javax.swing.JButton();
        btnViewCart  = new javax.swing.JButton();
        btnBack      = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lblTitle.setText("Available Food Menu");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(44, 62, 80));

        tblFoodMenu.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{"ID", "Food Name", "Price (Rs.)"}
        ) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        });
        jScrollPane1.setViewportView(tblFoodMenu);
        jScrollPane1.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 220)));

        lblQuantity.setText("Quantity:");
        lblQuantity.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        spnQuantity.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        spnQuantity.setPreferredSize(new Dimension(75, 26));

        btnAddToCart.setText("Add to Cart");
        btnAddToCart.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAddToCart.setPreferredSize(new Dimension(120, 30));
        btnAddToCart.addActionListener(this::btnAddToCartActionPerformed);

        btnViewCart.setText("View Cart");
        btnViewCart.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnViewCart.setPreferredSize(new Dimension(100, 30));
        btnViewCart.addActionListener(this::btnViewCartActionPerformed);

        btnBack.setText("Back");
        btnBack.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnBack.setPreferredSize(new Dimension(90, 30));
        btnBack.addActionListener(this::btnBackActionPerformed);

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 14));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 210, 220)));
        headerPanel.add(lblTitle);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);
        tablePanel.setBorder(BorderFactory.createEmptyBorder(14, 18, 10, 18));
        tablePanel.add(jScrollPane1, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 12));
        bottomPanel.setBackground(new Color(245, 246, 248));
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 210, 220)));
        bottomPanel.add(lblQuantity);
        bottomPanel.add(spnQuantity);
        bottomPanel.add(Box.createHorizontalStrut(16));
        bottomPanel.add(btnAddToCart);
        bottomPanel.add(Box.createHorizontalStrut(20));
        bottomPanel.add(btnViewCart);
        bottomPanel.add(btnBack);

        getContentPane().setBackground(Color.WHITE);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(headerPanel, BorderLayout.NORTH);
        getContentPane().add(tablePanel,  BorderLayout.CENTER);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);

    }// </editor-fold>//GEN-END:initComponents

    private void btnAddToCartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddToCartActionPerformed
        int selectedRow = tblFoodMenu.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a food item.",
                    "No Food Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int foodId   = Integer.parseInt(tblFoodMenu.getValueAt(selectedRow, 0).toString());
        int quantity = (Integer) spnQuantity.getValue();

        FoodItem selectedFood = null;
        if (availableItems != null) {
            for (FoodItem food : availableItems) {
                if (food.getFoodId() == foodId) { selectedFood = food; break; }
            }
        }
        if (selectedFood == null) {
            JOptionPane.showMessageDialog(this, "Selected food item was not found.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        CartItem existing = null;
        for (CartItem item : DataStore.cartItems) {
            if (item.getFoodItem().getFoodId() == foodId) { existing = item; break; }
        }
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            DataStore.cartItems.add(new CartItem(selectedFood, quantity));
        }

        JOptionPane.showMessageDialog(this,
                selectedFood.getFoodName() + " added to cart successfully!",
                "Added to Cart", JOptionPane.INFORMATION_MESSAGE);
        spnQuantity.setValue(1);
        tblFoodMenu.clearSelection();
    }//GEN-LAST:event_btnAddToCartActionPerformed

    private void btnViewCartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnViewCartActionPerformed
        new CartFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnViewCartActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new CustomerDashboardFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new FoodMenuFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton  btnAddToCart;
    private javax.swing.JButton  btnBack;
    private javax.swing.JButton  btnViewCart;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel   lblQuantity;
    private javax.swing.JLabel   lblTitle;
    private javax.swing.JSpinner spnQuantity;
    private javax.swing.JTable   tblFoodMenu;
    // End of variables declaration//GEN-END:variables
}
