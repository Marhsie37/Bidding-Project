package com.auction.server.dao;

import com.auction.shared.model.BidTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {
    private DatabaseConnection dbConnection;
    private Connection conn;
    private static final Logger logger = LoggerFactory.getLogger(BidDAO.class);

    public BidDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public BidDAO(Connection conn) {
        this.conn = conn;
    }

    private Connection getConnection() throws SQLException {
        if (this.conn != null) {
            return this.conn;
        }
        return dbConnection.getConnection();
    }

    public boolean createBid(BidTransaction bid) {
        String sql = "INSERT INTO bids (product_id, bidder_id, bid_amount, is_auto_bid) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
            logger.error("Error creating bid: ", e);
        }
        return false;
    }

    public List<BidTransaction> getBidsByProduct(int productId) {
        List<BidTransaction> bids = new ArrayList<>();
        String sql = "SELECT b.*, u.username as bidder_name FROM bids b " +
                "LEFT JOIN users u ON b.bidder_id = u.id " +
                "WHERE b.product_id = ? ORDER BY b.bid_time DESC";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapResultSetToBid(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting bids by product: ", e);
        }
        return bids;
    }

    public List<BidTransaction> getBidsByUser(int userId) {
        List<BidTransaction> bids = new ArrayList<>();
        String sql = "SELECT b.*, u.username as bidder_name FROM bids b " +
                "LEFT JOIN users u ON b.bidder_id = u.id " +
                "WHERE b.bidder_id = ? ORDER BY b.bid_time DESC LIMIT 50";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapResultSetToBid(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting bids by user: ", e);
        }
        return bids;
    }

    public double getCurrentHighestBid(int productId) {
        String sql = "SELECT MAX(bid_amount) as max_bid FROM bids WHERE product_id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("max_bid");
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting current highest bid: ", e);
        }
        return 0;
    }

    public int getCurrentHighestBidder(int productId) {
        String sql = "SELECT bidder_id FROM bids WHERE product_id = ? ORDER BY bid_amount DESC LIMIT 1";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("bidder_id");
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting current highest bidder: ", e);
        }
        return 0;
    }

    private BidTransaction mapResultSetToBid(ResultSet rs) throws SQLException {
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
        return bid;
    }
}