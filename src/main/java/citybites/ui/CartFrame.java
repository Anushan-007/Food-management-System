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
        setMinimumSize(new Dimension(860, 580));
        setSize(950, 650);
        setLocationRelativeTo(null);
        setResizable(true);

        spnQuantity.setModel(new SpinnerNumberModel(1, 1, 100, 1));
        btnUpdate.setEnabled(false);
        btnRemove.setEnabled(false);

        AppTheme.styleTable(tblCart);
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
        int row = tblCart.getSelectedRow();
        if (row == -1) return;
        selectedFoodId = Integer.parseInt(tblCart.getValueAt(row, 0).toString());
        int quantity   = Integer.parseInt(tblCart.getValueAt(row, 3).toString());
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

        // ── Header ───────────────────────────────────────────────
        JPanel header = AppTheme.headerPanel("Shopping Cart");

        // ── Table ────────────────────────────────────────────────
        tblCart.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{"ID", "Food Name", "Unit Price", "Quantity", "Subtotal"}
        ) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        });
        jScrollPane1.setViewportView(tblCart);
        jScrollPane1.setBorder(javax.swing.BorderFactory.createLineBorder(AppTheme.BORDER));

        // ── Labels ───────────────────────────────────────────────
        lblQuantity.setText("Qty:");
        lblQuantity.setFont(AppTheme.FONT_BODY);
        lblQuantity.setForeground(AppTheme.TEXT_PRIMARY);
        spnQuantity.setPreferredSize(new Dimension(75, 28));

        lblTotalTitle.setText("Total:");
        lblTotalTitle.setFont(AppTheme.FONT_SUBHEAD);
        lblTotalTitle.setForeground(AppTheme.TEXT_PRIMARY);

        lblTotal.setText("Rs. 0.00");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTotal.setForeground(AppTheme.SUCCESS);

        // ── Buttons ──────────────────────────────────────────────
        btnUpdate       = AppTheme.secondaryBtn("Update Qty");
        btnRemove       = AppTheme.dangerBtn("Remove");
        btnClearCart    = AppTheme.secondaryBtn("Clear Cart");
        btnConfirmOrder = AppTheme.successBtn("Confirm Order");
        btnBack         = AppTheme.secondaryBtn("Back");

        for (JButton b : new JButton[]{btnUpdate, btnRemove, btnClearCart}) {
            b.setPreferredSize(new Dimension(110, 30));
        }
        btnConfirmOrder.setPreferredSize(new Dimension(140, 32));
        btnBack.setPreferredSize(new Dimension(90, 30));

        btnUpdate.addActionListener(this::btnUpdateActionPerformed);
        btnRemove.addActionListener(this::btnRemoveActionPerformed);
        btnClearCart.addActionListener(this::btnClearCartActionPerformed);
        btnConfirmOrder.addActionListener(this::btnConfirmOrderActionPerformed);
        btnBack.addActionListener(this::btnBackActionPerformed);

        // ── Table panel ──────────────────────────────────────────
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(AppTheme.BG_CARD);
        tablePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 18, 10, 18));
        tablePanel.add(jScrollPane1, BorderLayout.CENTER);

        // ── Bottom: qty/edit row + total/confirm row ─────────────
        JPanel editRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        editRow.setBackground(AppTheme.BG_FOOTER);
        editRow.add(lblQuantity); editRow.add(spnQuantity);
        editRow.add(Box.createHorizontalStrut(6));
        editRow.add(btnUpdate); editRow.add(btnRemove); editRow.add(btnClearCart);

        JPanel totalRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        totalRow.setBackground(AppTheme.BG_FOOTER);
        totalRow.add(lblTotalTitle); totalRow.add(lblTotal);
        totalRow.add(Box.createHorizontalStrut(30));
        totalRow.add(btnConfirmOrder);
        totalRow.add(Box.createHorizontalStrut(10));
        totalRow.add(btnBack);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(AppTheme.BG_FOOTER);
        bottomPanel.setBorder(AppTheme.footerBorder());
        bottomPanel.add(editRow, BorderLayout.NORTH);
        bottomPanel.add(totalRow, BorderLayout.CENTER);

        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header,      BorderLayout.NORTH);
        getContentPane().add(tablePanel,  BorderLayout.CENTER);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
        if (selectedFoodId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a cart item."); return;
        }
        int quantity = (Integer) spnQuantity.getValue();
        for (CartItem item : DataStore.cartItems) {
            if (item.getFoodItem().getFoodId() == selectedFoodId) {
                item.setQuantity(quantity); break;
            }
        }
        JOptionPane.showMessageDialog(this, "Quantity updated successfully!");
        loadCartTable();
        resetSelection();
    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveActionPerformed
        if (selectedFoodId == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to remove."); return;
        }
        int r = JOptionPane.showConfirmDialog(this, "Remove this item from the cart?",
                "Confirm Remove", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            DataStore.cartItems.removeIf(item -> item.getFoodItem().getFoodId() == selectedFoodId);
            loadCartTable();
            resetSelection();
            JOptionPane.showMessageDialog(this, "Item removed from cart.");
        }
    }//GEN-LAST:event_btnRemoveActionPerformed

    private void btnClearCartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearCartActionPerformed
        if (DataStore.cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The cart is already empty."); return;
        }
        int r = JOptionPane.showConfirmDialog(this, "Remove all items from the cart?",
                "Confirm Clear Cart", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            DataStore.cartItems.clear();
            loadCartTable();
            resetSelection();
            JOptionPane.showMessageDialog(this, "Cart cleared successfully.");
        }
    }//GEN-LAST:event_btnClearCartActionPerformed

    private void btnConfirmOrderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConfirmOrderActionPerformed
        if (DataStore.cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Your cart is empty.",
                    "Empty Cart", JOptionPane.WARNING_MESSAGE); return;
        }
        Customer customer = SessionManager.getLoggedInCustomer();
        if (customer == null) {
            JOptionPane.showMessageDialog(this, "Please login before confirming the order.",
                    "Login Required", JOptionPane.WARNING_MESSAGE);
            new CustomerLoginFrame().setVisible(true); dispose(); return;
        }
        double totalAmount = DataStore.cartItems.stream().mapToDouble(CartItem::getSubtotal).sum();
        int r = JOptionPane.showConfirmDialog(this,
                "Confirm this order?\nTotal Amount: Rs. " + String.format("%.2f", totalAmount),
                "Confirm Order", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;

        try {
            Order order = OrderService.placeOrder(customer, DataStore.cartItems);
            DataStore.cartItems.clear();
            JOptionPane.showMessageDialog(this,
                    "Order placed successfully!\nOrder ID: " + order.getOrderId()
                    + "\nTotal: Rs. " + String.format("%.2f", order.getTotalAmount()),
                    "Order Successful", JOptionPane.INFORMATION_MESSAGE);
            new CustomerDashboardFrame().setVisible(true); dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to place order: " + e.getMessage(),
                    "Order Failed", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnConfirmOrderActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new FoodMenuFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    public static void main(String[] args) {
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
