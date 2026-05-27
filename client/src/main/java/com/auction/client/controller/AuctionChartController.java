package com.auction.client.controller;

import com.auction.shared.model.BidTransaction;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class AuctionChartController {

  @FXML
  private LineChart<String, Number> auctionLineChart;
  @FXML
  private CategoryAxis xAxis;
  @FXML
  private NumberAxis yAxis;

  private XYChart.Series<String, Number> priceSeries;
  private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

  @FXML
  public void initialize() {
    priceSeries = new XYChart.Series<>();
    priceSeries.setName("Giá hiện tại");

    auctionLineChart.getData().add(priceSeries);

    auctionLineChart.setAnimated(true);
  }

  public void loadChartData(List<BidTransaction> bidHistory) {
    // 🟢🟢🟢 THÊM LOG NÀY 🟢🟢🟢
    System.out.println("🟢 [4] AuctionChartController.loadChartData() - NHẬN ĐƯỢC " +
            (bidHistory == null ? 0 : bidHistory.size()) + " BID");

    Platform.runLater(() -> {
      priceSeries.getData().clear();

      if (bidHistory == null || bidHistory.isEmpty()) {
        System.out.println("🟢 [4] Không có dữ liệu, thoát");
        return;
      }

      bidHistory.sort(Comparator.comparing(BidTransaction::getBidTime));

      for (BidTransaction bid : bidHistory) {
        String formattedTime = bid.getBidTime().format(timeFormatter);
        System.out.println("🟢 [4] Vẽ điểm: " + formattedTime + " - " + bid.getBidAmount());
        priceSeries.getData().add(new XYChart.Data<>(formattedTime, bid.getBidAmount()));
      }

      System.out.println("🟢 [4] ĐÃ VẼ XONG " + priceSeries.getData().size() + " ĐIỂM");
    });
  }

  public void updateNewBidRealtime(BidTransaction newBid) {
    Platform.runLater(() -> {
      String formattedTime = newBid.getBidTime().format(timeFormatter);

      priceSeries.getData().add(new XYChart.Data<>(formattedTime, newBid.getBidAmount()));


      if (priceSeries.getData().size() > 15) {
        priceSeries.getData().remove(0);
      }
    });
  }
}