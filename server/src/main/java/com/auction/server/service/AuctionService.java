package com.auction.server.service;

import com.auction.shared.model.AuctionSession;
import com.auction.shared.model.BidTransaction;

import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuctionService {

    private static volatile AuctionService instance;
    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);
    private static final int ANTI_SNIPING_WINDOW_SECONDS = 30;
    private static final int ANTI_SNIPING_EXTENSION_SECONDS = 60;

    private NotificationService notificationService = NotificationService.getInstance();

    private Map<String, Map<String, Object>> usersDB = new ConcurrentHashMap<>();
    private AtomicInteger userIdGenerator = new AtomicInteger(1);

    private Map<Integer, Map<String, Object>> productsDB = new ConcurrentHashMap<>();
    private AtomicInteger productIdGenerator = new AtomicInteger(1);

    private Map<Integer, AuctionSession> sessions = new ConcurrentHashMap<>();

    private AuctionService() {
        Map<String, Object> adminData = new HashMap<>();
        adminData.put("id", 0);
        adminData.put("password", "admin123");
        adminData.put("role", "ADMIN");
        adminData.put("balance", 0.0); // Thêm số dư khởi tạo cho admin
        usersDB.put("admin", adminData);
    }

    public static AuctionService getInstance() {
        if (instance == null) {
            synchronized (AuctionService.class) {
                if (instance == null) {
                    instance = new AuctionService();
                }
            }
        }
        return instance;
    }

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
        newUser.put("balance", 0.0); // Khởi tạo số dư = 0 cho user mới

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

        Map<String, Object> product = new HashMap<>(data);
        product.put("productId", newProductId);
        productsDB.put(newProductId, product);

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

    public Map<String, Object> placeBid(int productId, String username, double bidAmount) {
        Map<String, Object> result = new HashMap<>();
        AuctionSession session = sessions.get(productId);

        if (session == null) {
            result.put("success", false);
            result.put("message", "Lỗi: Phiên đấu giá không tồn tại!");
            return result;
        }

        if (!"ACTIVE".equals(session.getStatus()) || LocalDateTime.now().isAfter(session.getEndTime())) {
            session.setStatus("FINISHED");
            result.put("success", false);
            result.put("message", "Lỗi: Phiên đấu giá đã đóng cửa hoặc kết thúc!");
            return result;
        }

        if (bidAmount <= session.getCurrentPrice()) {
            result.put("success", false);
            result.put("message", "Lỗi: Giá đặt phải cao hơn giá hiện hành!");
            return result;
        }

        int userId = (int) usersDB.get(username).get("id");

        session.setCurrentPrice(bidAmount);
        session.setCurrentWinnerId(userId);
        session.setCurrentWinnerName(username);

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

        if (secondsRemaining <= ANTI_SNIPING_WINDOW_SECONDS && secondsRemaining > 0) {
            LocalDateTime newEndTime = endTime.plusSeconds(ANTI_SNIPING_EXTENSION_SECONDS);
            auction.setEndTime(newEndTime);

            logger.info("Anti-sniping: gia hạn thêm " + ANTI_SNIPING_EXTENSION_SECONDS + "s cho sp " + auction.getProductId());
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

    public List<AuctionSession> getActiveAuctions() {
        List<AuctionSession> activeAuctions = new ArrayList<>();
        for (AuctionSession session : sessions.values()) {
            if ("ACTIVE".equals(session.getStatus())) {
                activeAuctions.add(session);
            }
        }
        return activeAuctions;
    }

    public void endAuction(int productId) {
        AuctionSession session = sessions.get(productId);
        if (session != null && "ACTIVE".equals(session.getStatus())) {
            session.setStatus("FINISHED");
            logger.info("Đã chốt phiên thủ công [" + session.getProductName() + "]. Winner: " + session.getCurrentWinnerName());
        }
    }

    public void checkAndEndAuctions() {
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<Integer, AuctionSession> entry : sessions.entrySet()) {
            AuctionSession session = entry.getValue();
            if ("ACTIVE".equals(session.getStatus()) && now.isAfter(session.getEndTime())) {
                session.setStatus("FINISHED");
                logger.info("Chốt phiên [" + session.getProductName() + "]. Winner: " + session.getCurrentWinnerName());
            }
        }
    }

    public synchronized Map<String, Object> addFunds(int userId, double amount) {
        Map<String, Object> result = new HashMap<>();
        if (amount <= 0) {
            result.put("success", false);
            result.put("message", "Số tiền nạp phải lớn hơn 0");
            return result;
        }

        Map<String, Object> targetUser = null;
        for (Map<String, Object> user : usersDB.values()) {
            if ((int) user.get("id") == userId) {
                targetUser = user;
                break;
            }
        }

        if (targetUser != null) {
            double currentBalance = ((Number) targetUser.getOrDefault("balance", 0.0)).doubleValue();
            double newBalance = currentBalance + amount;
            targetUser.put("balance", newBalance);

            result.put("success", true);
            result.put("message", "Nạp tiền thành công");
            result.put("balance", newBalance);
        } else {
            result.put("success", false);
            result.put("message", "Nạp tiền thất bại: Không tìm thấy người dùng");
        }
        return result;
    }

    public synchronized Map<String, Object> processPayment(int userId, int auctionId) {
        Map<String, Object> result = new HashMap<>();

        // Kiểm tra auction đã xong chưa
        AuctionSession auction = sessions.get(auctionId);
        if (auction == null || !"FINISHED".equals(auction.getStatus())) {
            result.put("success", false);
            result.put("message", "Phiên đấu giá chưa kết thúc hoặc không tồn tại");
            return result;
        }

        if (auction.getCurrentWinnerId() != userId) {
            result.put("success", false);
            result.put("message", "Bạn không phải người thắng cuộc");
            return result;
        }

        if ("PAID".equals(auction.getStatus())) {
            result.put("success", false);
            result.put("message", "Phiên đấu giá đã được thanh toán");
            return result;
        }

        // Tìm thông tin user
        Map<String, Object> targetUser = null;
        for (Map<String, Object> user : usersDB.values()) {
            if ((int) user.get("id") == userId) {
                targetUser = user;
                break;
            }
        }

        if (targetUser == null) {
            result.put("success", false);
            result.put("message", "Lỗi: Không tìm thấy thông tin người dùng");
            return result;
        }

        // Ktra số dư
        double balance = ((Number) targetUser.getOrDefault("balance", 0.0)).doubleValue();
        double amount = auction.getCurrentPrice();

        if (balance < amount) {
            result.put("success", false);
            result.put("message", "Số dư không đủ. Cần: " + amount + ", Hiện có: " + balance);
            return result;
        }

        // Thực hiện thanh toán 
        targetUser.put("balance", balance - amount);
        auction.setStatus("PAID");

        double newBalance = ((Number) targetUser.get("balance")).doubleValue();
        result.put("success", true);
        result.put("message", "Thanh toán thành công!");
        result.put("balance", newBalance);
        result.put("amount", amount);

        return result;
    }

    public Map<String, Object> getUserBalance(int userId) {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> targetUser = null;
        for (Map<String, Object> user : usersDB.values()) {
            if ((int) user.get("id") == userId) {
                targetUser = user;
                break;
            }
        }

        if (targetUser != null) {
            double balance = ((Number) targetUser.getOrDefault("balance", 0.0)).doubleValue();
            result.put("success", true);
            result.put("balance", balance);
            result.put("message", "Số dư: " + balance);
        } else {
            result.put("success", false);
            result.put("message", "Không tìm thấy người dùng");
        }
        return result;
    }
}
