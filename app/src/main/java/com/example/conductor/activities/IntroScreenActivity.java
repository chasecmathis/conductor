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

public class IntroScreenActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_screen);
        int color = Color.parseColor("#664C33");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(color);

        requestNotificationListenerPermission();
        requestCameraPermission();
    }

    public void startButtonClicked(View v) {


        Intent toMediaControllerIntent = new Intent(this, MediaControllerInterfaceActivity.class);
        startActivity(toMediaControllerIntent);
    }


    private void requestCameraPermission() {
        ;
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

    protected boolean isNotificationListenerEnabled() {
        // Check if the notification listener service is enabled
        String listener = getPackageName() + "/" + MediaNotificationListener.class.getName();
        String enabledListeners = Settings.Secure.getString(
                getContentResolver(), "enabled_notification_listeners");
        return enabledListeners != null && enabledListeners.contains(listener);
    }
}
