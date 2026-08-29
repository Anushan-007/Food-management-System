package citybites.ui;

import citybites.model.Order;
import citybites.model.OrderItem;
import citybites.service.OrderService;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class AdminOrdersFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(AdminOrdersFrame.class.getName());
    private int selectedOrderId = -1;
    private List<Order> orders;

    public AdminOrdersFrame() {
        initComponents();
        setTitle("City Bites - Customer Orders");
        setSize(1050, 750);
        setLocationRelativeTo(null);
        setResizable(false);

        cmbStatus.setModel(new javax.swing.DefaultComboBoxModel<>(
                new String[]{"Pending", "Preparing", "Completed", "Cancelled"}));
        btnUpdateStatus.setEnabled(false);

        applyTableStyle(tblOrders);
        applyTableStyle(tblOrderItems);

        tblOrders.getColumnModel().getColumn(0).setPreferredWidth(80);
        tblOrders.getColumnModel().getColumn(1).setPreferredWidth(200);
        tblOrders.getColumnModel().getColumn(2).setPreferredWidth(150);
        tblOrders.getColumnModel().getColumn(3).setPreferredWidth(120);
        tblOrders.getColumnModel().getColumn(4).setPreferredWidth(120);

        tblOrderItems.getColumnModel().getColumn(0).setPreferredWidth(320);
        tblOrderItems.getColumnModel().getColumn(1).setPreferredWidth(120);
        tblOrderItems.getColumnModel().getColumn(2).setPreferredWidth(100);
        tblOrderItems.getColumnModel().getColumn(3).setPreferredWidth(130);

        loadOrdersTable();

        tblOrders.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) selectOrder();
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

    private void loadOrdersTable() {
        DefaultTableModel model = (DefaultTableModel) tblOrders.getModel();
        model.setRowCount(0);
        try {
            orders = OrderService.getAllOrders();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (Order order : orders) {
                model.addRow(new Object[]{
                    order.getOrderId(),
                    order.getCustomer().getFullName(),
                    order.getOrderDate().format(fmt),
                    String.format("%.2f", order.getTotalAmount()),
                    order.getStatus()
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading orders: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
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
        if (orders == null) return null;
        for (Order o : orders) {
            if (o.getOrderId() == selectedOrderId) return o;
        }
        return null;
    }

    private void loadOrderItems(Order order) {
        DefaultTableModel model = (DefaultTableModel) tblOrderItems.getModel();
        model.setRowCount(0);
        for (OrderItem item : order.getOrderItems()) {
            model.addRow(new Object[]{
                item.getFoodName(),
                String.format("%.2f", item.getUnitPrice()),
                item.getQuantity(),
                String.format("%.2f", item.getSubtotal())
            });
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblTitle        = new javax.swing.JLabel();
        jScrollPane1    = new javax.swing.JScrollPane();
        tblOrders       = new javax.swing.JTable();
        lblOrderItems   = new javax.swing.JLabel();
        jScrollPane2    = new javax.swing.JScrollPane();
        tblOrderItems   = new javax.swing.JTable();
        lblStatus       = new javax.swing.JLabel();
        cmbStatus       = new javax.swing.JComboBox<>();
        btnUpdateStatus = new javax.swing.JButton();
        btnBack         = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lblTitle.setText("Customer Orders");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(44, 62, 80));

        lblOrderItems.setText("Selected Order Items");
        lblOrderItems.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblOrderItems.setForeground(new Color(52, 73, 94));

        lblStatus.setText("Order Status:");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        tblOrders.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{"Order ID", "Customer", "Order Date", "Total", "Status"}
        ) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        });
        jScrollPane1.setViewportView(tblOrders);
        jScrollPane1.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 220)));

        tblOrderItems.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{"Food Name", "Unit Price", "Quantity", "Subtotal"}
        ) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        });
        jScrollPane2.setViewportView(tblOrderItems);
        jScrollPane2.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 220)));

        cmbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbStatus.setPreferredSize(new Dimension(140, 28));
        cmbStatus.setModel(new javax.swing.DefaultComboBoxModel<>(
                new String[]{"Pending", "Preparing", "Completed", "Cancelled"}));

        btnUpdateStatus.setText("Update Status");
        btnUpdateStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnUpdateStatus.setPreferredSize(new Dimension(130, 30));
        btnUpdateStatus.addActionListener(this::btnUpdateStatusActionPerformed);

        btnBack.setText("Back");
        btnBack.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnBack.setPreferredSize(new Dimension(90, 30));
        btnBack.addActionListener(this::btnBackActionPerformed);

        JPanel ordersSection = new JPanel(new BorderLayout(0, 6));
        ordersSection.setBackground(Color.WHITE);
        JLabel ordersLabel = new JLabel("All Orders");
        ordersLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        ordersLabel.setForeground(new Color(52, 73, 94));
        ordersSection.add(ordersLabel,  BorderLayout.NORTH);
        ordersSection.add(jScrollPane1, BorderLayout.CENTER);

        JPanel itemsSection = new JPanel(new BorderLayout(0, 6));
        itemsSection.setBackground(Color.WHITE);
        itemsSection.add(lblOrderItems, BorderLayout.NORTH);
        itemsSection.add(jScrollPane2,  BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        statusBar.setBackground(new Color(245, 246, 248));
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 210, 220)));
        statusBar.add(lblStatus);
        statusBar.add(cmbStatus);
        statusBar.add(Box.createHorizontalStrut(10));
        statusBar.add(btnUpdateStatus);
        statusBar.add(Box.createHorizontalStrut(20));
        statusBar.add(btnBack);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, ordersSection, itemsSection);
        splitPane.setDividerLocation(310);
        splitPane.setDividerSize(8);
        splitPane.setBackground(Color.WHITE);
        splitPane.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 14));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 210, 220)));
        headerPanel.add(lblTitle);

        getContentPane().setBackground(Color.WHITE);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(headerPanel, BorderLayout.NORTH);
        getContentPane().add(splitPane,   BorderLayout.CENTER);
        getContentPane().add(statusBar,   BorderLayout.SOUTH);

    }// </editor-fold>//GEN-END:initComponents

    private void btnUpdateStatusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateStatusActionPerformed
        if (selectedOrderId == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order.",
                    "No Order Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String newStatus = cmbStatus.getSelectedItem().toString();
        try {
            OrderService.updateOrderStatus(selectedOrderId, newStatus);
            JOptionPane.showMessageDialog(this, "Order status updated successfully!");
            loadOrdersTable();
            resetSelection();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error updating status: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnUpdateStatusActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new AdminDashboardFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    private void resetSelection() {
        selectedOrderId = -1;
        tblOrders.clearSelection();
        ((DefaultTableModel) tblOrderItems.getModel()).setRowCount(0);
        cmbStatus.setSelectedItem("Pending");
        btnUpdateStatus.setEnabled(false);
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new AdminOrdersFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton   btnBack;
    private javax.swing.JButton   btnUpdateStatus;
    private javax.swing.JComboBox<String> cmbStatus;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel    lblOrderItems;
    private javax.swing.JLabel    lblStatus;
    private javax.swing.JLabel    lblTitle;
    private javax.swing.JTable    tblOrderItems;
    private javax.swing.JTable    tblOrders;
    // End of variables declaration//GEN-END:variables
}
