package com.auction.server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TransactionDAO {
    private static final Logger logger = LoggerFactory.getLogger(TransactionDAO.class);
    private Connection conn;

    public TransactionDAO() {
    }

    public TransactionDAO(Connection conn) {
        this.conn = conn;
    }

    private Connection getConnection() throws SQLException {
        if (this.conn != null) {
            return this.conn;
        }
        return DatabaseConnection.getInstance().getConnection();
    }

    public boolean logTransaction(int userId, double amount, String type, String description) {
        String sql = "INSERT INTO transactions (user_id, amount, type, description) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setDouble(2, amount);
            stmt.setString(3, type);
            stmt.setString(4, description);
            int result = stmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            logger.error("LỖI: Không thể ghi log giao dịch", e);
            return false;
        }
    }
}