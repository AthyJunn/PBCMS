package com.example.pbcms;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.File;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import de.hdodenhof.circleimageview.CircleImageView;

public class StaffProfileActivity extends AppCompatActivity {
    private MaterialButton btnSignOut;
    private ImageButton menuButton;
    private TextView userName, userEmailPhone, userEmployee;
    private CircleImageView profileImage;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ImageButton btnAddStaff, homeButton, historyButton, searchStaffButton, notificationsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide(); // Hide default action bar
        }

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        userName = findViewById(R.id.user_name);
        userEmailPhone = findViewById(R.id.user_email_phone);
        userEmployee = findViewById(R.id.employee_id);
        profileImage = findViewById(R.id.profile_image);

        menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(v -> showPopupMenu(v));

        btnSignOut = findViewById(R.id.btnSignOut);
        btnSignOut.setOnClickListener(v -> signOutUser());
        homeButton = findViewById(R.id.homeButton);
        historyButton = findViewById(R.id.historyButton);
        notificationsButton = findViewById(R.id.notificationsButton);

        updateButtonUI(homeButton);

        homeButton.setOnClickListener(v -> {
            updateButtonUI(homeButton);
            startActivity(new Intent(StaffProfileActivity.this, StaffHomeActivity.class));
        });

        historyButton.setOnClickListener(v -> {
            updateButtonUI(historyButton);
            startActivity(new Intent(StaffProfileActivity.this, StaffHistoryActivity.class));
        });

        notificationsButton.setOnClickListener(v -> {
            updateButtonUI(notificationsButton);
            startActivity(new Intent(StaffProfileActivity.this, StaffNotificationsActivity.class));
        });

        loadStaffProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStaffProfile(); // Reload updated profile when returning from EditProfileActivity
    }

    private void loadStaffProfile() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = currentUser.getEmail();

        db.collection("Staff")
                .whereEqualTo("Email", email)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);

                        String firstName = doc.getString("First Name");
                        String lastName = doc.getString("Last Name");
                        String phone = doc.getString("Phone Number");
                        String employeeNo = doc.getString("Employee#");
                        String fullName = firstName + " " + lastName;

                        userName.setText(fullName);
                        userEmailPhone.setText(email + " | " + phone);
                        userEmployee.setText(employeeNo);

                        // Load local profile image
                        File imgFile = new File(getFilesDir(), doc.getId() + "_profile.jpg");
                        if (imgFile.exists()) {
                            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                            profileImage.setImageBitmap(bitmap);
                        }

                    } else {
                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);

        // Add menu items manually
        popupMenu.getMenu().add(0, 1, 0, "Edit Profile");

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                // Open EditProfileActivity
                Intent intent = new Intent(StaffProfileActivity.this, StaffEditProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    private void signOutUser() {
        //Sign out current user
        FirebaseAuth.getInstance().signOut();

        // Show confirmation
        Toast.makeText(StaffProfileActivity.this, "Signed out successfully", Toast.LENGTH_SHORT).show();

        // Redirect to LoginActivity
        Intent intent = new Intent(StaffProfileActivity.this, SigninActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Close the current activity
    }

    private void updateButtonUI(ImageButton selectedButton) {
        // Reset all buttons first
        resetButton(homeButton);
        resetButton(historyButton);
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