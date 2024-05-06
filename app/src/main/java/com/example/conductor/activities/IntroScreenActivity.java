package com.example.conductor.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;

import com.example.conductor.listeners.MediaNotificationListener;
import com.example.conductor.R;

/**
 * This activity represents the introduction screen of the application.
 * It handles permission requests and navigates to the main screen.
 */
public class IntroScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_screen);

        // Set status bar color
        int color = Color.parseColor("#664C33");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(color);

        // Request necessary permissions
        requestNotificationListenerPermission();
    }

    /**
     * Method triggered when the start button is clicked.
     * Navigates to the main screen.
     */
    public void startButtonClicked(View v) {
        Intent toMediaControllerIntent = new Intent(this, MediaControllerInterfaceActivity.class);
        startActivity(toMediaControllerIntent);
    }

    /**
     * Requests notification listener permission if needed.
     * Shows an alert dialog if the permission is not granted.
     * Starts the notification listener service otherwise.
     */
    private void requestNotificationListenerPermission() {
        if (!isNotificationListenerEnabled()) {
            // Show alert dialog to guide user to enable notification access
            showAlertDialog();
        } else {
            // Start the notification listener service
            Intent mServiceIntent = new Intent(this, MediaNotificationListener.class);
            startService(mServiceIntent);
        }
    }

    /**
     * Displays an alert dialog informing the user about the need for notification permission.
     * Provides an option to navigate to the settings to grant the permission.
     */
    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Must allow notification permissions for this app")
                .setMessage("Please grant notification permissions within the settings app")
                .setPositiveButton("Go to settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Dismiss the dialog
                        dialog.dismiss();
                        // Open settings to enable notification access
                        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                        startActivity(intent);
                    }
                })
                .show();
    }

    /**
     * Checks if the notification listener service is enabled.
     *
     * @return true if the notification listener service is enabled, false otherwise.
     */
    protected boolean isNotificationListenerEnabled() {
        // Construct the listener component name
        String listener = getPackageName() + "/" + MediaNotificationListener.class.getName();
        // Get enabled notification listeners from system settings
        String enabledListeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        // Check if our listener is enabled
        return enabledListeners != null && enabledListeners.contains(listener);
    }
}
