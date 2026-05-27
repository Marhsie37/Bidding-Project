package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * WindowManager - Quản lý cửa sổ ứng dụng
 * Hỗ trợ: kéo thả, undecorated, tùy chỉnh kích thước, callback khi đóng
 */
public class WindowManager {

  private static final Logger logger = LoggerFactory.getLogger(WindowManager.class);

  // Cấu hình mặc định
  private static final Config DEFAULT_CONFIG = new Config();

  // Lưu trữ các cửa sổ đang mở
  private static final Map<String, Stage> openStages = new HashMap<>();

  /**
   * Cấu hình cho cửa sổ
   */
  public static class Config {
    private boolean undecorated = true;
    private boolean resizable = false;
    private boolean centerOnScreen = true;
    private boolean enableDrag = true;
    private boolean autoLogoutOnClose = true;
    private double width = -1;
    private double height = -1;
    private String title = "";
    private Consumer<Stage> onCloseCallback;

    public Config setUndecorated(boolean undecorated) {
      this.undecorated = undecorated;
      return this;
    }

    public Config setResizable(boolean resizable) {
      this.resizable = resizable;
      return this;
    }

    public Config setCenterOnScreen(boolean centerOnScreen) {
      this.centerOnScreen = centerOnScreen;
      return this;
    }

    public Config setEnableDrag(boolean enableDrag) {
      this.enableDrag = enableDrag;
      return this;
    }

    public Config setAutoLogoutOnClose(boolean autoLogoutOnClose) {
      this.autoLogoutOnClose = autoLogoutOnClose;
      return this;
    }

    public Config setSize(double width, double height) {
      this.width = width;
      this.height = height;
      return this;
    }

    public Config setTitle(String title) {
      this.title = title;
      return this;
    }

    public Config setOnCloseCallback(Consumer<Stage> callback) {
      this.onCloseCallback = callback;
      return this;
    }
  }

  // ==================== CÁC METHOD MỞ CỬA SỔ ====================

  /**
   * Mở cửa sổ với cấu hình mặc định (dùng Class)
   */
  public static Stage openWindow(String fxmlPath, Class<?> callerClass) {
    return openWindow(fxmlPath, callerClass, DEFAULT_CONFIG);
  }

  /**
   * Mở cửa sổ với cấu hình tùy chỉnh (dùng Class)
   */
  public static Stage openWindow(String fxmlPath, Class<?> callerClass, Config config) {
    if (fxmlPath == null || callerClass == null) {
      logger.error("fxmlPath hoặc callerClass bị null!");
      return null;
    }

    try {
      URL fxmlUrl = callerClass.getResource(fxmlPath);
      if (fxmlUrl == null) {
        logger.error("Không tìm thấy file FXML: {}", fxmlPath);
        return null;
      }

      FXMLLoader loader = new FXMLLoader(fxmlUrl);
      Parent root = loader.load();
      Stage stage = new Stage();

      if (config.undecorated) {
        stage.initStyle(StageStyle.UNDECORATED);
      }

      Scene scene = new Scene(root);

      if (config.width > 0 && config.height > 0) {
        stage.setWidth(config.width);
        stage.setHeight(config.height);
      }
      stage.setScene(scene);
      stage.sizeToScene();

      if (config.centerOnScreen) {
        stage.centerOnScreen();
      }

      if (config.title != null && !config.title.isEmpty()) {
        stage.setTitle(config.title);
      }

      stage.setResizable(config.resizable);

      // 🟢 THÊM KÉO THẢ CHO CỬA SỔ (dùng chung cho mọi màn hình)
      setupDragAndDrop(root, stage);

      stage.setOnCloseRequest(event -> {
        logger.info("Đóng cửa sổ: {}", fxmlPath);

        if (config.onCloseCallback != null) {
          config.onCloseCallback.accept(stage);
        }

        if (config.autoLogoutOnClose) {
          performLogout();
        }

        openStages.remove(fxmlPath);
        event.consume();
      });

      openStages.put(fxmlPath, stage);
      stage.show();
      logger.info("Đã mở cửa sổ: {}", fxmlPath);
      return stage;

    } catch (IOException e) {
      logger.error("Lỗi khi mở cửa sổ {}: {}", fxmlPath, e.getMessage(), e);
      return null;
    }
  }

  /**
   * Mở cửa sổ với cấu hình tùy chỉnh (dùng Object)
   */
  public static Stage openWindow(String fxmlPath, Object caller, Config config) {
    return openWindow(fxmlPath, caller.getClass(), config);
  }

  /**
   * Mở cửa sổ với kích thước tùy chỉnh
   */
  public static Stage openWindow(String fxmlPath, Class<?> callerClass, double width, double height) {
    Config config = new Config().setSize(width, height);
    return openWindow(fxmlPath, callerClass, config);
  }

  /**
   * Mở cửa sổ có thanh tiêu đề (decorated)
   */
  public static Stage openDecoratedWindow(String fxmlPath, Class<?> callerClass, String title) {
    Config config = new Config()
            .setUndecorated(false)
            .setEnableDrag(false)
            .setTitle(title);
    return openWindow(fxmlPath, callerClass, config);
  }

  /**
   * Mở cửa sổ có thanh tiêu đề (decorated) - dùng Object
   */
  public static Stage openDecoratedWindow(String fxmlPath, Object caller, String title) {
    return openDecoratedWindow(fxmlPath, caller.getClass(), title);
  }

  // ==================== UTILITY METHODS ====================

  /**
   * Thiết lập kéo thả cho cửa sổ (dùng chung cho mọi màn hình)
   */
  private static void setupDragAndDrop(Parent root, Stage stage) {
    final double[] xOffset = new double[1];
    final double[] yOffset = new double[1];

    root.setOnMousePressed(event -> {
      xOffset[0] = event.getSceneX();
      yOffset[0] = event.getSceneY();
    });

    root.setOnMouseDragged(event -> {
      stage.setX(event.getScreenX() - xOffset[0]);
      stage.setY(event.getScreenY() - yOffset[0]);
    });
  }

  /**
   * Thực hiện logout
   */
  private static void performLogout() {
    SocketClient socketClient = SocketClient.getInstance();
    if (socketClient.isConnected()) {
      socketClient.logout(response -> {
        logger.info("Đã logout, đóng kết nối.");
        socketClient.disconnect();
        Platform.exit();
        System.exit(0);
      });
    } else {
      socketClient.disconnect();
      Platform.exit();
      System.exit(0);
    }
  }

  /**
   * Đóng một cửa sổ cụ thể
   */
  public static void closeWindow(String fxmlPath) {
    Stage stage = openStages.get(fxmlPath);
    if (stage != null) {
      stage.close();
      openStages.remove(fxmlPath);
      logger.info("Đã đóng cửa sổ: {}", fxmlPath);
    }
  }

  /**
   * Đóng tất cả cửa sổ
   */
  public static void closeAllWindows() {
    for (Stage stage : openStages.values()) {
      if (stage != null) {
        stage.close();
      }
    }
    openStages.clear();
    logger.info("Đã đóng tất cả cửa sổ");
  }

  /**
   * Lấy cửa sổ đang mở
   */
  public static Stage getWindow(String fxmlPath) {
    return openStages.get(fxmlPath);
  }

  /**
   * Kiểm tra cửa sổ có đang mở không
   */
  public static boolean isWindowOpen(String fxmlPath) {
    return openStages.containsKey(fxmlPath);
  }

  /**
   * Đặt cấu hình mặc định mới
   */
  public static void setDefaultConfig(Config config) {
    if (config != null) {
      DEFAULT_CONFIG.undecorated = config.undecorated;
      DEFAULT_CONFIG.resizable = config.resizable;
      DEFAULT_CONFIG.centerOnScreen = config.centerOnScreen;
      DEFAULT_CONFIG.enableDrag = config.enableDrag;
      DEFAULT_CONFIG.autoLogoutOnClose = config.autoLogoutOnClose;
      DEFAULT_CONFIG.width = config.width;
      DEFAULT_CONFIG.height = config.height;
      DEFAULT_CONFIG.title = config.title;
      DEFAULT_CONFIG.onCloseCallback = config.onCloseCallback;
    }
  }

  /**
   * Lấy cấu hình mặc định hiện tại
   */
  public static Config getDefaultConfig() {
    Config copy = new Config();
    copy.undecorated = DEFAULT_CONFIG.undecorated;
    copy.resizable = DEFAULT_CONFIG.resizable;
    copy.centerOnScreen = DEFAULT_CONFIG.centerOnScreen;
    copy.enableDrag = DEFAULT_CONFIG.enableDrag;
    copy.autoLogoutOnClose = DEFAULT_CONFIG.autoLogoutOnClose;
    copy.width = DEFAULT_CONFIG.width;
    copy.height = DEFAULT_CONFIG.height;
    copy.title = DEFAULT_CONFIG.title;
    copy.onCloseCallback = DEFAULT_CONFIG.onCloseCallback;
    return copy;
  }

  /**
   * Mở cửa sổ với cấu hình mặc định (dùng Object)
   */
  public static Stage openWindow(String fxmlPath, Object caller) {
    try {
      FXMLLoader loader = new FXMLLoader(caller.getClass().getResource(fxmlPath));
      Parent root = loader.load();
      Stage stage = new Stage();
      stage.initStyle(StageStyle.UNDECORATED);

      Scene scene = new Scene(root);
      stage.setScene(scene);
      stage.sizeToScene();
      stage.centerOnScreen();

      // Kéo thả
      setupDragAndDrop(root, stage);

      stage.show();
      return stage;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }
}