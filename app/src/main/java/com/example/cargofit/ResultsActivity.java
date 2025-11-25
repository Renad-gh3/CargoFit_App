package com.example.cargofit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
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
import java.util.List;

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

    }

    // ----------------------------------------------------------
    // LOAD DATA FROM FIREBASE
    // ----------------------------------------------------------
    private void loadCargoDataFromFirebase() {

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("cargoData");

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
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ResultsActivity.this, "Firebase error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
