package com.example.pbcms;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AdminChangePwdActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_change_pwd);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide(); // Hide the default action bar
        }

        backButton=findViewById(R.id.btnBack);
        backButton.setOnClickListener(v -> finish());

        auth = FirebaseAuth.getInstance();
    }

    public void onChangePassword(View view) {
        TextInputEditText currentPasswordField = findViewById(R.id.etPassword);
        TextInputLayout currentPasswordLayout= findViewById(R.id.currentPasswordLayout);
        TextInputEditText newPasswordField = findViewById(R.id.etNewPassword);
        TextInputLayout newPasswordLayout = findViewById(R.id.newPasswordLayout);
        TextInputEditText confirmNewPasswordField = findViewById(R.id.confirmPassword);
        TextInputLayout confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);

        String currentPassword = currentPasswordField.getText().toString().trim();
        String newPassword = newPasswordField.getText().toString().trim();
        String confirmNewPassword = confirmNewPasswordField.getText().toString().trim();

        // Clear previous errors
        currentPasswordLayout.setError(null);
        newPasswordLayout.setError(null);
        confirmPasswordLayout.setError(null);

        if (!newPassword.equals(confirmNewPassword)) {
            Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean hasError = false;

        // Password validation
        if (currentPassword.isEmpty()) {
            currentPasswordLayout.setError("Password is required");
            hasError = true;
        }
        if (newPassword.isEmpty()) {
            newPasswordLayout.setError("Please fill out this field");
            hasError = true;
        }
        if (confirmNewPassword.isEmpty()) {
            confirmPasswordLayout.setError("Please fill out this field");
            hasError = true;
        }
        if (newPassword.length() < 6) {
            newPasswordLayout.setError("Password must be at least 6 characters");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

            user.reauthenticate(credential).addOnSuccessListener(aVoid -> {
                user.updatePassword(newPassword)
                        .addOnSuccessListener(aVoid1 -> {
                            Toast.makeText(AdminChangePwdActivity.this, "Password changed successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(AdminChangePwdActivity.this, "Failed to change password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }).addOnFailureListener(e -> {
                Toast.makeText(AdminChangePwdActivity.this, "Re-authentication failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(AdminChangePwdActivity.this, "User not logged in.", Toast.LENGTH_SHORT).show();
        }

    }

}

