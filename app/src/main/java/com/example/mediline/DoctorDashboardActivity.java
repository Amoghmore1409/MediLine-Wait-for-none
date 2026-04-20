package com.example.mediline;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediline.adapter.PatientAdapter;
import com.example.mediline.model.Appointment;
import com.example.mediline.repository.AppointmentRepository;
import com.example.mediline.repository.ClinicRepository;
import com.example.mediline.util.SessionManager;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DoctorDashboardActivity extends AppCompatActivity {

    private RecyclerView patientList;
    private PatientAdapter adapter;
    private List<Appointment> appointments = new ArrayList<>();
    private AppointmentRepository appointmentRepo;
    private ClinicRepository clinicRepo;
    private SessionManager session;
    private String clinicId;
    private ListenerRegistration queueListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_doctor_dashboard);

        appointmentRepo = new AppointmentRepository();
        clinicRepo = new ClinicRepository();
        session = new SessionManager(this);

        // Date display
        TextView dateView = findViewById(R.id.doctor_date);
        dateView.setText("MedLine • " + new SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(new Date()));

        // RecyclerView
        patientList = findViewById(R.id.doctor_patient_list);
        patientList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PatientAdapter(appointments);
        patientList.setAdapter(adapter);

        // Manage Queue button
        findViewById(R.id.doctor_manage_queue_btn).setOnClickListener(v -> {
            Intent intent = new Intent(this, QueueManagementActivity.class);
            if (clinicId != null) intent.putExtra("CLINIC_ID", clinicId);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Bottom nav - Queue
        findViewById(R.id.doc_nav_queue).setOnClickListener(v -> {
            Intent intent = new Intent(this, QueueManagementActivity.class);
            if (clinicId != null) intent.putExtra("CLINIC_ID", clinicId);
            startActivity(intent);
        });

        // Bottom nav - Clinic
        findViewById(R.id.doc_nav_clinic).setOnClickListener(v -> {
            startActivity(new Intent(this, ClinicSetupActivity.class));
        });

        // Profile top button and bottom nav - show profile dialog with logout
        findViewById(R.id.doctor_profile_btn).setOnClickListener(v -> showProfileDialog());
        findViewById(R.id.doc_nav_profile).setOnClickListener(v -> showProfileDialog());

        // Initial load
        loadDashboardData();
    }

    private void showProfileDialog() {
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
    }

    private void loadDashboardData() {
        String userId = session.getUserId();
        if (userId == null) return;

        clinicRepo.getClinicByDoctor(userId, querySnapshot -> {
            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                var clinicDoc = querySnapshot.getDocuments().get(0);
                clinicId = clinicDoc.getId();

                // Start real-time listener for the dashboard
                startRealTimeUpdates();
            } else {
                // No clinic yet - show demo data
                addDemoPatients();
            }
        });
    }

    private void startRealTimeUpdates() {
        if (queueListener != null) queueListener.remove();

        queueListener = appointmentRepo.listenToQueue(clinicId, (apptSnapshot, error) -> {
            if (error != null) return;

            appointments.clear();
            int waitingCount = 0;
            int completedCount = 0;

            if (apptSnapshot != null) {
                for (var doc : apptSnapshot.getDocuments()) {
                    Appointment appt = doc.toObject(Appointment.class);
                    if (appt != null) {
                        appt.setAppointmentId(doc.getId());
                        String status = appt.getStatus();
                        
                        // We track all appointments to calculate stats, 
                        // but only show WAITING/IN_PROGRESS in the dashboard list
                        if ("WAITING".equals(status) || "IN_PROGRESS".equals(status)) {
                            appointments.add(appt);
                            if ("WAITING".equals(status)) waitingCount++;
                        } else if ("COMPLETED".equals(status)) {
                            completedCount++;
                        }
                    }
                }
                
                // Sort by token number
                appointments.sort(java.util.Comparator.comparingInt(Appointment::getTokenNumber));
            }

            adapter.notifyDataSetChanged();
            updateDashboardStats(waitingCount, completedCount);
        });
    }

    private void updateDashboardStats(int waitingCount, int completedCount) {
        TextView queueCount = findViewById(R.id.doctor_queue_count);
        queueCount.setText(String.valueOf(waitingCount));

        TextView patientsSeen = findViewById(R.id.doctor_patients_seen);
        patientsSeen.setText(String.valueOf(completedCount));

        TextView avgWait = findViewById(R.id.doctor_avg_wait);
        avgWait.setText((waitingCount * 5) + " min");

        android.widget.ProgressBar progressBar = findViewById(R.id.doctor_queue_progress);
        int total = waitingCount + completedCount;
        if (total > 0) {
            progressBar.setProgress((int) ((double) completedCount / total * 100));
        }
    }

    private void addDemoPatients() {
        appointments.clear();
        appointments.add(new Appointment("", 9, "WAITING", "APP", "", "Sarah J. Miller", "Routine Follow-up"));
        appointments.add(new Appointment("", 10, "WAITING", "APP", "", "Robert D. Vance", "Post-Op Consultation"));
        appointments.add(new Appointment("", 11, "WAITING", "APP", "", "Linda G. Thompson", "Emergency Triage"));
        adapter.notifyDataSetChanged();

        ((TextView) findViewById(R.id.doctor_queue_count)).setText("3");
        ((TextView) findViewById(R.id.doctor_patients_seen)).setText("8");
        ((TextView) findViewById(R.id.doctor_avg_wait)).setText("14 min");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (queueListener != null) {
            queueListener.remove();
        }
    }
}
