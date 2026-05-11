package com.auction.server.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoBidServiceTest {
    private static final Logger logger = LoggerFactory.getLogger(AutoBidServiceTest.class);
    @Test
    public void testSingletonInstance() {
        AutoBidService instance1 = AutoBidService.getInstance();
        AutoBidService instance2 = AutoBidService.getInstance();

        assertNotNull(instance1, "Instance không được phép null");
        assertSame(instance1, instance2, "Hệ thống chỉ được phép tồn tại duy nhất 1 AutoBidService (Singleton)");
    }

    @Test
    public void testStartAndStopService() {
        AutoBidService service = AutoBidService.getInstance();

        // Chạy thử hàm start và stop xem có văng lỗi đa luồng không
        assertDoesNotThrow(() -> {
            service.start();
            service.stop();
        }, "Hàm start và stop của ExecutorService không được phép văng lỗi");
    }
}