package com.auction.client.controller; // Kiểm tra lại tên package của bạn

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextField;
import javafx.scene.control.ListView;
import javafx.collections.transformation.FilteredList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ProductListController implements Initializable {

    @FXML
    private TextField searchField;

    @FXML
    private ListView<String> resultList;

    private ObservableList<String> masterData = FXCollections.observableArrayList("Đồng hồ", "Loa Bluetooth", "Máy đo điện", "Điện thoại");

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        FilteredList<String> filteredData = new FilteredList<>(masterData, p -> true);

        // 2. Thiết lập logic lọc khi gõ chữ
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(item -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return item.toLowerCase().contains(lowerCaseFilter);
            });
        });


        resultList.setItems(filteredData);
    }

    // Trong ProductListController.java
    @FXML
    public void toSelling(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Part1/Selling.fxml"));
        Stage window = (Stage) searchField.getScene().getWindow();
        window.setScene(new Scene(root));
        window.show();

    }
}