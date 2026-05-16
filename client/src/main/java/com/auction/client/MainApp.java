package com.auction.client;

import com.auction.client.controller.WindowManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        // WindowManager tự xử lý đóng cửa sổ và logout
        WindowManager.openUndecoratedWindow("/com/auction/client/view/LoginController.fxml", this);
    }

    public static void main(String[] args) {
        launch(args);
    }
}