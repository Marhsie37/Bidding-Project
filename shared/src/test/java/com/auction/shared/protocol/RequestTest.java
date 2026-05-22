package com.auction.shared.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RequestTest {

    private Request request;
    private Map<String, Object> testData;

    @BeforeEach
    void setUp() {
        testData = new HashMap<>();
        testData.put("username", "testUser");
        testData.put("password", "testPass");
        request = new Request(CommandType.LOGIN, testData);
    }

    @Test
    void testDefaultConstructor() {
        Request emptyRequest = new Request();
        assertNull(emptyRequest.getCommand());
        assertNull(emptyRequest.getData());
        assertNull(emptyRequest.getToken());
    }

    @Test
    void testConstructorWithCommandAndData() {
        assertEquals(CommandType.LOGIN, request.getCommand());
        assertEquals(testData, request.getData());
        assertNull(request.getToken());
    }

    @Test
    void testConstructorWithCommandDataAndToken() {
        String token = "test-token-123";
        Request authRequest = new Request(CommandType.GET_USER_INFO, testData, token);

        assertEquals(CommandType.GET_USER_INFO, authRequest.getCommand());
        assertEquals(testData, authRequest.getData());
        assertEquals(token, authRequest.getToken());
    }

    @Test
    void testSettersAndGetters() {
        Request testRequest = new Request();

        testRequest.setCommand(CommandType.PLACE_BID);
        assertEquals(CommandType.PLACE_BID, testRequest.getCommand());

        Map<String, Object> newData = new HashMap<>();
        newData.put("productId", 123);
        newData.put("bidAmount", 500.0);
        testRequest.setData(newData);
        assertEquals(newData, testRequest.getData());

        testRequest.setToken("new-token");
        assertEquals("new-token", testRequest.getToken());
    }

    @Test
    void testSerialization() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteOut);
        out.writeObject(request);
        out.close();

        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        ObjectInputStream in = new ObjectInputStream(byteIn);
        Request deserializedRequest = (Request) in.readObject();
        in.close();

        assertEquals(request.getCommand(), deserializedRequest.getCommand());
        assertEquals(request.getData(), deserializedRequest.getData());
        assertEquals(request.getToken(), deserializedRequest.getToken());
    }

    @Test
    void testSerialVersionUID() throws Exception {
        java.lang.reflect.Field field = Request.class.getDeclaredField("serialVersionUID");
        field.setAccessible(true);
        long actualValue = field.getLong(null);
        assertEquals(1L, actualValue, "Giá trị của serialVersionUID phải khớp cấu hình bằng 1L");
    }
}