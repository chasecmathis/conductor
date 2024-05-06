package com.example.conductor.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.conductor.R;

public class TutorialActivity extends AppCompatActivity {

    // Keys for SharedPreferences
    private static final String PREFS_NAME = "settings";
    private static final String UP_GESTURE_KEY = "up_gesture_key";
    private static final String DOWN_GESTURE_KEY = "down_gesture_key";
    private static final String PLAY_GESTURE_KEY = "play_gesture_key";
    private static final String PAUSE_GESTURE_KEY = "pause_gesture_key";
    private static final String SKIP_GESTURE_KEY = "skip_gesture_key";
    private static final String PREVIOUS_GESTURE_KEY = "previous_gesture_key";
    private static final String LIKE_GESTURE_KEY = "like_gesture_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorial_page);

        // Initialize TextViews
        TextView fist_text = findViewById(R.id.fist_text);
        TextView open_hand_text = findViewById(R.id.open_hand_text);
        TextView thumbs_up_text = findViewById(R.id.thumbs_up_text);
        TextView thumbs_down_text = findViewById(R.id.thumbs_down_text);
        TextView peace_sign_text = findViewById(R.id.peace_sign_text);
        TextView rock_on_text = findViewById(R.id.rock_on_text);
        TextView like_text = findViewById(R.id.like_text);

        TextView[] mapping = {thumbs_up_text, like_text, thumbs_down_text, fist_text, open_hand_text, peace_sign_text, rock_on_text};

        // Retrieve SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Retrieve gesture mappings from SharedPreferences and update TextViews accordingly
        updateMappingTextView(sharedPreferences, UP_GESTURE_KEY, mapping, "Volume Up");
        updateMappingTextView(sharedPreferences, DOWN_GESTURE_KEY, mapping, "Volume Down");
        updateMappingTextView(sharedPreferences, SKIP_GESTURE_KEY, mapping, "Skip");
        updateMappingTextView(sharedPreferences, PREVIOUS_GESTURE_KEY, mapping, "Previous");
        updateMappingTextView(sharedPreferences, PLAY_GESTURE_KEY, mapping, "Play");
        updateMappingTextView(sharedPreferences, PAUSE_GESTURE_KEY, mapping, "Pause");
        updateMappingTextView(sharedPreferences, LIKE_GESTURE_KEY, mapping, "Like");
    }

    /**
     * Updates the TextView for a specific gesture mapping.
     *
     * @param sharedPreferences The SharedPreferences instance.
     * @param key               The key for the gesture mapping in SharedPreferences.
     * @param mapping           The array of TextViews representing gesture mappings.
     * @param label             The label to set for the corresponding gesture mapping.
     */
    private void updateMappingTextView(SharedPreferences sharedPreferences, String key, TextView[] mapping, String label) {
        int value = sharedPreferences.getInt(key, 0);
        mapping[value].setText(label);
    }

    /**
     * Handles the click event for the back button.
     * Finishes the activity.
     *
     * @param v The clicked view.
     */
    public void tutorialBackClicked(View v) {
        this.finish();
    }
}
