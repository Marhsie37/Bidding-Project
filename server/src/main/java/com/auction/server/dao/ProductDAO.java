package com.auction.server.dao;

import com.auction.shared.model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {
    private DatabaseConnection dbConnection;

    public ProductDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public Product findById(int productId) {
        String sql = "SELECT p.*, u.username as seller_name " +
                "FROM products p LEFT JOIN users u ON p.seller_id = u.user_id " +
                "WHERE p.id = ?";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduct(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding product: " + e.getMessage());
        }
        return null;
    }

    public List<Product> getActiveProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, u.username as seller_name " +
                "FROM products p LEFT JOIN users u ON p.seller_id = u.user_id " +
                "WHERE p.status = 'ACTIVE' AND p.end_time > ? " +
                "ORDER BY p.end_time ASC";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting active products: " + e.getMessage());
        }
        return products;
    }

    public List<Product> getProductsBySeller(int sellerId) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, u.username as seller_name " +
                "FROM products p LEFT JOIN users u ON p.seller_id = u.user_id " +
                "WHERE p.seller_id = ? ORDER BY p.created_at DESC";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, sellerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting products by seller: " + e.getMessage());
        }
        return products;
    }

    public boolean createProduct(Product product) {
        String sql = "INSERT INTO products (name, description, starting_price, current_price, " +
                "seller_id, category, image_url, duration_hours, start_time, end_time, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getDescription());
            pstmt.setDouble(3, product.getStartingPrice());
            pstmt.setDouble(4, product.getStartingPrice()); // Ban đầu giá hiện tại = giá khởi điểm
            pstmt.setInt(5, product.getSellerId());
            pstmt.setString(6, product.getCategoryName()); // Đa hình: Trả về 'ELECTRONICS' hoặc 'ART'
            pstmt.setString(7, product.getImageUrl());
            pstmt.setInt(8, product.getDurationHours());
            pstmt.setTimestamp(9, Timestamp.valueOf(product.getStartTime()));
            pstmt.setTimestamp(10, Timestamp.valueOf(product.getEndTime()));
            pstmt.setString(11, "PENDING");

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) product.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error creating product: " + e.getMessage());
        }
        return false;
    }

    public boolean updateProduct(Product product) {
        String sql = "UPDATE products SET name = ?, description = ?, category = ?, image_url = ? WHERE id = ? AND seller_id = ?";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getDescription());
            pstmt.setString(3, product.getCategoryName());
            pstmt.setString(4, product.getImageUrl());
            pstmt.setInt(5, product.getId());
            pstmt.setInt(6, product.getSellerId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating product: " + e.getMessage());
        }
        return false;
    }

    public boolean updateCurrentPrice(int productId, double newPrice, int bidderId) {
        String sql = "UPDATE products SET current_price = ?, winner_id = ? WHERE id = ? AND status = 'ACTIVE'";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setDouble(1, newPrice);
            pstmt.setInt(2, bidderId);
            pstmt.setInt(3, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating current price: " + e.getMessage());
        }
        return false;
    }

    public boolean activateProduct(int productId) {
        String sql = "UPDATE products SET status = 'ACTIVE', start_time = ? WHERE id = ? AND status = 'PENDING'";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setInt(2, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error activating product: " + e.getMessage());
        }
        return false;
    }

    public boolean updateStatus(int productId, String status) {
        String sql = "UPDATE products SET status = ? WHERE id = ?";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating status: " + e.getMessage());
        }
        return false;
    }

    public boolean deleteProduct(int productId, int sellerId) {
        String sql = "DELETE FROM products WHERE id = ? AND seller_id = ? AND status = 'PENDING'";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            pstmt.setInt(2, sellerId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting product: " + e.getMessage());
        }
        return false;
    }

    public boolean adminDeleteProduct(int productId) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error admin deleting product: " + e.getMessage());
        }
        return false;
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        String category = rs.getString("category");
        Product p = "ELECTRONICS".equals(category) ? new Electronics() : new Art();

        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setStartingPrice(rs.getDouble("starting_price"));
        p.setCurrentPrice(rs.getDouble("current_price"));
        p.setSellerId(rs.getInt("seller_id"));
        p.setSellerName(rs.getString("seller_name"));
        p.setImageUrl(rs.getString("image_url"));
        p.setDurationHours(rs.getInt("duration_hours"));

        Timestamp st = rs.getTimestamp("start_time");
        if (st != null) p.setStartTime(st.toLocalDateTime());

        Timestamp et = rs.getTimestamp("end_time");
        if (et != null) p.setEndTime(et.toLocalDateTime());

        p.setStatus(rs.getString("status"));
        int winnerId = rs.getInt("winner_id");
        if (!rs.wasNull()) {
            p.setWinnerId(winnerId);
        }

        Timestamp ct = rs.getTimestamp("created_at");
        if (ct != null) p.setCreatedAt(ct.toLocalDateTime());

        return p;
        Timestamp ct = rs.getTimestamp("created_at");
        if (ct != null) p.setCreatedAt(ct.toLocalDateTime());

        return p;
    }
}