package com.auction.server;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuctionServerTest {

  private AuctionServer server;
  private Thread serverThread;

  @BeforeAll
  void setUp() throws InterruptedException {
    server = AuctionServer.getInstance();
    serverThread = new Thread(() -> server.start());
    serverThread.setDaemon(true);
    serverThread.start();

    // Give server time to start
    Thread.sleep(1000);
  }

  @AfterAll
  void tearDown() {
    if (server != null) {
      server.stop();
    }
    if (serverThread != null) {
      serverThread.interrupt();
    }
  }

  @Test
  void testGetInstance() {
    AuctionServer instance1 = AuctionServer.getInstance();
    AuctionServer instance2 = AuctionServer.getInstance();

    assertSame(instance1, instance2, "Should be singleton");
  }

  @Test
  void testServerStarts() {
    assertNotNull(server);
  }

  @Test
  void testRegisterAndUnregisterClient() {
    // Create a mock ClientHandler
    ClientHandler mockHandler = new ClientHandler(null) {
      @Override
      public String getUsername() {
        return "testUser";
      }
    };

    server.registerClient("testUser", mockHandler);
    assertNotNull(server.getClient("testUser"));

    server.unregisterClient("testUser");
    assertNull(server.getClient("testUser"));
  }

  @Test
  void testGetClientsMap() {
    assertNotNull(server.getClients());
  }

  @Test
  void testGetAuctionService() {
    assertNotNull(server.getAuctionService());
  }

  @Test
  void testCanConnectToServer() {
    try (Socket socket = new Socket("localhost", 9999)) {
      assertTrue(socket.isConnected());
    } catch (IOException e) {
      fail("Should be able to connect to server", e);
    }
  }
}