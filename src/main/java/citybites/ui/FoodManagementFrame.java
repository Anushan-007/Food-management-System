package citybites.ui;

import citybites.model.FoodItem;
import citybites.service.FoodService;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class FoodManagementFrame extends javax.swing.JFrame {

    private int selectedFoodId = -1;
    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(FoodManagementFrame.class.getName());

    public FoodManagementFrame() {
        initComponents();
        setTitle("City Bites - Food Management");
        setSize(950, 650);
        setLocationRelativeTo(null);
        setResizable(false);

        chkAvailable.setSelected(true);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);

        applyTableStyle(jTable1);

        jTable1.getColumnModel().getColumn(0).setPreferredWidth(60);
        jTable1.getColumnModel().getColumn(1).setPreferredWidth(310);
        jTable1.getColumnModel().getColumn(2).setPreferredWidth(130);
        jTable1.getColumnModel().getColumn(3).setPreferredWidth(110);

        loadFoodTable();

        jTable1.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) selectFoodFromTable();
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

    private void loadFoodTable() {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        try {
            for (FoodItem food : FoodService.getAllFoodItems()) {
                model.addRow(new Object[]{
                    food.getFoodId(),
                    food.getFoodName(),
                    String.format("%.2f", food.getPrice()),
                    food.isAvailable() ? "Yes" : "No"
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading food items: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selectFoodFromTable() {
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow == -1) return;
        selectedFoodId = Integer.parseInt(jTable1.getValueAt(selectedRow, 0).toString());
        txtFoodName.setText(jTable1.getValueAt(selectedRow, 1).toString());
        txtPrice.setText(jTable1.getValueAt(selectedRow, 2).toString());
        chkAvailable.setSelected(jTable1.getValueAt(selectedRow, 3).toString().equalsIgnoreCase("Yes"));
        btnAdd.setEnabled(false);
        btnUpdate.setEnabled(true);
        btnDelete.setEnabled(true);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblTitle     = new javax.swing.JLabel();
        lblFoodName  = new javax.swing.JLabel();
        txtFoodName  = new javax.swing.JTextField();
        lblPrice     = new javax.swing.JLabel();
        txtPrice     = new javax.swing.JTextField();
        chkAvailable = new javax.swing.JCheckBox();
        btnAdd       = new javax.swing.JButton();
        btnUpdate    = new javax.swing.JButton();
        btnDelete    = new javax.swing.JButton();
        btnClear     = new javax.swing.JButton();
        btnBack      = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1      = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lblTitle.setText("Food Management");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(44, 62, 80));

        Font labelFont = new Font("Segoe UI", Font.PLAIN, 13);
        lblFoodName.setText("Food Name:"); lblFoodName.setFont(labelFont);
        lblPrice.setText("Price (Rs.):"); lblPrice.setFont(labelFont);

        txtFoodName.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtFoodName.setPreferredSize(new Dimension(230, 26));
        txtPrice.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtPrice.setPreferredSize(new Dimension(120, 26));

        chkAvailable.setSelected(true);
        chkAvailable.setText("Available");
        chkAvailable.setFont(labelFont);
        chkAvailable.setBackground(Color.WHITE);

        Font btnFont = new Font("Segoe UI", Font.BOLD, 12);
        Dimension btnSize = new Dimension(90, 28);

        btnAdd.setText("Add");       btnAdd.setFont(btnFont);    btnAdd.setPreferredSize(btnSize);
        btnAdd.addActionListener(this::btnAddActionPerformed);

        btnUpdate.setText("Update"); btnUpdate.setFont(btnFont); btnUpdate.setPreferredSize(btnSize);
        btnUpdate.addActionListener(this::btnUpdateActionPerformed);

        btnDelete.setText("Delete"); btnDelete.setFont(btnFont); btnDelete.setPreferredSize(btnSize);
        btnDelete.addActionListener(this::btnDeleteActionPerformed);

        btnClear.setText("Clear");
        btnClear.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClear.setPreferredSize(btnSize);
        btnClear.addActionListener(this::btnClearActionPerformed);

        btnBack.setText("Back");
        btnBack.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnBack.setPreferredSize(btnSize);
        btnBack.addActionListener(this::btnBackActionPerformed);

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{"ID", "Food Name", "Price", "Available"}
        ) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        });
        jTable1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jTable1);
        jScrollPane1.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 220)));

        JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        formPanel.setBackground(Color.WHITE);
        formPanel.add(lblFoodName); formPanel.add(txtFoodName);
        formPanel.add(Box.createHorizontalStrut(10));
        formPanel.add(lblPrice);    formPanel.add(txtPrice);
        formPanel.add(Box.createHorizontalStrut(10));
        formPanel.add(chkAvailable);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.add(btnAdd); btnPanel.add(btnUpdate); btnPanel.add(btnDelete);
        btnPanel.add(btnClear); btnPanel.add(btnBack);

        JPanel topPanel = new JPanel();
        topPanel.setBackground(Color.WHITE);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBorder(BorderFactory.createEmptyBorder(18, 20, 12, 20));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        topPanel.add(lblTitle);
        topPanel.add(Box.createVerticalStrut(16));
        topPanel.add(formPanel);
        topPanel.add(Box.createVerticalStrut(12));
        topPanel.add(btnPanel);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);
        tablePanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        tablePanel.add(jScrollPane1, BorderLayout.CENTER);

        getContentPane().setBackground(Color.WHITE);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(topPanel,   BorderLayout.NORTH);
        getContentPane().add(tablePanel, BorderLayout.CENTER);

    }// </editor-fold>//GEN-END:initComponents

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        String foodName  = txtFoodName.getText().trim();
        String priceText = txtPrice.getText().trim();
        if (foodName.isEmpty() || priceText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter food name and price.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        double price;
        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Price must be a valid number.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtPrice.requestFocus();
            return;
        }
        if (price <= 0) {
            JOptionPane.showMessageDialog(this, "Price must be greater than zero.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            FoodService.addFoodItem(foodName, price, chkAvailable.isSelected(), null);
            JOptionPane.showMessageDialog(this, "Food item added successfully!");
            loadFoodTable();
            clearForm();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnAddActionPerformed

    private void clearForm() {
        selectedFoodId = -1;
        txtFoodName.setText("");
        txtPrice.setText("");
        chkAvailable.setSelected(true);
        jTable1.clearSelection();
        btnAdd.setEnabled(true);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);
        txtFoodName.requestFocus();
    }

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        clearForm();
    }//GEN-LAST:event_btnClearActionPerformed

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
        if (selectedFoodId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a food item.");
            return;
        }
        String foodName  = txtFoodName.getText().trim();
        String priceText = txtPrice.getText().trim();
        if (foodName.isEmpty() || priceText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter food name and price.");
            return;
        }
        double price;
        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Price must be a valid number.");
            return;
        }
        if (price <= 0) {
            JOptionPane.showMessageDialog(this, "Price must be greater than zero.");
            return;
        }
        try {
            FoodService.updateFoodItem(selectedFoodId, foodName, price, chkAvailable.isSelected(), null);
            JOptionPane.showMessageDialog(this, "Food item updated successfully!");
            loadFoodTable();
            clearForm();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        if (selectedFoodId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a food item.");
            return;
        }
        int response = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this food item?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            try {
                FoodService.deleteFoodItem(selectedFoodId);
                JOptionPane.showMessageDialog(this, "Food item deleted successfully!");
                loadFoodTable();
                clearForm();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Cannot delete: this item is referenced by existing orders.\n" + e.getMessage(),
                        "Delete Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_btnDeleteActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new AdminDashboardFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new FoodManagementFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton   btnAdd;
    private javax.swing.JButton   btnBack;
    private javax.swing.JButton   btnClear;
    private javax.swing.JButton   btnDelete;
    private javax.swing.JButton   btnUpdate;
    private javax.swing.JCheckBox chkAvailable;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable    jTable1;
    private javax.swing.JLabel    lblFoodName;
    private javax.swing.JLabel    lblPrice;
    private javax.swing.JLabel    lblTitle;
    private javax.swing.JTextField txtFoodName;
    private javax.swing.JTextField txtPrice;
    // End of variables declaration//GEN-END:variables
}
