package citybites.ui;

import citybites.model.FoodRating;
import citybites.model.Order;
import citybites.model.OrderItem;
import citybites.service.FoodRatingService;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.swing.*;

/**
 * Modal dialog that lets a customer rate individual food items from a completed order.
 *
 * <p>Layout: one rating card per order item, stacked in a scrollable list.
 * Each card shows the food name, an interactive {@link StarRatingPanel} (26 px painted
 * stars — no Unicode characters), a helper text label, and an optional review text area.
 * Existing ratings are pre-loaded when the dialog opens.
 *
 * <p>Stars are painted via Graphics2D/Path2D to avoid the Unicode glyph issue
 * (★ ☆ render as squares on Windows). Hover highlights stars 1–N in a lighter orange.
 * Clicking a star sets the rating. Arrow keys and number keys 1–5 also work.
 *
 * <p>The dialog reads the logged-in customer from {@link citybites.util.SessionManager}
 * via {@link FoodRatingService}; no customer ID is passed from the UI.
 */
public class FoodRatingDialog extends JDialog {

    private final Order order;

    /** orderItemId → interactive StarRatingPanel */
    private final Map<Integer, StarRatingPanel> starPanels  = new HashMap<>();
    /** orderItemId → hint label ("Select a rating" / "N / 5") */
    private final Map<Integer, JLabel>          ratingHints = new HashMap<>();
    /** orderItemId → review JTextArea */
    private final Map<Integer, JTextArea>       reviewAreas = new HashMap<>();

    public FoodRatingDialog(Window owner, Order order) {
        super(owner, "Rate Your Order", ModalityType.APPLICATION_MODAL);
        this.order = order;
        setResizable(true);
        buildUI();
        pack();
        setMinimumSize(new Dimension(540, 320));
        setLocationRelativeTo(owner);
    }

    // ── UI construction ────────────────────────────────────────────────────────

    private void buildUI() {
        getContentPane().setBackground(AppTheme.BG_MAIN);
        getContentPane().setLayout(new BorderLayout());

        // ── Header strip ─────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppTheme.BG_HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        JLabel title = new JLabel("Rate Your Order  #" + order.getOrderId());
        title.setFont(AppTheme.FONT_HEADING);
        title.setForeground(AppTheme.TEXT_WHITE);
        header.add(title, BorderLayout.WEST);
        getContentPane().add(header, BorderLayout.NORTH);

        // ── Rating cards (scrollable) ─────────────────────────────────────
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(AppTheme.BG_MAIN);
        listPanel.setBorder(BorderFactory.createEmptyBorder(14, 16, 8, 16));

        List<OrderItem> items = order.getOrderItems();
        if (items.isEmpty()) {
            JLabel empty = new JLabel("No items found for this order.");
            empty.setFont(AppTheme.FONT_BODY);
            empty.setForeground(AppTheme.TEXT_MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(empty);
        } else {
            for (int i = 0; i < items.size(); i++) {
                listPanel.add(buildItemPanel(items.get(i)));
                if (i < items.size() - 1) {
                    listPanel.add(Box.createVerticalStrut(12));
                }
            }
        }
        listPanel.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(AppTheme.BG_MAIN);
        scroll.setPreferredSize(new Dimension(560, 400));
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        getContentPane().add(scroll, BorderLayout.CENTER);

        // ── Footer ─────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        footer.setBackground(AppTheme.BG_FOOTER);
        footer.setBorder(AppTheme.footerBorder());

        JButton btnSubmit = AppTheme.primaryBtn("Submit Ratings");
        btnSubmit.setPreferredSize(new Dimension(150, AppTheme.BTN_H));
        btnSubmit.addActionListener(e -> submitRatings());

        JButton btnClose = AppTheme.secondaryBtn("Close");
        btnClose.addActionListener(e -> dispose());

        footer.add(btnClose);
        footer.add(btnSubmit);
        getContentPane().add(footer, BorderLayout.SOUTH);
    }

    // ── Item card builder ─────────────────────────────────────────────────────

    /** Builds one card-style panel for a single order item. */
    private JPanel buildItemPanel(OrderItem item) {
        int orderItemId = item.getItemId();

        // Load any existing rating
        Optional<FoodRating> existingOpt = FoodRatingService.getRatingForOrderItem(orderItemId);
        int    existingRating = existingOpt.map(FoodRating::getRating).orElse(0);
        String existingReview = existingOpt.map(FoodRating::getReviewText).orElse(null);
        boolean alreadyRated  = existingOpt.isPresent();

        // ── Card container ────────────────────────────────────────────────
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(AppTheme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(14, 16, 12, 16)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ── Food name row ─────────────────────────────────────────────────
        JPanel nameRow = new JPanel(new BorderLayout(8, 0));
        nameRow.setOpaque(false);
        JLabel nameLbl = new JLabel(item.getFoodName());
        nameLbl.setFont(AppTheme.FONT_SUBHEAD);
        nameLbl.setForeground(AppTheme.TEXT_PRIMARY);
        nameRow.add(nameLbl, BorderLayout.WEST);
        if (alreadyRated) {
            JLabel badge = new JLabel("Already rated");
            badge.setFont(AppTheme.FONT_SMALL);
            badge.setForeground(AppTheme.SUCCESS);
            nameRow.add(badge, BorderLayout.EAST);
        }
        nameRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, nameRow.getPreferredSize().height));
        nameRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(nameRow);
        card.add(Box.createVerticalStrut(10));

        // ── Interactive star panel + hint label ───────────────────────────
        StarRatingPanel starPanel = StarRatingPanel.interactive(existingRating);
        starPanels.put(orderItemId, starPanel);

        JLabel hintLabel = new JLabel(
                existingRating > 0 ? existingRating + " / 5" : "Select a rating");
        hintLabel.setFont(AppTheme.FONT_SMALL);
        hintLabel.setForeground(
                existingRating > 0 ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_MUTED);
        ratingHints.put(orderItemId, hintLabel);

        starPanel.setOnRatingChanged(() -> {
            int r = starPanel.getRating();
            hintLabel.setText(r > 0 ? r + " / 5" : "Select a rating");
            hintLabel.setForeground(r > 0 ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_MUTED);
        });

        JPanel starRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        starRow.setBackground(AppTheme.BG_CARD);
        starRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        starRow.add(starPanel);
        starRow.add(hintLabel);
        starRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, starRow.getPreferredSize().height));
        card.add(starRow);
        card.add(Box.createVerticalStrut(10));

        // ── Review text area ─────────────────────────────────────────────
        JLabel reviewLbl = new JLabel("Review (optional):");
        reviewLbl.setFont(AppTheme.FONT_SMALL);
        reviewLbl.setForeground(AppTheme.TEXT_MUTED);
        reviewLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(reviewLbl);
        card.add(Box.createVerticalStrut(4));

        JTextArea reviewArea = new JTextArea(
                existingReview != null ? existingReview : "", 3, 30);
        reviewArea.setFont(AppTheme.FONT_BODY);
        reviewArea.setLineWrap(true);
        reviewArea.setWrapStyleWord(true);
        reviewArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        reviewAreas.put(orderItemId, reviewArea);

        JScrollPane reviewScroll = new JScrollPane(reviewArea);
        reviewScroll.setBorder(null);
        reviewScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        reviewScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        card.add(reviewScroll);

        // Constrain card height to its natural content — no vertical stretching
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height + 20));

        return card;
    }

    // ── Submit handler ────────────────────────────────────────────────────────

    private void submitRatings() {
        List<OrderItem> items = order.getOrderItems();

        // Validation: every item must have a rating selected
        for (OrderItem item : items) {
            StarRatingPanel panel = starPanels.get(item.getItemId());
            if (panel != null && panel.getRating() == 0) {
                AppTheme.showWarning(this, "Rating Required",
                        "Please select a rating for " + item.getFoodName() + ".");
                panel.requestFocusInWindow();
                return;
            }
        }

        // Submit all ratings
        boolean anySubmitted = false;
        boolean anyError     = false;
        StringBuilder errors = new StringBuilder();

        for (OrderItem item : items) {
            int orderItemId     = item.getItemId();
            StarRatingPanel panel = starPanels.get(orderItemId);
            int ratingValue     = (panel != null) ? panel.getRating() : 0;
            if (ratingValue == 0) continue; // safety guard

            JTextArea area = reviewAreas.get(orderItemId);
            String review  = (area != null) ? area.getText() : null;

            String error = FoodRatingService.saveRating(orderItemId, ratingValue, review);
            if (error == null) {
                anySubmitted = true;
            } else {
                anyError = true;
                errors.append("• ").append(item.getFoodName())
                      .append(": ").append(error).append("\n");
            }
        }

        if (anyError) {
            AppTheme.showWarning(this, "Some Ratings Not Saved", errors.toString().trim());
        }
        if (anySubmitted) {
            AppTheme.showInfo(this, "Ratings Saved",
                    "Your ratings have been saved successfully.");
            dispose();
        } else if (!anyError) {
            AppTheme.showWarning(this, "Nothing Submitted",
                    "Please select a star rating for at least one item.");
        }
    }
}
