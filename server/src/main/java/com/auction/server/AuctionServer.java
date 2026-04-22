package com.auction.server;

import com.auction.server.service.AuctionService;
import com.auction.server.service.AutoBidService;
import com.auction.shared.model.AuctionSession;



import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

public class AuctionServer {
    private static final int PORT = 8888;
    private static AuctionServer instance;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private ConcurrentHashMap<String,ClientHandler> clients;
    private AuctionService auctionService;
    private AutoBidService autoBidService;
    private boolean running;

    private AuctionServer(){
        this.threadPool = Executors.newCachedThreadPool();
        this.clients = new ConcurrentHashMap<>();
        this.auctionService = AuctionService.getInstance();
        this.autoBidService = AutoBidService.getInstance();
        this.running = true;

    }
    public static AuctionServer getInstance(){
        if (instance == null){
            instance = new AuctionServer();
        }
        return  instance;
    }
    public void start(){
        try{
            serverSocket = new ServerSocket(PORT);
            System.out.println("Auction Server started on port " + PORT);
            autoBidService.start();
            startAuctionMonitor();
            while (running){
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                threadPool.execute(handler);
            }
        } catch (IOException e){
            System.err.println("Server error: " + e.getMessage());

        }
    }
    private void startAuctionMonitor() {
        Thread monitor = new Thread(() -> {
            while (running) {
                try {
                    List<AuctionSession> activeAuctions = auctionService.getActiveAuctions();
                    for (AuctionSession auction : activeAuctions) {
                        if (LocalDateTime.now().isAfter(auction.getScheduledEndTime())) {
                            auctionService.endAuction(auction.getId());
                        }
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Monitor error: " + e.getMessage());
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
        System.out.println("Server stopped");
    }
    public void registerClient(String username, ClientHandler handler){
        clients.put(username,handler);
        System.out.println("Client registered: " + username);

    }
    public void unregisterClient(String username){
        clients.remove(username);
        System.out.println("Client unregistered: " + username);
    }
    public ClientHandler getClient(String username){
        return clients.get(username);
    }
    public ConcurrentHashMap<String,ClientHandler> getClients(){
        return clients;
    }
    public AuctionService getAuctionService() {
        return auctionService;
    }
    public static void main(String[] args){
        AuctionServer server = AuctionServer.getInstance();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();

    }


}
