package com.auction.client.utils;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class NotificationToast {

  private static final int TOAST_DURATION_MS = 3000;
  private static final int FADE_DURATION_MS = 300;
  private static final int TOAST_HEIGHT = 60;
  private static final int TOAST_MARGIN = 10;

  // 🟢 Danh sách toast đang hiển thị
  private static List<ToastInfo> activeToasts = new ArrayList<>();
  private static int nextYOffset = 0;

  public enum NotificationType {
    INFO("🔔", "#2196F3"),
    SUCCESS("✅", "#4CAF50"),
    WARNING("⚠️", "#FF9800"),
    BID("💰", "#9C27B0"),
    AUCTION_END("🏆", "#F44336"),
    TIME_EXTEND("⏰", "#FF5722"),
    SUBSCRIBE("✅", "#4CAF50"),
    UNSUBSCRIBE("❌", "#9E9E9E");

    final String icon;
    final String color;

    NotificationType(String icon, String color) {
      this.icon = icon;
      this.color = color;
    }
  }

  private static class ToastInfo {
    Popup popup;
    VBox wrapper;
    int yOffset;
  }

  public static void show(Window owner, String message, NotificationType type) {
    if (owner == null)
      return;

    Platform.runLater(() -> {
      // 🟢 Tính toán vị trí Y mới (dịch các toast cũ lên trên)
      updateToastPositions();

      Popup popup = new Popup();
      popup.setAutoFix(true);
      popup.setAutoHide(true);
      popup.setConsumeAutoHidingEvents(false);

      HBox content = new HBox(12);
      content.setAlignment(Pos.CENTER_LEFT);
      content.setStyle(String.format(
              "-fx-background-color: %s; -fx-background-radius: 8; -fx-padding: 12 20; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);",
              type.color));

      Label iconLabel = new Label(type.icon);
      iconLabel.setStyle("-fx-font-size: 20px;");

      Label messageLabel = new Label(message);
      messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
      messageLabel.setWrapText(true);

      Label closeBtn = new Label("✕");
      closeBtn.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");
      closeBtn.setOnMouseClicked(e -> {
        removeToast(popup);
        popup.hide();
      });

      content.getChildren().addAll(iconLabel, messageLabel, closeBtn);

      VBox wrapper = new VBox(content);
      Rectangle progressBar = new Rectangle(0, 3);
      progressBar.setFill(Color.rgb(255, 255, 255, 0.6));
      wrapper.getChildren().add(progressBar);

      popup.getContent().add(wrapper);

      // 🟢 Tính toán vị trí (góc phải dưới, xếp từ dưới lên)
      Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
      double screenWidth = screenBounds.getWidth();
      double screenHeight = screenBounds.getHeight();

      // Đợi layout xong để lấy width
      popup.setOnShown(e -> {
        double toastWidth = wrapper.getWidth();
        double x = screenWidth - toastWidth - 20;
        double y = screenHeight - 100 - nextYOffset;
        popup.setX(x);
        popup.setY(y);

        // Lưu lại toast info
        ToastInfo info = new ToastInfo();
        info.popup = popup;
        info.wrapper = wrapper;
        info.yOffset = nextYOffset;
        activeToasts.add(info);

        // Tăng offset cho toast tiếp theo
        nextYOffset += TOAST_HEIGHT + TOAST_MARGIN;

        // Animation progress bar
        double barWidth = content.getWidth();
        progressBar.setWidth(barWidth);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progressBar.widthProperty(), barWidth)),
                new KeyFrame(Duration.millis(TOAST_DURATION_MS), new KeyValue(progressBar.widthProperty(), 0)));
        timeline.play();

        // Tự động đóng sau thời gian
        PauseTransition pause = new PauseTransition(Duration.millis(TOAST_DURATION_MS));
        pause.setOnFinished(ev -> {
          removeToast(popup);
          FadeTransition fade = new FadeTransition(Duration.millis(FADE_DURATION_MS), wrapper);
          fade.setFromValue(1);
          fade.setToValue(0);
          fade.setOnFinished(ev2 -> popup.hide());
          fade.play();
        });
        pause.play();
      });

      popup.show(owner);
    });
  }

  // 🟢 Cập nhật vị trí các toast khi có toast mới hoặc toast cũ đóng
  private static void updateToastPositions() {
    int newOffset = 0;
    for (ToastInfo info : activeToasts) {
      info.yOffset = newOffset;
      if (info.popup.isShowing()) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double screenHeight = screenBounds.getHeight();
        double newY = screenHeight - 100 - newOffset;
        info.popup.setY(newY);
      }
      newOffset += TOAST_HEIGHT + TOAST_MARGIN;
    }
    nextYOffset = newOffset;
  }

  // 🟢 Xóa toast khỏi danh sách và cập nhật vị trí các toast còn lại
  private static void removeToast(Popup popup) {
    activeToasts.removeIf(info -> info.popup == popup);
    updateToastPositions();
  }
}