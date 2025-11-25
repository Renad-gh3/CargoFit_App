package com.example.cargofit;

public class CargoItem {
    public String id;
    private String productName;
    private int quantity;
    private double weight;
    private double length;
    private double width;
    private double height;
    private String type;
    private String origin;
    private String destination;

    // Constructor vacío necesario para Firebase
    public CargoItem() {}

    public CargoItem(String productName, int quantity, double weight,
                     double length, double width, double height,
                     String type, String origin, String destination) {
        this.id = null;
        this.productName = productName;
        this.quantity = quantity;
        this.weight = weight;
        this.length = length;
        this.width = width;
        this.height = height;
        this.type = type;
        this.origin = origin;
        this.destination = destination;
    }

    // Getters and Setters
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public double getLength() { return length; }
    public void setLength(double length) { this.length = length; }

    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }

    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
}