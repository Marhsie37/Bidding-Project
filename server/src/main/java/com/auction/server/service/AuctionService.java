package com.auction.server.service;

import com.auction.server.dao.BidDAO;
import com.auction.server.dao.ProductDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.dao.UserProductDAO;
import com.auction.shared.model.AuctionSession;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Product;
import com.auction.shared.model.User;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AuctionService {

    private static AuctionService instance;

    private UserDAO userDAO;

    private BidDAO bidDAO = new BidDAO();

    private UserProductDAO userProductDAO = new UserProductDAO();

    private ProductDAO productDAO = new ProductDAO();

    private static final int ANTI_SNIPING_WINDOW_SECONDS = 30;
    private static final int ANTI_SNIPING_EXTENSION_SECONDS = 60;

    private NotificationService notificationService = NotificationService.getInstance();

    //private Map<String, Map<String, Object>> usersDB = new ConcurrentHashMap<>();
    //private AtomicInteger userIdGenerator = new AtomicInteger(1);



    private Map<Integer, Map<String, Object>> productsDB = new ConcurrentHashMap<>();
    private AtomicInteger productIdGenerator = new AtomicInteger(1);

    private Map<Integer, AuctionSession> sessions = new ConcurrentHashMap<>();

    private AuctionService() {
        Map<String, Object> adminData = new HashMap<>();
        adminData.put("id", 0);
        adminData.put("password", "admin123");
        adminData.put("role", "ADMIN");
        //usersDB.put("admin", adminData);
        userDAO = new UserDAO();
        loadActiveSessions();
    }

    public static synchronized AuctionService getInstance() {
        if (instance == null) {
            instance = new AuctionService();
        }
        return instance;
    }

    public Map<String, Object> login(String username, String password) {
        Map<String, Object> result = new HashMap<>();

        User user = userDAO.findByUsername(username);

        if (user != null && user.getPassword().equals(password)) {
            System.out.println("🔍 DEBUG login - username: " + username + " | status: " + user.getStatus()); // THÊM DÒNG NÀY
            // ✅ THÊM: Kiểm tra bị ban
            if ("BANNED".equalsIgnoreCase(user.getStatus())) {
                result.put("success", false);
                result.put("message", "Tài khoản của bạn đã bị khóa!");
                return result;
            }

            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("role", user.getRole());
            userData.put("fullName", user.getFullName());
            userData.put("email", user.getEmail());
            userData.put("balance", user.getBalance());

            result.put("success", true);
            result.put("userData", userData);
        } else {
            result.put("success", false);
            result.put("message", "Sai tài khoản hoặc mật khẩu!");
        }
        return result;
    }



    public Map<String, Object> register(Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        String username = (String) data.get("username");
        String password = (String) data.get("password");
        String email = (String) data.get("email");
        String fullName = (String) data.get("fullName");
        String role = (String) data.getOrDefault("role", "BIDDER");

        // 1. Kiểm tra dữ liệu đầu vào
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            result.put("success", false);
            result.put("message", "Tài khoản và mật khẩu không được để trống!");
            return result;
        }

        // 2. Kiểm tra username đã tồn tại trong DATABASE chưa
        User existingUser = userDAO.findByUsername(username);
        if (existingUser != null) {
            result.put("success", false);
            result.put("message", "Tên đăng nhập đã tồn tại!");
            return result;
        }

        // 3. Lưu vào DATABASE (MySQL)
        boolean success = userDAO.createUser(username, password, email, fullName, role);

        if (success) {
            result.put("success", true);
            result.put("message", "Đăng ký thành công!");
        } else {
            result.put("success", false);
            result.put("message", "Lỗi database, không thể đăng ký!");
        }
        return result;
    }


    public Map<String, Object> addProduct(Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Lấy thông tin từ request
            String productName = (String) data.get("name");
            String description = (String) data.getOrDefault("description", "");
            double startingPrice = ((Number) data.get("startingPrice")).doubleValue();
            String imageUrl = (String) data.getOrDefault("imageUrl", "");
            int durationSeconds = ((Number) data.getOrDefault("durationSeconds", 86400)).intValue();
            String category = (String) data.getOrDefault("category", "");

            // Lấy sellerId từ username
            String username = (String) data.get("sellerId");
            User seller = userDAO.findByUsername(username);

            if (seller == null) {
                result.put("success", false);
                result.put("message", "Không tìm thấy người bán!");
                return result;
            }

            // Tạo Product object
            Product product = new Product();
            product.setName(productName);
            product.setDescription(description);
            product.setStartingPrice(startingPrice);
            product.setCurrentPrice(startingPrice);
            product.setSellerId(seller.getId());
            product.setImageUrl(imageUrl);
            product.setCategory(category);

            // Tính thời gian kết thúc từ số giây
            LocalDateTime endTime = LocalDateTime.now().plusSeconds(durationSeconds);
            product.setEndTime(endTime);
            product.setStatus("ACTIVE");
            product.setDurationHours(durationSeconds / 3600);

            // Lưu vào database
            boolean success = productDAO.createProduct(product);

            if (success) {
                // Tạo session đấu giá
                AuctionSession newSession = new AuctionSession(
                        product.getId(),
                        productName,
                        startingPrice,
                        endTime
                );
                newSession.setStatus("ACTIVE");
                sessions.put(product.getId(), newSession);

                // Cũng lưu vào productsDB
                Map<String, Object> productMap = new HashMap<>(data);
                productMap.put("productId", product.getId());
                productMap.put("sellerId", username);
                productsDB.put(product.getId(), productMap);

                result.put("success", true);
                result.put("message", "Đăng bán sản phẩm thành công!");
                result.put("productId", product.getId());
            } else {
                result.put("success", false);
                result.put("message", "Lỗi database, không thể đăng bán!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Lỗi: " + e.getMessage());
        }

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

        Object productIdObj = data.get("productId");
        if (productIdObj == null) {
            result.put("success", false);
            result.put("message", "Thiếu ID sản phẩm!");
            return result;
        }

        int productId = ((Number) productIdObj).intValue();
        String username = (String) data.get("sellerId");

        System.out.println("=== CẬP NHẬT SẢN PHẨM ID: " + productId + " ===");

        User seller = userDAO.findByUsername(username);
        if (seller == null) {
            result.put("success", false);
            result.put("message", "Không tìm thấy người dùng!");
            return result;
        }

        Product product = productDAO.findById(productId);
        if (product == null) {
            result.put("success", false);
            result.put("message", "Sản phẩm không tồn tại!");
            return result;
        }

        if (product.getSellerId() != seller.getId()) {
            result.put("success", false);
            result.put("message", "Bạn không có quyền sửa sản phẩm này!");
            return result;
        }

        // Cập nhật từng trường
        if (data.containsKey("name")) product.setName((String) data.get("name"));
        if (data.containsKey("description")) product.setDescription((String) data.get("description"));
        if (data.containsKey("imageUrl")) product.setImageUrl((String) data.get("imageUrl"));
        if (data.containsKey("category")) product.setCategory((String) data.get("category"));

        if (data.containsKey("startingPrice")) {
            double newPrice = ((Number) data.get("startingPrice")).doubleValue();
            product.setStartingPrice(newPrice);
            product.setCurrentPrice(newPrice);
            System.out.println("✅ Cập nhật giá: " + newPrice);
        }

        if (data.containsKey("durationHours")) {
            int newDuration = ((Number) data.get("durationHours")).intValue();
            product.setDurationHours(newDuration);
            LocalDateTime newEndTime = LocalDateTime.now().plusHours(newDuration);
            product.setEndTime(newEndTime);
            System.out.println("✅ Cập nhật thời gian: " + newDuration + " giờ, kết thúc lúc: " + newEndTime);
        }

        boolean success = productDAO.updateProduct(product);

        if (success) {
            // Cập nhật RAM
            if (productsDB.containsKey(productId)) {
                productsDB.get(productId).putAll(data);
            }
            if (sessions.containsKey(productId)) {
                AuctionSession session = sessions.get(productId);
                if (data.containsKey("startingPrice")) {
                    session.setCurrentPrice(((Number) data.get("startingPrice")).doubleValue());
                }
                if (data.containsKey("durationHours")) {
                    session.setEndTime(LocalDateTime.now().plusHours(((Number) data.get("durationHours")).intValue()));
                }
                if (data.containsKey("name")) {
                    session.setProductName((String) data.get("name"));
                }
            }

            result.put("success", true);
            result.put("message", "Cập nhật thành công!");
            System.out.println("✅ Cập nhật thành công!");
        } else {
            result.put("success", false);
            result.put("message", "Cập nhật thất bại!");
            System.err.println("❌ Cập nhật thất bại!");
        }

        return result;
    }

    public Map<String, Object> deleteProduct(int productId, String username) {
        Map<String, Object> result = new HashMap<>();

        // 1. Tìm seller từ username
        User seller = userDAO.findByUsername(username);
        if (seller == null) {
            result.put("success", false);
            result.put("message", "Không tìm thấy người dùng!");
            return result;
        }

        // 2. Tìm sản phẩm trong DATABASE (không phải RAM)
        Product product = productDAO.findById(productId);
        if (product == null) {
            result.put("success", false);
            result.put("message", "Sản phẩm không tồn tại!");
            return result;
        }

        // 3. Kiểm tra quyền (chỉ seller mới được xóa)
        if (product.getSellerId() != seller.getId()) {
            result.put("success", false);
            result.put("message", "Bạn không có quyền xóa sản phẩm này!");
            return result;
        }

        // 4. Xóa khỏi database
        boolean success = productDAO.deleteProduct(productId, seller.getId());

        if (success) {
            // 5. Xóa khỏi RAM (nếu có)
            productsDB.remove(productId);
            sessions.remove(productId);

            result.put("success", true);
            result.put("message", "Đã xóa sản phẩm thành công!");
        } else {
            result.put("success", false);
            result.put("message", "Không thể xóa sản phẩm!");
        }

        return result;
    }

    public Map<String, Object> getSellerProducts(String username) {
        Map<String, Object> result = new HashMap<>();

        User seller = userDAO.findByUsername(username);
        if (seller != null) {
            // productDAO.getProductsBySeller() phải trả về List<Product>
            List<Product> products = productDAO.getProductsBySeller(seller.getId());
            result.put("products", products);
        } else {
            result.put("products", new ArrayList<Product>());
        }

        result.put("success", true);
        return result;
    }

    // main bidding logic
    public Map<String, Object> placeBid(int productId, String username, double bidAmount) {
        Map<String, Object> result = new HashMap<>();
        try {
            System.out.println("🔍 [DEBUG] placeBid bắt đầu: productId=" + productId + ", user=" + username + ", amount=" + bidAmount);

            AuctionSession session = sessions.get(productId);
            if (session == null) {
                System.err.println("❌ Session null cho productId: " + productId);
                result.put("success", false);
                result.put("message", "Phiên đấu giá không tồn tại!");
                return result;
            }

            // Kiểm tra thời gian
            if (!"ACTIVE".equals(session.getStatus()) || LocalDateTime.now().isAfter(session.getEndTime())) {
                session.setStatus("FINISHED");
                result.put("success", false);
                result.put("message", "Phiên đấu giá đã kết thúc!");
                return result;
            }

            // Kiểm tra giá phải cao hơn giá hiện tại
            if (bidAmount <= session.getCurrentPrice()) {
                result.put("success", false);
                result.put("message", "Giá đặt phải cao hơn giá hiện tại (" + session.getCurrentPrice() + ")!");
                return result;
            }

            // Lấy thông tin user
            User user = userDAO.findByUsername(username);
            if (user == null) {
                result.put("success", false);
                result.put("message", "Không tìm thấy người dùng!");
                return result;
            }

            // Kiểm tra số dư
            if (user.getBalance() < bidAmount) {
                result.put("success", false);
                result.put("message", "Số dư không đủ! Số dư hiện tại: " + user.getBalance() + " VNĐ");
                return result;
            }

            // Cập nhật session trong RAM
            session.setCurrentPrice(bidAmount);
            session.setCurrentWinnerName(username);

            // Lưu vào database
            productDAO.updateCurrentPrice(productId, bidAmount);

            // Lưu lịch sử đấu giá
            BidTransaction bid = new BidTransaction(productId, user.getId(), username, bidAmount, false);
            bidDAO.createBid(bid);

            // Kiểm tra và gia hạn nếu cần (anti-sniping)
            checkAndExtendAuctionIfNeeded(session);

            result.put("success", true);
            result.put("message", "Đặt giá thành công!");
            result.put("currentPrice", bidAmount);
            result.put("newEndTime", session.getEndTime().toString());

            System.out.println("✅ [DEBUG] placeBid thành công");

        } catch (Exception e) {
            System.err.println("❌❌❌ LỖI TRONG placeBid: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Lỗi server: " + e.getMessage());
        }
        return result;
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

        // Lấy user để kiểm tra số dư
        User user = userDAO.findByUsername(username);
        if (user == null) {
            result.put("success", false);
            result.put("message", "Không tìm thấy người dùng!");
            return result;
        }

        // ✅ KIỂM TRA SỐ DƯ
        if (user.getBalance() < maxBid) {
            result.put("success", false);
            result.put("message", "Số dư không đủ để đặt Auto Bid! Số dư hiện tại: " + user.getBalance() + " VNĐ, Max Bid: " + maxBid + " VNĐ");
            return result;
        }

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
        List<User> allUsers = userDAO.getAllUsers();

        // ✅ Lọc bỏ ADMIN trước khi gửi về client
        List<User> filteredUsers = new ArrayList<>();
        for (User u : allUsers) {
            if (!"ADMIN".equalsIgnoreCase(u.getRole())) {
                filteredUsers.add(u);
            }
        }

        System.out.println("📊 getAllUsers() - Số users trong database: " + filteredUsers.size());
        for (User u : filteredUsers) {
            System.out.println("   User: " + u.getUsername() + " | Role: " + u.getRole() + " | Status: " + u.getStatus());
        }

        result.put("success", true);
        result.put("users", filteredUsers);
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
        boolean success = userDAO.deleteUser(userId); // ✅ Gọi DAO thật
        result.put("success", success);
        result.put("message", success ? "Đã xóa người dùng!" : "Xóa thất bại! (Có thể là ADMIN hoặc không tồn tại)");
        return result;
    }

    // Tìm dòng ~370, sửa method này:
    public Map<String, Object> getAllProducts() {
        Map<String, Object> result = new HashMap<>();
        List<Product> products = productDAO.getAllProducts();

        // ✅ THÊM 3 DÒNG NÀY
        System.out.println("📦 getAllProducts() được gọi!");
        System.out.println("   Số products từ database: " + (products != null ? products.size() : 0));

        result.put("success", true);
        result.put("products", products);
        return result;
    }

    public Map<String, Object> adminDeleteProduct(int productId) {
        Map<String, Object> result = new HashMap<>();
        boolean success = productDAO.adminDeleteProduct(productId); // ✅ Gọi DAO thật
        if (success) {
            productsDB.remove(productId);
            sessions.remove(productId);
            result.put("success", true);
            result.put("message", "Đã xóa sản phẩm!");
        } else {
            result.put("success", false);
            result.put("message", "Xóa thất bại!");
        }
        return result;
    }

    // get list sp đang đấu giá
    public Map<String, Object> getActiveProducts() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Product> activeProducts = productDAO.getActiveProducts();
            result.put("success", true);
            result.put("products", activeProducts);
        } catch (Exception e) {
            System.err.println("❌ Lỗi getActiveProducts: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("products", new ArrayList<>());
            result.put("message", "Lỗi: " + e.getMessage());
        }
        return result;
    }

    public void endAuction(int productId) {
        AuctionSession session = sessions.get(productId);
        if (session != null && "ACTIVE".equals(session.getStatus())) {

            // Lấy thông tin sản phẩm từ database
            Product product = productDAO.findById(productId);
            if (product != null) {
                String winnerName = session.getCurrentWinnerName();
                double finalPrice = session.getCurrentPrice();

                // Tìm user thắng cuộc và người bán
                User winner = userDAO.findByUsername(winnerName);
                User seller = userDAO.findByUsername(product.getSellerName());

                if (winner != null && seller != null) {
                    if (winner.getBalance() >= finalPrice) {
                        // Trừ tiền người thắng
                        userDAO.updateBalance(winner.getId(), winner.getBalance() - finalPrice);
                        // Cộng tiền người bán
                        userDAO.updateBalance(seller.getId(), seller.getBalance() + finalPrice);

                        // Lưu sản phẩm vào kho người thắng
                        userProductDAO.addPurchasedProduct(winner.getId(), productId, product.getName(), finalPrice);

                        // Cập nhật winner_id cho sản phẩm
                        productDAO.updateWinner(productId, winner.getId());
                        // Đánh dấu sản phẩm đã bán
                        productDAO.updateStatus(productId, "SOLD");

                        // GỬI THÔNG BÁO ĐẾN NGƯỜI THẮNG VÀ NGƯỜI BÁN
                        notificationService.notifyAuctionEnd(productId, winner.getId(), winnerName, finalPrice);

                        System.out.println("✅ Chốt phiên: " + session.getProductName() +
                                " | Thắng: " + winnerName + " | Giá: " + finalPrice);
                    } else {
                        System.out.println("⚠️ " + winnerName + " không đủ tiền! Hủy giao dịch.");
                        productDAO.updateStatus(productId, "FAILED");
                    }
                }
            }

            session.setStatus("FINISHED");
            // Xóa khỏi danh sách đang đấu giá (sessions)
            // sessions.remove(productId); // Comment nếu muốn giữ lịch sử
        }
    }

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

    public List<AuctionSession> getActiveAuctions() {
        List<AuctionSession> activeAuctions = new ArrayList<>();
        for (AuctionSession session : sessions.values()) {
            if ("ACTIVE".equals(session.getStatus())) {
                activeAuctions.add(session);
            }
        }
        return activeAuctions;
    }

    public Map<String, Object> getBidHistory(int productId) {
        Map<String, Object> result = new HashMap<>();
        List<BidTransaction> history = bidDAO.getBidsByProduct(productId);
        result.put("success", true);
        result.put("history", history);
        return result;
    }

    private void checkAndExtendAuctionIfNeeded(AuctionSession auction) {
        LocalDateTime endTime = auction.getEndTime();
        LocalDateTime now = LocalDateTime.now();
        long secondsRemaining = ChronoUnit.SECONDS.between(now, endTime);
        System.out.println("🔍 [DEBUG] secondsRemaining = " + secondsRemaining);
        if (secondsRemaining <= 30 && secondsRemaining > 0) {
            LocalDateTime newEndTime = endTime.plusSeconds(60);
            auction.setEndTime(newEndTime);
            System.out.println("✅ Anti-sniping: gia hạn thêm 60s, endTime mới = " + newEndTime);
        }
    }

    public AuctionSession getSession(int productId) {
        return sessions.get(productId);
    }

    public Map<String, Object> banUser(int userId) {
        Map<String, Object> result = new HashMap<>();
        boolean success = userDAO.banUser(userId);
        result.put("success", success);
        result.put("message", success ? "Đã khóa người dùng!" : "Khóa thất bại!");
        return result;
    }

    public Map<String, Object> unbanUser(int userId) {
        Map<String, Object> result = new HashMap<>();
        boolean success = userDAO.unbanUser(userId);
        result.put("success", success);
        result.put("message", success ? "Đã mở khóa người dùng!" : "Mở khóa thất bại!");
        return result;
    }

    public Map<String, Object> getUserInfo(String username) {
        Map<String, Object> result = new HashMap<>();
        User user = userDAO.findByUsername(username);
        if (user != null) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("fullName", user.getFullName());
            userData.put("email", user.getEmail());
            userData.put("balance", user.getBalance());
            result.put("user", userData);
            result.put("success", true);
        } else {
            result.put("success", false);
            result.put("message", "Không tìm thấy người dùng!");
        }
        return result;
    }

    public Map<String, Object> updateUserInfo(Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        String username = (String) data.get("username");
        String fullName = (String) data.get("fullName");
        String email = (String) data.get("email");

        User user = userDAO.findByUsername(username);
        if (user == null) {
            result.put("success", false);
            result.put("message", "Không tìm thấy người dùng!");
            return result;
        }

        user.setFullName(fullName);
        user.setEmail(email);

        boolean success = userDAO.updateUser(user);
        result.put("success", success);
        result.put("message", success ? "Cập nhật thành công!" : "Cập nhật thất bại!");
        return result;
    }

    public Map<String, Object> rechargeBalance(String username, double amount) {
        Map<String, Object> result = new HashMap<>();
        User user = userDAO.findByUsername(username);
        if (user == null) {
            result.put("success", false);
            result.put("message", "Không tìm thấy người dùng!");
            return result;
        }

        double newBalance = user.getBalance() + amount;
        boolean success = userDAO.updateBalance(user.getId(), newBalance);

        result.put("success", success);
        result.put("message", success ? "Nạp " + amount + " VNĐ thành công!" : "Nạp tiền thất bại!");
        return result;
    }

    public void loadActiveSessions() {
        List<Product> activeProducts = productDAO.getActiveProducts();
        for (Product product : activeProducts) {
            AuctionSession session = new AuctionSession(
                    product.getId(),
                    product.getName(),
                    product.getCurrentPrice(),
                    product.getEndTime()
            );
            session.setStatus("ACTIVE");
            session.setCurrentWinnerName(product.getWinnerName());
            sessions.put(product.getId(), session);

            // Cũng load vào productsDB
            Map<String, Object> productMap = new HashMap<>();
            productMap.put("productId", product.getId());
            productMap.put("name", product.getName());
            productMap.put("currentPrice", product.getCurrentPrice());
            productsDB.put(product.getId(), productMap);
        }
        System.out.println("✅ Đã load " + activeProducts.size() + " sản phẩm đang đấu giá vào RAM");
    }

    public Map<String, Object> getPurchasedProducts(String username) {
        Map<String, Object> result = new HashMap<>();
        try {
            System.out.println("🔍 [DEBUG] getPurchasedProducts - username: " + username);
            User user = userDAO.findByUsername(username);
            if (user != null) {
                List<Product> products = userProductDAO.getPurchasedProducts(user.getId());
                result.put("products", products);
                result.put("success", true);
                System.out.println("✅ [DEBUG] Tìm thấy " + products.size() + " sản phẩm đã mua");
            } else {
                result.put("success", false);
                result.put("message", "Không tìm thấy người dùng!");
            }
        } catch (Exception e) {
            System.err.println("❌ [DEBUG] Lỗi trong getPurchasedProducts: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Lỗi: " + e.getMessage());
        }
        return result;
    }

    public Map<String, Object> extendAuctionTime(int productId) {
        Map<String, Object> result = new HashMap<>();
        AuctionSession session = sessions.get(productId);

        if (session != null && "ACTIVE".equals(session.getStatus())) {
            LocalDateTime newEndTime = session.getEndTime().plusSeconds(30);
            session.setEndTime(newEndTime);
            productDAO.updateEndTime(productId, newEndTime);

            result.put("success", true);
            result.put("message", "Đã gia hạn thêm 30 giây");
            result.put("newEndTime", newEndTime.toString());
        } else {
            result.put("success", false);
            result.put("message", "Không thể gia hạn");
        }
        return result;
    }

    // ==================== METHODS FOR TESTING ====================

    public Map<String, Object> addFunds(int userId, double amount) {
        Map<String, Object> result = new HashMap<>();
        if (amount <= 0) {
            result.put("success", false);
            result.put("message", "Số tiền phải lớn hơn 0");
            return result;
        }
        User user = userDAO.findByUsername(String.valueOf(userId));
        if (user == null) {
            result.put("success", false);
            result.put("message", "Không tìm thấy người dùng");
            return result;
        }
        boolean success = userDAO.updateBalance(userId, user.getBalance() + amount);
        result.put("success", success);
        result.put("message", success ? "Nạp tiền thành công" : "Nạp tiền thất bại");
        return result;
    }

    public Map<String, Object> processPayment(int userId, int productId) {
        Map<String, Object> result = new HashMap<>();
        AuctionSession session = sessions.get(productId);
        if (session == null) {
            result.put("success", false);
            result.put("message", "Phiên đấu giá không tồn tại");
            return result;
        }
        if (!"FINISHED".equals(session.getStatus())) {
            result.put("success", false);
            result.put("message", "Phiên đấu giá chưa kết thúc");
            return result;
        }
        if (session.getCurrentWinnerId() != userId) {
            result.put("success", false);
            result.put("message", "Bạn không phải người thắng cuộc");
            return result;
        }
        if ("PAID".equals(session.getStatus())) {
            result.put("success", false);
            result.put("message", "Sản phẩm đã được thanh toán");
            return result;
        }
        session.setStatus("PAID");
        result.put("success", true);
        result.put("message", "Thanh toán thành công");
        return result;
    }
}
