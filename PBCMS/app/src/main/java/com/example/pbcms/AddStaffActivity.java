package com.example.pbcms;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;
import jp.wasabeef.picasso.transformations.CropCircleTransformation;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AddStaffActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int EMAIL_REQUEST_CODE = 101;

    private EditText etFirstName, etLastName, etPhone, etEmail, etBirthday;
    private MaterialButton btnAdd;
    private ImageView ivProfileImage;
    private ImageButton ivProfileIcon, btnBack;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private Uri selectedImageUri = null;
    private String uid = null;
    private boolean isRemovePictureRequested = false;
    private String employeeNumber = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_staff);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etFirstName = findViewById(R.id.firstNameInput);
        etLastName = findViewById(R.id.lastNameInput);
        etPhone = findViewById(R.id.phoneInput);
        etEmail = findViewById(R.id.emailInput);
        etBirthday = findViewById(R.id.birthdayInput);
        btnAdd = findViewById(R.id.addButton);
        btnBack = findViewById(R.id.backButton);
        ivProfileImage = findViewById(R.id.profileImage);
        ivProfileIcon = findViewById(R.id.editProfileIcon);

        ivProfileIcon.setOnClickListener(v -> {
            showImagePickerDialog(); // Make sure this method is correct
            Toast.makeText(this, "Profile icon clicked!", Toast.LENGTH_SHORT).show();
        });

        etBirthday.setOnClickListener(v -> showDatePicker());
        btnAdd.setOnClickListener(v -> generateEmployeeNumberAndAddStaff());
        btnBack.setOnClickListener(v -> finish());
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
                    etBirthday.setText(selectedDate);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void generateEmployeeNumberAndAddStaff() {
        db.collection("Staff")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size() + 1;
                    employeeNumber = String.format("%05d", count); // e.g. 00001
                    startAddStaffProcess();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error generating employee number: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void startAddStaffProcess() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String birthday = etBirthday.getText().toString().trim();

        if (firstName.isEmpty()) {
            etFirstName.setError("First name is required");
            etFirstName.requestFocus();
            return;
        }
        if (lastName.isEmpty()) {
            etLastName.setError("Last name is required");
            etLastName.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }
        if (phone.isEmpty()) {
            etPhone.setError("Phone number is required");
            etPhone.requestFocus();
            return;
        }
        if (birthday.isEmpty()) {
            etBirthday.setError("Birthday is required");
            etBirthday.requestFocus();
            return;
        }

        db.collection("Staff")
                .whereEqualTo("Email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        etEmail.setError("Email already exists");
                        etEmail.requestFocus();
                    } else {
                        String generatedPassword = generateRandomPassword(6);
                        createFirebaseUser(firstName, lastName, email, phone, birthday, generatedPassword);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error checking email: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void createFirebaseUser(String firstName, String lastName, String email, String phone,
                                    String birthday, String password) {

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        uid = auth.getCurrentUser().getUid();
                        String localPath = selectedImageUri != null ? saveImageLocally(selectedImageUri) : null;

                        Map<String, Object> staff = new HashMap<>();
                        staff.put("First Name", firstName);
                        staff.put("Last Name", lastName);
                        staff.put("Email", email);
                        staff.put("Phone Number", phone);
                        staff.put("Birthday", birthday);
                        staff.put("Employee#", employeeNumber);
                        staff.put("profilePictureUrl", isRemovePictureRequested ? null : localPath);

                        db.collection("Staff").document(uid)
                                .set(staff)
                                .addOnSuccessListener(aVoid -> {
                                    if (isRemovePictureRequested) {
                                        deleteProfilePictureFromStorage();
                                    }
                                    Toast.makeText(this, "Staff added successfully!", Toast.LENGTH_SHORT).show();
                                    sendEmailWithPassword(email, firstName, password);
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Error saving staff data: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    } else {
                        Toast.makeText(this, "Failed to create user: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void sendEmailWithPassword(String email, String firstName, String password) {
        String subject = "Your Staff Account Credentials";
        String message = "Hi " + firstName + ",\n\n"
                + "Your staff account has been created.\n"
                + "Here is your temporary password:\n\n"
                + password + "\n\n"
                + "Please change it after you log in for the first time.\n\n"
                + "Best regards,\nAdmin Team";

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, message);

        try {
            startActivityForResult(Intent.createChooser(emailIntent, "Send email using..."), EMAIL_REQUEST_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No email client found to send credentials.", Toast.LENGTH_LONG).show();
            // Fallback: navigate anyway
            navigateToViewStaff();
        }
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
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
                    ivProfileImage.setImageResource(R.drawable.ic_baseline_account_circle_24);
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

        if (requestCode == EMAIL_REQUEST_CODE) {
            navigateToViewStaff();  // User returned from email client
        }

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            isRemovePictureRequested = false;

            Picasso.get()
                    .load(selectedImageUri)
                    .transform(new CropCircleTransformation())
                    .into(ivProfileImage);
        } else if (isRemovePictureRequested) {
            // Set default icon and apply round transformation
            Picasso.get()
                    .load(R.drawable.ic_baseline_account_circle_24)
                    .transform(new CropCircleTransformation())
                    .into(ivProfileImage);
        } else {
            Toast.makeText(this, "Failed to get image", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToViewStaff() {
        Intent intent = new Intent(AddStaffActivity.this, ViewStaffActivity.class);
        startActivity(intent);
        finish();
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

    private void deleteProfilePictureFromStorage() {
        if (uid != null && !uid.isEmpty()) {
            db.collection("Staff").document(uid)
                    .update("profilePictureUrl", null)
                    .addOnSuccessListener(aVoid -> {
                        File imageFile = new File(getFilesDir(), uid + "_profile.jpg");
                        if (imageFile.exists() && imageFile.delete()) {
                            Toast.makeText(this, "Profile picture removed", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Local file not found or couldn't be deleted", Toast.LENGTH_SHORT).show();
                        }
                        ivProfileImage.setImageResource(R.drawable.ic_baseline_account_circle_24);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to remove profile picture from Firestore", Toast.LENGTH_SHORT).show());
        }
    }
}


