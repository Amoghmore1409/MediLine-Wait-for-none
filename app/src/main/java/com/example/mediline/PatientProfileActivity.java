package com.example.mediline;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.mediline.model.User;
import com.example.mediline.repository.AuthRepository;
import com.example.mediline.repository.UserRepository;
import com.example.mediline.util.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Map;

public class PatientProfileActivity extends AppCompatActivity {

    private SessionManager session;
    private UserRepository userRepo;
    private AuthRepository authRepo;

    private TextView tvName;
    private TextView etEmail, etPhone;
    private ImageView imgAvatar;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        uploadImageToCloudinary(selectedImageUri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_patient_profile);

        session = new SessionManager(this);
        userRepo = new UserRepository();
        authRepo = new AuthRepository();

        MaterialToolbar toolbar = findViewById(R.id.toolbar_profile);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvName = findViewById(R.id.tv_profile_name);
        etEmail = findViewById(R.id.et_profile_email);
        etPhone = findViewById(R.id.et_profile_phone);
        
        imgAvatar = findViewById(R.id.img_profile_avatar);

        ImageView fabEditAvatar = findViewById(R.id.fab_edit_avatar);

        fabEditAvatar.setOnClickListener(v -> showImageOptionsDialog());

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

    private void showImageOptionsDialog() {
        String[] options = {"Change Photo", "Remove Photo"};
        new AlertDialog.Builder(this)
                .setTitle("Update Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(Intent.ACTION_PICK);
                        intent.setType("image/*");
                        imagePickerLauncher.launch(intent);
                    } else if (which == 1) {
                        removeImage();
                    }
                })
                .show();
    }

    private void removeImage() {
        String patientId = session.getUserId();
        if (patientId == null) return;

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Removing...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        userRepo.updateUserField(patientId, "profileImageUrl", null, task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) {
                imgAvatar.setImageResource(R.drawable.ic_person); // Reset to default
                Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to remove photo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading image...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        MediaManager.get().upload(imageUri).option("folder", "Profiles").callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {}

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {}

            @Override
            public void onSuccess(String requestId, Map resultData) {
                String secureUrl = (String) resultData.get("secure_url");
                saveUrlToFirestore(secureUrl, progressDialog);
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                progressDialog.dismiss();
                Toast.makeText(PatientProfileActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void saveUrlToFirestore(String url, ProgressDialog progressDialog) {
        String patientId = session.getUserId();
        if (patientId == null) {
            progressDialog.dismiss();
            return;
        }

        userRepo.updateUserField(patientId, "profileImageUrl", url, task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) {
                Glide.with(this).load(url).centerCrop().into(imgAvatar);
                Toast.makeText(this, "Photo updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to link photo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProfileData() {
        String patientId = session.getUserId();
        if (patientId == null) {
            Toast.makeText(this, "Error loading user session", Toast.LENGTH_SHORT).show();
            return;
        }

        tvName.setText(session.getUserName());
        etEmail.setText(session.getUserEmail());

        userRepo.getUser(patientId, documentSnapshot -> {
            if (documentSnapshot != null && documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    etPhone.setText(user.getPhone() != null ? user.getPhone() : "Not provided");
                    
                    if (user.getProfileImageUrl() != null) {
                        Glide.with(this).load(user.getProfileImageUrl()).centerCrop().into(imgAvatar);
                    }
                }
            } else {
                Toast.makeText(this, "User details not found", Toast.LENGTH_SHORT).show();
            }
        });
    }
}