package com.auction.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuctionServiceTest {
    private AuctionService auctionService;

    @BeforeEach
    public void setUp() {
        auctionService = AuctionService.getInstance();
        ensureUserExists("admin", "admin123", "admin_system@test.com", "Admin System", "ADMIN");
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
    public void testLoginSuccessWithAdmin() {
        Map<String, Object> result = auctionService.login("admin", "admin123");
        assertTrue((Boolean) result.get("success"), "Đăng nhập admin thất bại: " + result.get("message"));
    }

    @Test
    public void testAddProductAndGenerateSession() {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("name", "Laptop Gaming");
        productData.put("startingPrice", 10000.0);
        productData.put("durationHours", 24);

        Map<String, Object> result = auctionService.addProduct(productData);
        assertTrue((Boolean) result.get("success"), "Thêm sản phẩm thất bại: " + result.get("message"));
    }

    @Test
    public void testPlaceBidLogic() {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("name", "Chuột Razer");
        productData.put("startingPrice", 10000.0);
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        String bidder = generateUniqueStr("bidder");
        ensureUserExists(bidder, "123", bidder+"@test.com", "Normal Bidder", "BIDDER");
        int bidderId = (int) ((Map<String, Object>) auctionService.login(bidder, "123").get("user")).get("id");
        auctionService.addFunds(bidderId, 50000.0);

        Map<String, Object> validBid = auctionService.placeBid(productId, bidder, 15000.0);
        assertTrue((Boolean) validBid.get("success"), "Đặt giá thất bại. Lý do: " + validBid.get("message"));

        Map<String, Object> invalidBid = auctionService.placeBid(productId, bidder, 11000.0);
        assertFalse((Boolean) invalidBid.get("success"), "Đặt giá thấp hơn phải bị từ chối");
    }

    @Test
    public void testPlaceBidFailsWhenBidEqualsCurrentPrice() {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("name", "Màn hình Dell");
        productData.put("startingPrice", 10000.0);
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        String u1 = generateUniqueStr("user1");
        String u2 = generateUniqueStr("user2");
        ensureUserExists(u1, "123", u1+"@test.com", "User 1", "BIDDER");
        ensureUserExists(u2, "123", u2+"@test.com", "User 2", "BIDDER");

        int id1 = (int) ((Map<String, Object>) auctionService.login(u1, "123").get("user")).get("id");
        int id2 = (int) ((Map<String, Object>) auctionService.login(u2, "123").get("user")).get("id");
        auctionService.addFunds(id1, 50000.0);
        auctionService.addFunds(id2, 50000.0);

        Map<String, Object> bid1 = auctionService.placeBid(productId, u1, 15000.0);
        assertTrue((Boolean) bid1.get("success"), "U1 đặt giá thất bại. Lý do: " + bid1.get("message"));

        Map<String, Object> invalidBid = auctionService.placeBid(productId, u2, 15000.0);
        assertFalse((Boolean) invalidBid.get("success"), "Đặt giá bằng đúng giá hiện hành phải bị từ chối");
    }

    @Test
    public void testPlaceBidFailsWhenAuctionClosed() {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("name", "Bàn phím cơ");
        productData.put("startingPrice", 10000.0);
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        String lateBidder = generateUniqueStr("late");
        ensureUserExists(lateBidder, "123", lateBidder+"@test.com", "Late Bidder", "BIDDER");
        int lateId = (int) ((Map<String, Object>) auctionService.login(lateBidder, "123").get("user")).get("id");
        auctionService.addFunds(lateId, 50000.0);

        auctionService.endAuction(productId);

        Map<String, Object> lateBid = auctionService.placeBid(productId, lateBidder, 20000.0);
        assertFalse((Boolean) lateBid.get("success"), "Phiên đã đóng cửa thì không được phép đặt giá");
    }

    @Test
    public void testCheckAndEndAuctions() {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("name", "Màn hình 4K");
        productData.put("startingPrice", 20000.0);
        productData.put("durationHours", -1);
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        auctionService.checkAndEndAuctions();

        Map<String, Object> details = auctionService.getAuctionDetails(productId);
        com.auction.shared.model.AuctionSession session = (com.auction.shared.model.AuctionSession) details.get("session");
        assertEquals("FINISHED", session.getStatus(), "Hệ thống phải tự động đóng các phiên đã quá hạn");
    }

    @Test
    public void testFullAuctionLifecycleAndAutoPayment() {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("name", "Đồng hồ Rolex");
        productData.put("startingPrice", 10000.0);
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        String v1 = generateUniqueStr("vip1");
        String v2 = generateUniqueStr("vip2");
        ensureUserExists(v1, "123", v1+"@test.com", "Vip 1", "BIDDER");
        ensureUserExists(v2, "123", v2+"@test.com", "Vip 2", "BIDDER");

        int user1Id = (int) ((Map<String, Object>) auctionService.login(v1, "123").get("user")).get("id");
        int user2Id = (int) ((Map<String, Object>) auctionService.login(v2, "123").get("user")).get("id");

        auctionService.addFunds(user1Id, 100000.0);
        auctionService.addFunds(user2Id, 100000.0);

        auctionService.placeBid(productId, v2, 15000.0);
        auctionService.placeBid(productId, v1, 20000.0);

        auctionService.endAuction(productId);
        Map<String, Object> details = auctionService.getAuctionDetails(productId);
        com.auction.shared.model.AuctionSession session = (com.auction.shared.model.AuctionSession) details.get("session");

        assertEquals("FINISHED", session.getStatus());
        assertEquals(v1, session.getCurrentWinnerName());

        Map<String, Object> invalidPayment = auctionService.processPayment(user2Id, productId);
        assertFalse((Boolean) invalidPayment.get("success"), "Người thua KHÔNG ĐƯỢC phép thanh toán");

        Map<String, Object> firstPayment = auctionService.processPayment(user1Id, productId);
        assertTrue((Boolean) firstPayment.get("success"), "Người thắng thanh toán hợp lệ lần 1");
        assertEquals("PAID", session.getStatus(), "Phiên phải chuyển sang trạng thái PAID");

        Map<String, Object> doublePayment = auctionService.processPayment(user1Id, productId);
        assertFalse((Boolean) doublePayment.get("success"), "KHÔNG ĐƯỢC phép thanh toán đúp");
    }

    @Test
    public void testUpdateAndDeleteProduct() {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("name", "Chuột Logitech");
        productData.put("startingPrice", 10000.0);
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("productId", productId);
        updateData.put("sellerId", "admin");
        updateData.put("name", "Chuột Logitech Pro X");
        Map<String, Object> updateResult = auctionService.updateProduct(updateData);
        assertTrue((Boolean) updateResult.get("success"));

        Map<String, Object> deleteSuccess = auctionService.deleteProduct(productId, "admin");
        assertTrue((Boolean) deleteSuccess.get("success"));
    }

    @Test
    public void testRegisterAndLoginFailures() {
        String testUser = generateUniqueStr("failuser");
        Map<String, Object> regData = new HashMap<>();
        regData.put("username", testUser);
        regData.put("password", "123");
        regData.put("email", testUser + "@fail.com");
        regData.put("fullName", "Test Fail");
        regData.put("role", "BIDDER");
        auctionService.register(regData);

        Map<String, Object> duplicateReg = auctionService.register(regData);
        assertFalse((Boolean) duplicateReg.get("success"), "Trùng username phải báo lỗi");
    }

    @Test
    public void testAutomaticAuctionLifecycleByBackgroundThread() {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("name", "Test Vòng Đời Tự Động");
        productData.put("startingPrice", 10000.0);
        productData.put("durationHours", 24);
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        Map<String, Object> details1 = auctionService.getAuctionDetails(productId);
        com.auction.shared.model.AuctionSession session1 = (com.auction.shared.model.AuctionSession) details1.get("session");
        assertEquals("ACTIVE", session1.getStatus(), "Phiên phải ở trạng thái ACTIVE khi vừa mở");

        session1.setEndTime(LocalDateTime.now().minusSeconds(1));

        auctionService.checkAndEndAuctions();

        assertEquals("FINISHED", session1.getStatus(), "Hệ thống phải tự động chuyển sang FINISHED sau khi hết giờ");
    }
}
