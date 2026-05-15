package com.auction.shared.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommandTypeTest {

    @Test
    void testCommandTypeValues() {
        CommandType[] values = CommandType.values();
        assertNotNull(values);
        assertTrue(values.length > 0);
    }

    @Test
    void testAuthCommandsExist() {
        assertNotNull(CommandType.valueOf("LOGIN"));
        assertNotNull(CommandType.valueOf("REGISTER"));
        assertNotNull(CommandType.valueOf("LOGOUT"));
    }

    @Test
    void testUserCommandsExist() {
        assertNotNull(CommandType.valueOf("GET_USER_INFO"));
        assertNotNull(CommandType.valueOf("UPDATE_USER"));
    }

    @Test
    void testProductCommandsExist() {
        assertNotNull(CommandType.valueOf("GET_PRODUCTS"));
        assertNotNull(CommandType.valueOf("GET_PRODUCT_DETAILS"));
        assertNotNull(CommandType.valueOf("ADD_PRODUCT"));
        assertNotNull(CommandType.valueOf("UPDATE_PRODUCT"));
        assertNotNull(CommandType.valueOf("DELETE_PRODUCT"));
        assertNotNull(CommandType.valueOf("GET_MY_PRODUCTS"));
    }

    @Test
    void testAuctionCommandsExist() {
        assertNotNull(CommandType.valueOf("PLACE_BID"));
        assertNotNull(CommandType.valueOf("GET_AUCTION_DETAILS"));
        assertNotNull(CommandType.valueOf("GET_AUCTION_HISTORY"));
        assertNotNull(CommandType.valueOf("SUBSCRIBE_AUCTION"));
        assertNotNull(CommandType.valueOf("UNSUBSCRIBE_AUCTION"));
        assertNotNull(CommandType.valueOf("SET_AUTO_BID"));
        assertNotNull(CommandType.valueOf("REMOVE_AUTO_BID"));
    }

    @Test
    void testAdminCommandsExist() {
        assertNotNull(CommandType.valueOf("ADMIN_GET_ALL_USERS"));
        assertNotNull(CommandType.valueOf("ADMIN_UPDATE_USER"));
        assertNotNull(CommandType.valueOf("ADMIN_DELETE_USER"));
        assertNotNull(CommandType.valueOf("ADMIN_GET_ALL_PRODUCTS"));
        assertNotNull(CommandType.valueOf("ADMIN_DELETE_PRODUCT"));
        assertNotNull(CommandType.valueOf("ADMIN_GET_ALL_AUCTIONS"));
    }

    @Test
    void testResponseCommandsExist() {
        assertNotNull(CommandType.valueOf("SUCCESS"));
        assertNotNull(CommandType.valueOf("ERROR"));
        assertNotNull(CommandType.valueOf("BID_UPDATE"));
        assertNotNull(CommandType.valueOf("AUCTION_END"));
        assertNotNull(CommandType.valueOf("AUCTION_EXTENDED"));
    }

    @Test
    void testPaymentCommandsExist() {
        assertNotNull(CommandType.valueOf("ADD_FUNDS"));
        assertNotNull(CommandType.valueOf("PROCESS_PAYMENT"));
        assertNotNull(CommandType.valueOf("GET_USER_BALANCE"));
    }
}