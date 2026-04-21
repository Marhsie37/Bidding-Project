package com.auction.server.service;

import com.auction.shared.model.AuctionSession;
import com.auction.shared.model.BidTransaction;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AuctionService {

    private static AuctionService instance;

    private static final int ANTI_SNIPING_WINDOW_SECONDS = 30;
    private static final int ANTI_SNIPING_EXTENSION_SECONDS = 60;

    private NotificationService notificationService = NotificationService.getInstance();

    private Map<String, Map<String, Object>> usersDB = new ConcurrentHashMap<>();
    private AtomicInteger userIdGenerator = new AtomicInteger(1);

    private Map<Integer, Map<String, Object>> productsDB = new ConcurrentHashMap<>();
    private AtomicInteger productIdGenerator = new AtomicInteger(1);

    private Map<Integer, AuctionSession> sessions = new ConcurrentHashMap<>();

    private AuctionService() {
        // init tài khoản admin test
        Map<String, Object> adminData = new HashMap<>();
        adminData.put("id", 0);
        adminData.put("password", "admin123");
        adminData.put("role", "ADMIN");
        usersDB.put("admin", adminData);
    }

    public static synchronized AuctionService getInstance() {
        if (instance == null) {
            instance = new AuctionService();
        }
        return instance;
    }

    // auth module
    public Map<String, Object> login(String username, String password) {
        Map<String, Object> result = new HashMap<>();
        if (usersDB.containsKey(username)) {
            Map<String, Object> userInfo = usersDB.get(username);
            if (userInfo.get("password").equals(password)) {
                result.put("success", true);
                result.put("message", "Đăng nhập thành công!");
                result.put("role", userInfo.get("role"));
                result.put("user", userInfo); 
                return result;
            }
        }
        result.put("success", false);
        result.put("message", "Sai tài khoản hoặc mật khẩu!");
        return result;
    }

    public Map<String, Object> register(Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        String username = (String) data.get("username");
        String password = (String) data.get("password");

        if (usersDB.containsKey(username)) {
            result.put("success", false);
            result.put("message", "Tên đăng nhập đã tồn tại!");
            return result;
        }

        Map<String, Object> newUser = new HashMap<>();
        newUser.put("id", userIdGenerator.getAndIncrement());
        newUser.put("password", password);
        newUser.put("role", "USER");
        
        usersDB.put(username, newUser);

        result.put("success", true);
        result.put("message", "Đăng ký thành công! Bạn có thể đăng nhập ngay.");
        return result;
    }

    public Map<String, Object> addProduct(Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        int newProductId = productIdGenerator.getAndIncrement();
        String sellerId = (String) data.get("sellerId");
        String productName = (String) data.get("productName");
        double startingPrice = ((Number) data.get("startingPrice")).doubleValue();
        int durationMinutes = ((Number) data.getOrDefault("duration", 60)).intValue();

        // set data sp
        Map<String, Object> product = new HashMap<>(data);
        product.put("productId", newProductId);
        productsDB.put(newProductId, product);

        // tạo session đấu giá luôn khi add sp
        LocalDateTime endTime = LocalDateTime.now().plusMinutes(durationMinutes);
        AuctionSession newSession = new AuctionSession(newProductId, productName, startingPrice, endTime);
        sessions.put(newProductId, newSession);

        result.put("success", true);
        result.put("message", "Đăng bán sản phẩm thành công!");
        result.put("productId", newProductId);
        return result;
    }

    public Map<String, Object> getActiveProducts() {
        Map<String, Object> result = new HashMap<>();
        List<AuctionSession> activeSessions = new ArrayList<>();
        
        for (AuctionSession session : sessions.values()) {
            if ("ACTIVE".equals(session.getStatus())) {
                activeSessions.add(session);
            }
        }
        
        result.put("success", true);
        result.put("products", activeSessions);
        return result;
    }

    public Map<String, Object> getProductDetails(int productId) {
        Map<String, Object> result = new HashMap<>();
        if (productsDB.containsKey(productId)) {
            result.put("success", true);
            result.put("product", productsDB.get(productId));
            result.put("session", sessions.get(productId));
        } else {
            result.put("success", false);
            result.put("message", "Không tìm thấy sản phẩm!");
        }
        return result;
    }

    public Map<String, Object> updateProduct(Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        int productId = ((Number) data.get("productId")).intValue();
        String sellerId = (String) data.get("sellerId");

        if (!productsDB.containsKey(productId)) {
            result.put("success", false);
            result.put("message", "Sản phẩm không tồn tại!");
            return result;
        }

        Map<String, Object> product = productsDB.get(productId);
        if (!sellerId.equals(product.get("sellerId"))) {
            result.put("success", false);
            result.put("message", "Bạn không có quyền sửa sản phẩm của người khác!");
            return result;
        }

        product.putAll(data);
        result.put("success", true);
        result.put("message", "Cập nhật thành công!");
        return result;
    }

    public Map<String, Object> deleteProduct(int productId, String username) {
        Map<String, Object> result = new HashMap<>();
        if (!productsDB.containsKey(productId)) {
            result.put("success", false);
            result.put("message", "Sản phẩm không tồn tại!");
            return result;
        }

        Map<String, Object> product = productsDB.get(productId);
        if (!username.equals(product.get("sellerId"))) {
            result.put("success", false);
            result.put("message", "Bạn không có quyền xóa sản phẩm này!");
            return result;
        }

        productsDB.remove(productId);
        sessions.remove(productId);
        result.put("success", true);
        result.put("message", "Đã xóa sản phẩm thành công!");
        return result;
    }

    public Map<String, Object> getSellerProducts(String username) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> myProducts = new ArrayList<>();
        
        for (Map<String, Object> p : productsDB.values()) {
            if (username.equals(p.get("sellerId"))) {
                myProducts.add(p);
            }
        }
        
        result.put("success", true);
        result.put("products", myProducts);
        return result;
    }

    // main bidding logic
    public Map<String, Object> placeBid(int productId, String username, double bidAmount) {
        Map<String, Object> result = new HashMap<>();
        AuctionSession session = sessions.get(productId);

        if (session == null) {
            result.put("success", false);
            result.put("message", "Lỗi: Phiên đấu giá không tồn tại!");
            return result;
        }

        // validate time + status
        if (!"ACTIVE".equals(session.getStatus()) || LocalDateTime.now().isAfter(session.getEndTime())) {
            session.setStatus("FINISHED");
            result.put("success", false);
            result.put("message", "Lỗi: Phiên đấu giá đã đóng cửa hoặc kết thúc!");
            return result;
        }

        // validate giá
        if (bidAmount <= session.getCurrentPrice()) {
            result.put("success", false);
            result.put("message", "Lỗi: Giá đặt phải cao hơn giá hiện hành!");
            return result;
        }

        int userId = (int) usersDB.get(username).get("id");

        // update winner hiện tại
        session.setCurrentPrice(bidAmount);
        session.setCurrentWinnerId(userId);
        session.setCurrentWinnerName(username);

        // check anti-sniping để extend time
        checkAndExtendAuctionIfNeeded(session);

        result.put("success", true);
        result.put("message", "Đặt giá thành công! Bạn đang dẫn đầu với giá " + bidAmount);
        result.put("currentPrice", bidAmount);
        return result;
    }

    private void checkAndExtendAuctionIfNeeded(AuctionSession auction) {
        LocalDateTime endTime = auction.getEndTime(); 
        LocalDateTime now = LocalDateTime.now();
        
        long secondsRemaining = ChronoUnit.SECONDS.between(now, endTime);
        
        // nếu bid trong 30s cuối -> cộng thêm 60s
        if (secondsRemaining <= ANTI_SNIPING_WINDOW_SECONDS && secondsRemaining > 0) {
            LocalDateTime newEndTime = endTime.plusSeconds(ANTI_SNIPING_EXTENSION_SECONDS);
            auction.setEndTime(newEndTime);
            
            // auction.setExtensionCount(auction.getExtensionCount() + 1);
            
            System.out.println("Anti-sniping: gia hạn thêm " + ANTI_SNIPING_EXTENSION_SECONDS + "s cho sp " + auction.getProductId());

            if (notificationService != null) {
                
            }
        }
    }

    public Map<String, Object> getAuctionDetails(int productId) {
        Map<String, Object> result = new HashMap<>();
        if (sessions.containsKey(productId)) {
            result.put("success", true);
            result.put("session", sessions.get(productId));
        } else {
            result.put("success", false);
            result.put("message", "Phiên đấu giá không tồn tại.");
        }
        return result;
    }

    // auto-bid tính năng
    public Map<String, Object> setAutoBid(int productId, String username, double maxBid, double increment) {
        Map<String, Object> result = new HashMap<>();
        AuctionSession session = sessions.get(productId);
        if (session != null) {
            session.addAutoBid(username, maxBid);
            result.put("success", true);
            result.put("message", "Đã bật chế độ Auto-bid (Tối đa: " + maxBid + ")");
        } else {
            result.put("success", false);
            result.put("message", "Không tìm thấy phiên đấu giá.");
        }
        return result;
    }

    public Map<String, Object> removeAutoBid(int productId, String username) {
        Map<String, Object> result = new HashMap<>();
        AuctionSession session = sessions.get(productId);
        if (session != null) {
            session.removeAutoBid(username);
            result.put("success", true);
            result.put("message", "Đã tắt Auto-bid.");
        }
        return result;
    }

    // admin functions
    public Map<String, Object> getAllUsers() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("users", usersDB);
        return result;
    }

    public Map<String, Object> adminUpdateUser(Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Admin: Cập nhật User thành công");
        return result;
    }

    public Map<String, Object> adminDeleteUser(int userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Admin: Xóa User thành công");
        return result;
    }

    public Map<String, Object> getAllProducts() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("products", productsDB);
        return result;
    }

    public Map<String, Object> adminDeleteProduct(int productId) {
        Map<String, Object> result = new HashMap<>();
        if (productsDB.remove(productId) != null) {
            sessions.remove(productId);
            result.put("success", true);
            result.put("message", "Admin: Đã xóa sổ sản phẩm!");
        } else {
            result.put("success", false);
            result.put("message", "Admin: Sản phẩm không tồn tại!");
        }
        return result;
    }

    // check end time
    public void checkAndEndAuctions() {
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<Integer, AuctionSession> entry : sessions.entrySet()) {
            AuctionSession session = entry.getValue();
            if ("ACTIVE".equals(session.getStatus()) && now.isAfter(session.getEndTime())) {
                session.setStatus("FINISHED");
                System.out.println("Chốt phiên [" + session.getProductName() + "]. Winner: " + session.getCurrentWinnerName());
            }
        }
    }
}
