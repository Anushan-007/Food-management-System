package citybites.ui;

import citybites.model.FoodItem;
import citybites.service.FoodService;
import citybites.util.ImageManager;
import java.awt.*;
import java.io.File;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class FoodManagementFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(FoodManagementFrame.class.getName());

    private DefaultTableModel tableModel;
    /** Temporary source file chosen via JFileChooser. Preview-only — never stored. */
    private java.io.File selectedSourceFile = null;
    /** Managed relative filename stored in MySQL. Null until a row is selected or image imported. */
    private String persistedImagePath = null;

    public FoodManagementFrame() {
        initComponents();
        setTitle("City Bites - Food Management");
        setMinimumSize(new Dimension(1000, 640));
        setSize(1200, 740);
        setLocationRelativeTo(null);
        setResizable(true);
        loadFoodTable();
        // Recompute proportional column widths whenever the frame is shown or resized
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) { applyFoodTableColumnWidths(); }
            @Override public void componentShown(java.awt.event.ComponentEvent e)   { applyFoodTableColumnWidths(); }
        });
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        txtName        = new javax.swing.JTextField();
        txtPrice       = new javax.swing.JTextField();
        txtStock       = new javax.swing.JTextField();
        txtSearch      = new javax.swing.JTextField();
        btnAdd         = new javax.swing.JButton();
        btnUpdate      = new javax.swing.JButton();
        btnDelete      = new javax.swing.JButton();
        btnClear       = new javax.swing.JButton();
        btnPickImage   = new javax.swing.JButton();
        lblImagePreview = new javax.swing.JLabel();
        tblFood        = new javax.swing.JTable();
        lblName        = new javax.swing.JLabel();
        lblPrice       = new javax.swing.JLabel();
        lblStock       = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        // ── Navigation header ────────────────────────────────────────
        JButton refreshBtn = AppTheme.secondaryBtn("Refresh");
        refreshBtn.addActionListener(e -> loadFoodTable());

        JButton backBtn = AppTheme.ghostBtn("← Back");
        backBtn.setForeground(new Color(150, 170, 190));
        backBtn.addActionListener(e -> btnBackActionPerformed(null));

        JPanel header = AppTheme.navHeader("Food Management", null, refreshBtn, backBtn);

        // ── Form panel (left) ────────────────────────────────────────
        lblName.setText("Food Name");
        lblName.setFont(AppTheme.FONT_LABEL);
        lblName.setForeground(AppTheme.TEXT_PRIMARY);

        lblPrice.setText("Price (Rs.)");
        lblPrice.setFont(AppTheme.FONT_LABEL);
        lblPrice.setForeground(AppTheme.TEXT_PRIMARY);

        lblStock.setText("Stock Quantity");
        lblStock.setFont(AppTheme.FONT_LABEL);
        lblStock.setForeground(AppTheme.TEXT_PRIMARY);

        AppTheme.styleField(txtName);
        AppTheme.styleField(txtPrice);
        AppTheme.styleField(txtStock);

        // Image preview
        lblImagePreview.setPreferredSize(new Dimension(180, 120));
        lblImagePreview.setMinimumSize(new Dimension(180, 120));
        lblImagePreview.setMaximumSize(new Dimension(180, 120));
        lblImagePreview.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        lblImagePreview.setHorizontalAlignment(SwingConstants.CENTER);
        lblImagePreview.setText("No Image");
        lblImagePreview.setFont(AppTheme.FONT_SMALL);
        lblImagePreview.setForeground(AppTheme.TEXT_MUTED);
        lblImagePreview.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnPickImage = AppTheme.secondaryBtn("Choose Image...");
        btnPickImage.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnPickImage.setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.BTN_H));
        btnPickImage.addActionListener(e -> btnPickImageActionPerformed(null));

        // Form fields in GridBagLayout
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setBackground(AppTheme.BG_CARD);
        GridBagConstraints c = new GridBagConstraints();
        c.fill   = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.insets  = new Insets(5, 0, 5, 0);

        c.gridx = 0; c.gridy = 0; fields.add(lblName,  c);
        c.gridy = 1;               fields.add(txtName,  c);
        c.gridy = 2;               fields.add(lblPrice, c);
        c.gridy = 3;               fields.add(txtPrice, c);
        c.gridy = 4;               fields.add(lblStock, c);
        c.gridy = 5;               fields.add(txtStock, c);

        // Action buttons
        btnAdd    = AppTheme.primaryBtn("Add Item");
        btnUpdate = AppTheme.secondaryBtn("Update Item");
        btnDelete = AppTheme.dangerBtn("Delete Item");
        btnClear  = AppTheme.ghostBtn("Clear Form");

        btnAdd.setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.BTN_H));
        btnUpdate.setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.BTN_H));
        btnDelete.setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.BTN_H));
        btnClear.setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.BTN_H));

        btnAdd.addActionListener(e -> btnAddActionPerformed(null));
        btnUpdate.addActionListener(e -> btnUpdateActionPerformed(null));
        btnDelete.addActionListener(e -> btnDeleteActionPerformed(null));
        btnClear.addActionListener(e -> btnClearActionPerformed(null));

        JPanel formCard = new JPanel();
        formCard.setBackground(AppTheme.BG_CARD);
        formCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));

        JLabel formTitle = new JLabel("Food Details");
        formTitle.setFont(AppTheme.FONT_SUBHEAD);
        formTitle.setForeground(AppTheme.TEXT_PRIMARY);
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        fields.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnAdd.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnUpdate.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnDelete.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnClear.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPickImage.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPickImage.setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.BTN_H));

        JPanel imgWrapper = new JPanel();
        imgWrapper.setLayout(new BoxLayout(imgWrapper, BoxLayout.Y_AXIS));
        imgWrapper.setBackground(AppTheme.BG_CARD);
        imgWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        imgWrapper.add(lblImagePreview);
        imgWrapper.add(Box.createVerticalStrut(6));
        imgWrapper.add(btnPickImage);

        formCard.add(formTitle);
        formCard.add(Box.createVerticalStrut(16));
        formCard.add(fields);
        formCard.add(Box.createVerticalStrut(12));
        formCard.add(imgWrapper);
        formCard.add(Box.createVerticalStrut(16));
        formCard.add(btnAdd);
        formCard.add(Box.createVerticalStrut(6));
        formCard.add(btnUpdate);
        formCard.add(Box.createVerticalStrut(6));
        formCard.add(btnDelete);
        formCard.add(Box.createVerticalStrut(6));
        formCard.add(btnClear);
        formCard.add(Box.createVerticalGlue());

        JScrollPane formScroll = new JScrollPane(formCard);
        formScroll.setBorder(null);
        formScroll.setPreferredSize(new Dimension(280, 0));

        // ── Table panel (right) ──────────────────────────────────────
        String[] columns = {"ID", "Name", "Price (Rs.)", "Stock", "Available", "Image"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c2) { return false; }
        };
        tblFood = new javax.swing.JTable(tableModel);
        AppTheme.styleTable(tblFood);
        tblFood.setRowHeight(64);
        // AUTO_RESIZE_OFF lets applyFoodTableColumnWidths() control proportions
        tblFood.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Capture the L&F header renderer once before any per-column overrides
        final javax.swing.table.TableCellRenderer origHdr =
                tblFood.getTableHeader().getDefaultRenderer();

        // ID — hidden; keep narrow before removal
        tblFood.getColumnModel().getColumn(0).setMaxWidth(50);

        // ── Food Name (left / left) ───────────────────────────────────────────
        var colName = tblFood.getColumnModel().getColumn(1);
        colName.setMinWidth(220);
        colName.setHeaderRenderer(headerRenderer(origHdr, SwingConstants.LEFT));
        var nameRenderer = new DefaultTableCellRenderer();
        nameRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        colName.setCellRenderer(nameRenderer);

        // ── Price (Rs.) (right / right) ──────────────────────────────────────
        var colPrice = tblFood.getColumnModel().getColumn(2);
        colPrice.setMinWidth(120);
        colPrice.setHeaderRenderer(headerRenderer(origHdr, SwingConstants.RIGHT));
        var priceRenderer = new DefaultTableCellRenderer();
        priceRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        colPrice.setCellRenderer(priceRenderer);

        // ── Stock (centre / centre) ───────────────────────────────────────────
        var colStock = tblFood.getColumnModel().getColumn(3);
        colStock.setMinWidth(80);
        colStock.setHeaderRenderer(headerRenderer(origHdr, SwingConstants.CENTER));
        var stockRenderer = new DefaultTableCellRenderer();
        stockRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        colStock.setCellRenderer(stockRenderer);

        // ── Available (centre / coloured badge) ──────────────────────────────
        var colAvail = tblFood.getColumnModel().getColumn(4);
        colAvail.setMinWidth(110);
        colAvail.setHeaderRenderer(headerRenderer(origHdr, SwingConstants.CENTER));
        colAvail.setCellRenderer(new AvailabilityCellRenderer());

        // ── Image (centre / thumbnail) ────────────────────────────────────────
        var colImg = tblFood.getColumnModel().getColumn(5);
        colImg.setMinWidth(100);
        colImg.setHeaderRenderer(headerRenderer(origHdr, SwingConstants.CENTER));
        colImg.setCellRenderer(new ImageCellRenderer());

        tblFood.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) tableRowSelected();
        });
        // Hide internal ID column from view; model column 0 still accessible for CRUD
        tblFood.getColumnModel().removeColumn(tblFood.getColumnModel().getColumn(0));

        // Search bar for table
        AppTheme.styleField(txtSearch);
        txtSearch.putClientProperty("JTextField.placeholderText", "Search food items...");
        txtSearch.setPreferredSize(new Dimension(260, AppTheme.BTN_H));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { filterTable(txtSearch.getText()); }
            @Override public void removeUpdate(DocumentEvent e)  { filterTable(txtSearch.getText()); }
            @Override public void changedUpdate(DocumentEvent e) { filterTable(txtSearch.getText()); }
        });

        JButton clearSearchBtn = AppTheme.ghostBtn("Clear");
        clearSearchBtn.addActionListener(e -> {
            txtSearch.setText("");
            loadFoodTable();
        });

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBar.setBackground(AppTheme.BG_MAIN);
        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(AppTheme.FONT_BODY);
        searchLbl.setForeground(AppTheme.TEXT_PRIMARY);
        searchBar.add(searchLbl);
        searchBar.add(txtSearch);
        searchBar.add(clearSearchBtn);

        JScrollPane tableScroll = new JScrollPane(tblFood);
        tableScroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        tableScroll.getViewport().setBackground(AppTheme.BG_CARD);

        JPanel tablePanel = new JPanel(new BorderLayout(0, 8));
        tablePanel.setBackground(AppTheme.BG_MAIN);
        tablePanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        tablePanel.add(searchBar, BorderLayout.NORTH);
        tablePanel.add(tableScroll, BorderLayout.CENTER);

        // ── Body: form left + table right ───────────────────────────
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setBackground(AppTheme.BG_MAIN);
        body.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        body.add(formScroll,  BorderLayout.WEST);
        body.add(tablePanel, BorderLayout.CENTER);

        // ── Root layout ──────────────────────────────────────────────
        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header, BorderLayout.NORTH);
        getContentPane().add(body,   BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // ── Data loading ─────────────────────────────────────────────────

    private void loadFoodTable() {
        try {
            tableModel.setRowCount(0);
            List<FoodItem> items = FoodService.getAllFoodItems();
            for (FoodItem f : items) {
                tableModel.addRow(new Object[]{
                    f.getFoodId(), f.getFoodName(),
                    String.format("%.2f", f.getPrice()),
                    f.getStockQuantity(), f.isAvailable(), f.getImagePath()
                });
            }
        } catch (Exception e) {
            AppTheme.showError(this, "Database Error",
                    "Could not load food items: " + e.getMessage());
        }
        // Re-apply proportional widths after data changes; invokeLater ensures layout is complete
        SwingUtilities.invokeLater(this::applyFoodTableColumnWidths);
    }

    /**
     * Distributes table column widths proportionally across the visible viewport.
     * Called on frame show, resize, and after data load to eliminate the fixed-width
     * problem that causes the Name column to absorb all remaining space.
     * View columns after ID removal: 0=Name 1=Price 2=Stock 3=Available 4=Image.
     */
    private void applyFoodTableColumnWidths() {
        int totalWidth = (tblFood.getParent() != null) ? tblFood.getParent().getWidth() : 0;
        if (totalWidth <= 0) totalWidth = tblFood.getWidth();
        if (totalWidth <= 0) return;
        var cols = tblFood.getColumnModel();
        cols.getColumn(0).setPreferredWidth((int) (totalWidth * 0.35));
        cols.getColumn(1).setPreferredWidth((int) (totalWidth * 0.16));
        cols.getColumn(2).setPreferredWidth((int) (totalWidth * 0.12));
        cols.getColumn(3).setPreferredWidth((int) (totalWidth * 0.17));
        cols.getColumn(4).setPreferredWidth((int) (totalWidth * 0.20));
    }

    private void filterTable(String query) {
        try {
            tableModel.setRowCount(0);
            List<FoodItem> items = FoodService.getAllFoodItems();
            String q = (query == null) ? "" : query.toLowerCase().trim();
            for (FoodItem f : items) {
                if (q.isEmpty() || f.getFoodName().toLowerCase().contains(q)) {
                    tableModel.addRow(new Object[]{
                        f.getFoodId(), f.getFoodName(),
                        String.format("%.2f", f.getPrice()),
                        f.getStockQuantity(), f.isAvailable(), f.getImagePath()
                    });
                }
            }
        } catch (Exception e) {
            logger.warning("Filter failed: " + e.getMessage());
        }
    }

    private void tableRowSelected() {
        int viewRow = tblFood.getSelectedRow();
        if (viewRow < 0) return;
        int row = tblFood.convertRowIndexToModel(viewRow);
        txtName.setText(tableModel.getValueAt(row, 1).toString());
        txtPrice.setText(tableModel.getValueAt(row, 2).toString());
        txtStock.setText(tableModel.getValueAt(row, 3).toString());
        Object imgPath = tableModel.getValueAt(row, 5);
        persistedImagePath = (imgPath != null) ? imgPath.toString() : null;
        selectedSourceFile  = null;   // no new image chosen yet
        if (persistedImagePath != null && !persistedImagePath.isBlank()) {
            lblImagePreview.setIcon(ImageManager.loadScaled(persistedImagePath, 180, 120));
            lblImagePreview.setText(null);
        } else {
            lblImagePreview.setIcon(null);
            lblImagePreview.setText("No Image");
        }
    }

    // ── CRUD actions ─────────────────────────────────────────────────

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        String name  = txtName.getText().trim();
        String price = txtPrice.getText().trim();
        String stock = txtStock.getText().trim();
        if (name.isEmpty() || price.isEmpty() || stock.isEmpty()) {
            AppTheme.showWarning(this, "Validation Error",
                    "Please fill in all fields before adding.");
            return;
        }
        // Import image BEFORE DB insert so we can roll back the managed file on DB failure.
        // Never store the raw source path — only the managed relative filename goes to MySQL.
        String managedPath = null;
        if (selectedSourceFile != null) {
            try {
                managedPath = ImageManager.importImage(selectedSourceFile.toPath());
            } catch (java.io.IOException ex) {
                AppTheme.showWarning(this, "Image Error",
                        "Could not import image: " + ex.getMessage());
                return;
            }
        }
        try {
            double p = Double.parseDouble(price);
            int    s = Integer.parseInt(stock);
            if (p <= 0 || s < 0) throw new NumberFormatException();
            FoodService.addFoodItem(name, p, s > 0, s, managedPath);
            AppTheme.showInfo(this, "Success", "\"" + name + "\" added successfully.");
            loadFoodTable();
            clearForm();
        } catch (NumberFormatException ex) {
            if (managedPath != null) ImageManager.deleteManagedImage(managedPath);
            AppTheme.showWarning(this, "Validation Error",
                    "Price must be a positive number and Stock a non-negative integer.");
        } catch (Exception ex) {
            if (managedPath != null) ImageManager.deleteManagedImage(managedPath);
            AppTheme.showError(this, "Database Error",
                    "Could not add item: " + ex.getMessage());
        }
    }//GEN-LAST:event_btnAddActionPerformed

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
        int viewRow = tblFood.getSelectedRow();
        if (viewRow < 0) {
            AppTheme.showWarning(this, "No Selection",
                    "Please select a food item from the table to update.");
            return;
        }
        int row = tblFood.convertRowIndexToModel(viewRow);
        String name  = txtName.getText().trim();
        String price = txtPrice.getText().trim();
        String stock = txtStock.getText().trim();
        if (name.isEmpty() || price.isEmpty() || stock.isEmpty()) {
            AppTheme.showWarning(this, "Validation Error",
                    "Please fill in all fields before updating.");
            return;
        }
        // If no new image was chosen, retain the existing persisted path.
        // Only import (and eventually replace) when the user actively selected a new file.
        String newManagedPath = persistedImagePath;
        if (selectedSourceFile != null) {
            try {
                newManagedPath = ImageManager.importImage(selectedSourceFile.toPath());
            } catch (java.io.IOException ex) {
                AppTheme.showWarning(this, "Image Error",
                        "Could not import new image: " + ex.getMessage());
                return;
            }
        }
        try {
            int    id = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
            double p  = Double.parseDouble(price);
            int    s  = Integer.parseInt(stock);
            if (p <= 0 || s < 0) throw new NumberFormatException();
            FoodService.updateFoodItem(id, name, p, s > 0, s, newManagedPath);
            // Delete old managed image only after DB update succeeds and
            // only if a genuinely new image replaced it.
            if (selectedSourceFile != null
                    && persistedImagePath != null
                    && !persistedImagePath.equals(newManagedPath)) {
                ImageManager.deleteManagedImage(persistedImagePath);
            }
            AppTheme.showInfo(this, "Updated", "\"" + name + "\" updated successfully.");
            loadFoodTable();
            clearForm();
        } catch (NumberFormatException ex) {
            if (selectedSourceFile != null && newManagedPath != null
                    && !newManagedPath.equals(persistedImagePath)) {
                ImageManager.deleteManagedImage(newManagedPath);
            }
            AppTheme.showWarning(this, "Validation Error",
                    "Price must be a positive number and Stock a non-negative integer.");
        } catch (Exception ex) {
            if (selectedSourceFile != null && newManagedPath != null
                    && !newManagedPath.equals(persistedImagePath)) {
                ImageManager.deleteManagedImage(newManagedPath);
            }
            AppTheme.showError(this, "Database Error",
                    "Could not update item: " + ex.getMessage());
        }
    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        int viewRow = tblFood.getSelectedRow();
        if (viewRow < 0) {
            AppTheme.showWarning(this, "No Selection",
                    "Please select a food item from the table to delete.");
            return;
        }
        int row = tblFood.convertRowIndexToModel(viewRow);
        String name = tableModel.getValueAt(row, 1).toString();
        if (!AppTheme.showConfirm(this, "Confirm Delete",
                "Delete \"" + name + "\"? This cannot be undone.")) {
            return;
        }
        try {
            int id = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
            FoodService.deleteFoodItem(id);
            AppTheme.showInfo(this, "Deleted", "\"" + name + "\" deleted.");
            loadFoodTable();
            clearForm();
        } catch (Exception ex) {
            AppTheme.showError(this, "Database Error",
                    "Could not delete item: " + ex.getMessage());
        }
    }//GEN-LAST:event_btnDeleteActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        clearForm();
    }//GEN-LAST:event_btnClearActionPerformed

    private void btnPickImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPickImageActionPerformed
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Food Image");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Image files (*.jpg, *.jpeg, *.png)", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedSourceFile = chooser.getSelectedFile();
            // Preview from source directly — NOT persisted yet, NOT through managed directory
            lblImagePreview.setIcon(
                ImageManager.loadScaledPreview(selectedSourceFile, 180, 120));
            lblImagePreview.setText(null);
        }
    }//GEN-LAST:event_btnPickImageActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new AdminDashboardFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    private void clearForm() {
        txtName.setText("");
        txtPrice.setText("");
        txtStock.setText("");
        selectedSourceFile  = null;   // discard temp source — no managed file touched
        persistedImagePath  = null;
        lblImagePreview.setIcon(null);
        lblImagePreview.setText("No Image");
        tblFood.clearSelection();
    }

    /**
     * Returns a per-column header renderer that wraps the L&F default renderer
     * and overrides only the horizontal alignment, so the native header look is preserved.
     */
    private static javax.swing.table.TableCellRenderer headerRenderer(
            javax.swing.table.TableCellRenderer orig, int alignment) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = orig.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                if (c instanceof JLabel) ((JLabel) c).setHorizontalAlignment(alignment);
                return c;
            }
        };
    }

    /**
     * Renders a scaled food thumbnail in the Image column.
     * Results are cached by managed relative filename to avoid repeated disk reads during repaints.
     */
    private static class ImageCellRenderer extends DefaultTableCellRenderer {
        private static final java.util.Map<String, ImageIcon> CACHE =
                new java.util.LinkedHashMap<String, ImageIcon>(64, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(java.util.Map.Entry<String, ImageIcon> e) {
                        return size() > 50;
                    }
                };

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                    table, "", isSelected, hasFocus, row, column);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setVerticalAlignment(SwingConstants.CENTER);
            String path = (value != null) ? value.toString() : null;
            if (path == null || path.isBlank()) {
                lbl.setIcon(ImageManager.placeholder(80, 56));
            } else {
                lbl.setIcon(CACHE.computeIfAbsent(path, p -> ImageManager.loadScaled(p, 80, 56)));
            }
            return lbl;
        }
    }

    /**
     * Renders the Available column as a coloured "Available" / "Unavailable" badge.
     * Foreground colour is suppressed when the row is selected so contrast is preserved.
     */
    private static class AvailabilityCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            String text = Boolean.TRUE.equals(value) ? "Available" : "Unavailable";
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                    table, text, isSelected, hasFocus, row, column);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setFont(AppTheme.FONT_SMALL);
            if (!isSelected) {
                lbl.setForeground(Boolean.TRUE.equals(value) ? AppTheme.SUCCESS : AppTheme.DANGER);
            }
            return lbl;
        }
    }

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new FoodManagementFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton    btnAdd;
    private javax.swing.JButton    btnUpdate;
    private javax.swing.JButton    btnDelete;
    private javax.swing.JButton    btnClear;
    private javax.swing.JButton    btnPickImage;
    private javax.swing.JTextField txtName;
    private javax.swing.JTextField txtPrice;
    private javax.swing.JTextField txtStock;
    private javax.swing.JTextField txtSearch;
    private javax.swing.JTable     tblFood;
    private javax.swing.JLabel     lblName;
    private javax.swing.JLabel     lblPrice;
    private javax.swing.JLabel     lblStock;
    private javax.swing.JLabel     lblImagePreview;
    // End of variables declaration//GEN-END:variables
}
