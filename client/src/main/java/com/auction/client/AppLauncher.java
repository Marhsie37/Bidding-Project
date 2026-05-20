package com.auction.client;

import com.auction.client.network.SocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppLauncher {
    private static final Logger logger = LoggerFactory.getLogger(AppLauncher.class);
    public static void main(String[] args) {
        try {
            // 1. Phải kết nối TRƯỚC khi khởi chạy giao diện
            logger.info("Đang kết nối admin đến Server port 9999...");
            SocketClient.getInstance().connect();

            // 2. Sau khi kết nối thành công mới mở App
            MainApp.main(args);
        } catch (Exception e) {
            // Nếu Server chưa bật, nó sẽ nhảy vào đây
            logger.error("LỖI KẾT NỐI: Server chưa bật hoặc sai Port!");
            e.printStackTrace();
        }
    }
}