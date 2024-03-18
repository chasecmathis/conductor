package com.example.conductor;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MediaControllerInterfaceActivity extends AppCompatActivity {
    MediaSessionManager mediaSessionManager;

    private AudioManager audioManager;
    private MusicController musicController;
    private ProximityEventListener proximityListener;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.media_controller_interface);

        this.audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        this.musicController = new MusicController(this.audioManager);
        this.proximityListener = new ProximityEventListener(this);

        IntentFilter filter = new IntentFilter("PROXIMITY_ALERT");
        registerReceiver(proximityAlertReceiver, filter, RECEIVER_NOT_EXPORTED);

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
        else {
            Intent mServiceIntent = new Intent(this, MediaNotificationListener.class);
            startService(mServiceIntent);
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

    protected void onResume() {
        super.onResume();
        requestNotificationListenerPermission();
    }

    protected void onPause() {
        super.onPause();
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.unregisterListener(this.proximityListener);
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


    private BroadcastReceiver proximityAlertReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("DEBUG", "xCyx: detected object in close proximity");
        }
    };



    public void volumeUpClicked(View v) {
        this.musicController.raiseVolume();
    }

    public void volumeDownClicked(View v) {
        this.musicController.lowerVolume();
    }

    public void settingsClicked(View v) {
        setContentView(R.layout.settings_page);
    }

    public void settingsBackClicked(View v) {
        setContentView(R.layout.media_controller_interface);
    }
}