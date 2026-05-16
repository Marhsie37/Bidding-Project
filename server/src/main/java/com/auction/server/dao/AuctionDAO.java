package com.auction.server.dao;

import com.auction.shared.model.AuctionSession;
import com.auction.shared.model.BidTransaction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {
    private DatabaseConnection dbConnection;

    public AuctionDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public AuctionSession getAuctionSession(int productId) {
        String sql = "SELECT p.id, p.name, p.current_price, p.end_time, p.status, " +
                "u.id as winner_id, u.username as winner_name " +
                "FROM products p " +
                "LEFT JOIN users u ON p.winner_id = u.id " +
                "WHERE p.id = ?";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    AuctionSession session = new AuctionSession();
                    session.setProductId(rs.getInt("id"));
                    session.setProductName(rs.getString("name"));
                    session.setCurrentPrice(rs.getDouble("current_price"));

                    Timestamp endTime = rs.getTimestamp("end_time");
                    if (endTime != null) {
                        session.setEndTime(endTime.toLocalDateTime());
                    }

                    session.setStatus(rs.getString("status"));

                    int winnerId = rs.getInt("winner_id");
                    if (!rs.wasNull()) {
                        session.setCurrentWinnerId(winnerId);
                    }

                    session.setCurrentWinnerName(rs.getString("winner_name"));
                    return session;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting auction session: " + e.getMessage());
        }
        return null;
    }

    public boolean saveBid(BidTransaction bid) {
        String sql = "INSERT INTO bids (product_id, bidder_id, bid_amount, is_auto_bid) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, bid.getAuctionId());
            pstmt.setInt(2, bid.getBidderId());
            pstmt.setDouble(3, bid.getBidAmount());
            pstmt.setBoolean(4, bid.isAutoBid());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        bid.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error saving bid: " + e.getMessage());
        }
        return false;
    }

    public List<BidTransaction> getBidHistory(int productId, int limit) {
        List<BidTransaction> bids = new ArrayList<>();
        String sql = "SELECT b.*, u.username as bidder_name " +
                "FROM bids b LEFT JOIN users u ON b.bidder_id = u.id " +
                "WHERE b.product_id = ? ORDER BY b.bid_time DESC LIMIT ?";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BidTransaction bid = new BidTransaction();
                    bid.setId(rs.getInt("id"));
                    bid.setAuctionId(rs.getInt("product_id"));
                    bid.setBidderId(rs.getInt("bidder_id"));
                    bid.setBidderName(rs.getString("bidder_name"));
                    bid.setBidAmount(rs.getDouble("bid_amount"));

                    Timestamp bidTime = rs.getTimestamp("bid_time");
                    if (bidTime != null) {
                        bid.setBidTime(bidTime.toLocalDateTime());
                    }

                    bid.setAutoBid(rs.getBoolean("is_auto_bid"));
                    bids.add(bid);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting bid history: " + e.getMessage());
        }
        return bids;
    }

    public double getHighestBid(int productId) {
        String sql = "SELECT MAX(bid_amount) as max_bid FROM bids WHERE product_id = ?";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("max_bid");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting highest bid: " + e.getMessage());
        }
        return 0;
    }

    public int getHighestBidder(int productId) {
        String sql = "SELECT bidder_id FROM bids WHERE product_id = ? ORDER BY bid_amount DESC LIMIT 1";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("bidder_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting highest bidder: " + e.getMessage());
        }
        return 0;
    }

    public boolean endAuction(int productId, int winnerId, double finalPrice) {
        String sql = "UPDATE products SET status = 'ENDED', winner_id = ?, current_price = ? WHERE id = ?";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            if (winnerId > 0) {
                pstmt.setInt(1, winnerId);
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setDouble(2, finalPrice);
            pstmt.setInt(3, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error ending auction: " + e.getMessage());
            return false;
        }
    }

    public List<BidTransaction> getBidsAfterTime(int productId, LocalDateTime afterTime) {
        List<BidTransaction> bids = new ArrayList<>();
        String sql = "SELECT b.*, u.username as bidder_name " +
                "FROM bids b LEFT JOIN users u ON b.bidder_id = u.id " +
                "WHERE b.product_id = ? AND b.bid_time > ? ORDER BY b.bid_time ASC";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            pstmt.setTimestamp(2, Timestamp.valueOf(afterTime));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BidTransaction bid = new BidTransaction();
                    bid.setId(rs.getInt("id"));
                    bid.setAuctionId(rs.getInt("product_id"));
                    bid.setBidderId(rs.getInt("bidder_id"));
                    bid.setBidderName(rs.getString("bidder_name"));
                    bid.setBidAmount(rs.getDouble("bid_amount"));

                    Timestamp bidTime = rs.getTimestamp("bid_time");
                    if (bidTime != null) {
                        bid.setBidTime(bidTime.toLocalDateTime());
                    }

                    bid.setAutoBid(rs.getBoolean("is_auto_bid"));
                    bids.add(bid);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting bids after time: " + e.getMessage());
        }
        return bids;
    }
}