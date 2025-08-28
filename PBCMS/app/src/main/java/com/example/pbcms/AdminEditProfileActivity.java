package com.example.pbcms;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.provider.MediaStore;
import android.app.AlertDialog;

import java.util.Calendar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

public class AdminEditProfileActivity extends AppCompatActivity {

    private EditText firstname, lastname, email, phone, birthday, pwd;
    private ImageView profileImage;
    private ImageButton editProfileIcon, btnSave, btnBack;
    private MaterialButton btnChangePwd;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String currentCollection = "";
    private String uid = "";
    private static final int PICK_IMAGE_REQUEST = 1;

    private Uri selectedImageUri = null; // Holds temporarily selected image

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_edit_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        firstname = findViewById(R.id.firstname);
        lastname = findViewById(R.id.lastname);
        email = findViewById(R.id.email);
        phone = findViewById(R.id.phone);
        birthday = findViewById(R.id.birthday);
        pwd = findViewById(R.id.pwd);
        profileImage = findViewById(R.id.profileImage);
        editProfileIcon = findViewById(R.id.editProfileIcon);

        btnSave = findViewById(R.id.toolbar_right_icon);
        btnSave.setEnabled(false);
        btnBack = findViewById(R.id.btn_back);
        btnChangePwd = findViewById(R.id.btnChangePwd);

        btnBack.setOnClickListener(v -> finish());
        btnChangePwd.setOnClickListener(v -> startActivity(new Intent(this, AdminChangePwdActivity.class)));

        btnSave.setOnClickListener(v -> {
            if (!validateInputs()) {
                // Don't save, the first empty field will have focus and error message shown
                return;
            }
            saveUserProfile();

            if (selectedImageUri != null) {
                String localPath = saveImageLocally(selectedImageUri);
                updateFirestoreProfilePicture(localPath);
                selectedImageUri = null;
            }

            startActivity(new Intent(this, AdminProfileActivity.class));
        });

        birthday.setOnClickListener(v -> showDatePickerDialog());
        editProfileIcon.setOnClickListener(v -> showImagePickerDialog());

        loadUserProfile();
    }

    private boolean validateInputs() {
        if (firstname.getText().toString().trim().isEmpty()) {
            firstname.requestFocus();
            firstname.setError("First name is required");
            return false;
        }
        if (lastname.getText().toString().trim().isEmpty()) {
            lastname.requestFocus();
            lastname.setError("Last name is required");
            return false;
        }
        if (email.getText().toString().trim().isEmpty()) {
            email.requestFocus();
            email.setError("Email is required");
            return false;
        }
        if (phone.getText().toString().trim().isEmpty()) {
            phone.requestFocus();
            phone.setError("Phone number is required");
            return false;
        }
        if (birthday.getText().toString().trim().isEmpty()) {
            birthday.requestFocus();
            birthday.setError("Birthday is required");
            return false;
        }
        return true;
    }

    private void loadUserProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String userEmail = user.getEmail();

        db.collection("Admin")
                .whereEqualTo("Email", userEmail)
                .get()
                .addOnSuccessListener(adminQuery -> {
                    if (!adminQuery.isEmpty()) {
                        DocumentSnapshot doc = adminQuery.getDocuments().get(0);
                        fillProfile(doc, "Admin");
                    } else {
                        db.collection("Staff")
                                .whereEqualTo("Email", userEmail)
                                .get()
                                .addOnSuccessListener(staffQuery -> {
                                    if (!staffQuery.isEmpty()) {
                                        DocumentSnapshot doc = staffQuery.getDocuments().get(0);
                                        fillProfile(doc, "Staff");
                                    } else {
                                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Error fetching staff profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error fetching admin profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void fillProfile(DocumentSnapshot doc, String collectionName) {
        currentCollection = collectionName;
        uid = doc.getId();

        firstname.setText(doc.getString("First Name"));
        lastname.setText(doc.getString("Last Name"));
        email.setText(doc.getString("Email"));
        phone.setText(doc.getString("Phone Number"));
        birthday.setText(doc.getString("Birthday"));

        if (pwd != null) {
            pwd.setText("••••••");
            pwd.setFocusable(false);
            pwd.setClickable(false);
            pwd.setEnabled(false);
        }

        if (email != null) {
            email.setFocusable(false);
            email.setClickable(false);
            email.setEnabled(false);
        }

        btnSave.setEnabled(true);
        loadProfilePicture();
    }

    private void saveUserProfile() {
        if (currentCollection.isEmpty() || uid.isEmpty()) {
            Toast.makeText(this, "User collection not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("First Name", firstname.getText().toString().trim());
        updates.put("Last Name", lastname.getText().toString().trim());
        updates.put("Email", email.getText().toString().trim());
        updates.put("Phone Number", phone.getText().toString().trim());
        updates.put("Birthday", birthday.getText().toString().trim());

        db.collection(currentCollection).document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    birthday.setText(formattedDate);
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Profile Picture");
        builder.setItems(new CharSequence[]{"Choose from Gallery", "Remove Picture"}, (dialog, which) -> {
            switch (which) {
                case 0:
                    Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pickIntent, PICK_IMAGE_REQUEST);
                    break;
                case 1:
                    profileImage.setImageResource(R.drawable.ic_baseline_account_circle_24);
                    deleteProfilePictureFromStorage();
                    break;
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                Picasso.get().load(selectedImageUri).into(profileImage);
            } else {
                Toast.makeText(this, "Failed to get image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String saveImageLocally(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            File imageFile = new File(getFilesDir(), uid + "_profile.jpg");
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            return imageFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void updateFirestoreProfilePicture(String localPath) {
        if (localPath == null) return;

        db.collection(currentCollection).document(uid)
                .update("profilePictureUrl", localPath)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update picture URI", Toast.LENGTH_SHORT).show());
    }

    private void loadProfilePicture() {
        if (currentCollection.isEmpty() || uid.isEmpty()) return;

        db.collection(currentCollection).document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String localPath = doc.getString("profilePictureUrl");
                        if (localPath != null && !localPath.isEmpty()) {
                            File imgFile = new File(localPath);
                            if (imgFile.exists()) {
                                Picasso.get().load(imgFile).into(profileImage);
                            } else {
                                profileImage.setImageResource(R.drawable.ic_baseline_account_circle_24);
                            }
                        } else {
                            profileImage.setImageResource(R.drawable.ic_baseline_account_circle_24);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile picture", Toast.LENGTH_SHORT).show());
    }

    private void deleteProfilePictureFromStorage() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            db.collection("Admin").document(user.getUid())
                    .update("profilePictureUrl", null)
                    .addOnSuccessListener(aVoid -> {
                        File imageFile = new File(getFilesDir(), uid + "_profile.jpg");
                        if (imageFile.exists() && imageFile.delete()) {
                            Toast.makeText(this, "Profile picture removed", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to delete local file", Toast.LENGTH_SHORT).show();
                        }
                        profileImage.setImageResource(R.drawable.ic_baseline_account_circle_24);
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to remove profile picture", Toast.LENGTH_SHORT).show());
        }
    }
}