package com.auction.server;

import com.auction.server.service.AuctionService;
import com.auction.server.service.AutoBidService;
import com.auction.server.service.NotificationService;
import com.auction.shared.protocol.CommandType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private String username;
    private String role;
    private boolean connected;
    private AuctionService auctionService;
    private NotificationService notificationService;
    private String currentRequestId; // ✅ THÊM MỚI - thêm cùng chỗ với các field khác

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.connected = true;
        this.auctionService = AuctionService.getInstance();
        this.notificationService = NotificationService.getInstance();

        try {
            this.outputStream = new ObjectOutputStream(socket.getOutputStream());
            this.inputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.err.println("Error creating streams: " + e.getMessage());
            connected = false;
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
        } catch (EOFException e) {
            System.out.println("Client disconnected: " + username);
        } catch (IOException | ClassNotFoundException e) {
            if (connected) {
                System.err.println("Error handling client: " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }

    private void handleRequest(Request request) {
        if (request == null) {
            System.err.println("SERVER LỖI: Nhận được Request rỗng (null)!");
            return;
        }

        this.currentRequestId = request.getRequestId(); // ✅ THÊM DÒNG NÀY

        CommandType command = request.getCommand();
        Map<String, Object> payload = (Map<String, Object>) request.getData();
        System.out.println(">>> SERVER NHẬN LỆNH: " + command);

        switch (command) {
            case LOGIN:
                handleLogin(payload);
                break;
            case LOGOUT:
                handleLogout();
                break;
            case REGISTER:
                handleRegister(payload);
                break;
            case GET_PRODUCTS:
                handleGetProducts(payload);
                break;
            case GET_PRODUCT_DETAILS:
                handleGetProductDetails(payload);
                break;
            case PLACE_BID:
                handlePlaceBid(payload);
                break;
            case ADD_PRODUCT:
                handleAddProduct(payload);
                break;
            case GET_AUCTION_HISTORY:
                handleGetAuctionHistory(payload);
                break;
            case UPDATE_PRODUCT:
                handleUpdateProduct(payload);
                break;
            case DELETE_PRODUCT:
                handleDeleteProduct(payload);
                break;
            case GET_MY_PRODUCTS:
                handleGetMyProducts(payload);
                break;
            case SUBSCRIBE_AUCTION:
                handleSubscribeAuction(payload);
                break;
            case UNSUBSCRIBE_AUCTION:
                handleUnsubscribeAuction(payload);
                break;
            case ADMIN_BAN_USER:
                handleAdminBanUser(payload);
                break;
            case ADMIN_UNBAN_USER:
                handleAdminUnbanUser(payload);
                break;
            case SET_AUTO_BID:
                handleSetAutoBid(payload);
                break;
            case REMOVE_AUTO_BID:
                handleRemoveAutoBid(payload);
                break;
            case ADMIN_GET_ALL_USERS:
                Map<String, Object> usersResult = auctionService.getAllUsers();
                sendResponse(CommandType.ADMIN_GET_ALL_USERS, true, "Success", usersResult);
                break;

            case ADMIN_UPDATE_USER:
                handleAdminUpdateUser(payload);
                break;
            case ADMIN_DELETE_USER:
                handleAdminDeleteUser(payload);
                break;
            case ADMIN_GET_ALL_PRODUCTS:
                Map<String, Object> productsResult = auctionService.getAllProducts();
                sendResponse(CommandType.ADMIN_GET_ALL_PRODUCTS, true, "Success", productsResult);
                break;
            case ADMIN_DELETE_PRODUCT:
                handleAdminDeleteProduct(payload);
                break;
            case RECHARGE_BALANCE:
                handleRechargeBalance(payload);
                break;
            case GET_USER_INFO:
                handleGetUserInfo(payload);
                break;
            case UPDATE_USER:
                handleUpdateUser(payload);
                break;
            case GET_PURCHASED_PRODUCTS:
                handleGetPurchasedProducts(payload);
                break;
            default:
                sendError("Lệnh không được hỗ trợ: " + command);
                break;
        }
    }

    private void handleLogin(Map<String, Object> data) {
        String user = (String) data.get("username");
        String pass = (String) data.get("password");

        Map<String, Object> result = auctionService.login(user, pass);
        boolean success = (boolean) result.getOrDefault("success", false);

        if (success) {
            this.username = user;
            Map<String, Object> userData = (Map<String, Object>) result.get("userData");
            if (userData != null) {
                this.role = (String) userData.get("role"); // ← ĐÚNG
            }
            AuctionServer.getInstance().registerClient(username, this);
        }

        sendResponse(CommandType.LOGIN, success, (String) result.get("message"), result);
    }

    private void handleLogout() {
        System.out.println("📤 Client logout: " + username);
        if (username != null) {
            AuctionServer.getInstance().unregisterClient(username);
        }
        sendResponse(CommandType.LOGOUT, true, "Logged out", null);
        disconnect();
    }

    private void handleRegister(Map<String, Object> payload) {
        Map<String, Object> result = auctionService.register(payload);
        sendResponse(CommandType.REGISTER, (boolean) result.get("success"),
                (String) result.get("message"), result);
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
        sendResponse(CommandType.ADD_PRODUCT, (boolean) result.get("success"),
                (String) result.get("message"), result);
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

    private void handlePlaceBid(Map<String, Object> data) {
        try {
            int productId = ((Number) data.get("productId")).intValue();
            double bidAmount = ((Number) data.get("bidAmount")).doubleValue();

            System.out.println("💰 ĐẶT GIÁ: sản phẩm " + productId + ", người dùng " + username + ", giá " + bidAmount);

            Map<String, Object> result = auctionService.placeBid(productId, username, bidAmount);
            boolean success = result != null && result.containsKey("success") && (boolean) result.get("success");

            if (success) {
                notificationService.notifyBidUpdate(productId, username, bidAmount);
            }

            sendResponse(CommandType.PLACE_BID, success,
                    success ? (String) result.get("message") : "Đặt giá thất bại",
                    success ? result : null);

        } catch (Exception e) {
            System.err.println("❌ Lỗi trong handlePlaceBid: " + e.getMessage());
            e.printStackTrace();
            sendResponse(CommandType.PLACE_BID, false, "Lỗi: " + e.getMessage(), null);
        }
    }

    private void handleGetAuctionHistory(Map<String, Object> data) {
        int productId = ((Number) data.get("productId")).intValue();
        Map<String, Object> result = auctionService.getBidHistory(productId);
        sendResponse(CommandType.GET_AUCTION_HISTORY, true, "Success", result);
    }

    private void handleGetAuctionDetails(Map<String, Object> data) {
        int productId = ((Number) data.get("productId")).intValue();
        Map<String, Object> result = auctionService.getAuctionDetails(productId);
        sendResponse(CommandType.GET_AUCTION_DETAILS, true, "Success", result);
    }

    private void handleSubscribeAuction(Map<String, Object> data) {
        int productId = ((Number) data.get("productId")).intValue();
        if (username != null) {
            notificationService.subscribe(productId, username, this);
            sendResponse(CommandType.SUBSCRIBE_AUCTION, true, "Subscribed", null);
            System.out.println("✅ User " + username + " subscribed to auction " + productId);
        } else {
            sendResponse(CommandType.SUBSCRIBE_AUCTION, false, "User not logged in", null);
        }
    }

    private void handleUnsubscribeAuction(Map<String, Object> data) {
        int productId = ((Number) data.get("productId")).intValue();
        notificationService.unsubscribe(productId, username);
        sendResponse(CommandType.UNSUBSCRIBE_AUCTION, true, "Unsubscribed", null);
    }

    private void handleSetAutoBid(Map<String, Object> data) {
        int productId = ((Number) data.get("productId")).intValue();
        double maxBid = ((Number) data.get("maxBid")).doubleValue();
        double increment = ((Number) data.get("increment")).doubleValue();

        AutoBidService.getInstance().registerAutoBid(productId, username, maxBid);

        Map<String, Object> result = auctionService.setAutoBid(productId, username, maxBid, increment);
        sendResponse(CommandType.SET_AUTO_BID, (boolean) result.get("success"),
                (String) result.get("message"), result);
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

    private void handleAdminGetAllUsers(Map<String, Object> data) {
        if (!isAdmin()) return;
        Map<String, Object> result = auctionService.getAllUsers();
        sendResponse(CommandType.ADMIN_GET_ALL_USERS, true, "Success", result);
        // KHÔNG disconnect ở đây
    }

    private void handleAdminGetAllProducts(Map<String, Object> data) {
        if (!isAdmin()) return;
        Map<String, Object> result = auctionService.getAllProducts();
        sendResponse(CommandType.ADMIN_GET_ALL_PRODUCTS, true, "Success", result);
        // KHÔNG disconnect ở đây
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



    private void handleAdminDeleteProduct(Map<String, Object> data) {
        if (!isAdmin()) return;
        int productId = ((Number) data.get("productId")).intValue();
        Map<String, Object> result = auctionService.adminDeleteProduct(productId);
        sendResponse(CommandType.ADMIN_DELETE_PRODUCT, (boolean) result.get("success"),
                (String) result.get("message"), null);
    }

    private void handleRechargeBalance(Map<String, Object> data) {
        double amount = ((Number) data.get("amount")).doubleValue();
        Map<String, Object> result = auctionService.rechargeBalance(username, amount);
        sendResponse(CommandType.RECHARGE_BALANCE, (boolean) result.get("success"),
                (String) result.get("message"), result);
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

    private void handleUpdateUser(Map<String, Object> data) {
        data.put("username", username);
        Map<String, Object> result = auctionService.updateUserInfo(data);
        sendResponse(CommandType.UPDATE_USER, (boolean) result.get("success"),
                (String) result.get("message"), result);
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

    private void sendResponse(CommandType command, boolean success, String message, Map<String, Object> data) {
        if (!connected || outputStream == null) {
            System.err.println("❌ Không gửi response: socket đã đóng");
            return;
        }
        try {
            Response response = new Response(command, success, message, data);
            response.setRequestId(currentRequestId); // ✅ THÊM MỚI - echo requestId về client
            outputStream.writeObject(response);
            outputStream.flush();
        } catch (IOException e) {
            System.err.println("❌ Lỗi gửi response: " + e.getMessage());
        }
    }

    private void sendError(String message) {
        sendResponse(CommandType.ERROR, false, message, null);
    }

    private boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public void disconnect() {
        if (!connected) return;
        connected = false;
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }

    public String getUsername() {
        return username;
    }

    public void sendBidUpdate(int productId, String bidderName, double bidAmount) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        data.put("bidderName", bidderName);
        data.put("bidAmount", bidAmount);
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

    public void sendAuctionExtended(int productId, LocalDateTime newEndTime) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        data.put("newEndTime", newEndTime.toString());
        sendResponse(CommandType.AUCTION_EXTENDED, true, "Auction extended", data);
    }
}