package citybites.dao.impl;

import citybites.config.DatabaseConnection;
import citybites.dao.FoodItemDAO;
import citybites.model.FeaturedAssignmentResult;
import citybites.model.FoodItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FoodItemDAOImpl implements FoodItemDAO {

    private static final Logger logger = Logger.getLogger(FoodItemDAOImpl.class.getName());

    /** Base SELECT with LEFT JOIN for category name and featured_position. */
    private static final String BASE_SQL =
        "SELECT fi.food_id, fi.food_name, fi.price, fi.available, " +
        "       fi.stock_quantity, fi.image_path, fi.category_id, " +
        "       COALESCE(fc.category_name, 'Uncategorized') AS category_name, " +
        "       fi.featured_position " +
        "FROM food_items fi " +
        "LEFT JOIN food_categories fc ON fi.category_id = fc.category_id";

    @Override
    public List<FoodItem> findAll() {
        List<FoodItem> list = new ArrayList<>();
        String sql = BASE_SQL + " ORDER BY fi.food_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodItemDAO.findAll failed.", e);
        }
        return list;
    }

    @Override
    public List<FoodItem> findAvailable() {
        List<FoodItem> list = new ArrayList<>();
        String sql = BASE_SQL + " WHERE fi.available = 1 ORDER BY fi.food_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodItemDAO.findAvailable failed.", e);
        }
        return list;
    }

    @Override
    public Optional<FoodItem> findById(int foodId) {
        String sql = BASE_SQL + " WHERE fi.food_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, foodId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodItemDAO.findById failed.", e);
        }
        return Optional.empty();
    }

    @Override
    public int insert(FoodItem item) {
        String sql = "INSERT INTO food_items " +
                     "(food_name, price, available, stock_quantity, image_path, category_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1,  item.getFoodName());
            ps.setDouble(2,  item.getPrice());
            ps.setBoolean(3, item.isAvailable());
            ps.setInt(4,     item.getStockQuantity());
            ps.setString(5,  item.getImagePath());
            if (item.getCategoryId() > 0) {
                ps.setInt(6, item.getCategoryId());
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodItemDAO.insert failed.", e);
            throw new RuntimeException("Could not add food item: " + e.getMessage(), e);
        }
        return -1;
    }

    @Override
    public boolean update(FoodItem item) {
        String sql = "UPDATE food_items " +
                     "SET food_name=?, price=?, available=?, stock_quantity=?, " +
                     "    image_path=?, category_id=? " +
                     "WHERE food_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,  item.getFoodName());
            ps.setDouble(2,  item.getPrice());
            ps.setBoolean(3, item.isAvailable());
            ps.setInt(4,     item.getStockQuantity());
            ps.setString(5,  item.getImagePath());
            if (item.getCategoryId() > 0) {
                ps.setInt(6, item.getCategoryId());
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }
            ps.setInt(7, item.getFoodId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodItemDAO.update failed.", e);
            throw new RuntimeException("Could not update food item: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(int foodId) {
        String sql = "DELETE FROM food_items WHERE food_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, foodId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodItemDAO.delete failed.", e);
            throw new RuntimeException("Could not delete food item: " + e.getMessage(), e);
        }
    }

    @Override
    public int countAll() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM food_items");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodItemDAO.countAll failed.", e);
        }
        return 0;
    }

    @Override
    public int countAvailable() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM food_items WHERE available = 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodItemDAO.countAvailable failed.", e);
        }
        return 0;
    }

    // ── Featured-foods ────────────────────────────────────────────────────────

    @Override
    public List<FoodItem> findFeatured() {
        List<FoodItem> list = new ArrayList<>();
        String sql = BASE_SQL +
            " WHERE fi.featured_position IS NOT NULL AND fi.available = 1" +
            " ORDER BY fi.featured_position ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodItemDAO.findFeatured failed.", e);
        }
        return list;
    }

    /**
     * Non-locking read: returns the food_id of the food holding {@code slot}, excluding
     * {@code excludeFoodId}, or {@code 0} when the slot is free.
     * Used as a pre-flight check in the UI so a confirmation dialog can be shown
     * before any mutations are attempted.
     */
    @Override
    public int findSlotOccupant(int slot, int excludeFoodId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT food_id FROM food_items " +
                 "WHERE featured_position = ? AND food_id != ?")) {
            ps.setInt(1, slot);
            ps.setInt(2, excludeFoodId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("food_id");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodItemDAO.findSlotOccupant failed.", e);
        }
        return 0;
    }

    /**
     * Atomically inserts a food item and assigns it to a featured slot in one transaction.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>If {@code featuredPosition} is non-null, lock the target slot occupant
     *       (if any) with {@code FOR UPDATE} <em>before</em> the INSERT.</li>
     *   <li>If the slot is occupied and {@code replaceOccupied} is {@code false},
     *       rollback immediately — the database is unchanged — and return {@code 0}.</li>
     *   <li>INSERT the food row; obtain the generated key.</li>
     *   <li>Displace the previous occupant (if any) and assign the new food to the slot.</li>
     *   <li>Commit and return the new food_id.</li>
     * </ol>
     * Any {@link SQLException} triggers a rollback and re-throws as a {@link RuntimeException}.
     * {@code autoCommit} is always restored.
     */
    @Override
    public int insertWithFeaturedPosition(
            FoodItem item, Integer featuredPosition, boolean replaceOccupied) {
        Connection conn = null;
        boolean prevAutoCommit = true;
        try {
            conn = DatabaseConnection.getConnection();
            prevAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            // ── 1. Pre-check slot before the INSERT ───────────────────────────
            Integer occupantId = null;
            if (featuredPosition != null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT food_id FROM food_items " +
                        "WHERE featured_position = ? FOR UPDATE")) {
                    ps.setInt(1, featuredPosition);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) occupantId = rs.getInt("food_id");
                    }
                }
                // Slot is occupied and caller has not authorised displacement → rollback
                if (occupantId != null && !replaceOccupied) {
                    conn.rollback();
                    return 0; // sentinel: SLOT_OCCUPIED, no DB change
                }
            }

            // ── 2. Insert the food row ────────────────────────────────────────
            String sql = "INSERT INTO food_items " +
                         "(food_name, price, available, stock_quantity, image_path, category_id) " +
                         "VALUES (?, ?, ?, ?, ?, ?)";
            int newFoodId;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1,  item.getFoodName());
                ps.setDouble(2,  item.getPrice());
                ps.setBoolean(3, item.isAvailable());
                ps.setInt(4,     item.getStockQuantity());
                ps.setString(5,  item.getImagePath());
                if (item.getCategoryId() > 0) {
                    ps.setInt(6, item.getCategoryId());
                } else {
                    ps.setNull(6, java.sql.Types.INTEGER);
                }
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("INSERT returned no generated key.");
                    }
                    newFoodId = keys.getInt(1);
                }
            }

            // ── 3. Assign the featured slot (if requested) ────────────────────
            if (featuredPosition != null) {
                // Displace existing occupant
                if (occupantId != null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE food_items SET featured_position = NULL WHERE food_id = ?")) {
                        ps.setInt(1, occupantId);
                        ps.executeUpdate();
                    }
                }
                // Assign the new food to the slot
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE food_items SET featured_position = ? WHERE food_id = ?")) {
                    ps.setInt(1, featuredPosition);
                    ps.setInt(2, newFoodId);
                    ps.executeUpdate();
                }
            }

            conn.commit();
            return newFoodId;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {
                    logger.log(Level.SEVERE, "insertWithFeaturedPosition: rollback failed.", ex);
                }
            }
            logger.log(Level.SEVERE, "FoodItemDAO.insertWithFeaturedPosition failed.", e);
            throw new RuntimeException("Could not add food item: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(prevAutoCommit); } catch (SQLException ex) {
                    logger.log(Level.SEVERE,
                            "insertWithFeaturedPosition: could not restore autoCommit.", ex);
                }
                try { conn.close(); } catch (SQLException ex) {
                    logger.log(Level.SEVERE,
                            "insertWithFeaturedPosition: could not close connection.", ex);
                }
            }
        }
    }

    /**
     * Transactionally assigns {@code targetPosition} to {@code foodId} using a single
     * JDBC connection.
     *
     * <p>Algorithm (all within one transaction):
     * <ol>
     *   <li>If {@code targetPosition} is null → clear the food's slot and return CLEARED.</li>
     *   <li>Lock the food row with {@code FOR UPDATE}; read its current slot.</li>
     *   <li>If the food is already at the target slot → commit (no-op) and return ASSIGNED.</li>
     *   <li>Lock the target slot's current occupant (if any) with {@code FOR UPDATE}.</li>
     *   <li>If the slot is occupied and {@code replaceOccupied} is false → rollback and
     *       return SLOT_OCCUPIED (no DB state changed).</li>
     *   <li>Clear the food's previous slot (if it had one).</li>
     *   <li>Displace the occupant (if any).</li>
     *   <li>Assign the food to the target slot.</li>
     *   <li>Commit and return ASSIGNED (with the displaced food's ID if any).</li>
     * </ol>
     * Any {@link SQLException} triggers a rollback and re-throws as a {@link RuntimeException}.
     * {@code autoCommit} is always restored in the {@code finally} block.
     */
    @Override
    public FeaturedAssignmentResult assignFeaturedPosition(
            int foodId, Integer targetPosition, boolean replaceOccupied) {
        Connection conn = null;
        boolean prevAutoCommit = true;
        try {
            conn = DatabaseConnection.getConnection();
            prevAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            // ── 1. Clear case ─────────────────────────────────────────────────
            if (targetPosition == null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE food_items SET featured_position = NULL WHERE food_id = ?")) {
                    ps.setInt(1, foodId);
                    ps.executeUpdate();
                }
                conn.commit();
                return FeaturedAssignmentResult.cleared();
            }

            // ── 2. Lock food row; read its current slot ───────────────────────
            Integer currentSlot;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT featured_position FROM food_items WHERE food_id = ? FOR UPDATE")) {
                ps.setInt(1, foodId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new RuntimeException("Food item " + foodId + " not found.");
                    }
                    currentSlot = rs.getObject("featured_position", Integer.class);
                }
            }

            // ── 3. Short-circuit: food is already at the target slot ──────────
            if (targetPosition.equals(currentSlot)) {
                conn.commit();
                return FeaturedAssignmentResult.assigned(null);
            }

            // ── 4. Lock the target slot's current occupant ────────────────────
            Integer occupantId = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT food_id FROM food_items " +
                    "WHERE featured_position = ? AND food_id != ? FOR UPDATE")) {
                ps.setInt(1, targetPosition);
                ps.setInt(2, foodId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) occupantId = rs.getInt("food_id");
                }
            }

            // ── 5. Refuse if occupied and caller does not want to replace ─────
            if (occupantId != null && !replaceOccupied) {
                conn.rollback();
                return FeaturedAssignmentResult.slotOccupied(occupantId);
            }

            // ── 6. Clear food's current slot (prevents transient UK conflict) ─
            if (currentSlot != null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE food_items SET featured_position = NULL WHERE food_id = ?")) {
                    ps.setInt(1, foodId);
                    ps.executeUpdate();
                }
            }

            // ── 7. Displace the existing occupant ────────────────────────────
            if (occupantId != null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE food_items SET featured_position = NULL WHERE food_id = ?")) {
                    ps.setInt(1, occupantId);
                    ps.executeUpdate();
                }
            }

            // ── 8. Assign to the target slot ─────────────────────────────────
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE food_items SET featured_position = ? WHERE food_id = ?")) {
                ps.setInt(1, targetPosition);
                ps.setInt(2, foodId);
                ps.executeUpdate();
            }

            conn.commit();
            return FeaturedAssignmentResult.assigned(occupantId);

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {
                    logger.log(Level.SEVERE, "assignFeaturedPosition: rollback failed.", ex);
                }
            }
            logger.log(Level.SEVERE, "FoodItemDAO.assignFeaturedPosition failed.", e);
            throw new RuntimeException(
                    "Could not assign featured position: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(prevAutoCommit); } catch (SQLException ex) {
                    logger.log(Level.SEVERE,
                            "assignFeaturedPosition: could not restore autoCommit.", ex);
                }
                try { conn.close(); } catch (SQLException ex) {
                    logger.log(Level.SEVERE,
                            "assignFeaturedPosition: could not close connection.", ex);
                }
            }
        }
    }

    /**
     * Transactionally updates food fields and the featured-slot assignment in one
     * JDBC transaction.
     *
     * <p>Algorithm (all within one transaction):
     * <ol>
     *   <li>Lock the food row with {@code FOR UPDATE}; read its current slot.</li>
     *   <li>If the slot is changing to a non-null value, lock the target slot's current
     *       occupant (if any) with {@code FOR UPDATE}.</li>
     *   <li>If the slot is occupied and {@code replaceOccupied} is {@code false},
     *       rollback and return SLOT_OCCUPIED — no DB state changed.</li>
     *   <li>UPDATE food fields (name, price, available, stock_quantity, image_path,
     *       category_id).</li>
     *   <li>Clear the food's old slot (if it had one), displace any occupant, then
     *       assign the new slot or leave it null.</li>
     *   <li>Commit and return ASSIGNED or CLEARED.</li>
     * </ol>
     * {@code autoCommit} is always restored in the {@code finally} block.
     */
    @Override
    public FeaturedAssignmentResult updateWithFeaturedPosition(
            FoodItem item, Integer targetPosition, boolean replaceOccupied) {
        Connection conn = null;
        boolean prevAutoCommit = true;
        try {
            conn = DatabaseConnection.getConnection();
            prevAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            // ── 1. Lock food row; read its current slot ────────────────────
            Integer currentSlot;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT featured_position FROM food_items WHERE food_id = ? FOR UPDATE")) {
                ps.setInt(1, item.getFoodId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new RuntimeException(
                                "Food item " + item.getFoodId() + " not found.");
                    }
                    currentSlot = rs.getObject("featured_position", Integer.class);
                }
            }

            boolean slotChanging = !java.util.Objects.equals(targetPosition, currentSlot);

            // ── 2. Lock target slot occupant (if slot is changing to non-null) ──
            Integer occupantId = null;
            if (slotChanging && targetPosition != null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT food_id FROM food_items " +
                        "WHERE featured_position = ? AND food_id != ? FOR UPDATE")) {
                    ps.setInt(1, targetPosition);
                    ps.setInt(2, item.getFoodId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) occupantId = rs.getInt("food_id");
                    }
                }

                // ── 3. Refuse if occupied and caller does not want to replace ──
                if (occupantId != null && !replaceOccupied) {
                    conn.rollback();
                    return FeaturedAssignmentResult.slotOccupied(occupantId);
                }
            }

            // ── 4. Update food fields ──────────────────────────────────────
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE food_items " +
                    "SET food_name=?, price=?, available=?, stock_quantity=?, " +
                    "    image_path=?, category_id=? " +
                    "WHERE food_id=?")) {
                ps.setString(1,  item.getFoodName());
                ps.setDouble(2,  item.getPrice());
                ps.setBoolean(3, item.isAvailable());
                ps.setInt(4,     item.getStockQuantity());
                ps.setString(5,  item.getImagePath());
                if (item.getCategoryId() > 0) {
                    ps.setInt(6, item.getCategoryId());
                } else {
                    ps.setNull(6, java.sql.Types.INTEGER);
                }
                ps.setInt(7, item.getFoodId());
                ps.executeUpdate();
            }

            // ── 5. Handle slot changes ────────────────────────────────────
            if (slotChanging) {
                // Clear food's old slot first (prevents transient UNIQUE KEY conflict)
                if (currentSlot != null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE food_items SET featured_position = NULL WHERE food_id = ?")) {
                        ps.setInt(1, item.getFoodId());
                        ps.executeUpdate();
                    }
                }
                // Displace the existing occupant of the target slot
                if (occupantId != null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE food_items SET featured_position = NULL WHERE food_id = ?")) {
                        ps.setInt(1, occupantId);
                        ps.executeUpdate();
                    }
                }
                // Assign food to the new slot (or leave null when clearing)
                if (targetPosition != null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE food_items SET featured_position = ? WHERE food_id = ?")) {
                        ps.setInt(1, targetPosition);
                        ps.setInt(2, item.getFoodId());
                        ps.executeUpdate();
                    }
                }
            }

            conn.commit();
            return (targetPosition == null)
                    ? FeaturedAssignmentResult.cleared()
                    : FeaturedAssignmentResult.assigned(occupantId);

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {
                    logger.log(Level.SEVERE, "updateWithFeaturedPosition: rollback failed.", ex);
                }
            }
            logger.log(Level.SEVERE, "FoodItemDAO.updateWithFeaturedPosition failed.", e);
            throw new RuntimeException("Could not update food item: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(prevAutoCommit); } catch (SQLException ex) {
                    logger.log(Level.SEVERE,
                            "updateWithFeaturedPosition: could not restore autoCommit.", ex);
                }
                try { conn.close(); } catch (SQLException ex) {
                    logger.log(Level.SEVERE,
                            "updateWithFeaturedPosition: could not close connection.", ex);
                }
            }
        }
    }

    private static FoodItem map(ResultSet rs) throws SQLException {
        FoodItem item = new FoodItem();
        item.setFoodId(rs.getInt("food_id"));
        item.setFoodName(rs.getString("food_name"));
        item.setPrice(rs.getDouble("price"));
        item.setAvailable(rs.getBoolean("available"));
        item.setStockQuantity(rs.getInt("stock_quantity"));
        item.setImagePath(rs.getString("image_path"));
        item.setCategoryId(rs.getInt("category_id")); // 0 when NULL (uncategorized)
        item.setCategoryName(rs.getString("category_name")); // COALESCE -> "Uncategorized"
        item.setFeaturedPosition(rs.getObject("featured_position", Integer.class));
        return item;
    }
}
