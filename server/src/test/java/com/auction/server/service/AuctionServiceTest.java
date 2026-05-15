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
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("productName", "Chuột Razer");
        productData.put("startingPrice", 500.0);
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        Map<String, Object> validBid = auctionService.placeBid(productId, "admin", 600.0);
        assertTrue((Boolean) validBid.get("success"), "Đặt giá cao hơn phải thành công");

        Map<String, Object> invalidBid = auctionService.placeBid(productId, "admin", 550.0);
        assertFalse((Boolean) invalidBid.get("success"), "Đặt giá thấp hơn giá hiện tại phải bị hệ thống từ chối");
    }

    @Test
    public void testPlaceBidFailsWhenBidEqualsCurrentPrice() {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("productName", "Màn hình Dell");
        productData.put("startingPrice", 500.0);
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        Map<String, Object> reg1 = new HashMap<>(); reg1.put("username", "user1"); reg1.put("password", "123");
        Map<String, Object> reg2 = new HashMap<>(); reg2.put("username", "user2"); reg2.put("password", "123");
        auctionService.register(reg1);
        auctionService.register(reg2);

        auctionService.placeBid(productId, "user1", 600.0);

        Map<String, Object> invalidBid = auctionService.placeBid(productId, "user2", 600.0);
        assertFalse((Boolean) invalidBid.get("success"), "Đặt giá bằng đúng giá hiện hành phải bị từ chối");
    }

    @Test
    public void testPlaceBidFailsWhenAuctionNotFound() {
        Map<String, Object> invalidBid = auctionService.placeBid(9999, "admin", 1000.0);
        assertFalse((Boolean) invalidBid.get("success"), "Phiên đấu giá không tồn tại phải báo lỗi");
    }

    @Test
    public void testPlaceBidFailsWhenAuctionClosed() {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("productName", "Bàn phím cơ");
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
        productData.put("productName", "Màn hình 4K");
        productData.put("startingPrice", 2000.0);
        productData.put("duration", -1); // Ép thời gian kết thúc ở quá khứ (âm 1 phút)
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        auctionService.checkAndEndAuctions();

        Map<String, Object> details = auctionService.getAuctionDetails(productId);
        com.auction.shared.model.AuctionSession session = (com.auction.shared.model.AuctionSession) details.get("session");
        assertEquals("FINISHED", session.getStatus(), "Hệ thống phải tự động đóng các phiên đã quá hạn");
    }

    @Test
    public void testFullAuctionLifecycleAndPayment() {
        // Giai đoạn 1: Tạo sản phẩm
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("productName", "Đồng hồ Rolex");
        productData.put("startingPrice", 5000.0);
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        // Đăng ký 2 User và nạp tiền
        Map<String, Object> regData1 = new HashMap<>(); regData1.put("username", "vip1"); regData1.put("password", "123");
        Map<String, Object> regData2 = new HashMap<>(); regData2.put("username", "vip2"); regData2.put("password", "123");
        auctionService.register(regData1);
        auctionService.register(regData2);

        int user1Id = (int) ((Map<String, Object>) auctionService.login("vip1", "123").get("user")).get("id");
        int user2Id = (int) ((Map<String, Object>) auctionService.login("vip2", "123").get("user")).get("id");

        auctionService.addFunds(user1Id, 10000.0); // User 1 có 10k
        auctionService.addFunds(user2Id, 2000.0);  // User 2 có 2k

        // Giai đoạn 2: Cạnh tranh
        auctionService.placeBid(productId, "vip2", 5500.0);
        auctionService.placeBid(productId, "vip1", 6000.0); // vip1 dẫn đầu

        // Giai đoạn 3: Chốt phiên
        auctionService.endAuction(productId);
        Map<String, Object> details = auctionService.getAuctionDetails(productId);
        com.auction.shared.model.AuctionSession session = (com.auction.shared.model.AuctionSession) details.get("session");

        assertEquals("FINISHED", session.getStatus());
        assertEquals("vip1", session.getCurrentWinnerName());

        // Giai đoạn 4: Thanh toán
        Map<String, Object> invalidPayment = auctionService.processPayment(user2Id, productId);
        assertFalse((Boolean) invalidPayment.get("success"), "Người thua không được phép thanh toán");

        Map<String, Object> validPayment = auctionService.processPayment(user1Id, productId);
        assertTrue((Boolean) validPayment.get("success"), "Người thắng thanh toán phải thành công");
        assertEquals("PAID", session.getStatus());

        Map<String, Object> doublePayment = auctionService.processPayment(user1Id, productId);
        assertFalse((Boolean) doublePayment.get("success"), "Không được phép thanh toán đúp");
    }

    @Test
    public void testUpdateAndDeleteProduct() {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("productName", "Chuột Logitech");
        productData.put("startingPrice", 100.0);
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("productId", productId);
        updateData.put("sellerId", "admin");
        updateData.put("productName", "Chuột Logitech Pro X");
        Map<String, Object> updateResult = auctionService.updateProduct(updateData);
        assertTrue((Boolean) updateResult.get("success"));

        Map<String, Object> deleteFail = auctionService.deleteProduct(productId, "hacker");
        assertFalse((Boolean) deleteFail.get("success"));

        Map<String, Object> deleteSuccess = auctionService.deleteProduct(productId, "admin");
        assertTrue((Boolean) deleteSuccess.get("success"));
    }

    @Test
    public void testRegisterAndLoginFailures() {
        Map<String, Object> regData = new HashMap<>();
        regData.put("username", "testuser");
        regData.put("password", "123");
        auctionService.register(regData);

        Map<String, Object> duplicateReg = auctionService.register(regData);
        assertFalse((Boolean) duplicateReg.get("success"));

        Map<String, Object> wrongPass = auctionService.login("testuser", "wrongpass");
        assertFalse((Boolean) wrongPass.get("success"));

        Map<String, Object> noUser = auctionService.login("ghost", "123");
        assertFalse((Boolean) noUser.get("success"));
    }

    @Test
    public void testAutoBidSettings() {
        Map<String, Object> productData = new HashMap<>();
        productData.put("sellerId", "admin");
        productData.put("productName", "Tai nghe Sony");
        productData.put("startingPrice", 500.0);
        Map<String, Object> addResult = auctionService.addProduct(productData);
        int productId = (int) addResult.get("productId");

        Map<String, Object> setAuto = auctionService.setAutoBid(productId, "user1", 2000.0, 10.0);
        assertTrue((Boolean) setAuto.get("success"));

        Map<String, Object> removeAuto = auctionService.removeAutoBid(productId, "user1");
        assertTrue((Boolean) removeAuto.get("success"));

        Map<String, Object> setAutoFail = auctionService.setAutoBid(9999, "user1", 2000.0, 10.0);
        assertFalse((Boolean) setAutoFail.get("success"));
    }

    @Test
    public void testAdminManagementFunctions() {
        Map<String, Object> allUsers = auctionService.getAllUsers();
        assertTrue((Boolean) allUsers.get("success"));
        assertNotNull(allUsers.get("users"));

        Map<String, Object> allProducts = auctionService.getAllProducts();
        assertTrue((Boolean) allProducts.get("success"));
        assertNotNull(allProducts.get("products"));

        Map<String, Object> updateResult = auctionService.adminUpdateUser(new HashMap<>());
        assertTrue((Boolean) updateResult.get("success"));

        Map<String, Object> deleteUserResult = auctionService.adminDeleteUser(1);
        assertTrue((Boolean) deleteUserResult.get("success"));
    }

    @Test
    public void testAddFundsLogic() {
        Map<String, Object> regData = new HashMap<>();
        regData.put("username", "rich_kid");
        regData.put("password", "123");
        auctionService.register(regData);

        int userId = (int) ((Map<String, Object>) auctionService.login("rich_kid", "123").get("user")).get("id");

        Map<String, Object> invalidFund = auctionService.addFunds(userId, -50.0);
        assertFalse((Boolean) invalidFund.get("success"));

        Map<String, Object> ghostFund = auctionService.addFunds(9999, 100.0);
        assertFalse((Boolean) ghostFund.get("success"));
    }
}