package citybites.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Set;
import java.util.logging.*;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * Centralized image management for City Bites food images.
 *
 * <p>Images are stored in a stable user-specific managed directory:
 * {@code ~/.citybites/food-images}.  MySQL only ever stores the short
 * relative filename (e.g. {@code 1788079424637_cheese-pizza.jpg}).
 * The full absolute path is <strong>never</strong> persisted in the database.
 *
 * <p>Public API:
 * <ul>
 *   <li>{@link #importImage(Path)} – copy a source file into the managed
 *       directory; returns only the managed relative filename.</li>
 *   <li>{@link #resolveImage(String)} – convert the stored filename to the
 *       absolute managed {@link Path}; rejects path-traversal attempts.</li>
 *   <li>{@link #loadScaled(String, int, int)} – load a managed image for
 *       display; falls back to a placeholder on any failure.</li>
 *   <li>{@link #loadScaledPreview(File, int, int)} – preview a source file
 *       without touching the managed directory (pre-save only).</li>
 *   <li>{@link #deleteManagedImage(String)} – safely delete a managed image.</li>
 *   <li>{@link #placeholder(int, int)} – generate a grey placeholder icon.</li>
 * </ul>
 */
public class ImageManager {

    private static final Logger logger = Logger.getLogger(ImageManager.class.getName());

    /**
     * Stable managed image base directory: {@code ~/.citybites/food-images}.
     * This is the only value that knows the full absolute path; everything
     * stored in MySQL is a filename relative to this directory.
     */
    public static final Path MANAGED_DIR = Path.of(
            System.getProperty("user.home"), ".citybites", "food-images");

    /** Accepted image extensions (lower-case, without leading dot). */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    static {
        try {
            Files.createDirectories(MANAGED_DIR);
        } catch (IOException e) {
            logger.warning("Could not create managed image directory: "
                           + MANAGED_DIR + " — " + e.getMessage());
        }
    }

    private ImageManager() {}

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Imports {@code source} into the managed image directory.
     *
     * <ol>
     *   <li>Validates that {@code source} exists and is a regular file.</li>
     *   <li>Accepts only {@code .jpg}, {@code .jpeg} and {@code .png}.</li>
     *   <li>Creates the managed directory when missing.</li>
     *   <li>Generates a collision-safe, sanitized filename.</li>
     *   <li>Copies the source file — the original is never modified or deleted.</li>
     *   <li>Returns <strong>only</strong> the managed relative filename, never
     *       the original absolute path.</li>
     * </ol>
     *
     * @param source absolute path to the user-chosen source image
     * @return managed relative filename (e.g. {@code "1788079424637_burger.jpg"})
     * @throws IOException if the source is missing, unsupported, or the copy fails
     */
    public static String importImage(Path source) throws IOException {
        // 1. Source must exist and be a regular file
        if (!Files.exists(source) || !Files.isRegularFile(source)) {
            throw new IOException(
                "Source image does not exist or is not a regular file: " + source);
        }
        // 2. Accepted extensions only
        String ext = extension(source.getFileName().toString());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IOException(
                "Unsupported image format '." + ext
                + "'. Accepted formats: jpg, jpeg, png.");
        }
        // 3. Ensure managed directory exists
        Files.createDirectories(MANAGED_DIR);
        // 4+5. Collision-safe, sanitized filename
        String sanitized = sanitize(source.getFileName().toString());
        String fileName  = System.currentTimeMillis() + "_" + sanitized;
        // 6. Copy to managed directory — source file is never touched
        Path target = MANAGED_DIR.resolve(fileName);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Image imported to managed directory: " + fileName);
        // 7. Return only the relative filename — never the original source path
        return fileName;
    }

    /**
     * Converts the stored relative filename into the absolute managed {@link Path}.
     *
     * <ul>
     *   <li>Returns {@code null} for {@code null} or blank input.</li>
     *   <li>Prevents {@code ..} path-traversal attacks.</li>
     *   <li>Returns {@code null} when the resolved file does not exist.</li>
     * </ul>
     *
     * @param storedPath value stored in MySQL (e.g. {@code "1788079424637_burger.jpg"})
     * @return absolute Path inside the managed directory, or {@code null}
     */
    public static Path resolveImage(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) return null;
        try {
            Path resolved = MANAGED_DIR.resolve(storedPath).normalize();
            // Security: reject anything that escapes the managed directory
            if (!resolved.startsWith(MANAGED_DIR)) {
                logger.warning("Path traversal attempt blocked: '" + storedPath + "'");
                return null;
            }
            return Files.exists(resolved) ? resolved : null;
        } catch (Exception e) {
            logger.warning("Cannot resolve image path '" + storedPath
                           + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Loads and scales a managed food image for display.
     * Falls back to a {@link #placeholder(int, int)} on any failure
     * (missing file, corrupt data, {@code null} input).
     *
     * @param storedPath managed relative filename from MySQL
     * @param width      target width in pixels
     * @param height     target height in pixels
     */
    public static ImageIcon loadScaled(String storedPath, int width, int height) {
        Path p = resolveImage(storedPath);
        if (p != null) {
            try {
                BufferedImage img = ImageIO.read(p.toFile());
                if (img != null) {
                    return new ImageIcon(scaleSmoothly(img, width, height));
                }
            } catch (IOException e) {
                logger.log(Level.WARNING,
                    "Could not load managed image: " + storedPath, e);
            }
        }
        return placeholder(width, height);
    }

    /**
     * Loads and scales a preview directly from a source {@link File}.
     *
     * <p>This is for pre-save preview only — it bypasses the managed directory
     * entirely.  The source file is never stored or modified.
     *
     * @param sourceFile file chosen by the user via {@link javax.swing.JFileChooser}
     * @param width      target width in pixels
     * @param height     target height in pixels
     */
    public static ImageIcon loadScaledPreview(File sourceFile, int width, int height) {
        if (sourceFile != null && sourceFile.exists()) {
            try {
                BufferedImage img = ImageIO.read(sourceFile);
                if (img != null) {
                    return new ImageIcon(scaleSmoothly(img, width, height));
                }
            } catch (IOException e) {
                logger.log(Level.WARNING,
                    "Could not preview source image: " + sourceFile, e);
            }
        }
        return placeholder(width, height);
    }

    /**
     * Safely deletes a managed image file.
     *
     * <p>Only deletes files that are confirmed to be inside the managed directory.
     * Never touches source files or files outside the managed directory.
     *
     * @param storedPath managed relative filename to delete
     */
    public static void deleteManagedImage(String storedPath) {
        Path p = resolveImage(storedPath);
        if (p != null) {
            try {
                Files.deleteIfExists(p);
                logger.info("Deleted managed image: " + storedPath);
            } catch (IOException e) {
                logger.warning("Could not delete managed image '"
                               + storedPath + "': " + e.getMessage());
            }
        }
    }

    /**
     * Creates a light-grey placeholder {@link ImageIcon} with a "No Image" label.
     * Used when the stored path is {@code null}, blank, or the file is missing.
     */
    public static ImageIcon placeholder(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(236, 240, 241));
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(150, 160, 170));
        g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        FontMetrics fm = g.getFontMetrics();
        String text = "No Image";
        g.drawString(text,
            (width  - fm.stringWidth(text)) / 2,
            height / 2 + fm.getAscent() / 2);
        g.dispose();
        return new ImageIcon(img);
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private static Image scaleSmoothly(BufferedImage src, int w, int h) {
        double scale = Math.min(w / (double) src.getWidth(),
                                h / (double) src.getHeight());
        int sw = (int) (src.getWidth()  * scale);
        int sh = (int) (src.getHeight() * scale);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setColor(new Color(236, 240, 241));
        g.fillRect(0, 0, w, h);
        g.drawImage(src.getScaledInstance(sw, sh, Image.SCALE_SMOOTH),
                    (w - sw) / 2, (h - sh) / 2, null);
        g.dispose();
        return out;
    }

    /**
     * Returns the lower-case extension (without leading dot) of {@code filename},
     * or {@code ""} if there is no dot.
     * Package-private for testing.
     */
    static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0 && dot < filename.length() - 1)
               ? filename.substring(dot + 1).toLowerCase()
               : "";
    }

    /** Replaces characters that are not alphanumeric, dot, underscore or hyphen. */
    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
