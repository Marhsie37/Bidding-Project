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
}