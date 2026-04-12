package com.example.mediline;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mediline.model.Appointment;
import com.example.mediline.repository.AppointmentRepository;
import com.example.mediline.util.SessionManager;

import java.util.List;

public class ClinicDetailsActivity extends AppCompatActivity {

    private AppointmentRepository appointmentRepo;
    private SessionManager session;
    private String clinicId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_clinic_details);

        appointmentRepo = new AppointmentRepository();
        session = new SessionManager(this);

        // Get clinic data from intent
        clinicId = getIntent().getStringExtra("CLINIC_ID");
        String clinicName = getIntent().getStringExtra("CLINIC_NAME");
        String clinicAddress = getIntent().getStringExtra("CLINIC_ADDRESS");
        String specialty = getIntent().getStringExtra("CLINIC_SPECIALTY");
        double fee = getIntent().getDoubleExtra("CLINIC_FEE", 0);
        String openTime = getIntent().getStringExtra("CLINIC_OPEN");
        String closeTime = getIntent().getStringExtra("CLINIC_CLOSE");

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
        feeView.setText(String.format("$%.0f", fee));
        nextSlot.setText("Today");
        hoursWeekday.setText((openTime != null ? openTime : "08:00") + " — " + (closeTime != null ? closeTime : "17:00"));

        // Load queue data
        if (clinicId != null) {
            appointmentRepo.getQueueForClinic(clinicId, querySnapshot -> {
                if (querySnapshot != null) {
                    List<Appointment> queue = querySnapshot.toObjects(Appointment.class);
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
    }

    private void bookAppointment() {
        if (clinicId == null) return;

        String patientId = session.getUserId();
        String patientName = session.getUserName();

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
                    Toast.makeText(this, "Booked! Token #" + appointment.getTokenNumber(), Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(this, QueueStatusActivity.class);
                    intent.putExtra("TOKEN_NUMBER", appointment.getTokenNumber());
                    intent.putExtra("CLINIC_ID", clinicId);
                    intent.putExtra("CLINIC_NAME", getIntent().getStringExtra("CLINIC_NAME"));
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } else {
                    Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
