package com.example.mediline;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mediline.model.Appointment;
import com.example.mediline.repository.AppointmentRepository;
import com.example.mediline.service.QueueNotificationService;
import com.example.mediline.util.SessionManager;
import com.example.mediline.util.UiUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.ListenerRegistration;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import android.net.Uri;
import android.widget.ImageView;
import android.view.View;
import android.app.ProgressDialog;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ClinicDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private AppointmentRepository appointmentRepo;
    private SessionManager session;
    private String clinicId;
    private String clinicName;
    private double clinicLatitude;
    private double clinicLongitude;
    private ListenerRegistration queueListener;
    private GoogleMap mMap;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (savedInstanceState != null) {
            // No state to restore right now
        }
        
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_clinic_details);

        appointmentRepo = new AppointmentRepository();
        session = new SessionManager(this);

        // Get clinic data from intent
        clinicId = getIntent().getStringExtra("CLINIC_ID");
        clinicName = getIntent().getStringExtra("CLINIC_NAME");
        String clinicAddress = getIntent().getStringExtra("CLINIC_ADDRESS");
        String specialty = getIntent().getStringExtra("CLINIC_SPECIALTY");
        double fee = getIntent().getDoubleExtra("CLINIC_FEE", 0);
        String openTime = getIntent().getStringExtra("CLINIC_OPEN");
        String closeTime = getIntent().getStringExtra("CLINIC_CLOSE");
        clinicLatitude = getIntent().getDoubleExtra("CLINIC_LATITUDE", 0);
        clinicLongitude = getIntent().getDoubleExtra("CLINIC_LONGITUDE", 0);

        // Load map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.clinic_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Populate UI
        TextView nameView = findViewById(R.id.clinic_detail_name);
        TextView descView = findViewById(R.id.clinic_detail_desc);
        TextView doctorName = findViewById(R.id.clinic_doctor_name);
        TextView doctorSpec = findViewById(R.id.clinic_doctor_spec);
        TextView doctorDesc = findViewById(R.id.clinic_doctor_desc);
        TextView feeView = findViewById(R.id.clinic_fee);
        TextView nextSlot = findViewById(R.id.clinic_next_slot);
        TextView hoursWeekday = findViewById(R.id.clinic_hours_weekday);
        TextView queueNumber = findViewById(R.id.clinic_queue_number);
        TextView estWait = findViewById(R.id.clinic_est_wait);

        nameView.setText(clinicName != null ? clinicName : "Clinic");
        descView.setText(specialty != null ? specialty + " • Advanced Care" : "Medical Center");
        doctorName.setText("Dr. Specialist, MD");
        doctorSpec.setText(specialty != null ? specialty + " • Experienced" : "General Practice");
        doctorDesc.setText("Specializing in advanced diagnostics and patient care. Leading the clinical research division.");
        feeView.setText(String.format("₹%.0f", fee));
        nextSlot.setText("Today");
        hoursWeekday.setText((openTime != null ? openTime : "08:00") + " — " + (closeTime != null ? closeTime : "17:00"));

        // Load queue data with real-time listener
        if (clinicId != null) {
            queueListener = appointmentRepo.listenToQueue(clinicId, (querySnapshot, error) -> {
                if (error != null) {
                    // Error loading queue, but don't crash
                    return;
                }

                if (querySnapshot != null) {
                    List<Appointment> queue = querySnapshot.toObjects(Appointment.class);
                    // Filter to WAITING and sort locally
                    queue.removeIf(appt -> !"WAITING".equals(appt.getStatus()));
                    queue.sort(java.util.Comparator.comparingInt(Appointment::getTokenNumber));
                    
                    int currentServing = queue.isEmpty() ? 0 : queue.get(0).getTokenNumber();
                    queueNumber.setText("#" + currentServing);
                    estWait.setText("Est. wait: " + (queue.size() * 5) + " mins");

                    ProgressBar progressBar = findViewById(R.id.clinic_queue_progress);
                    if (!queue.isEmpty()) {
                        progressBar.setProgress((int) ((double) currentServing / (currentServing + queue.size()) * 100));
                    }
                }
            });
        }

        // Back button
        findViewById(R.id.clinic_back_btn).setOnClickListener(v -> finish());

        // Book Appointment
        findViewById(R.id.clinic_book_btn).setOnClickListener(v -> bookAppointment());

        // Open in Maps App
        findViewById(R.id.btn_open_maps).setOnClickListener(v -> {
            if (clinicLatitude != 0 && clinicLongitude != 0) {
                String uri = "geo:" + clinicLatitude + "," + clinicLongitude + "?q=" + clinicLatitude + "," + clinicLongitude;
                if (clinicName != null) {
                    uri += "(" + Uri.encode(clinicName) + ")";
                }
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    // Fallback if maps app not installed
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
                }
            } else {
                UiUtils.showErrorDialog(this, "Location unavailable", "This clinic hasn't set their exact map coordinates yet.");
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (clinicLatitude != 0 && clinicLongitude != 0) {
            LatLng location = new LatLng(clinicLatitude, clinicLongitude);
            mMap.addMarker(new MarkerOptions().position(location).title(clinicName != null ? clinicName : "Clinic Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
            
            // Disable interactions to use it just for visual context since it's in a ScrollView
            mMap.getUiSettings().setScrollGesturesEnabled(false);
            mMap.getUiSettings().setZoomGesturesEnabled(false);
            mMap.setOnMapClickListener(latLng -> findViewById(R.id.btn_open_maps).performClick());
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void bookAppointment() {
        if (clinicId == null) {
            UiUtils.showErrorDialog(this, "Error", "Clinic ID not found");
            return;
        }

        String patientId = session.getUserId();
        String patientName = session.getUserName();

        if (patientId == null || patientId.isEmpty()) {
            UiUtils.showErrorDialog(this, "Error", "Patient ID not found. Please login again.");
            return;
        }

        // Check if patient already has an active appointment at this clinic
        appointmentRepo.getPatientActiveAppointmentAtClinic(patientId, clinicId, existingAppointments -> {
            if (existingAppointments != null && !existingAppointments.isEmpty()) {
                Appointment activeAppt = null;
                for (var doc : existingAppointments.getDocuments()) {
                    Appointment appt = doc.toObject(Appointment.class);
                    if (appt != null && ("WAITING".equals(appt.getStatus()) || "IN_PROGRESS".equals(appt.getStatus()))) {
                        activeAppt = appt;
                        break;
                    }
                }
                
                if (activeAppt != null) {
                    // Patient already has an active appointment
                    UiUtils.showWarningDialog(this,
                        "Active Appointment",
                        "You already have an active appointment (Token #" + activeAppt.getTokenNumber() +
                        "). Complete it before booking again.");
                    return;
                }
            }

            // No active appointment, proceed with booking
            proceedWithBooking(patientId, patientName);
        }).addOnFailureListener(e -> {
            UiUtils.showErrorDialog(this, "Error", "Error checking appointments: " + e.getMessage());
        });
    }

    private void proceedWithBooking(String patientId, String patientName) {
        finalizeBooking(patientId, patientName);
    }

    private void finalizeBooking(String patientId, String patientName) {
        // Get next token number
        appointmentRepo.getNextTokenNumber(clinicId, querySnapshot -> {
            int nextToken = 1;
            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                Appointment last = querySnapshot.getDocuments().get(0).toObject(Appointment.class);
                if (last != null) {
                    nextToken = last.getTokenNumber() + 1;
                }
            }

            Appointment appointment = new Appointment(
                    clinicId, nextToken, "WAITING", "APP",
                    patientId, patientName, "Consultation"
            );

            appointmentRepo.createAppointment(appointment, task -> {
                if (task.isSuccessful()) {
                    UiUtils.showSuccessDialog(this, "Booking Successful", "You have been booked!\n\nToken #" + appointment.getTokenNumber());

                    // Start Tracking Service immediately
                    Intent serviceIntent = new Intent(this, QueueNotificationService.class);
                    serviceIntent.putExtra("CLINIC_ID", clinicId);
                    serviceIntent.putExtra("PATIENT_ID", patientId);
                    serviceIntent.putExtra("APPOINTMENT_ID", task.getResult().getId());
                    serviceIntent.putExtra("TOKEN_NUMBER", appointment.getTokenNumber());
                    startService(serviceIntent);

                    Intent intent = new Intent(this, QueueStatusActivity.class);
                    intent.putExtra("TOKEN_NUMBER", appointment.getTokenNumber());
                    intent.putExtra("CLINIC_ID", clinicId);
                    intent.putExtra("CLINIC_NAME", getIntent().getStringExtra("CLINIC_NAME"));
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } else {
                    UiUtils.showErrorDialog(this, "Booking Failed", "Booking failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                }
            });
        }).addOnFailureListener(e -> {
            UiUtils.showErrorDialog(this, "Error", "Failed to get token number: " + e.getMessage());
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listener to prevent memory leaks
        if (queueListener != null) {
            queueListener.remove();
        }
    }
}
