package citybites.ui;

import citybites.model.Customer;
import citybites.model.Order;
import citybites.model.OrderItem;
import citybites.service.OrderService;
import citybites.util.SessionManager;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class CustomerOrdersFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(CustomerOrdersFrame.class.getName());

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    private DefaultTableModel ordersModel;
    private DefaultTableModel itemsModel;
    private List<Order> cachedOrders = new ArrayList<>();

    public CustomerOrdersFrame() {
        initComponents();
        setTitle("City Bites - My Orders");
        setMinimumSize(new Dimension(860, 580));
        setSize(1060, 680);
        setLocationRelativeTo(null);
        setResizable(true);
        loadOrders();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        tblOrders = new javax.swing.JTable();
        tblItems  = new javax.swing.JTable();
        btnBack   = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        // ── Navigation header ────────────────────────────────────────
        JButton refreshBtn = AppTheme.secondaryBtn("Refresh");
        refreshBtn.addActionListener(e -> loadOrders());

        btnBack = AppTheme.ghostBtn("← Back");
        btnBack.setForeground(new Color(150, 170, 190));
        btnBack.addActionListener(this::btnBackActionPerformed);

        Customer customer = SessionManager.getLoggedInCustomer();
        String userLabel  = (customer != null) ? customer.getFullName() : null;
        JPanel header = AppTheme.navHeader("My Orders", userLabel, refreshBtn, btnBack);

        // ── Orders table ─────────────────────────────────────────────
        String[] orderCols = {"ID", "Date & Time", "Total (Rs.)", "Status"};
        ordersModel = new DefaultTableModel(orderCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblOrders = new javax.swing.JTable(ordersModel);
        AppTheme.styleTable(tblOrders);
        tblOrders.getColumnModel().getColumn(0).setMaxWidth(60);
        tblOrders.getColumnModel().getColumn(1).setPreferredWidth(160);
        tblOrders.getColumnModel().getColumn(2).setPreferredWidth(120);
        tblOrders.getColumnModel().getColumn(3).setPreferredWidth(120);
        tblOrders.getColumnModel().getColumn(3).setCellRenderer(new AppTheme.StatusCellRenderer());
        tblOrders.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) orderRowSelected();
        });
        // Hide internal ID column from view; cachedOrders.get(row) still works for item lookup
        tblOrders.getColumnModel().removeColumn(tblOrders.getColumnModel().getColumn(0));

        JScrollPane ordersScroll = new JScrollPane(tblOrders);
        ordersScroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        ordersScroll.getViewport().setBackground(AppTheme.BG_CARD);

        JLabel ordersTitle = new JLabel("Order History");
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
        itemsScroll.setPreferredSize(new Dimension(0, 200));

        JLabel itemsTitle = new JLabel("Items in Selected Order");
        itemsTitle.setFont(AppTheme.FONT_SUBHEAD);
        itemsTitle.setForeground(AppTheme.TEXT_PRIMARY);
        itemsTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JPanel detailPanel = new JPanel(new BorderLayout(0, 6));
        detailPanel.setBackground(AppTheme.BG_MAIN);
        detailPanel.add(itemsTitle,  BorderLayout.NORTH);
        detailPanel.add(itemsScroll, BorderLayout.CENTER);

        // ── Split pane ───────────────────────────────────────────────
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, ordersPanel, detailPanel);
        split.setDividerLocation(300);
        split.setResizeWeight(0.55);
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

    private void loadOrders() {
        ordersModel.setRowCount(0);
        itemsModel.setRowCount(0);
        cachedOrders.clear();

        Customer customer = SessionManager.getLoggedInCustomer();
        if (customer == null) {
            showEmptyState("Not logged in");
            return;
        }
        try {
            cachedOrders = OrderService.getOrdersByCustomer(customer.getCustomerId());
            if (cachedOrders.isEmpty()) {
                showEmptyState("No orders yet");
                return;
            }
            for (Order o : cachedOrders) {
                String dateStr = (o.getOrderDate() != null)
                        ? o.getOrderDate().format(DT_FMT) : "—";
                ordersModel.addRow(new Object[]{
                    o.getOrderId(),
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

    private void showEmptyState(String reason) {
        // Show an info row so the table isn't just blank
        ordersModel.addRow(new Object[]{"—", reason, "—", "—"});
    }

    private void orderRowSelected() {
        int row = tblOrders.getSelectedRow();
        if (row < 0 || row >= cachedOrders.size()) return;
        Order order = cachedOrders.get(row);
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

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new CustomerDashboardFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new CustomerOrdersFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable tblOrders;
    private javax.swing.JTable tblItems;
    private javax.swing.JButton btnBack;
    // End of variables declaration//GEN-END:variables
}
