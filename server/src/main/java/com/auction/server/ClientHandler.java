package com.auction.server;

import com.auction.shared.protocol.*;
import com.auction.server.service.AuctionService;
import com.auction.server.service.NotificationService;


import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
public class ClientHandler implements Runnable {
    private  Socket socket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private String username;
    private String role;
    private boolean connected;
    private AuctionService auctionService;
    private NotificationService notificationService;


    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.connected = true;
        this.auctionService = AuctionService.getInstance();
        this.notificationService = NotificationService.getInstance();


        try {
            this.outputStream = new ObjectOutputStream(socket.getOutputStream());
            this.inputStream = new ObjectInputStream((socket.getInputStream()));
        } catch (IOException e) {
            System.err.println("Error creating streams: " + e.getMessage());

        }
    }

    public void run(){
        try{
            while (connected){
                Object obj = inputStream.readObject();
                if (obj instanceof Request){
                    Request request = (Request) obj;
                    handleRequest(request);
                }
            }
        } catch (EOFException e){
            System.out.println("Client disconnected: " + username);

        } catch (IOException | ClassNotFoundException e){
            System.err.println("Error handling client: " + e.getMessage());

        } finally {
            disconnect();
        }
    }
    public void handleRequest(Request request){
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
            default:
                sendError("Unknown command");

        }
    }
    public void handleLogin(Map<String,Object> data){
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
    private void handlePlaceBid(Map<String,Object> data){
        int productId = ((Number) data.get("productId")).intValue();
        double bidAmount = ((Number) data.get("bidAmount")).doubleValue();
        Map<String, Object> result = auctionService.placeBid(productId, username, bidAmount);
        if ((boolean) result.get("success")){
            //notify all subscibers
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
            outputStream.writeObject(response);
            outputStream.flush();

        }catch (IOException e){
            System.err.println("Error sending response: " + e.getMessage());
        }
    }
    private void sendError(String message) {
        sendResponse(CommandType.ERROR, false, message, null);
    }
    private void disconnect(){
        connected = false;
        try{
            if (username != null){
                AuctionServer.getInstance().unregisterClient(username);
            }
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();

        } catch (IOException e){
            System.err.println("Error disconnecting: " + e.getMessage());
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


}
