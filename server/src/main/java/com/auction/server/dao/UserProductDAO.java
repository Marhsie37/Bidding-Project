package com.auction.server.dao;

import com.auction.shared.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserProductDAO {
  private DatabaseConnection dbConnection;
  private static final Logger logger = LoggerFactory.getLogger(UserProductDAO.class);

  public UserProductDAO() {
    this.dbConnection = DatabaseConnection.getInstance();
  }

  public void addPurchasedProduct(int userId, int productId, String productName, double finalPrice) {
    String sql = "INSERT INTO user_products (user_id, product_id, product_name, product_price, purchased_at) " +
            "VALUES (?, ?, ?, ?, NOW()) " +
            "ON DUPLICATE KEY UPDATE product_price = ?, purchased_at = NOW()";

    try (Connection conn = dbConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setInt(1, userId);
      pstmt.setInt(2, productId);
      pstmt.setString(3, productName);
      pstmt.setDouble(4, finalPrice);
      pstmt.setDouble(5, finalPrice); // cho ON DUPLICATE KEY
      pstmt.executeUpdate();

      logger.info("✅ Đã thêm vào user_products: userId={}, productId={}, price={}", userId, productId, finalPrice);

    } catch (SQLException e) {
      logger.error("❌ Lỗi thêm vào user_products: ", e);
    }
  }

  public List<Product> getPurchasedProducts(int userId) {
    List<Product> products = new ArrayList<>();
    String sql = "SELECT up.product_id, up.product_name, up.product_price, p.image_url, p.description " +
            "FROM user_products up " +
            "INNER JOIN products p ON up.product_id = p.id " +
            "WHERE up.user_id = ? ORDER BY up.purchased_at DESC";

    try (Connection conn = dbConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setInt(1, userId);
      logger.info("🔍 [DEBUG] getPurchasedProducts - userId: " + userId);

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          Product p = new Product();
          p.setId(rs.getInt("product_id"));
          p.setName(rs.getString("product_name"));
          p.setCurrentPrice(rs.getDouble("product_price"));
          p.setImageUrl(rs.getString("image_url"));
          p.setDescription(rs.getString("description"));
          products.add(p);
          logger.info("✅ [DEBUG] Đã lấy sản phẩm: " + p.getName() + " có ảnh: " + (p.getImageUrl() != null && !p.getImageUrl().isEmpty()));
        }
      }
    } catch (SQLException e) {
      logger.error("❌ [DEBUG] Lỗi SQL trong getPurchasedProducts: ", e);
      e.printStackTrace();
    }

    logger.info("✅ [DEBUG] Tổng số sản phẩm tìm thấy: " + products.size());
    return products;
  }
}