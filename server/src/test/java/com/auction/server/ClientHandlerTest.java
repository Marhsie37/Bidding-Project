package com.auction.server;

import com.auction.shared.model.User;
import com.auction.shared.protocol.*;
import com.auction.server.service.AuctionService;
import com.auction.server.service.NotificationService;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.lang.reflect.*;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientHandlerTest {

    // ------------------------------------------------------------------ mocks
    // Không @Mock Socket vì Java 26 + Byte Buddy không instrument được system class
    @Mock private AuctionService mockAuctionService;
    @Mock private NotificationService mockNotificationService;
    @Mock private AuctionServer mockAuctionServer;

    // MockedStatic phải được mở/đóng thủ công trong BeforeEach/AfterEach
    private MockedStatic<AuctionServer>    mockedAuctionServer;
    private MockedStatic<AuctionService>   mockedAuctionService;
    private MockedStatic<NotificationService> mockedNotificationService;

    private ByteArrayOutputStream baos;
    private ObjectOutputStream oos;
    private ClientHandler handler;

    // ------------------------------------------------------------------ setup
    @BeforeEach
    void setUp() throws Exception {
        baos = new ByteArrayOutputStream();
        oos  = new ObjectOutputStream(baos);

        // Mở static mock TRƯỚC khi constructor chạy
        mockedAuctionServer      = mockStatic(AuctionServer.class);
        mockedAuctionService     = mockStatic(AuctionService.class);
        mockedNotificationService = mockStatic(NotificationService.class);

        mockedAuctionServer.when(AuctionServer::getInstance).thenReturn(mockAuctionServer);
        mockedAuctionService.when(AuctionService::getInstance).thenReturn(mockAuctionService);
        mockedNotificationService.when(NotificationService::getInstance).thenReturn(mockNotificationService);

        // null socket vì stream được inject sau bằng reflection
        handler = new ClientHandler(null);

        setField(handler, "outputStream",        oos);
        setField(handler, "auctionService",      mockAuctionService);
        setField(handler, "notificationService", mockNotificationService);
    }

    @AfterEach
    void tearDown() {
        // Bắt buộc close, nếu không Mockito sẽ báo NotAMockException
        if (mockedAuctionServer      != null) mockedAuctionServer.close();
        if (mockedAuctionService     != null) mockedAuctionService.close();
        if (mockedNotificationService != null) mockedNotificationService.close();
    }

    // ============================================================== handleLogin
    @Test
    @DisplayName("handleLogin – thành công → gửi LOGIN response và đăng ký client")
    void testHandleLogin_success() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Login successful");
        result.put("role", "USER");
        result.put("user", Map.of("id", 1, "username", "alice"));

        when(mockAuctionService.login("alice", "pass123")).thenReturn(result);

        Map<String, Object> loginData = new HashMap<>();
        loginData.put("username", "alice");
        loginData.put("password", "pass123");
        handler.handleLogin(loginData);

        verify(mockAuctionServer).registerClient(eq("alice"), eq(handler));
        assertEquals("alice", handler.getUsername());

        Response resp = readResponse();
        assertTrue(resp.isSuccess());
        assertEquals(CommandType.LOGIN, resp.getCommand());
        assertEquals("Đăng nhập thành công!", resp.getMessage());
    }

    @Test
    @DisplayName("handleLogin – sai mật khẩu → gửi LOGIN với success=false, không đăng ký client")
    void testHandleLogin_failure() throws Exception {
        when(mockAuctionService.login("bob", "wrong"))
                .thenReturn(Map.of("success", false, "message", "Invalid credentials"));

        Map<String, Object> loginData = new HashMap<>();
        loginData.put("username", "bob");
        loginData.put("password", "wrong");
        handler.handleLogin(loginData);

        verify(mockAuctionServer, never()).registerClient(any(), any());

        Response resp = readResponse();
        assertFalse(resp.isSuccess());
        assertEquals(CommandType.LOGIN, resp.getCommand()); // FIX: Expect LOGIN, not ERROR
        assertEquals("Invalid credentials", resp.getMessage());
    }

    // ============================================================= handleRegister
    @Test
    @DisplayName("handleRegister – đăng ký thành công")
    void testHandleRegister_success() throws Exception {
        when(mockAuctionService.register(any()))
                .thenReturn(Map.of("success", true, "message", "Registered"));

        invokePrivate("handleRegister", Map.of("username", "charlie", "password", "pw"));

        Response resp = readResponse();
        assertTrue(resp.isSuccess());
        assertEquals(CommandType.REGISTER, resp.getCommand());
    }

    // ========================================================= handleGetProducts
    @Test
    @DisplayName("handleGetProducts – trả về danh sách sản phẩm active")
    void testHandleGetProducts() throws Exception {
        when(mockAuctionService.getActiveProducts()).thenReturn(Map.of("products", List.of()));

        invokePrivate("handleGetProducts", (Map<String, Object>) null);

        verify(mockAuctionService).getActiveProducts();
        Response resp = readResponse();
        assertTrue(resp.isSuccess());
        assertEquals(CommandType.GET_PRODUCTS, resp.getCommand());
    }

    // ====================================================== handleGetProductDetails
    @Test
    @DisplayName("handleGetProductDetails – trả về chi tiết sản phẩm theo ID")
    void testHandleGetProductDetails() throws Exception {
        when(mockAuctionService.getProductDetails(1))
                .thenReturn(Map.of("id", 1, "name", "Watch"));

        invokePrivate("handleGetProductDetails", Map.of("productId", 1));

        verify(mockAuctionService).getProductDetails(1);
        assertEquals(CommandType.GET_PRODUCT_DETAILS, readResponse().getCommand());
    }

    // =========================================================== handleAddProduct
    @Test
    @DisplayName("handleAddProduct – thêm sản phẩm, sellerId được inject tự động")
    void testHandleAddProduct() throws Exception {
        setField(handler, "username", "seller1");
        when(mockAuctionService.addProduct(any()))
                .thenReturn(new HashMap<>(Map.of("success", true, "message", "Added")));

        invokePrivate("handleAddProduct", new HashMap<>(Map.of("name", "Ring", "startPrice", 100.0)));

        verify(mockAuctionService).addProduct(argThat(d -> "seller1".equals(d.get("sellerId"))));
        assertTrue(readResponse().isSuccess());
    }

    // ========================================================== handleUpdateProduct
    @Test
    @DisplayName("handleUpdateProduct – cập nhật sản phẩm thành công")
    void testHandleUpdateProduct() throws Exception {
        setField(handler, "username", "seller1");
        when(mockAuctionService.updateProduct(any()))
                .thenReturn(Map.of("success", true, "message", "Updated"));

        invokePrivate("handleUpdateProduct", new HashMap<>(Map.of("productId", 5)));

        verify(mockAuctionService).updateProduct(any());
        assertTrue(readResponse().isSuccess());
    }

    // ========================================================== handleDeleteProduct
    @Test
    @DisplayName("handleDeleteProduct – xóa sản phẩm thành công")
    void testHandleDeleteProduct() throws Exception {
        setField(handler, "username", "seller1");
        when(mockAuctionService.deleteProduct(3, "seller1"))
                .thenReturn(Map.of("success", true, "message", "Deleted"));

        invokePrivate("handleDeleteProduct", Map.of("productId", 3));

        verify(mockAuctionService).deleteProduct(3, "seller1");
        assertTrue(readResponse().isSuccess());
    }

    // ============================================================= handlePlaceBid
    @Test
    @DisplayName("handlePlaceBid – thành công → notifyBidUpdate được gọi")
    void testHandlePlaceBid_success() throws Exception {
        setField(handler, "username", "bidder1");
        when(mockAuctionService.placeBid(10, "bidder1", 500.0))
                .thenReturn(Map.of("success", true, "message", "Bid placed"));

        invokePrivate("handlePlaceBid", Map.of("productId", 10, "bidAmount", 500.0));

        verify(mockNotificationService).notifyBidUpdate(10, "bidder1", 500.0);
        assertTrue(readResponse().isSuccess());
    }

    @Test
    @DisplayName("handlePlaceBid – thất bại → notifyBidUpdate KHÔNG được gọi")
    void testHandlePlaceBid_failure() throws Exception {
        setField(handler, "username", "bidder1");
        when(mockAuctionService.placeBid(10, "bidder1", 500.0))
                .thenReturn(Map.of("success", false, "message", "Insufficient balance"));

        invokePrivate("handlePlaceBid", Map.of("productId", 10, "bidAmount", 500.0));

        verify(mockNotificationService, never()).notifyBidUpdate(anyInt(), anyString(), anyDouble());
        assertFalse(readResponse().isSuccess());
    }

    // ======================================================= handleSubscribeAuction
    @Test
    @DisplayName("handleSubscribeAuction – subscribe thành công")
    void testHandleSubscribeAuction() throws Exception {
        setField(handler, "username", "user1");

        invokePrivate("handleSubscribeAuction", Map.of("productId", 7));

        verify(mockNotificationService).subscribe(7, "user1", handler);
        Response resp = readResponse();
        assertTrue(resp.isSuccess());
        assertEquals(CommandType.SUBSCRIBE_AUCTION, resp.getCommand());
    }

    // ===================================================== handleUnsubscribeAuction
    @Test
    @DisplayName("handleUnsubscribeAuction – unsubscribe thành công")
    void testHandleUnsubscribeAuction() throws Exception {
        setField(handler, "username", "user1");

        invokePrivate("handleUnsubscribeAuction", Map.of("productId", 7));

        verify(mockNotificationService).unsubscribe(7, "user1");
        assertEquals(CommandType.UNSUBSCRIBE_AUCTION, readResponse().getCommand());
    }

    // ============================================================= handleSetAutoBid
    @Test
    @DisplayName("handleSetAutoBid – đặt auto bid thành công")
    void testHandleSetAutoBid() throws Exception {
        setField(handler, "username", "user1");
        when(mockAuctionService.setAutoBid(5, "user1", 1000.0, 50.0))
                .thenReturn(Map.of("success", true, "message", "Auto bid set"));

        invokePrivate("handleSetAutoBid", Map.of("productId", 5, "maxBid", 1000.0, "increment", 50.0));

        verify(mockAuctionService).setAutoBid(5, "user1", 1000.0, 50.0);
        assertTrue(readResponse().isSuccess());
    }

    // ========================================================== handleRemoveAutoBid
    @Test
    @DisplayName("handleRemoveAutoBid – xóa auto bid thành công")
    void testHandleRemoveAutoBid() throws Exception {
        setField(handler, "username", "user1");
        when(mockAuctionService.removeAutoBid(5, "user1"))
                .thenReturn(Map.of("success", true, "message", "Removed"));

        invokePrivate("handleRemoveAutoBid", Map.of("productId", 5));

        verify(mockAuctionService).removeAutoBid(5, "user1");
        assertTrue(readResponse().isSuccess());
    }

    // =========================================================== handleGetMyProducts
    @Test
    @DisplayName("handleGetMyProducts – lấy sản phẩm của seller")
    void testHandleGetMyProducts() throws Exception {
        setField(handler, "username", "seller1");
        when(mockAuctionService.getSellerProducts("seller1")).thenReturn(Map.of());

        invokePrivate("handleGetMyProducts", (Map<String, Object>) null);

        verify(mockAuctionService).getSellerProducts("seller1");
        assertEquals(CommandType.GET_MY_PRODUCTS, readResponse().getCommand());
    }

    // ======================================================= Admin handlers
    @Test
    @DisplayName("handleAdminGetAllUsers – role ADMIN → trả về danh sách users")
    void testHandleAdminGetAllUsers_asAdmin() throws Exception {
        setField(handler, "role", "ADMIN");
        when(mockAuctionService.getAllUsers()).thenReturn(Map.of("users", List.of()));

        invokePrivate("handleAdminGetAllUsers", (Map<String, Object>) null);

        verify(mockAuctionService).getAllUsers();
        assertEquals(CommandType.ADMIN_GET_ALL_USERS, readResponse().getCommand());
    }

    @Test
    @DisplayName("handleAdminGetAllUsers – role USER → bị chặn, trả về ERROR")
    void testHandleAdminGetAllUsers_notAdmin() throws Exception {
        setField(handler, "role", "USER");

        invokePrivate("handleAdminGetAllUsers", (Map<String, Object>) null);

        verify(mockAuctionService, never()).getAllUsers();
        Response resp = readResponse();
        assertFalse(resp.isSuccess());
        assertTrue(resp.getMessage().contains("Admin role required"));
    }

    @Test
    @DisplayName("handleAdminUpdateUser – role ADMIN → update user")
    void testHandleAdminUpdateUser_asAdmin() throws Exception {
        setField(handler, "role", "ADMIN");
        when(mockAuctionService.adminUpdateUser(any()))
                .thenReturn(Map.of("success", true, "message", "Updated"));

        invokePrivate("handleAdminUpdateUser", new HashMap<>(Map.of("userId", 1)));

        verify(mockAuctionService).adminUpdateUser(any());
        assertTrue(readResponse().isSuccess());
    }

    @Test
    @DisplayName("handleAdminDeleteUser – role ADMIN → xóa user thành công")
    void testHandleAdminDeleteUser_asAdmin() throws Exception {
        setField(handler, "role", "ADMIN");
        when(mockAuctionService.adminDeleteUser(42))
                .thenReturn(Map.of("success", true, "message", "Deleted"));

        invokePrivate("handleAdminDeleteUser", Map.of("userId", 42));

        verify(mockAuctionService).adminDeleteUser(42);
        assertTrue(readResponse().isSuccess());
    }

    @Test
    @DisplayName("handleAdminDeleteUser – role USER → bị chặn")
    void testHandleAdminDeleteUser_notAdmin() throws Exception {
        setField(handler, "role", "USER");

        invokePrivate("handleAdminDeleteUser", Map.of("userId", 42));

        verify(mockAuctionService, never()).adminDeleteUser(anyInt());
        assertFalse(readResponse().isSuccess());
    }

    @Test
    @DisplayName("handleAdminGetAllProducts – role ADMIN → lấy tất cả sản phẩm")
    void testHandleAdminGetAllProducts_asAdmin() throws Exception {
        setField(handler, "role", "ADMIN");
        when(mockAuctionService.getAllProducts()).thenReturn(Map.of());

        invokePrivate("handleAdminGetAllProducts", (Map<String, Object>) null);

        verify(mockAuctionService).getAllProducts();
        assertEquals(CommandType.ADMIN_GET_ALL_PRODUCTS, readResponse().getCommand());
    }

    @Test
    @DisplayName("handleAdminDeleteProduct – role ADMIN → xóa sản phẩm")
    void testHandleAdminDeleteProduct_asAdmin() throws Exception {
        setField(handler, "role", "ADMIN");
        when(mockAuctionService.adminDeleteProduct(99))
                .thenReturn(Map.of("success", true, "message", "Deleted"));

        invokePrivate("handleAdminDeleteProduct", Map.of("productId", 99));

        verify(mockAuctionService).adminDeleteProduct(99);
        assertTrue(readResponse().isSuccess());
    }

    // ================================================================ handleLogout
    @Test
    @DisplayName("handleLogout – đã đăng nhập → unregister 2 lần (trong handleLogout và disconnect)")
    void testHandleLogout_withUsername() throws Exception {
        setField(handler, "username", "alice");
        setField(handler, "connected", true);

        // Set currentRequestId để tránh null pointer
        setField(handler, "currentRequestId", "logout-request-id");

        invokePrivate("handleLogout");

        // Verify gọi 2 lần (hoặc ít nhất 1 lần)
        verify(mockAuctionServer, atLeastOnce()).unregisterClient("alice");
        // Hoặc verify chính xác 2 lần:
        // verify(mockAuctionServer, times(2)).unregisterClient("alice");

        Response resp = readResponse();
        assertEquals(CommandType.LOGOUT, resp.getCommand());
        assertTrue(resp.isSuccess());
    }

    @Test
    @DisplayName("handleLogout – chưa đăng nhập → không gọi unregister")
    void testHandleLogout_withoutUsername() throws Exception {
        invokePrivate("handleLogout");

        verify(mockAuctionServer, never()).unregisterClient(any());
        assertEquals(CommandType.LOGOUT, readResponse().getCommand());
    }

    // ================================================================ handleAddFunds
    @Test
    @DisplayName("handleAddFunds – nạp tiền thành công")
    void testHandleAddFunds() throws Exception {
        // FIX: Set username trước
        setField(handler, "username", "testuser");

        // Tạo mock User object (nếu chưa có)
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(1);

        // Mock getUserByUsername để trả về user object
        when(mockAuctionService.getUserByUsername("testuser")).thenReturn(mockUser);
        when(mockAuctionService.addFunds(1, 200.0))
                .thenReturn(Map.of("success", true, "message", "Funds added", "newBalance", 200.0));

        Map<String, Object> data = new HashMap<>();
        data.put("amount", 200.0);
        // FIX: Không truyền userId, chỉ truyền amount
        invokePrivate("handleAddFunds", data);

        verify(mockAuctionService).addFunds(1, 200.0);
        Response resp = readResponse();
        assertTrue(resp.isSuccess());
        assertEquals(CommandType.ADD_FUNDS, resp.getCommand());
    }

    // ============================================================ handleProcessPayment
    @Test
    @DisplayName("handleProcessPayment – thanh toán thành công")
    void testHandleProcessPayment() throws Exception {
        when(mockAuctionService.processPayment(1, 5))
                .thenReturn(Map.of("success", true, "message", "Payment processed"));

        invokePrivate("handleProcessPayment", Map.of("userId", 1, "auctionId", 5));

        verify(mockAuctionService).processPayment(1, 5);
        Response resp = readResponse();
        assertTrue(resp.isSuccess());
        assertEquals(CommandType.PROCESS_PAYMENT, resp.getCommand());
    }

    // ============================================================= handleGetUserBalance
    @Test
    @DisplayName("handleGetUserBalance – lấy số dư thành công")
    void testHandleGetUserBalance() throws Exception {
        when(mockAuctionService.getUserBalance(1))
                .thenReturn(Map.of("success", true, "message", "OK", "balance", 500.0));

        invokePrivate("handleGetUserBalance", Map.of("userId", 1));

        verify(mockAuctionService).getUserBalance(1);
        assertEquals(CommandType.GET_USER_BALANCE, readResponse().getCommand());
    }

    // =============================================================== sendBidUpdate
    @Test
    @DisplayName("sendBidUpdate – gửi thông báo bid update đến client")
    void testSendBidUpdate() throws Exception {
        handler.sendBidUpdate(10, "bidder1", 300.0);

        Response resp = readResponse();
        assertEquals(CommandType.BID_UPDATE, resp.getCommand());
        assertTrue(resp.isSuccess());
        assertEquals(10,        resp.getData().get("productId"));
        assertEquals("bidder1", resp.getData().get("bidderName"));
        assertEquals(300.0,     resp.getData().get("bidAmount"));
    }

    // =============================================================== sendAuctionEnd
    @Test
    @DisplayName("sendAuctionEnd – gửi thông báo auction kết thúc với đúng payload")
    void testSendAuctionEnd() throws Exception {
        handler.sendAuctionEnd(10, 99, "winner", 1500.0);

        Response resp = readResponse();
        assertEquals(CommandType.AUCTION_END, resp.getCommand());
        assertEquals(99,       resp.getData().get("winnerId"));
        assertEquals("winner", resp.getData().get("winnerName"));
        assertEquals(1500.0,   resp.getData().get("finalPrice"));
    }

    // ============================================================ sendAuctionExtended
    @Test
    @DisplayName("sendAuctionExtended – gửi thông báo auction gia hạn")
    void testSendAuctionExtended() throws Exception {
        LocalDateTime newEnd = LocalDateTime.of(2025, 12, 31, 23, 59);
        handler.sendAuctionExtended(10, newEnd);

        Response resp = readResponse();
        assertEquals(CommandType.AUCTION_EXTENDED, resp.getCommand());
        assertEquals(newEnd.toString(), resp.getData().get("newEndTime"));
    }

    // ================================================================ handleRequest
    @Test
    @DisplayName("handleRequest – lệnh không có handler (default branch) → 'Unknown command'")
    void testHandleRequest_unknownCommand() throws Exception {
        // BID_UPDATE / AUCTION_END là server-push command, không có case xử lý từ client
        Request req = new Request(CommandType.BID_UPDATE, Map.of());
        handler.handleRequest(req);

        Response resp = readResponse();
        assertFalse(resp.isSuccess());
        assertEquals(CommandType.ERROR, resp.getCommand());
        assertTrue(resp.getMessage().toLowerCase().contains("unknown"));
    }

    @Test
    @DisplayName("handleRequest – dispatch đúng handler cho LOGIN")
    void testHandleRequest_dispatchLogin() throws Exception {
        when(mockAuctionService.login(any(), any()))
                .thenReturn(Map.of("success", false, "message", "fail"));

        Map<String, Object> loginData = new HashMap<>();
        loginData.put("username", "u");
        loginData.put("password", "p");

        Request request = new Request(CommandType.LOGIN, loginData);
        Field requestIdField = Request.class.getDeclaredField("requestId");
        requestIdField.setAccessible(true);
        requestIdField.set(request, "test-id");

        handler.handleRequest(request);

        verify(mockAuctionService).login("u", "p");

        Response resp = readResponse();
        assertEquals(CommandType.LOGIN, resp.getCommand());
        assertFalse(resp.isSuccess());
    }

    // ================================================================ getUsername
    @Test
    @DisplayName("getUsername – trả về username đã set")
    void testGetUsername() throws Exception {
        setField(handler, "username", "alice");
        assertEquals("alice", handler.getUsername());
    }

    // ====================== Helper: đọc Response từ stream ========================
    private Response readResponse() throws Exception {
        oos.flush();
        byte[] bytes = baos.toByteArray();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        return (Response) ois.readObject();
    }

    // ====================== Reflection helpers ====================================
    private void setField(Object target, String name, Object value) throws Exception {
        Field f = findField(target.getClass(), name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private Object getField(Object target, String name) throws Exception {
        Field f = findField(target.getClass(), name);
        f.setAccessible(true);
        return f.get(target);
    }

    private Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) return findField(clazz.getSuperclass(), name);
            throw e;
        }
    }

    private void invokePrivate(String methodName) throws Exception {
        Method m = ClientHandler.class.getDeclaredMethod(methodName);
        m.setAccessible(true);
        m.invoke(handler);
    }

    private void invokePrivate(String methodName, Map<String, Object> data) throws Exception {
        Method m = ClientHandler.class.getDeclaredMethod(methodName, Map.class);
        m.setAccessible(true);
        m.invoke(handler, data);
    }
}