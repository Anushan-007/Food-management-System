package citybites.ui;

import citybites.model.FoodCategory;
import citybites.service.FoodCategoryService;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

public class CategoryManagementFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(CategoryManagementFrame.class.getName());

    // ── Table model: 3 columns — ID (hidden), Category Name, Description ──────
    private DefaultTableModel tableModel;

    /**
     * The ID of the category currently loaded into the form for editing.
     * {@code -1} means the form is in Add mode (no category selected).
     */
    private int selectedCategoryId = -1;

    public CategoryManagementFrame() {
        initComponents();
        setTitle("City Bites - Category Management");
        setMinimumSize(new Dimension(760, 520));
        setSize(920, 620);
        setLocationRelativeTo(null);
        setResizable(true);
        setAddMode();          // initial button state
        loadCategoryTable();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        txtName        = new javax.swing.JTextField();
        txtDescription = new javax.swing.JTextArea();
        txtSearch      = new javax.swing.JTextField();
        btnAdd         = new javax.swing.JButton();
        btnUpdate      = new javax.swing.JButton();
        btnDelete      = new javax.swing.JButton();
        btnClear       = new javax.swing.JButton();
        tblCat         = new javax.swing.JTable();
        lblName        = new javax.swing.JLabel();
        lblDescription = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        // ── Navigation header ────────────────────────────────────────
        JButton refreshBtn = AppTheme.secondaryBtn("Refresh");
        refreshBtn.addActionListener(e -> { loadCategoryTable(); clearForm(); });

        JButton backBtn = AppTheme.ghostBtn("← Back");
        backBtn.setForeground(new Color(150, 170, 190));
        backBtn.addActionListener(e -> btnBackActionPerformed(null));

        JPanel header = AppTheme.navHeader("Category Management", null, refreshBtn, backBtn);

        // ── Form fields ──────────────────────────────────────────────
        lblName.setText("Category Name");
        lblName.setFont(AppTheme.FONT_LABEL);
        lblName.setForeground(AppTheme.TEXT_PRIMARY);

        AppTheme.styleField(txtName);
        txtName.putClientProperty("JTextField.placeholderText", "Enter category name...");

        lblDescription.setText("Description (optional)");
        lblDescription.setFont(AppTheme.FONT_LABEL);
        lblDescription.setForeground(AppTheme.TEXT_PRIMARY);

        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        txtDescription.setFont(AppTheme.FONT_BODY);
        txtDescription.setForeground(AppTheme.TEXT_PRIMARY);
        txtDescription.setBackground(AppTheme.BG_INPUT);
        txtDescription.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        txtDescription.setRows(3);

        JScrollPane descScroll = new JScrollPane(txtDescription);
        descScroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        descScroll.setPreferredSize(new Dimension(0, 80));

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setBackground(AppTheme.BG_CARD);
        GridBagConstraints c = new GridBagConstraints();
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.anchor  = GridBagConstraints.WEST;
        c.weightx = 1;
        c.insets  = new Insets(4, 0, 4, 0);

        c.gridx = 0; c.gridy = 0; fields.add(lblName,      c);
        c.gridy = 1;               fields.add(txtName,      c);
        c.gridy = 2;               fields.add(lblDescription, c);
        c.gridy = 3;               fields.add(descScroll,   c);

        // ── Buttons ──────────────────────────────────────────────────
        btnAdd    = AppTheme.primaryBtn("Add Category");
        btnUpdate = AppTheme.secondaryBtn("Update Category");
        btnDelete = AppTheme.dangerBtn("Delete Category");
        btnClear  = AppTheme.ghostBtn("Clear Form");

        btnAdd.setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.BTN_H));
        btnUpdate.setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.BTN_H));
        btnDelete.setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.BTN_H));
        btnClear.setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.BTN_H));

        btnAdd.addActionListener(e    -> btnAddActionPerformed(null));
        btnUpdate.addActionListener(e -> btnUpdateActionPerformed(null));
        btnDelete.addActionListener(e -> btnDeleteActionPerformed(null));
        btnClear.addActionListener(e  -> btnClearActionPerformed(null));

        // ── Form card ────────────────────────────────────────────────
        JPanel formCard = new JPanel();
        formCard.setBackground(AppTheme.BG_CARD);
        formCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));

        JLabel formTitle = new JLabel("Category Details");
        formTitle.setFont(AppTheme.FONT_SUBHEAD);
        formTitle.setForeground(AppTheme.TEXT_PRIMARY);
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        fields.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnAdd.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnUpdate.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnDelete.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnClear.setAlignmentX(Component.LEFT_ALIGNMENT);

        formCard.add(formTitle);
        formCard.add(Box.createVerticalStrut(14));
        formCard.add(fields);
        formCard.add(Box.createVerticalStrut(18));
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

        // ── Table panel ──────────────────────────────────────────────
        // Model has 3 columns: ID (hidden), Category Name, Description
        String[] columns = {"ID", "Category Name", "Description"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c2) { return false; }
        };
        tblCat = new javax.swing.JTable(tableModel);
        AppTheme.styleTable(tblCat);
        tblCat.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Remove the ID column from the view; the model column 0 stays intact for CRUD
        tblCat.getColumnModel().removeColumn(tblCat.getColumnModel().getColumn(0));

        tblCat.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) tableRowSelected();
        });

        // Search bar
        AppTheme.styleField(txtSearch);
        txtSearch.putClientProperty("JTextField.placeholderText", "Search by name or description...");
        txtSearch.setPreferredSize(new Dimension(260, AppTheme.BTN_H));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { filterTable(txtSearch.getText()); }
            @Override public void removeUpdate(DocumentEvent e)  { filterTable(txtSearch.getText()); }
            @Override public void changedUpdate(DocumentEvent e) { filterTable(txtSearch.getText()); }
        });

        JButton clearSearchBtn = AppTheme.ghostBtn("Clear");
        clearSearchBtn.addActionListener(e -> {
            txtSearch.setText("");
            loadCategoryTable();
        });

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBar.setBackground(AppTheme.BG_MAIN);
        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(AppTheme.FONT_BODY);
        searchLbl.setForeground(AppTheme.TEXT_PRIMARY);
        searchBar.add(searchLbl);
        searchBar.add(txtSearch);
        searchBar.add(clearSearchBtn);

        JScrollPane tableScroll = new JScrollPane(tblCat);
        tableScroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        tableScroll.getViewport().setBackground(AppTheme.BG_CARD);

        JPanel tablePanel = new JPanel(new BorderLayout(0, 8));
        tablePanel.setBackground(AppTheme.BG_MAIN);
        tablePanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        tablePanel.add(searchBar,   BorderLayout.NORTH);
        tablePanel.add(tableScroll, BorderLayout.CENTER);

        // ── Body ─────────────────────────────────────────────────────
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setBackground(AppTheme.BG_MAIN);
        body.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        body.add(formScroll, BorderLayout.WEST);
        body.add(tablePanel, BorderLayout.CENTER);

        // ── Root layout ──────────────────────────────────────────────
        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header, BorderLayout.NORTH);
        getContentPane().add(body,   BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // ── Form modes ────────────────────────────────────────────────────────

    /** Switches to Add mode: Add enabled, Update/Delete disabled, no selected ID. */
    private void setAddMode() {
        selectedCategoryId = -1;
        btnAdd.setEnabled(true);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);
    }

    /** Switches to Edit mode: Add disabled, Update/Delete enabled. */
    private void setEditMode() {
        btnAdd.setEnabled(false);
        btnUpdate.setEnabled(true);
        btnDelete.setEnabled(true);
    }

    // ── Data loading ──────────────────────────────────────────────────────

    private void loadCategoryTable() {
        try {
            tableModel.setRowCount(0);
            for (FoodCategory cat : FoodCategoryService.getAllCategories()) {
                tableModel.addRow(new Object[]{
                    cat.getCategoryId(),
                    cat.getCategoryName(),
                    cat.getDescription()
                });
            }
        } catch (Exception e) {
            AppTheme.showError(this, "Database Error",
                    "Could not load categories: " + e.getMessage());
        }
    }

    private void filterTable(String query) {
        try {
            tableModel.setRowCount(0);
            String q = (query == null) ? "" : query.toLowerCase().trim();
            for (FoodCategory cat : FoodCategoryService.getAllCategories()) {
                boolean nameMatch = q.isEmpty() ||
                        cat.getCategoryName().toLowerCase().contains(q);
                boolean descMatch = !q.isEmpty() &&
                        cat.getDescription() != null &&
                        cat.getDescription().toLowerCase().contains(q);
                if (nameMatch || descMatch) {
                    tableModel.addRow(new Object[]{
                        cat.getCategoryId(),
                        cat.getCategoryName(),
                        cat.getDescription()
                    });
                }
            }
        } catch (Exception e) {
            logger.warning("Filter failed: " + e.getMessage());
        }
    }

    private void tableRowSelected() {
        int viewRow = tblCat.getSelectedRow();
        if (viewRow < 0) {
            setAddMode();
            return;
        }
        int modelRow = tblCat.convertRowIndexToModel(viewRow);
        selectedCategoryId = (int) tableModel.getValueAt(modelRow, 0);  // ID from hidden model col
        txtName.setText(tableModel.getValueAt(modelRow, 1).toString());
        Object desc = tableModel.getValueAt(modelRow, 2);
        txtDescription.setText(desc != null ? desc.toString() : "");
        setEditMode();
    }

    // ── CRUD actions ──────────────────────────────────────────────────────

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        // Defensive guard: do not add if a category is currently selected for editing
        if (selectedCategoryId != -1) return;

        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            AppTheme.showWarning(this, "Validation Error",
                    "Please enter a category name.");
            return;
        }
        String desc = txtDescription.getText().trim();
        if (desc.length() > FoodCategoryService.MAX_DESCRIPTION_LENGTH) {
            AppTheme.showWarning(this, "Validation Error",
                    "Description must not exceed " + FoodCategoryService.MAX_DESCRIPTION_LENGTH +
                    " characters.");
            return;
        }
        try {
            FoodCategoryService.addCategory(name, desc.isEmpty() ? null : desc);
            AppTheme.showInfo(this, "Success", "Category \"" + name + "\" added.");
            loadCategoryTable();
            clearForm();
        } catch (IllegalArgumentException ex) {
            AppTheme.showWarning(this, "Validation Error", ex.getMessage());
        } catch (Exception ex) {
            AppTheme.showError(this, "Database Error",
                    "Could not add category: " + ex.getMessage());
        }
    }//GEN-LAST:event_btnAddActionPerformed

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
        if (selectedCategoryId < 1) {
            AppTheme.showWarning(this, "No Selection",
                    "Please select a category to update.");
            return;
        }
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            AppTheme.showWarning(this, "Validation Error",
                    "Please enter a category name.");
            return;
        }
        String desc = txtDescription.getText().trim();
        if (desc.length() > FoodCategoryService.MAX_DESCRIPTION_LENGTH) {
            AppTheme.showWarning(this, "Validation Error",
                    "Description must not exceed " + FoodCategoryService.MAX_DESCRIPTION_LENGTH +
                    " characters.");
            return;
        }
        try {
            FoodCategoryService.updateCategory(
                    selectedCategoryId, name, desc.isEmpty() ? null : desc);
            AppTheme.showInfo(this, "Updated", "Category updated to \"" + name + "\".");
            loadCategoryTable();
            clearForm();
        } catch (IllegalArgumentException ex) {
            AppTheme.showWarning(this, "Validation Error", ex.getMessage());
        } catch (Exception ex) {
            AppTheme.showError(this, "Database Error",
                    "Could not update category: " + ex.getMessage());
        }
    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        if (selectedCategoryId < 1) {
            AppTheme.showWarning(this, "No Selection",
                    "Please select a category to delete.");
            return;
        }
        String name = txtName.getText().trim();
        if (!AppTheme.showConfirm(this, "Confirm Delete",
                "Delete \"" + name + "\"? Food items in this category will become uncategorized.")) {
            return;
        }
        try {
            FoodCategoryService.deleteCategory(selectedCategoryId);
            AppTheme.showInfo(this, "Deleted", "Category \"" + name + "\" deleted.");
            loadCategoryTable();
            clearForm();
        } catch (IllegalStateException ex) {
            AppTheme.showWarning(this, "Cannot Delete", ex.getMessage());
        } catch (Exception ex) {
            AppTheme.showError(this, "Database Error",
                    "Could not delete category: " + ex.getMessage());
        }
    }//GEN-LAST:event_btnDeleteActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        clearForm();
    }//GEN-LAST:event_btnClearActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new AdminDashboardFrame().setVisible(true);
        dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    private void clearForm() {
        tblCat.clearSelection();
        txtName.setText("");
        txtDescription.setText("");
        setAddMode();
        txtName.requestFocusInWindow();
    }

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new CategoryManagementFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton    btnAdd;
    private javax.swing.JButton    btnUpdate;
    private javax.swing.JButton    btnDelete;
    private javax.swing.JButton    btnClear;
    private javax.swing.JTextField txtName;
    private javax.swing.JTextArea  txtDescription;
    private javax.swing.JTextField txtSearch;
    private javax.swing.JTable     tblCat;
    private javax.swing.JLabel     lblName;
    private javax.swing.JLabel     lblDescription;
    // End of variables declaration//GEN-END:variables
}
