package citybites.dao;

import citybites.model.FeaturedAssignmentResult;
import citybites.model.FoodItem;
import java.util.List;
import java.util.Optional;

/**
 * Data-access contract for the food_items table.
 */
public interface FoodItemDAO {

    List<FoodItem> findAll();

    List<FoodItem> findAvailable();

    Optional<FoodItem> findById(int foodId);

    int insert(FoodItem item);

    boolean update(FoodItem item);

    boolean delete(int foodId);

    int countAll();

    int countAvailable();

    // ── Featured-foods (dashboard) ───────────────────────────────────────────

    /** Returns available foods with a non-null featured_position, ordered by position ASC. */
    List<FoodItem> findFeatured();

    /**
     * Returns the food_id of whatever food currently holds {@code slot} (excluding
     * {@code excludeFoodId}), or {@code 0} when the slot is unoccupied.
     *
     * <p>This is a plain read — no locking, no transaction.  Use it as a non-destructive
     * pre-flight check before showing a confirmation dialog to the user.
     */
    int findSlotOccupant(int slot, int excludeFoodId);

    /**
     * Atomically inserts a new food item and, when {@code featuredPosition} is non-null,
     * assigns it to the requested dashboard slot — all within a single JDBC transaction.
     *
     * <p>The occupancy check is performed <em>before</em> the INSERT so that, on a
     * conflict with {@code replaceOccupied=false}, the transaction is rolled back
     * and the database is left completely unchanged (no orphan food row).
     *
     * @return the new {@code food_id} on success; {@code 0} when {@code featuredPosition}
     *         is non-null, the slot is already taken, and {@code replaceOccupied} is
     *         {@code false} — in which case no DB change of any kind has been made.
     * @throws RuntimeException on any SQL error (after rolling back)
     */
    int insertWithFeaturedPosition(FoodItem item, Integer featuredPosition, boolean replaceOccupied);

    /**
     * Transactionally assigns {@code targetPosition} (1–4) to {@code foodId}, or clears
     * the slot when {@code targetPosition} is null.
     *
     * <p>All reads and writes share a single JDBC {@link java.sql.Connection} with
     * {@code autoCommit=false}.  Relevant rows are locked with {@code FOR UPDATE} before
     * any mutation so concurrent requests cannot interleave.  The connection is committed
     * on success and rolled back on any failure; {@code autoCommit} is always restored.
     *
     * @param replaceOccupied when {@code false} and the target slot is already taken,
     *                        the method returns
     *                        {@link FeaturedAssignmentResult.Status#SLOT_OCCUPIED} without
     *                        touching the database; when {@code true} the occupying food is
     *                        displaced (its slot is set to NULL).
     */
    FeaturedAssignmentResult assignFeaturedPosition(
            int foodId, Integer targetPosition, boolean replaceOccupied);

    /**
     * Transactionally updates a food item's fields and its featured-slot assignment in a
     * single JDBC transaction.
     *
     * <p>Algorithm (all within one transaction, {@code FOR UPDATE} locking throughout):
     * <ol>
     *   <li>Lock the food row; read its {@code currentSlot}.</li>
     *   <li>If {@code targetPosition} is non-null and differs from {@code currentSlot},
     *       lock the target slot's current occupant (if any).</li>
     *   <li>If the slot is occupied and {@code replaceOccupied} is {@code false},
     *       roll back and return {@link FeaturedAssignmentResult.Status#SLOT_OCCUPIED}
     *       — no DB state is changed.</li>
     *   <li>Execute the field UPDATE (food_name, price, …).</li>
     *   <li>Adjust the slot: clear the food's old slot, displace any occupant, assign
     *       the new slot (or null).</li>
     *   <li>Commit and return {@link FeaturedAssignmentResult.Status#ASSIGNED} (or
     *       {@link FeaturedAssignmentResult.Status#CLEARED} when {@code targetPosition}
     *       is null).</li>
     * </ol>
     *
     * @throws RuntimeException on any SQL error (after rolling back)
     */
    FeaturedAssignmentResult updateWithFeaturedPosition(
            FoodItem item, Integer targetPosition, boolean replaceOccupied);
}
