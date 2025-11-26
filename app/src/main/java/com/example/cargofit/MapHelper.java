package com.example.cargofit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Scanner;

public class MapHelper {
    private Context context;
    private static final String API_KEY =  "AIzaSyD2FbQeBg7gJOq6Bi531U8B-YkwlP1V-jg";

    public MapHelper(Context context) {
        this.context = context;
    }

    public LatLng parseLocation(String locationName) {
        try {
            Geocoder geocoder = new Geocoder(context);
            List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
            if (addresses == null || addresses.isEmpty()) return null;
            Address address = addresses.get(0);
            return new LatLng(address.getLatitude(), address.getLongitude());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public void drawRoute(GoogleMap map, LatLng from, LatLng to) {
        if (map == null || from == null || to == null) return;

        map.addMarker(new MarkerOptions().position(from).title("Origin"));
        map.addMarker(new MarkerOptions().position(to).title("Destination"));

        map.addPolyline(new PolylineOptions().add(from, to).width(10).color(context.getResources().getColor(R.color.dark_blue)));

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(from);
        builder.include(to);

        map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(from, 5));

    }

    @SuppressLint("StaticFieldLeak")
    public void fetchDistanceFromAPI(String origin, String destination, DistanceCallback callback) {
        new AsyncTask<Void, Void, DistanceResult>() {
            @Override
            protected DistanceResult doInBackground(Void... voids) {
                try {
                    String urlStr = "https://maps.googleapis.com/maps/api/distancematrix/json?origins="
                            + origin.replace(" ", "+")
                            + "&destinations=" + destination.replace(" ", "+")
                            + "&units=metric&key=" + API_KEY;

                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.connect();

                    InputStream is = conn.getInputStream();
                    Scanner scanner = new Scanner(is);
                    StringBuilder sb = new StringBuilder();
                    while (scanner.hasNext()) sb.append(scanner.nextLine());
                    scanner.close();

                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray rows = json.getJSONArray("rows");
                    JSONObject elements = rows.getJSONObject(0).getJSONArray("elements").getJSONObject(0);

                    String distanceText = elements.getJSONObject("distance").getString("text");
                    double distanceKm = elements.getJSONObject("distance").getDouble("value") / 1000.0;

                    double estimatedCost = distanceKm * 2.5; // معدل ثابت مثالياً يمكن تعديله

                    return new DistanceResult(distanceText, estimatedCost);

                } catch (Exception e) {
                    Log.e("DistanceMatrixAPI", "Error fetching distance", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(DistanceResult result) {
                if (callback != null) callback.onResult(result);
            }
        }.execute();
    }

    public interface DistanceCallback {
        void onResult(DistanceResult result);
    }

    public static class DistanceResult {
        private String distanceText;
        private double estimatedCost;

        public DistanceResult(String distanceText, double estimatedCost) {
            this.distanceText = distanceText;
            this.estimatedCost = estimatedCost;
        }

        public String getDistanceText() {
            return distanceText;
        }

        public double getEstimatedCost() {
            return estimatedCost;
        }
    }
}
