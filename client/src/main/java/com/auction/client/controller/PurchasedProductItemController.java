package com.auction.client.controller;

import com.auction.shared.model.Product;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PurchasedProductItemController {

    private static final Logger logger = LoggerFactory.getLogger(PurchasedProductItemController.class);

    @FXML private Label lblName;
    @FXML private Label lblPrice;
    @FXML private ImageView imgProduct;

    public void setData(Product p) {
        if (p == null) {
            logger.warn("Product is null, cannot set data");
            return;
        }

        // Set tên sản phẩm
        String name = p.getName();
        lblName.setText(name != null ? name : "Không có tên");

        // Set giá (định dạng có dấu phẩy)
        lblPrice.setText(String.format("%,.0f VNĐ", p.getCurrentPrice()));

        // Load ảnh
        String url = p.getImageUrl();
        loadImage(url);
    }

    private void loadImage(String url) {
        if (url == null || url.isEmpty()) {
            logger.debug("Không có URL ảnh cho sản phẩm");
            imgProduct.setImage(null);
            return;
        }

        logger.info("🔍 URL ảnh: {}", url);

        try {
            // Xử lý đường dẫn ảnh
            String imageUrl = url;
            if (!imageUrl.startsWith("http") && !imageUrl.startsWith("file:")) {
                imageUrl = "file:" + imageUrl;
            }

            Image img = new Image(imageUrl, true);

            // Xử lý lỗi load ảnh
            img.errorProperty().addListener((obs, oldErr, newErr) -> {
                if (newErr) {
                    logger.error("Không thể tải ảnh từ URL: {}", url);
                    Platform.runLater(() -> imgProduct.setImage(null));
                }
            });

            imgProduct.setImage(img);
            logger.info("✅ Đã load ảnh thành công: {}", url);

        } catch (Exception e) {
            logger.error("❌ Lỗi load ảnh: {} - {}", url, e.getMessage());
            imgProduct.setImage(null);
        }
    }

    public void updateProductInfo(Product p) {
        if (p == null) return;
        setData(p);
    }
}