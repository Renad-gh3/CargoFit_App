package com.example.cargofit;

public class Truck {
    String truckId;
    String truckName;
    double length;
    double width;
    double height;
    double maxWeight;


    public Truck() {}

    public Truck(String truckId, String truckName, double length, double width, double height, double maxWeight) {
        this.truckId = truckId;
        this.truckName = truckName;
        this.length = length;
        this.width = width;
        this.height = height;
        this.maxWeight = maxWeight;
    }

    public String getTruckId() { return truckId; }
    public String getTruckName() { return truckName; }
    public double getLength() { return length; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getMaxWeight() { return maxWeight; }

}