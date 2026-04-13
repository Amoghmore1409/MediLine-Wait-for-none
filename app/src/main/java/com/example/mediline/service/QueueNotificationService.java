package com.example.mediline.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.mediline.PatientHomeActivity;
import com.example.mediline.R;
import com.example.mediline.model.Appointment;
import com.example.mediline.repository.AppointmentRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class QueueNotificationService extends Service {

    private static final String CHANNEL_ID = "MEDILINE_QUEUE_CHANNEL";
    private AppointmentRepository appointmentRepo;
    private ListenerRegistration queueListener;
    private ListenerRegistration prescriptionListener;
    
    private String clinicId;
    private String patientId;
    private String appointmentId;
    private int myTokenNumber;

    private boolean prescriptionAlertSent = false;
    private boolean approachingAlertSent = false;

    @Override
    public void onCreate() {
        super.onCreate();
        appointmentRepo = new AppointmentRepository();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        clinicId = intent.getStringExtra("CLINIC_ID");
        patientId = intent.getStringExtra("PATIENT_ID");
        appointmentId = intent.getStringExtra("APPOINTMENT_ID");
        myTokenNumber = intent.getIntExtra("TOKEN_NUMBER", 0);

        if (clinicId != null && appointmentId != null) {
            setupListeners();
        }

        return START_STICKY;
    }

    private void setupListeners() {
        if (queueListener != null) queueListener.remove();
        if (prescriptionListener != null) prescriptionListener.remove();

        // 1. Listen to the entire queue for "approaching" notification
        queueListener = appointmentRepo.listenToQueue(clinicId, (snapshots, error) -> {
            if (error != null || snapshots == null) return;
            
            int patientsAhead = 0;
            List<Appointment> queue = snapshots.toObjects(Appointment.class);
            
            for (Appointment appt : queue) {
                if ("WAITING".equals(appt.getStatus()) && appt.getTokenNumber() < myTokenNumber) {
                    patientsAhead++;
                }
            }

            // Fire approaching notification when exactly 2 patients are ahead (or 1 / 0) and we haven't alerted yet
            if (patientsAhead <= 2 && !approachingAlertSent) {
                approachingAlertSent = true;
                sendNotification(
                    "Your Turn is Approaching!",
                    "There are only " + patientsAhead + " patients ahead of you. Please head to the clinic."
                );
            }
        });

        // 2. Listen to the specific appointment for prescription updates
        prescriptionListener = appointmentRepo.getAppointmentRef(appointmentId).addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null) return;

            Appointment appt = snapshot.toObject(Appointment.class);
            if (appt != null) {
                if (appt.getPrescriptionUrl() != null && !appt.getPrescriptionUrl().isEmpty() && !prescriptionAlertSent) {
                    prescriptionAlertSent = true;
                    sendNotification(
                        "Prescription Received",
                        "Your doctor has uploaded your prescription. Tap to view it."
                    );
                    
                    // Once prescription is sent, appointment is likely done
                    stopSelf();
                }

                if ("COMPLETED".equals(appt.getStatus()) || "CANCELLED".equals(appt.getStatus())) {
                    stopSelf(); // Kill service if appointment is done
                }
            }
        });
    }

    private void sendNotification(String title, String message) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent intent = new Intent(this, PatientHomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_medical_services)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // Using timestamp as unique ID
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Queue Notifications";
            String description = "Alerts for appointment status and prescriptions";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Unbound service
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (queueListener != null) queueListener.remove();
        if (prescriptionListener != null) prescriptionListener.remove();
    }
}
