package com.example.cargofit;

import java.io.Serializable;
import java.util.List;

public class AssignedTruck implements Serializable {
    public String truckId;
    public String truckName;
    public double totalWeight;   // kg used
    public double totalVolume;   // real volume in cm^3 used (not scaled)
    public List<UnitItem> items;

    public AssignedTruck() {}

    public AssignedTruck(String truckId, String truckName, double totalWeight, double totalVolume, List<UnitItem> items) {
        this.truckId = truckId;
        this.truckName = truckName;
        this.totalWeight = totalWeight;
        this.totalVolume = totalVolume;
        this.items = items;
    }
}
