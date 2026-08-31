package citybites.dao;

import citybites.model.FoodCategory;
import java.util.List;
import java.util.Optional;

/**
 * Data-access contract for the food_categories table.
 */
public interface FoodCategoryDAO {

    List<FoodCategory> findAll();

    Optional<FoodCategory> findById(int id);

    /** Finds a category by exact name, or empty if not found. */
    Optional<FoodCategory> findByName(String name);

    /**
     * Returns {@code true} if any category other than {@code excludeId} has the
     * given name (case-insensitive). Pass {@code excludeId = -1} when adding a new
     * category so no ID is excluded.
     */
    boolean existsByNameCaseInsensitive(String name, int excludeId);

    /** Inserts a new category and returns the generated ID. */
    int insert(String name, String description);

    boolean update(int id, String name, String description);

    boolean delete(int id);

    /**
     * Returns {@code true} if at least one food_items row references this category.
     * Used to prevent deletion of categories that are still assigned to food items.
     * Fails safe — returns {@code true} (preventing deletion) on any database error.
     */
    boolean isCategoryInUse(int id);

    /**
     * Returns {@code true} if a row with the given {@code category_id} exists,
     * {@code false} if the query succeeds and no row is found.
     *
     * @throws RuntimeException wrapping the original {@link java.sql.SQLException}
     *                          if a database error occurs, so that callers do not
     *                          silently proceed with a stale / optimistic result
     */
    boolean existsById(int id);
}
