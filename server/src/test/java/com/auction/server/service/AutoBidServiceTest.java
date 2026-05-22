package com.auction.server.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoBidServiceTest {
    private static final Logger logger = LoggerFactory.getLogger(AutoBidServiceTest.class);

    private AuctionService auctionService;
    private AutoBidService autoBidService;

    @BeforeEach
    public void setUp() {
        auctionService = AuctionService.getInstance();
        autoBidService = AutoBidService.getInstance();

        ensureUserExists("admin", "admin123", "admin_auto@test.com", "Admin System", "ADMIN");
    }

    @AfterEach
    public void tearDown() {
        try {
            autoBidService.stop();
        } catch (Exception e) {
            logger.error("Lỗi khi stop AutoBidService", e);
        }
    }

    private void ensureUserExists(String username, String password, String email, String fullName, String role) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("password", password);
        data.put("email", email);
        data.put("fullName", fullName);
        data.put("role", role);
        auctionService.register(data);
    }

    private String generateUniqueStr(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 5);
    }

    @Test
    public void testSingletonInstance() {
        AutoBidService instance1 = AutoBidService.getInstance();
        AutoBidService instance2 = AutoBidService.getInstance();

        assertNotNull(instance1, "Instance không được phép null");
        assertSame(instance1, instance2, "Hệ thống chỉ được phép tồn tại duy nhất 1 AutoBidService (Singleton)");
    }

    @Test
    public void testStartAndStopService() {
        assertDoesNotThrow(() -> {
            autoBidService.start();
            autoBidService.stop();
        }, "Hàm start và stop của ExecutorService không được phép văng lỗi");
    }

    @Test
    public void testRegisterAndUnregisterAutoBid() {
        autoBidService.registerAutoBid(9999, "test_bot", 5000.0);

        Map<String, Double> bids = autoBidService.getAutoBids(9999);
        assertTrue(bids.containsKey("test_bot"), "Phải lưu được thông tin auto-bid");
        assertEquals(5000.0, bids.get("test_bot"));

        autoBidService.unregisterAutoBid(9999, "test_bot");
        bids = autoBidService.getAutoBids(9999);
        assertFalse(bids.containsKey("test_bot"), "Phải xóa được thông tin auto-bid");
    }

    @Test
    public void testAutoBidLogic() throws InterruptedException {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("name", "VGA RTX 5090 Ti"); // Gọi đúng key "name" theo bản update mới nhất
        productData.put("startingPrice", 1000.0);
        productData.put("durationHours", 24);
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        String botNgheo = generateUniqueStr("bot_ngheo");
        String botGiau = generateUniqueStr("bot_giau");
        ensureUserExists(botNgheo, "123", botNgheo+"@mail.com", "Bot Nghèo", "BIDDER");
        ensureUserExists(botGiau, "123", botGiau+"@mail.com", "Bot Giàu", "BIDDER");

        int idNgheo = (int) ((Map<String, Object>) auctionService.login(botNgheo, "123").get("user")).get("id");
        int idGiau = (int) ((Map<String, Object>) auctionService.login(botGiau, "123").get("user")).get("id");
        auctionService.addFunds(idNgheo, 10000.0);
        auctionService.addFunds(idGiau, 10000.0);

        autoBidService.registerAutoBid(productId, botNgheo, 2000.0);
        autoBidService.registerAutoBid(productId, botGiau, 5000.0);

        autoBidService.start();

        Thread.sleep(3500);

        Map<String, Object> details = auctionService.getAuctionDetails(productId);
        com.auction.shared.model.AuctionSession session = (com.auction.shared.model.AuctionSession) details.get("session");

        assertNotNull(session.getCurrentWinnerName(), "Phải có người chiến thắng sau khi Auto-bid chạy");

        assertEquals(botGiau, session.getCurrentWinnerName(), "Bot giàu phải là người thắng cuối cùng");
        assertTrue(session.getCurrentPrice() > 1000.0, "Thuật toán Auto-bid phải tự động đẩy giá lên");
    }
}