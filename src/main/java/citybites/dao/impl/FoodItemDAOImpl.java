package citybites.dao.impl;

import citybites.config.DatabaseConnection;
import citybites.dao.FoodItemDAO;
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

    /** Base SELECT with LEFT JOIN for category name. */
    private static final String BASE_SQL =
        "SELECT fi.food_id, fi.food_name, fi.price, fi.available, " +
        "       fi.stock_quantity, fi.image_path, fi.category_id, " +
        "       COALESCE(fc.category_name, 'Uncategorized') AS category_name " +
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
        return item;
    }
}
