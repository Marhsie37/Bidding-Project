package com.auction.shared.model;

public abstract class Item extends Entity {
    private String name;
    private String description;
    private double startingPrice;

    public Item(String id, String name, String description, double startingPrice) {
        super(id);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
    }

    public String getName() { return name; }
    public double getStartingPrice() { return startingPrice; }
}