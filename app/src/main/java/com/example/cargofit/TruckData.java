package com.example.cargofit;

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class TruckData {
    private List<Truck> trucks = new ArrayList<>();
    protected void onCreate(Bundle savedInstanceState) {
        uploadToFirebase();
        loadTrucks();
    }


    private void loadTrucks() {
        FirebaseDatabase.getInstance()
                .getReference("trucks")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        trucks.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Truck t = ds.getValue(Truck.class);
                            if (t != null) trucks.add(t);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void uploadToFirebase() {

        DatabaseReference truckRef = FirebaseDatabase.getInstance().getReference("trucks");
        trucks.clear();

        Truck t1 = new Truck("T1", "Small Van - T1", 1700, 1500, 1200, 500);
        Truck t11 = new Truck("T11", "Small Van - T11", 1700, 1500, 1200, 700);
        Truck t111 = new Truck("T111", "Small Van - T111", 1700, 1500, 1200, 900);

        Truck t2 = new Truck("T2", "Medium Truck - T2", 2400, 1700, 1400, 1000);
        Truck t22 = new Truck("T22", "Medium Truck - T22", 2400, 1700, 1400, 1100);
        Truck t222 = new Truck("T222", "Medium Truck - T222", 2400, 1700, 1400, 1200);

        Truck t3 =new Truck("T3", "Large Truck - T3", 3400, 1700, 1700, 1300);
        Truck t33 =new Truck("T33", "Large Truck - T33", 3400, 1700, 1700, 1400);
        Truck t333 =new Truck("T33", "Large Truck - T333", 3400, 1700, 1700, 1500);

        Truck t4 = new Truck("T4", "Refrigerated Truck - T4", 4000, 2200, 2200, 1600);
        Truck t44 = new Truck("T44", "Refrigerated Truck - T44", 4000, 2200, 2200, 1700);
        Truck t444 = new Truck("T444", "Refrigerated Truck - T444", 4000, 2200, 2200, 1800);

        Truck t5 = new Truck("T5", "Heavy Duty Truck - T5", 6100, 2300, 2400, 2000);
        Truck t55 = new Truck("T55", "Heavy Duty Truck - T55", 6100, 2300, 2400, 2250);
        Truck t555 = new Truck("T555", "Heavy Duty Truck - T555", 6100, 2300, 2400, 2500);

        trucks.add(t1);
        trucks.add(t11);
        trucks.add(t111);

        trucks.add(t2);
        trucks.add(t22);
        trucks.add(t222);

        trucks.add(t3);
        trucks.add(t33);
        trucks.add(t333);

        trucks.add(t4);
        trucks.add(t44);
        trucks.add(t444);

        trucks.add(t5);
        trucks.add(t55);
        trucks.add(t555);

        for (Truck tr : trucks) {
            String key = truckRef.push().getKey();
            if (key != null) truckRef.child(key).setValue(tr);
        }
    }
}
