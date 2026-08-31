package citybites.service;

import citybites.dao.FoodCategoryDAO;
import citybites.dao.impl.FoodCategoryDAOImpl;
import citybites.model.FoodCategory;
import java.util.List;
import java.util.Optional;

/**
 * Handles CRUD operations for food categories.
 */
public class FoodCategoryService {

    private static final FoodCategoryDAO dao = new FoodCategoryDAOImpl();

    /** Maximum allowed description length (matches the VARCHAR(255) column). */
    public static final int MAX_DESCRIPTION_LENGTH = 255;

    private FoodCategoryService() {}

    public static List<FoodCategory> getAllCategories() {
        return dao.findAll();
    }

    public static Optional<FoodCategory> getCategoryById(int id) {
        return dao.findById(id);
    }

    /**
     * Adds a category with the given name and no description.
     * Convenience overload — delegates to {@link #addCategory(String, String)}.
     */
    public static int addCategory(String name) {
        return addCategory(name, null);
    }

    /**
     * Adds a category with the given name and optional description.
     *
     * @throws IllegalArgumentException if the name is blank, the description exceeds
     *                                  255 characters, or another category with the
     *                                  same name already exists (case-insensitive)
     */
    public static int addCategory(String name, String description) {
        String trimName = (name == null) ? "" : name.trim();
        if (trimName.isEmpty()) {
            throw new IllegalArgumentException("Category name must not be blank.");
        }
        String trimDesc = normaliseDescription(description);
        validateDescription(trimDesc);
        if (dao.existsByNameCaseInsensitive(trimName, -1)) {
            throw new IllegalArgumentException("A category with this name already exists.");
        }
        return dao.insert(trimName, trimDesc);
    }

    /**
     * Updates a category's name, leaving its description unchanged.
     * Convenience overload — delegates to {@link #updateCategory(int, String, String)}.
     */
    public static boolean updateCategory(int id, String name) {
        // Fetch the existing description so the convenience overload never silently clears it
        String existingDesc = dao.findById(id)
                .map(FoodCategory::getDescription)
                .orElse(null);
        return updateCategory(id, name, existingDesc);
    }

    /**
     * Updates a category's name and description.
     *
     * @throws IllegalArgumentException if the name is blank, the description exceeds
     *                                  255 characters, or another category (not {@code id})
     *                                  already has the same name (case-insensitive)
     */
    public static boolean updateCategory(int id, String name, String description) {
        String trimName = (name == null) ? "" : name.trim();
        if (trimName.isEmpty()) {
            throw new IllegalArgumentException("Category name must not be blank.");
        }
        String trimDesc = normaliseDescription(description);
        validateDescription(trimDesc);
        if (dao.existsByNameCaseInsensitive(trimName, id)) {
            throw new IllegalArgumentException("A category with this name already exists.");
        }
        return dao.update(id, trimName, trimDesc);
    }

    /**
     * Deletes the category with the given ID.
     *
     * @throws IllegalStateException if one or more food items are still assigned
     *                               to this category (deletion would orphan them)
     */
    public static boolean deleteCategory(int id) {
        if (dao.isCategoryInUse(id)) {
            throw new IllegalStateException(
                "This category is assigned to food items and cannot be deleted.");
        }
        return dao.delete(id);
    }

    /**
     * Returns the category_id of the built-in "Other" category.
     *
     * @throws IllegalStateException if the "Other" category does not exist in the
     *                               database (database was not properly initialised)
     */
    public static int getOtherCategoryId() {
        return dao.findByName("Other")
            .map(FoodCategory::getCategoryId)
            .orElseThrow(() -> new IllegalStateException(
                "'Other' category not found. Ensure the database has been properly initialised."));
    }

    /** Returns {@code true} if at least one food item references this category. */
    public static boolean isCategoryInUse(int id) {
        return dao.isCategoryInUse(id);
    }

    /**
     * Returns {@code true} if a category with the given ID exists in food_categories.
     * Used by FoodService to validate category references before INSERT / UPDATE.
     */
    public static boolean categoryExistsById(int id) {
        return dao.existsById(id);
    }

    // ── Private helpers ────────────────────────────────────────────────────

    /** Trims the description and converts blank strings to {@code null}. */
    private static String normaliseDescription(String description) {
        if (description == null) return null;
        String t = description.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * Validates that the description (already trimmed) does not exceed the column limit.
     *
     * @throws IllegalArgumentException if the description exceeds 255 characters
     */
    private static void validateDescription(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                "Description must not exceed " + MAX_DESCRIPTION_LENGTH + " characters.");
        }
    }
}
