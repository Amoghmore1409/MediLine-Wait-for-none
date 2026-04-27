package com.example.mediline;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mediline.model.Clinic;
import com.example.mediline.repository.ClinicRepository;
import com.example.mediline.util.SessionManager;

import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ClinicSetupActivity extends AppCompatActivity {

    private EditText nameInput, feeInput, addressInput, openTimeInput, closeTimeInput, averageTimeInput;
    private Spinner specialtySpinner;
    private ProgressBar progressBar;
    private ClinicRepository clinicRepo;
    private SessionManager session;
    private String existingClinicId;
    
    // We will extract coordinates dynamically upon save or via map picker
    private double currentLat = 0;
    private double currentLng = 0;

    private final ActivityResultLauncher<Intent> mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    currentLat = result.getData().getDoubleExtra("LATITUDE", 0);
                    currentLng = result.getData().getDoubleExtra("LONGITUDE", 0);
                    
                    // Reverse geocode to update address field if needed
                    updateAddressFromLocation(currentLat, currentLng);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_clinic_setup);

        clinicRepo = new ClinicRepository();
        session = new SessionManager(this);

        nameInput = findViewById(R.id.setup_clinic_name);
        feeInput = findViewById(R.id.setup_fee);
        addressInput = findViewById(R.id.setup_address);
        openTimeInput = findViewById(R.id.setup_open_time);
        closeTimeInput = findViewById(R.id.setup_close_time);
        averageTimeInput = findViewById(R.id.setup_average_time);
        specialtySpinner = findViewById(R.id.setup_specialty);
        progressBar = findViewById(R.id.setup_progress);

        // Specialty dropdown
        String[] specialties = {"General Practice", "Cardiology", "Pediatrics", "Dermatology", "Neurology", "Orthopedics"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, specialties);
        specialtySpinner.setAdapter(spinnerAdapter);

        // Back button
        findViewById(R.id.clinic_setup_back_btn).setOnClickListener(v -> finish());

        // Save button
        findViewById(R.id.setup_save_btn).setOnClickListener(v -> handleSaveClick());

        // Map Picker Trigger
        findViewById(R.id.btn_pick_location).setOnClickListener(v -> {
            Intent intent = new Intent(this, MapPickerActivity.class);
            intent.putExtra("LATITUDE", currentLat);
            intent.putExtra("LONGITUDE", currentLng);
            mapPickerLauncher.launch(intent);
        });

        // Load existing clinic data if any
        loadExistingClinic();
    }

    private void updateAddressFromLocation(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                addressInput.setText(addresses.get(0).getAddressLine(0));
            }
        } catch (IOException e) {
            Log.e("ClinicSetup", "Reverse geocoding failed", e);
        }
    }

    private void loadExistingClinic() {
        String userId = session.getUserId();
        if (userId == null) return;

        clinicRepo.getClinicByDoctor(userId, querySnapshot -> {
            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                var doc = querySnapshot.getDocuments().get(0);
                existingClinicId = doc.getId();
                Clinic clinic = doc.toObject(Clinic.class);

                if (clinic != null) {
                    nameInput.setText(clinic.getName());
                    addressInput.setText(clinic.getAddress());
                    feeInput.setText(String.valueOf((int) clinic.getConsultationFee()));
                    openTimeInput.setText(clinic.getOpeningTime());
                    closeTimeInput.setText(clinic.getClosingTime());
                    
                    if (clinic.getAverageVisitTimeMinutes() > 0) {
                        averageTimeInput.setText(String.valueOf(clinic.getAverageVisitTimeMinutes()));
                    } else {
                        averageTimeInput.setText("15");
                    }

                    currentLat = clinic.getLatitude();
                    currentLng = clinic.getLongitude();

                    // Set spinner selection
                    String spec = clinic.getSpecialization();
                    if (spec != null) {
                        for (int i = 0; i < specialtySpinner.getCount(); i++) {
                            if (spec.equals(specialtySpinner.getItemAtPosition(i).toString())) {
                                specialtySpinner.setSelection(i);
                                break;
                            }
                        }
                    }
                }
            }
        });
    }

    private void handleSaveClick() {
        String name = nameInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();

        if (name.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // If we haven't picked a location yet, try to geocode the address
        if (currentLat == 0 && currentLng == 0) {
            new Thread(() -> {
                double lat = 0;
                double lng = 0;
                
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocationName(name + " " + address, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        lat = addresses.get(0).getLatitude();
                        lng = addresses.get(0).getLongitude();
                    } else {
                        List<Address> addressFallback = geocoder.getFromLocationName(address, 1);
                        if (addressFallback != null && !addressFallback.isEmpty()) {
                            lat = addressFallback.get(0).getLatitude();
                            lng = addressFallback.get(0).getLongitude();
                        }
                    }
                } catch (IOException e) {
                    Log.e("ClinicSetup", "Geocoding failed", e);
                }

                final double finalLat = lat;
                final double finalLng = lng;

                runOnUiThread(() -> saveClinicWithCoordinates(name, address, finalLat, finalLng));
            }).start();
        } else {
            saveClinicWithCoordinates(name, address, currentLat, currentLng);
        }
    }

    private void saveClinicWithCoordinates(String name, String address, double lat, double lng) {
        String feeStr = feeInput.getText().toString().trim();
        String avgTimeStr = averageTimeInput.getText().toString().trim();
        String openTime = openTimeInput.getText().toString().trim();
        String closeTime = closeTimeInput.getText().toString().trim();
        String specialty = specialtySpinner.getSelectedItem().toString();

        double fee = 0;
        try { fee = Double.parseDouble(feeStr); } catch (NumberFormatException ignored) {}
        
        int avgTime = 15;
        try { avgTime = Integer.parseInt(avgTimeStr); } catch (NumberFormatException ignored) {}

        String userId = session.getUserId();
        Clinic clinic = new Clinic(userId, name, address, lat, lng, openTime, closeTime, fee, specialty, avgTime);

        if (existingClinicId != null) {
            // Update existing
            clinicRepo.updateClinic(existingClinicId, clinic, task -> {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Clinic updated!", Toast.LENGTH_SHORT).show();
                    navigateToDashboard();
                } else {
                    Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Create new
            clinicRepo.createClinic(clinic, task -> {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Clinic created!", Toast.LENGTH_SHORT).show();
                    navigateToDashboard();
                } else {
                    Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, DoctorDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
