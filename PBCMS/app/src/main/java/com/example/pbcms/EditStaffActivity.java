package com.example.pbcms;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;
import jp.wasabeef.picasso.transformations.CropCircleTransformation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditStaffActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText firstNameInput, lastNameInput, emailInput, phoneInput, birthDateInput;
    private MaterialButton confirmButton;
    private ImageButton backButton, menuButton;
    private ImageView profileImage;
    private ImageButton editProfileIcon;

    private String staffEmail;
    private String staffDocumentId;
    private String staffUid;
    private boolean isRemovePictureRequested = false;

    private Uri selectedImageUri = null;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_staff);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        staffEmail = getIntent().getStringExtra("staffEmail");
        if (staffEmail == null || staffEmail.isEmpty()) {
            Toast.makeText(this, "Staff member not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initializeUI();
        loadStaffDataByEmail();
        setupListeners();
    }

    private void initializeUI() {
        backButton = findViewById(R.id.backButton);
        menuButton = findViewById(R.id.menu_button);
        firstNameInput = findViewById(R.id.firstNameInput);
        lastNameInput = findViewById(R.id.lastNameInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        birthDateInput = findViewById(R.id.birthDateInput);
        confirmButton = findViewById(R.id.confirmButton);
        profileImage = findViewById(R.id.profileImage);
        editProfileIcon = findViewById(R.id.editProfileIcon);

        firstNameInput.setTextColor(getResources().getColor(android.R.color.black));
        lastNameInput.setTextColor(getResources().getColor(android.R.color.black));
        emailInput.setTextColor(getResources().getColor(android.R.color.black));
        phoneInput.setTextColor(getResources().getColor(android.R.color.black));
        birthDateInput.setTextColor(getResources().getColor(android.R.color.black));

        menuButton.setOnClickListener(this::showPopupMenu);

        // Set up profile picture functionality
        editProfileIcon.setOnClickListener(v -> showImagePickerDialog());
        birthDateInput.setOnClickListener(v -> showDatePicker());
    }

    private void loadStaffDataByEmail() {
        db.collection("Staff")
                .whereEqualTo("Email", staffEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                            staffDocumentId = document.getId();
                            staffUid = document.getId(); // Using document ID as UID

                            String firstName = document.getString("First Name");
                            String lastName = document.getString("Last Name");
                            String email = document.getString("Email");
                            String phone = document.getString("Phone Number");
                            String birthDate = document.getString("Birthday");
                            String profilePictureUrl = document.getString("profilePictureUrl");

                            firstNameInput.setText(firstName != null ? firstName : "");
                            lastNameInput.setText(lastName != null ? lastName : "");
                            emailInput.setText(email != null ? email : "");
                            phoneInput.setText(phone != null ? phone : "");
                            birthDateInput.setText(birthDate != null ? birthDate : "");

                            // Load profile picture if exists
                            if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                                File imgFile = new File(profilePictureUrl);
                                if (imgFile.exists()) {
                                    Picasso.get()
                                            .load(imgFile)
                                            .transform(new CropCircleTransformation())
                                            .into(profileImage);
                                } else {
                                    // Set default profile picture
                                    setDefaultProfilePicture();
                                }
                            } else {
                                // Set default profile picture
                                setDefaultProfilePicture();
                            }

                        } else {
                            Toast.makeText(this, "Staff member not found", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Error loading staff data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void setDefaultProfilePicture() {
        Picasso.get()
                .load(R.drawable.ic_baseline_account_circle_24)
                .transform(new CropCircleTransformation())
                .into(profileImage);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
        confirmButton.setOnClickListener(v -> updateStaff());
    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenu().add(0, 1, 0, "Delete Staff");

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                deleteStaff();
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    private void updateStaff() {
        String firstName = firstNameInput.getText().toString().trim();
        String lastName = lastNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String birthDate = birthDateInput.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill in required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (staffDocumentId == null) {
            Toast.makeText(this, "Staff data not loaded properly", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference staffRef = db.collection("Staff").document(staffDocumentId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("First Name", firstName);
        updates.put("Last Name", lastName);
        updates.put("Email", email);
        updates.put("Phone Number", phone);
        updates.put("Birthday", birthDate);

        // Handle profile picture update
        if (isRemovePictureRequested) {
            updates.put("profilePictureUrl", null);
            deleteProfilePictureFromStorage();
        } else if (selectedImageUri != null) {
            String localPath = saveImageLocally(selectedImageUri);
            if (localPath != null) {
                updates.put("profilePictureUrl", localPath);
            }
        }

        // Update Firestore first
        staffRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Staff updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating staff: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
                    setDefaultProfilePicture();
                    selectedImageUri = null;
                    isRemovePictureRequested = true;
                    break;
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            isRemovePictureRequested = false;

            Picasso.get()
                    .load(selectedImageUri)
                    .transform(new CropCircleTransformation())
                    .into(profileImage);
        } else if (isRemovePictureRequested) {
            // Set default icon and apply round transformation
            setDefaultProfilePicture();
        } else {
            Toast.makeText(this, "Failed to get image", Toast.LENGTH_SHORT).show();
        }
    }

    private String saveImageLocally(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            File imageFile = new File(getFilesDir(), staffUid + "_profile.jpg");
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

    private void deleteProfilePictureFromStorage() {
        if (staffUid != null && !staffUid.isEmpty()) {
            File imageFile = new File(getFilesDir(), staffUid + "_profile.jpg");
            if (imageFile.exists() && imageFile.delete()) {
                Toast.makeText(this, "Profile picture removed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Local file not found or couldn't be deleted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, month1, dayOfMonth) -> {
                    String selectedDate = dayOfMonth + "/" + (month1 + 1) + "/" + year1;
                    birthDateInput.setText(selectedDate);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void deleteStaff() {
        if (staffDocumentId == null) {
            Toast.makeText(this, "Staff data not loaded properly", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference staffRef = db.collection("Staff").document(staffDocumentId);

        staffRef.delete()
                .addOnSuccessListener(aVoid -> {
                    // Also delete the profile picture if exists
                    deleteProfilePictureFromStorage();
                    Toast.makeText(this, "Staff deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error deleting staff: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}