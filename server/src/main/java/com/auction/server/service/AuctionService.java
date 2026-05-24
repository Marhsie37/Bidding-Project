package com.auction.server.service;

import com.auction.server.dao.BidDAO;
import com.auction.server.dao.ProductDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.dao.UserProductDAO;
import com.auction.shared.model.AuctionSession;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Product;
import com.auction.shared.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionService {

    private static volatile AuctionService instance;
    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);
    private static final int ANTI_SNIPING_WINDOW_SECONDS = 30;
    private static final int ANTI_SNIPING_EXTENSION_SECONDS = 60;

    private NotificationService notificationService = NotificationService.getInstance();


    /*private Map<String, Map<String, Object>> usersDB = new ConcurrentHashMap<>();
    private AtomicInteger userIdGenerator = new AtomicInteger(1);

    private Map<Integer, Map<String, Object>> productsDB = new ConcurrentHashMap<>();
    private AtomicInteger productIdGenerator = new AtomicInteger(1);


    2 dòng code này chỉ lấy dữ liệu từ RAM sẽ bị mất khi tắt kết nối nên phải đổi code khác để lấy dữ liệu từ database

    Phải thay bằng tạo userDAO với productDAO để lấy thông tin trên database
    */
    private UserDAO userDAO;
    private ProductDAO productDAO;
    private BidDAO bidDAO = new BidDAO();
    private UserProductDAO userProductDAO = new UserProductDAO();


    private Map<Integer, AuctionSession> sessions = new ConcurrentHashMap<>();

    private AuctionService() {
        /*Map<String, Object> adminData = new HashMap<>();
        adminData.put("id", 0);
        adminData.put("password", "admin123");
        adminData.put("role", "ADMIN");
        adminData.put("balance", 0.0);
        usersDB.put("admin", adminData);

        Hàm này chỉ dùng với RAM tạm thời thôi còn tài khoản đã tạo ở database rồi
         */

        this.userDAO = new UserDAO();
        this.productDAO = new ProductDAO();
        loadActiveSessions(); // ✅ THÊM DÒNG NÀY
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
        User user = userDAO.findByUsername(username);//Code giúp lấy dữ liệu từ database ko phải dùng cái tạm thời
        if (user != null && user.getPassword().equals(password)) {   //Lúc đầu dùng containsKey được vì nó là map nhưng giờ là object nên không được dùng

            if ("BANNED".equalsIgnoreCase(user.getStatus())) {
                result.put("success", false);
                result.put("message", "Tài khoản đã bị khóa!");
                return result;
            }

            Map<String, Object> userInfo = new HashMap<>();

            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("role", user.getRole());
            userInfo.put("balance", user.getBalance());

            result.put("success", true);
            result.put("message", "Đăng nhập thành công!");
            result.put("role", userInfo.get("role"));
            result.put("user", userInfo);
            return result;

        }
        result.put("success", false);
        result.put("message", "Sai tài khoản hoặc mật khẩu!");
        return result;
    }

    public Map<String, Object> register(Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        String username = (String) data.get("username");
        String password = (String) data.get("password");

        String email = (String) data.get("email");
        String fullName = (String) data.get("fullName");
        String role = (String) data.getOrDefault("role", "BIDDER");// 3 dòng này thêm cho đầy đủ thông tin thay vì chỉ có user name và pass


        User existingUser = userDAO.findByUsername(username);
        if (existingUser != null) {
            result.put("success", false);
            result.put("message", "Tên đăng nhập đã tồn tại!");
            return result;
        }

        User existingEmail = userDAO.findByEmail(email);
        if (existingEmail != null) {
            result.put("success", false);
            result.put("message", "Email đã được đăng ký!");
            return result;
        }  //Cái này nên có vì có dùng gmail để tạo tài khoản nếu không thì có thể bỏ và bỏ tạo thêm gmail ở giao diện

        /*
        Map<String, Object> newUser = new HashMap<>();
        newUser.put("id", userIdGenerator.getAndIncrement());
        newUser.put("password", password);
        newUser.put("role", "USER");
        newUser.put("balance", 0.0);

        usersDB.put(username, newUser);

        Nên bỏ luôn đoaạn code này vì nó đang sử dụng RAM tạm thời mà không sử dụng database
         */

        boolean success = userDAO.createUser(username, password, email, fullName, role);
//          ↑                 ↑
//      kết quả            method này mới thực sự tạo user trong database
        if (success) {
            result.put("success", true);
            result.put("message", "Đăng ký thành công! Bạn có thể đăng nhập ngay.");
        } else {
            result.put("success", false);
            result.put("message", "Đăng ký thất bại! Lỗi hệ thống.");
        }
        return result;
    }

    public Map<String, Object> addProduct(Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        //int newProductId = productIdGenerator.getAndIncrement(); Nên bỏ cái này vì data base có cơ chế AUTO_INCREMENT
        String sellerId = (String) data.get("sellerId");
        //String productName = (String) data.get("productName");
        double startingPrice = ((Number) data.get("startingPrice")).doubleValue();
        //int durationMinutes = ((Number) data.getOrDefault("duration", 60)).intValue();
        String productName = (String) data.get("name"); // ✅
        int durationHours = ((Number) data.getOrDefault("durationHours", 24)).intValue(); // ✅

        String description = (String) data.getOrDefault("description", ""); //Thêm cái này để mô ta chi tiết sản phẩm
        String imageUrl = (String) data.getOrDefault("imageUrl", "");//Thêm địa chỉ sản
        try {
            // Tìm người bán theo username
            User seller = userDAO.findByUsername(sellerId);
            if (seller == null) {
                result.put("success", false);
                result.put("message", "Không tìm thấy người bán!");
                return result;
            }

            // Tạo Product object
            LocalDateTime endTime = LocalDateTime.now().plusSeconds(durationHours); //đổi plusHours về plusSeconds sẽ đổi từ giờ thành giây
            Product product = new Product();
            product.setName(productName);
            product.setDescription(description);
            product.setStartingPrice(startingPrice);
            product.setCurrentPrice(startingPrice);
            product.setSellerId(seller.getId());
            product.setImageUrl(imageUrl);
            product.setStatus("ACTIVE");
            product.setEndTime(endTime);
            product.setDurationHours(durationHours); // ✅

            // Lưu vào database
            boolean success = productDAO.createProduct(product);

            if (success) {
                // Tạo session cho đấu giá realtime (giống code cũ)
                AuctionSession newSession = new AuctionSession(product.getId(), productName, startingPrice, endTime);
                newSession.setStatus("ACTIVE");
                sessions.put(product.getId(), newSession);

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

    public Map<String, Object> getActiveProducts() {
        Map<String, Object> result = new HashMap<>();
        List<Product> activeSessions = new ArrayList<>();

        for (AuctionSession session : sessions.values()) {
            if ("ACTIVE".equals(session.getStatus())) {
                //activeSessions.add(session);
                Product p = productDAO.findById(session.getProductId());
                if (p != null) activeSessions.add(p);
            }
        }

        result.put("success", true);
        result.put("products", activeSessions);
        return result;
    }

    public Map<String, Object> getProductDetails(int productId) {
        Map<String, Object> result = new HashMap<>();
        Product product = productDAO.findById(productId);
        if (product != null) {
            result.put("success", true);
            result.put("product", product);
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

        try {
            User seller = userDAO.findByUsername(sellerId);
            Product product = productDAO.findById(productId);
            if (product == null) {
                result.put("success", false);
                result.put("message", "Sản phẩm không tồn tại!");
                return result;
            }

            if (product.getSellerId() != seller.getId()) {
                result.put("success", false);
                result.put("message", "Bạn không có quyền sửa sản phẩm của người khác!");
                return result;
            }

            AuctionSession session = sessions.get(productId);
            boolean hasBids = false;
            if (session != null) {
                // Kiểm tra xem đã có giá đấu nào chưa (giá hiện tại > giá khởi điểm)
                hasBids = session.getCurrentPrice() > product.getStartingPrice();
            }

            // 5. Nếu đã có giá đấu, không cho sửa startingPrice và duration
            if (hasBids) {
                if (data.containsKey("startingPrice") || data.containsKey("durationHours")) {
                    result.put("success", false);
                    result.put("message", "Không thể sửa giá hoặc thời gian vì đã có người đặt giá!");
                    return result;
                }
            }

            // 6. Cập nhật các field
            if (data.containsKey("name")) {
                product.setName((String) data.get("name"));
                if (session != null) session.setProductName((String) data.get("name"));
            }
            if (data.containsKey("description")) {
                product.setDescription((String) data.get("description"));
            }
            if (data.containsKey("imageUrl")) {
                product.setImageUrl((String) data.get("imageUrl"));
            }

            // 7. Chỉ cho sửa startingPrice và duration nếu chưa có giá đấu
            if (!hasBids) {
                if (data.containsKey("startingPrice")) {
                    double newPrice = ((Number) data.get("startingPrice")).doubleValue();
                    product.setStartingPrice(newPrice);
                    product.setCurrentPrice(newPrice);
                    if (session != null) session.setCurrentPrice(newPrice);
                }
                if (data.containsKey("durationHours")) {
                    int durationHours = ((Number) data.get("durationHours")).intValue();
                    LocalDateTime newEndTime = LocalDateTime.now().plusHours(durationHours);
                    product.setEndTime(newEndTime);
                    product.setDurationHours(durationHours);
                    if (session != null) session.setEndTime(newEndTime);
                }
            }

            // 8. Lưu vào database
            boolean success = productDAO.updateProduct(product);

            if (success) {
                result.put("success", true);
                result.put("message", "Cập nhật thành công!");
            } else {
                result.put("success", false);
                result.put("message", "Cập nhật thất bại!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Lỗi: " + e.getMessage());
        }
        return result;
    }

    public Map<String, Object> deleteProduct(int productId, String username) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 1. Tìm người dùng theo username
            User seller = userDAO.findByUsername(username);
            if (seller == null) {
                result.put("success", false);
                result.put("message", "Không tìm thấy người dùng!");
                return result;
            }

            // 2. Tìm sản phẩm trong database
            Product product = productDAO.findById(productId);
            if (product == null) {
                result.put("success", false);
                result.put("message", "Sản phẩm không tồn tại!");
                return result;
            }

            // 3. Kiểm tra quyền (chỉ chủ sở hữu mới được xóa)
            if (product.getSellerId() != seller.getId()) {
                result.put("success", false);
                result.put("message", "Bạn không có quyền xóa sản phẩm này!");
                return result;
            }

            // 4. Xóa khỏi database
            boolean success = productDAO.deleteProduct(productId, seller.getId());

            if (success) {
                // 5. Xóa khỏi RAM (sessions) nếu có
                sessions.remove(productId);

                result.put("success", true);
                result.put("message", "Đã xóa sản phẩm thành công!");
            } else {
                result.put("success", false);
                result.put("message", "Xóa sản phẩm thất bại!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Lỗi: " + e.getMessage());
        }
        return result;
    }

    public Map<String, Object> getSellerProducts(String username) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 1. Tìm người dùng theo username
            User seller = userDAO.findByUsername(username);
            if (seller == null) {
                result.put("success", false);
                result.put("message", "Không tìm thấy người dùng!");
                result.put("products", new ArrayList<>()); //Không tìm thấy thì sẽ trả về danh sách rỗng thay vì null để tránh lỗi
                return result;
            }

            // 2. Lấy danh sách sản phẩm của người bán từ database
            List<Product> products = productDAO.getProductsBySeller(seller.getId());

            result.put("success", true);
            result.put("products", products);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Lỗi: " + e.getMessage());
            result.put("products", new ArrayList<>());
        }
        return result;
    }

    public Map<String, Object> placeBid(int productId, String username, double bidAmount) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 1. Lấy phiên đấu giá từ RAM (vẫn giữ để realtime)
            AuctionSession session = sessions.get(productId);
            if (session == null) {
                result.put("success", false);
                result.put("message", "Phiên đấu giá không tồn tại!");
                return result;
            }

            // 9. Kiểm tra và gia hạn nếu cần (anti-sniping)
            checkAndExtendAuctionIfNeeded(session);

            // 2. Kiểm tra phiên còn hoạt động không
            if (!"ACTIVE".equals(session.getStatus()) || LocalDateTime.now().isAfter(session.getEndTime())) {
                session.setStatus("FINISHED");
                result.put("success", false);
                result.put("message", "Phiên đấu giá đã kết thúc!");
                return result;
            }

            // 3. Kiểm tra giá đặt phải cao hơn giá hiện tại ít nhất 5000
            if (bidAmount < session.getCurrentPrice() + 5000) {
                result.put("success", false);
                result.put("message", "Giá đặt phải lớn hơn hoặc bằng giá hiện tại cộng thêm 5,000 VNĐ (tối thiểu " + (session.getCurrentPrice() + 5000) + " VNĐ)!");
                return result;
            }

            // 4. Lấy thông tin user từ database (thay vì từ usersDB)
            User user = userDAO.findByUsername(username);
            if (user == null) {
                result.put("success", false);
                result.put("message", "Không tìm thấy người dùng!");
                return result;
            }

            // 5. Kiểm tra số dư (lấy từ database)
            if (user.getBalance() < bidAmount) {
                result.put("success", false);
                result.put("message", "Số dư không đủ! Số dư hiện tại: " + user.getBalance() + " VNĐ");
                return result;
            }

            // 6. Cập nhật session trong RAM
            session.setCurrentPrice(bidAmount);
            session.setCurrentWinnerId(user.getId());
            session.setCurrentWinnerName(username);

            // 7. Lưu vào database (cập nhật giá hiện tại của sản phẩm)
            productDAO.updateCurrentPrice(productId, bidAmount);

            // 8. Lưu lịch sử đấu giá vào database
            BidTransaction bid = new BidTransaction(productId, user.getId(), username, bidAmount, false);
            bidDAO.createBid(bid);


            // 10. Gửi thông báo realtime
            if (notificationService != null) {
                notificationService.notifyBidUpdate(productId, username, bidAmount);
            }

            // Kích hoạt xử lý Auto-bid ngay lập tức khi có người đặt giá mới
            AutoBidService.getInstance().processAllAutoBids();

            result.put("success", true);
            result.put("message", "Đặt giá thành công! Bạn đang dẫn đầu với giá " + bidAmount);
            result.put("currentPrice", bidAmount);
            result.put("newEndTime", session.getEndTime().withNano(0).toString());
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Lỗi server: " + e.getMessage());
        }
        return result;
    }

    private void checkAndExtendAuctionIfNeeded(AuctionSession auction) {
        LocalDateTime endTime = auction.getEndTime();
        LocalDateTime now = LocalDateTime.now();

        long secondsRemaining = ChronoUnit.SECONDS.between(now, endTime);

        if (secondsRemaining <= ANTI_SNIPING_WINDOW_SECONDS && secondsRemaining > 0) {
            LocalDateTime newEndTime = endTime.plusSeconds(ANTI_SNIPING_EXTENSION_SECONDS);
            auction.setEndTime(newEndTime);

            productDAO.updateEndTime(auction.getProductId(), newEndTime); //Đồng bộ thời gian cho database


            logger.info("Anti-sniping: gia hạn thêm " + ANTI_SNIPING_EXTENSION_SECONDS + "s cho sp " + auction.getProductId());

            if (notificationService != null) {
                notificationService.notifyAuctionExtended(auction.getProductId(), newEndTime);
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

    public Map<String, Object> setAutoBid(int productId, String username, double maxBid, double increment) {
        Map<String, Object> result = new HashMap<>();
        if (increment < 5000) {
            result.put("success", false);
            result.put("message", "Bước giá tự động không được dưới 5,000 VNĐ!");
            return result;
        }
        AuctionSession session = sessions.get(productId);
        if (session != null) {
            session.addAutoBid(username, maxBid);
            AutoBidService.getInstance().registerAutoBid(productId, username, maxBid, increment);

            // Kích hoạt xử lý Auto-bid ngay lập tức
            AutoBidService.getInstance().processAllAutoBids();

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
            AutoBidService.getInstance().unregisterAutoBid(productId, username);
            result.put("success", true);
            result.put("message", "Đã tắt Auto-bid.");
        }
        return result;
    }

    public Map<String, Object> getAllUsers() {
        Map<String, Object> result = new HashMap<>();
        List<User> users = userDAO.getAllUsers(); //Cần gọi cái này để lấy dữ liệu từ data base
        List<User> filtered = new ArrayList<>();
        for (User u : users) {
            if (!"ADMIN".equalsIgnoreCase(u.getRole())) {
                filtered.add(u);
            }
        }
        result.put("success", true);
        result.put("users", filtered);
        return result;
    }

    public Map<String, Object> getAllProducts() {
        Map<String, Object> result = new HashMap<>();
        List<Product> products = productDAO.getAllProducts();
        result.put("success", true);
        result.put("products", products);
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
        boolean success = userDAO.deleteUser(userId);
        result.put("success", success);
        if (success) {
            result.put("message", "Admin: Xóa User thành công");
        } else {
            result.put("message", "Admin: Xóa User thất bại");
        }

        return result;
    }

    public Map<String, Object> adminDeleteProduct(int productId) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 1. Kiểm tra sản phẩm có tồn tại trong database không
            Product product = productDAO.findById(productId);
            if (product == null) {
                result.put("success", false);
                result.put("message", "Sản phẩm không tồn tại!");
                return result;
            }

            // 2. Xóa khỏi database
            boolean success = productDAO.adminDeleteProduct(productId);

            if (success) {
                // 3. Xóa khỏi RAM (sessions) nếu có
                sessions.remove(productId);

                result.put("success", true);
                result.put("message", "Admin đã xóa sản phẩm thành công!");
            } else {
                result.put("success", false);
                result.put("message", "Xóa sản phẩm thất bại!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Lỗi: " + e.getMessage());
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

            // Dọn dẹp cấu hình Auto-bid của sản phẩm này
            AutoBidService.getInstance().removeProductAutoBids(productId);

            int winnerId = session.getCurrentWinnerId();
            double finalPrice = session.getCurrentPrice();
            productDAO.updateStatus(productId, "ENDED");

            if (winnerId > 0) {
                productDAO.updateWinner(productId, winnerId);

                // Lưu vào user_products (sản phẩm đã mua)
                Product product = productDAO.findById(productId);
                if (product != null) {
                    userProductDAO.addPurchasedProduct(winnerId, productId,
                            product.getName(), finalPrice);

                    // ✅ Trừ tiền người thắng
                    User winner = userDAO.findById(winnerId);
                    if (winner != null && winner.getBalance() >= finalPrice) {
                        userDAO.updateBalance(winnerId, winner.getBalance() - finalPrice);
                        logger.info("Đã trừ {} VNĐ của người thắng: {}", finalPrice, winner.getUsername());
                    } else if (winner != null) {
                        logger.warn("Người thắng {} không đủ số dư ({} < {})",
                                winner.getUsername(), winner.getBalance(), finalPrice);
                    }

                    // ✅ Cộng tiền người bán
                    User seller = userDAO.findById(product.getSellerId());
                    if (seller != null) {
                        userDAO.updateBalance(seller.getId(), seller.getBalance() + finalPrice);
                        logger.info("Đã cộng {} VNĐ cho người bán: {}", finalPrice, seller.getUsername());
                    }

                    // Cập nhật trạng thái sản phẩm thành SOLD
                    productDAO.updateStatus(productId, "SOLD");
                }
            }

            logger.info("Đã chốt phiên [" + session.getProductName() + "]. Winner: " + session.getCurrentWinnerName());

            if (notificationService != null) {
                notificationService.notifyAuctionEnd(productId, session.getCurrentWinnerId(), session.getCurrentWinnerName(), session.getCurrentPrice());
            }
        }
    }


    public synchronized Map<String, Object> addFunds(int userId, double amount) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (amount < 5000) {
                result.put("success", false);
                result.put("message", "Số tiền nạp không được dưới 5,000 VNĐ!");
                return result;
            }

            // Tìm user trong database
            User user = userDAO.findById(userId);
            if (user == null) {
                result.put("success", false);
                result.put("message", "Không tìm thấy người dùng!");
                return result;
            }

            // Cập nhật số dư trong database
            double newBalance = user.getBalance() + amount;
            boolean success = userDAO.updateBalance(userId, newBalance);

            if (success) {
                result.put("success", true);
                result.put("message", "Nạp tiền thành công!");
                result.put("balance", newBalance);
            } else {
                result.put("success", false);
                result.put("message", "Nạp tiền thất bại!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Lỗi: " + e.getMessage());
        }
        return result;
    }

    public synchronized Map<String, Object> processPayment(int userId, int auctionId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Lấy phiên đấu giá từ RAM
            AuctionSession auction = sessions.get(auctionId);
            if (auction == null || !"FINISHED".equals(auction.getStatus())) {
                result.put("success", false);
                result.put("message", "Phiên đấu giá chưa kết thúc hoặc không tồn tại");
                return result;
            }

            // 2. Kiểm tra người dùng có phải người thắng không
            if (auction.getCurrentWinnerId() != userId) {
                result.put("success", false);
                result.put("message", "Bạn không phải người thắng cuộc");
                return result;
            }

            // 3. Kiểm tra đã thanh toán chưa
            if ("PAID".equals(auction.getStatus())) {
                result.put("success", false);
                result.put("message", "Phiên đấu giá đã được thanh toán");
                return result;
            }

            // 4. Tìm user trong database
            User user = userDAO.findById(userId);
            if (user == null) {
                result.put("success", false);
                result.put("message", "Không tìm thấy người dùng!");
                return result;
            }

            double amount = auction.getCurrentPrice();

            // 5. Kiểm tra số dư
            if (user.getBalance() < amount) {
                result.put("success", false);
                result.put("message", "Số dư không đủ. Cần: " + amount + ", Hiện có: " + user.getBalance());
                return result;
            }

            // 6. Trừ tiền người thắng
            double newBalance = user.getBalance() - amount;
            boolean updateBalanceSuccess = userDAO.updateBalance(userId, newBalance);

            if (!updateBalanceSuccess) {
                result.put("success", false);
                result.put("message", "Thanh toán thất bại! Lỗi cập nhật số dư.");
                return result;
            }

            // 7. Cộng tiền cho người bán
            User seller = userDAO.findById(auction.getCurrentWinnerId() == userId ? userId : 0);
            // Lấy seller từ sản phẩm
            Product product = productDAO.findById(auction.getProductId());
            if (product != null) {
                User sellerUser = userDAO.findById(product.getSellerId());
                if (sellerUser != null) {
                    userDAO.updateBalance(sellerUser.getId(), sellerUser.getBalance() + amount);
                }
            }

            // 8. Cập nhật trạng thái phiên
            auction.setStatus("PAID");

            // 9. Cập nhật trạng thái sản phẩm trong database
            productDAO.updateStatus(auction.getProductId(), "SOLD");

            // 10. Lưu vào danh sách sản phẩm đã mua
            // userProductDAO.addPurchasedProduct(userId, auction.getProductId(),
            //     auction.getProductName(), amount);

            result.put("success", true);
            result.put("message", "Thanh toán thành công!");
            result.put("balance", newBalance);
            result.put("amount", amount);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Lỗi: " + e.getMessage());
        }
        return result;
    }

    public Map<String, Object> getUserBalance(int userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Tìm user trong database
            User user = userDAO.findById(userId);

            if (user != null) {
                result.put("success", true);
                result.put("balance", user.getBalance());
                result.put("message", "Số dư: " + user.getBalance());
            } else {
                result.put("success", false);
                result.put("message", "Không tìm thấy người dùng!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Lỗi: " + e.getMessage());
        }
        return result;
    }


    // Lấy User theo username (dùng nội bộ bởi ClientHandler)
    public User getUserByUsername(String username) {
        return userDAO.findByUsername(username);
    }

    // Ban user (khóa tài khoản)
    public Map<String, Object> banUser(int userId) {
        Map<String, Object> result = new HashMap<>();
        boolean success = userDAO.banUser(userId);
        result.put("success", success);
        result.put("message", success ? "Đã khóa người dùng!" : "Khóa thất bại!");
        return result;
    }

    // Unban user (mở khóa tài khoản)
    public Map<String, Object> unbanUser(int userId) {
        Map<String, Object> result = new HashMap<>();
        boolean success = userDAO.unbanUser(userId);
        result.put("success", success);
        result.put("message", success ? "Đã mở khóa người dùng!" : "Mở khóa thất bại!");
        return result;
    }

    // Lấy thông tin user (cho Profile)
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
            userData.put("role", user.getRole());
            result.put("user", userData);
            result.put("success", true);
        } else {
            result.put("success", false);
            result.put("message", "Không tìm thấy người dùng!");
        }
        return result;
    }

    // Lấy danh sách sản phẩm đã mua
    public Map<String, Object> getPurchasedProducts(String username) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userDAO.findByUsername(username);
            if (user != null) {
                List<Product> products = userProductDAO.getPurchasedProducts(user.getId());
                result.put("products", products);
                result.put("success", true);
            } else {
                result.put("success", false);
                result.put("message", "Không tìm thấy người dùng!");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Lỗi: " + e.getMessage());
        }
        return result;
    }


    private void loadActiveSessions() {
        List<Product> activeProducts = productDAO.getActiveProducts();
        for (Product product : activeProducts) {
            AuctionSession session = new AuctionSession(
                    product.getId(),
                    product.getName(),
                    product.getCurrentPrice(),
                    product.getEndTime()
            );
            session.setStatus("ACTIVE");
            sessions.put(product.getId(), session);
        }
        logger.info("✅ Đã load " + activeProducts.size() + " sản phẩm vào RAM");
    }


    public void checkAndEndAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Integer> endedProductIds = new ArrayList<>();

        for (Map.Entry<Integer, AuctionSession> entry : sessions.entrySet()) {
            AuctionSession session = entry.getValue();
            if ("ACTIVE".equals(session.getStatus()) && now.isAfter(session.getEndTime())) {
                endedProductIds.add(session.getProductId());
            }
        }

        // 🟢 GỌI endAuction() CHO TỪNG SẢN PHẨM ĐÃ KẾT THÚC
        for (int productId : endedProductIds) {
            endAuction(productId);  // ← THÊM DÒNG NÀY
            logger.info("✅ Đã kết thúc phiên đấu giá cho sản phẩm ID: {}", productId);
        }
    }
}