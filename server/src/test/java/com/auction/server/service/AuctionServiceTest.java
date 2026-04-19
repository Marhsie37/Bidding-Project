package com.auction.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

public class AuctionServiceTest {
    private AuctionService auctionService;

    @BeforeEach
    public void setUp() {
        // Reset lại service trước mỗi bài test để dữ liệu không bị lộn xộn
        auctionService = AuctionService.getInstance();
    }

    @Test
    public void testLoginSuccessWithAdmin() {
        Map<String, Object> result = auctionService.login("admin", "admin123");
        assertTrue((Boolean) result.get("success"), "Tài khoản admin mặc định phải đăng nhập thành công");
        assertEquals("ADMIN", result.get("role"), "Quyền phải là ADMIN");
    }

    @Test
    public void testAddProductAndGenerateSession() {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("productName", "Laptop Gaming Skibidi");
        productData.put("startingPrice", 1000.0);

        Map<String, Object> result = auctionService.addProduct(productData);
        assertTrue((Boolean) result.get("success"), "Thêm sản phẩm phải thành công");
        assertNotNull(result.get("productId"), "Hệ thống phải tự sinh ra productId");
    }

    @Test
    public void testPlaceBidLogic() {
        // 1. Tạo sản phẩm để test
        Map<String, Object> productData = new HashMap<>();
        productData.put("startingPrice", 500.0);
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        // 2. Test đặt giá thành công (Giá cao hơn khởi điểm)
        Map<String, Object> validBid = auctionService.placeBid(productId, "admin", 600.0);
        assertTrue((Boolean) validBid.get("success"), "Đặt giá cao hơn phải thành công");

        // 3. Test đặt giá thất bại (Giá thấp hơn giá hiện tại)
        Map<String, Object> invalidBid = auctionService.placeBid(productId, "admin", 550.0);
        assertFalse((Boolean) invalidBid.get("success"), "Đặt giá thấp hơn giá hiện tại phải bị hệ thống từ chối");
    }
}