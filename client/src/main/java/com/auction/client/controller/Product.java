package Part1;

import java.util.ArrayList;
import java.util.List;

public class Product {
    private String name;
    private String price;
    private String imageUrl;
    private long endTime;
    private String description;
    private List<String> bidHistory = new ArrayList<>();
    private double increment = 10.0;

    public Product(String name, String price, String imageUrl, int durationInSeconds) {
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.endTime = System.currentTimeMillis() + (durationInSeconds * 1000L);
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getBidHistory() { return bidHistory; }

    public void addBid(String bidder, double amount) {
        bidHistory.add(0, bidder + " đã đặt: " + amount + " VNĐ");
    }

    public void setPrice(String price) { this.price = price; }
    public String getName() { return name; }
    public String getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public double getIncrement() { return increment; }
    public void setIncrement(double increment) { this.increment = increment; }



    public void setName(String name) { this.name = name; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public void resetEndTime(int durationInSeconds) {
        this.endTime = System.currentTimeMillis() + (durationInSeconds * 1000L);
    }


    public int getRemainingSeconds() {
        long remaining = (endTime - System.currentTimeMillis()) / 1000;
        return (remaining > 0) ? (int) remaining : 0;
    }
}