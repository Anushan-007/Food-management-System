package citybites.util;

import java.io.*;
import java.nio.file.*;
import java.util.Set;
import java.util.logging.*;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;

/**
 * Manages customer profile images.
 *
 * <p>Profile images are kept in a <strong>separate</strong> directory from food images:
 * {@code ~/.citybites/profile-images}.  MySQL only ever stores the short relative
 * filename (e.g. {@code "1788079424637_avatar.jpg"}).  Absolute paths are never
 * persisted.
 *
 * <p>Key guarantees:
 * <ul>
 *   <li>Profile images are completely isolated from food images; operations on
 *       one directory cannot affect the other.</li>
 *   <li>Only {@code .jpg}, {@code .jpeg} and {@code .png} are accepted.</li>
 *   <li>Path-traversal attacks are blocked in {@link #resolveImage}.</li>
 *   <li>Import copies the source file — the original is never modified or deleted.</li>
 *   <li>Preview ({@link #loadScaledPreview}) reads the source file directly without
 *       touching the managed directory — use this before Save.</li>
 *   <li>Import ({@link #importImage}) happens only on an explicit Save; never on
 *       file selection.</li>
 * </ul>
 */
public class ProfileImageManager {

    private static final Logger logger = Logger.getLogger(ProfileImageManager.class.getName());

    /**
     * Managed directory for customer profile images.
     * Completely separate from {@link ImageManager#MANAGED_DIR} (food images).
     */
    public static final Path PROFILE_DIR = Path.of(
            System.getProperty("user.home"), ".citybites", "profile-images");

    /** Accepted image extensions (lower-case, without leading dot). No GIF. */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    static {
        try {
            Files.createDirectories(PROFILE_DIR);
        } catch (IOException e) {
            logger.warning("Could not create profile image directory: "
                           + PROFILE_DIR + " — " + e.getMessage());
        }
    }

    private ProfileImageManager() {}

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Imports {@code source} into the profile image directory.
     *
     * <p>Call this <strong>only on Save</strong>, never on file selection.
     *
     * @param source absolute path to the user-chosen source image
     * @return relative filename stored in the profile-images directory
     * @throws IOException if the source is missing, unsupported, or the copy fails
     */
    public static String importImage(Path source) throws IOException {
        if (!Files.exists(source) || !Files.isRegularFile(source)) {
            throw new IOException(
                "Source image does not exist or is not a regular file: " + source);
        }
        String ext = extension(source.getFileName().toString());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IOException(
                "Unsupported image format '." + ext
                + "'. Accepted formats: jpg, jpeg, png.");
        }
        Files.createDirectories(PROFILE_DIR);
        String sanitized = sanitize(source.getFileName().toString());
        String fileName  = System.currentTimeMillis() + "_" + sanitized;
        Path   target    = PROFILE_DIR.resolve(fileName);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Profile image imported: " + fileName);
        return fileName;
    }

    /**
     * Converts the stored relative filename into the absolute managed {@link Path}.
     *
     * <ul>
     *   <li>Returns {@code null} for {@code null} or blank input.</li>
     *   <li>Blocks {@code ..} path-traversal attempts.</li>
     *   <li>Returns {@code null} if the file does not exist.</li>
     * </ul>
     *
     * @param storedPath value stored in MySQL (e.g. {@code "1788079424637_avatar.jpg"})
     * @return absolute Path inside the managed directory, or {@code null}
     */
    public static Path resolveImage(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) return null;
        try {
            Path resolved = PROFILE_DIR.resolve(storedPath).normalize();
            if (!resolved.startsWith(PROFILE_DIR)) {
                logger.warning("Path traversal attempt blocked: '" + storedPath + "'");
                return null;
            }
            return Files.exists(resolved) ? resolved : null;
        } catch (Exception e) {
            logger.warning("Cannot resolve profile image '" + storedPath
                           + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Loads and scales a managed profile image for display.
     * Falls back to a placeholder on any failure (missing file, corrupt data, null input).
     *
     * @param storedPath managed relative filename from MySQL
     * @param width  target width in pixels
     * @param height target height in pixels
     */
    public static ImageIcon loadScaled(String storedPath, int width, int height) {
        Path p = resolveImage(storedPath);
        if (p != null) {
            // loadScaledPreview reads from any File — reuse it since we already resolved the path
            return ImageManager.loadScaledPreview(p.toFile(), width, height);
        }
        return ImageManager.placeholder(width, height);
    }

    /**
     * Safely deletes a managed profile image file.
     * Only deletes files confirmed to be inside {@link #PROFILE_DIR}.
     * Never touches source files, food images, or files outside the profile directory.
     *
     * @param storedPath managed relative filename to delete
     */
    public static void deleteProfileImage(String storedPath) {
        Path p = resolveImage(storedPath);
        if (p != null) {
            try {
                Files.deleteIfExists(p);
                logger.info("Deleted profile image: " + storedPath);
            } catch (IOException e) {
                logger.warning("Could not delete profile image '"
                               + storedPath + "': " + e.getMessage());
            }
        }
    }

    // ── Helpers (public for tests) ─────────────────────────────────────────────

    /**
     * Returns the lower-case extension (without dot) of {@code filename},
     * or {@code ""} if no dot is present.
     */
    public static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0 && dot < filename.length() - 1)
               ? filename.substring(dot + 1).toLowerCase()
               : "";
    }

    /** Replaces characters not safe in a filename with underscores. */
    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
