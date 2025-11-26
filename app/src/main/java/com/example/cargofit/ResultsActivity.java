package com.example.cargofit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.*;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private TextView tvSummaryTotalItems, tvSummaryTotalWeight, tvSummaryTotalVolume;
    private TextView tvRouteDistance, tvRouteCost;
    private GoogleMap mMap;
    private MapHelper mapHelper;
    private List<CargoItem> cargoList = new ArrayList<>();
    private String origin = "";
    private String destination = "";
    private ImageButton btnHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        tvSummaryTotalItems = findViewById(R.id.tv_summary_total_items);
        tvSummaryTotalWeight = findViewById(R.id.tv_summary_total_weight);
        tvSummaryTotalVolume = findViewById(R.id.tv_summary_total_volume);

        tvRouteDistance = findViewById(R.id.tv_route_distance);
        tvRouteCost     = findViewById(R.id.tv_route_cost);

        mapHelper = new MapHelper(this);

        btnHome = findViewById(R.id.btn_home);
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(ResultsActivity.this, UploadExcelActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);

        if (mapFragment != null) mapFragment.getMapAsync(this);

        loadCargoDataFromFirebase();
        loadAssignedTrucks();
    }

    // ----------------------------------------------------------
    // LOAD DATA FROM FIREBASE
    // ----------------------------------------------------------
    private void loadCargoDataFromFirebase() {

        String userId = getIntent().getStringExtra("userId");
        String orderId = getIntent().getStringExtra("orderId");

        if (userId == null || orderId == null) {
            Toast.makeText(this, "Missing orderId or userId!", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("orders")
                .child(orderId)
                .child("cargoData");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                cargoList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    CargoItem item = ds.getValue(CargoItem.class);
                    if (item != null) cargoList.add(item);
                }

                if (!cargoList.isEmpty()) {
                    origin = cargoList.get(0).getOrigin();
                    destination = cargoList.get(0).getDestination();

                    calculateSummary();
                    updateMapAndDistance();
                } else {
                    Toast.makeText(ResultsActivity.this, "No cargo items found!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ResultsActivity.this, "Firebase error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAssignedTrucks() {

        String userId = getIntent().getStringExtra("userId");
        String orderId = getIntent().getStringExtra("orderId");

        if (userId == null || orderId == null) {
            Toast.makeText(this, "Missing userId/orderId!", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("orders")
                .child(orderId)
                .child("assignedTrucks");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot ds) {

                LinearLayout container = findViewById(R.id.truck_results_container);
                container.removeAllViews();

                if (!ds.exists()) {
                    addTextToContainer(container, "No truck assignments found.");
                    return;
                }

                List<AssignedTruck> oneTruckList = new ArrayList<>();

                for (DataSnapshot truckSnap : ds.getChildren()) {
                    AssignedTruck truck = truckSnap.getValue(AssignedTruck.class);
                    if (truck == null) continue;

                    // ⬅ Title
                    addTextToContainer(container,
                            truck.truckName +
                                    "\nTotal Weight: " + truck.totalWeight +
                                    " kg\nTotal Volume: " + truck.totalVolume + " Mm³");

                    // Items
                    if (truck.items != null) {
                        for (UnitItem item : truck.items) {
                            addTextToContainer(container,
                                    " • " + item.productName +
                                            " (W: " + item.weight +
                                            " KG, V: " + (item.realVolume/1000) + ")");
                        }
                    }
                    oneTruckList.add(truck);
                }
                displayTruckAssignments(oneTruckList);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ResultsActivity.this,
                        "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addTextToContainer(LinearLayout container, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15);
        tv.setTextColor(0xFF000000);
        tv.setPadding(0, 6, 0, 6);
        container.addView(tv);
    }

    @SuppressLint("SetTextI18n")
    private void displayTruckAssignments(List<AssignedTruck> assignedList) {

        LinearLayout container = findViewById(R.id.truck_results_container);
        container.removeAllViews();

        for (AssignedTruck truck : assignedList) {

            View card = getLayoutInflater().inflate(R.layout.item_cargo, container, false);

            TextView tvName = card.findViewById(R.id.tv_truck_name);
            TextView tvWeight = card.findViewById(R.id.tv_total_weight);
            TextView tvVolume = card.findViewById(R.id.tv_total_volume);
            LinearLayout itemsContainer = card.findViewById(R.id.items_container);

            tvName.setText( truck.truckName);
            tvWeight.setText("Total Weight: " + truck.totalWeight + " kg");
            tvVolume.setText("Total Volume: " + truck.totalVolume + " cm³");

            // Group items
            Map<String, GroupedItem> grouped = groupUnits(truck.items);

            for (GroupedItem g : grouped.values()) {
                TextView t = new TextView(this);
                t.setText("• " + g.name +
                        " — Qty: " + g.quantity +
                        " (W: " + g.weight + " KG, V: " + g.volume + " cm³)");
                t.setTextSize(14f);
                t.setTextColor(Color.DKGRAY);

                itemsContainer.addView(t);
            }

            container.addView(card);
        }
    }

    private static class GroupedItem {
        String name;
        double weight;
        double volume;
        int quantity;

        GroupedItem(String name, double weight, double volume) {
            this.name = name;
            this.weight = weight;
            this.volume = volume;
            this.quantity = 1;
        }
    }

    private Map<String, GroupedItem> groupUnits(List<UnitItem> units) {
        Map<String, GroupedItem> map = new HashMap<>();

        for (UnitItem u : units) {
            if (!map.containsKey(u.productName)) {
                map.put(u.productName, new GroupedItem(u.productName, u.weight, u.realVolume));
            } else {
                map.get(u.productName).quantity++;
            }
        }
        return map;
    }



    // ----------------------------------------------------------
    // CALCULATE SUMMARY
    // ----------------------------------------------------------
    @SuppressLint("SetTextI18n")
    private void calculateSummary() {
        int totalItems = 0;
        double totalWeight = 0;
        double totalVolume = 0;

        for (CargoItem c : cargoList) {
            totalItems += c.getQuantity();
            totalWeight += c.getWeight() * c.getQuantity();

            double volume = (c.getLength() / 100.0)
                    * (c.getWidth() / 100.0)
                    * (c.getHeight() / 100.0);

            totalVolume += volume * c.getQuantity();
        }

        tvSummaryTotalItems.setText("Total Items: " + totalItems);
        tvSummaryTotalWeight.setText("Total Weight: " + totalWeight + " kg");
        tvSummaryTotalVolume.setText("Total Volume: " + totalVolume + " m³");
    }

    // ----------------------------------------------------------
    // UPDATE MAP USING THE NEW HELPER (🔥 CLEAN)
    // ----------------------------------------------------------
    private void updateMapAndDistance() {

        if (mMap == null || origin.isEmpty() || destination.isEmpty())
            return;

        // 1) Parse origin / destination → LatLng
        LatLng from = mapHelper.parseLocation(origin);
        LatLng to   = mapHelper.parseLocation(destination);

        if (from != null || to != null) {
            mapHelper.drawRoute(mMap, from, to);

            // Distance Matrix API
            mapHelper.fetchDistanceFromAPI(origin, destination, result -> {
                if (result != null) {
                    tvRouteDistance.setText("Distance: " + result.getDistanceText());
                    tvRouteCost.setText("Cost: " + result.getEstimatedCost() + " SAR");
                }
            });
        };
    }
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        updateMapAndDistance();
    }

}
