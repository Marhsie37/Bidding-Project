package com.auction.client.utils;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

public class NotificationToast {

    private static final int TOAST_DURATION_MS = 4000;
    private static final int FADE_DURATION_MS = 300;

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

    public static void show(Window owner, String message, NotificationType type) {
        Popup popup = new Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(true);

        // Tạo container chính
        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 8; -fx-padding: 12 20; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);",
                type.color
        ));

        // Icon
        Label iconLabel = new Label(type.icon);
        iconLabel.setStyle("-fx-font-size: 20px;");

        // Message
        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        messageLabel.setWrapText(true);

        // Close button
        Label closeBtn = new Label("✕");
        closeBtn.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");
        closeBtn.setOnMouseClicked(e -> popup.hide());

        content.getChildren().addAll(iconLabel, messageLabel, closeBtn);

        // Progress bar animation
        VBox wrapper = new VBox(content);
        Rectangle progressBar = new Rectangle(0, 3);
        progressBar.setFill(Color.rgb(255, 255, 255, 0.6));
        progressBar.widthProperty().bind(content.widthProperty());
        wrapper.getChildren().add(progressBar);

        popup.getContent().add(wrapper);

        // Hiển thị ở góc phải dưới
        popup.setOnShown(e -> {
            double x = owner.getX() + owner.getWidth() - wrapper.getBoundsInParent().getWidth() - 20;
            double y = owner.getY() + owner.getHeight() - 100;
            popup.setX(x);
            popup.setY(y);

            // Animation progress bar
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(progressBar.widthProperty(), content.getWidth())),
                    new KeyFrame(Duration.millis(TOAST_DURATION_MS), new KeyValue(progressBar.widthProperty(), 0))
            );
            timeline.play();

            // Fade out và đóng
            PauseTransition pause = new PauseTransition(Duration.millis(TOAST_DURATION_MS));
            pause.setOnFinished(ev -> {
                FadeTransition fade = new FadeTransition(Duration.millis(FADE_DURATION_MS), wrapper);
                fade.setFromValue(1);
                fade.setToValue(0);
                fade.setOnFinished(ev2 -> popup.hide());
                fade.play();
            });
            pause.play();
        });

        popup.show(owner);
    }
}