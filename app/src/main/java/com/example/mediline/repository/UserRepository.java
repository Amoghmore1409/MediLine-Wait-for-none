package com.example.mediline.repository;

import com.example.mediline.model.DoctorProfile;
import com.example.mediline.model.PatientProfile;
import com.example.mediline.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserRepository {
    private final FirebaseFirestore db;

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void createUser(String userId, User user, OnCompleteListener<Void> listener) {
        db.collection("users").document(userId).set(user).addOnCompleteListener(listener);
    }

    public void getUser(String userId, OnSuccessListener<DocumentSnapshot> listener) {
        db.collection("users").document(userId).get().addOnSuccessListener(listener);
    }

    public void updateUserField(String userId, String field, Object value, OnCompleteListener<Void> listener) {
        db.collection("users").document(userId).update(field, value).addOnCompleteListener(listener);
    }

    public void createDoctorProfile(String doctorId, DoctorProfile profile, OnCompleteListener<Void> listener) {
        db.collection("doctorProfiles").document(doctorId).set(profile).addOnCompleteListener(listener);
    }

    public void getDoctorProfile(String doctorId, OnSuccessListener<DocumentSnapshot> listener) {
        db.collection("doctorProfiles").document(doctorId).get().addOnSuccessListener(listener);
    }

    public void createPatientProfile(String patientId, PatientProfile profile, OnCompleteListener<Void> listener) {
        db.collection("patientProfiles").document(patientId).set(profile).addOnCompleteListener(listener);
    }

    public void getPatientProfile(String patientId, OnSuccessListener<DocumentSnapshot> listener) {
        db.collection("patientProfiles").document(patientId).get().addOnSuccessListener(listener);
    }
}
