package com.example.pbcms;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import java.util.Calendar;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class AdminHomeActivity extends AppCompatActivity {

    private ImageButton profileButton, homeButton, historyButton, searchStaffButton, notificationsButton;
    private TextView temperature, humidity, doorStatus, wifiStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide(); // Hide the default action bar
        }

        profileButton = findViewById(R.id.profileButton);
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminHomeActivity.this, AdminProfileActivity.class);
                startActivity(intent);
            }
        });
        TextView firstName = findViewById(R.id.firstName);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String email = currentUser.getEmail();

            if (email != null) {
                // Query Staff collection by email
                db.collection("Staff")
                        .whereEqualTo("Email", email)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                // Get first matching document
                                String name = querySnapshot.getDocuments().get(0).getString("First Name");
                                if (name != null && !name.isEmpty()) {
                                    firstName.setText(name);
                                } else {
                                    firstName.setText("");
                                }
                            } else {
                                // If not found in Staff, check Admin collection by email
                                db.collection("Admin")
                                        .whereEqualTo("Email", email)
                                        .get()
                                        .addOnSuccessListener(adminQuerySnapshot -> {
                                            if (!adminQuerySnapshot.isEmpty()) {
                                                String name = adminQuerySnapshot.getDocuments().get(0).getString("First Name");
                                                if (name != null && !name.isEmpty()) {
                                                    firstName.setText(name);
                                                } else {
                                                    firstName.setText("");
                                                }
                                            } else {
                                                // Not found in Admin either
                                                firstName.setText("");
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            firstName.setText("");
                                            Log.e("AdminHomeActivity", "Failed to load admin name", e);
                                        });
                            }
                        })
                        .addOnFailureListener(e -> {
                            firstName.setText("");
                            Log.e("AdminHomeActivity", "Failed to load staff name", e);
                        });
            } else {
                // Email null fallback
                firstName.setText("");
                Log.e("AdminHomeActivity", "Current user email is null");
            }
        }

        ImageView infoIcon = findViewById(R.id.infoIcon);
        DatabaseReference lastOpenRef = FirebaseDatabase.getInstance().getReference("lastOpenTime");

        lastOpenRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String lastOpenTime = snapshot.getValue(String.class);
                if (lastOpenTime == null) lastOpenTime = "No data";

                String tooltipText = "Last opened: " + lastOpenTime;

                // Tooltip requires API 26+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    infoIcon.setTooltipText(tooltipText);
                } else {
                    // For older versions, you can fallback to Toast on click:
                    infoIcon.setOnClickListener(v -> {
                        Toast.makeText(getApplicationContext(), tooltipText, Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error if needed
            }
        });

        TextView greetingMessage = findViewById(R.id.greetingMessage); // Make sure this matches your XML ID
        greetingMessage.setText(getGreeting());

        temperature = findViewById(R.id.tempValueCard);
        humidity = findViewById(R.id.humidityValueCard);
        doorStatus = findViewById(R.id.doorStatus);
        wifiStatus = findViewById(R.id.wifiStatus);

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        rootRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer tempVal = snapshot.child("temperature").getValue(Integer.class);
                    Integer humidityVal = snapshot.child("humidity").getValue(Integer.class);
                    String doorStatusVal = snapshot.child("doorStatus").getValue(String.class);
                    String wifiStatusVal = snapshot.child("wifiStatus").getValue(String.class);

                    temperature.setText(tempVal != null ? String.valueOf(tempVal) : "--");
                    humidity.setText(humidityVal != null ? String.valueOf(humidityVal) : "--");
                    doorStatus.setText(doorStatusVal != null ? doorStatusVal : "--");
                    wifiStatus.setText(wifiStatusVal != null ? wifiStatusVal : "--");

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Failed to read data", error.toException());
            }
        });

        homeButton = findViewById(R.id.homeButton);
        historyButton = findViewById(R.id.historyButton);
        searchStaffButton = findViewById(R.id.searchStaffButton);
        notificationsButton = findViewById(R.id.notificationsButton);

        updateButtonUI(homeButton);

        homeButton.setOnClickListener(v -> {
            updateButtonUI(homeButton);
            startActivity(new Intent(AdminHomeActivity.this, AdminHomeActivity.class));
        });

        historyButton.setOnClickListener(v -> {
            updateButtonUI(historyButton);
            startActivity(new Intent(AdminHomeActivity.this, AdminHistoryActivity.class));
        });

        searchStaffButton.setOnClickListener(v -> {
            updateButtonUI(searchStaffButton);
            startActivity(new Intent(AdminHomeActivity.this, StaffViewActivity.class));
        });

        notificationsButton.setOnClickListener(v -> {
            updateButtonUI(notificationsButton);
            startActivity(new Intent(AdminHomeActivity.this, AdminNotificationsActivity.class));
        });

    }
    private String getGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY); // 24-hour format (0 - 23)

        if (hour >= 5 && hour < 12) {
            return "Good Morning";
        } else if (hour >= 12 && hour < 17) {
            return "Good Afternoon";
        } else if (hour >= 17 && hour < 21) {
            return "Good Evening";
        } else {
            return "Good Night";
        }
    }

    private void updateButtonUI(ImageButton selectedButton) {
        // Reset all buttons first
        resetButton(homeButton);
        resetButton(historyButton);
        resetButton(searchStaffButton);
        resetButton(notificationsButton);

        // Apply selected style to the clicked one
        selectedButton.setBackgroundResource(R.drawable.baseline_circle_24); // white circular background
        selectedButton.setColorFilter(getResources().getColor(android.R.color.black)); // black icon
    }

    private void resetButton(ImageButton button) {
        button.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        button.setColorFilter(getResources().getColor(android.R.color.white));
    }
}