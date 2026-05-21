package com.auction.server.dao;

import com.auction.shared.model.User;
import org.junit.jupiter.api.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TransactionDAOTest {
    private Connection testConn;
    private TransactionDAO transactionDAO;
    private UserDAO userDAO;
    private int testUserId;
    private static final Logger logger = LoggerFactory.getLogger(TransactionDAOTest.class);

    @BeforeAll
    void setup() throws Exception {
        testConn = DriverManager.getConnection("jdbc:h2:mem:testdb_tx;DB_CLOSE_DELAY=-1;NON_KEYWORDS=TYPE");

        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "username VARCHAR(50) UNIQUE, " +
                    "password VARCHAR(255), " +
                    "email VARCHAR(100) UNIQUE, " +
                    "full_name VARCHAR(100), " +
                    "role VARCHAR(20), " +
                    "balance DOUBLE DEFAULT 0, " +
                    "active BOOLEAN DEFAULT TRUE, " +
                    "status VARCHAR(20) DEFAULT 'ACTIVE', " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "user_id INT, " +
                    "amount DOUBLE, " +
                    "type VARCHAR(50), " +
                    "description VARCHAR(255), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }

        transactionDAO = new TransactionDAO(testConn);
        userDAO = new UserDAO(testConn);

        String tempUser = "trans_user_" + System.currentTimeMillis();
        userDAO.createUser(tempUser, "123", tempUser + "@test.com", "Trans Tester", "BIDDER");
        testUserId = userDAO.findByUsername(tempUser).getId();
    }

    @AfterAll
    void tearDown() throws SQLException {
        if (testConn != null) {
            testConn.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test ghi log nạp tiền thành công")
    void testLogDeposit() {
        boolean result = transactionDAO.logTransaction(
                testUserId,
                500000.0,
                "DEPOSIT",
                "Nạp tiền qua chuyển khoản ngân hàng"
        );
        assertTrue(result);
    }

    @Test
    @Order(2)
    @DisplayName("Test ghi log trừ tiền đặt giá (BID_HOLD)")
    void testLogBidHold() {
        boolean result = transactionDAO.logTransaction(
                testUserId,
                -150000.0,
                "BID_HOLD",
                "Tạm giữ tiền cho phiên đấu giá Laptop"
        );
        assertTrue(result);
    }

    @Test
    @Order(3)
    @DisplayName("Kiểm tra dữ liệu thực tế trong Database H2")
    void testVerifyDataInDb() {
        String sql = "SELECT COUNT(*) FROM transactions WHERE user_id = " + testUserId;
        try (Statement stmt = testConn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int count = rs.getInt(1);
                assertEquals(2, count);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}