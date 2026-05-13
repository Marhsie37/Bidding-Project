package com.auction.server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TransactionDAO {
    private static final Logger logger = LoggerFactory.getLogger(TransactionDAO.class);

    // Hàm ghi lại lịch sử giao dịch
    public boolean logTransaction(int userId, double amount, String type, String description) {
        String sql = "INSERT INTO transactions (user_id, amount, type, description) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setDouble(2, amount);
            stmt.setString(3, type);
            stmt.setString(4, description);

            int result = stmt.executeUpdate();
            if (result > 0) {
                logger.info("LOG: Giao dịch {} cho User ID {} đã được ghi lại ({} VNĐ)", type, userId, amount);
                return true;
            }
        } catch (SQLException e) {
            logger.info("LỖI: Không thể ghi log giao dịch cho User ID: " + userId, e);
        }
        return false;
    }
}