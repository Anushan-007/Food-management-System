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

    @Override
    public List<FoodItem> findAll() {
        List<FoodItem> list = new ArrayList<>();
        String sql = "SELECT food_id, food_name, price, available, image_path " +
                     "FROM food_items ORDER BY food_id";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql);
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
        String sql = "SELECT food_id, food_name, price, available, image_path " +
                     "FROM food_items WHERE available = 1 ORDER BY food_id";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodItemDAO.findAvailable failed.", e);
        }
        return list;
    }

    @Override
    public Optional<FoodItem> findById(int foodId) {
        String sql = "SELECT food_id, food_name, price, available, image_path " +
                     "FROM food_items WHERE food_id = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
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
        String sql = "INSERT INTO food_items (food_name, price, available, image_path) " +
                     "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getFoodName());
            ps.setDouble(2, item.getPrice());
            ps.setBoolean(3, item.isAvailable());
            ps.setString(4, item.getImagePath());
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
        String sql = "UPDATE food_items SET food_name=?, price=?, available=?, image_path=? " +
                     "WHERE food_id=?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, item.getFoodName());
            ps.setDouble(2, item.getPrice());
            ps.setBoolean(3, item.isAvailable());
            ps.setString(4, item.getImagePath());
            ps.setInt(5, item.getFoodId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodItemDAO.update failed.", e);
            throw new RuntimeException("Could not update food item: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(int foodId) {
        String sql = "DELETE FROM food_items WHERE food_id = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, foodId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodItemDAO.delete failed.", e);
            throw new RuntimeException("Could not delete food item: " + e.getMessage(), e);
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private static FoodItem map(ResultSet rs) throws SQLException {
        FoodItem item = new FoodItem();
        item.setFoodId(rs.getInt("food_id"));
        item.setFoodName(rs.getString("food_name"));
        item.setPrice(rs.getDouble("price"));
        item.setAvailable(rs.getBoolean("available"));
        item.setImagePath(rs.getString("image_path"));
        return item;
    }
}
