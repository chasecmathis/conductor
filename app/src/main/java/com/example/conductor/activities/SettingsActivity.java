package com.example.conductor.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.conductor.R;

import java.util.HashSet;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "settings";
    private static final String SAMPLE_RATE_KEY = "sample_rate_key";
    private static final String SHUTTER_UPTIME_KEY = "shutter_uptime_key";
    private static final String UP_GESTURE_KEY = "up_gesture_key";
    private static final String DOWN_GESTURE_KEY = "down_gesture_key";
    private static final String PLAY_GESTURE_KEY = "play_gesture_key";
    private static final String PAUSE_GESTURE_KEY = "pause_gesture_key";
    private static final String SKIP_GESTURE_KEY = "skip_gesture_key";
    private static final String PREVIOUS_GESTURE_KEY = "previous_gesture_key";
    private static final String LIKE_GESTURE_KEY = "like_gesture_key";

    Spinner up_spinner;
    Spinner down_spinner;
    Spinner skip_spinner;
    Spinner previous_spinner;
    Spinner like_spinner;
    Spinner pause_spinner;
    Spinner play_spinner;

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



        //Setup gesture mapping
        up_spinner = findViewById(R.id.up_spinner);
        down_spinner = findViewById(R.id.down_spinner);
        skip_spinner = findViewById(R.id.skip_spinner);
        previous_spinner = findViewById(R.id.previous_spinner);
        like_spinner = findViewById(R.id.like_spinner);
        pause_spinner = findViewById(R.id.pause_spinner);
        play_spinner = findViewById(R.id.play_spinner);


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


        //Retrieve old values of mapping

        int storedValue = sharedPreferences.getInt(UP_GESTURE_KEY, 1);
        Log.d("TEST", String.valueOf(storedValue));
        up_spinner.setSelection(storedValue);
        storedValue = sharedPreferences.getInt(DOWN_GESTURE_KEY, 2);
        down_spinner.setSelection(storedValue);
        storedValue = sharedPreferences.getInt(SKIP_GESTURE_KEY, 5);
        skip_spinner.setSelection(storedValue);
        storedValue = sharedPreferences.getInt(PREVIOUS_GESTURE_KEY, 6);
        previous_spinner.setSelection(storedValue);
        storedValue = sharedPreferences.getInt(PLAY_GESTURE_KEY, 3);
        play_spinner.setSelection(storedValue);
        storedValue = sharedPreferences.getInt(PAUSE_GESTURE_KEY, 4);
        pause_spinner.setSelection(storedValue);
        storedValue = sharedPreferences.getInt(LIKE_GESTURE_KEY, 0);
        like_spinner.setSelection(storedValue);
    }


    public void settingsBackClicked(View v) {
        if(mappings_valid()) {
            store_mappings();
            this.finish();
        }
        else {
            Toast.makeText(getApplicationContext(), "Make sure every function has a unique gesture", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean mappings_valid(){
        Set<Integer> different = new HashSet<>();
        different.add(up_spinner.getSelectedItemPosition());
        different.add(down_spinner.getSelectedItemPosition());
        different.add(skip_spinner.getSelectedItemPosition());
        different.add(previous_spinner.getSelectedItemPosition());
        different.add(play_spinner.getSelectedItemPosition());
        different.add(pause_spinner.getSelectedItemPosition());
        different.add(like_spinner.getSelectedItemPosition());
        return different.size() == 7;
    }

    private void store_mappings(){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(UP_GESTURE_KEY, up_spinner.getSelectedItemPosition());
        editor.putInt(DOWN_GESTURE_KEY, down_spinner.getSelectedItemPosition());
        editor.putInt(PLAY_GESTURE_KEY, play_spinner.getSelectedItemPosition());
        editor.putInt(PAUSE_GESTURE_KEY, pause_spinner.getSelectedItemPosition());
        editor.putInt(SKIP_GESTURE_KEY, skip_spinner.getSelectedItemPosition());
        editor.putInt(PREVIOUS_GESTURE_KEY, previous_spinner.getSelectedItemPosition());
        editor.putInt(LIKE_GESTURE_KEY, like_spinner.getSelectedItemPosition());
        editor.apply();
    }
}
