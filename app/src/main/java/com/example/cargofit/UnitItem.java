package com.example.cargofit;

import java.io.Serializable;

public class UnitItem implements Serializable {
    public String unitId;          // unique id: cargoId + "_" + index
    public String cargoId;         // original CargoItem id
    public String productName;
    public double weight;          // kg
    public int scaledVolume;       // volume after scaling (integer)
    public double realVolume;      // in cm^3 (for reporting)
    public String origin;
    public String destination;
    public String type;

    public UnitItem() {}

    public UnitItem(String unitId, String cargoId, String productName, double weight,
                    int scaledVolume, double realVolume, String origin, String destination, String type) {
        this.unitId = unitId;
        this.cargoId = cargoId;
        this.productName = productName;
        this.weight = weight;
        this.scaledVolume = scaledVolume;
        this.realVolume = realVolume;
        this.origin = origin;
        this.destination = destination;
        this.type = type;
    }
}
