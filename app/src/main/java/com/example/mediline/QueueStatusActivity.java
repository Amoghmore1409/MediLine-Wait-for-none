package com.example.mediline;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mediline.model.Appointment;
import com.example.mediline.repository.AppointmentRepository;
import com.example.mediline.util.SessionManager;
import com.example.mediline.util.UiUtils;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class QueueStatusActivity extends AppCompatActivity {

    private AppointmentRepository appointmentRepo;
    private SessionManager session;
    private ListenerRegistration queueListener;
    private int myTokenNumber;
    private String clinicId;
    private String myAppointmentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_queue_status);

        appointmentRepo = new AppointmentRepository();
        session = new SessionManager(this);

        myTokenNumber = getIntent().getIntExtra("TOKEN_NUMBER", 0);
        clinicId = getIntent().getStringExtra("CLINIC_ID");
        String clinicName = getIntent().getStringExtra("CLINIC_NAME");

        // Set static values
        TextView tokenView = findViewById(R.id.queue_token);
        tokenView.setText("#" + myTokenNumber);

        TextView clinicInfo = findViewById(R.id.queue_clinic_info);
        clinicInfo.setText(clinicName != null ? clinicName : "Clinic");

        // Back button
        findViewById(R.id.queue_back_btn).setOnClickListener(v -> finish());

        // Check-in button
        findViewById(R.id.queue_checkin_btn).setOnClickListener(v ->
                Toast.makeText(this, "Manual check-in confirmed!", Toast.LENGTH_SHORT).show());

        // Cancel button
        findViewById(R.id.queue_cancel_btn).setOnClickListener(v -> cancelAppointment());

        // Listen for real-time queue updates
        if (clinicId != null) {
            startQueueListener();
        }
    }

    private void cancelAppointment() {
        if (myAppointmentId == null) {
            Toast.makeText(this, "Could not find your active appointment.", Toast.LENGTH_SHORT).show();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cancel Appointment")
                .setMessage("Are you sure you want to cancel your appointment? This action cannot be undone.")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    appointmentRepo.deleteAppointment(myAppointmentId, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Appointment cancelled successfully.", Toast.LENGTH_SHORT).show();
                            
                            // Inform any tracking service to stop (handled by service listening to null object)
                            // Finish the activity
                            finish();
                        } else {
                            UiUtils.showErrorDialog(this, "Error", "Failed to cancel appointment.");
                        }
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void startQueueListener() {
        queueListener = appointmentRepo.listenToQueue(clinicId, (snapshots, error) -> {
            if (error != null || snapshots == null) return;

            int currentServing = 0;
            int patientsAhead = 0;
            int lowestWaiting = Integer.MAX_VALUE;

            for (var doc : snapshots.getDocuments()) {
                Appointment appt = doc.toObject(Appointment.class);
                if (appt != null) {
                    appt.setAppointmentId(doc.getId()); // Store ID from Firestore
                    
                    if (appt.getPatientId() != null && appt.getPatientId().equals(session.getUserId())) {
                        myAppointmentId = appt.getAppointmentId();
                        myTokenNumber = appt.getTokenNumber(); // Dynamically update token number just in case
                    }

                    if ("IN_PROGRESS".equals(appt.getStatus())) {
                        currentServing = appt.getTokenNumber();
                    } else if ("WAITING".equals(appt.getStatus())) {
                        if (appt.getTokenNumber() < lowestWaiting) {
                            lowestWaiting = appt.getTokenNumber();
                        }
                        if (appt.getTokenNumber() < myTokenNumber) {
                            patientsAhead++;
                        }
                    }
                }
            }

            if (currentServing == 0 && lowestWaiting != Integer.MAX_VALUE) {
                currentServing = lowestWaiting;
            }

            // Update UI
            TextView servingView = findViewById(R.id.queue_serving_number);
            servingView.setText("#" + currentServing);

            TextView aheadText = findViewById(R.id.queue_ahead_text);
            aheadText.setText(String.format(getString(R.string.queue_patients_ahead), patientsAhead));

            TextView waitTime = findViewById(R.id.queue_wait_time);
            waitTime.setText(String.valueOf(patientsAhead * 5)); // ~5 min per patient

            // Progress
            TextView progressCurrent = findViewById(R.id.queue_progress_current);
            progressCurrent.setText("Current: #" + currentServing);
            TextView progressTarget = findViewById(R.id.queue_progress_target);
            progressTarget.setText("Target: #" + myTokenNumber);

            ProgressBar progressBar = findViewById(R.id.queue_progress);
            if (myTokenNumber > 0) {
                int progress = Math.min(100, (int) ((double) currentServing / myTokenNumber * 100));
                progressBar.setProgress(progress);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (queueListener != null) {
            queueListener.remove();
        }
    }
}
