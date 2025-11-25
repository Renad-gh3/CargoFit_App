package com.example.cargofit;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.firebase.auth.FirebaseAuth;

public class UploadExcelActivity extends AppCompatActivity {

    private static final int PICK_CSV_FILE = 101;

    private ImageView uploadIcon;
    private TextView uploadText, tvFileName;
    private MaterialButton btnCalculate;
    private View progressBar, uploadBox;

    private Uri selectedFileUri;
    private DatabaseReference databaseReference;
    private List<CargoItem> cargoList;
    private String currentOrderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_excel);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initViews();
        setupClickListeners();
        setContentView(R.layout.activity_results);
    }

    private void initViews() {
        uploadBox = findViewById(R.id.uploadBox);
        uploadIcon = findViewById(R.id.uploadIcon);
        uploadText = findViewById(R.id.uploadText);
        tvFileName = findViewById(R.id.tvFileName);
        btnCalculate = findViewById(R.id.btnCalculate);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        uploadBox.setOnClickListener(v -> openFilePicker());
        btnCalculate.setOnClickListener(v -> {
            if (selectedFileUri != null) {
                new ReadCsvTask().execute();
            } else {
                Toast.makeText(this, "Please select a CSV file first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // تغيير إلى جميع الملفات
        String[] mimeTypes = {"text/csv", "application/vnd.ms-excel", "text/comma-separated-values"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, PICK_CSV_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_CSV_FILE && resultCode == RESULT_OK) {
            if (data != null) {
                selectedFileUri = data.getData();
                updateUIAfterFileSelection();
            }
        }
    }

    private void updateUIAfterFileSelection() {
        String fileName = getFileName(selectedFileUri);
        tvFileName.setText("Selected: " + fileName);

        uploadIcon.setImageResource(R.drawable.ic_checkmark);
        uploadText.setText("File Ready!");
        btnCalculate.setEnabled(true);
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (var cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow("_display_name"));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private class ReadCsvTask extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            btnCalculate.setEnabled(false);
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
                if (inputStream == null) return "Unable to open file";

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String headerLine = reader.readLine();

                if (headerLine == null) return "File is empty";

                // تحسين تقسيم CSV مع التعامل مع الفاصلة داخل النصوص
                String[] headers = parseCsvLine(headerLine);

                // التحقق من الأعمدة بطريقة مرنة
                String validationResult = validateHeaders(headers);
                if (!validationResult.equals("SUCCESS")) {
                    reader.close();
                    return validationResult;
                }

                // إنشاء خريطة لتحديد مواقع الأعمدة
                Map<String, Integer> columnMap = createColumnMap(headers);

                cargoList = new ArrayList<>();
                String line;
                int rowNum = 2;
                StringBuilder errors = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue; // تخطي الأسطر الفارغة

                    String[] values = parseCsvLine(line);
                    if (values.length < 9) {
                        errors.append("Row ").append(rowNum).append(": Not enough columns (expected 9, found ").append(values.length).append(")\n");
                        rowNum++;
                        continue;
                    }

                    String rowValidation = validateRowData(values, rowNum, columnMap);
                    if (!rowValidation.equals("SUCCESS")) {
                        errors.append(rowValidation).append("\n");
                        rowNum++;
                        continue;
                    }

                    try {
                        CargoItem cargo = createCargoFromRow(values, columnMap);
                        if (cargo != null) {
                            cargoList.add(cargo);
                        }
                    } catch (Exception e) {
                        errors.append("Row ").append(rowNum).append(": Error creating cargo item - ").append(e.getMessage()).append("\n");
                    }
                    rowNum++;
                }

                reader.close();
                inputStream.close();

                if (cargoList.isEmpty()) {
                    return "No valid data found in the file";
                }

                if (errors.length() > 0) {
                    return "Processed " + cargoList.size() + " items with errors:\n" + errors.toString();
                }

                return "SUCCESS: " + cargoList.size() + " items processed successfully";

            } catch (Exception e) {
                Log.e("CSVError", "Error reading CSV", e);
                return "CSV Read Error: " + e.getMessage();
            }
        }

        private String[] parseCsvLine(String line) {
            List<String> values = new ArrayList<>();
            StringBuilder currentValue = new StringBuilder();
            boolean inQuotes = false;

            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);

                if (c == '"') {
                    inQuotes = !inQuotes;
                } else if (c == ',' && !inQuotes) {
                    values.add(currentValue.toString().trim());
                    currentValue = new StringBuilder();
                } else {
                    currentValue.append(c);
                }
            }
            values.add(currentValue.toString().trim());

            return values.toArray(new String[0]);
        }

        private String validateHeaders(String[] headers) {
            if (headers.length < 9) {
                return "File has only " + headers.length + " columns. Expected 9 columns.";
            }

            // الأعمدة المطلوبة بطريقة مرنة
            String[] requiredColumns = {
                    "product name", "quantity", "weight",
                    "length", "width", "height",
                    "type", "origin", "destination"
            };

            Map<String, Integer> columnMap = createColumnMap(headers);

            StringBuilder missingColumns = new StringBuilder();
            for (String required : requiredColumns) {
                if (!columnMap.containsKey(required)) {
                    if (missingColumns.length() > 0) missingColumns.append(", ");
                    missingColumns.append(required);
                }
            }

            if (missingColumns.length() > 0) {
                return "Missing or incorrect columns: " + missingColumns.toString() +
                        "\nFound columns: " + String.join(", ", headers);
            }

            return "SUCCESS";
        }

        private Map<String, Integer> createColumnMap(String[] headers) {
            Map<String, Integer> columnMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                String cleanHeader = headers[i].toLowerCase().replace("(kg)", "").replace("(cm)", "").replaceAll("[^a-z]", " ").trim();
                columnMap.put(cleanHeader, i);
            }
            return columnMap;
        }

        private String validateRowData(String[] values, int rowNum, Map<String, Integer> columnMap) {
            try {
                // Product Name
                String productName = values[columnMap.get("product name")].trim();
                if (productName.isEmpty() || isNumeric(productName)) {
                    return "Row " + rowNum + ": Invalid Product Name";
                }

                // Quantity
                int quantity = Integer.parseInt(values[columnMap.get("quantity")].trim());
                if (quantity < 1) {
                    return "Row " + rowNum + ": Invalid Quantity (must be ≥ 1)";
                }

                // Weight
                double weight = Double.parseDouble(values[columnMap.get("weight")].trim());
                if (weight <= 0) {
                    return "Row " + rowNum + ": Invalid Weight (must be > 0)";
                }

                // Dimensions
                double length = Double.parseDouble(values[columnMap.get("length")].trim());
                if (length <= 0) return "Row " + rowNum + ": Invalid Length (must be > 0)";

                double width = Double.parseDouble(values[columnMap.get("width")].trim());
                if (width <= 0) return "Row " + rowNum + ": Invalid Width (must be > 0)";

                double height = Double.parseDouble(values[columnMap.get("height")].trim());
                if (height <= 0) return "Row " + rowNum + ": Invalid Height (must be > 0)";

                // Type, Origin, Destination
                String type = values[columnMap.get("type")].trim();
                if (type.isEmpty() || isNumeric(type)) {
                    return "Row " + rowNum + ": Invalid Type";
                }

                String origin = values[columnMap.get("origin")].trim();
                if (origin.isEmpty()) {
                    return "Row " + rowNum + ": Invalid Origin";
                }

                String destination = values[columnMap.get("destination")].trim();
                if (destination.isEmpty()) {
                    return "Row " + rowNum + ": Invalid Destination";
                }

                return "SUCCESS";
            } catch (NumberFormatException e) {
                return "Row " + rowNum + ": Invalid number format";
            } catch (Exception e) {
                return "Row " + rowNum + ": Validation error - " + e.getMessage();
            }
        }

        private CargoItem createCargoFromRow(String[] values, Map<String, Integer> columnMap) {
            try {
                return new CargoItem(
                        values[columnMap.get("product name")].trim(),
                        Integer.parseInt(values[columnMap.get("quantity")].trim()),
                        Double.parseDouble(values[columnMap.get("weight")].trim()),
                        Double.parseDouble(values[columnMap.get("length")].trim()),
                        Double.parseDouble(values[columnMap.get("width")].trim()),
                        Double.parseDouble(values[columnMap.get("height")].trim()),
                        values[columnMap.get("type")].trim(),
                        values[columnMap.get("origin")].trim(),
                        values[columnMap.get("destination")].trim()
                );
            } catch (Exception e) {
                Log.e("CargoCreation", "Error creating cargo from row", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressBar.setVisibility(View.GONE);

            if (result.startsWith("SUCCESS")) {
                uploadToFirebase();
                Toast.makeText(UploadExcelActivity.this, result, Toast.LENGTH_LONG).show();
            } else {
                btnCalculate.setEnabled(true);
                // عرض رسالة الخطأ مع تفاصيل أكثر
                if (result.contains("Missing or incorrect columns")) {
                    // عرض الأعمدة المطلوبة مقابل الموجودة
                    Toast.makeText(UploadExcelActivity.this, result, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(UploadExcelActivity.this, result, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void uploadToFirebase() {
        if (cargoList == null || cargoList.isEmpty()) return;

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ordersRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("orders");

        // احسب orderId مرة وحدة فقط
        ordersRef.get().addOnSuccessListener(snapshot -> {
            long orderCount = snapshot.getChildrenCount() + 1;
            currentOrderId = "order_" + orderCount;

            DatabaseReference cargoRef = ordersRef
                    .child(currentOrderId)
                    .child("cargoData");

            for (CargoItem cargo : cargoList) {
                String key = cargoRef.push().getKey();
                if (key != null) {
                    cargo.id = key;
                    cargoRef.child(key).setValue(cargo);
                }
            }

            Toast.makeText(this, "Data uploaded successfully!", Toast.LENGTH_LONG).show();

            // مرري نفس orderId مباشرة للخوارزمية
            fetchTrucksAndAssignProducts();
        });
    }



    private void fetchTrucksAndAssignProducts() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference trucksRef = FirebaseDatabase.getInstance().getReference("trucks");

        trucksRef.get().addOnSuccessListener(trucksSnapshot -> {
            List<Truck> trucks = new ArrayList<>();
            for (var child : trucksSnapshot.getChildren()) {
                Truck truck = child.getValue(Truck.class);
                if (truck != null) trucks.add(truck);
            }

            // استخدمي نفس orderId المحسوب سابقًا
            DPAssigner.assignAndSave(
                    UploadExcelActivity.this,
                    cargoList,
                    trucks,
                    userId,
                    currentOrderId
            );
        });
    }




    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}