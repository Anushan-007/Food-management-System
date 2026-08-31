package citybites.dao.impl;

import citybites.config.DatabaseConnection;
import citybites.dao.FoodCategoryDAO;
import citybites.model.FoodCategory;
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

public class FoodCategoryDAOImpl implements FoodCategoryDAO {

    private static final Logger logger = Logger.getLogger(FoodCategoryDAOImpl.class.getName());

    @Override
    public List<FoodCategory> findAll() {
        List<FoodCategory> list = new ArrayList<>();
        String sql = "SELECT category_id, category_name, description " +
                     "FROM food_categories ORDER BY category_name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodCategoryDAO.findAll failed.", e);
        }
        return list;
    }

    @Override
    public Optional<FoodCategory> findById(int id) {
        String sql = "SELECT category_id, category_name, description " +
                     "FROM food_categories WHERE category_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodCategoryDAO.findById failed.", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<FoodCategory> findByName(String name) {
        String sql = "SELECT category_id, category_name, description " +
                     "FROM food_categories WHERE category_name = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodCategoryDAO.findByName failed.", e);
        }
        return Optional.empty();
    }

    @Override
    public boolean existsByNameCaseInsensitive(String name, int excludeId) {
        String sql = "SELECT COUNT(*) FROM food_categories " +
                     "WHERE LOWER(category_name) = LOWER(?) AND category_id != ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, excludeId);   // -1 never matches a real PK (positive), so checks all rows
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodCategoryDAO.existsByNameCaseInsensitive failed.", e);
            throw new RuntimeException("Could not check category name uniqueness: " + e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean isCategoryInUse(int id) {
        String sql = "SELECT COUNT(*) FROM food_items WHERE category_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodCategoryDAO.isCategoryInUse failed — failing safe.", e);
            return true;  // fail safe: prevent deletion on DB error
        }
        return false;
    }

    @Override
    public boolean existsById(int id) {
        String sql = "SELECT COUNT(*) FROM food_categories WHERE category_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodCategoryDAO.existsById failed.", e);
            throw new RuntimeException("Could not check category existence: " + e.getMessage(), e);
        }
        return false;
    }

    @Override
    public int insert(String name, String description) {
        String sql = "INSERT INTO food_categories (category_name, description) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, description);   // null → SQL NULL (JDBC spec)
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodCategoryDAO.insert failed.", e);
            throw new RuntimeException("Could not add category: " + e.getMessage(), e);
        }
        return -1;
    }

    @Override
    public boolean update(int id, String name, String description) {
        String sql = "UPDATE food_categories SET category_name = ?, description = ? " +
                     "WHERE category_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, description);   // null → SQL NULL
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodCategoryDAO.update failed.", e);
            throw new RuntimeException("Could not update category: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM food_categories WHERE category_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "FoodCategoryDAO.delete failed.", e);
            throw new RuntimeException("Could not delete category: " + e.getMessage(), e);
        }
    }

    private static FoodCategory map(ResultSet rs) throws SQLException {
        return new FoodCategory(
            rs.getInt("category_id"),
            rs.getString("category_name"),
            rs.getString("description")
        );
    }
}
