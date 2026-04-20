package com.example.mediline;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mediline.model.User;
import com.example.mediline.repository.AuthRepository;
import com.example.mediline.repository.UserRepository;
import com.example.mediline.util.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class DoctorProfileActivity extends AppCompatActivity {

    private SessionManager session;
    private UserRepository userRepo;
    private AuthRepository authRepo;

    private TextView tvName;
    private TextInputEditText etEmail, etPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_doctor_profile);

        session = new SessionManager(this);
        userRepo = new UserRepository();
        authRepo = new AuthRepository();

        MaterialToolbar toolbar = findViewById(R.id.toolbar_profile);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvName = findViewById(R.id.tv_profile_name);
        etEmail = findViewById(R.id.et_profile_email);
        etPhone = findViewById(R.id.et_profile_phone);
        
        MaterialButton btnLogout = findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            session.logout();
            authRepo.signOut();
            Intent intent = new Intent(this, OnboardingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        loadProfileData();
    }

    private void loadProfileData() {
        String doctorId = session.getUserId();
        if (doctorId == null) {
            Toast.makeText(this, "Error loading user session", Toast.LENGTH_SHORT).show();
            return;
        }

        tvName.setText("Dr. " + session.getUserName());
        etEmail.setText(session.getUserEmail());

        userRepo.getUser(doctorId, documentSnapshot -> {
            if (documentSnapshot != null && documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    etPhone.setText(user.getPhone() != null ? user.getPhone() : "Not provided");
                }
            } else {
                Toast.makeText(this, "User details not found", Toast.LENGTH_SHORT).show();
            }
        });
    }
}