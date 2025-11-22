package com.example.cargofit;

import android.os.Bundle;
import android.widget.Toast;

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

        Truck t1 = new Truck("T1", "Small Van", 1200, 250, 150, 150);
        Truck t2 = new Truck("T2", "Medium Truck", 3500, 400, 200, 200);
        Truck t3 =new Truck("T3", "Large Truck", 8000, 600, 240, 240);
        Truck t4 = new Truck("T4", "Refrigerated Truck", 5000, 500, 220, 220);
        Truck t5 = new Truck("T5", "Heavy Duty Truck", 15000, 800, 260, 260);

        trucks.add(t1);
        trucks.add(t2);
        trucks.add(t3);
        trucks.add(t4);
        trucks.add(t5);

        for (Truck tr : trucks) {
            String key = truckRef.push().getKey();
            if (key != null) truckRef.child(key).setValue(tr);
        }
    }
}
