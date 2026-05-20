package com.auction.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
        assertTrue((Boolean) result.get("success"), "Tài khoản admin phải đăng nhập thành công");
    }

    @Test
    public void testAddProductAndGenerateSession() {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("name", "Laptop Gaming Skibidi");
        productData.put("startingPrice", 1000.0);
        productData.put("durationHours", 24);

        Map<String, Object> result = auctionService.addProduct(productData);
        assertTrue((Boolean) result.get("success"), "Thêm sản phẩm phải thành công");
    }

    @Test
    public void testPlaceBidLogic() {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("name", "Chuột Razer");
        productData.put("startingPrice", 500.0);
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        int adminId = (int) ((Map<String, Object>) auctionService.login("admin", "admin123").get("user")).get("id");
        auctionService.addFunds(adminId, 5000.0);

        Map<String, Object> validBid = auctionService.placeBid(productId, "admin", 600.0);
        assertTrue((Boolean) validBid.get("success"), "Đặt giá cao hơn và đủ tiền thì phải thành công");

        Map<String, Object> invalidBid = auctionService.placeBid(productId, "admin", 550.0);
        assertFalse((Boolean) invalidBid.get("success"), "Đặt giá thấp hơn giá hiện tại phải bị hệ thống từ chối");
    }

    @Test
    public void testPlaceBidFailsWhenBidEqualsCurrentPrice() {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("name", "Màn hình Dell");
        productData.put("startingPrice", 500.0);
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        String u1 = generateUniqueStr("user1");
        String u2 = generateUniqueStr("user2");
        ensureUserExists(u1, "123", u1+"@test.com", "User 1", "BIDDER");
        ensureUserExists(u2, "123", u2+"@test.com", "User 2", "BIDDER");

        int id1 = (int) ((Map<String, Object>) auctionService.login(u1, "123").get("user")).get("id");
        int id2 = (int) ((Map<String, Object>) auctionService.login(u2, "123").get("user")).get("id");
        auctionService.addFunds(id1, 1000.0);
        auctionService.addFunds(id2, 1000.0);

        auctionService.placeBid(productId, u1, 600.0);

        Map<String, Object> invalidBid = auctionService.placeBid(productId, u2, 600.0);
        assertFalse((Boolean) invalidBid.get("success"), "Đặt giá bằng đúng giá hiện hành phải bị từ chối");
    }

    @Test
    public void testPlaceBidFailsWhenAuctionNotFound() {
        Map<String, Object> invalidBid = auctionService.placeBid(99999, "admin", 1000.0);
        assertFalse((Boolean) invalidBid.get("success"), "Phiên đấu giá không tồn tại phải báo lỗi");
    }

    @Test
    public void testPlaceBidFailsWhenAuctionClosed() {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("name", "Bàn phím cơ");
        productData.put("startingPrice", 500.0);
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        auctionService.endAuction(productId);

        Map<String, Object> lateBid = auctionService.placeBid(productId, "admin", 1000.0);
        assertFalse((Boolean) lateBid.get("success"), "Phiên đã đóng cửa thì không được phép đặt giá");
    }

    @Test
    public void testCheckAndEndAuctions() {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("name", "Màn hình 4K");
        productData.put("startingPrice", 2000.0);
        productData.put("durationHours", -1); // Ép thời gian kết thúc ở quá khứ
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
        productData.put("startingPrice", 5000.0);
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        String v1 = generateUniqueStr("vip1");
        String v2 = generateUniqueStr("vip2");
        ensureUserExists(v1, "123", v1+"@test.com", "Vip 1", "BIDDER");
        ensureUserExists(v2, "123", v2+"@test.com", "Vip 2", "BIDDER");

        int user1Id = (int) ((Map<String, Object>) auctionService.login(v1, "123").get("user")).get("id");
        int user2Id = (int) ((Map<String, Object>) auctionService.login(v2, "123").get("user")).get("id");

        auctionService.addFunds(user1Id, 10000.0);
        auctionService.addFunds(user2Id, 10000.0); // FIX: Nạp đủ 10k cho V2 để không bị hụt tiền khi bid

        auctionService.placeBid(productId, v2, 5500.0);
        auctionService.placeBid(productId, v1, 6000.0);

        auctionService.endAuction(productId);
        Map<String, Object> details = auctionService.getAuctionDetails(productId);
        com.auction.shared.model.AuctionSession session = (com.auction.shared.model.AuctionSession) details.get("session");

        assertEquals("FINISHED", session.getStatus());
        assertEquals(v1, session.getCurrentWinnerName());

        Map<String, Object> balanceInfo = auctionService.getUserBalance(user1Id);
        assertEquals(4000.0, (Double) balanceInfo.get("balance"), "Hệ thống phải tự động trừ tiền của người thắng khi endAuction");
    }

    @Test
    public void testUpdateAndDeleteProduct() {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("name", "Chuột Logitech");
        productData.put("startingPrice", 100.0);
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("productId", productId);
        updateData.put("sellerId", "admin");
        updateData.put("name", "Chuột Logitech Pro X");
        Map<String, Object> updateResult = auctionService.updateProduct(updateData);
        assertTrue((Boolean) updateResult.get("success"));

        Map<String, Object> deleteFail = auctionService.deleteProduct(productId, "hacker_boi");
        assertFalse((Boolean) deleteFail.get("success"));

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

        Map<String, Object> wrongPass = auctionService.login(testUser, "wrongpass");
        assertFalse((Boolean) wrongPass.get("success"), "Sai mật khẩu phải chặn");
    }

    @Test
    public void testAutoBidSettings() {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("name", "Tai nghe Sony");
        productData.put("startingPrice", 500.0);
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        Map<String, Object> setAuto = auctionService.setAutoBid(productId, "admin", 2000.0, 10.0);
        assertTrue((Boolean) setAuto.get("success"));

        Map<String, Object> removeAuto = auctionService.removeAutoBid(productId, "admin");
        assertTrue((Boolean) removeAuto.get("success"));
    }

    @Test
    public void testAddFundsLogic() {
        String richKid = generateUniqueStr("rich_kid");
        ensureUserExists(richKid, "123", richKid+"@money.com", "Rich Kid", "BIDDER");

        int userId = (int) ((Map<String, Object>) auctionService.login(richKid, "123").get("user")).get("id");

        Map<String, Object> invalidFund = auctionService.addFunds(userId, -50.0);
        assertFalse((Boolean) invalidFund.get("success"), "Tiền âm phải chặn");

        Map<String, Object> ghostFund = auctionService.addFunds(999999, 100.0);
        assertFalse((Boolean) ghostFund.get("success"), "User ảo phải chặn");
    }
}