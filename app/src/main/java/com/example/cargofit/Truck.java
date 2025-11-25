package com.example.cargofit;

public class Truck {
    public String truckId;
    public String truckName;
    public int height;
    public int length;
    public int width;
    public double maxWeight;

    public Truck() {}

    public Truck(String truckId, String truckName, int height, int length, int width, double maxWeight) {
        this.truckId = truckId;
        this.truckName = truckName;
        this.height = height;
        this.length = length;
        this.width = width;
        this.maxWeight = maxWeight;
    }
}
