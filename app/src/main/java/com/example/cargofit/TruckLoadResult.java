package com.example.cargofit;

import java.util.List;

public class TruckLoadResult {

    private String truckId;
    private List<CargoItem> assignedItems;
    private double usedWeight;
    private double loadPercentage;

    public TruckLoadResult() {}

    public TruckLoadResult(String truckId, List<CargoItem> assignedItems, double usedWeight, double loadPercentage) {
        this.truckId = truckId;
        this.assignedItems = assignedItems;
        this.usedWeight = usedWeight;
        this.loadPercentage = loadPercentage;
    }

    public String getTruckId() { return truckId; }
    public List<CargoItem> getAssignedItems() { return assignedItems; }
    public double getUsedWeight() { return usedWeight; }
    public double getLoadPercentage() { return loadPercentage; }
}
