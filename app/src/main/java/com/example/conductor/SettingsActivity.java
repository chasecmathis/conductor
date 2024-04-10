package com.example.conductor;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_page);

        int color = Color.parseColor("#664C33");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(color);

        RadioGroup radioGroup = findViewById(R.id.imageRateOptions);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // Find which radio button is selected
                RadioButton radioButton = findViewById(checkedId);
                String selectedOption = radioButton.getText().toString();

                // Do something with the selected option
                Toast.makeText(SettingsActivity.this, "Selected Option: " + selectedOption, Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void settingsBackClicked(View v) {
        finish();
    }
}
