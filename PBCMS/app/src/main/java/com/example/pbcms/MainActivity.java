package com.example.pbcms;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide(); // Hide the default action bar
        }

        // Firebase initialization
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:71197206218:android:5dac539c10bebbec823633")
                    .setApiKey("AIzaSyAuJjF7dME30MFvqMLxfgXZeG19rGnMFak")
                    .setDatabaseUrl("https://pbcms-c955a-default-rtdb.firebaseio.com")
                    .setProjectId("pbcms-c955a")
                    .setStorageBucket("pbcms-c955a.firebasestorage.app") // Corrected
                    .build();

            FirebaseApp.initializeApp(this, options);
        }

        Button startButton = findViewById(R.id.getStartedButton);
        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SigninActivity.class);
            startActivity(intent);
        });
    }
}