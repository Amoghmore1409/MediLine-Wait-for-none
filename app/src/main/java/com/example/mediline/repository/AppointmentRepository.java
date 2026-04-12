package com.example.mediline.repository;

import com.example.mediline.model.Appointment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

    public void getAppointment(String appointmentId, OnSuccessListener<DocumentSnapshot> listener) {
        db.collection("appointments").document(appointmentId).get().addOnSuccessListener(listener);
    }

    public void getPatientAppointments(String patientId, OnSuccessListener<QuerySnapshot> listener) {
        db.collection("appointments")
                .whereEqualTo("patientId", patientId)
                .orderBy("tokenNumber", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(listener);
    }

    public void getNextTokenNumber(String clinicId, OnSuccessListener<QuerySnapshot> listener) {
        db.collection("appointments")
                .whereEqualTo("clinicId", clinicId)
                .orderBy("tokenNumber", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(listener);
    }
}
