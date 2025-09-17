package com.example.pbcms;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Picasso;

import jp.wasabeef.picasso.transformations.CropCircleTransformation;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ViewStaffActivity extends AppCompatActivity {

    private ImageButton btnAddStaff, homeButton, historyButton, searchStaffButton, notificationsButton;
    private EditText searchEditText;
    private LinearLayout contentLayout;
    private List<View> staffViews = new ArrayList<>();

    private FirebaseFirestore db;
    private ListenerRegistration staffListener; // 🔥 real-time listener reference

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_staff);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide(); // Hide the default action bar
        }

        // Firebase instance
        db = FirebaseFirestore.getInstance();

        // UI components
        btnAddStaff = findViewById(R.id.addStaffBtn);
        searchEditText = findViewById(R.id.searchEditText);
        contentLayout = findViewById(R.id.contentLayout);

        homeButton = findViewById(R.id.homeButton);
        historyButton = findViewById(R.id.historyButton);
        searchStaffButton = findViewById(R.id.searchStaffButton);
        notificationsButton = findViewById(R.id.notificationsButton);

        updateButtonUI(searchStaffButton);

        homeButton.setOnClickListener(v -> {
            updateButtonUI(homeButton);
            startActivity(new Intent(ViewStaffActivity.this, AdminHomeActivity.class));
        });

        historyButton.setOnClickListener(v -> {
            updateButtonUI(historyButton);
            startActivity(new Intent(ViewStaffActivity.this, AdminHistoryActivity.class));
        });

        notificationsButton.setOnClickListener(v -> {
            updateButtonUI(notificationsButton);
            startActivity(new Intent(ViewStaffActivity.this, AdminNotificationsActivity.class));
        });

        // Add Staff button action
        btnAddStaff.setOnClickListener(v -> {
            Intent intent = new Intent(ViewStaffActivity.this, AddStaffActivity.class);
            startActivity(intent);
        });

        // Search logic
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStaffList(s.toString());
            }
        });

        // Load staff from Firestore in real-time
        startStaffListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop listening when activity is destroyed
        if (staffListener != null) {
            staffListener.remove();
        }
    }

    private void startStaffListener() {
        CollectionReference staffRef = db.collection("Staff");

        staffListener = staffRef.addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null) {
                Toast.makeText(this, "Failed to load staff: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (queryDocumentSnapshots == null) return;

            contentLayout.removeAllViews();
            staffViews.clear();

            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String firstName = doc.getString("First Name");
                String lastName = doc.getString("Last Name");
                String fullName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");

                View staffView = getLayoutInflater().inflate(R.layout.item_staff_card, contentLayout, false);
                TextView nameText = staffView.findViewById(R.id.staffName);
                nameText.setText(fullName.trim());

                ImageView profileImage = staffView.findViewById(R.id.profileImage);
                String imageUrl = doc.getString("profilePictureUrl");

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Picasso.get()
                            .load(new File(imageUrl))  // ✅ changed from File() to URL
                            .placeholder(R.drawable.ic_baseline_account_circle_24)
                            .transform(new CropCircleTransformation())
                            .into(profileImage);
                } else {
                    profileImage.setImageResource(R.drawable.ic_baseline_account_circle_12);
                }

                String email = doc.getString("Email"); // make sure your Firestore has "Email" field

                staffView.setOnClickListener(v -> {
                    Log.d("DEBUG", "Staff view clicked");
                    Intent intent = new Intent(ViewStaffActivity.this, EditStaffActivity.class);
                    intent.putExtra("staffEmail", email);
                    startActivity(intent);
                });

                staffViews.add(staffView);
                contentLayout.addView(staffView);
            }
        });
    }

    private void filterStaffList(String query) {
        for (View view : staffViews) {
            TextView nameText = view.findViewById(R.id.staffName);
            String name = nameText.getText().toString().toLowerCase();
            view.setVisibility(name.contains(query.toLowerCase()) ? View.VISIBLE : View.GONE);
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

