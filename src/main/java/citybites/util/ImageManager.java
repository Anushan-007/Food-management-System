package citybites.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * Manages food images stored in a local folder next to the application.
 * Only the image filename is stored in MySQL; the folder path is resolved at runtime.
 */
public class ImageManager {

    private static final Logger logger = Logger.getLogger(ImageManager.class.getName());
    private static final String IMAGES_DIR =
            System.getProperty("user.dir") + File.separator + "food-images";

    static {
        File dir = new File(IMAGES_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            logger.warning("Could not create food-images directory: " + IMAGES_DIR);
        }
    }

    private ImageManager() {}

    /**
     * Copies the source image file into the managed folder.
     * Returns the stored filename (not a full path).
     */
    public static String copyImage(File source) throws IOException {
        String fileName = System.currentTimeMillis() + "_" + sanitize(source.getName());
        Path   target   = Paths.get(IMAGES_DIR, fileName);
        Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Image copied: " + fileName);
        return fileName;
    }

    /**
     * Resolves a stored filename to an absolute File.
     * Returns null if the file does not exist.
     */
    public static File resolve(String fileName) {
        if (fileName == null || fileName.isBlank()) return null;
        File f = new File(IMAGES_DIR, fileName);
        return f.exists() ? f : null;
    }

    /**
     * Loads and scales an image for display.
     * Returns a placeholder ImageIcon if the image cannot be loaded.
     */
    public static ImageIcon loadScaled(String fileName, int width, int height) {
        File f = resolve(fileName);
        if (f != null) {
            try {
                BufferedImage img = ImageIO.read(f);
                if (img != null) {
                    return new ImageIcon(scaleSmoothly(img, width, height));
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not load image: " + fileName, e);
            }
        }
        return placeholder(width, height);
    }

    /** Creates a light-gray placeholder ImageIcon. */
    public static ImageIcon placeholder(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new java.awt.Color(236, 240, 241));
        g.fillRect(0, 0, width, height);
        g.setColor(new java.awt.Color(150, 160, 170));
        g.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 11));
        java.awt.FontMetrics fm = g.getFontMetrics();
        String text = "No Image";
        g.drawString(text, (width - fm.stringWidth(text)) / 2, height / 2 + fm.getAscent() / 2);
        g.dispose();
        return new ImageIcon(img);
    }

    // Private helpers

    private static Image scaleSmoothly(BufferedImage src, int w, int h) {
        double srcW = src.getWidth();
        double srcH = src.getHeight();
        double scale = Math.min(w / srcW, h / srcH);
        int    sw    = (int) (srcW * scale);
        int    sh    = (int) (srcH * scale);

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setColor(new java.awt.Color(236, 240, 241));
        g.fillRect(0, 0, w, h);
        g.drawImage(src.getScaledInstance(sw, sh, Image.SCALE_SMOOTH),
                    (w - sw) / 2, (h - sh) / 2, null);
        g.dispose();
        return out;
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
