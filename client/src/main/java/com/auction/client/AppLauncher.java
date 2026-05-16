package com.auction.client;

import com.auction.client.network.SocketClient;

public class AppLauncher {
    public static void main(String[] args) {
        try {
            // 1. Phải kết nối TRƯỚC khi khởi chạy giao diện
            System.out.println("Đang kết nốiadmin đến Server port 9999...");
            SocketClient.getInstance().connect();

            // 2. Sau khi kết nối thành công mới mở App
            MainApp.main(args);
        } catch (Exception e) {
            // Nếu Server chưa bật, nó sẽ nhảy vào đây
            System.err.println("LỖI KẾT NỐI: Server chưa bật hoặc sai Port!");
            e.printStackTrace();
        }
    }
}