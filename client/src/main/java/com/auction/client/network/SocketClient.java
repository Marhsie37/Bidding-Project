package com.auction.client.network;

import com.auction.shared.protocol.CommandType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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

    // ✅ pendingCallbacks theo requestId - tránh ghi đè lẫn nhau (từ bản 2)
    private Map<String, Consumer<Response>> pendingCallbacks = new ConcurrentHashMap<>();
    // ✅ responseHandlers theo CommandType - dùng cho BID_UPDATE, AUCTION_END... (từ bản 1)
    private Map<CommandType, Consumer<Response>> responseHandlers = new ConcurrentHashMap<>();

    private Thread listenerThread;
    private final Object writeLock = new Object();
    private static final Logger logger = LoggerFactory.getLogger(SocketClient.class);

    private SocketClient() {
        this.serverHost = "localhost";
        this.serverPort = 9999;
    }

    public static SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    public void connect() throws IOException {
        // XÓA DÒNG if (connected.get()) { ... return; }
        socket = new Socket(serverHost, serverPort);
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());
        connected = true;
        startListener();
        logger.info("Connected to server");
    }


    private void startListener() {
        listenerThread = new Thread(() -> {
            try {
                while (connected) {
                    Object obj = inputStream.readObject();
                    if (obj instanceof Response) {
                        Response response = (Response) obj;

                        String requestId = response.getRequestId();
                        Consumer<Response> handler = null;

                        if (requestId != null) {
                            handler = pendingCallbacks.remove(requestId);
                        }

                        // ✅ Fallback: tìm theo CommandType (từ bản 1)
                        if (handler == null) {
                            handler = responseHandlers.get(response.getCommand());
                        }

                        if (handler != null) {
                            handler.accept(response);
                        } else {
                            logger.warn("⚠️ Không có handler cho: {} (requestId={})",
                                    response.getCommand(), requestId);
                        }
                    }
                }
            } catch (EOFException e) {
                logger.info("Connection closed by server");
            } catch (IOException | ClassNotFoundException e) {
                if (connected) {
                    logger.error("Error receiving response: ", e);
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void sendRequest(Request request) throws IOException {
        if (!connected || socket == null || socket.isClosed()) {
            throw new IOException("Not connected to server");
        }
        if (authToken != null) {
            request.setToken(authToken);
        }
        // ✅ Đồng bộ ghi socket (từ bản 2)
        synchronized (writeLock) {
            outputStream.writeObject(request);
            outputStream.flush();
        }
    }

    public void sendRequestAsync(Request request, Consumer<Response> callback) {
        // ✅ Đăng ký handler TRƯỚC khi gửi
        if (callback != null) {
            responseHandlers.put(request.getCommand(), callback);
        }
        // ✅ Gửi trong 1 thread riêng nhưng có synchronized bên trong
        new Thread(() -> {
            try {
                sendRequest(request);
            } catch (IOException e) {
                System.err.println("LỖI GỬI SOCKET: " + e.getMessage());
                responseHandlers.remove(request.getCommand());
                if (callback != null) {
                    callback.accept(new Response(request.getCommand(), false, e.getMessage()));
                }
            }
        }).start();
    }

    public void login(String username, String password, Consumer<Response> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("password", password);
        sendRequestAsync(new Request(CommandType.LOGIN, data), callback);
    }

    public void register(Map<String, Object> userData, Consumer<Response> callback) {
        sendRequestAsync(new Request(CommandType.REGISTER, userData), callback);
    }

    public void getProducts(Consumer<Response> callback) {
        sendRequestAsync(new Request(CommandType.GET_PRODUCTS, new HashMap<>()), callback);
    }

    public void getProductDetails(int productId, Consumer<Response> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        sendRequestAsync(new Request(CommandType.GET_PRODUCT_DETAILS, data), callback);
    }

    public void placeBid(int productId, double bidAmount, Consumer<Response> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        data.put("bidAmount", bidAmount);
        sendRequestAsync(new Request(CommandType.PLACE_BID, data), callback);
    }

    public void addProduct(Map<String, Object> productData, Consumer<Response> callback) {
        sendRequestAsync(new Request(CommandType.ADD_PRODUCT, productData), callback);
    }

    public void updateProduct(Map<String, Object> productData, Consumer<Response> callback) {
        sendRequestAsync(new Request(CommandType.UPDATE_PRODUCT, productData), callback);
    }

    public void deleteProduct(int productId, Consumer<Response> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        sendRequestAsync(new Request(CommandType.DELETE_PRODUCT, data), callback);
    }

    public void getMyProducts(Consumer<Response> callback) {
        sendRequestAsync(new Request(CommandType.GET_MY_PRODUCTS, new HashMap<>()), callback);
    }

    public void subscribeAuction(int productId, Consumer<Response> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        sendRequestAsync(new Request(CommandType.SUBSCRIBE_AUCTION, data), callback);
    }

    public void unsubscribeAuction(int productId, Consumer<Response> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        sendRequestAsync(new Request(CommandType.UNSUBSCRIBE_AUCTION, data), callback);
    }

    public void setAutoBid(int productId, double maxBid, double increment, Consumer<Response> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        data.put("maxBid", maxBid);
        data.put("increment", increment);
        sendRequestAsync(new Request(CommandType.SET_AUTO_BID, data), callback);
    }

    public void removeAutoBid(int productId, Consumer<Response> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        sendRequestAsync(new Request(CommandType.REMOVE_AUTO_BID, data), callback);
    }

    public void adminGetAllUsers(Consumer<Response> callback) {
        sendRequestAsync(new Request(CommandType.ADMIN_GET_ALL_USERS, new HashMap<>()), callback);
    }

    public void adminUpdateUser(Map<String, Object> userData, Consumer<Response> callback) {
        sendRequestAsync(new Request(CommandType.ADMIN_UPDATE_USER, userData), callback);
    }

    public void adminDeleteUser(int userId, Consumer<Response> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        sendRequestAsync(new Request(CommandType.ADMIN_DELETE_USER, data), callback);
    }

    public void adminGetAllProducts(Consumer<Response> callback) {
        sendRequestAsync(new Request(CommandType.ADMIN_GET_ALL_PRODUCTS, new HashMap<>()), callback);
    }

    public void adminDeleteProduct(int productId, Consumer<Response> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        sendRequestAsync(new Request(CommandType.ADMIN_DELETE_PRODUCT, data), callback);
    }

    public void logout(Consumer<Response> callback) {
        sendRequestAsync(new Request(CommandType.LOGOUT, new HashMap<>()), callback);
    }

    // ========== PERSISTENT HANDLERS (từ bản 1) ==========
    public void setBidUpdateHandler(Consumer<Response> handler) {
        responseHandlers.put(CommandType.BID_UPDATE, handler);
    }

    public void setAuctionEndHandler(Consumer<Response> handler) {
        responseHandlers.put(CommandType.AUCTION_END, handler);
    }

    public void setAuctionExtendedHandler(Consumer<Response> handler) {
        responseHandlers.put(CommandType.AUCTION_EXTENDED, handler);
    }

    // ========== UTILITY ==========
    public void disconnect() {
        connected = false;
        pendingCallbacks.clear();
        responseHandlers.clear();
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            logger.error("Error disconnecting: ", e);
        }
        inputStream = null;
        outputStream = null;
        socket = null;
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

    public void clearHandlers() {
        pendingCallbacks.clear();
        responseHandlers.clear();
        logger.info("✅ Đã xóa toàn bộ handlers");
    }


    public Consumer<Response> getResponseHandler(CommandType command) {
        return responseHandlers.get(command);
    }
    public void registerResponseHandler(CommandType command, Consumer<Response> handler) {
        responseHandlers.put(command, handler);
    }





}