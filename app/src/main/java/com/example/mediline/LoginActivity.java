package com.example.mediline;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.mediline.model.User;
import com.example.mediline.repository.AuthRepository;
import com.example.mediline.repository.UserRepository;
import com.example.mediline.util.SessionManager;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText emailInput, passwordInput;
    private ProgressBar progressBar;
    private AuthRepository authRepo;
    private UserRepository userRepo;
    private SessionManager session;
    private String userRole;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        authRepo = new AuthRepository();
        userRepo = new UserRepository();
        session = new SessionManager(this);
        credentialManager = CredentialManager.create(this);

        userRole = getIntent().getStringExtra("USER_ROLE");
        if (userRole == null) userRole = "PATIENT";

        emailInput = findViewById(R.id.login_email);
        passwordInput = findViewById(R.id.login_password);
        progressBar = findViewById(R.id.login_progress);

        findViewById(R.id.login_sign_in_btn).setOnClickListener(v -> attemptLogin());

        findViewById(R.id.login_create_account).setOnClickListener(v -> {
            Intent intent = new Intent(this, SignUpActivity.class);
            intent.putExtra("USER_ROLE", userRole);
            startActivity(intent);
        });

        findViewById(R.id.login_google_btn).setOnClickListener(v -> signInWithGoogle());
    }

    // ─── Email/Password sign-in ───────────────────────────────────────
    private void attemptLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        authRepo.signInWithEmail(email, password, task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful() && task.getResult().getUser() != null) {
                String uid = task.getResult().getUser().getUid();
                handleSuccessfulAuth(uid, email);
            } else {
                String msg = task.getException() != null ? task.getException().getMessage() : getString(R.string.error_generic);
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ─── Google Sign-In via Credential Manager ────────────────────────
    private void signInWithGoogle() {
        String webClientId = getString(R.string.default_web_client_id);

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        progressBar.setVisibility(View.VISIBLE);

        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        runOnUiThread(() -> handleGoogleCredential(result));
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Log.e(TAG, "Google Sign-In failed", e);
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(LoginActivity.this,
                                    "Google Sign-In failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                }
        );
    }

    private void handleGoogleCredential(GetCredentialResponse response) {
        Credential credential = response.getCredential();

        if (credential instanceof CustomCredential) {
            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                GoogleIdTokenCredential googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(((CustomCredential) credential).getData());

                String idToken = googleIdTokenCredential.getIdToken();

                // Authenticate with Firebase using the Google ID token
                AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(this, task -> {
                            progressBar.setVisibility(View.GONE);
                            if (task.isSuccessful() && task.getResult().getUser() != null) {
                                String uid = task.getResult().getUser().getUid();
                                String email = task.getResult().getUser().getEmail();
                                String displayName = task.getResult().getUser().getDisplayName();

                                // Create user document if first time
                                boolean isNewUser = task.getResult().getAdditionalUserInfo() != null
                                        && task.getResult().getAdditionalUserInfo().isNewUser();

                                if (isNewUser) {
                                    User user = new User(null, email, userRole);
                                    userRepo.createUser(uid, user, dbTask -> {
                                        // Proceed regardless — Firestore write is best-effort
                                    });
                                }

                                session.saveLoginState(uid, userRole,
                                        displayName != null ? displayName : "",
                                        email != null ? email : "");
                                navigateToHome(userRole);
                            } else {
                                String msg = task.getException() != null
                                        ? task.getException().getMessage()
                                        : getString(R.string.error_generic);
                                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                            }
                        });
            }
        } else {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Unexpected credential type", Toast.LENGTH_SHORT).show();
        }
    }

    // ─── Shared post-auth logic ───────────────────────────────────────
    private void handleSuccessfulAuth(String uid, String email) {
        // Save session and navigate immediately — don't wait for Firestore
        session.saveLoginState(uid, userRole, "", email);
        navigateToHome(userRole);

        // Try to read Firestore user doc in background to update role if needed
        userRepo.getUser(uid, doc -> {
            if (doc.exists()) {
                String role = doc.getString("role");
                if (role != null && !role.equals(userRole)) {
                    session.saveLoginState(uid, role, "", email);
                }
            }
        });
    }

    private void navigateToHome(String role) {
        Intent intent;
        if ("DOCTOR".equals(role)) {
            intent = new Intent(this, DoctorDashboardActivity.class);
        } else {
            intent = new Intent(this, PatientHomeActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
