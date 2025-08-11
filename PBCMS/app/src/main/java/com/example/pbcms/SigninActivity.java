package com.example.pbcms;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class SigninActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide(); // Hide the default action bar
        }
    }
}
