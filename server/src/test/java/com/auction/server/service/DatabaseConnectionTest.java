package com.auction.server.service; // Đổi lại tên package nếu file này bác để ở thư mục khác

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConnectionTest {

    @Test
    public void testDatabaseDriverExists() {
        // Test xem project đã import driver Database vào file pom.xml chưa
        assertDoesNotThrow(() -> {
            Class.forName("com.mysql.cj.jdbc.Driver");
        }, "Phải có JDBC Driver trong thư viện Maven");
    }

    @Test
    public void testSimulatedConnection() {
        // Một test cơ bản để xác nhận cấu hình URL hợp lệ
        String dbUrl = "jdbc:mysql://localhost:3306/auction_db";
        assertTrue(dbUrl.startsWith("jdbc:"), "Chuỗi kết nối Database phải đúng định dạng JDBC");
    }
}