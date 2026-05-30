package com.auction.client.controller;

import com.auction.shared.model.Product;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Base64;

public class ProductItemController {
  private static final Logger logger = LoggerFactory.getLogger(ProductItemController.class);

  @FXML
  private Label lblName;
  @FXML
  private Label lblPrice;
  @FXML
  private Label lblTimer;
  @FXML
  private ImageView imgView;
  @FXML
  private Button btnEdit;
  @FXML
  private Button btnDelete;

  public void setData(Product p) {
    if (p == null) {
      logger.warn("Product is null, cannot set data");
      return;
    }

    // Set tên sản phẩm
    String name = p.getName();
    lblName.setText(name != null ? name : "Không có tên");

    // Set giá
    lblPrice.setText(String.format("%,.0f VNĐ", p.getCurrentPrice()));

    // Set timer (sẽ được cập nhật bởi Timeline bên ngoài)
    // Không set text mặc định ở đây

    // Load ảnh
    String url = p.getImageUrl();
    if (url != null && !url.isEmpty()) {
      loadImage(url);
    } else {
      logger.debug("Không có URL ảnh cho sản phẩm: {}", name);
      imgView.setImage(null);
    }
  }

  private void loadImage(String url) {
    try {
      // Hỗ trợ ảnh dạng Base64
      if (url.startsWith("data:image") || isBase64(url)) {
        String base64Data = url.contains(",") ? url.split(",", 2)[1] : url;
        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
        Image img = new Image(new ByteArrayInputStream(imageBytes));
        imgView.setImage(img);
        return;
      }

      // Xử lý đường dẫn file thông thường
      String imageUrl = url;
      if (!imageUrl.startsWith("http") && !imageUrl.startsWith("file:")) {
        imageUrl = "file:" + imageUrl;
      }

      Image img = new Image(imageUrl, true);
      img.errorProperty().addListener((obs, oldErr, newErr) -> {
        if (newErr) {
          logger.error("În không thể tải ảnh từ URL: {}", url);
          Platform.runLater(() -> imgView.setImage(null));
        }
      });
      imgView.setImage(img);

    } catch (Exception e) {
      logger.error("Lỗi khi tải ảnh: {} - {}", url, e.getMessage());
      imgView.setImage(null);
    }
  }

  private boolean isBase64(String str) {
    if (str == null || str.length() < 100) return false;
    // Base64 strings only contain A-Z, a-z, 0-9, +, /, =
    return str.matches("^[A-Za-z0-9+/=]+$");
  }

  public void updatePrice(double newPrice) {
    lblPrice.setText(String.format("%,.0f VNĐ", newPrice));
  }

  public void updateTimer(String timeText) {
    lblTimer.setText(timeText);
  }

  public Button getBtnEdit() {
    return btnEdit;
  }

  public Button getBtnDelete() {
    return btnDelete;
  }

  public Label getLblTimer() {
    return lblTimer;
  }

  public Label getLblName() {
    return lblName;
  }

  public Label getLblPrice() {
    return lblPrice;
  }

  public ImageView getImgView() {
    return imgView;
  }
}