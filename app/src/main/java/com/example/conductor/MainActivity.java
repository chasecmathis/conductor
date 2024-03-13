package com.example.conductor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private MediaSessionCompat mediaSession;

    MediaSessionManager mediaSessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestNotificationListenerPermission();

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);

        // Set up the button click listener
        Button pauseButton = findViewById(R.id.button_pause);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle play/pause behavior
                pauseButtonClick();
            }
        });

        Button playButton = findViewById(R.id.button_play);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle play/pause behavior
                playButtonClick();
            }
        });

        Button skipButton = findViewById(R.id.button_skip);
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle play/pause behavior
                skipButtonClick();
            }
        });

        Button previousButton = findViewById(R.id.button_previous);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle play/pause behavior
                previousButtonClick();
            }
        });
    }


    private void requestNotificationListenerPermission() {
        if (!isNotificationListenerEnabled()) {
            // Open settings to enable notification access
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivity(intent);
        }
    }

    private boolean isNotificationListenerEnabled() {
        // Check if the notification listener service is enabled
        String listener = getPackageName() + "/" + MediaNotificationListener.class.getName();
        String enabledListeners = Settings.Secure.getString(
                getContentResolver(), "enabled_notification_listeners");
        return enabledListeners != null && enabledListeners.contains(listener);
    }

    public void onPauseButtonClick(View view){
        pauseButtonClick();
    }

    public void onPlayButtonClick(View view){
        playButtonClick();
    }

    public void onPreviousButtonClick(View view){
        previousButtonClick();
    }

    public void onSkipButtonClick(View view){
        skipButtonClick();
    }

    @Override
    protected void onDestroy() {
//        // Unregister the receiver to avoid memory leaks
//        if (mediaNotificationListener != null) {
//            unbindService(mediaNotificationListener);
//        }

        super.onDestroy();
    }

    // This method will be called when the button is clicked
    private void pauseButtonClick() {
        for (MediaController controller : mediaSessionManager.getActiveSessions(new ComponentName(this, getClass()))) {
            controller.getTransportControls().pause();
        }
    }

    private void playButtonClick() {
        for (MediaController controller : mediaSessionManager.getActiveSessions(new ComponentName(this, getClass()))) {
            controller.getTransportControls().play();
        }
    }
    private void skipButtonClick() {
        for (MediaController controller : mediaSessionManager.getActiveSessions(new ComponentName(this, getClass()))) {
            controller.getTransportControls().skipToNext();
        }
    }
    private void previousButtonClick() {
        for (MediaController controller : mediaSessionManager.getActiveSessions(new ComponentName(this, getClass()))) {
            controller.getTransportControls().skipToPrevious();
        }
    }
}