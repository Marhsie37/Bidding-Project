package com.auction.shared.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ResponseTest {

    private Response successResponse;
    private Response errorResponse;
    private Response dataResponse;

    @BeforeEach
    void setUp() {
        successResponse = new Response(CommandType.SUCCESS, true, "Operation successful");
        errorResponse = new Response(CommandType.ERROR, false, "Operation failed");

        Map<String, Object> data = new HashMap<>();
        data.put("productId", 100);
        data.put("bidAmount", 250.0);
        dataResponse = new Response(CommandType.BID_UPDATE, true, "Bid placed", data);
    }

    @Test
    void testDefaultConstructor() {
        Response emptyResponse = new Response();
        assertNull(emptyResponse.getCommand());
        assertFalse(emptyResponse.isSuccess());
        assertNull(emptyResponse.getMessage());
        assertNull(emptyResponse.getData());
    }

    @Test
    void testConstructorWithCommandSuccessAndMessage() {
        assertEquals(CommandType.SUCCESS, successResponse.getCommand());
        assertTrue(successResponse.isSuccess());
        assertEquals("Operation successful", successResponse.getMessage());
        assertNull(successResponse.getData());
    }

    @Test
    void testConstructorWithAllFields() {
        assertEquals(CommandType.BID_UPDATE, dataResponse.getCommand());
        assertTrue(dataResponse.isSuccess());
        assertEquals("Bid placed", dataResponse.getMessage());
        assertNotNull(dataResponse.getData());
        assertEquals(100, dataResponse.getData().get("productId"));
        assertEquals(250.0, dataResponse.getData().get("bidAmount"));
    }

    @Test
    void testSettersAndGetters() {
        Response testResponse = new Response();

        testResponse.setCommand(CommandType.AUCTION_END);
        assertEquals(CommandType.AUCTION_END, testResponse.getCommand());

        testResponse.setSuccess(false);
        assertFalse(testResponse.isSuccess());

        testResponse.setMessage("Auction ended");
        assertEquals("Auction ended", testResponse.getMessage());

        Map<String, Object> newData = new HashMap<>();
        newData.put("winnerId", 42);
        newData.put("finalPrice", 1000.0);
        testResponse.setData(newData);
        assertEquals(newData, testResponse.getData());
    }

    @Test
    void testSerialization() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteOut);
        out.writeObject(dataResponse);
        out.close();

        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        ObjectInputStream in = new ObjectInputStream(byteIn);
        Response deserializedResponse = (Response) in.readObject();
        in.close();

        assertEquals(dataResponse.getCommand(), deserializedResponse.getCommand());
        assertEquals(dataResponse.isSuccess(), deserializedResponse.isSuccess());
        assertEquals(dataResponse.getMessage(), deserializedResponse.getMessage());
        assertEquals(dataResponse.getData(), deserializedResponse.getData());
    }
}