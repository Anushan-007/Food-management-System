package citybites.ui;

import citybites.model.Customer;
import citybites.model.Order;
import citybites.model.OrderItem;
import citybites.service.OrderService;
import citybites.util.SessionManager;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class CustomerOrdersFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(CustomerOrdersFrame.class.getName());
    private List<Order> orders;

    public CustomerOrdersFrame() {
        initComponents();
        setTitle("City Bites - My Orders");
        setMinimumSize(new Dimension(900, 640));
        setSize(1000, 720);
        setLocationRelativeTo(null);
        setResizable(true);

        AppTheme.styleTable(tblOrders);
        AppTheme.styleTable(tblOrderItems);

        tblOrders.getColumnModel().getColumn(0).setPreferredWidth(90);
        tblOrders.getColumnModel().getColumn(1).setPreferredWidth(200);
        tblOrders.getColumnModel().getColumn(2).setPreferredWidth(130);
        tblOrders.getColumnModel().getColumn(3).setPreferredWidth(120);

        tblOrderItems.getColumnModel().getColumn(0).setPreferredWidth(340);
        tblOrderItems.getColumnModel().getColumn(1).setPreferredWidth(130);
        tblOrderItems.getColumnModel().getColumn(2).setPreferredWidth(100);
        tblOrderItems.getColumnModel().getColumn(3).setPreferredWidth(130);

        // Colour-code status column (column 3)
        tblOrders.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());

        loadCustomerOrders();

        tblOrders.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) loadSelectedOrderItems();
        });
    }

    private void loadCustomerOrders() {
        DefaultTableModel model = (DefaultTableModel) tblOrders.getModel();
        model.setRowCount(0);
        Customer customer = SessionManager.getLoggedInCustomer();
        if (customer == null) return;
        try {
            orders = OrderService.getOrdersByCustomer(customer.getCustomerId());
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (Order order : orders) {
                model.addRow(new Object[]{
                    order.getOrderId(),
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

    private void loadSelectedOrderItems() {
        int row = tblOrders.getSelectedRow();
        if (row == -1) return;
        int orderId = Integer.parseInt(tblOrders.getValueAt(row, 0).toString());
        Order selected = null;
        if (orders != null) {
            for (Order o : orders) { if (o.getOrderId() == orderId) { selected = o; break; } }
        }
        if (selected == null) return;
        DefaultTableModel model = (DefaultTableModel) tblOrderItems.getModel();
        model.setRowCount(0);
        for (OrderItem item : selected.getOrderItems()) {
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
        lblTitle      = new javax.swing.JLabel();
        jScrollPane1  = new javax.swing.JScrollPane();
        tblOrders     = new javax.swing.JTable();
        lblItemsTitle = new javax.swing.JLabel();
        jScrollPane2  = new javax.swing.JScrollPane();
        tblOrderItems = new javax.swing.JTable();
        btnRefresh    = new javax.swing.JButton();
        btnBack       = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        // ── Header ──────────────────────────────────────────────
        JPanel header = AppTheme.headerPanel("My Orders");

        // ── Orders table ─────────────────────────────────────────
        tblOrders.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{"Order ID", "Order Date", "Total (Rs.)", "Status"}
        ) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        });
        jScrollPane1.setViewportView(tblOrders);
        jScrollPane1.setBorder(javax.swing.BorderFactory.createLineBorder(AppTheme.BORDER));

        // ── Items table ──────────────────────────────────────────
        tblOrderItems.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{"Food Name", "Unit Price", "Quantity", "Subtotal"}
        ) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        });
        jScrollPane2.setViewportView(tblOrderItems);
        jScrollPane2.setBorder(javax.swing.BorderFactory.createLineBorder(AppTheme.BORDER));

        // ── Labels ───────────────────────────────────────────────
        JLabel ordersLabel = new JLabel("My Orders");
        ordersLabel.setFont(AppTheme.FONT_HEADING);
        ordersLabel.setForeground(AppTheme.TEXT_PRIMARY);

        lblItemsTitle.setText("Order Items");
        lblItemsTitle.setFont(AppTheme.FONT_HEADING);
        lblItemsTitle.setForeground(AppTheme.TEXT_PRIMARY);

        // ── Sections ─────────────────────────────────────────────
        JPanel ordersSection = new JPanel(new BorderLayout(0, 6));
        ordersSection.setBackground(AppTheme.BG_CARD);
        ordersSection.add(ordersLabel,  BorderLayout.NORTH);
        ordersSection.add(jScrollPane1, BorderLayout.CENTER);

        JPanel itemsSection = new JPanel(new BorderLayout(0, 6));
        itemsSection.setBackground(AppTheme.BG_CARD);
        itemsSection.add(lblItemsTitle, BorderLayout.NORTH);
        itemsSection.add(jScrollPane2,  BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, ordersSection, itemsSection);
        split.setDividerLocation(320);
        split.setDividerSize(6);
        split.setBackground(AppTheme.BG_CARD);
        split.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 18, 0, 18));

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(AppTheme.BG_MAIN);
        contentPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 0, 0, 0));
        contentPanel.add(split, BorderLayout.CENTER);

        // ── Bottom bar ───────────────────────────────────────────
        btnRefresh = AppTheme.secondaryBtn("Refresh");
        btnRefresh.setPreferredSize(new Dimension(90, 30));
        btnRefresh.addActionListener(e -> loadCustomerOrders());

        btnBack = AppTheme.secondaryBtn("Back");
        btnBack.setPreferredSize(new Dimension(90, 30));
        btnBack.addActionListener(this::btnBackActionPerformed);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 10));
        bottomBar.setBackground(AppTheme.BG_FOOTER);
        bottomBar.setBorder(AppTheme.footerBorder());
        bottomBar.add(btnRefresh);
        bottomBar.add(btnBack);

        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header,       BorderLayout.NORTH);
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        getContentPane().add(bottomBar,    BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new CustomerDashboardFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new CustomerOrdersFrame().setVisible(true));
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
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel  lblItemsTitle;
    private javax.swing.JLabel  lblTitle;
    private javax.swing.JTable  tblOrderItems;
    private javax.swing.JTable  tblOrders;
    // End of variables declaration//GEN-END:variables
}
