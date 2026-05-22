package com.auction.server;

import com.auction.server.dao.BidDAO;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.protocol.*;
import com.auction.server.service.AuctionService;
import com.auction.server.service.NotificationService;


import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHandler implements Runnable {
    private  Socket socket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private String username;
    private String role;
    private boolean connected;
    private AuctionService auctionService;
    private NotificationService notificationService;
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private static final java.util.concurrent.ConcurrentHashMap<Integer, Boolean> onlineUsers = new java.util.concurrent.ConcurrentHashMap<>();

    // Thêm biến instance để lưu userId hiện tại
    private int currentUserId = -1;
    private String currentRequestId; // thêm cùng chỗ với các field khác
    private BidDAO bidDAO = new BidDAO();

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.connected = true;
        this.auctionService = AuctionService.getInstance();
        this.notificationService = NotificationService.getInstance();
        if (socket == null) return;


        try {
            socket.setSoTimeout(0);
            this.outputStream = new ObjectOutputStream(socket.getOutputStream());
            this.inputStream = new ObjectInputStream((socket.getInputStream()));
        } catch (IOException e) {
            logger.error("Error creating streams: ",e);
            connected = false; //Nên thêm cái này lỡ nếu tạo stream thất bại, connected vẫn là true, run() vẫn chạy, nhưng inputStream/outputStream là null → sẽ bị NullPointerException khi đọc/ghi.
        }

    }

    public void run() {
        try {
            while (connected) {
                Object obj = inputStream.readObject();
                if (obj instanceof Request) {
                    handleRequest((Request) obj);
                }
            }
        } catch (SocketException e) {
            System.err.println(" SOCKET EXCEPTION: " + e.getMessage());
            e.printStackTrace();
        } catch (EOFException e) {
            System.out.println("Client disconnected: " + username);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error handling client: " + e.getMessage());
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }
    public void handleRequest(Request request){
        this.currentRequestId = request.getRequestId();
        CommandType command = request.getCommand();
        Map<String,Object> data = request.getData();
        switch (command){
            case LOGIN :
                handleLogin(data);
                break;
            case REGISTER:
                handleRegister(data);
                break;
            case GET_PRODUCTS:
                handleGetProducts(data);
                break;
            case GET_PRODUCT_DETAILS:
                handleGetProductDetails(data);
                break;
            case ADD_PRODUCT:
                handleAddProduct(data);
                break;
            case UPDATE_PRODUCT:
                handleUpdateProduct(data);
                break;
            case DELETE_PRODUCT:
                handleDeleteProduct(data);
                break;
            case PLACE_BID:
                handlePlaceBid(data);
                break;
            case GET_AUCTION_DETAILS:
                handleGetAuctionDetails(data);
                break;
            case SUBSCRIBE_AUCTION:
                handleSubscribeAuction(data);
                break;
            case UNSUBSCRIBE_AUCTION:
                handleUnsubscribeAuction(data);
                break;
            case SET_AUTO_BID:
                handleSetAutoBid(data);
                break;
            case REMOVE_AUTO_BID:
                handleRemoveAutoBid(data);
                break;
            case GET_MY_PRODUCTS:
                handleGetMyProducts(data);
                break;
            case ADMIN_GET_ALL_USERS:
                handleAdminGetAllUsers(data);
                break;
            case ADMIN_UPDATE_USER:
                handleAdminUpdateUser(data);
                break;
            case ADMIN_DELETE_USER:
                handleAdminDeleteUser(data);
                break;
            case ADMIN_GET_ALL_PRODUCTS:
                handleAdminGetAllProducts(data);
                break;
            case ADMIN_DELETE_PRODUCT:
                handleAdminDeleteProduct(data);
                break;
            case LOGOUT:
                handleLogout();
                break;
            case ADD_FUNDS:
                handleAddFunds(data);
                break;
            case PROCESS_PAYMENT:
                handleProcessPayment(data);
                break;
            case GET_USER_BALANCE:
                handleGetUserBalance(data);
                break;


            case ADMIN_BAN_USER: //Admin khóa 1 người dùng
                handleAdminBanUser(data);
                break;
            case ADMIN_UNBAN_USER: //Admin mở khóa người dùng
                handleAdminUnbanUser(data);
                break;
            case GET_USER_INFO: //Profile thị thông tin
                handleGetUserInfo(data);
                break;
            case GET_PURCHASED_PRODUCTS: //Xem được sản phẩm đã mua
                handleGetPurchasedProducts(data);
                break;
            // Thêm vào switch trong handleRequest():
            case GET_AUCTION_HISTORY:
                handleGetAuctionHistory(data);
                break;


            default:
                sendError("Unknown command");

        }
    }
    /*public void handleLogin(Map<String,Object> data){
        String username = (String) data.get("username");
        String password = (String) data.get("password");
        Map<String,Object> result = auctionService.login(username,password);

        if ((boolean) result.get("success")){
            this.username = username;
            this.role = (String) result.get("role");
            AuctionServer.getInstance().registerClient(username, this);

            Map<String,Object> responseData = new HashMap<>();
            responseData.put("user",result.get("user"));
            responseData.put("role",role);

            sendResponse(CommandType.LOGIN,true,(String) result.get("message"),responseData);

        } else {
            sendError((String) result.get("message"));
        }
    }*/

    public void handleLogin(Map<String,Object> data) {
        String username = (String) data.get("username");
        String password = (String) data.get("password");
        Map<String,Object> result = auctionService.login(username, password);

        if ((boolean) result.get("success")) {
            // 🟢 LẤY USER ID
            Map<String,Object> userInfo = (Map<String,Object>) result.get("user");
            int userId = (int) userInfo.get("id");

            // 🟢🟢🟢 KIỂM TRA TÀI KHOẢN ĐANG ĐƯỢC DÙNG Ở NƠI KHÁC 🟢🟢🟢
            if (onlineUsers.containsKey(userId)) {
                sendResponse(CommandType.LOGIN, false, "Tài khoản đang được đăng nhập ở nơi khác!", null);
                return;
            }

            // 🟢 LƯU USER ID VÀO MAP VÀ BIẾN INSTANCE
            onlineUsers.put(userId, true);
            this.currentUserId = userId;  // Cần thêm biến này ở đầu class
            this.username = username;
            this.role = (String) result.get("role");

            AuctionServer.getInstance().registerClient(username, this);

            Map<String,Object> responseData = new HashMap<>();
            responseData.put("userData", userInfo);
            responseData.put("role", this.role);

            sendResponse(CommandType.LOGIN, true, "Đăng nhập thành công!", responseData);
        } else {
            sendResponse(CommandType.LOGIN, false, (String) result.get("message"), null);
        }
    }


    private void handleRegister(Map<String,Object> data){
        Map<String, Object> result = auctionService.register(data);
        sendResponse(CommandType.REGISTER, (boolean) result.get("success"), (String) result.get("message"), null);

    }
    private void handleGetProducts(Map<String, Object> data) {
        Map<String, Object> result = auctionService.getActiveProducts();
        sendResponse(CommandType.GET_PRODUCTS, true, "Success", result);
    }

    private void handleGetProductDetails(Map<String, Object> data) {
        int productId = ((Number) data.get("productId")).intValue();
        Map<String, Object> result = auctionService.getProductDetails(productId);
        sendResponse(CommandType.GET_PRODUCT_DETAILS, true, "Success", result);
    }
    private void handleAddProduct(Map<String, Object> data) {
        data.put("sellerId", username);
        Map<String, Object> result = auctionService.addProduct(data);
        boolean success = (boolean) result.get("success");
        sendResponse(CommandType.ADD_PRODUCT, success, (String) result.get("message"), result);

        // Broadcast thông báo có sản phẩm mới cho toàn bộ client đang kết nối
        if (success) {
            com.auction.shared.protocol.Response broadcast = new com.auction.shared.protocol.Response(
                    CommandType.NEW_PRODUCT_ADDED, true, "Có sản phẩm mới!", null);
            AuctionServer.getInstance().broadcastToAll(broadcast);
            logger.info("Đã broadcast NEW_PRODUCT_ADDED tới tất cả client");
        }
    }

    private void handleUpdateProduct(Map<String, Object> data) {
        data.put("sellerId", username);
        Map<String, Object> result = auctionService.updateProduct(data);
        sendResponse(CommandType.UPDATE_PRODUCT, (boolean) result.get("success"),
                (String) result.get("message"), result);
    }

    private void handleDeleteProduct(Map<String, Object> data) {
        int productId = ((Number) data.get("productId")).intValue();
        Map<String, Object> result = auctionService.deleteProduct(productId, username);
        sendResponse(CommandType.DELETE_PRODUCT, (boolean) result.get("success"),
                (String) result.get("message"), null);
    }
    private void handlePlaceBid(Map<String,Object> data){
        int productId = ((Number) data.get("productId")).intValue();
        double bidAmount = ((Number) data.get("bidAmount")).doubleValue();
        Map<String, Object> result = auctionService.placeBid(productId, username, bidAmount);

        // 🟢 THÊM 3 DÒNG NÀY 🟢
        if ((boolean) result.get("success")) {
            notificationService.notifyBidUpdate(productId, username, bidAmount);
        }

        sendResponse(CommandType.PLACE_BID, (boolean) result.get("success"), (String) result.get("message"), result);
    }
    private void handleGetAuctionDetails(Map<String, Object> data) {
        int productId = ((Number) data.get("productId")).intValue();
        Map<String, Object> result = auctionService.getAuctionDetails(productId);
        sendResponse(CommandType.GET_AUCTION_DETAILS, true, "Success", result);
    }

    private void handleSubscribeAuction(Map<String, Object> data) {
        int productId = ((Number) data.get("productId")).intValue();
        notificationService.subscribe(productId, username, this); // this la clienthandler
        sendResponse(CommandType.SUBSCRIBE_AUCTION, true, "Subscribed", null);
    }
    private void handleUnsubscribeAuction(Map<String, Object> data) {
        int productId = ((Number) data.get("productId")).intValue();
        notificationService.unsubscribe(productId, username);
        sendResponse(CommandType.UNSUBSCRIBE_AUCTION, true, "Unsubscribed", null);
    }
    private void handleSetAutoBid(Map<String,Object> data){
        int productId = ((Number) data.get("productId")).intValue();
        double maxBid = ((Number) data.get("maxBid")).doubleValue();
        double increment = ((Number) data.get("increment")).doubleValue();
        Map<String,Object> result = auctionService.setAutoBid(productId, username, maxBid, increment);
        sendResponse(CommandType.SET_AUTO_BID, (boolean) result.get("success"), (String) result.get("message"), result);

    }
    private void handleRemoveAutoBid(Map<String, Object> data) {
        int productId = ((Number) data.get("productId")).intValue();
        Map<String, Object> result = auctionService.removeAutoBid(productId, username);
        sendResponse(CommandType.REMOVE_AUTO_BID, (boolean) result.get("success"), (String) result.get("message"), null);
    }
    private void handleGetMyProducts(Map<String, Object> data) {
        Map<String, Object> result = auctionService.getSellerProducts(username);
        sendResponse(CommandType.GET_MY_PRODUCTS, true, "Success", result);
    }

    private void handleAdminGetAllUsers(Map<String, Object> data) {
        if (!isAdmin()) return;
        Map<String, Object> result = auctionService.getAllUsers();
        sendResponse(CommandType.ADMIN_GET_ALL_USERS, true, "Success", result);
    }
    private void handleAdminUpdateUser(Map<String, Object> data) {
        if (!isAdmin()) return;
        Map<String, Object> result = auctionService.adminUpdateUser(data);
        sendResponse(CommandType.ADMIN_UPDATE_USER, (boolean) result.get("success"),
                (String) result.get("message"), result);
    }
    private void handleAdminDeleteUser(Map<String, Object> data) {
        if (!isAdmin()) return;
        int userId = ((Number) data.get("userId")).intValue();
        Map<String, Object> result = auctionService.adminDeleteUser(userId);
        sendResponse(CommandType.ADMIN_DELETE_USER, (boolean) result.get("success"),
                (String) result.get("message"), null);
    }
    private void handleAdminGetAllProducts(Map<String, Object> data) {
        if (!isAdmin()) return;
        Map<String, Object> result = auctionService.getAllProducts();
        sendResponse(CommandType.ADMIN_GET_ALL_PRODUCTS, true, "Success", result);
    }

    private void handleAdminDeleteProduct(Map<String, Object> data) {
        if (!isAdmin()) return;
        int productId = ((Number) data.get("productId")).intValue();
        Map<String, Object> result = auctionService.adminDeleteProduct(productId);
        sendResponse(CommandType.ADMIN_DELETE_PRODUCT, (boolean) result.get("success"),
                (String) result.get("message"), null);
    }
    private void handleLogout() {
        if (username != null) {
            AuctionServer.getInstance().unregisterClient(username);
        }
        sendResponse(CommandType.LOGOUT, true, "Logged out", null);
        connected = false;
        disconnect();
    }
    private boolean isAdmin() {
        if (!"ADMIN".equals(role)) {
            sendError("Access denied: Admin role required");
            return false;
        }
        return true;
    }
    public void sendBidUpdate(int productId, String bidderName, double bidAmount) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        data.put("bidderName", bidderName);
        data.put("bidAmount", bidAmount);
        data.put("timestamp", java.time.LocalDateTime.now().toString());

        sendResponse(CommandType.BID_UPDATE, true, "New bid placed", data);
    }
    public void sendAuctionEnd(int productId, int winnerId, String winnerName, double finalPrice) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        data.put("winnerId", winnerId);
        data.put("winnerName", winnerName);
        data.put("finalPrice", finalPrice);

        sendResponse(CommandType.AUCTION_END, true, "Auction ended", data);
    }

    private void sendResponse(CommandType command, boolean success, String message, Map<String,Object> data){
        try{
            Response response = new Response(command,success,message,data);
            response.setRequestId(currentRequestId);
            outputStream.writeObject(response);
            outputStream.flush();

        }catch (IOException e){
            logger.error("Error sending response: " ,e);
        }
    }

    // Cho phép AuctionServer gọi để push dữ liệu (broadcast) xuống client này
    public void sendResponsePublic(com.auction.shared.protocol.Response response) {
        try {
            synchronized (outputStream) {
                outputStream.writeObject(response);
                outputStream.flush();
            }
        } catch (IOException e) {
            logger.error("Error broadcasting to client {}: ", username, e);
        }
    }
    private void sendError(String message) {
        sendResponse(CommandType.ERROR, false, message, null);
    }
    private void disconnect(){
        connected = false;
        try{
            // 🟢🟢🟢 XÓA USER ID KHỎI MAP ĐANG ONLINE 🟢🟢🟢
            if (currentUserId > 0) {
                onlineUsers.remove(currentUserId);
                logger.info("User {} đã logout, xóa khỏi danh sách online", currentUserId);
            }

            if (username != null){
                AuctionServer.getInstance().unregisterClient(username);
            }
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();

        } catch (IOException e){
            logger.error("Error disconnecting: " ,e);
        }
    }
    public String getUsername(){
        return username;
    }

    public void sendAuctionExtended(int productId, LocalDateTime newEndTime) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        data.put("newEndTime", newEndTime.toString());

        sendResponse(CommandType.AUCTION_EXTENDED, true, "Auction extended", data);
    }

    private void handleAddFunds(Map<String, Object> data) {
        // Lấy userId từ username đang đăng nhập (an toàn hơn nhận từ client)
        com.auction.shared.model.User user = auctionService.getUserByUsername(username);
        if (user == null) {
            sendResponse(CommandType.ADD_FUNDS, false, "Không tìm thấy người dùng!", null);
            return;
        }
        double amount = ((Number) data.get("amount")).doubleValue();
        Map<String, Object> result = auctionService.addFunds(user.getId(), amount);
        sendResponse(CommandType.ADD_FUNDS,
                (boolean) result.get("success"),
                (String) result.get("message"),
                result);
    }

    private void handleProcessPayment(Map<String, Object> data) {
        int userId = ((Number) data.get("userId")).intValue();
        int auctionId = ((Number) data.get("auctionId")).intValue();
        Map<String, Object> result = auctionService.processPayment(userId, auctionId);
        sendResponse(CommandType.PROCESS_PAYMENT,
                (boolean) result.get("success"),
                (String) result.get("message"),
                result);
    }

    private void handleGetUserBalance(Map<String, Object> data) {
        int userId = ((Number) data.get("userId")).intValue();
        Map<String, Object> result = auctionService.getUserBalance(userId);
        sendResponse(CommandType.GET_USER_BALANCE,
                (boolean) result.get("success"),
                (String) result.get("message"),
                result);
    }




    private void handleAdminBanUser(Map<String, Object> data) {
        if (!isAdmin()) return;
        int userId = ((Number) data.get("userId")).intValue();
        Map<String, Object> result = auctionService.banUser(userId);
        sendResponse(CommandType.ADMIN_BAN_USER, (boolean) result.get("success"),
                (String) result.get("message"), null);
    }

    private void handleAdminUnbanUser(Map<String, Object> data) {
        if (!isAdmin()) return;
        int userId = ((Number) data.get("userId")).intValue();
        Map<String, Object> result = auctionService.unbanUser(userId);
        sendResponse(CommandType.ADMIN_UNBAN_USER, (boolean) result.get("success"),
                (String) result.get("message"), null);
    }

    private void handleGetUserInfo(Map<String, Object> data) {
        try {
            System.out.println("📊 handleGetUserInfo() - username: " + username);
            Map<String, Object> result = auctionService.getUserInfo(username);
            sendResponse(CommandType.GET_USER_INFO, true, "Success", result);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(CommandType.GET_USER_INFO, false, "Error: " + e.getMessage(), null);
        }
    }

    private void handleGetPurchasedProducts(Map<String, Object> data) {
        try {
            System.out.println("🔍 handleGetPurchasedProducts - username: " + username);
            Map<String, Object> result = auctionService.getPurchasedProducts(username);
            sendResponse(CommandType.GET_PURCHASED_PRODUCTS, true, "Success", result);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(CommandType.GET_PURCHASED_PRODUCTS, false, "Error: " + e.getMessage(), null);
        }
    }






    // Thêm method coi lịch sử bid
    private void handleGetAuctionHistory(Map<String, Object> data) {
        int productId = ((Number) data.get("productId")).intValue();
        List<BidTransaction> history = bidDAO.getBidsByProduct(productId);
        Map<String, Object> result = new HashMap<>();
        result.put("history", history);
        sendResponse(CommandType.GET_AUCTION_HISTORY, true, "Success", result);
    }

}
