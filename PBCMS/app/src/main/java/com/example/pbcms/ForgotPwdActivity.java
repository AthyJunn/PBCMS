package com.example.pbcms;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPwdActivity extends AppCompatActivity {

    private TextInputEditText emailInput;
    private MaterialButton sendResetLinkButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_pwd);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide(); // Hide the default action bar
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        Log.d("FirebaseAuth", "🔥 FirebaseAuth initialized successfully!");

        ImageButton backButton = findViewById(R.id.btnBack);
        emailInput = findViewById(R.id.etEmail);
        sendResetLinkButton = findViewById(R.id.btnReset);

        backButton.setOnClickListener(v -> finish());

        // Send Reset Link functionality
        sendResetLinkButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(ForgotPwdActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ForgotPwdActivity.this, "Reset link sent! Check your email.",
                                    Toast.LENGTH_LONG).show();
                            Log.d("ForgotPassword", "Password reset email sent.");
                        } else {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() :
                                    "Unknown error occurred";
                            Toast.makeText(ForgotPwdActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                            Log.e("ForgotPassword", "Failed to send reset email: " + errorMessage);
                        }
                    });
        });

    }
}
