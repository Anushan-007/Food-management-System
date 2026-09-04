package citybites.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import javax.swing.*;

/**
 * A lightweight Swing panel that paints a 1–5 star rating using Graphics2D.
 * Avoids Unicode characters (★ ☆) that render as squares on Windows.
 *
 * <p><b>Read-only mode</b> (admin review dialog, food management table):
 * <pre>new StarRatingPanel(4)  // four filled stars at 16 px</pre>
 *
 * <p><b>Interactive mode</b> (customer rating dialog):
 * <pre>StarRatingPanel.interactive(existingRating)  // 26 px, click/hover/keyboard</pre>
 */
public class StarRatingPanel extends JPanel {

    /** Default star size in pixels (read-only mode). */
    public static final int STAR_SIZE = 16;

    /** Gap between stars in pixels. */
    private static final int GAP = 2;

    /** Total number of stars always displayed. */
    public static final int STAR_COUNT = 5;

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color FILLED_COLOR = AppTheme.BRAND_ACCENT;         // orange
    private static final Color HOVER_COLOR  = new Color(240, 150, 50);        // lighter orange on hover
    private static final Color EMPTY_COLOR  = new Color(203, 213, 225);       // light grey
    private static final Color STROKE_COLOR = new Color(170, 183, 197);       // slightly darker grey

    // ── State ─────────────────────────────────────────────────────────────────
    private final boolean interactive;
    private final int     starSize;
    private       int     currentRating;   // 0–5; mutable in interactive mode
    private       int     hoverRating;     // 0 = no hover (interactive only)
    private       Runnable onRatingChanged; // nullable callback

    // ─────────────────────────────────────────────────────────────────────────
    // Constructors
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Read-only constructor — backward compatible with all existing admin code.
     *
     * @param rating value between 0 and 5 (clamped to this range)
     */
    public StarRatingPanel(int rating) {
        this(rating, STAR_SIZE, false);
    }

    /**
     * Interactive factory — creates 26 px clickable stars pre-loaded with the given rating.
     *
     * @param initialRating 0 = no selection yet; 1–5 = pre-selected
     */
    public static StarRatingPanel interactive(int initialRating) {
        return new StarRatingPanel(initialRating, 26, true);
    }

    /** Master constructor — private. */
    private StarRatingPanel(int initialRating, int starSize, boolean interactive) {
        this.currentRating = Math.max(0, Math.min(5, initialRating));
        this.starSize      = starSize;
        this.interactive   = interactive;

        setOpaque(false);
        int totalWidth = STAR_COUNT * starSize + (STAR_COUNT - 1) * GAP;
        Dimension sz   = new Dimension(totalWidth, starSize);
        setPreferredSize(sz);
        setMinimumSize(sz);
        if (!interactive) setMaximumSize(sz); // admin: constrain to exact size

        if (interactive) setupInteraction();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns the current selected rating (0 = none, 1–5 = selected). */
    public int getRating() {
        return currentRating;
    }

    /**
     * Programmatically sets the rating and fires the change callback.
     * Only has effect in interactive mode; clamped to 0–5.
     */
    public void setRating(int r) {
        if (!interactive) return;
        int clamped = Math.max(0, Math.min(5, r));
        if (currentRating != clamped) {
            currentRating = clamped;
            repaint();
            if (onRatingChanged != null) onRatingChanged.run();
        }
    }

    /** Registers a callback invoked whenever the rating changes (interactive mode). */
    public void setOnRatingChanged(Runnable callback) {
        this.onRatingChanged = callback;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Interaction setup
    // ─────────────────────────────────────────────────────────────────────────

    private void setupInteraction() {
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(true);
        ToolTipManager.sharedInstance().registerComponent(this);

        // Mouse click + exit
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int star = starAt(e.getX());
                if (star >= 1) {
                    currentRating = star;
                    hoverRating   = star;
                    repaint();
                    if (onRatingChanged != null) onRatingChanged.run();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (hoverRating != 0) {
                    hoverRating = 0;
                    repaint();
                }
            }
        });

        // Hover preview
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int star = starAt(e.getX());
                if (hoverRating != star) {
                    hoverRating = star;
                    repaint();
                }
            }
        });

        // Keyboard: Left/Down = −1, Right/Up = +1, keys 1–5 = set directly
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_LEFT  || key == KeyEvent.VK_DOWN) {
                    setRating(currentRating - 1);
                } else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_UP) {
                    setRating(currentRating + 1);
                } else if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_5) {
                    setRating(key - KeyEvent.VK_0);
                }
            }
        });

        // Focus ring repaint
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { repaint(); }
            @Override public void focusLost(FocusEvent e)   { repaint(); }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Painting
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);

            int displayRating = (interactive && hoverRating > 0) ? hoverRating : currentRating;
            boolean hovering  = interactive && hoverRating > 0;

            for (int i = 0; i < STAR_COUNT; i++) {
                int    x    = i * (starSize + GAP);
                Path2D star = buildStar(x, 0, starSize);
                boolean filled = i < displayRating;

                if (filled) {
                    Color fc = hovering ? HOVER_COLOR : FILLED_COLOR;
                    g2.setColor(fc);
                    g2.fill(star);
                    g2.setColor(fc.darker());
                    g2.setStroke(new BasicStroke(0.6f));
                    g2.draw(star);
                } else {
                    g2.setColor(EMPTY_COLOR);
                    g2.fill(star);
                    g2.setColor(STROKE_COLOR);
                    g2.setStroke(new BasicStroke(0.8f));
                    g2.draw(star);
                }
            }

            // Focus ring for interactive mode
            if (interactive && isFocusOwner()) {
                g2.setColor(AppTheme.BORDER_FOCUS);
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        0, new float[]{3, 3}, 0));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
            }
        } finally {
            g2.dispose();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tooltip per star
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public String getToolTipText(MouseEvent e) {
        if (!interactive) return null;
        int star = starAt(e.getX());
        return star >= 1 ? "Rate " + star + " out of 5" : null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the star index (1–5) at the given x coordinate, or 0 if not over any star.
     */
    private int starAt(int x) {
        int cellWidth = starSize + GAP;
        int star = x / cellWidth + 1;
        return (star >= 1 && star <= STAR_COUNT) ? star : 0;
    }

    /**
     * Builds a five-point star Path2D centred inside an (x, y, size, size) box.
     *
     * @param x    left edge of the bounding box
     * @param y    top edge of the bounding box
     * @param size width and height of the bounding box
     * @return the path
     */
    public static Path2D buildStar(double x, double y, double size) {
        double cx     = x + size / 2.0;
        double cy     = y + size / 2.0;
        double outerR = size / 2.0 * 0.95;
        double innerR = outerR * 0.40;

        Path2D path = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI / 2 * 3 + i * Math.PI / 5; // start at top
            double r     = (i % 2 == 0) ? outerR : innerR;
            double px    = cx + r * Math.cos(angle);
            double py    = cy + r * Math.sin(angle);
            if (i == 0) path.moveTo(px, py);
            else        path.lineTo(px, py);
        }
        path.closePath();
        return path;
    }
}
