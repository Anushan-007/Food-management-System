package citybites.dao.impl;

import citybites.config.DatabaseConnection;
import citybites.dao.FoodRatingDAO;
import citybites.model.FoodRating;
import citybites.model.FoodRatingSummary;
import citybites.model.FoodReviewDetail;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FoodRatingDAOImpl implements FoodRatingDAO {

    private static final Logger logger = Logger.getLogger(FoodRatingDAOImpl.class.getName());

    @Override
    public Optional<FoodRating> findByOrderItemId(int orderItemId) {
        String sql =
            "SELECT fr.rating_id, fr.order_item_id, fr.rating, fr.review_text, " +
            "       fr.created_at, fr.updated_at, " +
            "       oi.food_name, oi.order_id " +
            "FROM food_ratings fr " +
            "JOIN order_items oi ON oi.item_id = fr.order_item_id " +
            "WHERE fr.order_item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderItemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodRatingDAO.findByOrderItemId failed.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<FoodRating> findByOrderId(int orderId) {
        List<FoodRating> list = new ArrayList<>();
        String sql =
            "SELECT fr.rating_id, fr.order_item_id, fr.rating, fr.review_text, " +
            "       fr.created_at, fr.updated_at, " +
            "       oi.food_name, oi.order_id " +
            "FROM order_items oi " +
            "LEFT JOIN food_ratings fr ON fr.order_item_id = oi.item_id " +
            "WHERE oi.order_id = ? " +
            "ORDER BY oi.item_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Only rows where a rating exists (LEFT JOIN may give null rating_id)
                    if (rs.getObject("rating_id") != null) {
                        list.add(map(rs));
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodRatingDAO.findByOrderId failed.", e);
        }
        return list;
    }

    @Override
    public void saveRating(int orderItemId, int customerId, int ratingValue, String reviewText) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // ── Step 1: Lock the order row and verify ownership + status ──────────
                String verifySql =
                    "SELECT o.order_id, o.status, o.customer_id " +
                    "FROM order_items oi " +
                    "JOIN orders o ON o.order_id = oi.order_id " +
                    "WHERE oi.item_id = ? FOR UPDATE";
                try (PreparedStatement ps = conn.prepareStatement(verifySql)) {
                    ps.setInt(1, orderItemId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            throw new IllegalArgumentException(
                                "Order item not found: item_id=" + orderItemId);
                        }
                        if (rs.getInt("customer_id") != customerId) {
                            conn.rollback();
                            throw new IllegalArgumentException(
                                "This order item does not belong to your account.");
                        }
                        String status = rs.getString("status");
                        if (!"Completed".equals(status)) {
                            conn.rollback();
                            throw new IllegalArgumentException(
                                "Only completed orders can be rated. Current status: " + status);
                        }
                    }
                }

                // ── Step 2: Check for an existing rating ──────────────────────────────
                Integer existingId = null;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT rating_id FROM food_ratings WHERE order_item_id = ?")) {
                    ps.setInt(1, orderItemId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) existingId = rs.getInt("rating_id");
                    }
                }

                // ── Step 3: Insert or update ──────────────────────────────────────────
                if (existingId == null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO food_ratings (order_item_id, rating, review_text) " +
                            "VALUES (?, ?, ?)")) {
                        ps.setInt(1, orderItemId);
                        ps.setInt(2, ratingValue);
                        if (reviewText != null) ps.setString(3, reviewText);
                        else                   ps.setNull(3, Types.VARCHAR);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE food_ratings SET rating = ?, review_text = ? " +
                            "WHERE rating_id = ?")) {
                        ps.setInt(1, ratingValue);
                        if (reviewText != null) ps.setString(2, reviewText);
                        else                   ps.setNull(2, Types.VARCHAR);
                        ps.setInt(3, existingId);
                        ps.executeUpdate();
                    }
                }

                conn.commit();

            } catch (IllegalArgumentException e) {
                try { conn.rollback(); } catch (SQLException ignored) {}
                throw e;
            } catch (SQLException e) {
                try { conn.rollback(); } catch (SQLException ignored) {}
                logger.log(Level.SEVERE, "FoodRatingDAO.saveRating rolled back.", e);
                throw new RuntimeException("Could not save rating: " + e.getMessage(), e);
            } finally {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodRatingDAO.saveRating connection error.", e);
            throw new RuntimeException("Database error saving rating: " + e.getMessage(), e);
        }
    }

    // ── Admin read-only methods ────────────────────────────────────────────────

    @Override
    public List<FoodReviewDetail> getReviewsByFoodId(int foodId) {
        List<FoodReviewDetail> list = new ArrayList<>();
        String sql =
            "SELECT fi.food_id, fi.food_name, " +
            "       fc.category_name, " +
            "       fi.image_path, " +
            "       c.full_name   AS customer_full_name, " +
            "       fr.rating, fr.review_text, " +
            "       o.order_date, " +
            "       fr.created_at, fr.updated_at " +
            "FROM food_ratings fr " +
            "JOIN order_items oi  ON oi.item_id      = fr.order_item_id " +
            "JOIN orders o        ON o.order_id       = oi.order_id " +
            "JOIN customers c     ON c.customer_id    = o.customer_id " +
            "JOIN food_items fi   ON fi.food_id       = oi.food_id " +
            "LEFT JOIN food_categories fc ON fc.category_id = fi.category_id " +
            "WHERE oi.food_id = ? " +
            "ORDER BY fr.created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, foodId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapReviewDetail(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodRatingDAO.getReviewsByFoodId failed.", e);
        }
        return list;
    }

    @Override
    public Map<Integer, FoodRatingSummary> getRatingSummaryForAllFoods() {
        Map<Integer, FoodRatingSummary> map = new LinkedHashMap<>();
        String sql =
            "SELECT fi.food_id, " +
            "       AVG(fr.rating)      AS avg_rating, " +
            "       COUNT(fr.rating_id) AS rating_count " +
            "FROM food_items fi " +
            "LEFT JOIN order_items oi ON oi.food_id       = fi.food_id " +
            "LEFT JOIN food_ratings fr ON fr.order_item_id = oi.item_id " +
            "GROUP BY fi.food_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int    foodId = rs.getInt("food_id");
                int    count  = rs.getInt("rating_count");
                // AVG returns SQL NULL when count == 0; getDouble returns 0.0 in that case
                double avg    = (count > 0) ? rs.getDouble("avg_rating") : 0.0;
                map.put(foodId, new FoodRatingSummary(foodId, avg, count));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodRatingDAO.getRatingSummaryForAllFoods failed.", e);
        }
        return map;
    }

    // ── Mapping ────────────────────────────────────────────────────────────────

    private static FoodReviewDetail mapReviewDetail(ResultSet rs) throws SQLException {
        FoodReviewDetail d = new FoodReviewDetail();
        d.setFoodId(rs.getInt("food_id"));
        d.setFoodName(rs.getString("food_name"));
        d.setCategoryName(rs.getString("category_name"));
        d.setImagePath(rs.getString("image_path"));
        d.setCustomerFullName(rs.getString("customer_full_name"));
        d.setRating(rs.getInt("rating"));
        d.setReviewText(rs.getString("review_text"));

        Timestamp orderDate = rs.getTimestamp("order_date");
        if (orderDate != null) d.setOrderDate(orderDate.toLocalDateTime());

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) d.setCreatedAt(created.toLocalDateTime());

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) d.setUpdatedAt(updated.toLocalDateTime());

        return d;
    }

    private static FoodRating map(ResultSet rs) throws SQLException {
        FoodRating r = new FoodRating();
        r.setRatingId(rs.getInt("rating_id"));
        r.setOrderItemId(rs.getInt("order_item_id"));
        r.setRating(rs.getInt("rating"));
        r.setReviewText(rs.getString("review_text"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) r.setCreatedAt(created.toLocalDateTime());

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) r.setUpdatedAt(updated.toLocalDateTime());

        r.setFoodName(rs.getString("food_name"));
        r.setOrderId(rs.getInt("order_id"));
        return r;
    }
}
