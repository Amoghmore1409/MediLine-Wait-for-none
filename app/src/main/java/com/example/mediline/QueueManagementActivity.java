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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.bumptech.glide.Glide;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;

import android.net.Uri;
import android.app.ProgressDialog;
import android.app.Dialog;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
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
    private Uri prescriptionPhotoUri;

    private final ActivityResultLauncher<Uri> takePrescriptionLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && currentPatient != null) {
                    uploadPrescription();
                }
            });

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

        // Buttons for Records
        findViewById(R.id.qm_view_record_btn).setOnClickListener(v -> showMedicalRecord());
        findViewById(R.id.qm_upload_prescription_btn).setOnClickListener(v -> startPrescriptionCapture());

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
        TextView viewRecordBtn = findViewById(R.id.qm_view_record_btn);
        TextView uploadPrescriptionBtn = findViewById(R.id.qm_upload_prescription_btn);

        if (currentPatient != null) {
            currentToken.setText("Token #" + String.format("%03d", currentPatient.getTokenNumber()));
            currentPatientName.setText("Patient: " + (currentPatient.getPatientName() != null ? currentPatient.getPatientName() : "Walk-in"));
            
            uploadPrescriptionBtn.setVisibility(View.VISIBLE);
            if (currentPatient.getPatientMedicalRecordUrl() != null && !currentPatient.getPatientMedicalRecordUrl().isEmpty()) {
                viewRecordBtn.setVisibility(View.VISIBLE);
            } else {
                viewRecordBtn.setVisibility(View.GONE);
            }
        } else {
            currentToken.setText("Token #---");
            currentPatientName.setText("No patient in consultation");
            viewRecordBtn.setVisibility(View.GONE);
            uploadPrescriptionBtn.setVisibility(View.GONE);
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

    private void showMedicalRecord() {
        if (currentPatient == null || currentPatient.getPatientMedicalRecordUrl() == null) return;
        
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.activity_clinic_details); // We can just use a dynamic imageview instead of external layout
        dialog.dismiss(); // Clear if showing
        
        Dialog imgDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Glide.with(this).load(currentPatient.getPatientMedicalRecordUrl()).into(imageView);
        imageView.setOnClickListener(v -> imgDialog.dismiss());
        imgDialog.setContentView(imageView);
        imgDialog.show();
    }

    private void startPrescriptionCapture() {
        try {
            File imageFile = File.createTempFile("prescription_", ".jpg", getCacheDir());
            prescriptionPhotoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);
            takePrescriptionLauncher.launch(prescriptionPhotoUri);
        } catch (IOException e) {
            Toast.makeText(this, "Could not create image file", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadPrescription() {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Uploading prescription...");
        progress.setCancelable(false);
        progress.show();

        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("Images/Prescriptions/" + System.currentTimeMillis() + ".jpg");
        storageRef.putFile(prescriptionPhotoUri).addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                progress.dismiss();
                // update current appointment
                appointmentRepo.updateAppointmentField(currentPatient.getAppointmentId(), "prescriptionUrl", uri.toString(), task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Prescription uploaded successfully!", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }).addOnFailureListener(e -> {
            progress.dismiss();
            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
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
