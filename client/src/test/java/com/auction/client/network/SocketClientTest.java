package com.auction.client.network;

import com.auction.server.AuctionServer;
import com.auction.shared.protocol.*;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SocketClientTest {

    private SocketClient socketClient;
    private static AuctionServer server;
    private static Thread serverThread;
    private static boolean serverStarted = false;

    @BeforeAll
    static void startServer() throws InterruptedException {
        if (!serverStarted) {
            server = AuctionServer.getInstance();
            serverThread = new Thread(() -> {
                try {
                    server.start();
                } catch (Exception e) {
                    System.err.println("Server error: " + e.getMessage());
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();

            // Đợi server khởi động lâu hơn
            Thread.sleep(5000);
            serverStarted = true;
            System.out.println("Server started for testing");
        }
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop();
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        // Reset singleton
        resetSocketClientSingleton();

        socketClient = SocketClient.getInstance();

        // Connect với retry
        int retries = 3;
        while (retries > 0 && !socketClient.isConnected()) {
            try {
                socketClient.connect();
                Thread.sleep(1000);
            } catch (Exception e) {
                retries--;
                if (retries == 0) throw e;
            }
        }
    }

    @AfterEach
    void tearDown() {
        try {
            socketClient.disconnect();
        } catch (Exception e) {
            // Ignore
        }
        resetSocketClientSingleton();
    }

    private void resetSocketClientSingleton() {
        try {
            java.lang.reflect.Field instanceField = SocketClient.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            // Ignore
        }
    }

    // ============= BASIC TESTS =============
    @Test
    void testGetInstance() {
        SocketClient instance1 = SocketClient.getInstance();
        SocketClient instance2 = SocketClient.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    void testSetAndGetAuthToken() {
        String token = "test-token-123";
        socketClient.setAuthToken(token);
        assertEquals(token, socketClient.getAuthToken());

        socketClient.setAuthToken(null);
        assertNull(socketClient.getAuthToken());
    }

    @Test
    void testSetHandlers() {
        assertDoesNotThrow(() -> {
            socketClient.setBidUpdateHandler(response -> {});
            socketClient.setAuctionEndHandler(response -> {});
            socketClient.setAuctionExtendedHandler(response -> {});
        });
    }

    @Test
    void testInitialState() {
        assertTrue(socketClient.isConnected());
    }

    @Test
    void testDisconnect() {
        assertDoesNotThrow(() -> socketClient.disconnect());
        assertFalse(socketClient.isConnected());
    }

    // ============= AUTH TESTS =============
    @Test
    void testRegisterAndLogin() throws InterruptedException {
        String username = "testuser_" + System.currentTimeMillis();
        String password = "test123";

        // Register
        CountDownLatch registerLatch = new CountDownLatch(1);
        AtomicReference<Response> registerResponse = new AtomicReference<>();
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("password", password);
        userData.put("email", username + "@test.com");
        userData.put("role", "USER");

        socketClient.register(userData, response -> {
            registerResponse.set(response);
            registerLatch.countDown();
        });

        boolean registered = registerLatch.await(10, TimeUnit.SECONDS);
        assertTrue(registered, "Register timeout");
        assertNotNull(registerResponse.get(), "Register response null");

        // Login
        CountDownLatch loginLatch = new CountDownLatch(1);
        AtomicReference<Response> loginResponse = new AtomicReference<>();

        socketClient.login(username, password, response -> {
            loginResponse.set(response);
            loginLatch.countDown();
        });

        boolean loggedIn = loginLatch.await(10, TimeUnit.SECONDS);
        assertTrue(loggedIn, "Login timeout");
        assertNotNull(loginResponse.get(), "Login response null");
        assertTrue(loginResponse.get().isSuccess(), "Login failed: " + loginResponse.get().getMessage());
    }

    @Test
    void testLoginWithWrongPassword() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Response> responseRef = new AtomicReference<>();

        socketClient.login("nonexistent_user_" + System.currentTimeMillis(), "wrongpassword", response -> {
            responseRef.set(response);
            latch.countDown();
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertNotNull(responseRef.get());
        assertFalse(responseRef.get().isSuccess(), "Login should fail with wrong credentials");
    }

    // ============= PRODUCT TESTS =============
    @Test
    void testGetProducts() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Response> responseRef = new AtomicReference<>();

        socketClient.getProducts(response -> {
            responseRef.set(response);
            latch.countDown();
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertNotNull(responseRef.get());
        assertTrue(responseRef.get().isSuccess(), "Get products failed: " + responseRef.get().getMessage());
    }

    @Test
    void testAddProduct() throws InterruptedException {
        // Create and login seller
        String seller = "seller_" + System.currentTimeMillis();
        boolean loggedIn = registerAndLogin(seller, "pass123");
        assertTrue(loggedIn, "Failed to register/login seller");

        // Add product
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Response> responseRef = new AtomicReference<>();
        Map<String, Object> productData = new HashMap<>();
        productData.put("name", "Test Product " + System.currentTimeMillis());
        productData.put("description", "Test Description");
        productData.put("startingPrice", 100.0);
        productData.put("reservePrice", 150.0);
        productData.put("durationHours", 24);

        socketClient.addProduct(productData, response -> {
            responseRef.set(response);
            latch.countDown();
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertNotNull(responseRef.get());
    }

    // ============= BID TESTS =============
    @Test
    void testPlaceBid() throws InterruptedException {
        // Create seller and product
        String seller = "seller_bid_" + System.currentTimeMillis();
        registerAndLogin(seller, "pass123");

        int productId = createProduct(seller, "Bid Product", 100.0);
        assertTrue(productId > 0, "Failed to create product");

        // Login as bidder
        String bidder = "bidder_" + System.currentTimeMillis();
        registerAndLogin(bidder, "pass123");

        // Place bid
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Response> responseRef = new AtomicReference<>();

        socketClient.placeBid(productId, 150.0, response -> {
            responseRef.set(response);
            latch.countDown();
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertNotNull(responseRef.get());
    }

    // ============= AUTO BID TESTS =============
    @Test
    void testSetAutoBid() throws InterruptedException {
        String seller = "seller_auto_" + System.currentTimeMillis();
        registerAndLogin(seller, "pass123");

        int productId = createProduct(seller, "Auto Bid Product", 100.0);
        assertTrue(productId > 0);

        String bidder = "bidder_auto_" + System.currentTimeMillis();
        registerAndLogin(bidder, "pass123");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Response> responseRef = new AtomicReference<>();

        socketClient.setAutoBid(productId, 500.0, 50.0, response -> {
            responseRef.set(response);
            latch.countDown();
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertNotNull(responseRef.get());
    }

    @Test
    void testRemoveAutoBid() throws InterruptedException {
        String seller = "seller_remove_" + System.currentTimeMillis();
        registerAndLogin(seller, "pass123");

        int productId = createProduct(seller, "Remove Auto Bid Product", 100.0);
        assertTrue(productId > 0);

        String bidder = "bidder_remove_" + System.currentTimeMillis();
        registerAndLogin(bidder, "pass123");

        // Set auto bid first
        CountDownLatch setLatch = new CountDownLatch(1);
        socketClient.setAutoBid(productId, 500.0, 50.0, response -> setLatch.countDown());
        assertTrue(setLatch.await(10, TimeUnit.SECONDS));

        // Remove auto bid
        CountDownLatch removeLatch = new CountDownLatch(1);
        AtomicReference<Response> responseRef = new AtomicReference<>();

        socketClient.removeAutoBid(productId, response -> {
            responseRef.set(response);
            removeLatch.countDown();
        });

        assertTrue(removeLatch.await(10, TimeUnit.SECONDS));
        assertNotNull(responseRef.get());
    }

    // ============= SUBSCRIBE TESTS =============
    @Test
    void testSubscribeAuction() throws InterruptedException {
        String seller = "seller_sub_" + System.currentTimeMillis();
        registerAndLogin(seller, "pass123");

        int productId = createProduct(seller, "Subscribe Product", 100.0);
        assertTrue(productId > 0);

        String subscriber = "subscriber_" + System.currentTimeMillis();
        registerAndLogin(subscriber, "pass123");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Response> responseRef = new AtomicReference<>();

        socketClient.subscribeAuction(productId, response -> {
            responseRef.set(response);
            latch.countDown();
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertNotNull(responseRef.get());
        assertTrue(responseRef.get().isSuccess());
    }

    @Test
    void testUnsubscribeAuction() throws InterruptedException {
        String seller = "seller_unsub_" + System.currentTimeMillis();
        registerAndLogin(seller, "pass123");

        int productId = createProduct(seller, "Unsubscribe Product", 100.0);
        assertTrue(productId > 0);

        String subscriber = "unsubscriber_" + System.currentTimeMillis();
        registerAndLogin(subscriber, "pass123");

        // Subscribe first
        CountDownLatch subLatch = new CountDownLatch(1);
        socketClient.subscribeAuction(productId, response -> subLatch.countDown());
        assertTrue(subLatch.await(10, TimeUnit.SECONDS));

        // Unsubscribe
        CountDownLatch unsubLatch = new CountDownLatch(1);
        AtomicReference<Response> responseRef = new AtomicReference<>();

        socketClient.unsubscribeAuction(productId, response -> {
            responseRef.set(response);
            unsubLatch.countDown();
        });

        assertTrue(unsubLatch.await(10, TimeUnit.SECONDS));
        assertNotNull(responseRef.get());
        assertTrue(responseRef.get().isSuccess());
    }

    // ============= HELPER METHODS =============
    private boolean registerAndLogin(String username, String password) throws InterruptedException {
        CountDownLatch registerLatch = new CountDownLatch(1);
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("password", password);
        userData.put("email", username + "@test.com");
        userData.put("role", "USER");

        AtomicReference<Boolean> success = new AtomicReference<>(false);
        socketClient.register(userData, response -> {
            success.set(response.isSuccess());
            registerLatch.countDown();
        });

        if (!registerLatch.await(10, TimeUnit.SECONDS)) return false;
        if (!success.get()) return false;

        CountDownLatch loginLatch = new CountDownLatch(1);
        socketClient.login(username, password, response -> {
            success.set(response.isSuccess());
            loginLatch.countDown();
        });

        return loginLatch.await(10, TimeUnit.SECONDS) && success.get();
    }

    private int createProduct(String seller, String name, double price) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Integer> productIdRef = new AtomicReference<>(-1);
        Map<String, Object> productData = new HashMap<>();
        productData.put("name", name);
        productData.put("description", "Test Description");
        productData.put("startingPrice", price);
        productData.put("reservePrice", price + 50);
        productData.put("durationHours", 24);

        socketClient.addProduct(productData, response -> {
            if (response.isSuccess() && response.getData() != null && response.getData().containsKey("productId")) {
                productIdRef.set((Integer) response.getData().get("productId"));
            }
            latch.countDown();
        });

        latch.await(10, TimeUnit.SECONDS);
        return productIdRef.get();
    }
}