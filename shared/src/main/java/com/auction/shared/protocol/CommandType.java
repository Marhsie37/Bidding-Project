package com.auction.shared.protocol;

public enum CommandType {
    // Auth commands
    LOGIN,
    REGISTER,
    LOGOUT,

    // User commands
    GET_USER_INFO,
    UPDATE_USER,

    // Product commands
    GET_PRODUCTS,
    GET_PRODUCT_DETAILS,
    ADD_PRODUCT,
    UPDATE_PRODUCT,
    DELETE_PRODUCT,
    GET_MY_PRODUCTS,

    // Auction commands
    PLACE_BID,
    GET_AUCTION_DETAILS,
    GET_AUCTION_HISTORY,
    SUBSCRIBE_AUCTION,
    UNSUBSCRIBE_AUCTION,
    SET_AUTO_BID,
    REMOVE_AUTO_BID,

    // Admin commands
    ADMIN_GET_ALL_USERS,
    ADMIN_UPDATE_USER,
    ADMIN_DELETE_USER,
    ADMIN_GET_ALL_PRODUCTS,
    ADMIN_DELETE_PRODUCT,
    ADMIN_GET_ALL_AUCTIONS,

    // Response
    SUCCESS,
    ERROR,
    BID_UPDATE,
    AUCTION_END,
    AUCTION_EXTENDED,

    ADD_FUNDS,        // nap tien vao tai khoan
    PROCESS_PAYMENT,  // xu ly thanh toan
    GET_USER_BALANCE  // lay so du

}
