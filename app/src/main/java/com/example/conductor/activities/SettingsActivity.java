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

import androidx.appcompat.app.AppCompatActivity;

import com.example.conductor.R;

import java.util.HashSet;
import java.util.Set;

/**
 * This activity allows users to adjust settings related to image rate, shutter uptime, and gesture mappings.
 */
public class SettingsActivity extends AppCompatActivity {

    // Keys for SharedPreferences
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

    // Spinners for gesture mappings
    Spinner up_spinner;
    Spinner down_spinner;
    Spinner skip_spinner;
    Spinner previous_spinner;
    Spinner like_spinner;
    Spinner pause_spinner;
    Spinner play_spinner;

    // SharedPreferences object
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_page);

        // Set status bar color
        int color = Color.parseColor("#664C33");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(color);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Setup radio button groups
        setupRadioGroups();

        // Setup gesture mappings
        setupGestureMappings();
    }

    /**
     * Sets up radio button groups for image rate and shutter uptime settings.
     */
    private void setupRadioGroups() {
        RadioGroup imageRateGroup = findViewById(R.id.imageRateOptions);
        imageRateGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton radioButton = findViewById(checkedId);
            String imageRate = radioButton.getText().toString();

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(SAMPLE_RATE_KEY, imageRate);
            editor.apply();
        });

        RadioGroup shutterUptimeGroup = findViewById(R.id.shutterUptimeOptions);
        shutterUptimeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton radioButton = findViewById(checkedId);
            String shutterUptime = radioButton.getText().toString();

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(SHUTTER_UPTIME_KEY, shutterUptime);
            editor.apply();
        });
    }

    /**
     * Sets up spinners for gesture mappings.
     */
    private void setupGestureMappings() {
        up_spinner = findViewById(R.id.up_spinner);
        down_spinner = findViewById(R.id.down_spinner);
        skip_spinner = findViewById(R.id.skip_spinner);
        previous_spinner = findViewById(R.id.previous_spinner);
        like_spinner = findViewById(R.id.like_spinner);
        pause_spinner = findViewById(R.id.pause_spinner);
        play_spinner = findViewById(R.id.play_spinner);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Restore settings and mappings
        restoreSettings();
    }

    /**
     * Restores settings and gesture mappings when the activity resumes.
     */
    private void restoreSettings() {
        // Restore image rate setting
        String imageRate = sharedPreferences.getString(SAMPLE_RATE_KEY, "");
        restoreRadioGroupSelection(R.id.imageRateOptions, imageRate);

        // Restore shutter uptime setting
        String shutterUptime = sharedPreferences.getString(SHUTTER_UPTIME_KEY, "");
        restoreRadioGroupSelection(R.id.shutterUptimeOptions, shutterUptime);

        // Restore gesture mappings
        restoreGestureMappings();
    }

    /**
     * Restores the selection of a radio button group based on the provided setting.
     *
     * @param radioGroupId The ID of the radio button group.
     * @param setting      The setting to be restored.
     */
    private void restoreRadioGroupSelection(int radioGroupId, String setting) {
        if (!setting.isEmpty()) {
            RadioGroup radioGroup = findViewById(radioGroupId);
            for (int i = 0; i < radioGroup.getChildCount(); i++) {
                RadioButton radioButton = (RadioButton) radioGroup.getChildAt(i);
                if (radioButton.getText().toString().equals(setting)) {
                    radioButton.setChecked(true);
                    break;
                }
            }
        }
    }

    /**
     * Restores gesture mappings from SharedPreferences.
     */
    private void restoreGestureMappings() {
        // Retrieve and set mappings for each gesture
        setGestureMapping(UP_GESTURE_KEY, up_spinner);
        setGestureMapping(DOWN_GESTURE_KEY, down_spinner);
        setGestureMapping(SKIP_GESTURE_KEY, skip_spinner);
        setGestureMapping(PREVIOUS_GESTURE_KEY, previous_spinner);
        setGestureMapping(PLAY_GESTURE_KEY, play_spinner);
        setGestureMapping(PAUSE_GESTURE_KEY, pause_spinner);
        setGestureMapping(LIKE_GESTURE_KEY, like_spinner);
    }

    /**
     * Sets a gesture mapping in the corresponding spinner.
     *
     * @param key     The key for the gesture mapping in SharedPreferences.
     * @param spinner The spinner to set the mapping in.
     */
    private void setGestureMapping(String key, Spinner spinner) {
        int storedValue = sharedPreferences.getInt(key, 0);
        spinner.setSelection(storedValue);
    }

    /**
     * Handles the click event for the back button.
     * Validates mappings before finishing the activity.
     *
     * @param v The clicked view.
     */
    public void settingsBackClicked(View v) {
        if (areMappingsValid()) {
            storeMappings();
            this.finish();
        } else {
            Toast.makeText(getApplicationContext(), "Make sure every function has a unique gesture", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Checks whether gesture mappings are valid.
     *
     * @return True if mappings are valid, false otherwise.
     */
    private boolean areMappingsValid() {
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

    /**
     * Stores gesture mappings in SharedPreferences.
     */
    private void storeMappings() {
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
