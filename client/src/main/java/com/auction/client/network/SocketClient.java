package com.auction.client.network;
import com.auction.shared.protocol.*;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
public class SocketClient {
    private static SocketClient instance;
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private String serverHost;
    private int serverPort;
    private String authToken;
    private boolean connected;
    private Map<CommandType,Consumer<Response>> responseHandlers;
    private Thread listenerThread;

    private SocketClient(){
        this.serverHost = "localhost";
        this.serverPort = 8888;
        this.responseHandlers = new ConcurrentHashMap<>();
    }
    public static SocketClient getInstance(){
        if (instance == null){
            instance = new SocketClient();
        }
        return instance;
    }
    public void connect() throws IOException{
        if (connected) return;
        socket = new Socket(serverHost,serverPort);
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());
        connected = true;

        startListener();
        System.out.println("Connected to server");

    }
    private void startListener(){
        listenerThread = new Thread(() -> {
            try{
                while (connected){
                    Object obj = inputStream.readObject();
                    if (obj instanceof Response){
                        Response response = (Response) obj;
                        Consumer<Response> handler = responseHandlers.get(response.getCommand());
                        if (handler != null){
                            handler.accept(response);
                        }
                    }
                }
            } catch (EOFException e){
                System.out.println("Connection closed by server");
            } catch (IOException | ClassNotFoundException e){
                if (connected){
                    System.err.println("Error receiving response: " + e.getMessage());
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    public void sendRequest(Request request) throws IOException{
        if (!connected){
            throw new IOException("Not connected to server");
        }
        if (authToken != null) {
            request.setToken(authToken);  // Thêm token vào request
        }
        outputStream.writeObject(request);
        outputStream.flush();
    }
    public void sendRequestAsync(Request request,Consumer<Response> callback){
        if (callback != null){
            responseHandlers.put(request.getCommand(),callback);

        }
        new Thread(() -> {
            try{
                sendRequest(request);
            } catch (IOException e){
                System.err.println("Error sending request: " + e.getMessage());
                if (callback != null){
                    Response errorResponse = new Response(request.getCommand(), false, e.getMessage());
                    callback.accept(errorResponse);
                }
            }
        });
    }
    public void login(String username, String password, Consumer<Response> callback){
        Map<String,Object> data = new HashMap<>();
        data.put("username",username);
        data.put("password",password);

        Request request = new Request(CommandType.LOGIN,data);
        sendRequestAsync(request,callback);
    }
    public void register(Map<String, Object> userData, Consumer<Response> callback){
        Request request = new Request(CommandType.REGISTER,userData);
        sendRequestAsync(request, callback);
    }
    public void getProducts(Consumer<Response> callback) {
        Request request = new Request(CommandType.GET_PRODUCTS, new HashMap<>());
        sendRequestAsync(request, callback);
    }

    public void getProductDetails(int productId, Consumer<Response> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        Request request = new Request(CommandType.GET_PRODUCT_DETAILS, data);
        sendRequestAsync(request, callback);
    }

    public void placeBid(int productId, double bidAmount, Consumer<Response> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        data.put("bidAmount", bidAmount);
        Request request = new Request(CommandType.PLACE_BID, data);
        sendRequestAsync(request, callback);
    }

    public void addProduct(Map<String, Object> productData, Consumer<Response> callback) {
        Request request = new Request(CommandType.ADD_PRODUCT, productData);
        sendRequestAsync(request, callback);
    }

    public void updateProduct(Map<String, Object> productData, Consumer<Response> callback) {
        Request request = new Request(CommandType.UPDATE_PRODUCT, productData);
        sendRequestAsync(request, callback);
    }

    public void deleteProduct(int productId, Consumer<Response> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        Request request = new Request(CommandType.DELETE_PRODUCT, data);
        sendRequestAsync(request, callback);
    }

    public void subscribeAuction(int productId, Consumer<Response> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        Request request = new Request(CommandType.SUBSCRIBE_AUCTION, data);
        sendRequestAsync(request, callback);
    }

    public void unsubscribeAuction(int productId, Consumer<Response> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        Request request = new Request(CommandType.UNSUBSCRIBE_AUCTION, data);
        sendRequestAsync(request, callback);
    }

    public void setAutoBid(int productId, double maxBid, double increment,Consumer<Response> callback){
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        data.put("maxBid", maxBid);
        data.put("increment", increment);
        Request request = new Request(CommandType.SET_AUTO_BID, data);
        sendRequestAsync(request, callback);

    }
    public void removeAutoBid(int productId, Consumer<Response> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        Request request = new Request(CommandType.REMOVE_AUTO_BID, data);
        sendRequestAsync(request, callback);
    }

    public void getMyProducts(Consumer<Response> callback) {
        Request request = new Request(CommandType.GET_MY_PRODUCTS, new HashMap<>());
        sendRequestAsync(request, callback);
    }

    public void adminGetAllUsers(Consumer<Response> callback) {
        Request request = new Request(CommandType.ADMIN_GET_ALL_USERS, new HashMap<>());
        sendRequestAsync(request, callback);
    }

    public void adminUpdateUser(Map<String, Object> userData, Consumer<Response> callback) {
        Request request = new Request(CommandType.ADMIN_UPDATE_USER, userData);
        sendRequestAsync(request, callback);
    }

    public void adminDeleteUser(int userId, Consumer<Response> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        Request request = new Request(CommandType.ADMIN_DELETE_USER, data);
        sendRequestAsync(request, callback);
    }

    public void adminGetAllProducts(Consumer<Response> callback) {
        Request request = new Request(CommandType.ADMIN_GET_ALL_PRODUCTS, new HashMap<>());
        sendRequestAsync(request, callback);
    }

    public void adminDeleteProduct(int productId, Consumer<Response> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        Request request = new Request(CommandType.ADMIN_DELETE_PRODUCT, data);
        sendRequestAsync(request, callback);
    }

    public void logout(Consumer<Response> callback) {
        Request request = new Request(CommandType.LOGOUT, new HashMap<>());
        sendRequestAsync(request, callback);
    }

    public void setBidUpdateHandler(Consumer<Response> handler) {
        responseHandlers.put(CommandType.BID_UPDATE, handler);
    }

    public void setAuctionEndHandler(Consumer<Response> handler) {
        responseHandlers.put(CommandType.AUCTION_END, handler);
    }
    public void setAuctionExtendedHandler(Consumer<Response> handler) {
        responseHandlers.put(CommandType.AUCTION_EXTENDED, handler);
    }

    public void disconnect() {
        connected = false;
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    public String getAuthToken() {
        return authToken;
    }

    // Nạp tiền
    public void addFunds(int userId, double amount, Consumer<Response> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("amount", amount);
        Request request = new Request(CommandType.ADD_FUNDS, data);
        sendRequestAsync(request, callback);
    }

    // Thanh toán
    public void processPayment(int userId, int auctionId, Consumer<Response> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("auctionId", auctionId);
        Request request = new Request(CommandType.PROCESS_PAYMENT, data);
        sendRequestAsync(request, callback);
    }

    // Lấy số dư
    public void getBalance(int userId, Consumer<Response> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        Request request = new Request(CommandType.GET_USER_BALANCE, data);
        sendRequestAsync(request, callback);
    }

}
