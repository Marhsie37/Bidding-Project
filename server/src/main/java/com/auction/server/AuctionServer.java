package com.auction.server;

import com.auction.server.service.AuctionService;
import com.auction.server.service.AutoBidService;
import com.auction.shared.model.AuctionSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionServer {
    private static final int PORT = 9999;
    private static volatile AuctionServer instance;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private ConcurrentHashMap<String, ClientHandler> clients;
    private AuctionService auctionService;
    private AutoBidService autoBidService;
    private boolean running;
    private static final Logger logger = LoggerFactory.getLogger(AuctionServer.class);

    private AuctionServer() {
        this.threadPool = Executors.newCachedThreadPool();
        this.clients = new ConcurrentHashMap<>();
        this.auctionService = AuctionService.getInstance();
        this.autoBidService = AutoBidService.getInstance();
        this.running = true;

    }

    public static AuctionServer getInstance() {
        if (instance == null) {
            synchronized (AuctionServer.class) {
                if (instance == null) {
                    instance = new AuctionServer();
                }
            }
        }
        return instance;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Auction Server started on port " + PORT);
            autoBidService.start();
            startAuctionMonitor();
            while (running) {
                Socket clientSocket = serverSocket.accept();
                // THÊM 2 DÒNG NÀY
                clientSocket.setSoTimeout(0); // Vô hiệu hóa timeout đọc
                clientSocket.setKeepAlive(true); // Giữ kết nối sống
                ClientHandler handler = new ClientHandler(clientSocket);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private void startAuctionMonitor() {
        Thread monitor = new Thread(() -> {
            while (running) {
                try {
                    List<AuctionSession> activeAuctions = auctionService.getActiveAuctions();
                    for (AuctionSession auction : activeAuctions) {
                        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
                            auctionService.endAuction(auction.getId());
                        }
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Monitor error: ", e);
                }
            }
        });
        monitor.setDaemon(true);
        monitor.start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        threadPool.shutdown();
        autoBidService.stop();
        logger.info("Server stopped");
    }

    public void registerClient(String username, ClientHandler handler) {
        // 1. Giữ logic phòng thủ của bạn: Tự động xóa vết client cũ nếu trùng tên
        if (clients.containsKey(username)) {
            clients.remove(username);
            // KHÔNG gọi old.disconnect() ở đây để tránh lỗi giật/sập Socket Client dưới máy
            logger.info("Đã thay thế client cũ: " + username);
        }

        // 2. Đăng ký client mới vào hệ thống như bình thường
        clients.put(username, handler);

        // 3. Dùng Logger chuẩn của Git thay cho System.out
        logger.info("Client registered: " + username);
    }

    public void broadcastToAll(com.auction.shared.protocol.Response response) {
        for (ClientHandler handler : clients.values()) {
            handler.sendResponsePublic(response);
        }
    }

    public void unregisterClient(String username) {
        clients.remove(username);
        logger.info("Client unregistered: " + username);
    }

    public ClientHandler getClient(String username) {
        return clients.get(username);
    }

    public ConcurrentHashMap<String, ClientHandler> getClients() {
        return clients;
    }

    public AuctionService getAuctionService() {
        return auctionService;
    }

    public static void main(String[] args) {
        AuctionServer server = AuctionServer.getInstance();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();

    }

}
