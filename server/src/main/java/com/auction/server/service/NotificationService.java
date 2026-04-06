package com.auction.server.service;

import com.auction.server.ClientHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.logging.Handler;

public class NotificationService {
    private static NotificationService instance;
    private ConcurrentHashMap<Integer,CopyOnWriteArrayList<ClientHandler>> subscribers;
    private NotificationService(){
        this.subscribers = new ConcurrentHashMap<>();
    }
    public static NotificationService getInstance(){
        if (instance == null){
            instance = new NotificationService();
        }
        return instance;
    }
    public void subscribe(int auctionId, String username, ClientHandler handler) {
        subscribers.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>())
                .add(handler);
        System.out.println("User " + username + " subscribed to auction " + auctionId);
    }
    public void unsubscribe(int auctionId, String username){
        CopyOnWriteArrayList<ClientHandler> handlers = subscribers.get(auctionId);
        if (handlers != null){
            handlers.removeIf(handler -> username.equals(handler.getUsername()));
        }
    }
    public void notifyBidUpdate(int auctionId, String bidderName, double bidAmount){
        CopyOnWriteArrayList<ClientHandler> handlers = subscribers.get(auctionId);
        if (handlers != null){
            for (ClientHandler handler : handlers){
                handler.sendBidUpdate(auctionId,bidderName,bidAmount);
            }
        }
    }
    public void notifyAuctionEnd(int auctionId, int winnerId, String winnerName, double finalPrice){
        CopyOnWriteArrayList<ClientHandler> handlers = subscribers.get(auctionId);
        if (handlers != null){
            for (ClientHandler handler : handlers){
                handler.sendAuctionEnd(auctionId,winnerId,winnerName,finalPrice);
            }

        } //clean up subscribers
        subscribers.remove(auctionId);
    }


}
