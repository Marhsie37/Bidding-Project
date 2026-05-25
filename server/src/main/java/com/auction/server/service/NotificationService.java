package com.auction.server.service;

import com.auction.server.ClientHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationService {
  private static volatile NotificationService instance;
  private ConcurrentHashMap<Integer, CopyOnWriteArrayList<ClientHandler>> subscribers;

  private NotificationService() {
    this.subscribers = new ConcurrentHashMap<>();
  }

  private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

  public static NotificationService getInstance() {
    if (instance == null) {
      synchronized (NotificationService.class) {
        if (instance == null) {
          instance = new NotificationService();
        }
      }
    }
    return instance;
  }

  public void subscribe(int auctionId, String username, ClientHandler handler) {
    subscribers.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>())
            .add(handler); //ktra xem auctionId co trong map chua, neu chua co tao CopyOnWrite.. moi, neu da co tra ve danh sach hien tai
    logger.info("User " + username + " subscribed to auction " + auctionId);
  }

  public void unsubscribe(int auctionId, String username) {
    CopyOnWriteArrayList<ClientHandler> handlers = subscribers.get(auctionId);
    if (handlers != null) {
      handlers.removeIf(handler -> username.equals(handler.getUsername()));
    }
  }

  public void notifyBidUpdate(int auctionId, String bidderName, double bidAmount) {
    CopyOnWriteArrayList<ClientHandler> handlers = subscribers.get(auctionId);
    if (handlers != null) {
      for (ClientHandler handler : handlers) {
        handler.sendBidUpdate(auctionId, bidderName, bidAmount);
      }
    }
  }

  public void notifyAuctionEnd(int auctionId, int winnerId, String winnerName, double finalPrice) {
    CopyOnWriteArrayList<ClientHandler> handlers = subscribers.get(auctionId);
    if (handlers != null) {
      for (ClientHandler handler : handlers) {
        handler.sendAuctionEnd(auctionId, winnerId, winnerName, finalPrice);
      }

    } //clean up subscribers
    subscribers.remove(auctionId);
  }

  public void notifyAuctionExtended(int auctionId, LocalDateTime newEndTime) {
    CopyOnWriteArrayList<ClientHandler> handlers = subscribers.get(auctionId);
    if (handlers != null) {
      for (ClientHandler handler : handlers) {
        handler.sendAuctionExtended(auctionId, newEndTime);
      }
    }
  }


}
