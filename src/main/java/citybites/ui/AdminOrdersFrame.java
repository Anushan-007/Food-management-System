package citybites.ui;

import citybites.model.Order;
import citybites.model.OrderItem;
import citybites.service.OrderService;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class AdminOrdersFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(AdminOrdersFrame.class.getName());
    private int selectedOrderId = -1;
    private List<Order> orders;

    public AdminOrdersFrame() {
        initComponents();
        setTitle("City Bites - Customer Orders");
        setMinimumSize(new Dimension(980, 680));
        setSize(1100, 760);
        setLocationRelativeTo(null);
        setResizable(true);

        // Status dropdown includes all valid target statuses
        cmbStatus.setModel(new javax.swing.DefaultComboBoxModel<>(
                new String[]{"Preparing", "Ready", "Completed", "Cancelled"}));
        btnUpdateStatus.setEnabled(false);

        AppTheme.styleTable(tblOrders);
        AppTheme.styleTable(tblOrderItems);

        tblOrders.getColumnModel().getColumn(0).setPreferredWidth(80);
        tblOrders.getColumnModel().getColumn(1).setPreferredWidth(200);
        tblOrders.getColumnModel().getColumn(2).setPreferredWidth(150);
        tblOrders.getColumnModel().getColumn(3).setPreferredWidth(120);
        tblOrders.getColumnModel().getColumn(4).setPreferredWidth(110);

        tblOrderItems.getColumnModel().getColumn(0).setPreferredWidth(300);
        tblOrderItems.getColumnModel().getColumn(1).setPreferredWidth(120);
        tblOrderItems.getColumnModel().getColumn(2).setPreferredWidth(90);
        tblOrderItems.getColumnModel().getColumn(3).setPreferredWidth(120);

        // Colour-code status column
        tblOrders.getColumnModel().getColumn(4).setCellRenderer(new StatusCellRenderer());

        loadOrdersTable();

        tblOrders.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) selectOrder();
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
            JOptionPane.showMessageDialog(this, "Error loading orders: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selectOrder() {
        int row = tblOrders.getSelectedRow();
        if (row == -1) return;
        selectedOrderId = Integer.parseInt(tblOrders.getValueAt(row, 0).toString());
        Order selected = findSelectedOrder();
        if (selected == null) return;
        // Pre-select the next logical status
        String next = nextStatus(selected.getStatus());
        if (next != null) cmbStatus.setSelectedItem(next);
        loadOrderItems(selected);
        btnUpdateStatus.setEnabled(true);
        lblCurrentStatus.setText("Current status: " + selected.getStatus());
    }

    private String nextStatus(String current) {
        return switch (current) {
            case "Pending"   -> "Preparing";
            case "Preparing" -> "Ready";
            case "Ready"     -> "Completed";
            default          -> null;
        };
    }

    private Order findSelectedOrder() {
        if (orders == null) return null;
        for (Order o : orders) { if (o.getOrderId() == selectedOrderId) return o; }
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
        lblCurrentStatus = new javax.swing.JLabel();
        jScrollPane1    = new javax.swing.JScrollPane();
        tblOrders       = new javax.swing.JTable();
        lblOrderItems   = new javax.swing.JLabel();
        jScrollPane2    = new javax.swing.JScrollPane();
        tblOrderItems   = new javax.swing.JTable();
        lblStatus       = new javax.swing.JLabel();
        cmbStatus       = new javax.swing.JComboBox<>();
        btnUpdateStatus = new javax.swing.JButton();
        btnRefresh      = new javax.swing.JButton();
        btnBack         = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        // ── Header ──────────────────────────────────────────────
        JPanel header = AppTheme.headerPanel("Customer Orders");

        // ── Tables ──────────────────────────────────────────────
        tblOrders.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{"Order ID", "Customer", "Order Date", "Total (Rs.)", "Status"}
        ) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        });
        jScrollPane1.setViewportView(tblOrders);
        jScrollPane1.setBorder(javax.swing.BorderFactory.createLineBorder(AppTheme.BORDER));

        tblOrderItems.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{"Food Name", "Unit Price", "Quantity", "Subtotal"}
        ) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        });
        jScrollPane2.setViewportView(tblOrderItems);
        jScrollPane2.setBorder(javax.swing.BorderFactory.createLineBorder(AppTheme.BORDER));

        // ── Labels ──────────────────────────────────────────────
        lblOrderItems.setText("Selected Order Items");
        lblOrderItems.setFont(AppTheme.FONT_HEADING);
        lblOrderItems.setForeground(AppTheme.TEXT_PRIMARY);

        lblStatus.setText("Change Status:");
        lblStatus.setFont(AppTheme.FONT_BODY);
        lblStatus.setForeground(AppTheme.TEXT_PRIMARY);

        lblCurrentStatus.setText("Select an order above");
        lblCurrentStatus.setFont(AppTheme.FONT_SMALL);
        lblCurrentStatus.setForeground(AppTheme.TEXT_MUTED);

        // ── Buttons ─────────────────────────────────────────────
        cmbStatus.setFont(AppTheme.FONT_BODY);
        cmbStatus.setPreferredSize(new Dimension(140, 28));

        btnUpdateStatus = AppTheme.primaryBtn("Update Status");
        btnUpdateStatus.setPreferredSize(new Dimension(130, 30));
        btnUpdateStatus.addActionListener(this::btnUpdateStatusActionPerformed);

        btnRefresh = AppTheme.secondaryBtn("Refresh");
        btnRefresh.setPreferredSize(new Dimension(90, 30));
        btnRefresh.addActionListener(e -> { loadOrdersTable(); resetSelection(); });

        btnBack = AppTheme.secondaryBtn("Back");
        btnBack.setPreferredSize(new Dimension(90, 30));
        btnBack.addActionListener(this::btnBackActionPerformed);

        // ── Section labels ───────────────────────────────────────
        JLabel allOrdersLbl = new JLabel("All Orders");
        allOrdersLbl.setFont(AppTheme.FONT_HEADING);
        allOrdersLbl.setForeground(AppTheme.TEXT_PRIMARY);

        JPanel ordersSection = new JPanel(new BorderLayout(0, 6));
        ordersSection.setBackground(AppTheme.BG_CARD);
        ordersSection.add(allOrdersLbl, BorderLayout.NORTH);
        ordersSection.add(jScrollPane1, BorderLayout.CENTER);

        JPanel itemsSection = new JPanel(new BorderLayout(0, 6));
        itemsSection.setBackground(AppTheme.BG_CARD);
        itemsSection.add(lblOrderItems, BorderLayout.NORTH);
        itemsSection.add(jScrollPane2,  BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, ordersSection, itemsSection);
        split.setDividerLocation(310);
        split.setDividerSize(6);
        split.setBackground(AppTheme.BG_CARD);
        split.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 18, 0, 18));

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(AppTheme.BG_MAIN);
        contentPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 0, 0, 0));
        contentPanel.add(split, BorderLayout.CENTER);

        // ── Status bar ───────────────────────────────────────────
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        statusBar.setBackground(AppTheme.BG_FOOTER);
        statusBar.setBorder(AppTheme.footerBorder());
        statusBar.add(lblCurrentStatus);
        statusBar.add(Box.createHorizontalStrut(16));
        statusBar.add(lblStatus);
        statusBar.add(cmbStatus);
        statusBar.add(btnUpdateStatus);
        statusBar.add(Box.createHorizontalStrut(10));
        statusBar.add(btnRefresh);
        statusBar.add(Box.createHorizontalStrut(10));
        statusBar.add(btnBack);

        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header,       BorderLayout.NORTH);
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        getContentPane().add(statusBar,    BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    private void btnUpdateStatusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateStatusActionPerformed
        if (selectedOrderId == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order.",
                    "No Order Selected", JOptionPane.WARNING_MESSAGE); return;
        }
        String newStatus = cmbStatus.getSelectedItem().toString();
        try {
            OrderService.updateOrderStatus(selectedOrderId, newStatus);
            JOptionPane.showMessageDialog(this, "Order #" + selectedOrderId + " updated to: " + newStatus);
            loadOrdersTable();
            resetSelection();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Cannot update: " + e.getMessage(),
                    "Status Update Failed", JOptionPane.ERROR_MESSAGE);
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
        cmbStatus.setSelectedIndex(0);
        btnUpdateStatus.setEnabled(false);
        lblCurrentStatus.setText("Select an order above");
    }

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new AdminOrdersFrame().setVisible(true));
    }

    /** Renders the Status column with AppTheme colours. */
    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected && value != null) {
                setForeground(AppTheme.statusColor(value.toString()));
                setFont(AppTheme.FONT_SUBHEAD);
            }
            return this;
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton   btnBack;
    private javax.swing.JButton   btnUpdateStatus;
    private javax.swing.JButton   btnRefresh;
    private javax.swing.JComboBox<String> cmbStatus;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel    lblOrderItems;
    private javax.swing.JLabel    lblStatus;
    private javax.swing.JLabel    lblTitle;
    private javax.swing.JLabel    lblCurrentStatus;
    private javax.swing.JTable    tblOrderItems;
    private javax.swing.JTable    tblOrders;
    // End of variables declaration//GEN-END:variables
}
