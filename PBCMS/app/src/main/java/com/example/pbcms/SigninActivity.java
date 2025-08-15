package com.example.pbcms;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.example.pbcms.AdminHomeActivity;
import com.example.pbcms.StaffHomeActivity;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SigninActivity extends AppCompatActivity {
    private Button btnSignIn;
    private TextView forgotPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide(); // Hide the default action bar
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnSignIn = findViewById(R.id.btnSignIn);
        forgotPassword = findViewById(R.id.forgotPassword);

        btnSignIn.setOnClickListener(v -> signInUser());

        forgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(SigninActivity.this, ForgotPwdActivity.class);
            startActivity(intent);
        });
    }

    private void signInUser() {
        TextInputLayout emailLayout = findViewById(R.id.emailLayout);
        TextInputLayout passwordLayout = findViewById(R.id.passwordLayout);
        TextInputEditText etEmail = findViewById(R.id.etEmail);
        TextInputEditText etPassword = findViewById(R.id.etPassword);

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        boolean hasError = false;

        // Email validation
        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            hasError = true;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Enter a valid email");
            hasError = true;
        } else {
            emailLayout.setError(null);
        }

        // Password validation
        if (password.isEmpty()) {
            passwordLayout.setError("Password is required");
            hasError = true;
        } else if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            hasError = true;
        } else {
            passwordLayout.setError(null);
        }

        // Stop if validation failed
        if (hasError) {
            return;
        }

        //firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // After successful sign-in, check Firestore by email field
                    db.collection("Staff")
                            .whereEqualTo("Email", email)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (!querySnapshot.isEmpty()) {
                                    navigateToHomeScreen("Staff");
                                } else {
                                    db.collection("Admin")
                                            .whereEqualTo("Email", email)
                                            .get()
                                            .addOnSuccessListener(adminQuerySnapshot -> {
                                                if (!adminQuerySnapshot.isEmpty()) {
                                                    navigateToHomeScreen("Admin");
                                                } else {
                                                    Toast.makeText(this, "User not found in staff or admin collections", Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this, "Error checking admin role: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                            );
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error checking staff role: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void navigateToHomeScreen(String userType) {
        Intent intent;
        if (userType.equals("Admin")) {
            intent = new Intent(SigninActivity.this, AdminHomeActivity.class);
        } else if (userType.equals("Staff")) {
            intent = new Intent(SigninActivity.this, StaffHomeActivity.class);
        } else {
            Toast.makeText(SigninActivity.this, "Unknown user type", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(intent);
        finish();
    }
}