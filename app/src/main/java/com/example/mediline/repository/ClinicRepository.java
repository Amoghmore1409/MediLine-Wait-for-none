package com.example.mediline.repository;

import com.example.mediline.model.Clinic;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class ClinicRepository {
    private final FirebaseFirestore db;

    public ClinicRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void createClinic(Clinic clinic, OnCompleteListener<DocumentReference> listener) {
        db.collection("clinics").add(clinic).addOnCompleteListener(listener);
    }

    public void updateClinic(String clinicId, Clinic clinic, OnCompleteListener<Void> listener) {
        db.collection("clinics").document(clinicId).set(clinic).addOnCompleteListener(listener);
    }

    public void getClinic(String clinicId, OnSuccessListener<DocumentSnapshot> listener) {
        db.collection("clinics").document(clinicId).get().addOnSuccessListener(listener);
    }

    public void getClinicByDoctor(String doctorId, OnSuccessListener<QuerySnapshot> listener) {
        db.collection("clinics")
                .whereEqualTo("doctorId", doctorId)
                .limit(1)
                .get()
                .addOnSuccessListener(listener);
    }

    public void getAllClinics(OnSuccessListener<QuerySnapshot> listener) {
        db.collection("clinics").get().addOnSuccessListener(listener);
    }
}
