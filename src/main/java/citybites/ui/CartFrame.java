package citybites.ui;

import citybites.data.DataStore;
import citybites.model.CartItem;
import citybites.model.Customer;
import citybites.model.Order;
import citybites.service.OrderService;
import citybites.util.SessionManager;
import java.awt.*;
import javax.swing.*;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

public class CartFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(CartFrame.class.getName());
    private int selectedFoodId = -1;

    public CartFrame() {
        initComponents();
        setTitle("City Bites - Shopping Cart");
        setSize(950, 650);
        setLocationRelativeTo(null);
        setResizable(false);

        spnQuantity.setModel(new SpinnerNumberModel(1, 1, 100, 1));
        btnUpdate.setEnabled(false);
        btnRemove.setEnabled(false);

        applyTableStyle(tblCart);

        tblCart.getColumnModel().getColumn(0).setPreferredWidth(60);
        tblCart.getColumnModel().getColumn(1).setPreferredWidth(330);
        tblCart.getColumnModel().getColumn(2).setPreferredWidth(120);
        tblCart.getColumnModel().getColumn(3).setPreferredWidth(90);
        tblCart.getColumnModel().getColumn(4).setPreferredWidth(120);

        loadCartTable();

        tblCart.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) selectCartItem();
        });
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

    private void loadCartTable() {
        DefaultTableModel model = (DefaultTableModel) tblCart.getModel();
        model.setRowCount(0);
        double total = 0;
        for (CartItem cartItem : DataStore.cartItems) {
            model.addRow(new Object[]{
                cartItem.getFoodItem().getFoodId(),
                cartItem.getFoodItem().getFoodName(),
                String.format("%.2f", cartItem.getFoodItem().getPrice()),
                cartItem.getQuantity(),
                String.format("%.2f", cartItem.getSubtotal())
            });
            total += cartItem.getSubtotal();
        }
        lblTotal.setText("Rs. " + String.format("%.2f", total));
        btnConfirmOrder.setEnabled(!DataStore.cartItems.isEmpty());
    }

    private void selectCartItem() {
        int selectedRow = tblCart.getSelectedRow();
        if (selectedRow == -1) return;
        selectedFoodId = Integer.parseInt(tblCart.getValueAt(selectedRow, 0).toString());
        int quantity   = Integer.parseInt(tblCart.getValueAt(selectedRow, 3).toString());
        spnQuantity.setValue(quantity);
        btnUpdate.setEnabled(true);
        btnRemove.setEnabled(true);
    }

    private void resetSelection() {
        selectedFoodId = -1;
        tblCart.clearSelection();
        spnQuantity.setValue(1);
        btnUpdate.setEnabled(false);
        btnRemove.setEnabled(false);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblTitle        = new javax.swing.JLabel();
        jScrollPane1    = new javax.swing.JScrollPane();
        tblCart         = new javax.swing.JTable();
        lblQuantity     = new javax.swing.JLabel();
        spnQuantity     = new javax.swing.JSpinner();
        lblTotalTitle   = new javax.swing.JLabel();
        lblTotal        = new javax.swing.JLabel();
        btnUpdate       = new javax.swing.JButton();
        btnRemove       = new javax.swing.JButton();
        btnClearCart    = new javax.swing.JButton();
        btnConfirmOrder = new javax.swing.JButton();
        btnBack         = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lblTitle.setText("Shopping Cart");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(44, 62, 80));

        tblCart.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{"ID", "Food Name", "Unit Price", "Quantity", "Subtotal"}
        ) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        });
        jScrollPane1.setViewportView(tblCart);
        jScrollPane1.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 220)));

        lblQuantity.setText("Qty:");
        lblQuantity.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        spnQuantity.setPreferredSize(new Dimension(75, 26));

        lblTotalTitle.setText("Total:");
        lblTotalTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotalTitle.setForeground(new Color(44, 62, 80));

        lblTotal.setText("Rs. 0.00");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTotal.setForeground(new Color(39, 174, 96));

        Font btnFont = new Font("Segoe UI", Font.PLAIN, 13);
        Dimension btn = new Dimension(120, 30);

        btnUpdate.setText("Update Quantity"); btnUpdate.setFont(btnFont); btnUpdate.setPreferredSize(btn);
        btnUpdate.addActionListener(this::btnUpdateActionPerformed);

        btnRemove.setText("Remove Item");     btnRemove.setFont(btnFont); btnRemove.setPreferredSize(btn);
        btnRemove.addActionListener(this::btnRemoveActionPerformed);

        btnClearCart.setText("Clear Cart");
        btnClearCart.setFont(btnFont);
        btnClearCart.setPreferredSize(new Dimension(100, 30));
        btnClearCart.addActionListener(this::btnClearCartActionPerformed);

        btnConfirmOrder.setText("Confirm Order");
        btnConfirmOrder.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnConfirmOrder.setPreferredSize(new Dimension(130, 30));
        btnConfirmOrder.addActionListener(this::btnConfirmOrderActionPerformed);

        btnBack.setText("Back");
        btnBack.setFont(btnFont);
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

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        row1.setBackground(new Color(245, 246, 248));
        row1.add(lblQuantity); row1.add(spnQuantity);
        row1.add(Box.createHorizontalStrut(6));
        row1.add(btnUpdate); row1.add(btnRemove); row1.add(btnClearCart);
        row1.add(Box.createHorizontalStrut(20));
        row1.add(lblTotalTitle); row1.add(lblTotal);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        row2.setBackground(new Color(245, 246, 248));
        row2.add(btnConfirmOrder);
        row2.add(Box.createHorizontalStrut(10));
        row2.add(btnBack);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(245, 246, 248));
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 210, 220)));
        bottomPanel.add(row1, BorderLayout.NORTH);
        bottomPanel.add(row2, BorderLayout.CENTER);

        getContentPane().setBackground(Color.WHITE);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(headerPanel, BorderLayout.NORTH);
        getContentPane().add(tablePanel,  BorderLayout.CENTER);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);

    }// </editor-fold>//GEN-END:initComponents

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
        if (selectedFoodId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a cart item.");
            return;
        }
        int quantity = (Integer) spnQuantity.getValue();
        for (CartItem item : DataStore.cartItems) {
            if (item.getFoodItem().getFoodId() == selectedFoodId) {
                item.setQuantity(quantity);
                break;
            }
        }
        JOptionPane.showMessageDialog(this, "Quantity updated successfully!");
        loadCartTable();
        resetSelection();
    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveActionPerformed
        if (selectedFoodId == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to remove.");
            return;
        }
        int response = JOptionPane.showConfirmDialog(this,
                "Remove this item from the cart?", "Confirm Remove", JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            DataStore.cartItems.removeIf(item -> item.getFoodItem().getFoodId() == selectedFoodId);
            loadCartTable();
            resetSelection();
            JOptionPane.showMessageDialog(this, "Item removed from cart.");
        }
    }//GEN-LAST:event_btnRemoveActionPerformed

    private void btnClearCartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearCartActionPerformed
        if (DataStore.cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The cart is already empty.");
            return;
        }
        int response = JOptionPane.showConfirmDialog(this,
                "Remove all items from the cart?", "Confirm Clear Cart", JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            DataStore.cartItems.clear();
            loadCartTable();
            resetSelection();
            JOptionPane.showMessageDialog(this, "Cart cleared successfully.");
        }
    }//GEN-LAST:event_btnClearCartActionPerformed

    private void btnConfirmOrderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConfirmOrderActionPerformed
        if (DataStore.cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Your cart is empty.",
                    "Empty Cart", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Customer customer = SessionManager.getLoggedInCustomer();
        if (customer == null) {
            JOptionPane.showMessageDialog(this,
                    "Please login before confirming the order.",
                    "Login Required", JOptionPane.WARNING_MESSAGE);
            new CustomerLoginFrame().setVisible(true);
            dispose();
            return;
        }

        double totalAmount = DataStore.cartItems.stream()
                .mapToDouble(CartItem::getSubtotal).sum();

        int response = JOptionPane.showConfirmDialog(this,
                "Confirm this order?\nTotal Amount: Rs. " + String.format("%.2f", totalAmount),
                "Confirm Order", JOptionPane.YES_NO_OPTION);
        if (response != JOptionPane.YES_OPTION) return;

        try {
            Order order = OrderService.placeOrder(customer, DataStore.cartItems);
            DataStore.cartItems.clear();
            JOptionPane.showMessageDialog(this,
                    "Order confirmed successfully!\nOrder ID: " + order.getOrderId()
                    + "\nTotal: Rs. " + String.format("%.2f", order.getTotalAmount()),
                    "Order Successful", JOptionPane.INFORMATION_MESSAGE);
            new CustomerDashboardFrame().setVisible(true);
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to place order: " + e.getMessage(),
                    "Order Failed", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnConfirmOrderActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new FoodMenuFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new CartFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton  btnBack;
    private javax.swing.JButton  btnClearCart;
    private javax.swing.JButton  btnConfirmOrder;
    private javax.swing.JButton  btnRemove;
    private javax.swing.JButton  btnUpdate;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel   lblQuantity;
    private javax.swing.JLabel   lblTitle;
    private javax.swing.JLabel   lblTotal;
    private javax.swing.JLabel   lblTotalTitle;
    private javax.swing.JSpinner spnQuantity;
    private javax.swing.JTable   tblCart;
    // End of variables declaration//GEN-END:variables
}
