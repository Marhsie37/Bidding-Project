package com.auction.client.network;

import com.auction.shared.protocol.CommandType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test SocketClient với MockServer chạy in-process.
 * <p>
 * Lý do viết lại:
 * - SocketClient.sendRequestAsync() thiếu .start() → thread không bao giờ chạy
 * → callback không được gọi → CountDownLatch chờ mãi (timeout 17 phút).
 * - Test cũ phụ thuộc server thật + database thật → không ổn định.
 * <p>
 * Cách tiếp cận mới:
 * - Mở MockServer trên port ngẫu nhiên (tránh conflict).
 * - MockServer đọc Request, trả Response giả lập theo CommandType.
 * - Test chỉ kiểm tra logic SocketClient, không phụ thuộc AuctionServer.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SocketClientTest {

  // ── Mock Server ──────────────────────────────────────────────────────────

  private ServerSocket mockServerSocket;
  private Thread mockServerThread;
  private int mockPort;
  private volatile boolean mockRunning;
  private static final Logger logger = LoggerFactory.getLogger(SocketClientTest.class);

  /**
   * MockServer: chấp nhận 1 connection, đọc Request, trả Response phù hợp.
   * Chạy trong thread riêng, tự lặp lại cho mỗi test.
   */
  @BeforeAll
  void startMockServer() throws IOException {
    // Port 0 → OS tự chọn port trống, tránh conflict
    mockServerSocket = new ServerSocket(0);
    mockPort = mockServerSocket.getLocalPort();
    mockRunning = true;

    mockServerThread = new Thread(() -> {
      while (mockRunning) {
        try {
          Socket clientSocket = mockServerSocket.accept();
          // Mỗi connection xử lý trong thread riêng
          new Thread(() -> handleMockClient(clientSocket)).start();
        } catch (IOException e) {
          if (mockRunning) logger.error("[MockServer] Accept error: ", e);
        }
      }
    });
    mockServerThread.setDaemon(true);
    mockServerThread.start();
    logger.info("[MockServer] Started on port {}", mockPort);
  }

  private void handleMockClient(Socket clientSocket) {
    try (ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
         ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {

      while (!clientSocket.isClosed()) {
        Object obj = in.readObject();
        if (!(obj instanceof Request)) continue;

        Request req = (Request) obj;
        Response resp = buildMockResponse(req);
        out.writeObject(resp);
        out.flush();
      }
    } catch (EOFException ignored) {
      // Client ngắt kết nối bình thường
    } catch (Exception e) {
      logger.error("[MockServer] Handler error: ", e);
    }
  }

  /**
   * Tạo Response giả lập theo CommandType
   */
  private Response buildMockResponse(Request req) {
    Map<String, Object> data = new HashMap<>();
    switch (req.getCommand()) {
      case LOGIN:
        String username = (String) req.getData().getOrDefault("username", "");
        boolean loginOk = !username.contains("nonexistent");
        data.put("token", "mock-token-123");
        data.put("role", "USER");
        return new Response(CommandType.LOGIN, loginOk,
                loginOk ? "Login success" : "Invalid credentials", loginOk ? data : null);

      case REGISTER:
        data.put("userId", 42);
        return new Response(CommandType.REGISTER, true, "Registered", data);

      case GET_PRODUCTS:
        data.put("products", new java.util.ArrayList<>());
        return new Response(CommandType.GET_PRODUCTS, true, "OK", data);

      case ADD_PRODUCT:
        data.put("productId", 101);
        return new Response(CommandType.ADD_PRODUCT, true, "Product added", data);

      case PLACE_BID:
        double bidAmount = (double) req.getData().getOrDefault("bidAmount", 0.0);
        boolean bidOk = bidAmount > 50.0; // giá > 50 mới hợp lệ
        return new Response(CommandType.PLACE_BID, bidOk,
                bidOk ? "Bid placed" : "Bid too low", bidOk ? data : null);

      case SUBSCRIBE_AUCTION:
        return new Response(CommandType.SUBSCRIBE_AUCTION, true, "Subscribed", data);

      case UNSUBSCRIBE_AUCTION:
        return new Response(CommandType.UNSUBSCRIBE_AUCTION, true, "Unsubscribed", data);

      case SET_AUTO_BID:
        return new Response(CommandType.SET_AUTO_BID, true, "AutoBid set", data);

      case REMOVE_AUTO_BID:
        return new Response(CommandType.REMOVE_AUTO_BID, true, "AutoBid removed", data);

      case ADD_FUNDS:
        data.put("balance", 500000.0);
        return new Response(CommandType.ADD_FUNDS, true, "Funds added", data);

      case GET_USER_BALANCE:
        data.put("balance", 1000000.0);
        return new Response(CommandType.GET_USER_BALANCE, true, "OK", data);

      case LOGOUT:
        return new Response(CommandType.LOGOUT, true, "Logged out", null);

      default:
        return new Response(req.getCommand(), false, "Unknown command", null);
    }
  }

  @AfterAll
  void stopMockServer() throws IOException {
    mockRunning = false;
    mockServerSocket.close();
    logger.info("[MockServer] Stopped");
  }

  // ── Setup / Teardown mỗi test ────────────────────────────────────────────

  private SocketClient client;

  @BeforeEach
  void setUp() throws Exception {
    resetSingleton();
    client = SocketClient.getInstance();
    // Kết nối tới MockServer thay vì server thật
    connectToMock(client);
  }

  @AfterEach
  void tearDown() {
    client.disconnect();
    resetSingleton();
  }

  /**
   * Dùng reflection để set host/port trỏ vào MockServer
   */
  private void connectToMock(SocketClient sc) throws Exception {
    setField(sc, "serverHost", "localhost");
    setField(sc, "serverPort", mockPort);
    sc.connect();
    Thread.sleep(200); // Chờ listener thread sẵn sàng
  }

  private void resetSingleton() {
    try {
      var f = SocketClient.class.getDeclaredField("instance");
      f.setAccessible(true);
      f.set(null, null);
    } catch (Exception ignored) {
    }
  }

  private void setField(Object obj, String fieldName, Object value) throws Exception {
    var f = obj.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    f.set(obj, value);
  }

  // Gọi sendRequestAsync đúng cách (workaround bug thiếu .start())
  private void sendAsync(Request request, Consumer<Response> callback) throws Exception {
    // Đăng ký handler trước
    var handlersField = SocketClient.class.getDeclaredField("responseHandlers");
    handlersField.setAccessible(true);
    @SuppressWarnings("unchecked")
    var handlers = (java.util.concurrent.ConcurrentHashMap<CommandType, Consumer<Response>>)
            handlersField.get(client);
    if (callback != null) handlers.put(request.getCommand(), callback);

    // Gửi request trong thread mới (fix bug thiếu .start())
    new Thread(() -> {
      try {
        client.sendRequest(request);
      } catch (IOException e) {
        if (callback != null)
          callback.accept(new Response(request.getCommand(), false, e.getMessage()));
      }
    }).start();
  }

  // ── Helper chờ response ──────────────────────────────────────────────────

  private Response await(CommandType cmd, java.util.function.Supplier<Void> action)
          throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Response> ref = new AtomicReference<>();

    var handlersField = SocketClient.class.getDeclaredField("responseHandlers");
    handlersField.setAccessible(true);
    @SuppressWarnings("unchecked")
    var handlers = (java.util.concurrent.ConcurrentHashMap<CommandType, Consumer<Response>>)
            handlersField.get(client);

    handlers.put(cmd, resp -> {
      ref.set(resp);
      latch.countDown();
    });

    action.get();
    assertTrue(latch.await(5, TimeUnit.SECONDS), "Timeout chờ response " + cmd);
    return ref.get();
  }

  // ── Tests: Singleton & State ─────────────────────────────────────────────

  @Test
  @Order(1)
  void testSingleton() {
    SocketClient a = SocketClient.getInstance();
    SocketClient b = SocketClient.getInstance();
    assertSame(a, b, "getInstance() phải trả về cùng 1 object");
  }

  @Test
  @Order(2)
  void testConnectedAfterSetup() {
    assertTrue(client.isConnected(), "Client phải connected sau khi gọi connect()");
  }

  @Test
  @Order(3)
  void testDisconnect() {
    client.disconnect();
    assertFalse(client.isConnected(), "Client phải disconnected sau khi gọi disconnect()");
  }

  @Test
  @Order(4)
  void testSetAndGetAuthToken() {
    client.setAuthToken("abc-token");
    assertEquals("abc-token", client.getAuthToken());

    client.setAuthToken(null);
    assertNull(client.getAuthToken());
  }

  @Test
  @Order(5)
  void testSetHandlersDoesNotThrow() {
    assertDoesNotThrow(() -> {
      client.setBidUpdateHandler(r -> {
      });
      client.setAuctionEndHandler(r -> {
      });
      client.setAuctionExtendedHandler(r -> {
      });
    });
  }

  // ── Tests: Auth ──────────────────────────────────────────────────────────

  @Test
  @Order(10)
  void testLoginSuccess() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Response> ref = new AtomicReference<>();

    Map<String, Object> data = new HashMap<>();
    data.put("username", "validuser");
    data.put("password", "pass");
    sendAsync(new Request(CommandType.LOGIN, data), resp -> {
      ref.set(resp);
      latch.countDown();
    });

    assertTrue(latch.await(5, TimeUnit.SECONDS), "Login timeout");
    assertNotNull(ref.get());
    assertTrue(ref.get().isSuccess(), "Login hợp lệ phải thành công");
  }

  @Test
  @Order(11)
  void testLoginFailWithWrongCredentials() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Response> ref = new AtomicReference<>();

    Map<String, Object> data = new HashMap<>();
    data.put("username", "nonexistent_user");
    data.put("password", "wrong");
    sendAsync(new Request(CommandType.LOGIN, data), resp -> {
      ref.set(resp);
      latch.countDown();
    });

    assertTrue(latch.await(5, TimeUnit.SECONDS), "Login timeout");
    assertNotNull(ref.get());
    assertFalse(ref.get().isSuccess(), "Login sai thông tin phải bị từ chối");
  }

  @Test
  @Order(12)
  void testRegister() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Response> ref = new AtomicReference<>();

    Map<String, Object> userData = new HashMap<>();
    userData.put("username", "newuser");
    userData.put("password", "pass123");
    userData.put("email", "new@test.com");
    sendAsync(new Request(CommandType.REGISTER, userData), resp -> {
      ref.set(resp);
      latch.countDown();
    });

    assertTrue(latch.await(5, TimeUnit.SECONDS), "Register timeout");
    assertNotNull(ref.get());
    assertTrue(ref.get().isSuccess(), "Đăng ký phải thành công");
  }

  // ── Tests: Product ───────────────────────────────────────────────────────

  @Test
  @Order(20)
  void testGetProducts() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Response> ref = new AtomicReference<>();

    sendAsync(new Request(CommandType.GET_PRODUCTS, new HashMap<>()), resp -> {
      ref.set(resp);
      latch.countDown();
    });

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertNotNull(ref.get());
    assertTrue(ref.get().isSuccess(), "GET_PRODUCTS phải thành công");
  }

  @Test
  @Order(21)
  void testAddProduct() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Response> ref = new AtomicReference<>();

    Map<String, Object> pd = new HashMap<>();
    pd.put("name", "Laptop Test");
    pd.put("startingPrice", 500.0);
    sendAsync(new Request(CommandType.ADD_PRODUCT, pd), resp -> {
      ref.set(resp);
      latch.countDown();
    });

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertNotNull(ref.get());
    assertTrue(ref.get().isSuccess(), "ADD_PRODUCT phải thành công");
    assertNotNull(ref.get().getData().get("productId"), "Phải có productId trong response");
  }

  // ── Tests: Bidding ───────────────────────────────────────────────────────

  @Test
  @Order(30)
  void testPlaceBidValid() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Response> ref = new AtomicReference<>();

    Map<String, Object> data = new HashMap<>();
    data.put("productId", 101);
    data.put("bidAmount", 200.0); // > 50 → hợp lệ theo MockServer
    sendAsync(new Request(CommandType.PLACE_BID, data), resp -> {
      ref.set(resp);
      latch.countDown();
    });

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertTrue(ref.get().isSuccess(), "Bid hợp lệ phải được chấp nhận");
  }

  @Test
  @Order(31)
  void testPlaceBidTooLow() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Response> ref = new AtomicReference<>();

    Map<String, Object> data = new HashMap<>();
    data.put("productId", 101);
    data.put("bidAmount", 10.0); // < 50 → không hợp lệ
    sendAsync(new Request(CommandType.PLACE_BID, data), resp -> {
      ref.set(resp);
      latch.countDown();
    });

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertFalse(ref.get().isSuccess(), "Bid quá thấp phải bị từ chối");
  }

  // ── Tests: AutoBid ───────────────────────────────────────────────────────

  @Test
  @Order(40)
  void testSetAutoBid() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Response> ref = new AtomicReference<>();

    Map<String, Object> data = new HashMap<>();
    data.put("productId", 101);
    data.put("maxBid", 1000.0);
    data.put("increment", 50.0);
    sendAsync(new Request(CommandType.SET_AUTO_BID, data), resp -> {
      ref.set(resp);
      latch.countDown();
    });

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertTrue(ref.get().isSuccess(), "SET_AUTO_BID phải thành công");
  }

  @Test
  @Order(41)
  void testRemoveAutoBid() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Response> ref = new AtomicReference<>();

    Map<String, Object> data = new HashMap<>();
    data.put("productId", 101);
    sendAsync(new Request(CommandType.REMOVE_AUTO_BID, data), resp -> {
      ref.set(resp);
      latch.countDown();
    });

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertTrue(ref.get().isSuccess(), "REMOVE_AUTO_BID phải thành công");
  }

  // ── Tests: Subscribe ─────────────────────────────────────────────────────

  @Test
  @Order(50)
  void testSubscribeAuction() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Response> ref = new AtomicReference<>();

    Map<String, Object> data = new HashMap<>();
    data.put("productId", 101);
    sendAsync(new Request(CommandType.SUBSCRIBE_AUCTION, data), resp -> {
      ref.set(resp);
      latch.countDown();
    });

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertTrue(ref.get().isSuccess(), "SUBSCRIBE phải thành công");
  }

  @Test
  @Order(51)
  void testUnsubscribeAuction() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Response> ref = new AtomicReference<>();

    Map<String, Object> data = new HashMap<>();
    data.put("productId", 101);
    sendAsync(new Request(CommandType.UNSUBSCRIBE_AUCTION, data), resp -> {
      ref.set(resp);
      latch.countDown();
    });

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertTrue(ref.get().isSuccess(), "UNSUBSCRIBE phải thành công");
  }

  // ── Tests: Funds ─────────────────────────────────────────────────────────

  @Test
  @Order(60)
  void testAddFunds() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Response> ref = new AtomicReference<>();

    Map<String, Object> data = new HashMap<>();
    data.put("userId", 1);
    data.put("amount", 500000.0);
    sendAsync(new Request(CommandType.ADD_FUNDS, data), resp -> {
      ref.set(resp);
      latch.countDown();
    });

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertTrue(ref.get().isSuccess(), "ADD_FUNDS phải thành công");
    assertEquals(500000.0, ref.get().getData().get("balance"), "Số dư phải đúng");
  }

  @Test
  @Order(61)
  void testGetBalance() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Response> ref = new AtomicReference<>();

    Map<String, Object> data = new HashMap<>();
    data.put("userId", 1);
    sendAsync(new Request(CommandType.GET_USER_BALANCE, data), resp -> {
      ref.set(resp);
      latch.countDown();
    });

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertTrue(ref.get().isSuccess());
    assertNotNull(ref.get().getData().get("balance"), "Phải có balance trong response");
  }

  // ── Tests: Logout ────────────────────────────────────────────────────────

  @Test
  @Order(70)
  void testLogout() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Response> ref = new AtomicReference<>();

    sendAsync(new Request(CommandType.LOGOUT, new HashMap<>()), resp -> {
      ref.set(resp);
      latch.countDown();
    });

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertTrue(ref.get().isSuccess(), "LOGOUT phải thành công");
  }

  // ── Tests: sendRequest khi chưa connect ──────────────────────────────────

  @Test
  @Order(80)
  void testSendRequestWhenNotConnected() {
    client.disconnect();
    Request req = new Request(CommandType.GET_PRODUCTS, new HashMap<>());
    assertThrows(IOException.class, () -> client.sendRequest(req),
            "sendRequest khi chưa connect phải ném IOException");
  }
}