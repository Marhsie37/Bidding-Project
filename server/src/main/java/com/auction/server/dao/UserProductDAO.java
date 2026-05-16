package com.auction.server.dao;

import com.auction.shared.model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserProductDAO {
    private DatabaseConnection dbConnection;

    public UserProductDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public boolean addPurchasedProduct(int userId, int productId, String productName, double price) {
        String sql = "INSERT INTO user_products (user_id, product_id, product_name, product_price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, productId);
            pstmt.setString(3, productName);
            pstmt.setDouble(4, price);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding purchased product: " + e.getMessage());
            return false;
        }
    }

    public List<Product> getPurchasedProducts(int userId) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT product_id, product_name, product_price FROM user_products WHERE user_id = ? ORDER BY purchased_at DESC";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            System.out.println("🔍 [DEBUG] getPurchasedProducts - userId: " + userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Product p = new Product();
                    p.setId(rs.getInt("product_id"));
                    p.setName(rs.getString("product_name"));
                    p.setCurrentPrice(rs.getDouble("product_price"));
                    products.add(p);
                    System.out.println("✅ [DEBUG] Đã lấy sản phẩm: " + p.getName());
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ [DEBUG] Lỗi SQL trong getPurchasedProducts: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("✅ [DEBUG] Tổng số sản phẩm tìm thấy: " + products.size());
        return products;
    }
}