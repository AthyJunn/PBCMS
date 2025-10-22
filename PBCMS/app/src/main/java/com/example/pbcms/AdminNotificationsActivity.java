package com.example.pbcms;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminNotificationsActivity extends AppCompatActivity {

    private ImageButton homeButton, historyButton, notificationsButton, searchStaffButton;
    private LinearLayout temperatureNotification, humidityNotification, doorNotification, emptyState;
    private TextView temperatureTitle, temperatureMessage, humidityTitle, humidityMessage, doorTitle, doorMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_notifications);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Initialize views
        initializeViews();
        setupBottomNavigation();
        hideAllNotifications();
        emptyState.setVisibility(View.VISIBLE);

        // ✅ Realtime database listener
        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("notifications");
        notifRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                hideAllNotifications();

                if (!snapshot.exists()) {
                    emptyState.setVisibility(View.VISIBLE);
                    return;
                }

                emptyState.setVisibility(View.GONE);

                for (DataSnapshot notifSnap : snapshot.getChildren()) {
                    String title = notifSnap.child("title").getValue(String.class);
                    String message = notifSnap.child("message").getValue(String.class);
                    updateNotificationUI(title, message);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to read notifications", error.toException());
            }
        });
    }

    private void initializeViews() {
        temperatureNotification = findViewById(R.id.temperatureNotif);
        humidityNotification = findViewById(R.id.humidityNotification);
        doorNotification = findViewById(R.id.doorNotification);
        emptyState = findViewById(R.id.emptyState);

        temperatureTitle = findViewById(R.id.temperatureTitle);
        temperatureMessage = findViewById(R.id.temperatureMessage);

        humidityTitle = findViewById(R.id.humidityTitle);
        humidityMessage = findViewById(R.id.humidityMessage);

        doorTitle = findViewById(R.id.doorTitle);
        doorMessage = findViewById(R.id.doorMessage);
    }

    private void updateNotificationUI(String title, String message) {
        if (title == null || message == null) return;

        String lowerTitle = title.toLowerCase();

        if (lowerTitle.contains("temperature")) {
            temperatureTitle.setText(title);
            temperatureMessage.setText(message);
            temperatureNotification.setVisibility(View.VISIBLE);
            findViewById(R.id.temperatureDivider).setVisibility(View.VISIBLE);
        } else if (lowerTitle.contains("humidity")) {
            humidityTitle.setText(title);
            humidityMessage.setText(message);
            humidityNotification.setVisibility(View.VISIBLE);
            findViewById(R.id.humidityDivider).setVisibility(View.VISIBLE);
        } else if (lowerTitle.contains("door")) {
            doorTitle.setText(title);
            doorMessage.setText(message);
            doorNotification.setVisibility(View.VISIBLE);
        }
    }

    private void hideAllNotifications() {
        temperatureNotification.setVisibility(View.GONE);
        humidityNotification.setVisibility(View.GONE);
        doorNotification.setVisibility(View.GONE);
        findViewById(R.id.temperatureDivider).setVisibility(View.GONE);
        findViewById(R.id.humidityDivider).setVisibility(View.GONE);
    }

    private void setupBottomNavigation() {
        homeButton = findViewById(R.id.homeButton);
        historyButton = findViewById(R.id.historyButton);
        notificationsButton = findViewById(R.id.notificationsButton);
        searchStaffButton = findViewById(R.id.searchStaffButton);

        updateButtonUI(notificationsButton);

        homeButton.setOnClickListener(v -> {
            updateButtonUI(homeButton);
            startActivity(new Intent(this, AdminHomeActivity.class));
            finish();
        });

        historyButton.setOnClickListener(v -> {
            updateButtonUI(historyButton);
            startActivity(new Intent(this, AdminHistoryActivity.class));
            finish();
        });

        notificationsButton.setOnClickListener(v -> updateButtonUI(notificationsButton));

        searchStaffButton.setOnClickListener(v -> {
            updateButtonUI(searchStaffButton);
            startActivity(new Intent(this, ViewStaffActivity.class));
            finish();
        });
    }

    private void updateButtonUI(ImageButton selectedButton) {
        resetButton(homeButton);
        resetButton(historyButton);
        resetButton(notificationsButton);
        resetButton(searchStaffButton);

        selectedButton.setBackgroundResource(R.drawable.baseline_circle_24);
        selectedButton.setColorFilter(getResources().getColor(android.R.color.black));
    }

    private void resetButton(ImageButton button) {
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setColorFilter(getResources().getColor(android.R.color.white));
    }
}