package com.auction.server.dao;

import com.auction.shared.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class    UserDAO {
    private DatabaseConnection dbConnection;
    private Connection conn;
    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);
    public UserDAO(Connection conn) {
        this.conn = conn;
    }
    public UserDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }
    private Connection getConnection() throws SQLException {
        if (this.conn != null) {
            return this.conn; // Trả về kết `nối ảo H2 nếu đang chạy Test
        }
        return dbConnection.getConnection(); // Trả về kết nối MySQL thật
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user: " ,e);
        }
        return null;
    }

    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by email: " ,e);
        }
        return null;
    }

    public boolean createUser(String username, String password, String email, String fullName, String role) {
        String sql = "INSERT INTO users (username, password, email, full_name, role) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, email);
            pstmt.setString(4, fullName);
            pstmt.setString(5, role);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error creating user: " ,e);
            return false;
        }
    }

    public boolean updateUser(User user) {
        String sql = "UPDATE users SET email = ?, full_name = ?, balance = ?, active = ? WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getFullName());
            pstmt.setDouble(3, user.getBalance());
            pstmt.setBoolean(4, user.isActive());
            pstmt.setInt(5, user.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating user: " ,e);
            return false;
        }
    }

    public boolean updateBalance(int userId, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setDouble(1, newBalance);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating balance: " ,e);
            return false;
        }
    }

    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE id = ? AND role != 'ADMIN'";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting user: " ,e);
            return false;
        }
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            logger.error("Error getting all users: " ,e);
        }
        return users;
    }

    public List<User> getUsersByRole(String role) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY id";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, role);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting users by role: " ,e);
        }
        return users;
    }
    // Nạp tiền vào tài khoản
    public boolean addFunds(int userId, double amount) {
        String sql = "UPDATE users SET balance = balance + ? WHERE id = ?";
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDouble(1, amount);
                stmt.setInt(2, userId);

                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    logger.info("GIAO DỊCH: User ID {} nạp thành công {} VNĐ", userId, amount);
                    return true;
                } else {
                    logger.warn("CẢNH BÁO: Thử nạp tiền cho User ID {} không tồn tại", userId);
                }
            }
        } catch (SQLException e) {
            logger.error("LỖI HỆ THỐNG: Không thể thực hiện nạp tiền cho User " + userId, e);
        }
        return false;
    }
    // Lịch sử nạp tiền thành công
    public boolean depositMoney(int userId, double amount) {

        TransactionDAO transDAO = new TransactionDAO();
        transDAO.logTransaction(userId, amount, "DEPOSIT", "Người dùng nạp tiền qua ngân hàng");

        return true;
    }
    // Trừ tiền khi thanh toán
    public boolean deductFunds(int userId, double amount) {
        String checkSql = "SELECT balance FROM users WHERE id = ?";
        String deductSql = "UPDATE users SET balance = balance - ? WHERE id = ?";

        try {
            Connection conn = getConnection();

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, userId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getDouble("balance") >= amount) {

                        try (PreparedStatement deductStmt = conn.prepareStatement(deductSql)) {
                            deductStmt.setDouble(1, amount);
                            deductStmt.setInt(2, userId);
                            return deductStmt.executeUpdate() > 0;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error deducting funds: " ,e);
        }
        return false;
    }

    // Lấy số dư
    public double getBalance(int userId) {
        String sql = "SELECT balance FROM users WHERE id = ?";
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("balance");
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting balance: " ,e);
        }
        return 0.0;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEmail(rs.getString("email"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(rs.getString("role"));
        user.setBalance(rs.getDouble("balance"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        user.setActive(rs.getBoolean("active"));
        return user;
    }
}