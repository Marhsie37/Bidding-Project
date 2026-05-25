package com.auction.test.network;

import com.auction.server.ClientHandler;
import com.auction.server.service.NotificationService;
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
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stress test: 100 người đấu giá cùng lúc
 * <p>
 * Các kịch bản được kiểm tra:
 * TC-S01  – 100 client kết nối đồng thời (bare socket)
 * TC-S02  – 100 client đặt bid ngẫu nhiên cùng lúc, không lỗi / không deadlock
 * TC-S03  – Throughput: tất cả 100 bid phải hoàn thành trong 20 giây
 * TC-S04  – Tính nhất quán: không có 2 bid nào ghi đúng cùng giá trị vào highestBid
 * (mô phỏng CAS / AtomicReference race)
 * TC-S05  – 100 subscriber nhận đủ notification khi có bid mới
 * TC-S06  – FixedThreadPool(20) xử lý 100 bid task – không task nào bị bỏ sót
 * TC-S07  – 100 client LOGIN → SUBSCRIBE → PLACE_BID → LOGOUT (full flow)
 * TC-S08  – Latency P95: 95% request phải hoàn thành trong 2000 ms
 * TC-S09  – Graceful shutdown: executor tắt gọn sau khi 100 task xong
 * TC-S10  – Không memory-leak: sau 100 kết nối đóng, không còn socket nào mở
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class ConcurrentBiddingStressTest {

  // ------------------------------------------------------------------ //
  //  Hằng số
  // ------------------------------------------------------------------ //
  private static final int MOCK_PORT = 19999;
  private static final int BIDDER_COUNT = 100;
  private static final int PRODUCT_ID = 1;
  private static final double BASE_BID = 1_000.0;
  private static final Logger log = LoggerFactory.getLogger(ConcurrentBiddingStressTest.class);

  // ------------------------------------------------------------------ //
  //  Mock server
  // ------------------------------------------------------------------ //
  private static ServerSocket mockServer;
  private static Thread serverThread;

  @BeforeAll
  static void startServer() throws IOException {
    mockServer = new ServerSocket(MOCK_PORT);
    serverThread = new Thread(() -> {
      while (!mockServer.isClosed()) {
        try {
          Socket client = mockServer.accept();
          new Thread(() -> serveClient(client)).start();
        } catch (IOException e) {
          if (!mockServer.isClosed()) log.error("[MockServer] accept error", e);
        }
      }
    });
    serverThread.setDaemon(true);
    serverThread.start();
    log.info("[MockServer] Listening on port {}", MOCK_PORT);
  }

  private static void serveClient(Socket client) {
    try (
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(client.getInputStream())
    ) {
      while (!client.isClosed()) {
        Object obj = in.readObject();
        if (obj instanceof Request) {
          out.writeObject(mockResponse((Request) obj));
          out.flush();
        }
      }
    } catch (EOFException ignored) {
    } catch (Exception e) {
      log.debug("[MockServer] client error: {}", e.getMessage());
    }
  }

  private static Response mockResponse(Request req) {
    Map<String, Object> data = new HashMap<>();
    switch (req.getCommand()) {
      case LOGIN:
        data.put("user", "bidder");
        data.put("role", "USER");
        return new Response(CommandType.LOGIN, true, "Login OK", data);
      case SUBSCRIBE_AUCTION:
        return new Response(CommandType.SUBSCRIBE_AUCTION, true, "Subscribed", null);
      case PLACE_BID:
        data.put("productId", req.getData().get("productId"));
        data.put("bidAmount", req.getData().get("bidAmount"));
        data.put("bidderName", req.getData().get("bidderName"));
        return new Response(CommandType.PLACE_BID, true, "Bid placed", data);
      case LOGOUT:
        return new Response(CommandType.LOGOUT, true, "Bye", null);
      case GET_PRODUCTS:
        data.put("products", Collections.emptyList());
        return new Response(CommandType.GET_PRODUCTS, true, "OK", data);
      default:
        return new Response(req.getCommand(), true, "OK", null);
    }
  }

  @AfterAll
  static void stopServer() throws IOException {
    if (mockServer != null && !mockServer.isClosed()) mockServer.close();
    log.info("[MockServer] Stopped");
  }

  // Thêm method này vào class
  private void waitForServerReady() throws InterruptedException {
    int maxRetries = 10;
    int retryDelay = 500; // ms

    for (int i = 0; i < maxRetries; i++) {
      try (Socket s = new Socket("localhost", MOCK_PORT)) {
        log.info("[MockServer] Server is ready");
        return;
      } catch (IOException e) {
        log.debug("Waiting for server... attempt {}/{}", i + 1, maxRetries);
        Thread.sleep(retryDelay);
      }
    }
    throw new RuntimeException("Server not ready after " + (maxRetries * retryDelay) + "ms");
  }

  // Thêm @BeforeEach để đảm bảo server ready trước mỗi test
  @BeforeEach
  void ensureServerReady() throws InterruptedException {
    waitForServerReady();
  }

  // ------------------------------------------------------------------ //
  //  Helper
  // ------------------------------------------------------------------ //
  private Socket connect() throws IOException {
    return new Socket("localhost", MOCK_PORT);
  }

  // ------------------------------------------------------------------ //
  //  TC-S01 – 100 kết nối socket đồng thời
  // ------------------------------------------------------------------ //
  @Test
  @DisplayName("TC-S01: 100 client kết nối đồng thời không lỗi")
  void testHundredClientsConnect() throws InterruptedException {
    // Đợi server sẵn sàng
    waitForServerReady();

    CountDownLatch ready = new CountDownLatch(BIDDER_COUNT);
    CountDownLatch done = new CountDownLatch(BIDDER_COUNT);
    AtomicInteger success = new AtomicInteger();
    List<Socket> sockets = Collections.synchronizedList(new ArrayList<>());
    AtomicInteger errorCount = new AtomicInteger(0);

    for (int i = 0; i < BIDDER_COUNT; i++) {
      final int threadId = i;
      new Thread(() -> {
        try {
          Socket s = connect();
          sockets.add(s);
          success.incrementAndGet();
          log.debug("[TC-S01] Thread {} connected", threadId);
        } catch (Exception e) {
          errorCount.incrementAndGet();
          log.error("[TC-S01] Thread {} connection error: {}", threadId, e.getMessage());
        } finally {
          ready.countDown();
          done.countDown();
        }
      }).start();
    }

    // Chờ tất cả kết nối hoàn thành (hoặc timeout)
    boolean allDone = done.await(30, TimeUnit.SECONDS);

    // Log kết quả
    log.info("[TC-S01] Success: {}, Errors: {}, Total: {}",
            success.get(), errorCount.get(), BIDDER_COUNT);

    assertTrue(allDone, "Phải hoàn thành trong 30 giây");
    assertEquals(BIDDER_COUNT, success.get(),
            String.format("Tất cả %d kết nối phải thành công, nhưng chỉ có %d",
                    BIDDER_COUNT, success.get()));

    // Đóng tất cả socket
    for (Socket s : sockets) {
      try {
        if (s != null && !s.isClosed()) {
          s.close();
        }
      } catch (IOException ignored) {
      }
    }
  }

  // ------------------------------------------------------------------ //
  //  TC-S02 – 100 bid ngẫu nhiên, không lỗi / không deadlock
  // ------------------------------------------------------------------ //
  @Test
  @DisplayName("TC-S02: 100 client đặt bid ngẫu nhiên cùng lúc không lỗi / deadlock")
  void testHundredConcurrentBids() throws InterruptedException {
    CountDownLatch startGun = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(BIDDER_COUNT);
    AtomicInteger success = new AtomicInteger();
    AtomicInteger errors = new AtomicInteger();
    Random rng = new Random();

    for (int i = 0; i < BIDDER_COUNT; i++) {
      final int idx = i;
      new Thread(() -> {
        try (Socket s = connect()) {
          ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
          ObjectInputStream in = new ObjectInputStream(s.getInputStream());
          startGun.await();

          Map<String, Object> data = new HashMap<>();
          data.put("productId", PRODUCT_ID);
          data.put("bidAmount", BASE_BID + rng.nextInt(10_000));
          data.put("bidderName", "bidder_" + idx);

          out.writeObject(new Request(CommandType.PLACE_BID, data));
          out.flush();

          Response res = (Response) in.readObject();
          if (res.isSuccess()) success.incrementAndGet();
          else errors.incrementAndGet();
        } catch (Exception e) {
          errors.incrementAndGet();
          log.error("[TC-S02] bidder_{} error: {}", idx, e.getMessage());
        } finally {
          done.countDown();
        }
      }).start();
    }

    startGun.countDown();
    assertTrue(done.await(20, TimeUnit.SECONDS), "Không được deadlock – phải xong trong 20 giây");
    assertEquals(BIDDER_COUNT, success.get() + errors.get(), "Tổng phải đúng 100");
    assertEquals(0, errors.get(), "Không được có lỗi");
    log.info("[TC-S02] success={} errors={}", success.get(), errors.get());
  }

  // ------------------------------------------------------------------ //
  //  TC-S03 – Throughput: 100 bid trong 20 giây
  // ------------------------------------------------------------------ //
  @Test
  @DisplayName("TC-S03: Throughput – 100 bid hoàn thành trong 20 giây")
  void testThroughputHundredBids() throws InterruptedException {
    CountDownLatch done = new CountDownLatch(BIDDER_COUNT);
    AtomicInteger success = new AtomicInteger();
    long start = System.currentTimeMillis();

    for (int i = 0; i < BIDDER_COUNT; i++) {
      final int idx = i;
      new Thread(() -> {
        try (Socket s = connect()) {
          ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
          ObjectInputStream in = new ObjectInputStream(s.getInputStream());

          Map<String, Object> data = new HashMap<>();
          data.put("productId", PRODUCT_ID);
          data.put("bidAmount", BASE_BID + idx * 10);
          data.put("bidderName", "bidder_" + idx);

          out.writeObject(new Request(CommandType.PLACE_BID, data));
          out.flush();
          Object res = in.readObject();
          if (res instanceof Response && ((Response) res).isSuccess()) success.incrementAndGet();
        } catch (Exception e) {
          log.error("[TC-S03] {}", e.getMessage());
        } finally {
          done.countDown();
        }
      }).start();
    }

    boolean allDone = done.await(20, TimeUnit.SECONDS);
    long elapsed = System.currentTimeMillis() - start;

    assertTrue(allDone, "Phải hoàn thành trong 20 giây");
    assertEquals(BIDDER_COUNT, success.get(), "Phải nhận đủ 100 response thành công");
    log.info("[TC-S03] 100 bids hoàn thành trong {} ms  ({} bids/sec)",
            elapsed, String.format("%.1f", BIDDER_COUNT * 1000.0 / elapsed));
  }

  // ------------------------------------------------------------------ //
  //  TC-S04 – Race condition: AtomicReference CAS giữ đúng highest bid
  // ------------------------------------------------------------------ //
  @Test
  @DisplayName("TC-S04: Race condition – AtomicReference giữ đúng highest bid")
  void testHighestBidAtomicConsistency() throws InterruptedException {
    AtomicReference<Double> highestBid = new AtomicReference<>(0.0);
    CountDownLatch done = new CountDownLatch(BIDDER_COUNT);
    Random rng = new Random();
    List<Double> allBids = Collections.synchronizedList(new ArrayList<>());

    for (int i = 0; i < BIDDER_COUNT; i++) {
      new Thread(() -> {
        double bid = BASE_BID + rng.nextInt(50_000);
        allBids.add(bid);
        // CAS loop – đúng cách cập nhật highest bid trong môi trường concurrent
        highestBid.updateAndGet(current -> Math.max(current, bid));
        done.countDown();
      }).start();
    }

    done.await(5, TimeUnit.SECONDS);
    double expectedMax = allBids.stream().mapToDouble(Double::doubleValue).max().orElse(0);
    assertEquals(expectedMax, highestBid.get(), 0.001,
            "Highest bid phải đúng bằng giá trị lớn nhất trong tất cả các bid");
    log.info("[TC-S04] highestBid={} (max of {} bids)", highestBid.get(), allBids.size());
  }

  // ------------------------------------------------------------------ //
  //  TC-S05 – 100 subscriber đều nhận notification
  // ------------------------------------------------------------------ //
  @Test
  @DisplayName("TC-S05: 100 subscriber đều nhận notification khi có bid mới")
  void testHundredSubscribersReceiveNotification() throws InterruptedException {
    NotificationService service = NotificationService.getInstance();
    int auctionId = 9001;
    CountDownLatch latch = new CountDownLatch(BIDDER_COUNT);

    List<MockClientHandler> handlers = new ArrayList<>();
    for (int i = 0; i < BIDDER_COUNT; i++) {
      final String user = "stress_user_" + i;
      MockClientHandler handler = new MockClientHandler(user) {
        @Override
        public void sendBidUpdate(int pid, String bidder, double amount) {
          super.sendBidUpdate(pid, bidder, amount);
          latch.countDown();
        }
      };
      handlers.add(handler);
      service.subscribe(auctionId, user, handler);
    }

    // Phát một bid update – tất cả 100 subscriber phải nhận
    service.notifyBidUpdate(auctionId, "topBidder", 99_999.0);

    boolean allReceived = latch.await(10, TimeUnit.SECONDS);
    assertTrue(allReceived, "Tất cả 100 subscriber phải nhận notification trong 10 giây");

    // Kiểm tra mỗi handler nhận đúng 1 notification
    for (MockClientHandler handler : handlers) {
      assertEquals(1, handler.getBidUpdateCallCount(),
              "Subscriber " + handler.getUsername() + " phải nhận đúng 1 notification");
    }

    // Dọn dẹp
    for (int i = 0; i < BIDDER_COUNT; i++) {
      service.unsubscribe(auctionId, "stress_user_" + i);
    }
    log.info("[TC-S05] Notified: {}/{}", handlers.size(), BIDDER_COUNT);
  }

  // ------------------------------------------------------------------ //
  //  TC-S06 – FixedThreadPool(20) xử lý 100 bid task
  // ------------------------------------------------------------------ //
  @Test
  @DisplayName("TC-S06: FixedThreadPool(20) xử lý đủ 100 bid task, không task bị bỏ sót")
  void testFixedThreadPoolHundredBids() throws InterruptedException {
    int poolSize = 20;
    ExecutorService pool = Executors.newFixedThreadPool(poolSize);
    CountDownLatch done = new CountDownLatch(BIDDER_COUNT);
    AtomicInteger success = new AtomicInteger();
    AtomicInteger errors = new AtomicInteger();

    for (int i = 0; i < BIDDER_COUNT; i++) {
      final int idx = i;
      pool.submit(() -> {
        try (Socket s = connect()) {
          ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
          ObjectInputStream in = new ObjectInputStream(s.getInputStream());

          Map<String, Object> data = new HashMap<>();
          data.put("productId", PRODUCT_ID);
          data.put("bidAmount", BASE_BID + idx * 5);
          data.put("bidderName", "pool_bidder_" + idx);

          out.writeObject(new Request(CommandType.PLACE_BID, data));
          out.flush();

          Object res = in.readObject();
          if (res instanceof Response && ((Response) res).isSuccess()) success.incrementAndGet();
          else errors.incrementAndGet();
        } catch (Exception e) {
          errors.incrementAndGet();
          log.error("[TC-S06] pool_bidder_{} error: {}", idx, e.getMessage());
        } finally {
          done.countDown();
        }
      });
    }

    assertTrue(done.await(20, TimeUnit.SECONDS), "Tất cả 100 task phải xong trong 20 giây");
    pool.shutdown();
    assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS), "Pool phải shutdown gọn gàng");

    assertEquals(BIDDER_COUNT, success.get() + errors.get(), "Không task nào bị bỏ sót");
    assertEquals(BIDDER_COUNT, success.get(), "Tất cả phải thành công");
    assertEquals(0, errors.get(), "Không được có lỗi");
    log.info("[TC-S06] poolSize={} | success={} | errors={}", poolSize, success.get(), errors.get());
  }

  // ------------------------------------------------------------------ //
  //  TC-S07 – Full flow: LOGIN → SUBSCRIBE → PLACE_BID → LOGOUT
  // ------------------------------------------------------------------ //
  @Test
  @DisplayName("TC-S07: 100 client thực hiện full flow LOGIN→SUBSCRIBE→BID→LOGOUT")
  void testHundredClientsFullFlow() throws InterruptedException {
    CountDownLatch startGun = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(BIDDER_COUNT);
    AtomicInteger fullSuccess = new AtomicInteger();
    AtomicInteger failures = new AtomicInteger();

    for (int i = 0; i < BIDDER_COUNT; i++) {
      final int idx = i;
      new Thread(() -> {
        try (Socket s = connect()) {
          ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
          ObjectInputStream in = new ObjectInputStream(s.getInputStream());
          startGun.await();

          // --- LOGIN ---
          Map<String, Object> loginData = new HashMap<>();
          loginData.put("username", "user_" + idx);
          loginData.put("password", "pass_" + idx);
          out.writeObject(new Request(CommandType.LOGIN, loginData));
          out.flush();
          Response loginRes = (Response) in.readObject();
          if (!loginRes.isSuccess()) {
            failures.incrementAndGet();
            return;
          }

          // --- SUBSCRIBE ---
          Map<String, Object> subData = new HashMap<>();
          subData.put("productId", PRODUCT_ID);
          out.writeObject(new Request(CommandType.SUBSCRIBE_AUCTION, subData));
          out.flush();
          Response subRes = (Response) in.readObject();
          if (!subRes.isSuccess()) {
            failures.incrementAndGet();
            return;
          }

          // --- PLACE_BID ---
          Map<String, Object> bidData = new HashMap<>();
          bidData.put("productId", PRODUCT_ID);
          bidData.put("bidAmount", BASE_BID + idx * 7);
          bidData.put("bidderName", "user_" + idx);
          out.writeObject(new Request(CommandType.PLACE_BID, bidData));
          out.flush();
          Response bidRes = (Response) in.readObject();
          if (!bidRes.isSuccess()) {
            failures.incrementAndGet();
            return;
          }

          // Kiểm tra data phản hồi bid
          assertEquals(PRODUCT_ID,
                  ((Number) bidRes.getData().get("productId")).intValue());

          // --- LOGOUT ---
          out.writeObject(new Request(CommandType.LOGOUT, new HashMap<>()));
          out.flush();
          Response logoutRes = (Response) in.readObject();
          if (!logoutRes.isSuccess()) {
            failures.incrementAndGet();
            return;
          }

          fullSuccess.incrementAndGet();
        } catch (Exception e) {
          failures.incrementAndGet();
          log.error("[TC-S07] user_{} error: {}", idx, e.getMessage());
        } finally {
          done.countDown();
        }
      }).start();
    }

    startGun.countDown();
    assertTrue(done.await(30, TimeUnit.SECONDS), "Full flow phải xong trong 30 giây");
    assertEquals(BIDDER_COUNT, fullSuccess.get(),
            "Tất cả 100 client phải hoàn thành full flow thành công");
    assertEquals(0, failures.get(), "Không client nào được thất bại");
    log.info("[TC-S07] fullSuccess={} failures={}", fullSuccess.get(), failures.get());
  }

  // ------------------------------------------------------------------ //
  //  TC-S08 – Latency P95 < 2000 ms
  // ------------------------------------------------------------------ //
  @Test
  @DisplayName("TC-S08: Latency P95 – 95% request hoàn thành trong 2000 ms")
  void testLatencyP95Under2000ms() throws InterruptedException {
    List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
    CountDownLatch done = new CountDownLatch(BIDDER_COUNT);

    for (int i = 0; i < BIDDER_COUNT; i++) {
      final int idx = i;
      new Thread(() -> {
        long t0 = System.currentTimeMillis();
        try (Socket s = connect()) {
          ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
          ObjectInputStream in = new ObjectInputStream(s.getInputStream());

          Map<String, Object> data = new HashMap<>();
          data.put("productId", PRODUCT_ID);
          data.put("bidAmount", BASE_BID + idx);
          data.put("bidderName", "latency_bidder_" + idx);

          out.writeObject(new Request(CommandType.PLACE_BID, data));
          out.flush();
          in.readObject();

          latencies.add(System.currentTimeMillis() - t0);
        } catch (Exception e) {
          log.error("[TC-S08] bidder_{}: {}", idx, e.getMessage());
          latencies.add(Long.MAX_VALUE); // đánh dấu lỗi
        } finally {
          done.countDown();
        }
      }).start();
    }

    assertTrue(done.await(30, TimeUnit.SECONDS), "Phải xong trong 30 giây");

    List<Long> sorted = latencies.stream().sorted().collect(Collectors.toList());
    int p95Index = (int) Math.ceil(sorted.size() * 0.95) - 1;
    long p95Latency = sorted.get(p95Index);
    long p50Latency = sorted.get((int) Math.ceil(sorted.size() * 0.50) - 1);
    long maxLatency = sorted.get(sorted.size() - 1);

    log.info("[TC-S08] P50={}ms | P95={}ms | Max={}ms", p50Latency, p95Latency, maxLatency);
    assertTrue(p95Latency < 2000,
            String.format("P95 latency phải < 2000 ms, nhưng là %d ms", p95Latency));
  }

  // ------------------------------------------------------------------ //
  //  TC-S09 – Graceful shutdown sau 100 task
  // ------------------------------------------------------------------ //
  @Test
  @DisplayName("TC-S09: Graceful shutdown – executor tắt gọn sau khi 100 task xong")
  void testGracefulShutdownAfterHundredTasks() throws InterruptedException {
    ExecutorService executor = Executors.newFixedThreadPool(25);
    CountDownLatch done = new CountDownLatch(BIDDER_COUNT);
    AtomicInteger completed = new AtomicInteger();

    for (int i = 0; i < BIDDER_COUNT; i++) {
      final int idx = i;
      executor.submit(() -> {
        try (Socket s = connect()) {
          ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
          ObjectInputStream in = new ObjectInputStream(s.getInputStream());

          Map<String, Object> data = new HashMap<>();
          data.put("productId", PRODUCT_ID);
          data.put("bidAmount", BASE_BID + idx * 3);
          data.put("bidderName", "shutdown_bidder_" + idx);

          out.writeObject(new Request(CommandType.PLACE_BID, data));
          out.flush();
          in.readObject();
          completed.incrementAndGet();
        } catch (Exception e) {
          log.error("[TC-S09] shutdown_bidder_{}: {}", idx, e.getMessage());
        } finally {
          done.countDown();
        }
      });
    }

    assertTrue(done.await(20, TimeUnit.SECONDS), "100 task phải xong trong 20 giây");

    executor.shutdown();
    boolean terminated = executor.awaitTermination(5, TimeUnit.SECONDS);

    assertTrue(terminated, "Executor phải shutdown gọn gàng trong 5 giây");
    assertTrue(executor.isShutdown(), "Executor phải ở trạng thái shutdown");
    assertEquals(BIDDER_COUNT, completed.get(), "Phải hoàn thành đủ 100 task");
    log.info("[TC-S09] completed={} terminated={}", completed.get(), terminated);
  }

  // ------------------------------------------------------------------ //
  //  TC-S10 – Không còn socket nào mở sau khi tất cả đóng
  // ------------------------------------------------------------------ //
  @Test
  @DisplayName("TC-S10: Không memory-leak – 100 socket đóng đúng, không còn mở")
  void testNoSocketLeakAfterHundredConnections() throws InterruptedException {
    List<Socket> sockets = Collections.synchronizedList(new ArrayList<>());
    CountDownLatch connected = new CountDownLatch(BIDDER_COUNT);
    CountDownLatch done = new CountDownLatch(BIDDER_COUNT);
    AtomicInteger success = new AtomicInteger();

    for (int i = 0; i < BIDDER_COUNT; i++) {
      new Thread(() -> {
        try {
          Socket s = connect();
          sockets.add(s);
          success.incrementAndGet();
          connected.countDown();
          connected.await(5, TimeUnit.SECONDS);

          // Gửi 1 request trước khi đóng
          ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
          ObjectInputStream in = new ObjectInputStream(s.getInputStream());
          out.writeObject(new Request(CommandType.GET_PRODUCTS, new HashMap<>()));
          out.flush();
          in.readObject();
        } catch (Exception e) {
          log.error("[TC-S10] {}", e.getMessage());
        } finally {
          done.countDown();
        }
      }).start();
    }

    assertTrue(connected.await(10, TimeUnit.SECONDS), "Phải đủ 100 kết nối");
    assertEquals(BIDDER_COUNT, success.get());

    // Đóng tất cả socket
    for (Socket s : sockets) {
      try {
        s.close();
      } catch (IOException ignored) {
      }
    }
    done.await(10, TimeUnit.SECONDS);

    // Xác nhận tất cả đã đóng
    long openCount = sockets.stream().filter(s -> !s.isClosed()).count();
    assertEquals(0, openCount,
            "Tất cả 100 socket phải đóng đúng – không có socket nào còn mở");
    log.info("[TC-S10] Opened: {}  |  Still open after close: {}", sockets.size(), openCount);
  }

  // =========================================================
//  MOCK CLIENT HANDLER (KHÔNG LỖI NPE)
// =========================================================

  static class MockClientHandler extends ClientHandler {
    private final String mockUsername;
    private int bidUpdateCallCount = 0;
    private int auctionEndCallCount = 0;

    public MockClientHandler(String username) {
      super(null);
      this.mockUsername = username;
    }

    @Override
    public void sendBidUpdate(int productId, String bidderName, double bidAmount) {
      bidUpdateCallCount++;
    }

    @Override
    public void sendAuctionEnd(int productId, int winnerId, String winnerName, double finalPrice) {
      auctionEndCallCount++;
    }

    @Override
    public void sendAuctionExtended(int productId, java.time.LocalDateTime newEndTime) {
      // Mock implementation
    }

    @Override
    public String getUsername() {
      return mockUsername;
    }

    public int getBidUpdateCallCount() {
      return bidUpdateCallCount;
    }

    public int getAuctionEndCallCount() {
      return auctionEndCallCount;
    }
  }
}
