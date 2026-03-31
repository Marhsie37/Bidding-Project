package com.auction.auctionsystem.dao;

import com.auction.auctionsystem.database.DBContext;
import com.auction.auctionsystem.models.User;
import java.sql.*;

public class UserDAO {

    public User login(String user, String pass) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, user);
            ps.setString(2, pass);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("role")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}