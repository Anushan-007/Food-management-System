/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package citybites.ui;

import citybites.data.DataStore;
import citybites.model.Order;
import citybites.model.OrderItem;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author User
 */
public class AdminOrdersFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(AdminOrdersFrame.class.getName());
    private int selectedOrderId = -1;

    /**
     * Creates new form AdminOrdersFrame
     */
    public AdminOrdersFrame() {
        initComponents();
        setTitle("City Bites - Customer Orders");
        setSize(1050, 750);
        setLocationRelativeTo(null);
        setResizable(false);

        cmbStatus.setModel(new javax.swing.DefaultComboBoxModel<>(
                new String[]{ "Pending", "Preparing", "Completed", "Cancelled" }
        ));

        btnUpdateStatus.setEnabled(false);

        applyTableStyle(tblOrders);
        applyTableStyle(tblOrderItems);

        // Orders table column widths: Order ID, Customer, Order Date, Total, Status
        tblOrders.getColumnModel().getColumn(0).setPreferredWidth(80);
        tblOrders.getColumnModel().getColumn(1).setPreferredWidth(200);
        tblOrders.getColumnModel().getColumn(2).setPreferredWidth(150);
        tblOrders.getColumnModel().getColumn(3).setPreferredWidth(120);
        tblOrders.getColumnModel().getColumn(4).setPreferredWidth(120);

        // Order items table column widths: Food Name, Unit Price, Quantity, Subtotal
        tblOrderItems.getColumnModel().getColumn(0).setPreferredWidth(320);
        tblOrderItems.getColumnModel().getColumn(1).setPreferredWidth(120);
        tblOrderItems.getColumnModel().getColumn(2).setPreferredWidth(100);
        tblOrderItems.getColumnModel().getColumn(3).setPreferredWidth(130);

        loadOrdersTable();

        tblOrders.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                selectOrder();
            }
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

        // Use a custom renderer so the header colors are always visible,
        // regardless of which Look and Feel is active. Calling setBackground /
        // setForeground on JTableHeader alone is overridden by most L&Fs.
        table.getTableHeader().setDefaultRenderer(
                new javax.swing.table.DefaultTableCellRenderer() {
            {
                setOpaque(true);
                setBackground(new Color(52, 73, 94));
                setForeground(Color.WHITE);
                setFont(new Font("Segoe UI", Font.BOLD, 13));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(80, 100, 120)),
                        BorderFactory.createEmptyBorder(4, 8, 4, 8)
                ));
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

    private void loadOrdersTable() {
        DefaultTableModel model = (DefaultTableModel) tblOrders.getModel();
        model.setRowCount(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (Order order : DataStore.orders) {
            Object[] row = {
                order.getOrderId(),
                order.getCustomer().getFullName(),
                order.getOrderDate().format(formatter),
                String.format("%.2f", order.getTotalAmount()),
                order.getStatus()
            };
            model.addRow(row);
        }
    }

    private void selectOrder() {
        int selectedRow = tblOrders.getSelectedRow();
        if (selectedRow == -1) return;
        selectedOrderId = Integer.parseInt(tblOrders.getValueAt(selectedRow, 0).toString());
        Order selectedOrder = findSelectedOrder();
        if (selectedOrder == null) return;
        cmbStatus.setSelectedItem(selectedOrder.getStatus());
        loadOrderItems(selectedOrder);
        btnUpdateStatus.setEnabled(true);
    }

    private Order findSelectedOrder() {
        for (Order order : DataStore.orders) {
            if (order.getOrderId() == selectedOrderId) return order;
        }
        return null;
    }

    private void loadOrderItems(Order selectedOrder) {
        DefaultTableModel model = (DefaultTableModel) tblOrderItems.getModel();
        model.setRowCount(0);
        for (OrderItem item : selectedOrder.getOrderItems()) {
            Object[] row = {
                item.getFoodName(),
                String.format("%.2f", item.getUnitPrice()),
                item.getQuantity(),
                String.format("%.2f", item.getSubtotal())
            };
            model.addRow(row);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblTitle       = new javax.swing.JLabel();
        jScrollPane1   = new javax.swing.JScrollPane();
        tblOrders      = new javax.swing.JTable();
        lblOrderItems  = new javax.swing.JLabel();
        jScrollPane2   = new javax.swing.JScrollPane();
        tblOrderItems  = new javax.swing.JTable();
        lblStatus      = new javax.swing.JLabel();
        cmbStatus      = new javax.swing.JComboBox<>();
        btnUpdateStatus = new javax.swing.JButton();
        btnBack        = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        // ── Title ───────────────────────────────────────────────────────────────
        lblTitle.setText("Customer Orders");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(44, 62, 80));

        // ── Section Labels ──────────────────────────────────────────────────────
        lblOrderItems.setText("Selected Order Items");
        lblOrderItems.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblOrderItems.setForeground(new Color(52, 73, 94));

        lblStatus.setText("Order Status:");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // ── Orders Table ─────────────────────────────────────────────────────────
        tblOrders.setModel(new javax.swing.table.DefaultTableModel(
            new Object[][] {},
            new String[] { "Order ID", "Customer", "Order Date", "Total", "Status" }
        ) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        });
        jScrollPane1.setViewportView(tblOrders);
        jScrollPane1.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 220)));

        // ── Order Items Table ────────────────────────────────────────────────────
        tblOrderItems.setModel(new javax.swing.table.DefaultTableModel(
            new Object[][] {},
            new String[] { "Food Name", "Unit Price", "Quantity", "Subtotal" }
        ) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        });
        jScrollPane2.setViewportView(tblOrderItems);
        jScrollPane2.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 220)));

        // ── Status Controls ──────────────────────────────────────────────────────
        cmbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbStatus.setPreferredSize(new Dimension(140, 28));
        cmbStatus.setModel(new javax.swing.DefaultComboBoxModel<>(
                new String[]{ "Pending", "Preparing", "Completed", "Cancelled" }
        ));

        btnUpdateStatus.setText("Update Status");
        btnUpdateStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnUpdateStatus.setPreferredSize(new Dimension(130, 30));
        btnUpdateStatus.addActionListener(this::btnUpdateStatusActionPerformed);

        btnBack.setText("Back");
        btnBack.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnBack.setPreferredSize(new Dimension(90, 30));
        btnBack.addActionListener(this::btnBackActionPerformed);

        // ── Orders section panel ─────────────────────────────────────────────────
        JPanel ordersSection = new JPanel(new BorderLayout(0, 6));
        ordersSection.setBackground(Color.WHITE);
        JLabel ordersLabel = new JLabel("All Orders");
        ordersLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        ordersLabel.setForeground(new Color(52, 73, 94));
        ordersSection.add(ordersLabel, BorderLayout.NORTH);
        ordersSection.add(jScrollPane1, BorderLayout.CENTER);

        // ── Order items section panel ────────────────────────────────────────────
        JPanel itemsSection = new JPanel(new BorderLayout(0, 6));
        itemsSection.setBackground(Color.WHITE);
        itemsSection.add(lblOrderItems, BorderLayout.NORTH);
        itemsSection.add(jScrollPane2, BorderLayout.CENTER);

        // ── Status bar ──────────────────────────────────────────────────────────
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        statusBar.setBackground(new Color(245, 246, 248));
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 210, 220)));
        statusBar.add(lblStatus);
        statusBar.add(cmbStatus);
        statusBar.add(Box.createHorizontalStrut(10));
        statusBar.add(btnUpdateStatus);
        statusBar.add(Box.createHorizontalStrut(20));
        statusBar.add(btnBack);

        // ── Center: split vertically for two tables ──────────────────────────────
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, ordersSection, itemsSection);
        splitPane.setDividerLocation(310);
        splitPane.setDividerSize(8);
        splitPane.setBackground(Color.WHITE);
        splitPane.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));

        // ── Header Panel ────────────────────────────────────────────────────────
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 14));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 210, 220)));
        headerPanel.add(lblTitle);

        // ── Main Layout ─────────────────────────────────────────────────────────
        getContentPane().setBackground(Color.WHITE);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(headerPanel, BorderLayout.NORTH);
        getContentPane().add(splitPane, BorderLayout.CENTER);
        getContentPane().add(statusBar, BorderLayout.SOUTH);

    }// </editor-fold>//GEN-END:initComponents

    private void btnUpdateStatusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateStatusActionPerformed
        if (selectedOrderId == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order.", "No Order Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Order selectedOrder = findSelectedOrder();
        if (selectedOrder == null) return;
        String newStatus = cmbStatus.getSelectedItem().toString();
        selectedOrder.setStatus(newStatus);
        JOptionPane.showMessageDialog(this, "Order status updated successfully!");
        loadOrdersTable();
        resetSelection();
    }//GEN-LAST:event_btnUpdateStatusActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new AdminDashboardFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    private void resetSelection() {
        selectedOrderId = -1;
        tblOrders.clearSelection();
        DefaultTableModel itemsModel = (DefaultTableModel) tblOrderItems.getModel();
        itemsModel.setRowCount(0);
        cmbStatus.setSelectedItem("Pending");
        btnUpdateStatus.setEnabled(false);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new AdminOrdersFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnUpdateStatus;
    private javax.swing.JComboBox<String> cmbStatus;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblOrderItems;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JTable tblOrderItems;
    private javax.swing.JTable tblOrders;
    // End of variables declaration//GEN-END:variables
}
