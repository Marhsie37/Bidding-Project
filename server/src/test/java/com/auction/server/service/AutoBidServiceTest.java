package com.auction.server.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
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
    }

    @AfterEach
    public void tearDown() {
        // Dọn dẹp luồng chạy ngầm sau khi test xong để không rò rỉ bộ nhớ
        try {
            autoBidService.stop();
        } catch (Exception e) {
            logger.error("Lỗi khi stop AutoBidService", e);
        }
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
        // Chạy thử hàm start và stop xem có văng lỗi đa luồng không
        assertDoesNotThrow(() -> {
            autoBidService.start();
            autoBidService.stop();
        }, "Hàm start và stop của ExecutorService không được phép văng lỗi");
    }

    @Test
    public void testAutoBidPriorityQueueLogic() throws InterruptedException {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("productName", "VGA RTX 5090 Ti");
        productData.put("startingPrice", 1000.0);
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        Map<String, Object> bot1 = new HashMap<>(); bot1.put("username", "autobot_ngheo"); bot1.put("password", "123");
        Map<String, Object> bot2 = new HashMap<>(); bot2.put("username", "autobot_giau"); bot2.put("password", "123");
        auctionService.register(bot1);
        auctionService.register(bot2);

        auctionService.setAutoBid(productId, "autobot_ngheo", 2000.0, 10.0);

        auctionService.setAutoBid(productId, "autobot_giau", 5000.0, 10.0);

        // Bật luồng Auto-bid và chờ
        autoBidService.start();

        // Chờ 4 giây để ScheduledExecutorService có đủ thời gian quét (vì chu kỳ của bác là 3s/lần)
        Thread.sleep(4000);

        // Kiểm tra thuật toán Max-Heap (PriorityQueue)
        Map<String, Object> details = auctionService.getAuctionDetails(productId);
        com.auction.shared.model.AuctionSession session = (com.auction.shared.model.AuctionSession) details.get("session");

        assertNotNull(session.getCurrentWinnerName(), "Phải có người chiến thắng sau khi Auto-bid chạy");

        assertEquals("autobot_giau", session.getCurrentWinnerName(), "Thuật toán phải chọn người có maxBid cao nhất làm winner");
        assertTrue(session.getCurrentPrice() > 1000.0, "Thuật toán Auto-bid phải tự động đẩy giá lên");
    }
}