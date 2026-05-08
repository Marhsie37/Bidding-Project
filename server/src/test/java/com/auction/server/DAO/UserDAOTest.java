import com.auction.server.dao.UserDAO;
import com.auction.shared.model.User;
import org.junit.jupiter.api.*;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

class UserDAOTest {
    private static Connection conn;

    @BeforeAll
    static void setup() throws SQLException {
        conn = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        Statement stmt = conn.createStatement();

        stmt.execute("CREATE TABLE users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(50) NOT NULL, " +
                "password VARCHAR(50) NOT NULL, " +
                "email VARCHAR(100), " +
                "full_name VARCHAR(100), " +
                "role VARCHAR(20), " +
                "balance DOUBLE DEFAULT 0.0, " +
                "active BOOLEAN DEFAULT TRUE, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    }

    @Test
    void testCreateAndFindUser() {
        UserDAO dao = new UserDAO(conn);

        boolean created = dao.createUser("testuser", "password", "test@mail.com", "Test Name", "USER");

        assertTrue(created, "Nên tạo được user mới");
        assertNotNull(dao.findByUsername("testuser"), "Nên tìm thấy user vừa tạo");
    }
    @Test
    void testAddFunds() {
        UserDAO dao = new UserDAO(conn);
        // Giả sử testuser đã được tạo từ bài test trước (hoặc tạo mới tại đây)
        dao.createUser("moneyuser", "pass", "money@mail.com", "Money User", "BIDDER");
        User user = dao.findByUsername("moneyuser");

        // Nạp 500,000đ
        boolean success = dao.addFunds(user.getId(), 500000.0);
        assertTrue(success);

        double balance = dao.getBalance(user.getId());
        assertEquals(500000.0, balance, "Số dư phải là 500,000");
    }

    @Test
    void testDeductFunds() {
        UserDAO dao = new UserDAO(conn);
        dao.createUser("shopuser", "pass", "shop@mail.com", "Shop User", "BIDDER");
        User user = dao.findByUsername("shopuser");
        assertNotNull(user, "User 'shopuser' phải được tạo thành công!");
        // Nạp 1,000,000đ
        dao.addFunds(user.getId(), 1000000.0);

        // Trường hợp 1: Trừ số tiền hợp lệ (Mua món đồ 400k)
        boolean canPay = dao.deductFunds(user.getId(), 400000.0);
        assertTrue(canPay);
        assertEquals(600000.0, dao.getBalance(user.getId()), "Số dư còn lại phải là 600,000");

        // Trường hợp 2: Trừ quá số dư (Mua món đồ 2 triệu)
        boolean canPayOver = dao.deductFunds(user.getId(), 2000000.0);
        assertFalse(canPayOver, "Không được phép trừ nếu không đủ tiền");
        assertEquals(600000.0, dao.getBalance(user.getId()), "Số dư không được thay đổi khi giao dịch lỗi");
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM users"); // Xóa sạch data sau mỗi lần chạy một @Test
        }
    }
}