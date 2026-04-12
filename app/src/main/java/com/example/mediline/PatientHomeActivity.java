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

import java.util.ArrayList;
import java.util.List;

public class PatientHomeActivity extends AppCompatActivity implements ClinicAdapter.OnClinicClickListener {

    private RecyclerView clinicList;
    private ClinicAdapter adapter;
    private List<Clinic> clinics = new ArrayList<>();
    private ClinicRepository clinicRepo;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_patient_home);

        clinicRepo = new ClinicRepository();
        session = new SessionManager(this);

        clinicList = findViewById(R.id.patient_clinic_list);
        clinicList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ClinicAdapter(clinics, this);
        clinicList.setAdapter(adapter);

        setupBottomNav();
        loadClinics();
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
            Intent intent = new Intent(this, QueueStatusActivity.class);
            intent.putExtra("TOKEN_NUMBER", 0);
            intent.putExtra("CLINIC_NAME", "MedLine Clinic");
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Bookings tab — opens clinic details (for now, navigate to first clinic)
        findViewById(R.id.nav_bookings).setOnClickListener(v -> {
            if (!clinics.isEmpty()) {
                onClinicClick(clinics.get(0));
            } else {
                Toast.makeText(this, "No bookings yet", Toast.LENGTH_SHORT).show();
            }
        });

        // Profile tab — logout functionality
        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            // Simple logout for now
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Profile")
                    .setMessage("Logged in as: " + session.getUserEmail() + "\nRole: " + session.getUserRole())
                    .setPositiveButton("Logout", (dialog, which) -> {
                        session.logout();
                        new com.example.mediline.repository.AuthRepository().signOut();
                        Intent intent = new Intent(this, OnboardingActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void loadClinics() {
        clinicRepo.getAllClinics(querySnapshot -> {
            clinics.clear();
            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                for (var doc : querySnapshot.getDocuments()) {
                    Clinic clinic = doc.toObject(Clinic.class);
                    if (clinic != null) {
                        clinic.setClinicId(doc.getId());
                        clinics.add(clinic);
                    }
                }
            }

            // Add demo data if no clinics in Firestore
            if (clinics.isEmpty()) {
                Clinic demo1 = new Clinic("", "MedLine Central Hospital", "0.8 miles away • Downtown",
                        0, 0, "08:00", "20:00", 120, "Cardiology");
                demo1.setClinicId("demo1");
                clinics.add(demo1);

                Clinic demo2 = new Clinic("", "St. Mary Pediatrics", "1.2 miles • General Care",
                        0, 0, "09:00", "18:00", 80, "Pediatrics");
                demo2.setClinicId("demo2");
                clinics.add(demo2);

                Clinic demo3 = new Clinic("", "Wellness First Clinic", "2.5 miles • Specialist",
                        0, 0, "08:00", "17:00", 95, "General Practice");
                demo3.setClinicId("demo3");
                clinics.add(demo3);
            }
            adapter.notifyDataSetChanged();
        });
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
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onBookClick(Clinic clinic) {
        onClinicClick(clinic); // Same flow — go to details then book
    }
}
