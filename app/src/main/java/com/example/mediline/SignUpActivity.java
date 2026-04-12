package com.example.mediline;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mediline.model.User;
import com.example.mediline.repository.AuthRepository;
import com.example.mediline.repository.UserRepository;
import com.example.mediline.util.SessionManager;

public class SignUpActivity extends AppCompatActivity {

    private EditText nameInput, emailInput, phoneInput, passwordInput;
    private ProgressBar progressBar;
    private AuthRepository authRepo;
    private UserRepository userRepo;
    private SessionManager session;
    private String selectedRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        authRepo = new AuthRepository();
        userRepo = new UserRepository();
        session = new SessionManager(this);

        selectedRole = getIntent().getStringExtra("USER_ROLE");
        if (selectedRole == null) selectedRole = "PATIENT";

        nameInput = findViewById(R.id.signup_name);
        emailInput = findViewById(R.id.signup_email);
        phoneInput = findViewById(R.id.signup_phone);
        passwordInput = findViewById(R.id.signup_password);
        progressBar = findViewById(R.id.signup_progress);

        // Role toggle UI
        View patientTab = findViewById(R.id.signup_role_patient);
        View doctorTab = findViewById(R.id.signup_role_doctor);

        patientTab.setOnClickListener(v -> {
            selectedRole = "PATIENT";
            patientTab.setBackgroundResource(R.drawable.bg_role_card);
            patientTab.setElevation(4);
            doctorTab.setBackground(null);
            doctorTab.setElevation(0);
        });
        doctorTab.setOnClickListener(v -> {
            selectedRole = "DOCTOR";
            doctorTab.setBackgroundResource(R.drawable.bg_role_card);
            doctorTab.setElevation(4);
            patientTab.setBackground(null);
            patientTab.setElevation(0);
        });

        // Set initial state based on role
        if ("DOCTOR".equals(selectedRole)) {
            doctorTab.performClick();
        }

        findViewById(R.id.signup_register_btn).setOnClickListener(v -> attemptSignUp());

        findViewById(R.id.signup_login_link).setOnClickListener(v -> {
            finish(); // Go back to login
        });
    }

    private void attemptSignUp() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, R.string.error_short_password, Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        authRepo.signUpWithEmail(email, password, task -> {
            progressBar.setVisibility(View.GONE);

            if (task.isSuccessful() && task.getResult().getUser() != null) {
                String uid = task.getResult().getUser().getUid();

                // Save session immediately
                session.saveLoginState(uid, selectedRole, name, email);

                // Write to Firestore (fire-and-forget — don't block navigation)
                User user = new User(phone, email, selectedRole);
                userRepo.createUser(uid, user, dbTask -> {
                    if (!dbTask.isSuccessful()) {
                        android.util.Log.w("SignUpActivity",
                                "Firestore write failed", dbTask.getException());
                    }
                });

                Toast.makeText(this, R.string.success_registration, Toast.LENGTH_SHORT).show();

                Intent intent;
                if ("DOCTOR".equals(selectedRole)) {
                    intent = new Intent(this, ClinicSetupActivity.class);
                } else {
                    intent = new Intent(this, PatientHomeActivity.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            } else {
                String msg = task.getException() != null ? task.getException().getMessage() : getString(R.string.error_generic);
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
