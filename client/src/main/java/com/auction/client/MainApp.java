package com.auction.client;

import com.auction.client.controller.WindowManager;
import com.auction.client.utils.NotificationManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        MainApp.primaryStage = primaryStage;
        // WindowManager tự xử lý đóng cửa sổ và logout
        WindowManager.openWindow("/com/auction/client/view/LoginController.fxml", this);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    //  Thêm method để khởi tạo NotificationManager (gọi sau khi login thành công)
    public static void initNotificationManager() {
        if (primaryStage != null && com.auction.client.network.SocketClient.getInstance().isConnected()) {
            NotificationManager.getInstance().init(primaryStage,
                    com.auction.client.network.SocketClient.getInstance());
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}