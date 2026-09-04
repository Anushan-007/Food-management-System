package citybites.ui;

import citybites.model.FoodItem;
import citybites.model.FoodReviewDetail;
import citybites.service.FoodRatingService;
import citybites.service.FoodService;
import citybites.util.ImageManager;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import javax.swing.*;
import javax.swing.border.Border;

/**
 * Read-only admin dialog showing all customer ratings and reviews for a single food item.
 * Opened from FoodManagementFrame when "View Reviews" is clicked.
 *
 * <p>Stars are painted via Graphics2D (StarRatingPanel) to avoid Unicode glyph issues on Windows.
 */
public class FoodReviewsDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    public FoodReviewsDialog(Frame owner, int foodId) {
        super(owner, "Food Reviews", true);
        setMinimumSize(new Dimension(640, 420));
        setSize(740, 600);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // ESC closes the dialog
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        Optional<FoodItem> foodOpt = FoodService.getFoodItemById(foodId);
        List<FoodReviewDetail> reviews = FoodRatingService.getReviewsForFood(foodId);

        buildUI(foodOpt.orElse(null), reviews);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI construction
    // ─────────────────────────────────────────────────────────────────────────

    private void buildUI(FoodItem food, List<FoodReviewDetail> reviews) {
        Container cp = getContentPane();
        cp.setBackground(AppTheme.BG_MAIN);
        cp.setLayout(new BorderLayout());

        cp.add(buildHeader(food, reviews), BorderLayout.NORTH);
        cp.add(buildContent(reviews),      BorderLayout.CENTER);
        cp.add(buildFooter(),              BorderLayout.SOUTH);
    }

    // ── Header ──────────────────────────────────────────────────────────────

    private JPanel buildHeader(FoodItem food, List<FoodReviewDetail> reviews) {
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setBackground(AppTheme.BG_HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        // Left: 72×72 thumbnail
        JLabel thumb = new JLabel();
        thumb.setPreferredSize(new Dimension(72, 72));
        thumb.setHorizontalAlignment(SwingConstants.CENTER);
        thumb.setVerticalAlignment(SwingConstants.CENTER);
        if (food != null && food.getImagePath() != null && !food.getImagePath().isBlank()) {
            ImageIcon icon = ImageManager.loadScaled(food.getImagePath(), 72, 72);
            thumb.setIcon(icon != null ? icon : ImageManager.placeholder(72, 72));
        } else {
            thumb.setIcon(ImageManager.placeholder(72, 72));
        }
        header.add(thumb, BorderLayout.WEST);

        // Centre: food name + category
        JPanel centre = new JPanel();
        centre.setOpaque(false);
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));
        centre.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        String foodName = (food != null && food.getFoodName() != null)
                ? food.getFoodName() : "Food Item";
        JLabel nameLabel = new JLabel(foodName);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        nameLabel.setForeground(Color.WHITE);

        String catText = "";
        if (food != null) {
            String cat = food.getCategoryName();
            if (cat != null && !cat.isBlank()) catText = cat;
        }
        if (catText.isEmpty() && !reviews.isEmpty()) {
            String rc = reviews.get(0).getCategoryName();
            if (rc != null && !rc.isBlank()) catText = rc;
        }
        centre.add(nameLabel);
        if (!catText.isEmpty()) {
            JLabel catLabel = new JLabel(catText);
            catLabel.setFont(AppTheme.FONT_SMALL);
            catLabel.setForeground(new Color(160, 180, 200));
            centre.add(Box.createVerticalStrut(3));
            centre.add(catLabel);
        }
        header.add(centre, BorderLayout.CENTER);

        // Right: avg number + stars + count
        JPanel badge = new JPanel();
        badge.setOpaque(false);
        badge.setLayout(new BoxLayout(badge, BoxLayout.Y_AXIS));

        if (!reviews.isEmpty()) {
            double sum = reviews.stream().mapToInt(FoodReviewDetail::getRating).sum();
            double avg = sum / reviews.size();
            int    roundedAvg = (int) Math.round(avg);

            JLabel avgLabel = new JLabel(String.format("%.1f / 5", avg));
            avgLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
            avgLabel.setForeground(AppTheme.BRAND_ACCENT);
            avgLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

            StarRatingPanel stars = new StarRatingPanel(roundedAvg);
            stars.setAlignmentX(Component.RIGHT_ALIGNMENT);

            String countText = reviews.size() == 1 ? "1 review" : reviews.size() + " reviews";
            JLabel cntLabel = new JLabel(countText);
            cntLabel.setFont(AppTheme.FONT_SMALL);
            cntLabel.setForeground(new Color(160, 180, 200));
            cntLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

            badge.add(avgLabel);
            badge.add(Box.createVerticalStrut(4));
            badge.add(stars);
            badge.add(Box.createVerticalStrut(3));
            badge.add(cntLabel);
        } else {
            JLabel noRatingLabel = new JLabel("\u2014");
            noRatingLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
            noRatingLabel.setForeground(new Color(130, 150, 170));
            noRatingLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

            JLabel cntLabel = new JLabel("0 reviews");
            cntLabel.setFont(AppTheme.FONT_SMALL);
            cntLabel.setForeground(new Color(160, 180, 200));
            cntLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

            badge.add(noRatingLabel);
            badge.add(Box.createVerticalStrut(4));
            badge.add(cntLabel);
        }
        header.add(badge, BorderLayout.EAST);

        return header;
    }

    // ── Content ─────────────────────────────────────────────────────────────

    private JComponent buildContent(List<FoodReviewDetail> reviews) {
        if (reviews.isEmpty()) {
            return buildEmptyState();
        }

        JPanel cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setBackground(AppTheme.BG_MAIN);
        cardsPanel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        for (int i = 0; i < reviews.size(); i++) {
            JPanel card = buildReviewCard(reviews.get(i));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            cardsPanel.add(card);
            if (i < reviews.size() - 1) {
                cardsPanel.add(Box.createVerticalStrut(10));
            }
        }
        // Push cards to the top when there are few of them
        cardsPanel.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(AppTheme.BG_MAIN);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    private JPanel buildEmptyState() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(AppTheme.BG_MAIN);

        panel.add(Box.createVerticalGlue());

        // Painted star outline icon
        JLabel starIcon = new JLabel(new EmptyStarIcon(52));
        starIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(starIcon);

        panel.add(Box.createVerticalStrut(14));

        JLabel heading = new JLabel("No ratings yet");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 16));
        heading.setForeground(AppTheme.TEXT_PRIMARY);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(heading);

        panel.add(Box.createVerticalStrut(6));

        JLabel desc = new JLabel("Customers can rate this item after completing an order.");
        desc.setFont(AppTheme.FONT_BODY);
        desc.setForeground(AppTheme.TEXT_MUTED);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(desc);

        panel.add(Box.createVerticalGlue());

        return panel;
    }

    // ── Review card ─────────────────────────────────────────────────────────

    private JPanel buildReviewCard(FoodReviewDetail review) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(AppTheme.BG_CARD);
        Border line = BorderFactory.createLineBorder(AppTheme.BORDER);
        Border pad  = BorderFactory.createEmptyBorder(14, 16, 12, 16);
        card.setBorder(BorderFactory.createCompoundBorder(line, pad));

        // Top row: bold customer name | star panel + numeric | order date (right)
        JPanel topRow = new JPanel(new BorderLayout(8, 0));
        topRow.setOpaque(false);

        JLabel customerLbl = new JLabel(review.getCustomerFullName());
        customerLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        customerLbl.setForeground(AppTheme.TEXT_PRIMARY);

        // Stars + numeric rating together
        JPanel starSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        starSection.setOpaque(false);
        starSection.add(new StarRatingPanel(review.getRating()));
        JLabel numericLbl = new JLabel(review.getRating() + " / 5");
        numericLbl.setFont(AppTheme.FONT_SMALL);
        numericLbl.setForeground(AppTheme.TEXT_MUTED);
        starSection.add(numericLbl);

        JPanel topLeft = new JPanel();
        topLeft.setOpaque(false);
        topLeft.setLayout(new BoxLayout(topLeft, BoxLayout.Y_AXIS));
        topLeft.add(customerLbl);
        topLeft.add(Box.createVerticalStrut(3));
        topLeft.add(starSection);

        topRow.add(topLeft, BorderLayout.WEST);

        if (review.getOrderDate() != null) {
            JLabel orderDateLbl = new JLabel("Order: " + review.getOrderDate().format(DT_FMT));
            orderDateLbl.setFont(AppTheme.FONT_SMALL);
            orderDateLbl.setForeground(AppTheme.TEXT_MUTED);
            topRow.add(orderDateLbl, BorderLayout.EAST);
        }

        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, topRow.getPreferredSize().height));
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(topRow);

        // Review text section — softly tinted background if text present
        String text = review.getReviewText();
        card.add(Box.createVerticalStrut(10));

        if (text != null && !text.isBlank()) {
            JPanel textSection = new JPanel(new BorderLayout());
            textSection.setBackground(AppTheme.BG_ACCENT_SOFT);
            textSection.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(245, 215, 180)),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            textSection.setAlignmentX(Component.LEFT_ALIGNMENT);

            JTextArea reviewTxt = new JTextArea(text);
            reviewTxt.setEditable(false);
            reviewTxt.setOpaque(false);
            reviewTxt.setLineWrap(true);
            reviewTxt.setWrapStyleWord(true);
            reviewTxt.setFont(AppTheme.FONT_BODY);
            reviewTxt.setForeground(AppTheme.TEXT_PRIMARY);
            reviewTxt.setBorder(null);
            textSection.add(reviewTxt, BorderLayout.CENTER);
            textSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, textSection.getPreferredSize().height + 200));
            card.add(textSection);
        } else {
            JLabel noReview = new JLabel("No written review");
            noReview.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            noReview.setForeground(AppTheme.TEXT_MUTED);
            noReview.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(noReview);
        }

        // Bottom row: rated date right-aligned
        if (review.getCreatedAt() != null) {
            card.add(Box.createVerticalStrut(8));
            JLabel ratedLbl = new JLabel("Rated: " + review.getCreatedAt().format(DATE_FMT));
            ratedLbl.setFont(AppTheme.FONT_SMALL);
            ratedLbl.setForeground(AppTheme.TEXT_MUTED);
            ratedLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);
            card.add(ratedLbl);
        }

        // Constrain card height to its preferred content height
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height + 40));

        return card;
    }

    // ── Footer ───────────────────────────────────────────────────────────────

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        footer.setBackground(AppTheme.BG_MAIN);
        footer.setBorder(AppTheme.footerBorder());
        JButton closeBtn = AppTheme.secondaryBtn("Close");
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);
        return footer;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner helper: an Icon that paints one large empty-star outline
    // ─────────────────────────────────────────────────────────────────────────

    private static class EmptyStarIcon implements javax.swing.Icon {
        private final int size;
        EmptyStarIcon(int size) { this.size = size; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Path2D star = StarRatingPanel.buildStar(x, y, size);
                g2.setColor(new Color(203, 213, 225));
                g2.fill(star);
                g2.setColor(new Color(155, 170, 185));
                g2.setStroke(new BasicStroke(2.0f));
                g2.draw(star);
            } finally {
                g2.dispose();
            }
        }

        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }
    }
}
