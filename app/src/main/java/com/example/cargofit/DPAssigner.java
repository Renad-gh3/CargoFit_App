package com.example.cargofit;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class DPAssigner {

    private static final String TAG = "DPAssigner";

    // Used to scale down volumes so DP array is not too large
    public static final int SCALE = 1000;

    // Safety limits to prevent memory overflow on mobile devices
    private static final long MAX_DP_CELLS = 5_000_000L;
    private static final int MAX_UNIT_EXPANSION = 2500;

    /**
     * MAIN METHOD
     * Starts the assignment process in a background thread
     * Expands cargo -> selects best trucks -> saves results to Firebase
     */
    public static void assignAndSave(Context ctx, List<CargoItem> cargoList, List<Truck> trucks, String userId,
                                     String orderId) {
        new Thread(() -> {
            try {

                DatabaseReference baseRef = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(userId)
                        .child("orders")
                        .child(orderId);

                // Step 1: Convert cargo list into unit-level items
                List<UnitItem> units = expandToUnits(cargoList, SCALE);

                if (units.isEmpty()) {
                    runOnUiThreadToast(ctx, "No units to assign");
                    return;
                }

                // Step 2: If too many items, use Greedy algorithm instead of DP
                if (units.size() > MAX_UNIT_EXPANSION) {
                    Log.w(TAG, "Too many units, switching to Greedy");
                    List<AssignedTruck> assigned = greedyAssign(units, trucks, SCALE);
                    saveAssignedListToFirebase(ctx, assigned, userId, orderId);
                    return;
                }

                // Step 3: Calculate each truck's capacity
                List<TruckCapacity> capacities = new ArrayList<>();
                for (Truck t : trucks) {
                    double realVol = (double) t.height * t.length * t.width;
                    int scaledVol = Math.max(1, (int) Math.round(realVol / (double) SCALE));
                    int maxW = (int) Math.round(t.maxWeight);

                    capacities.add(new TruckCapacity(t, maxW, scaledVol, realVol));
                }

                // Step 4: Sort trucks from smallest to largest
                Collections.sort(capacities,
                        Comparator.comparingInt((TruckCapacity tc) -> tc.scaledVolume)
                                .thenComparingInt(tc -> tc.maxWeight));

                // Step 5: Start assignment loop
                List<UnitItem> remaining = new ArrayList<>(units);
                List<AssignedTruck> results = new ArrayList<>();

                while (!remaining.isEmpty() && !capacities.isEmpty()) {

                    // Best candidate tracking
                    TruckCapacity bestTruck = null;
                    List<UnitItem> bestSelected = new ArrayList<>();
                    double bestVolume = 0.0;

                    // Try each truck
                    for (TruckCapacity tc : capacities) {
                        int maxW = tc.maxWeight;
                        int maxV = tc.scaledVolume;
                        long cells = (long) (maxW + 1) * (long) (maxV + 1);

                        // Choose DP or Greedy based on size
                        List<UnitItem> selected = (cells <= MAX_DP_CELLS)
                                ? run2DDP(remaining, maxW, maxV)
                                : runGreedyOnRemaining(remaining, maxW, maxV);

                        // Calculate total volume for this truck
                        double vol = 0.0;
                        for (UnitItem u : selected) vol += u.realVolume;

                        // Pick truck that loads the most volume
                        if (!selected.isEmpty() && vol > bestVolume) {
                            bestVolume = vol;
                            bestTruck = tc;
                            bestSelected = selected;
                        }
                    }

                    // Stop if no truck fits
                    if (bestTruck == null) break;

                    // Calculate total weight & volume of selected items
                    double totalWeight = 0.0;
                    double totalVolume = 0.0;
                    for (UnitItem u : bestSelected) {
                        totalWeight += u.weight;
                        totalVolume += u.realVolume;
                    }

                    // Save this truck assignment
                    results.add(new AssignedTruck(
                            bestTruck.truck.truckId,
                            bestTruck.truck.truckName,
                            totalWeight,
                            totalVolume,
                            bestSelected
                    ));

                    // Remove loaded items
                    removeSelectedFromRemaining(remaining, bestSelected);

                    // Remove used truck so it's not reused
                    capacities.remove(bestTruck);
                }

                // Step 6: Save any unassigned items
                if (!remaining.isEmpty()) {
                    DatabaseReference db = FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(userId)
                            .child("orders")
                            .child(orderId)
                            .child("unassignedUnits");

                    for (UnitItem u : remaining) {
                        String key = db.push().getKey();
                        if (key != null) db.child(key).setValue(u);
                    }
                }

                // Step 7: Save final assignments to Firebase
                saveAssignedListToFirebase(ctx, results, userId, orderId);

            } catch (Exception e) {
                Log.e(TAG, "Assignment error", e);
                runOnUiThreadToast(ctx, "Error: " + e.getMessage());
            }
        }).start();
    }

    // ================= Helper Data Class =================

    // Holds calculated truck capacity info
    private static class TruckCapacity {
        Truck truck;
        int maxWeight;
        int scaledVolume;
        double realVolumeCapacity;

        TruckCapacity(Truck t, int maxWeight, int scaledVolume, double realVolumeCapacity) {
            this.truck = t;
            this.maxWeight = Math.max(1, maxWeight);
            this.scaledVolume = Math.max(1, scaledVolume);
            this.realVolumeCapacity = realVolumeCapacity;
        }
    }

    // ================= Core Logic =================

    /**
     * Converts Cargo items into Unit items (based on quantity)
     */
    private static List<UnitItem> expandToUnits(List<CargoItem> cargos, int scale) {
        List<UnitItem> units = new ArrayList<>();

        for (CargoItem c : cargos) {
            int qty = c.getQuantity();
            if (qty <= 0) continue;

            double realVol = c.getLength() * c.getWidth() * c.getHeight();
            int scaledVol = Math.max(1, (int) Math.round(realVol / (double) scale));

            for (int i = 0; i < qty; i++) {
                String uid = (c.id != null ? c.id : c.getProductName()) + "_" + i;

                units.add(new UnitItem(
                        uid,
                        c.id,
                        c.getProductName(),
                        c.getWeight(),
                        scaledVol,
                        realVol,
                        c.getOrigin(),
                        c.getDestination(),
                        c.getType()
                ));
            }
        }

        return units;
    }

    /**
     * Dynamic Programming 0/1 Knapsack (Weight + Volume)
     */
    private static List<UnitItem> run2DDP(List<UnitItem> items, int maxW, int maxV) {
        int n = items.size();
        int[] w = new int[n];
        int[] v = new int[n];
        int[] val = new int[n];

        for (int i = 0; i < n; i++) {
            w[i] = Math.max(1, (int) Math.round(items.get(i).weight));
            v[i] = Math.max(1, items.get(i).scaledVolume);
            val[i] = (int) Math.round(items.get(i).realVolume);
        }

        int[][] dp = new int[maxW + 1][maxV + 1];
        int[][] prev = new int[maxW + 1][maxV + 1];

        for (int i = 0; i <= maxW; i++) {
            for (int j = 0; j <= maxV; j++) {
                prev[i][j] = -1;
            }
        }

        for (int idx = 0; idx < n; idx++) {
            for (int cw = maxW; cw >= w[idx]; cw--) {
                for (int cv = maxV; cv >= v[idx]; cv--) {
                    int candidate = dp[cw - w[idx]][cv - v[idx]] + val[idx];
                    if (candidate > dp[cw][cv]) {
                        dp[cw][cv] = candidate;
                        prev[cw][cv] = idx;
                    }
                }
            }
        }

        List<UnitItem> selected = new ArrayList<>();
        int cw = maxW, cv = maxV;

        while (cw >= 0 && cv >= 0) {
            int idx = prev[cw][cv];
            if (idx == -1) break;
            selected.add(items.get(idx));
            cw -= w[idx];
            cv -= v[idx];
        }

        return selected;
    }

    /**
     * Greedy fallback algorithm if DP is too big
     */
    private static List<UnitItem> runGreedyOnRemaining(List<UnitItem> items, int maxW, int maxV) {
        List<UnitItem> sorted = new ArrayList<>(items);

        Collections.sort(sorted, (a, b) -> {
            int cmp = Integer.compare(b.scaledVolume, a.scaledVolume);
            return (cmp != 0) ? cmp : Double.compare(b.weight, a.weight);
        });

        List<UnitItem> selected = new ArrayList<>();
        int wUsed = 0, vUsed = 0;

        for (UnitItem u : sorted) {
            int wi = Math.max(1, (int) Math.round(u.weight));
            int vi = Math.max(1, u.scaledVolume);

            if (wUsed + wi <= maxW && vUsed + vi <= maxV) {
                selected.add(u);
                wUsed += wi;
                vUsed += vi;
            }
        }

        return selected;
    }

    /**
     * Greedy full assignment
     */
    private static List<AssignedTruck> greedyAssign(List<UnitItem> units, List<Truck> trucks, int scale) {
        List<AssignedTruck> result = new ArrayList<>();
        List<UnitItem> remaining = new ArrayList<>(units);

        List<TruckCapacity> caps = new ArrayList<>();
        for (Truck t : trucks) {
            double vol = (double) t.height * t.length * t.width;
            int scaledVol = Math.max(1, (int) Math.round(vol / (double) scale));
            int maxW = (int) Math.round(t.maxWeight);

            caps.add(new TruckCapacity(t, maxW, scaledVol, vol));
        }

        Collections.sort(caps, Comparator.comparingInt(tc -> tc.scaledVolume));

        for (TruckCapacity tc : caps) {
            List<UnitItem> sel = runGreedyOnRemaining(remaining, tc.maxWeight, tc.scaledVolume);

            double w = 0, v = 0;
            for (UnitItem u : sel) {
                w += u.weight;
                v += u.realVolume;
            }

            result.add(new AssignedTruck(tc.truck.truckId, tc.truck.truckName, w, v, sel));
            removeSelectedFromRemaining(remaining, sel);

            if (remaining.isEmpty()) break;
        }

        return result;
    }

    /**
     * Removes assigned items from remaining list
     */
    private static void removeSelectedFromRemaining(List<UnitItem> remaining, List<UnitItem> selected) {
        HashMap<String, Integer> map = new HashMap<>();

        for (UnitItem u : selected) {
            map.put(u.unitId, map.getOrDefault(u.unitId, 0) + 1);
        }

        List<UnitItem> newList = new ArrayList<>();

        for (UnitItem r : remaining) {
            if (map.containsKey(r.unitId) && map.get(r.unitId) > 0) {
                map.put(r.unitId, map.get(r.unitId) - 1);
            } else {
                newList.add(r);
            }
        }

        remaining.clear();
        remaining.addAll(newList);
    }

    // ================= Firebase Saving =================

    private static void saveAssignedListToFirebase(Context ctx, List<AssignedTruck> results, String userId,
                                                   String orderId) {

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("orders")
                .child(orderId)
                .child("assignedTrucks");

        for (AssignedTruck at : results) {
            ref.child(at.truckId).setValue(at);
        }

        runOnUiThreadToast(ctx, "Assignments saved successfully");

        new android.os.Handler(ctx.getMainLooper()).post(() -> {
            android.content.Intent intent =
                    new android.content.Intent(ctx, ResultsActivity.class);

            intent.putExtra("orderId", orderId);
            intent.putExtra("userId", userId);

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
        });
    }


    private static void runOnUiThreadToast(Context ctx, String message) {
        if (ctx == null) return;

        new android.os.Handler(ctx.getMainLooper()).post(
                () -> Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
        );
    }
}
