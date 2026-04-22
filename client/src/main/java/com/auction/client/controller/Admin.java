package Part1;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane; // Thêm nếu bạn muốn điều khiển Tab từ code
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class Admin {

    @FXML private VBox vBoxDisplay;
    @FXML private VBox vBoxProducts;
    @FXML private TabPane adminTabPane;
    @FXML private Label blTotalActive;

    @FXML
    public void initialize() {

        renderUserList();
        renderProductList();
    }

    public void renderUserList() {
        vBoxDisplay.getChildren().clear();
        vBoxDisplay.setSpacing(10);

        for (User user : DataManager.allUsers) {

            if (user.getRole().equalsIgnoreCase("Admin")) {
                continue;
            }

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Part1/UserRow.fxml"));
                HBox row = loader.load();

                UserItemController controller = loader.getController();
                controller.setData(user);


                controller.getBtnBan().setOnAction(e -> {
                    user.setStatus(user.getStatus().equals("ACTIVE") ? "BANNED" : "ACTIVE");
                    renderUserList();
                });

                vBoxDisplay.getChildren().add(row);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void renderProductList() {
        vBoxProducts.getChildren().clear();
        for (Product p : DataManager.sharedProductList) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Part1/ProductItem.fxml"));
                HBox productCard = loader.load();


                ProductItemController controller = loader.getController();
                controller.setData(p);

                controller.getBtnEdit().setVisible(false);
                controller.getBtnDelete().setText("Xóa khỏi hệ thống");
                controller.getBtnDelete().setOnAction(e -> {
                    DataManager.sharedProductList.remove(p);
                    renderProductList();
                });

                vBoxProducts.getChildren().add(productCard);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }



    @FXML
    public void toLogin(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "LoginController.fxml");
    }

    @FXML
    public void toSelling(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "Selling.fxml");
    }

    private void switchScene(ActionEvent event, String fxml) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(new Scene(root));
        window.centerOnScreen();
        window.show();
    }
}