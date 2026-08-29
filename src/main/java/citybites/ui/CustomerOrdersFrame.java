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
import javax.swing.table.DefaultTableModel;

public class CustomerOrdersFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(CustomerOrdersFrame.class.getName());
    private List<Order> orders;

    public CustomerOrdersFrame() {
        initComponents();
        setTitle("City Bites - My Orders");
        setSize(1050, 750);
        setLocationRelativeTo(null);
        setResizable(false);

        applyTableStyle(tblOrders);
        applyTableStyle(tblOrderItems);

        tblOrders.getColumnModel().getColumn(0).setPreferredWidth(90);
        tblOrders.getColumnModel().getColumn(1).setPreferredWidth(200);
        tblOrders.getColumnModel().getColumn(2).setPreferredWidth(130);
        tblOrders.getColumnModel().getColumn(3).setPreferredWidth(130);

        tblOrderItems.getColumnModel().getColumn(0).setPreferredWidth(380);
        tblOrderItems.getColumnModel().getColumn(1).setPreferredWidth(140);
        tblOrderItems.getColumnModel().getColumn(2).setPreferredWidth(110);
        tblOrderItems.getColumnModel().getColumn(3).setPreferredWidth(140);

        loadCustomerOrders();

        tblOrders.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) loadSelectedOrderItems();
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
            JOptionPane.showMessageDialog(this,
                    "Error loading orders: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSelectedOrderItems() {
        int selectedRow = tblOrders.getSelectedRow();
        if (selectedRow == -1) return;
        int orderId = Integer.parseInt(tblOrders.getValueAt(selectedRow, 0).toString());
        Order selectedOrder = null;
        if (orders != null) {
            for (Order o : orders) {
                if (o.getOrderId() == orderId) { selectedOrder = o; break; }
            }
        }
        if (selectedOrder == null) return;
        DefaultTableModel model = (DefaultTableModel) tblOrderItems.getModel();
        model.setRowCount(0);
        for (OrderItem item : selectedOrder.getOrderItems()) {
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
        btnBack       = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lblTitle.setText("My Orders");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(44, 62, 80));

        lblItemsTitle.setText("Selected Order Items");
        lblItemsTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblItemsTitle.setForeground(new Color(52, 73, 94));

        tblOrders.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{"Order ID", "Order Date", "Total", "Status"}
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

        btnBack.setText("Back");
        btnBack.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnBack.setPreferredSize(new Dimension(90, 30));
        btnBack.addActionListener(this::btnBackActionPerformed);

        JPanel ordersSection = new JPanel(new BorderLayout(0, 6));
        ordersSection.setBackground(Color.WHITE);
        JLabel ordersLabel = new JLabel("My Orders");
        ordersLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        ordersLabel.setForeground(new Color(52, 73, 94));
        ordersSection.add(ordersLabel,  BorderLayout.NORTH);
        ordersSection.add(jScrollPane1, BorderLayout.CENTER);

        JPanel itemsSection = new JPanel(new BorderLayout(0, 6));
        itemsSection.setBackground(Color.WHITE);
        itemsSection.add(lblItemsTitle, BorderLayout.NORTH);
        itemsSection.add(jScrollPane2,  BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, ordersSection, itemsSection);
        splitPane.setDividerLocation(320);
        splitPane.setDividerSize(8);
        splitPane.setBackground(Color.WHITE);
        splitPane.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 10));
        bottomBar.setBackground(new Color(245, 246, 248));
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 210, 220)));
        bottomBar.add(btnBack);

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 14));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 210, 220)));
        headerPanel.add(lblTitle);

        getContentPane().setBackground(Color.WHITE);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(headerPanel, BorderLayout.NORTH);
        getContentPane().add(splitPane,   BorderLayout.CENTER);
        getContentPane().add(bottomBar,   BorderLayout.SOUTH);

    }// </editor-fold>//GEN-END:initComponents

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new CustomerDashboardFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new CustomerOrdersFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBack;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel  lblItemsTitle;
    private javax.swing.JLabel  lblTitle;
    private javax.swing.JTable  tblOrderItems;
    private javax.swing.JTable  tblOrders;
    // End of variables declaration//GEN-END:variables
}
