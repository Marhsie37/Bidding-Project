package com.auction.client.network;

import com.auction.shared.protocol.CommandType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
    private Map<CommandType, Consumer<Response>> responseHandlers;
    private Thread listenerThread;

    private SocketClient() {
        this.serverHost = "localhost";
        this.serverPort = 9999;
        this.responseHandlers = new ConcurrentHashMap<>();
    }

    public static SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    public void connect() throws IOException {
        if (connected) return;
        socket = new Socket(serverHost, serverPort);
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());
        connected = true;
        startListener();
        System.out.println("Connected to server");
    }



    private void startListener() {
        listenerThread = new Thread(() -> {
            try {
                while (connected) {
                    Object obj = inputStream.readObject();
                    if (obj instanceof Response) {
                        Response response = (Response) obj;
                        Consumer<Response> handler = responseHandlers.get(response.getCommand());
                        if (handler != null) {
                            handler.accept(response);
                        }
                    }
                }
            } catch (EOFException e) {
                System.out.println("Connection closed by server");
            } catch (IOException | ClassNotFoundException e) {
                if (connected) {
                    System.err.println("Error receiving response: " + e.getMessage());
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
        // Đồng bộ luồng đầu ra
        outputStream.writeObject(request);
        outputStream.flush();
    }

    public void sendRequestAsync(Request request, Consumer<Response> callback) {
        if (callback != null) {
            responseHandlers.put(request.getCommand(), callback);
        }
        new Thread(() -> {
            try {
                sendRequest(request);
            } catch (IOException e) {
                System.err.println("LỖI GỬI SOCKET: " + e.getMessage());
                if (callback != null) {
                    callback.accept(new Response(request.getCommand(), false, e.getMessage()));
                }
                // ✅ Xóa handler sau khi có lỗi
                responseHandlers.remove(request.getCommand());
            }
        }).start();
    }

    // ========== CÁC METHOD GỬI REQUEST ==========
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
        data.put("producstId", productId);
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

    public boolean isConnected() { return connected; }
    public void setAuthToken(String token) { this.authToken = token; }
    public String getAuthToken() { return authToken; }

    public void clearHandlers() {
        responseHandlers.clear();
        System.out.println("✅ Đã xóa toàn bộ response handlers");
    }
}