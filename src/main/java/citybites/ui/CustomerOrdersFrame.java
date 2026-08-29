/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package citybites.ui;

import citybites.data.DataStore;
import citybites.model.Customer;
import citybites.model.Order;
import citybites.model.OrderItem;
import citybites.util.SessionManager;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author User
 */
public class CustomerOrdersFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(CustomerOrdersFrame.class.getName());

    /**
     * Creates new form CustomerOrdersFrame
     */
    public CustomerOrdersFrame() {
        initComponents();
        setTitle("City Bites - My Orders");
        setSize(1050, 750);
        setLocationRelativeTo(null);
        setResizable(false);

        applyTableStyle(tblOrders);
        applyTableStyle(tblOrderItems);

        // Orders table column widths: Order ID, Order Date, Total, Status
        tblOrders.getColumnModel().getColumn(0).setPreferredWidth(90);
        tblOrders.getColumnModel().getColumn(1).setPreferredWidth(200);
        tblOrders.getColumnModel().getColumn(2).setPreferredWidth(130);
        tblOrders.getColumnModel().getColumn(3).setPreferredWidth(130);

        // Order items column widths: Food Name, Unit Price, Quantity, Subtotal
        tblOrderItems.getColumnModel().getColumn(0).setPreferredWidth(380);
        tblOrderItems.getColumnModel().getColumn(1).setPreferredWidth(140);
        tblOrderItems.getColumnModel().getColumn(2).setPreferredWidth(110);
        tblOrderItems.getColumnModel().getColumn(3).setPreferredWidth(140);

        loadCustomerOrders();

        tblOrders.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                loadSelectedOrderItems();
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

    private void loadCustomerOrders() {
        DefaultTableModel model = (DefaultTableModel) tblOrders.getModel();
        model.setRowCount(0);
        Customer customer = SessionManager.getLoggedInCustomer();
        if (customer == null) return;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (Order order : DataStore.orders) {
            if (order.getCustomer().getCustomerId() != customer.getCustomerId()) continue;
            Object[] row = {
                order.getOrderId(),
                order.getOrderDate().format(formatter),
                String.format("%.2f", order.getTotalAmount()),
                order.getStatus()
            };
            model.addRow(row);
        }
    }

    private void loadSelectedOrderItems() {
        int selectedRow = tblOrders.getSelectedRow();
        if (selectedRow == -1) return;
        int orderId = Integer.parseInt(tblOrders.getValueAt(selectedRow, 0).toString());
        Order selectedOrder = null;
        for (Order order : DataStore.orders) {
            if (order.getOrderId() == orderId) {
                selectedOrder = order;
                break;
            }
        }
        if (selectedOrder == null) return;
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

        lblTitle      = new javax.swing.JLabel();
        jScrollPane1  = new javax.swing.JScrollPane();
        tblOrders     = new javax.swing.JTable();
        lblItemsTitle = new javax.swing.JLabel();
        jScrollPane2  = new javax.swing.JScrollPane();
        tblOrderItems = new javax.swing.JTable();
        btnBack       = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        // ── Title ───────────────────────────────────────────────────────────────
        lblTitle.setText("My Orders");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(44, 62, 80));

        // ── Section label ────────────────────────────────────────────────────────
        lblItemsTitle.setText("Selected Order Items");
        lblItemsTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblItemsTitle.setForeground(new Color(52, 73, 94));

        // ── Orders Table ─────────────────────────────────────────────────────────
        tblOrders.setModel(new javax.swing.table.DefaultTableModel(
            new Object[][] {},
            new String[] { "Order ID", "Order Date", "Total", "Status" }
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

        // ── Back button ──────────────────────────────────────────────────────────
        btnBack.setText("Back");
        btnBack.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnBack.setPreferredSize(new Dimension(90, 30));
        btnBack.addActionListener(this::btnBackActionPerformed);

        // ── Orders section panel ─────────────────────────────────────────────────
        JPanel ordersSection = new JPanel(new BorderLayout(0, 6));
        ordersSection.setBackground(Color.WHITE);
        JLabel ordersLabel = new JLabel("My Orders");
        ordersLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        ordersLabel.setForeground(new Color(52, 73, 94));
        ordersSection.add(ordersLabel, BorderLayout.NORTH);
        ordersSection.add(jScrollPane1, BorderLayout.CENTER);

        // ── Items section panel ──────────────────────────────────────────────────
        JPanel itemsSection = new JPanel(new BorderLayout(0, 6));
        itemsSection.setBackground(Color.WHITE);
        itemsSection.add(lblItemsTitle, BorderLayout.NORTH);
        itemsSection.add(jScrollPane2, BorderLayout.CENTER);

        // ── Split pane ───────────────────────────────────────────────────────────
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, ordersSection, itemsSection);
        splitPane.setDividerLocation(320);
        splitPane.setDividerSize(8);
        splitPane.setBackground(Color.WHITE);
        splitPane.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));

        // ── Bottom bar (Back button) ──────────────────────────────────────────────
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 10));
        bottomBar.setBackground(new Color(245, 246, 248));
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 210, 220)));
        bottomBar.add(btnBack);

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
        getContentPane().add(bottomBar, BorderLayout.SOUTH);

    }// </editor-fold>//GEN-END:initComponents

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new CustomerDashboardFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnBackActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new CustomerOrdersFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBack;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblItemsTitle;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JTable tblOrderItems;
    private javax.swing.JTable tblOrders;
    // End of variables declaration//GEN-END:variables
}
