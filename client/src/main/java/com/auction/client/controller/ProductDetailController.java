package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Product;
import com.auction.shared.protocol.CommandType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ProductDetailController {

  private static final Logger logger = LoggerFactory.getLogger(ProductDetailController.class);

  @FXML
  private ImageView imgDetail;
  @FXML
  private Label lblDetailPrice;
  @FXML
  private Label lblDetailTimer;
  @FXML
  private TextField txtBidAmount;
  @FXML
  private Label lblDescription;
  @FXML
  private TextField txtMaxAutoPrice;
  @FXML
  private TextField txtIncrement;
  @FXML
  private Button btnBid;
  @FXML
  private Button btnAutoBid;
  @FXML
  private ListView<String> lvBidHistory;
  @FXML
  private VBox auctionChart;
  @FXML
  private AuctionChartController auctionChartController;

  private Product product;
  private Timeline timerTimeline;

  @FXML
  public void initialize() {
    txtBidAmount.setTextFormatter(new TextFormatter<>(change -> {
      if (change.getText().matches("\\d*")) {
        return change;
      }
      return null;
    }));

    System.out.println("🔵 [1] ProductDetailController.initialize() - ĐÃ CHẠY");
  }

  public void setProductData(Product p) {
    this.product = p;
    updateDisplay();
    startTimer();
    loadBidHistory();
    registerRealtimeHandlers();
    System.out.println("🔵 [2] setProductData() - Sản phẩm ID: " + p.getId());

    // 🟢 TỰ ĐỘNG SUBSCRIBE (không cần checkbox)
    sendSubscribeRequest(true);

    System.out.println("🔍 auctionChartController = " + auctionChartController);
  }

  // Gửi request subscribe/unsubscribe
  private void sendSubscribeRequest(boolean subscribe) {
    Map<String, Object> data = new HashMap<>();
    data.put("productId", product.getId());
    CommandType cmd = subscribe ? CommandType.SUBSCRIBE_AUCTION : CommandType.UNSUBSCRIBE_AUCTION;

    Request req = new Request(cmd, data);
    SocketClient.getInstance().sendRequestAsync(req, response -> {
      Platform.runLater(() -> {
        if (response.isSuccess()) {
          logger.info("{} sản phẩm thành công", subscribe ? "Theo dõi" : "Hủy theo dõi");
        } else {
          logger.error("Lỗi khi {} sản phẩm: {}", subscribe ? "theo dõi" : "hủy theo dõi", response.getMessage());
        }
      });
    });
  }

  private void startTimer() {
    if (timerTimeline != null) {
      timerTimeline.stop();
    }

    timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
      if (product == null) return;
      int remaining = product.getRemainingSeconds();
      if (remaining <= 0) {
        lblDetailTimer.setText("HẾT HẠN!");
        timerTimeline.stop();
        lockExpiredUI();
      } else {
        lblDetailTimer.setText(formatTime(remaining));
      }
    }));
    timerTimeline.setCycleCount(Timeline.INDEFINITE);
    timerTimeline.play();

    // Khóa ngay nếu sản phẩm đã hết hạn khi mở màn hình
    if (product != null && product.getRemainingSeconds() <= 0) {
      lockExpiredUI();
    }
  }

  private void lockExpiredUI() {
    Platform.runLater(() -> {
      lblDetailTimer.setText("HẾT HẠN!");
      lblDetailTimer.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
      if (btnBid != null) {
        btnBid.setDisable(true);
        btnBid.setText("ĐÃ KẾT THÚC");
        btnBid.setStyle("-fx-background-color: #aaaaaa; -fx-text-fill: white; -fx-font-weight: bold;");
      }
      if (txtBidAmount != null) txtBidAmount.setDisable(true);
      if (btnAutoBid != null) btnAutoBid.setDisable(true);
      if (txtMaxAutoPrice != null) txtMaxAutoPrice.setDisable(true);
      if (txtIncrement != null) txtIncrement.setDisable(true);
    });
  }

  private void loadBidHistory() {
    if (product == null) return;

    Map<String, Object> data = new HashMap<>();
    data.put("productId", product.getId());

    Request request = new Request(CommandType.GET_AUCTION_HISTORY, data);
    SocketClient.getInstance().sendRequestAsync(request, response -> {
      Platform.runLater(() -> {
        if (response.isSuccess() && response.getData() != null) {
          Map<String, Object> resData = response.getData();
          Object historyObj = resData.get("history");

          System.out.println("🔵 [3] loadBidHistory() - Số bid nhận được: " +
                  (historyObj instanceof List ? ((List<?>) historyObj).size() : 0));

          lvBidHistory.getItems().clear();
          List<BidTransaction> bidHistoryList = new java.util.ArrayList<>();

          if (historyObj instanceof List) {
            List<?> rawList = (List<?>) historyObj;
            for (Object obj : rawList) {
              if (obj instanceof BidTransaction) {
                BidTransaction bid = (BidTransaction) obj;
                String bidder = bid.getBidderName();
                double amount = bid.getBidAmount();
                String time = bid.getBidTime() != null ? bid.getBidTime().toString() : "";
                lvBidHistory.getItems().add(String.format("%s: %,.0f VNĐ - %s", bidder, amount, time));
                bidHistoryList.add(bid);
              }
            }
          }

          if (auctionChartController != null) {
            auctionChartController.loadChartData(bidHistoryList);
            System.out.println("📊 Đã gửi " + bidHistoryList.size() + " bid cho biểu đồ");
          } else {
            System.out.println("❌ auctionChartController = NULL! Không thể vẽ biểu đồ");
          }

          if (lvBidHistory.getItems().isEmpty()) {
            lvBidHistory.getItems().add("Chưa có lượt đặt giá nào");
          }
        } else {
          lvBidHistory.getItems().add("Không thể tải lịch sử đấu giá");
        }
      });
    });
  }

  @FXML
  public void handlePlaceBid() {
    if (product == null) {
      showAlert("Lỗi", "Không có thông tin sản phẩm!");
      return;
    }

    try {
      double amount = Double.parseDouble(txtBidAmount.getText().trim());

      if (amount < product.getCurrentPrice() + 5000) {
        showAlert("Lỗi", "Giá đặt phải lớn hơn hoặc bằng giá hiện tại cộng thêm 5,000 VNĐ (tối thiểu " + String.format("%,.0f", product.getCurrentPrice() + 5000) + " VNĐ)!");
        return;
      }

      Map<String, Object> data = new HashMap<>();
      data.put("productId", product.getId());
      data.put("bidAmount", amount);

      Request req = new Request(CommandType.PLACE_BID, data);
      SocketClient.getInstance().sendRequestAsync(req, response -> {
        Platform.runLater(() -> {
          if (response.isSuccess()) {
            Map<String, Object> resData = response.getData();

            Object priceObj = resData.get("currentPrice");
            if (priceObj instanceof Number) {
              double newPrice = ((Number) priceObj).doubleValue();
              product.setCurrentPrice(newPrice);
            }

            if (resData.containsKey("newEndTime")) {
              String newEndTimeStr = (String) resData.get("newEndTime");
              try {
                LocalDateTime newEndTime = LocalDateTime.parse(newEndTimeStr);
                product.setEndTime(newEndTime);
                startTimer();
              } catch (Exception e) {
                logger.error("Lỗi parse newEndTime: {}", e.getMessage());
              }
            }

            updateDisplay();
            loadBidHistory();
            txtBidAmount.clear();
          } else {
            showAlert("Lỗi", response.getMessage() != null ? response.getMessage() : "Đặt giá thất bại!");
          }
        });
      });

    } catch (NumberFormatException e) {
      showAlert("Lỗi", "Vui lòng nhập số tiền hợp lệ!");
    }
  }

  @FXML
  public void handleStartAutoBid(ActionEvent event) {
    if (product == null) return;

    try {
      double maxPrice = Double.parseDouble(txtMaxAutoPrice.getText().trim());
      double increment = Double.parseDouble(txtIncrement.getText().trim());

      if (maxPrice <= product.getCurrentPrice()) {
        showAlert("Lỗi", "Max Bid phải lớn hơn giá hiện tại (" + String.format("%,.0f", product.getCurrentPrice()) + " VNĐ)!");
        return;
      }

      if (increment <= 0) {
        showAlert("Lỗi", "Bước giá phải lớn hơn 0!");
        return;
      }

      Map<String, Object> data = new HashMap<>();
      data.put("productId", product.getId());
      data.put("maxBid", maxPrice);
      data.put("increment", increment);

      Request req = new Request(CommandType.SET_AUTO_BID, data);
      SocketClient.getInstance().sendRequestAsync(req, response -> {
        Platform.runLater(() -> {
          if (response.isSuccess()) {
            showAlert("Thành công", response.getMessage());
          } else {
            showAlert("Lỗi", response.getMessage());
          }
        });
      });
    } catch (NumberFormatException e) {
      showAlert("Lỗi", "Vui lòng nhập số hợp lệ!");
    }
  }

  private String formatTime(int totalSeconds) {
    if (totalSeconds <= 0) return "HẾT HẠN!";
    int days = totalSeconds / 86400;
    int hours = (totalSeconds % 86400) / 3600;
    int minutes = (totalSeconds % 3600) / 60;
    int seconds = totalSeconds % 60;

    if (days > 0) return String.format("%d ngày %02d:%02d:%02d", days, hours, minutes, seconds);
    if (hours > 0) return String.format("%d:%02d:%02d", hours, minutes, seconds);
    return String.format("%02d:%02d", minutes, seconds);
  }

  private void updateDisplay() {
    if (product == null) return;

    lblDetailPrice.setText(String.format("%,.0f VNĐ", product.getCurrentPrice()));
    lblDescription.setText(product.getDescription() != null ? product.getDescription() : "Không có mô tả.");

    String url = product.getImageUrl();
    if (url != null && !url.isEmpty()) {
      try {
        if (url.startsWith("data:image") || isBase64(url)) {
          String base64Data = url.contains(",") ? url.split(",", 2)[1] : url;
          byte[] imageBytes = Base64.getDecoder().decode(base64Data);
          Image image = new Image(new ByteArrayInputStream(imageBytes));
          imgDetail.setImage(image);
        } else {
          if (!url.startsWith("http") && !url.startsWith("file:")) {
            url = "file:" + url;
          }
          Image image = new Image(url, true);
          imgDetail.setImage(image);
        }
      } catch (Exception e) {
        logger.error("Lỗi tải ảnh: ", e);
        imgDetail.setImage(null);
      }
    }
  }

  private boolean isBase64(String str) {
    if (str == null || str.length() < 100) return false;
    return str.matches("^[A-Za-z0-9+/=]+$");
  }

  private Consumer<Response> bidUpdateHandler;
  private Consumer<Response> auctionEndHandler;
  private Consumer<Response> auctionExtendedHandler;

  private void registerRealtimeHandlers() {
    if (product == null) return;
    int productId = product.getId();

    bidUpdateHandler = response -> {
      if (response.getData() == null || product == null) return;
      Map<String, Object> data = response.getData();
      if (((Number) data.get("productId")).intValue() != productId) return;

      double newPrice = ((Number) data.get("bidAmount")).doubleValue();
      Platform.runLater(() -> {
        product.setCurrentPrice(newPrice);
        updateDisplay();
        loadBidHistory();
      });
    };
    SocketClient.getInstance().addResponseHandler(CommandType.BID_UPDATE, bidUpdateHandler);

    auctionEndHandler = response -> {
      if (response.getData() == null || product == null) return;
      Map<String, Object> data = response.getData();
      if (((Number) data.get("productId")).intValue() != productId) return;

      String winnerName = (String) data.get("winnerName");
      double finalPrice = ((Number) data.get("finalPrice")).doubleValue();
      Platform.runLater(() -> {
        if (timerTimeline != null) timerTimeline.stop();
        lblDetailTimer.setText("ĐÃ KẾT THÚC");
        lblDetailTimer.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
      });
    };
    SocketClient.getInstance().addResponseHandler(CommandType.AUCTION_END, auctionEndHandler);

    auctionExtendedHandler = response -> {
      if (response.getData() == null || product == null) return;
      Map<String, Object> data = response.getData();
      if (((Number) data.get("productId")).intValue() != productId) return;

      String newEndTimeStr = (String) data.get("newEndTime");
      try {
        LocalDateTime newEndTime = LocalDateTime.parse(newEndTimeStr);
        Platform.runLater(() -> {
          product.setEndTime(newEndTime);
          startTimer();
        });
      } catch (Exception e) {
        logger.error("Lỗi parse: {}", e.getMessage());
      }
    };
    SocketClient.getInstance().addResponseHandler(CommandType.AUCTION_EXTENDED, auctionExtendedHandler);
  }

  private void unregisterHandlers() {
    if (bidUpdateHandler != null)
      SocketClient.getInstance().removeResponseHandler(CommandType.BID_UPDATE, bidUpdateHandler);
    if (auctionEndHandler != null)
      SocketClient.getInstance().removeResponseHandler(CommandType.AUCTION_END, auctionEndHandler);
    if (auctionExtendedHandler != null)
      SocketClient.getInstance().removeResponseHandler(CommandType.AUCTION_EXTENDED, auctionExtendedHandler);
  }

  @FXML
  public void goToMain(ActionEvent event) {
    if (timerTimeline != null) {
      timerTimeline.stop();
      unregisterHandlers();
    }

    // Hủy subscribe khi thoát
    if (product != null) {
      sendSubscribeRequest(false);
    }

    try {
      Stage oldStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      oldStage.close();
      WindowManager.openWindow("/com/auction/client/view/Bidder.fxml", this);
    } catch (Exception e) {
      logger.error("Lỗi khi quay về màn hình chính: ", e);
    }
  }

  private void showAlert(String title, String content) {
    Alert alert = new Alert(title.equals("Lỗi") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}