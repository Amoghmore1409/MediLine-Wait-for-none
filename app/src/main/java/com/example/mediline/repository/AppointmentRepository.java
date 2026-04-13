package com.example.mediline.repository;

import com.example.mediline.model.Appointment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class AppointmentRepository {
    private final FirebaseFirestore db;

    public AppointmentRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void createAppointment(Appointment appointment, OnCompleteListener<DocumentReference> listener) {
        db.collection("appointments").add(appointment).addOnCompleteListener(listener);
    }

    public void getQueueForClinic(String clinicId, OnSuccessListener<QuerySnapshot> listener) {
        db.collection("appointments")
                .whereEqualTo("clinicId", clinicId)
                .whereEqualTo("status", "WAITING")
                .orderBy("tokenNumber", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(listener);
    }

    public void getAllAppointmentsForClinic(String clinicId, OnSuccessListener<QuerySnapshot> listener) {
        db.collection("appointments")
                .whereEqualTo("clinicId", clinicId)
                .orderBy("tokenNumber", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(listener);
    }

    public ListenerRegistration listenToQueue(String clinicId, EventListener<QuerySnapshot> listener) {
        return db.collection("appointments")
                .whereEqualTo("clinicId", clinicId)
                .orderBy("tokenNumber", Query.Direction.ASCENDING)
                .addSnapshotListener(listener);
    }

    public void updateAppointmentStatus(String appointmentId, String status, OnCompleteListener<Void> listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        db.collection("appointments").document(appointmentId).update(updates).addOnCompleteListener(listener);
    }

    public void updateAppointmentField(String appointmentId, String field, Object value, OnCompleteListener<Void> listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(field, value);
        db.collection("appointments").document(appointmentId).update(updates).addOnCompleteListener(listener);
    }

    public void getAppointment(String appointmentId, OnSuccessListener<DocumentSnapshot> listener) {
        db.collection("appointments").document(appointmentId).get().addOnSuccessListener(listener);
    }

    public void getPatientAppointments(String patientId, OnSuccessListener<QuerySnapshot> listener) {
        db.collection("appointments")
                .whereEqualTo("patientId", patientId)
                .get()
                .addOnSuccessListener(listener);
    }

    public Task<QuerySnapshot> getPatientActiveAppointmentAtClinic(String patientId, String clinicId, OnSuccessListener<QuerySnapshot> listener) {
        return db.collection("appointments")
                .whereEqualTo("patientId", patientId)
                .whereEqualTo("clinicId", clinicId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Filter to only WAITING and IN_PROGRESS status
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        querySnapshot.getDocuments().removeIf(doc -> {
                            Appointment appt = doc.toObject(Appointment.class);
                            return appt == null || (!appt.getStatus().equals("WAITING") && !appt.getStatus().equals("IN_PROGRESS"));
                        });
                    }
                    listener.onSuccess(querySnapshot);
                });
    }

    public Task<QuerySnapshot> getNextTokenNumber(String clinicId, OnSuccessListener<QuerySnapshot> listener) {
        // Query without orderBy to avoid requiring composite index, will sort client-side
        return db.collection("appointments")
                .whereEqualTo("clinicId", clinicId)
                .limit(100)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Sort client-side and find max token number
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        querySnapshot.getDocuments().sort((a, b) -> {
                            Appointment appA = a.toObject(Appointment.class);
                            Appointment appB = b.toObject(Appointment.class);
                            return Integer.compare(appB.getTokenNumber(), appA.getTokenNumber());
                        });
                    }
                    listener.onSuccess(querySnapshot);
                });
    }
}

