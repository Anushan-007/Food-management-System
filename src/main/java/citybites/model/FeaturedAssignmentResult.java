package citybites.model;

/**
 * Immutable value object returned by the transactional
 * {@code FoodItemDAO.assignFeaturedPosition} operation.
 *
 * <ul>
 *   <li>{@link Status#ASSIGNED}     – the food was successfully placed in the requested slot;
 *       {@link #getRelatedFoodId()} carries the displaced food's ID (null if no displacement).</li>
 *   <li>{@link Status#CLEARED}      – the food's slot was cleared (null target position).</li>
 *   <li>{@link Status#SLOT_OCCUPIED}– the requested slot is occupied and
 *       {@code replaceOccupied} was {@code false}; no DB state was changed.
 *       {@link #getRelatedFoodId()} identifies the current occupant.</li>
 * </ul>
 */
public final class FeaturedAssignmentResult {

    public enum Status { ASSIGNED, CLEARED, SLOT_OCCUPIED }

    private final Status  status;
    private final Integer relatedFoodId; // displaced food (ASSIGNED) | occupant (SLOT_OCCUPIED) | null (CLEARED)

    private FeaturedAssignmentResult(Status status, Integer relatedFoodId) {
        this.status       = status;
        this.relatedFoodId = relatedFoodId;
    }

    /** Food was placed in the target slot; {@code displacedFoodId} is null when no food was displaced. */
    public static FeaturedAssignmentResult assigned(Integer displacedFoodId) {
        return new FeaturedAssignmentResult(Status.ASSIGNED, displacedFoodId);
    }

    /** Food's featured slot was cleared (target position was null). */
    public static FeaturedAssignmentResult cleared() {
        return new FeaturedAssignmentResult(Status.CLEARED, null);
    }

    /** The requested slot is occupied by {@code occupantFoodId}; no changes were made. */
    public static FeaturedAssignmentResult slotOccupied(int occupantFoodId) {
        return new FeaturedAssignmentResult(Status.SLOT_OCCUPIED, occupantFoodId);
    }

    public Status  getStatus()        { return status;        }
    public Integer getRelatedFoodId() { return relatedFoodId; }

    /** {@code true} for ASSIGNED and CLEARED; {@code false} for SLOT_OCCUPIED. */
    public boolean isSuccess() {
        return status == Status.ASSIGNED || status == Status.CLEARED;
    }
}
