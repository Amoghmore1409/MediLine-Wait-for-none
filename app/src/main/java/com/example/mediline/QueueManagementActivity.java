package com.example.mediline;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.List;

public class QueueManagementActivity extends AppCompatActivity {

    private RecyclerView patientList;
    private PatientAdapter adapter;
    private List<Appointment> waitingAppointments = new ArrayList<>();
    private AppointmentRepository appointmentRepo;
    private ClinicRepository clinicRepo;
    private SessionManager session;
    private ListenerRegistration queueListener;
    private String clinicId;
    private Appointment currentPatient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_queue_management);

        appointmentRepo = new AppointmentRepository();
        clinicRepo = new ClinicRepository();
        session = new SessionManager(this);

        clinicId = getIntent().getStringExtra("CLINIC_ID");

        patientList = findViewById(R.id.qm_patient_list);
        patientList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PatientAdapter(waitingAppointments);
        patientList.setAdapter(adapter);

        // Back button
        findViewById(R.id.qm_back_btn).setOnClickListener(v -> finish());

        // Next Patient button
        findViewById(R.id.qm_next_btn).setOnClickListener(v -> advanceQueue());

        // Pause / Resume
        findViewById(R.id.qm_pause_btn).setOnClickListener(v ->
                Toast.makeText(this, "Queue paused", Toast.LENGTH_SHORT).show());
        findViewById(R.id.qm_resume_btn).setOnClickListener(v ->
                Toast.makeText(this, "Queue resumed", Toast.LENGTH_SHORT).show());

        if (clinicId != null) {
            startQueueListener();
        } else {
            // Try to find clinic by doctor
            String userId = session.getUserId();
            if (userId != null) {
                clinicRepo.getClinicByDoctor(userId, querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        clinicId = querySnapshot.getDocuments().get(0).getId();
                        startQueueListener();
                    } else {
                        loadDemoData();
                    }
                });
            } else {
                loadDemoData();
            }
        }
    }

    private void startQueueListener() {
        queueListener = appointmentRepo.listenToQueue(clinicId, (snapshots, error) -> {
            if (error != null || snapshots == null) return;

            waitingAppointments.clear();
            currentPatient = null;
            int completedCount = 0;
            int totalCount = 0;

            for (var doc : snapshots.getDocuments()) {
                Appointment appt = doc.toObject(Appointment.class);
                if (appt != null) {
                    appt.setAppointmentId(doc.getId());
                    totalCount++;

                    if ("IN_PROGRESS".equals(appt.getStatus())) {
                        currentPatient = appt;
                    } else if ("WAITING".equals(appt.getStatus())) {
                        waitingAppointments.add(appt);
                    } else if ("COMPLETED".equals(appt.getStatus())) {
                        completedCount++;
                    }
                }
            }

            adapter.notifyDataSetChanged();
            updateUI(completedCount, totalCount);
        });
    }

    private void updateUI(int completed, int total) {
        TextView currentToken = findViewById(R.id.qm_current_token);
        TextView currentPatientName = findViewById(R.id.qm_current_patient);

        if (currentPatient != null) {
            currentToken.setText("Token #" + String.format("%03d", currentPatient.getTokenNumber()));
            currentPatientName.setText("Patient: " + (currentPatient.getPatientName() != null ? currentPatient.getPatientName() : "Walk-in"));
        } else {
            currentToken.setText("Token #---");
            currentPatientName.setText("No patient in consultation");
        }

        TextView completionText = findViewById(R.id.qm_completion_text);
        completionText.setText(completed + " / " + total);

        ProgressBar progressBar = findViewById(R.id.qm_completion_progress);
        if (total > 0) {
            progressBar.setProgress((int) ((double) completed / total * 100));
        }

        TextView avgWait = findViewById(R.id.qm_avg_wait);
        avgWait.setText((waitingAppointments.size() * 5) + " min");
    }

    private void advanceQueue() {
        if (clinicId == null) {
            Toast.makeText(this, "No clinic configured", Toast.LENGTH_SHORT).show();
            return;
        }

        // Complete current patient
        if (currentPatient != null) {
            appointmentRepo.updateAppointmentStatus(currentPatient.getAppointmentId(), "COMPLETED", task -> {
                if (!task.isSuccessful()) {
                    Toast.makeText(this, "Error completing patient", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Move next waiting to in-progress
        if (!waitingAppointments.isEmpty()) {
            Appointment next = waitingAppointments.get(0);
            appointmentRepo.updateAppointmentStatus(next.getAppointmentId(), "IN_PROGRESS", task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Now serving Token #" + next.getTokenNumber(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Queue is empty!", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadDemoData() {
        waitingAppointments.clear();
        waitingAppointments.add(new Appointment("", 43, "WAITING", "APP", "", "Robert Jenkins", "General Checkup"));
        waitingAppointments.add(new Appointment("", 44, "WAITING", "WALK_IN", "", "Elena Rodriguez", "Post-Op Recovery"));
        waitingAppointments.add(new Appointment("", 45, "WAITING", "APP", "", "Chen Wei", "Allergy Panel"));
        adapter.notifyDataSetChanged();

        ((TextView) findViewById(R.id.qm_current_token)).setText("Token #042");
        ((TextView) findViewById(R.id.qm_current_patient)).setText("Patient: Sarah Montgomery");
        ((TextView) findViewById(R.id.qm_completion_text)).setText("12 / 28");
        ((TextView) findViewById(R.id.qm_avg_wait)).setText("18 min");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (queueListener != null) {
            queueListener.remove();
        }
    }
}
