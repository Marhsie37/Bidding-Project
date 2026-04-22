package Part1;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ProductItemController {
    @FXML private Label lblName, lblPrice, lblTimer;
    @FXML private ImageView imgView;
    @FXML private Button btnEdit, btnDelete; // Thêm fx:id cho các nút này trong FXML

    public void setData(Product p) {
        lblName.setText(p.getName());
        lblPrice.setText(p.getPrice() + " VNĐ");

        String url = p.getImageUrl();
        if (url != null && !url.isEmpty()) {
            try {

                if (!url.startsWith("http") && !url.startsWith("file:")) {
                    url = "file:" + url;
                }


                Image img = new Image(url, true); //


                img.errorProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal) System.out.println("Lỗi không thể tải ảnh tại: " + p.getImageUrl());
                });

                imgView.setImage(img);

            } catch (Exception e) {
                System.out.println("Sai định dạng URL ảnh: " + e.getMessage());
            }
        }
    }

    public Button getBtnEdit() { return btnEdit; }
    public Button getBtnDelete() { return btnDelete; }
    public Label getLblTimer() { return lblTimer; }
}