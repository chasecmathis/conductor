package com.example.conductor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.core.view.WindowCompat;


public class TutorialActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "settings";
    private static final String UP_GESTURE_KEY = "up_gesture_key";
    private static final String DOWN_GESTURE_KEY = "down_gesture_key";
    private static final String PLAY_GESTURE_KEY = "play_gesture_key";
    private static final String PAUSE_GESTURE_KEY = "pause_gesture_key";
    private static final String SKIP_GESTURE_KEY = "skip_gesture_key";
    private static final String PREVIOUS_GESTURE_KEY = "previous_gesture_key";
    private static final String LIKE_GESTURE_KEY = "like_gesture_key";

    private final String[] GESTURE_MAP = {"Thumb_Up", "Pointing_Up", "Thumb_Down", "Closed_Fist", "Open_Palm", "Victory", "ILoveYou"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.tutorial_page);

        TextView fist_text = findViewById(R.id.fist_text);
        TextView open_hand_text = findViewById(R.id.open_hand_text);
        TextView thumbs_up_text = findViewById(R.id.thumbs_up_text);
        TextView thumbs_down_text = findViewById(R.id.thumbs_down_text);
        TextView peace_sign_text = findViewById(R.id.peace_sign_text);
        TextView rock_on_text = findViewById(R.id.rock_on_text);
        TextView like_text = findViewById(R.id.like_text);

        TextView [] mapping = {thumbs_up_text, like_text, thumbs_down_text, fist_text, open_hand_text, peace_sign_text, rock_on_text};

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        int up_val = sharedPreferences.getInt(UP_GESTURE_KEY, 1);
        mapping[up_val].setText("Volume Up");
        int down_val = sharedPreferences.getInt(DOWN_GESTURE_KEY, 2);
        mapping[down_val].setText("Volume Down");
        int skip_val = sharedPreferences.getInt(SKIP_GESTURE_KEY, 5);
        mapping[skip_val].setText("Skip");
        int prev_val = sharedPreferences.getInt(PREVIOUS_GESTURE_KEY, 6);
        mapping[prev_val].setText("Previous");
        int play_val = sharedPreferences.getInt(PLAY_GESTURE_KEY, 3);
        mapping[play_val].setText("Play");
        int pause_val = sharedPreferences.getInt(PAUSE_GESTURE_KEY, 4);
        mapping[pause_val].setText("Pause");
        int like_val = sharedPreferences.getInt(LIKE_GESTURE_KEY, 0);
        mapping[like_val].setText("Like");
    }

    public void tutorialBackClicked(View v) {
        this.finish();
    }
}
