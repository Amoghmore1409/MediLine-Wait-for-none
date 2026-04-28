package com.example.mediline;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediline.adapter.ClinicAdapter;
import com.example.mediline.model.Clinic;
import com.example.mediline.repository.ClinicRepository;
import com.example.mediline.util.SessionManager;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.example.mediline.repository.AppointmentRepository;
import com.example.mediline.model.Appointment;
import com.example.mediline.model.User;
import com.example.mediline.repository.UserRepository;
import com.example.mediline.service.QueueNotificationService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import android.app.Dialog;

public class PatientHomeActivity extends AppCompatActivity implements ClinicAdapter.OnClinicClickListener {

    private RecyclerView clinicList;
    private ClinicAdapter adapter;
    private List<Clinic> clinics = new ArrayList<>();
    private List<Clinic> allClinics = new ArrayList<>();
    private ClinicRepository clinicRepo;
    private AppointmentRepository appointmentRepo;
    private UserRepository userRepo;
    private SessionManager session;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentUserLocation;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1002;
    private String selectedCategory = "All";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_patient_home);

        clinicRepo = new ClinicRepository();
        appointmentRepo = new AppointmentRepository();
        userRepo = new UserRepository();
        session = new SessionManager(this);

        clinicList = findViewById(R.id.patient_clinic_list);
        clinicList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ClinicAdapter(clinics, this);
        clinicList.setAdapter(adapter);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        findViewById(R.id.patient_profile_btn).setOnClickListener(v -> 
                startActivity(new Intent(this, PatientProfileActivity.class)));

        setupSearch();
        setupCategories();
        setupBottomNav();
        loadClinics();
        checkNotificationPermission();
        checkForActiveAppointmentToTrack();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void checkForActiveAppointmentToTrack() {
        appointmentRepo.getPatientAppointments(session.getUserId(), querySnapshot -> {
            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                for (var doc : querySnapshot.getDocuments()) {
                    Appointment appt = doc.toObject(Appointment.class);
                    if (appt != null && ("WAITING".equals(appt.getStatus()) || "IN_PROGRESS".equals(appt.getStatus()))) {
                        appt.setAppointmentId(doc.getId());
                        startTrackingService(appt);
                        break;
                    }
                }
            }
        });
    }

    private void startTrackingService(Appointment active) {
        Intent serviceIntent = new Intent(this, QueueNotificationService.class);
        serviceIntent.putExtra("CLINIC_ID", active.getClinicId());
        serviceIntent.putExtra("PATIENT_ID", active.getPatientId());
        serviceIntent.putExtra("APPOINTMENT_ID", active.getAppointmentId());
        serviceIntent.putExtra("TOKEN_NUMBER", active.getTokenNumber());
        startService(serviceIntent);
    }

    private void setupSearch() {
        EditText searchInput = findViewById(R.id.patient_search);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                filterClinicsByCategory();
            }
        });
    }

    private void setupCategories() {
        android.widget.TextView all = findViewById(R.id.category_all);
        android.widget.TextView gen = findViewById(R.id.category_general);
        android.widget.TextView ped = findViewById(R.id.category_pediatrics);
        android.widget.TextView den = findViewById(R.id.category_dentistry);
        android.widget.TextView car = findViewById(R.id.category_cardiology);
        android.widget.TextView der = findViewById(R.id.category_dermatology);
        android.widget.TextView neu = findViewById(R.id.category_neurology);
        android.widget.TextView ort = findViewById(R.id.category_orthopedics);

        android.widget.TextView[] categories = {all, gen, ped, den, car, der, neu, ort};

        for (android.widget.TextView tv : categories) {
            tv.setOnClickListener(v -> {
                // reset all to inactive
                for (android.widget.TextView cat : categories) {
                    cat.setBackgroundResource(R.drawable.bg_category_inactive);
                    cat.setTextColor(getResources().getColor(R.color.on_surface_variant, getTheme()));
                }
                
                // set current to active
                tv.setBackgroundResource(R.drawable.bg_category_active);
                tv.setTextColor(getResources().getColor(R.color.white, getTheme()));
                
                selectedCategory = tv.getText().toString();
                filterClinicsByCategory();
            });
        }
    }

    private void filterClinicsByCategory() {
        clinics.clear();
        for (Clinic c : allClinics) {
            String spec = c.getSpecialization() != null ? c.getSpecialization().toLowerCase() : "";
            String name = c.getName() != null ? c.getName().toLowerCase() : "";
            String address = c.getAddress() != null ? c.getAddress().toLowerCase() : "";
            String cat = selectedCategory.toLowerCase();
            
            boolean matchesCategory = false;
            if ("all".equals(cat)) {
                matchesCategory = true;
            } else if ("general".equals(cat)) {
                // "General" matches General Practice, General Care, General.
                if (spec.contains("general") || spec.isEmpty()) {
                    matchesCategory = true;
                }
            } else if (spec.contains(cat)) {
                matchesCategory = true;
            }

            boolean matchesSearch = currentSearchQuery.isEmpty() || 
                                    name.contains(currentSearchQuery) || 
                                    address.contains(currentSearchQuery) || 
                                    spec.contains(currentSearchQuery);

            if (matchesCategory && matchesSearch) {
                clinics.add(c);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void setupBottomNav() {
        // Home tab — already here, do nothing
        findViewById(R.id.nav_home).setOnClickListener(v -> {
            // Already on home, scroll to top
            ((android.widget.ScrollView) findViewById(R.id.patient_home_root).findViewById(android.R.id.content))
                    .scrollTo(0, 0);
        });

        // Queue tab — show queue status (needs an active appointment)
        findViewById(R.id.nav_queue).setOnClickListener(v -> {
            appointmentRepo.getPatientAppointments(session.getUserId(), querySnapshot -> {
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    Appointment active = null;
                    for (var doc : querySnapshot.getDocuments()) {
                        Appointment appt = doc.toObject(Appointment.class);
                        if (appt != null && ("WAITING".equals(appt.getStatus()) || "IN_PROGRESS".equals(appt.getStatus()))) {
                            appt.setAppointmentId(doc.getId()); // ensure we have ID
                            active = appt;
                            break;
                        }
                    }
                    if (active != null) {
                        startTrackingService(active); // ensure tracking starts here too
                        
                        Intent intent = new Intent(this, QueueStatusActivity.class);
                        intent.putExtra("TOKEN_NUMBER", active.getTokenNumber());
                        intent.putExtra("CLINIC_ID", active.getClinicId());
                        intent.putExtra("CLINIC_NAME", "Active Appointment");
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    } else {
                        Toast.makeText(this, "You have no active appointments.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "You have no active appointments.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Bookings tab — show the appointments dialog instead of first clinic
        findViewById(R.id.nav_bookings).setOnClickListener(v -> {
            showAppointmentsDialog();
        });

        // More tab — Options (including Prescriptions and Profile)
        findViewById(R.id.nav_more).setOnClickListener(v -> {
            showMoreDialog();
        });
    }

    private void showMoreDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_patient_more, null);
        dialog.setContentView(view);

        view.findViewById(R.id.btn_prescriptions).setOnClickListener(v -> {
            dialog.dismiss();
            showPrescriptionsDialog();
        });

        view.findViewById(R.id.btn_profile).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, PatientProfileActivity.class));
        });

        dialog.show();
    }

    private void showPrescriptionsDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_patient_prescriptions, null);
        dialog.setContentView(view);
        android.widget.LinearLayout container = view.findViewById(R.id.prescriptions_container);
        
        // Show loading or wait
        appointmentRepo.getPatientAppointments(session.getUserId(), querySnapshot -> {
            if (querySnapshot == null || querySnapshot.isEmpty()) {
                TextView empty = new TextView(this);
                empty.setText("No prescriptions available yet.");
                empty.setTextColor(getColor(R.color.on_surface_variant));
                container.addView(empty);
                dialog.show();
                return;
            }

            boolean hasPrescription = false;
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

            for (var doc : querySnapshot.getDocuments()) {
                Appointment appt = doc.toObject(Appointment.class);
                if (appt != null && appt.getPrescriptionUrl() != null && !appt.getPrescriptionUrl().isEmpty()) {
                    hasPrescription = true;
                    android.view.View item = getLayoutInflater().inflate(R.layout.item_patient_prescription, container, false);
                    TextView name = item.findViewById(R.id.pre_clinic_name);
                    TextView date = item.findViewById(R.id.pre_date);
                    ImageView img = item.findViewById(R.id.img_prescription);

                    Glide.with(this).load(appt.getPrescriptionUrl()).centerCrop().into(img);
                    
                    String dateStr = "Unknown Date";
                    if (appt.getCreatedAt() != null) {
                        dateStr = sdf.format(appt.getCreatedAt());
                    }
                    date.setText(dateStr);
                    name.setText("Loading Clinic...");

                    if (appt.getClinicId() != null) {
                        clinicRepo.getClinic(appt.getClinicId(), clinicDoc -> {
                            if (clinicDoc != null && clinicDoc.exists() && clinicDoc.getString("name") != null) {
                                name.setText(clinicDoc.getString("name"));
                            } else {
                                name.setText("Unknown Clinic");
                            }
                        });
                    }

                    item.setOnClickListener(v -> {
                        Dialog imgDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                        ImageView fullImageView = new ImageView(this);
                        fullImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        Glide.with(this).load(appt.getPrescriptionUrl()).into(fullImageView);
                        fullImageView.setOnClickListener(imgV -> imgDialog.dismiss());
                        imgDialog.setContentView(fullImageView);
                        imgDialog.show();
                    });

                    container.addView(item);
                }
            }
            
            if (!hasPrescription) {
                TextView empty = new TextView(this);
                empty.setText("No prescriptions available yet.");
                empty.setTextColor(getColor(R.color.on_surface_variant));
                container.addView(empty);
            }
            
            dialog.show();
        });
    }

    private void loadClinics() {
        clinicRepo.getAllClinics(querySnapshot -> {
            allClinics.clear();
            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                for (var doc : querySnapshot.getDocuments()) {
                    Clinic clinic = doc.toObject(Clinic.class);
                    if (clinic != null) {
                        clinic.setClinicId(doc.getId());
                        allClinics.add(clinic);
                    }
                }
            }

            filterClinicsByCategory();
            
            checkLocationPermission();
        });
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchLocationAndSort();
        }
    }

    private void showAppointmentsDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_patient_appointments, null);
        dialog.setContentView(view);
        android.widget.LinearLayout container = view.findViewById(R.id.appointments_container);

        appointmentRepo.getPatientAppointments(session.getUserId(), querySnapshot -> {
            if (querySnapshot == null || querySnapshot.isEmpty()) {
                TextView empty = new TextView(this);
                empty.setText("No appointments booked yet.");
                empty.setTextColor(getColor(R.color.on_surface_variant));
                container.addView(empty);
                dialog.show();
                return;
            }

            for (var doc : querySnapshot.getDocuments()) {
                Appointment appt = doc.toObject(Appointment.class);
                if (appt != null) {
                    appt.setAppointmentId(doc.getId());

                    android.view.View item = getLayoutInflater().inflate(R.layout.item_patient_appointment, container, false);
                    TextView name = item.findViewById(R.id.apt_clinic_name);
                    TextView details = item.findViewById(R.id.apt_details);
                    Button cancelBtn = item.findViewById(R.id.apt_btn_cancel);

                    name.setText("Loading...");
                    details.setText("Status: " + appt.getStatus() + "\nToken: #" + appt.getTokenNumber());

                    if (appt.getClinicId() != null) {
                        clinicRepo.getClinic(appt.getClinicId(), clinicDoc -> {
                            String clinicName = "Unknown Clinic";
                            if (clinicDoc != null && clinicDoc.exists() && clinicDoc.getString("name") != null) {
                                clinicName = clinicDoc.getString("name");
                            }
                            name.setText(clinicName);
                        });
                    } else {
                        name.setText("Unknown Clinic");
                    }

                    if ("WAITING".equals(appt.getStatus())) {
                        cancelBtn.setVisibility(android.view.View.VISIBLE);
                        cancelBtn.setOnClickListener(v -> {
                            new androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("Cancel Appointment")
                                    .setMessage("Are you sure you want to cancel this booking?")
                                    .setPositiveButton("Yes, Cancel", (d, which) -> {
                                        appointmentRepo.deleteAppointment(appt.getAppointmentId(), task -> {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(this, "Appointment cancelled", Toast.LENGTH_SHORT).show();
                                                dialog.dismiss();
                                                showAppointmentsDialog(); // Refresh
                                            } else {
                                                Toast.makeText(this, "Failed to cancel", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    })
                                    .setNegativeButton("No", null)
                                    .show();
                        });
                    }
                    container.addView(item);
                }
            }
            dialog.show();
        });
    }

    private void fetchLocationAndSort() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentUserLocation = location;
                    adapter.setPatientLocation(location);
                    sortClinicsByDistance();
                }
            });
        }
    }

    private void sortClinicsByDistance() {
        if (currentUserLocation == null || clinics.isEmpty()) return;

        Collections.sort(clinics, (c1, c2) -> {
            float dist1 = Float.MAX_VALUE;
            float dist2 = Float.MAX_VALUE;
            
            if (c1.getLatitude() != 0 && c1.getLongitude() != 0) {
                Location loc1 = new Location("");
                loc1.setLatitude(c1.getLatitude());
                loc1.setLongitude(c1.getLongitude());
                dist1 = currentUserLocation.distanceTo(loc1);
            }
            if (c2.getLatitude() != 0 && c2.getLongitude() != 0) {
                Location loc2 = new Location("");
                loc2.setLatitude(c2.getLatitude());
                loc2.setLongitude(c2.getLongitude());
                dist2 = currentUserLocation.distanceTo(loc2);
            }
            return Float.compare(dist1, dist2);
        });
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocationAndSort();
            }
        }
    }

    @Override
    public void onClinicClick(Clinic clinic) {
        Intent intent = new Intent(this, ClinicDetailsActivity.class);
        intent.putExtra("CLINIC_ID", clinic.getClinicId());
        intent.putExtra("CLINIC_NAME", clinic.getName());
        intent.putExtra("CLINIC_ADDRESS", clinic.getAddress());
        intent.putExtra("CLINIC_SPECIALTY", clinic.getSpecialization());
        intent.putExtra("CLINIC_FEE", clinic.getConsultationFee());
        intent.putExtra("CLINIC_OPEN", clinic.getOpeningTime());
        intent.putExtra("CLINIC_CLOSE", clinic.getClosingTime());
        intent.putExtra("CLINIC_LATITUDE", clinic.getLatitude());
        intent.putExtra("CLINIC_LONGITUDE", clinic.getLongitude());
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onBookClick(Clinic clinic) {
        onClinicClick(clinic); // Same flow — go to details then book
    }

    @Override
    protected void onResume() {
        super.onResume();
        String userId = session.getUserId();
        if (userId != null) {
            userRepo.getUser(userId, documentSnapshot -> {
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    android.widget.ImageView profileImg = findViewById(R.id.patient_top_profile_img);
                    if (user != null && user.getProfileImageUrl() != null) {
                        Glide.with(this).load(user.getProfileImageUrl()).centerCrop().into(profileImg);
                    } else {
                        profileImg.setImageResource(R.drawable.ic_person_outline);
                    }
                }
            });
        }
    }
}
