package com.auction.client.controller;

import com.auction.shared.model.Product;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Base64;

public class ActiveProductItemController {
  private static final Logger logger = LoggerFactory.getLogger(ActiveProductItemController.class);

  @FXML
  private ImageView imgView;
  @FXML
  private Label lblName;
  @FXML
  private Label lblPrice;
  @FXML
  private Label lblStart;
  @FXML
  private Label lblStatus;
  @FXML
  private Label lblTimer;

  public void setData(Product p) {
    if (p == null) return;
    lblName.setText(p.getName() != null ? p.getName() : "Không có tên");
    lblPrice.setText("Giá hiện tại: " + String.format("%,.0f VNĐ", p.getCurrentPrice()));
    lblStart.setText("Giá khởi điểm: " + String.format("%,.0f VNĐ", p.getStartingPrice()));

    boolean isActive = "ACTIVE".equalsIgnoreCase(p.getStatus());
    lblStatus.setText(isActive ? "🟢 Đang đấu giá" : "🟢 Chờ duyệt");

    String url = p.getImageUrl();
    if (url != null && !url.isEmpty()) {
      loadImage(url);
    } else {
      imgView.setImage(null);
    }
  }

  private void loadImage(String url) {
    try {
      if (url.startsWith("data:image") || isBase64(url)) {
        String base64Data = url.contains(",") ? url.split(",", 2)[1] : url;
        byte[] bytes = Base64.getDecoder().decode(base64Data);
        imgView.setImage(new Image(new ByteArrayInputStream(bytes)));
      } else {
        String imgUrl = url.startsWith("http") || url.startsWith("file:") ? url : "file:" + url;
        imgView.setImage(new Image(imgUrl, true));
      }
    } catch (Exception e) {
      logger.warn("Không thể load ảnh: {}", e.getMessage());
    }
  }

  private boolean isBase64(String str) {
    if (str == null || str.length() < 100) return false;
    return str.matches("^[A-Za-z0-9+/=]+$");
  }

  public Label getLblTimer() {
    return lblTimer;
  }
}
