package com.example.conductor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "settings";
    private static final String SAMPLE_RATE_KEY = "sample_rate_key";
    private static final String SHUTTER_UPTIME_KEY = "shutter_uptime_key";

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_page);

        int color = Color.parseColor("#664C33");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(color);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        RadioGroup imageRateGroup = findViewById(R.id.imageRateOptions);
        imageRateGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // Find which radio button is selected
                RadioButton radioButton = findViewById(checkedId);
                String imageRate = radioButton.getText().toString();

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(SAMPLE_RATE_KEY, imageRate);
                editor.apply();
            }
        });

        RadioGroup shutterUptimeGroup = findViewById(R.id.shutterUptimeOptions);
        shutterUptimeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // Find which radio button is selected
                RadioButton radioButton = findViewById(checkedId);
                String shutterUptime = radioButton.getText().toString();

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(SHUTTER_UPTIME_KEY, shutterUptime);
                editor.apply();
            }
        });

    }

    protected void onResume() {
        super.onResume();

        // Retrieve old settings and set the correct radio buttons
        String imageRate = sharedPreferences.getString(SAMPLE_RATE_KEY, "");
        Log.d("DEBUG", "xCyx: image rate: " + imageRate);
        if (!imageRate.isEmpty()) {
            RadioGroup imageRateGroup = findViewById(R.id.imageRateOptions);
            for (int i = 0; i < imageRateGroup.getChildCount(); i++) {
                RadioButton imageRateButton = (RadioButton) imageRateGroup.getChildAt(i);
                if (imageRateButton.getText().toString().equals(imageRate)) {
                    imageRateButton.setChecked(true);
                }
            }
        }

        String shutterUptime = sharedPreferences.getString(SHUTTER_UPTIME_KEY, "");
        Log.d("DEBUG", "xCyx: shutter uptime: " + shutterUptime);
        if (!shutterUptime.isEmpty()) {
            RadioGroup shutterUptimeGroup = findViewById(R.id.shutterUptimeOptions);
            for (int i = 0; i < shutterUptimeGroup.getChildCount(); i++) {
                RadioButton shutterUptimeButton = (RadioButton) shutterUptimeGroup.getChildAt(i);
                if (shutterUptimeButton.getText().toString().equals(shutterUptime)) {
                    shutterUptimeButton.setChecked(true);
                }
            }
        }
    }


    public void settingsBackClicked(View v) {
        this.finish();
    }
}
