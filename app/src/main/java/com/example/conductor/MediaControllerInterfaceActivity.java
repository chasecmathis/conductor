package com.example.conductor;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MediaControllerInterfaceActivity extends AppCompatActivity {
    MediaSessionManager mediaSessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_controller_interface);

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

            showAlertDialog();

        }
    }

    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Must allow notification permissions for this app")
                .setMessage("Please grant notification permissions within the settings app")
                .setPositiveButton("Go to settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Handle the "OK" button click
                        dialog.dismiss(); // Dismiss the dialog
                        // Open settings to enable notification access
                        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                        startActivity(intent);
                    }
                })
                .show();
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
        super.onDestroy();
    }

    protected void onResume() {
        super.onResume();
        requestNotificationListenerPermission();
    }
    // This method will be called when the button is clicked
    private void pauseButtonClick() {
        MediaController controller = mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).get(0);
        if (controller != null) {
            controller.getTransportControls().pause();
        }
    }

    private void playButtonClick() {
        MediaController controller = mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).get(0);
        if (controller != null) {
            controller.getTransportControls().play();
        }
    }
    private void skipButtonClick() {
        MediaController controller = mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).get(0);
        if (controller != null) {
            controller.getTransportControls().skipToNext();
        }
    }
    private void previousButtonClick() {
        MediaController controller = mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).get(0);
        if (controller != null) {
            controller.getTransportControls().skipToPrevious();
        }
    }
}