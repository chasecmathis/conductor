package com.example.conductor;

import static com.spotify.sdk.android.auth.AccountsQueryParameters.CLIENT_ID;
import static com.spotify.sdk.android.auth.LoginActivity.REQUEST_CODE;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;

import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

public class IntroScreenActivity extends AppCompatActivity {

    private final String REDIRECT_URI = "conductor://callback";

    private final String CLIENT_ID = "7f2c54fd18184696b745e45d29052624";

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_screen);
        int color = Color.parseColor("#664C33");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(color);
    }

    public void startButtonClicked(View v) {
        requestNotificationListenerPermission();
        requestCameraPermission();

        Intent toMediaControllerIntent = new Intent(this, MediaControllerInterfaceActivity.class);
        startActivity(toMediaControllerIntent);
    }

    public void loginWithSpotify(View view) {
        AuthorizationRequest.Builder builder =
                new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "playlist-read", "playlist-read-private"});
        AuthorizationRequest request = builder.build();

        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    String accessToken = response.getAccessToken();

                    // Start MediaControllerInterfaceActivity on success
                    Intent toMediaControllerIntent = new Intent(this, MediaControllerInterfaceActivity.class);
                    toMediaControllerIntent.putExtra("SpotifyAuth", true);
                    startActivity(toMediaControllerIntent);
                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    String error = response.getError();

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Error")
                            .setMessage(error)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Dismiss the dialog
                                    dialog.dismiss();
                                }
                            })
                            .show();
                    break;

                // Most likely auth flow was canceled
                default:
                    break;
            }
        }
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
