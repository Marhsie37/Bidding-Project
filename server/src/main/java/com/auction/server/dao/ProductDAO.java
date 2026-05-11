package com.auction.server.dao;

import com.auction.shared.model.Product;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductDAO {
    private DatabaseConnection dbConnection;
    private static final Logger logger = LoggerFactory.getLogger(ProductDAO.class);
    public ProductDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public Product findById(int productId) {
        String sql = "SELECT p.*, u.username as seller_name " +
                "FROM products p LEFT JOIN users u ON p.seller_id = u.id " +
                "WHERE p.id = ?";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduct(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding product: ",e);
        }
        return null;
    }

    public List<Product> getActiveProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, u.username as seller_name " +
                "FROM products p LEFT JOIN users u ON p.seller_id = u.id " +
                "WHERE p.status IN ('PENDING', 'ACTIVE') AND p.end_time > ? " +
                "ORDER BY p.end_time ASC";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting active products: " ,e);
        }
        return products;
    }

    public List<Product> getProductsBySeller(int sellerId) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, u.username as seller_name " +
                "FROM products p LEFT JOIN users u ON p.seller_id = u.id " +
                "WHERE p.seller_id = ? ORDER BY p.created_at DESC";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, sellerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting products by seller: " ,e);
        }
        return products;
    }

    public boolean createProduct(Product product) {
        String sql = "INSERT INTO products (name, description, starting_price, current_price, " +
                "seller_id, category, image_url, duration_hours, end_time, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getDescription());
            pstmt.setDouble(3, product.getStartingPrice());
            pstmt.setDouble(4, product.getStartingPrice());
            pstmt.setInt(5, product.getSellerId());
            pstmt.setString(6, product.getCategory());
            pstmt.setString(7, product.getImageUrl());
            pstmt.setInt(8, product.getDurationHours());

            if (product.getEndTime() != null) {
                pstmt.setTimestamp(9, Timestamp.valueOf(product.getEndTime()));
            } else {
                pstmt.setNull(9, Types.TIMESTAMP);
            }

            pstmt.setString(10, "PENDING");

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        product.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error creating product: ",e);
        }
        return false;
    }

    public boolean updateProduct(Product product) {
        String sql = "UPDATE products SET name = ?, description = ?, category = ?, image_url = ? WHERE id = ? AND seller_id = ?";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getDescription());
            pstmt.setString(3, product.getCategory());
            pstmt.setString(4, product.getImageUrl());
            pstmt.setInt(5, product.getId());
            pstmt.setInt(6, product.getSellerId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating product: ",e);
            return false;
        }
    }

    public boolean updateCurrentPrice(int productId, double newPrice) {
        String sql = "UPDATE products SET current_price = ? WHERE id = ?";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setDouble(1, newPrice);
            pstmt.setInt(2, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating current price: ",e);
            return false;
        }
    }

    public boolean updateWinner(int productId, int winnerId) {
        String sql = "UPDATE products SET winner_id = ?, status = 'ENDED' WHERE id = ?";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            if (winnerId > 0) {
                pstmt.setInt(1, winnerId);
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setInt(2, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating winner: ",e);
            return false;
        }
    }

    public boolean updateStatus(int productId, String status) {
        String sql = "UPDATE products SET status = ? WHERE id = ?";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating status: ",e);
            return false;
        }
    }

    public boolean activateProduct(int productId) {
        String sql = "UPDATE products SET status = 'ACTIVE', start_time = ? WHERE id = ? AND status = 'PENDING'";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setInt(2, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error activating product: ",e);
            return false;
        }
    }

    public boolean deleteProduct(int productId, int sellerId) {
        String sql = "DELETE FROM products WHERE id = ? AND seller_id = ? AND status = 'PENDING'";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            pstmt.setInt(2, sellerId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting product: " ,e);
            return false;
        }
    }

    public boolean adminDeleteProduct(int productId) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error admin deleting product: " ,e);
            return false;
        }
    }

    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, u.username as seller_name " +
                "FROM products p LEFT JOIN users u ON p.seller_id = u.id " +
                "ORDER BY p.created_at DESC";
        try (Statement stmt = dbConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            logger.error("Error getting all products: " ,e);
        }
        return products;
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setId(rs.getInt("id"));
        product.setName(rs.getString("name"));
        product.setDescription(rs.getString("description"));
        product.setStartingPrice(rs.getDouble("starting_price"));
        product.setCurrentPrice(rs.getDouble("current_price"));
        product.setSellerId(rs.getInt("seller_id"));
        product.setSellerName(rs.getString("seller_name"));
        product.setCategory(rs.getString("category"));
        product.setImageUrl(rs.getString("image_url"));
        product.setDurationHours(rs.getInt("duration_hours"));

        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) product.setStartTime(startTime.toLocalDateTime());

        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) product.setEndTime(endTime.toLocalDateTime());

        product.setStatus(rs.getString("status"));

        int winnerId = rs.getInt("winner_id");
        if (!rs.wasNull()) {
            product.setWinnerId(winnerId);
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) product.setCreatedAt(createdAt.toLocalDateTime());

        return product;
    }
}