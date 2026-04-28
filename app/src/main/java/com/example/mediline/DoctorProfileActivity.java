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

public class DoctorProfileActivity extends AppCompatActivity {

    private SessionManager session;
    private UserRepository userRepo;
    private AuthRepository authRepo;

    private TextView tvName;
    private TextView etEmail, etPhone;
    private ImageView imgCover, imgAvatar;

    private boolean isUpdatingCover = false;

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
        setContentView(R.layout.activity_doctor_profile);

        session = new SessionManager(this);
        userRepo = new UserRepository();
        authRepo = new AuthRepository();

        MaterialToolbar toolbar = findViewById(R.id.toolbar_profile);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvName = findViewById(R.id.tv_profile_name);
        ImageView btnEditName = findViewById(R.id.btn_edit_name);
        btnEditName.setOnClickListener(v -> showEditNameDialog());
        etEmail = findViewById(R.id.et_profile_email);
        etPhone = findViewById(R.id.et_profile_phone);
        
        imgCover = findViewById(R.id.header_bg);
        imgAvatar = findViewById(R.id.img_profile_avatar);

        ImageView fabEditCover = findViewById(R.id.fab_edit_cover);
        ImageView fabEditAvatar = findViewById(R.id.fab_edit_avatar);

        fabEditCover.setOnClickListener(v -> showImageOptionsDialog(true));
        fabEditAvatar.setOnClickListener(v -> showImageOptionsDialog(false));

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

    private void showImageOptionsDialog(boolean isCover) {
        this.isUpdatingCover = isCover;
        String[] options = {"Change Photo", "Remove Photo"};
        new AlertDialog.Builder(this)
                .setTitle(isCover ? "Update Cover Photo" : "Update Profile Picture")
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

    private void showEditNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Name");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        input.setText(session.getUserName());
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!android.text.TextUtils.isEmpty(newName)) {
                updateName(newName);
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateName(String newName) {
        String doctorId = session.getUserId();
        if (doctorId == null) return;

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating name...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        userRepo.updateUserField(doctorId, "name", newName, task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) {
                session.updateUserName(newName);
                tvName.setText("Dr. " + newName);
                Toast.makeText(this, "Name updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to update name", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeImage() {
        String doctorId = session.getUserId();
        if (doctorId == null) return;

        String fieldToUpdate = isUpdatingCover ? "coverImageUrl" : "profileImageUrl";

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Removing...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        userRepo.updateUserField(doctorId, fieldToUpdate, null, task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) {
                if (isUpdatingCover) {
                    imgCover.setImageResource(0); // Clear image
                } else {
                    imgAvatar.setImageResource(R.drawable.ic_person); // Reset to default
                }
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
                Toast.makeText(DoctorProfileActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void saveUrlToFirestore(String url, ProgressDialog progressDialog) {
        String doctorId = session.getUserId();
        if (doctorId == null) {
            progressDialog.dismiss();
            return;
        }

        String fieldToUpdate = isUpdatingCover ? "coverImageUrl" : "profileImageUrl";

        userRepo.updateUserField(doctorId, fieldToUpdate, url, task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) {
                if (isUpdatingCover) {
                    Glide.with(this).load(url).centerCrop().into(imgCover);
                } else {
                    Glide.with(this).load(url).centerCrop().into(imgAvatar);
                }
                Toast.makeText(this, "Photo updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to link photo", Toast.LENGTH_SHORT).show();
            }
        });
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
                    
                    if (user.getProfileImageUrl() != null) {
                        Glide.with(this).load(user.getProfileImageUrl()).centerCrop().into(imgAvatar);
                    }
                    if (user.getCoverImageUrl() != null) {
                        Glide.with(this).load(user.getCoverImageUrl()).centerCrop().into(imgCover);
                    }
                }
            } else {
                Toast.makeText(this, "User details not found", Toast.LENGTH_SHORT).show();
            }
        });
    }
}