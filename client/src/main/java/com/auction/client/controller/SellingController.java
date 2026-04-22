package Part1;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class Selling implements Initializable {
    @FXML private VBox vboxDisplay;
    @FXML private TextField txtName, txtPrice, txtImageUrl, txtDuration;

    // Thêm dòng này để lưu sản phẩm đang được chọn để sửa
    private Product selectedProduct = null;

    private void reconstructProductUI(Product p) {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Part1/ProductItem.fxml"));
            HBox productRow = loader.load();
            /*Nạp file giao diện mẫu sau đó gán chuyuển file FXML thành một đối tượng
            HBox tên là productRow
            và new sẽ tạo ra một HBox mới
            * */


            ProductItemController controller = loader.getController();
            controller.setData(p);
            /*Mỗi file FXML có một Controller,Controller chính là các phương thức có trong
            phương thức như getBtnBan,btnDelete....
            * */

            Label lblTimer = controller.getLblTimer();
            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                int remaining = p.getRemainingSeconds();
                if (remaining <= 0) {
                    lblTimer.setText("HẾT HẠN!");
                    productRow.setOpacity(0.6);
                } else {
                    lblTimer.setText("Còn: " + remaining + "s");
                }
            }));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();


            controller.getBtnDelete().setOnAction(e -> {
                timeline.stop();
                vboxDisplay.getChildren().remove(productRow);
                DataManager.sharedProductList.remove(p);
            });

            controller.getBtnEdit().setOnAction(e -> {
                selectedProduct = p;
                txtName.setText(p.getName());
                txtPrice.setText(p.getPrice());
                txtImageUrl.setText(p.getImageUrl());
                txtDuration.setText("1200");
            });

            vboxDisplay.getChildren().add(productRow);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi: Không tìm thấy file ProductItem.fxml hoặc lỗi ép kiểu!");
        }
    }
    @Override

    public void initialize(URL location, ResourceBundle resources) {

        vboxDisplay.getChildren().clear();
        vboxDisplay.setSpacing(10);
        for (Product p : DataManager.sharedProductList) {
            reconstructProductUI(p);
        }
    }




    @FXML
    void handleAddProduct() {
        String name = txtName.getText().trim();
        String price = txtPrice.getText().trim();
        String url = txtImageUrl.getText().trim();
        String durationStr = txtDuration.getText().trim();

        if (name.isEmpty() || price.isEmpty() || url.isEmpty() || durationStr.isEmpty()) return;

        try {
            int totalSeconds = Integer.parseInt(durationStr);

            if (selectedProduct == null) {

                Product newP = new Product(name, price, url, totalSeconds);
                DataManager.sharedProductList.add(newP);
            } else {

                selectedProduct.setName(name);
                selectedProduct.setPrice(price);
                selectedProduct.setImageUrl(url);

                selectedProduct.resetEndTime(totalSeconds);

                selectedProduct = null;
            }

            vboxDisplay.getChildren().clear();
            for (Product p : DataManager.sharedProductList) {
                reconstructProductUI(p);
            }

            clearFields();
        } catch (NumberFormatException e) {
            System.out.println("Lỗi: Thời gian và giá phải là số!");
        }
    }

    @FXML
    public void toProductListController(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Part1/ProductListController.fxml"));

        Stage window = (Stage) vboxDisplay.getScene().getWindow();
        window.setScene(new Scene(root));
        window.show();
    }

    private void clearFields() {
        txtName.clear(); txtPrice.clear(); txtImageUrl.clear(); txtDuration.clear();
        txtName.requestFocus();
    }

    @FXML
    public void toAdmin(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Part1/Admin.fxml"));

        Stage window = (Stage) vboxDisplay.getScene().getWindow();
        window.setScene(new Scene(root));
        window.show();
    }

    public void goToLoginScreen(ActionEvent event) {
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/Part1/LoginController.fxml"));
            Scene loginScene = new Scene(loginRoot);
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(loginScene);
            window.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}