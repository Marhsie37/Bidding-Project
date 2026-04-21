package com.auction.test.network;

import com.auction.shared.protocol.*;
import com.auction.server.ClientHandler;
import com.auction.server.service.NotificationService;

import org.junit.jupiter.api.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;


public class NetWorkTest {

    private static final int MOCK_PORT = 9999;
    private static ServerSocket mockServerSocket;
    private static Thread mockServerThread;

   //Mock server
    @BeforeAll
    static void startMockServer() throws IOException {
        mockServerSocket = new ServerSocket(MOCK_PORT);
        mockServerThread = new Thread(() -> {
            while (!mockServerSocket.isClosed()) {
                try {
                    Socket client = mockServerSocket.accept();
                    new Thread(() -> handleMockClient(client)).start();
                } catch (IOException e) {
                    if (!mockServerSocket.isClosed()) {
                        System.err.println("[MockServer] accept error: " + e.getMessage());
                    }
                }
            }
        });
        mockServerThread.setDaemon(true);
        mockServerThread.start();
        System.out.println("[MockServer] Started on port " + MOCK_PORT);
    }

    private static void handleMockClient(Socket client) {
        try (
                ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                ObjectInputStream  in  = new ObjectInputStream(client.getInputStream())
        ) {
            while (!client.isClosed()) {
                Object obj = in.readObject();
                if (obj instanceof Request) {
                    Response res = buildMockResponse((Request) obj);
                    out.writeObject(res);
                    out.flush();
                }
            }
        } catch (EOFException ignored) {
        } catch (Exception e) {
            System.err.println("[MockServer] client error: " + e.getMessage());
        }
    }

    private static Response buildMockResponse(Request req) {
        Map<String, Object> data = new HashMap<>();
        switch (req.getCommand()) {
            case LOGIN:
                data.put("user", "testUser");
                data.put("role", "USER");
                return new Response(CommandType.LOGIN, true, "Login successful", data);
            case PLACE_BID:
                data.put("productId", req.getData().get("productId"));
                data.put("bidAmount", req.getData().get("bidAmount"));
                data.put("bidderName", "testBidder");
                return new Response(CommandType.PLACE_BID, true, "Bid placed", data);
            case GET_PRODUCTS:
                data.put("products", Collections.emptyList());
                return new Response(CommandType.GET_PRODUCTS, true, "Success", data);
            case SUBSCRIBE_AUCTION:
                return new Response(CommandType.SUBSCRIBE_AUCTION, true, "Subscribed", null);
            case LOGOUT:
                return new Response(CommandType.LOGOUT, true, "Logged out", null);
            default:
                return new Response(req.getCommand(), true, "OK", null);
        }
    }

    @AfterAll
    static void stopMockServer() throws IOException {
        if (mockServerSocket != null && !mockServerSocket.isClosed()) {
            mockServerSocket.close();
        }
        System.out.println("[MockServer] Stopped");
    }

    private Socket connectToMock() throws IOException {
        return new Socket("localhost", MOCK_PORT);
    }

    // =========================================================
    //  TEST GROUP 1: Kết nối Socket
    // =========================================================

    @Test
    @DisplayName("TC-01: Client kết nối được server")
    void testClientCanConnectToServer() throws Exception {
        try (Socket socket = connectToMock()) {
            assertTrue(socket.isConnected(), "Socket phải được kết nối");
            assertFalse(socket.isClosed(), "Socket không được đóng");
            assertEquals(MOCK_PORT, socket.getPort(), "Phải kết nối đúng port");
        }
    }

    @Test
    @DisplayName("TC-02: Kết nối đến port sai phải thất bại")
    void testConnectionToWrongPortFails() {
        assertThrows(IOException.class, () -> new Socket("localhost", 9998));
    }

    @Test
    @DisplayName("TC-03: Kết nối đến host không tồn tại phải thất bại")
    void testConnectionToUnknownHostFails() {
        assertThrows(UnknownHostException.class,
                () -> new Socket("this.host.does.not.exist.xyz", MOCK_PORT));
    }

    // =========================================================
    //  TEST GROUP 2: Nhiều client cùng lúc
    // =========================================================

    @Test
    @DisplayName("TC-04: 10 client kết nối đồng thời không lỗi")
    void testMultipleClientsConnect() throws InterruptedException {
        int clientCount = 10;
        CountDownLatch connected   = new CountDownLatch(clientCount);
        CountDownLatch done        = new CountDownLatch(clientCount);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Socket>  sockets      = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < clientCount; i++) {
            new Thread(() -> {
                try {
                    Socket s = connectToMock();
                    sockets.add(s);
                    successCount.incrementAndGet();
                    connected.countDown();
                    connected.await(3, TimeUnit.SECONDS);
                } catch (Exception e) {
                    System.err.println("[TC-04] " + e.getMessage());
                } finally {
                    done.countDown();
                }
            }).start();
        }

        boolean allDone = done.await(5, TimeUnit.SECONDS);
        for (Socket s : sockets) {
            try { s.close(); } catch (IOException ignored) {}
        }

        assertTrue(allDone, "Tất cả thread phải hoàn thành");
        assertEquals(clientCount, successCount.get());
    }

    @Test
    @DisplayName("TC-05: 20 client gửi request đồng thời không deadlock")
    void testTwentyClientsConcurrentRequests() throws InterruptedException {
        int clientCount = 20;
        CountDownLatch startGun    = new CountDownLatch(1);
        CountDownLatch done        = new CountDownLatch(clientCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < clientCount; i++) {
            new Thread(() -> {
                try (Socket s = connectToMock()) {
                    ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                    ObjectInputStream  in  = new ObjectInputStream(s.getInputStream());
                    startGun.await();
                    out.writeObject(new Request(CommandType.GET_PRODUCTS, new HashMap<>()));
                    out.flush();
                    Object res = in.readObject();
                    if (res instanceof Response && ((Response) res).isSuccess()) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("[TC-05] " + e.getMessage());
                } finally {
                    done.countDown();
                }
            }).start();
        }

        startGun.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "Phải hoàn thành trong 10 giây");
        assertEquals(clientCount, successCount.get());
    }

    // =========================================================
    //  TEST GROUP 3: Request / Response
    // =========================================================

    @Test
    @DisplayName("TC-06: LOGIN request nhận response đúng")
    void testLoginRequestResponse() throws Exception {
        try (Socket s = connectToMock()) {
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            ObjectInputStream  in  = new ObjectInputStream(s.getInputStream());

            Map<String, Object> data = new HashMap<>();
            data.put("username", "testUser");
            data.put("password", "testPass");

            out.writeObject(new Request(CommandType.LOGIN, data));
            out.flush();

            Response res = (Response) in.readObject();
            assertNotNull(res);
            assertEquals(CommandType.LOGIN, res.getCommand());
            assertTrue(res.isSuccess());
        }
    }

    @Test
    @DisplayName("TC-07: PLACE_BID request nhận response đúng productId và bidAmount")
    void testPlaceBidRequestResponse() throws Exception {
        try (Socket s = connectToMock()) {
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            ObjectInputStream  in  = new ObjectInputStream(s.getInputStream());

            int    productId = 42;
            double bidAmount = 1500.0;

            Map<String, Object> data = new HashMap<>();
            data.put("productId", productId);
            data.put("bidAmount", bidAmount);

            out.writeObject(new Request(CommandType.PLACE_BID, data));
            out.flush();

            Response res = (Response) in.readObject();
            assertNotNull(res);
            assertEquals(CommandType.PLACE_BID, res.getCommand());
            assertTrue(res.isSuccess());
            assertEquals(productId, ((Number) res.getData().get("productId")).intValue());
            assertEquals(bidAmount, ((Number) res.getData().get("bidAmount")).doubleValue(), 0.001);
        }
    }

    @Test
    @DisplayName("TC-08: Nhiều request liên tiếp trên cùng kết nối")
    void testMultipleRequestsOnSameConnection() throws Exception {
        try (Socket s = connectToMock()) {
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            ObjectInputStream  in  = new ObjectInputStream(s.getInputStream());

            CommandType[] commands = {
                    CommandType.GET_PRODUCTS,
                    CommandType.SUBSCRIBE_AUCTION,
                    CommandType.LOGOUT
            };

            for (CommandType cmd : commands) {
                Map<String, Object> data = new HashMap<>();
                if (cmd == CommandType.SUBSCRIBE_AUCTION) data.put("productId", 1);
                out.writeObject(new Request(cmd, data));
                out.flush();
                Response res = (Response) in.readObject();
                assertEquals(cmd, res.getCommand());
                assertTrue(res.isSuccess());
            }
        }
    }

    // =========================================================
    //  TEST GROUP 4: Concurrent Bidding
    // =========================================================

    @Test
    @DisplayName("TC-09: 10 thread đồng thời đặt bid - không race condition")
    void testConcurrentBidding() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch startGun     = new CountDownLatch(1);
        CountDownLatch done         = new CountDownLatch(threadCount);
        AtomicInteger  successCount = new AtomicInteger(0);
        AtomicInteger  errorCount   = new AtomicInteger(0);
        Random         random       = new Random();

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try (Socket s = connectToMock()) {
                    ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                    ObjectInputStream  in  = new ObjectInputStream(s.getInputStream());
                    startGun.await();

                    double bid = 1000 + random.nextInt(1000);
                    Map<String, Object> data = new HashMap<>();
                    data.put("productId", 1);
                    data.put("bidAmount", bid);

                    out.writeObject(new Request(CommandType.PLACE_BID, data));
                    out.flush();

                    Response res = (Response) in.readObject();
                    if (res.isSuccess()) successCount.incrementAndGet();
                    else errorCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            }).start();
        }

        startGun.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        assertEquals(threadCount, successCount.get() + errorCount.get());
        System.out.printf("[TC-09] %d success, %d error%n", successCount.get(), errorCount.get());
    }

    @Test
    @DisplayName("TC-10: AtomicInteger không bị race condition với 50 thread")
    void testAtomicBidCounterNoRaceCondition() throws InterruptedException {
        int threadCount = 50;
        AtomicInteger  counter = new AtomicInteger(0);
        CountDownLatch latch   = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                counter.incrementAndGet();
                latch.countDown();
            }).start();
        }

        latch.await(5, TimeUnit.SECONDS);
        assertEquals(threadCount, counter.get());
    }

    @Test
    @DisplayName("TC-11: ConcurrentHashMap không ConcurrentModificationException")
    void testConcurrentHashMapThreadSafe() throws InterruptedException {
        ConcurrentHashMap<String, String> clients = new ConcurrentHashMap<>();
        int threadCount = 20;
        CountDownLatch done = new CountDownLatch(threadCount * 2);

        for (int i = 0; i < threadCount; i++) {
            final String key = "user" + i;
            new Thread(() -> {
                clients.put(key, "handler-" + key);
                try { Thread.sleep(5); } catch (InterruptedException ignored) {}
                clients.remove(key);
                done.countDown();
            }).start();
        }

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                clients.forEach((k, v) -> assertNotNull(v));
                done.countDown();
            }).start();
        }

        assertTrue(done.await(10, TimeUnit.SECONDS));
    }

    // =========================================================
    //  TEST GROUP 5: Realtime Update
    // =========================================================

    @Test
    @DisplayName("TC-12: Bid mới đến được subscriber qua BlockingQueue")
    void testRealtimeBidUpdateReachesSubscriber() throws InterruptedException {
        BlockingQueue<String> updateQueue = new LinkedBlockingQueue<>();

        new Thread(() -> {
            try {
                Thread.sleep(100);
                updateQueue.put("BID_UPDATE:product=1:amount=2000.0");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        String update = updateQueue.poll(2, TimeUnit.SECONDS);
        assertNotNull(update, "Subscriber phải nhận được BID_UPDATE trong 2 giây");
        assertTrue(update.startsWith("BID_UPDATE"));
        assertTrue(update.contains("product=1"));
        assertTrue(update.contains("amount=2000.0"));
    }

    @Test
    @DisplayName("TC-13: sendBidUpdate ghi đúng data qua ObjectStream")
    void testSendBidUpdateWritesCorrectData() throws Exception {
        PipedOutputStream pipeOut = new PipedOutputStream();
        PipedInputStream  pipeIn  = new PipedInputStream(pipeOut);
        ObjectOutputStream oos    = new ObjectOutputStream(pipeOut);
        ObjectInputStream  ois    = new ObjectInputStream(pipeIn);

        int    productId = 10;
        String bidder    = "alice";
        double bidAmount = 3500.0;

        Map<String, Object> data = new HashMap<>();
        data.put("productId",  productId);
        data.put("bidderName", bidder);
        data.put("bidAmount",  bidAmount);
        data.put("timestamp",  java.time.LocalDateTime.now().toString());

        oos.writeObject(new Response(CommandType.BID_UPDATE, true, "New bid placed", data));
        oos.flush();

        Response received = (Response) ois.readObject();
        assertEquals(CommandType.BID_UPDATE, received.getCommand());
        assertTrue(received.isSuccess());
        assertEquals(productId, ((Number) received.getData().get("productId")).intValue());
        assertEquals(bidder,    received.getData().get("bidderName"));
        assertEquals(bidAmount, ((Number) received.getData().get("bidAmount")).doubleValue(), 0.001);

        oos.close();
        ois.close();
    }

    // =========================================================
    //  TEST GROUP 6: Observer Pattern
    // =========================================================

    @Test
    @DisplayName("TC-14: Subscribe thêm handler - notifyBidUpdate không crash")
    void testSubscribeAddsHandlerToList() {
        NotificationService service = NotificationService.getInstance();
        int auctionId = 100;
        ClientHandler mockHandler = createMockClientHandler("userA");
        service.subscribe(auctionId, "userA", mockHandler);
        assertDoesNotThrow(() -> service.notifyBidUpdate(auctionId, "userB", 500.0));
        service.unsubscribe(auctionId, "userA");
    }

    @Test
    @DisplayName("TC-15: Unsubscribe - handler không nhận notification sau khi hủy")
    void testUnsubscribeRemovesHandler() {
        NotificationService service = NotificationService.getInstance();
        int auctionId = 200;
        AtomicInteger notifyCount = new AtomicInteger(0);

        ClientHandler mockHandler = new ClientHandler(null) {
            @Override public void sendBidUpdate(int pid, String bidder, double amount) {
                notifyCount.incrementAndGet();
            }
        };

        service.subscribe(auctionId, "userB", mockHandler);
        service.notifyBidUpdate(auctionId, "someone", 100.0); // nhận được

        service.unsubscribe(auctionId, "userB");
        service.notifyBidUpdate(auctionId, "someone", 200.0); // không nhận nữa

        assertEquals(1, notifyCount.get());
    }

    @Test
    @DisplayName("TC-16: Tất cả subscriber nhận được notification")
    void testMultipleSubscribersAllReceiveNotification() throws InterruptedException {
        NotificationService service = NotificationService.getInstance();
        int auctionId = 300;
        int subscriberCount = 5;
        AtomicInteger totalNotified = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(subscriberCount);

        for (int i = 0; i < subscriberCount; i++) {
            final String user = "user_" + i;
            ClientHandler handler = new ClientHandler(null) {
                @Override public void sendBidUpdate(int pid, String bidder, double amount) {
                    totalNotified.incrementAndGet();
                    latch.countDown();
                }
            };
            service.subscribe(auctionId, user, handler);
        }

        service.notifyBidUpdate(auctionId, "bigBidder", 9999.0);
        assertTrue(latch.await(3, TimeUnit.SECONDS), "Tất cả subscriber phải nhận notification");
        assertEquals(subscriberCount, totalNotified.get());

        for (int i = 0; i < subscriberCount; i++) service.unsubscribe(auctionId, "user_" + i);
    }

    @Test
    @DisplayName("TC-17: Subscribers bị xóa sau AUCTION_END")
    void testSubscribersRemovedAfterAuctionEnd() {
        NotificationService service = NotificationService.getInstance();
        int auctionId = 400;
        AtomicInteger notifyCount = new AtomicInteger(0);

        ClientHandler handler = new ClientHandler(null) {
            @Override public void sendBidUpdate(int pid, String bidder, double amount) {
                notifyCount.incrementAndGet();
            }
            @Override public void sendAuctionEnd(int pid, int wId, String wName, double price) {}
        };

        service.subscribe(auctionId, "buyer", handler);
        service.notifyAuctionEnd(auctionId, 1, "buyer", 5000.0); // xóa subscriber
        service.notifyBidUpdate(auctionId, "late", 6000.0);      // không ai nhận

        assertEquals(0, notifyCount.get());
    }

    // =========================================================
    //  TEST GROUP 7: Thread Safety
    // =========================================================

    @Test
    @DisplayName("TC-18: ConcurrentHashMap handlers thread-safe khi nhiều thread ghi cùng lúc")
    void testResponseHandlersConcurrentAccess() throws InterruptedException {
        ConcurrentHashMap<CommandType, Consumer<Response>> handlers = new ConcurrentHashMap<>();
        int threadCount = 20;
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger putCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                handlers.put(CommandType.PLACE_BID, response -> {});
                putCount.incrementAndGet();
                done.countDown();
            }).start();
        }

        done.await(5, TimeUnit.SECONDS);
        assertEquals(threadCount, putCount.get());
        assertTrue(handlers.containsKey(CommandType.PLACE_BID));
    }

    @Test
    @DisplayName("TC-19: Listener thread thoát khi stream đóng")
    void testListenerThreadExitsOnSocketClose() throws Exception {
        PipedOutputStream serverOut = new PipedOutputStream();
        PipedInputStream  clientIn  = new PipedInputStream(serverOut);
        ObjectOutputStream serverOos = new ObjectOutputStream(serverOut);
        ObjectInputStream  clientOis = new ObjectInputStream(clientIn);

        AtomicBoolean listenerExited = new AtomicBoolean(false);
        Thread listener = new Thread(() -> {
            try { clientOis.readObject(); } catch (Exception ignored) {}
            finally { listenerExited.set(true); }
        });
        listener.setDaemon(true);
        listener.start();

        Thread.sleep(100);
        serverOos.close();
        listener.join(2000);
        assertTrue(listenerExited.get());
    }

    // =========================================================
    //  TEST GROUP 8: Serialization
    // =========================================================

    @Test
    @DisplayName("TC-20: Request serialize/deserialize đúng")
    void testRequestSerialization() throws Exception {
        Map<String, Object> reqData = new HashMap<>();
        reqData.put("productId", 7);
        reqData.put("bidAmount", 250.0);
        Request original = new Request(CommandType.PLACE_BID, reqData, "token-abc");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ObjectOutputStream(baos).writeObject(original);
        Request copy = (Request) new ObjectInputStream(
                new ByteArrayInputStream(baos.toByteArray())).readObject();

        assertEquals(CommandType.PLACE_BID, copy.getCommand());
        assertEquals(7, ((Number) copy.getData().get("productId")).intValue());
        assertEquals(250.0, ((Number) copy.getData().get("bidAmount")).doubleValue(), 0.001);
        assertEquals("token-abc", copy.getToken());
    }

    @Test
    @DisplayName("TC-21: Response serialize/deserialize đúng")
    void testResponseSerialization() throws Exception {
        Map<String, Object> resData = new HashMap<>();
        resData.put("winnerId", 3);
        resData.put("finalPrice", 9999.99);
        Response original = new Response(CommandType.AUCTION_END, true, "Auction ended", resData);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ObjectOutputStream(baos).writeObject(original);
        Response copy = (Response) new ObjectInputStream(
                new ByteArrayInputStream(baos.toByteArray())).readObject();

        assertEquals(CommandType.AUCTION_END, copy.getCommand());
        assertTrue(copy.isSuccess());
        assertEquals("Auction ended", copy.getMessage());
        assertEquals(9999.99, ((Number) copy.getData().get("finalPrice")).doubleValue(), 0.001);
    }

    // =========================================================
    //  UTILITY
    // =========================================================

    private ClientHandler createMockClientHandler(String username) {
        return new ClientHandler(null) {
            @Override public String getUsername() { return username; }
            @Override public void sendBidUpdate(int pid, String b, double a) {}
            @Override public void sendAuctionEnd(int pid, int wId, String wName, double p) {}
        };
    }
}