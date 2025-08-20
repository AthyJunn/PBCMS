package com.example.pbcms;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class AddStaffActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_staff);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide(); // Hide the default action bar
        }
    }
}