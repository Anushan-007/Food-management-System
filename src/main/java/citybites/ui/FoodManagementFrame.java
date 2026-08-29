package citybites.ui;

import citybites.model.FoodItem;
import citybites.service.FoodService;
import citybites.util.ImageManager;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class FoodManagementFrame extends javax.swing.JFrame {

    private int selectedFoodId = -1;
    private File selectedImageFile = null;
    private String currentImagePath = null;

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(FoodManagementFrame.class.getName());

    public FoodManagementFrame() {
        initComponents();
        setTitle("City Bites - Food Management");
        setMinimumSize(new Dimension(1000, 680));
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setResizable(true);

        chkAvailable.setSelected(true);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);

        AppTheme.styleTable(jTable1);
        jTable1.getColumnModel().getColumn(0).setPreferredWidth(55);
        jTable1.getColumnModel().getColumn(1).setPreferredWidth(260);
        jTable1.getColumnModel().getColumn(2).setPreferredWidth(110);
        jTable1.getColumnModel().getColumn(3).setPreferredWidth(80);
        jTable1.getColumnModel().getColumn(4).setPreferredWidth(80);
        jTable1.getColumnModel().getColumn(5).setPreferredWidth(200);

        loadFoodTable();

        jTable1.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) selectFoodFromTable();
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
                    food.getStockQuantity(),
                    food.isAvailable() ? "Yes" : "No",
                    food.getImagePath() != null ? food.getImagePath() : ""
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading food items: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selectFoodFromTable() {
        int row = jTable1.getSelectedRow();
        if (row == -1) return;
        selectedFoodId = Integer.parseInt(jTable1.getValueAt(row, 0).toString());
        txtFoodName.setText(jTable1.getValueAt(row, 1).toString());
        txtPrice.setText(jTable1.getValueAt(row, 2).toString());
        txtStock.setText(jTable1.getValueAt(row, 3).toString());
        chkAvailable.setSelected(jTable1.getValueAt(row, 4).toString().equalsIgnoreCase("Yes"));
        currentImagePath = jTable1.getValueAt(row, 5).toString();
        selectedImageFile = null;
        updateImagePreview(currentImagePath.isEmpty() ? null : currentImagePath);
        btnAdd.setEnabled(false);
        btnUpdate.setEnabled(true);
        btnDelete.setEnabled(true);
    }

    private void updateImagePreview(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            lblImagePreview.setIcon(ImageManager.placeholder(120, 90));
            lblImagePreview.setText(null);
        } else {
            lblImagePreview.setIcon(ImageManager.loadScaled(imagePath, 120, 90));
            lblImagePreview.setText(null);
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        lblTitle      = new javax.swing.JLabel();
        lblFoodName   = new javax.swing.JLabel();
        txtFoodName   = new javax.swing.JTextField();
        lblPrice      = new javax.swing.JLabel();
        txtPrice      = new javax.swing.JTextField();
        lblStock      = new javax.swing.JLabel();
        txtStock      = new javax.swing.JTextField();
        chkAvailable  = new javax.swing.JCheckBox();
        lblImagePreview = new javax.swing.JLabel();
        btnPickImage  = new javax.swing.JButton();
        btnAdd        = new javax.swing.JButton();
        btnUpdate     = new javax.swing.JButton();
        btnDelete     = new javax.swing.JButton();
        btnClear      = new javax.swing.JButton();
        btnBack       = new javax.swing.JButton();
        jScrollPane1  = new javax.swing.JScrollPane();
        jTable1       = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        // ── Top header ───────────────────────────────────────────
        JPanel header = AppTheme.headerPanel("Food Management");

        // ── Form fields ──────────────────────────────────────────
        lblFoodName.setText("Food Name:");   lblFoodName.setFont(AppTheme.FONT_LABEL); lblFoodName.setForeground(AppTheme.TEXT_PRIMARY);
        lblPrice.setText("Price (Rs.):");    lblPrice.setFont(AppTheme.FONT_LABEL);    lblPrice.setForeground(AppTheme.TEXT_PRIMARY);
        lblStock.setText("Stock Qty:");      lblStock.setFont(AppTheme.FONT_LABEL);    lblStock.setForeground(AppTheme.TEXT_PRIMARY);

        txtFoodName.setPreferredSize(new Dimension(220, 28)); AppTheme.styleField(txtFoodName);
        txtPrice.setPreferredSize(new Dimension(100, 28));    AppTheme.styleField(txtPrice);
        txtStock.setPreferredSize(new Dimension(80,  28));    AppTheme.styleField(txtStock);

        chkAvailable.setSelected(true);
        chkAvailable.setText("Available");
        chkAvailable.setFont(AppTheme.FONT_BODY);
        chkAvailable.setBackground(AppTheme.BG_CARD);
        chkAvailable.setForeground(AppTheme.TEXT_PRIMARY);

        // ── Image preview ─────────────────────────────────────────
        lblImagePreview.setPreferredSize(new Dimension(120, 90));
        lblImagePreview.setBorder(javax.swing.BorderFactory.createLineBorder(AppTheme.BORDER));
        lblImagePreview.setHorizontalAlignment(SwingConstants.CENTER);
        lblImagePreview.setIcon(ImageManager.placeholder(120, 90));

        btnPickImage = AppTheme.secondaryBtn("Choose Image");
        btnPickImage.setPreferredSize(new Dimension(120, 28));
        btnPickImage.addActionListener(this::btnPickImageActionPerformed);

        // ── Action buttons ───────────────────────────────────────
        btnAdd    = AppTheme.primaryBtn("Add");
        btnUpdate = AppTheme.successBtn("Update");
        btnDelete = AppTheme.dangerBtn("Delete");
        btnClear  = AppTheme.secondaryBtn("Clear");
        btnBack   = AppTheme.secondaryBtn("Back");

        for (JButton b : new JButton[]{btnAdd, btnUpdate, btnDelete, btnClear, btnBack}) {
            b.setPreferredSize(new Dimension(90, 30));
        }

        btnAdd.addActionListener(this::btnAddActionPerformed);
        btnUpdate.addActionListener(this::btnUpdateActionPerformed);
        btnDelete.addActionListener(this::btnDeleteActionPerformed);
        btnClear.addActionListener(this::btnClearActionPerformed);
        btnBack.addActionListener(this::btnBackActionPerformed);

        // ── Table ─────────────────────────────────────────────────
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{"ID", "Food Name", "Price", "Stock", "Available", "Image"}
        ) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        });
        jTable1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jTable1);
        jScrollPane1.setBorder(javax.swing.BorderFactory.createLineBorder(AppTheme.BORDER));

        // ── Form row 1: name, price, stock, available ──────────────
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row1.setBackground(AppTheme.BG_CARD);
        row1.add(lblFoodName); row1.add(txtFoodName);
        row1.add(Box.createHorizontalStrut(8));
        row1.add(lblPrice);    row1.add(txtPrice);
        row1.add(Box.createHorizontalStrut(8));
        row1.add(lblStock);    row1.add(txtStock);
        row1.add(Box.createHorizontalStrut(8));
        row1.add(chkAvailable);

        // ── Form row 2: image preview + pick button ─────────────────
        JPanel imageRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        imageRow.setBackground(AppTheme.BG_CARD);
        imageRow.add(new JLabel("Image:") {{ setFont(AppTheme.FONT_LABEL); setForeground(AppTheme.TEXT_PRIMARY); }});
        imageRow.add(lblImagePreview);
        imageRow.add(btnPickImage);

        // ── Button row ─────────────────────────────────────────────
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setBackground(AppTheme.BG_CARD);
        btnRow.add(btnAdd); btnRow.add(btnUpdate); btnRow.add(btnDelete);
        btnRow.add(Box.createHorizontalStrut(10));
        btnRow.add(btnClear); btnRow.add(btnBack);

        // ── Top form panel ─────────────────────────────────────────
        JPanel topPanel = new JPanel();
        topPanel.setBackground(AppTheme.BG_CARD);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER),
            javax.swing.BorderFactory.createEmptyBorder(14, 18, 12, 18)));
        topPanel.add(row1);
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(imageRow);
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(btnRow);

        // ── Table panel ───────────────────────────────────────────
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(AppTheme.BG_MAIN);
        tablePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 18, 14, 18));
        tablePanel.add(jScrollPane1, BorderLayout.CENTER);

        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header,     BorderLayout.NORTH);
        getContentPane().add(topPanel,   BorderLayout.CENTER);
        getContentPane().add(tablePanel, BorderLayout.SOUTH);

        // Give the table panel more vertical space
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header,     BorderLayout.NORTH);

        JPanel contentSplit = new JPanel(new BorderLayout());
        contentSplit.setBackground(AppTheme.BG_MAIN);
        contentSplit.add(topPanel,   BorderLayout.NORTH);
        contentSplit.add(tablePanel, BorderLayout.CENTER);
        getContentPane().add(contentSplit, BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void btnPickImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPickImageActionPerformed
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Food Image");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image Files (*.jpg, *.jpeg, *.png, *.gif)", "jpg", "jpeg", "png", "gif"));
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedImageFile = chooser.getSelectedFile();
            updateImagePreview(null);
            // Show local preview before saving
            try {
                javax.swing.ImageIcon icon = new javax.swing.ImageIcon(selectedImageFile.getAbsolutePath());
                Image img = icon.getImage().getScaledInstance(120, 90, Image.SCALE_SMOOTH);
                lblImagePreview.setIcon(new javax.swing.ImageIcon(img));
            } catch (Exception e) {
                lblImagePreview.setIcon(ImageManager.placeholder(120, 90));
            }
        }
    }//GEN-LAST:event_btnPickImageActionPerformed

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        String foodName = txtFoodName.getText().trim();
        String priceText = txtPrice.getText().trim();
        String stockText = txtStock.getText().trim();
        if (foodName.isEmpty() || priceText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter food name and price.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE); return;
        }
        double price; int stock;
        try { price = Double.parseDouble(priceText); } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Price must be a valid number.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE); return;
        }
        try { stock = stockText.isEmpty() ? 0 : Integer.parseInt(stockText); } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Stock quantity must be a whole number.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE); return;
        }
        if (price <= 0) {
            JOptionPane.showMessageDialog(this, "Price must be greater than zero.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE); return;
        }
        if (stock < 0) {
            JOptionPane.showMessageDialog(this, "Stock quantity cannot be negative.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE); return;
        }
        String imagePath = null;
        if (selectedImageFile != null) {
            try { imagePath = ImageManager.copyImage(selectedImageFile); }
            catch (IOException e) { logger.warning("Image copy failed: " + e.getMessage()); }
        }
        try {
            FoodService.addFoodItem(foodName, price, chkAvailable.isSelected(), stock, imagePath);
            JOptionPane.showMessageDialog(this, "Food item added successfully!");
            loadFoodTable();
            clearForm();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnAddActionPerformed

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
        if (selectedFoodId == -1) { JOptionPane.showMessageDialog(this, "Please select a food item."); return; }
        String foodName = txtFoodName.getText().trim();
        String priceText = txtPrice.getText().trim();
        String stockText = txtStock.getText().trim();
        if (foodName.isEmpty() || priceText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter food name and price.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE); return;
        }
        double price; int stock;
        try { price = Double.parseDouble(priceText); } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Price must be a valid number.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE); return;
        }
        try { stock = stockText.isEmpty() ? 0 : Integer.parseInt(stockText); } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Stock quantity must be a whole number.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE); return;
        }
        if (price <= 0) {
            JOptionPane.showMessageDialog(this, "Price must be greater than zero.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE); return;
        }
        String imagePath = currentImagePath;
        if (selectedImageFile != null) {
            try { imagePath = ImageManager.copyImage(selectedImageFile); }
            catch (IOException e) { logger.warning("Image copy failed: " + e.getMessage()); }
        }
        try {
            FoodService.updateFoodItem(selectedFoodId, foodName, price, chkAvailable.isSelected(), stock, imagePath);
            JOptionPane.showMessageDialog(this, "Food item updated successfully!");
            loadFoodTable();
            clearForm();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        if (selectedFoodId == -1) { JOptionPane.showMessageDialog(this, "Please select a food item."); return; }
        int r = JOptionPane.showConfirmDialog(this, "Delete this food item?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
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

    private void clearForm() {
        selectedFoodId = -1;
        selectedImageFile = null;
        currentImagePath = null;
        txtFoodName.setText("");
        txtPrice.setText("");
        txtStock.setText("");
        chkAvailable.setSelected(true);
        lblImagePreview.setIcon(ImageManager.placeholder(120, 90));
        jTable1.clearSelection();
        btnAdd.setEnabled(true);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);
        txtFoodName.requestFocus();
    }

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        clearForm();
    }//GEN-LAST:event_btnClearActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new AdminDashboardFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new FoodManagementFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton   btnAdd;
    private javax.swing.JButton   btnBack;
    private javax.swing.JButton   btnClear;
    private javax.swing.JButton   btnDelete;
    private javax.swing.JButton   btnUpdate;
    private javax.swing.JButton   btnPickImage;
    private javax.swing.JCheckBox chkAvailable;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable    jTable1;
    private javax.swing.JLabel    lblFoodName;
    private javax.swing.JLabel    lblPrice;
    private javax.swing.JLabel    lblStock;
    private javax.swing.JLabel    lblTitle;
    private javax.swing.JLabel    lblImagePreview;
    private javax.swing.JTextField txtFoodName;
    private javax.swing.JTextField txtPrice;
    private javax.swing.JTextField txtStock;
    // End of variables declaration//GEN-END:variables
}
