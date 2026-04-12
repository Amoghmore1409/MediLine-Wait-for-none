package com.example.mediline;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class OtpVerificationActivity extends AppCompatActivity {

    private EditText otp1, otp2, otp3, otp4;
    private TextView timerText, resendBtn;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp_verification);

        userRole = getIntent().getStringExtra("USER_ROLE");
        if (userRole == null) userRole = "PATIENT";

        otp1 = findViewById(R.id.otp_1);
        otp2 = findViewById(R.id.otp_2);
        otp3 = findViewById(R.id.otp_3);
        otp4 = findViewById(R.id.otp_4);
        timerText = findViewById(R.id.otp_timer);
        resendBtn = findViewById(R.id.otp_resend_btn);

        // Auto-focus next input
        setupAutoFocus(otp1, otp2);
        setupAutoFocus(otp2, otp3);
        setupAutoFocus(otp3, otp4);
        setupAutoFocus(otp4, null);

        // Start countdown
        startCountdown();

        // Verify button
        findViewById(R.id.otp_verify_btn).setOnClickListener(v -> verifyOtp());

        resendBtn.setOnClickListener(v -> {
            if (resendBtn.getAlpha() == 1.0f) {
                Toast.makeText(this, "OTP resent!", Toast.LENGTH_SHORT).show();
                startCountdown();
            }
        });
    }

    private void setupAutoFocus(EditText current, EditText next) {
        current.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1 && next != null) {
                    next.requestFocus();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void startCountdown() {
        resendBtn.setAlpha(0.5f);
        resendBtn.setClickable(false);

        new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secs = millisUntilFinished / 1000;
                timerText.setText(String.format(getString(R.string.otp_resend_timer),
                        String.format("00:%02d", secs)));
            }

            @Override
            public void onFinish() {
                timerText.setText("");
                resendBtn.setAlpha(1.0f);
                resendBtn.setClickable(true);
            }
        }.start();
    }

    private void verifyOtp() {
        String code = otp1.getText().toString() + otp2.getText().toString()
                + otp3.getText().toString() + otp4.getText().toString();

        if (code.length() < 4) {
            Toast.makeText(this, "Please enter the complete OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        // For now, any 4-digit code works as a demo
        Toast.makeText(this, "Verified successfully!", Toast.LENGTH_SHORT).show();

        Intent intent;
        if ("DOCTOR".equals(userRole)) {
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
