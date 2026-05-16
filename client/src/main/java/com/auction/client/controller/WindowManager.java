package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.io.IOException;

public class WindowManager {

    public static Stage openUndecoratedWindow(String fxmlPath, Object caller) {
        try {
            FXMLLoader loader = new FXMLLoader(caller.getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = new Stage();

            stage.initStyle(StageStyle.UNDECORATED);

            Scene scene = new Scene(root);
            stage.setScene(scene);

            stage.sizeToScene();
            stage.centerOnScreen();

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

            // ✅ THÊM SỰ KIỆN ĐÓNG CỬA SỔ
            stage.setOnCloseRequest(event -> {
                System.out.println("🔄 Đóng cửa sổ, gửi logout lên server...");
                SocketClient socketClient = SocketClient.getInstance();
                if (socketClient.isConnected()) {
                    socketClient.logout(response -> {
                        System.out.println("✅ Đã logout, đóng kết nối.");
                        socketClient.disconnect();
                        Platform.exit();
                        System.exit(0);
                    });
                } else {
                    socketClient.disconnect();
                    Platform.exit();
                    System.exit(0);
                }
                event.consume();
            });

            stage.show();
            return stage;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}