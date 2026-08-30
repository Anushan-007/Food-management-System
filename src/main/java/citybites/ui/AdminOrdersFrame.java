package citybites.ui;

import citybites.model.Order;
import citybites.model.OrderItem;
import citybites.service.OrderService;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class AdminOrdersFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(AdminOrdersFrame.class.getName());

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    private DefaultTableModel ordersModel;
    private DefaultTableModel itemsModel;
    private List<Order> cachedOrders = new ArrayList<>();

    public AdminOrdersFrame() {
        initComponents();
        setTitle("City Bites - Customer Orders");
        setMinimumSize(new Dimension(1000, 620));
        setSize(1200, 720);
        setLocationRelativeTo(null);
        setResizable(true);
        loadOrdersTable();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        tblOrders       = new javax.swing.JTable();
        tblItems        = new javax.swing.JTable();
        cmbStatus       = new javax.swing.JComboBox<>();
        btnUpdateStatus = new javax.swing.JButton();
        btnBack         = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        // ── Navigation header ────────────────────────────────────────
        JButton refreshBtn = AppTheme.secondaryBtn("Refresh");
        refreshBtn.addActionListener(e -> loadOrdersTable());

        btnBack = AppTheme.ghostBtn("← Back");
        btnBack.setForeground(new Color(150, 170, 190));
        btnBack.addActionListener(this::btnBackActionPerformed);

        JPanel header = AppTheme.navHeader("Customer Orders", null, refreshBtn, btnBack);

        // ── Orders table ─────────────────────────────────────────────
        String[] orderCols = {"ID", "Customer", "Date & Time", "Total (Rs.)", "Status"};
        ordersModel = new DefaultTableModel(orderCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblOrders = new javax.swing.JTable(ordersModel);
        AppTheme.styleTable(tblOrders);
        tblOrders.getColumnModel().getColumn(0).setMaxWidth(60);
        tblOrders.getColumnModel().getColumn(2).setPreferredWidth(160);
        tblOrders.getColumnModel().getColumn(3).setPreferredWidth(120);
        tblOrders.getColumnModel().getColumn(4).setPreferredWidth(110);
        tblOrders.getColumnModel().getColumn(4).setCellRenderer(new AppTheme.StatusCellRenderer());
        tblOrders.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) orderRowSelected();
        });
        // Hide internal ID column from view; model column 0 still accessible for status updates
        tblOrders.getColumnModel().removeColumn(tblOrders.getColumnModel().getColumn(0));

        JScrollPane ordersScroll = new JScrollPane(tblOrders);
        ordersScroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        ordersScroll.getViewport().setBackground(AppTheme.BG_CARD);

        JLabel ordersTitle = new JLabel("All Orders");
        ordersTitle.setFont(AppTheme.FONT_SUBHEAD);
        ordersTitle.setForeground(AppTheme.TEXT_PRIMARY);
        ordersTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JPanel ordersPanel = new JPanel(new BorderLayout(0, 6));
        ordersPanel.setBackground(AppTheme.BG_MAIN);
        ordersPanel.add(ordersTitle,  BorderLayout.NORTH);
        ordersPanel.add(ordersScroll, BorderLayout.CENTER);

        // ── Order items table ────────────────────────────────────────
        String[] itemCols = {"Food Item", "Qty", "Unit Price (Rs.)", "Subtotal (Rs.)"};
        itemsModel = new DefaultTableModel(itemCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblItems = new javax.swing.JTable(itemsModel);
        AppTheme.styleTable(tblItems);
        tblItems.getColumnModel().getColumn(1).setMaxWidth(60);

        JScrollPane itemsScroll = new JScrollPane(tblItems);
        itemsScroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        itemsScroll.getViewport().setBackground(AppTheme.BG_CARD);
        itemsScroll.setPreferredSize(new Dimension(0, 180));

        JLabel itemsTitle = new JLabel("Order Items");
        itemsTitle.setFont(AppTheme.FONT_SUBHEAD);
        itemsTitle.setForeground(AppTheme.TEXT_PRIMARY);
        itemsTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        // ── Status update ────────────────────────────────────────────
        cmbStatus = new javax.swing.JComboBox<>(
                new String[]{"Pending", "Processing", "Ready", "Delivered", "Cancelled"});
        cmbStatus.setFont(AppTheme.FONT_BODY);
        cmbStatus.setPreferredSize(new Dimension(160, AppTheme.BTN_H));

        btnUpdateStatus = AppTheme.primaryBtn("Update Status");
        btnUpdateStatus.addActionListener(this::btnUpdateStatusActionPerformed);
        btnUpdateStatus.setPreferredSize(new Dimension(160, AppTheme.BTN_H));

        JLabel statusLbl = new JLabel("Set Status:");
        statusLbl.setFont(AppTheme.FONT_BODY);
        statusLbl.setForeground(AppTheme.TEXT_PRIMARY);

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        statusBar.setBackground(AppTheme.BG_MAIN);
        statusBar.add(statusLbl);
        statusBar.add(cmbStatus);
        statusBar.add(btnUpdateStatus);

        JPanel detailPanel = new JPanel(new BorderLayout(0, 8));
        detailPanel.setBackground(AppTheme.BG_MAIN);
        detailPanel.add(itemsTitle,  BorderLayout.NORTH);
        detailPanel.add(itemsScroll, BorderLayout.CENTER);
        detailPanel.add(statusBar,   BorderLayout.SOUTH);

        // ── Split pane ───────────────────────────────────────────────
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, ordersPanel, detailPanel);
        split.setDividerLocation(320);
        split.setResizeWeight(0.6);
        split.setBorder(null);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(AppTheme.BG_MAIN);
        body.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        body.add(split, BorderLayout.CENTER);

        // ── Root layout ──────────────────────────────────────────────
        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header, BorderLayout.NORTH);
        getContentPane().add(body,   BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // ── Data loading ──────────────────────────────────────────────────

    private void loadOrdersTable() {
        try {
            ordersModel.setRowCount(0);
            itemsModel.setRowCount(0);
            cachedOrders = OrderService.getAllOrders();
            for (Order o : cachedOrders) {
                String customerName = (o.getCustomer() != null)
                        ? o.getCustomer().getFullName() : "—";
                String dateStr = (o.getOrderDate() != null)
                        ? o.getOrderDate().format(DT_FMT) : "—";
                ordersModel.addRow(new Object[]{
                    o.getOrderId(),
                    customerName,
                    dateStr,
                    String.format("%.2f", o.getTotalAmount()),
                    o.getStatus()
                });
            }
        } catch (Exception e) {
            AppTheme.showError(this, "Database Error",
                    "Could not load orders: " + e.getMessage());
        }
    }

    private void orderRowSelected() {
        int row = tblOrders.getSelectedRow();
        if (row < 0 || row >= cachedOrders.size()) return;
        Order order = cachedOrders.get(row);
        cmbStatus.setSelectedItem(order.getStatus());
        itemsModel.setRowCount(0);
        for (OrderItem item : order.getOrderItems()) {
            itemsModel.addRow(new Object[]{
                item.getFoodName(),
                item.getQuantity(),
                String.format("%.2f", item.getUnitPrice()),
                String.format("%.2f", item.getSubtotal())
            });
        }
    }

    // ── Event handlers ────────────────────────────────────────────────

    private void btnUpdateStatusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateStatusActionPerformed
        int row = tblOrders.getSelectedRow();
        if (row < 0) {
            AppTheme.showWarning(this, "No Selection",
                    "Please select an order to update.");
            return;
        }
        int    orderId   = (int) ordersModel.getValueAt(row, 0);
        String newStatus = cmbStatus.getSelectedItem().toString();
        String customer  = ordersModel.getValueAt(row, 1).toString();

        if (!AppTheme.showConfirm(this, "Confirm Update",
                "Set order #" + orderId + " (" + customer + ") to \"" + newStatus + "\"?")) {
            return;
        }
        try {
            OrderService.updateOrderStatus(orderId, newStatus);
            AppTheme.showInfo(this, "Updated",
                    "Order #" + orderId + " status set to \"" + newStatus + "\".");
            loadOrdersTable();
        } catch (Exception ex) {
            AppTheme.showError(this, "Database Error",
                    "Could not update status: " + ex.getMessage());
        }
    }//GEN-LAST:event_btnUpdateStatusActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new AdminDashboardFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new AdminOrdersFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable    tblOrders;
    private javax.swing.JTable    tblItems;
    private javax.swing.JComboBox<String> cmbStatus;
    private javax.swing.JButton   btnUpdateStatus;
    private javax.swing.JButton   btnBack;
    // End of variables declaration//GEN-END:variables
}
