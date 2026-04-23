package com.auction.client.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ProductListController implements Initializable {

    @FXML private VBox vboxGallery;
    @FXML private HBox imageHBox;
    @FXML private Pane sliderContainer; // Khung chứa cố định

    private int currentSlideIndex = 0;
    private final int totalSlides = 3;
    private final double slideWidth = 612.0;
    private Timeline autoSlideTimer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupImageSlider();
        refreshGallery();
    }

    private void setupImageSlider() {
        if (imageHBox == null || sliderContainer == null) return;
        //Tránh trường hợp quên đặt fx:id hay là biến khác tên


        imageHBox.setTranslateX(0);
        //Đảm bảo Hbox nằm ở vị trí x = 0 nằm sát với viền

        Rectangle clip = new Rectangle(slideWidth, 234);
        //Tạo một hình chữ nhật có chiều rộng bằng slideWidth = 612,chiều cao là 234

        sliderContainer.setClip(clip);
        //slideContainer xác định vị trí ảnh trong stage
        //clip là phần được nhìn thấy,nếu đi qua phần này thì sẽ mất


        autoSlideTimer = new Timeline(new KeyFrame(Duration.seconds(4), e -> goNext()));
        /* new KeyFrame là mốc thời gian quan trọng,thiết lập như một đồng hồ báo thức
        * Duration.seconds(4) : cứ sau 4 giây sẽ làm hành động gì đó
        * Hành động ở đây là một hàm lamda e->goNext :Sau 4 giầy thì nó sẽ thưc hiện goNext
        * đi sang ảnh tiêp theo
        */

        autoSlideTimer.setCycleCount(Timeline.INDEFINITE);
        //Thiết lập số lần lặp trong toàn bộ 4 giây là vô tận
        autoSlideTimer.play();
        //Khi gọi play thì đồng hồ mới bắt đầu đếm ngược khi chạy chương trình

        sliderContainer.setOnMouseEntered(event -> autoSlideTimer.pause());
        //Khi đưa chuột vào ảnh thì thời gian đếm ngược sẽ dừng lại để người dùng coi sản phẩm
        sliderContainer.setOnMouseExited(event -> autoSlideTimer.play());
        //Khi đưa chuột ra khỏi ảnh thì tiếp tục đếm
    }

    @FXML
    public void goNext() {
        currentSlideIndex = (currentSlideIndex + 1) % totalSlides;
        updateSliderPosition();
    }
    //Ý tưởng là lấy hiện tại cộng thêm 1 nhưng để không quá số lượng thì phải chia cho tổng số ảnh

    @FXML
    public void goPrevious() {
        currentSlideIndex = (currentSlideIndex - 1 + totalSlides) % totalSlides;
        updateSliderPosition();
    }
    //Ý tưởng là lâ hiện tại trừ i 1 nhưng không được để số âm nên phải cộng thêm tộng rồi mới chia lấy dư cho tổng

    private void updateSliderPosition() {
        double targetX = -(currentSlideIndex * slideWidth);
        /*Lấy chiều rộng nhân cho số ảnh hiện tại thì sẽ đến vị trí ảnh cần chạy
        ví dụ muốn kéo sang ảnh 1 thì 1 x 612 = 612 mà đặt là -612 thì ảnh mời dịch sang phải
         */

        TranslateTransition transition = new TranslateTransition(Duration.millis(700), imageHBox);
        //Duration.millis(700) : sau 700 milis thì ảnh sau mới lấp lên hoàn toàn ảnh đầu
        transition.setToX(targetX);
        //setToX : dịch chuyển đến tọa độ targetX != setTranslateX : dịch chuyển tới luôn
        //setByX(targetX) : dịch chuyển x một khoảng targetX
        transition.play();
    }


    public void refreshGallery() {
        if (vboxGallery == null) return;
        vboxGallery.getChildren().clear();
        vboxGallery.setSpacing(15); // Khoảng cách giữa các sản phẩm

        if (DataManager.sharedProductList != null) {
            for (Product p : DataManager.sharedProductList) {
                try {

                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/Part1/ProductItemStage.fxml"));
                    Parent productCard = loader.load();

                    ProductItemController itemController = loader.getController();
                    itemController.setData(p);


                    productCard.setOnMouseClicked(event -> {
                        openProductDetail(p);
                    });

                    vboxGallery.getChildren().add(productCard);

                } catch (IOException e) {
                    System.err.println("Lỗi khi nạp mẫu sản phẩm FXML");
                    e.printStackTrace();
                }
            }
        }
    }

    private void openProductDetail(Product p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Part1/ProductDetailController.fxml"));
            Parent root = loader.load();
            ProductDetailController controller = loader.getController();
            controller.setProductData(p);

            Stage stage = (Stage) vboxGallery.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void toSelling(ActionEvent event) throws IOException {
        changeScene(event, "/Part1/Selling.fxml");
    }

    @FXML
    private void toAdmin(ActionEvent event) throws IOException {
        changeScene(event, "/Part1/Admin.fxml");
    }

    @FXML
    private void goToLoginScreen(ActionEvent event) throws IOException {
        changeScene(event, "/Part1/LoginController.fxml");
    }

    private void changeScene(ActionEvent event, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(new Scene(root));
        window.show();
    }
}