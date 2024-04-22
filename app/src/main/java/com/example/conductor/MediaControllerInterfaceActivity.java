package com.example.conductor;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.example.conductor.fragments.CameraFragment;
import com.example.conductor.fragments.ShutterFragment;

/**
 * The main activity for Conductor
 * Initializes key modules and handles interaction with media control
 */
public class MediaControllerInterfaceActivity extends AppCompatActivity {
    MediaSessionManager mediaSessionManager;

    SpotifyHelper spotify;

    private boolean spotifyAuth;

    private final int CAMERA_PERMISSION_REQUEST_CODE = 7;

    private String LIKE_SONG = "Thumb_Up";

    private String VOLUME_UP = "Pointing_Up";

    private String VOLUME_DOWN = "Thumb_Down";

    private String PAUSE = "Open_Palm";

    private String PLAY = "Closed_Fist";

    private String SKIP = "Victory";

    private String PREVIOUS = "ILoveYou";

    private final String[] GESTURE_MAP = {"Thumb_Up", "Pointing_Up", "Thumb_Down", "Closed_Fist", "Open_Palm", "Victory", "ILoveYou"};


    private AudioManager audioManager;
    private MusicController musicController;
    private ProximityEventListener proximityListener;

    private CameraFragment camFrag;
    private ShutterFragment shutterFrag;

    private Handler mainThread = new Handler(Looper.getMainLooper());

    private Handler shutterHandler;
    private HandlerThread shutterThread;

    private int cameraActiveInterval_MS = 5000;

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

    private SharedPreferences sharedPreferences;

    /**
     * Main initializer for starting Conductor
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.media_controller_interface);

        //Set up sensors and audio control
        this.audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        this.musicController = new MusicController(this.audioManager);
        this.proximityListener = new ProximityEventListener(this, cameraActiveInterval_MS);


        //Prepare broadcast receivers for ML and proximity messages
        IntentFilter filter = new IntentFilter("PROXIMITY_ALERT");
        LocalBroadcastManager.getInstance(this).registerReceiver(proximityAlertReceiver, filter);

        IntentFilter MLfilter = new IntentFilter("LABEL");
        LocalBroadcastManager.getInstance(this).registerReceiver(MLReceiver, MLfilter);

        // Start media session manager for controlling music
        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);

        // Start spotify helper
        this.spotify = new SpotifyHelper(this);
        this.spotifyAuth = getIntent().getBooleanExtra("SpotifyAuth", false);

        //Start all physical playback control buttons
        initButtons();

        //Camera initialization
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        //Handle fragment swapping to turn camera on and off
        camFrag = new CameraFragment(cameraManager);
        shutterFrag = new ShutterFragment(mediaSessionManager, this);
    }

    public void onPauseButtonClick(View view) {
        pauseButtonClick();
    }

    public void onPlayButtonClick(View view) {
        playButtonClick();
    }

    public void onPreviousButtonClick(View view) {
        previousButtonClick();
    }

    public void onSkipButtonClick(View view) {
        skipButtonClick();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (spotifyAuth) spotify.initializeSpotifyAppRemote();
    }

    protected void onResume() {
        super.onResume();
        int color = Color.parseColor("#664C33");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(color);

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this.proximityListener, sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setSettings();
        startShutterThread();
    }

    protected void onPause() {
        super.onPause();
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.unregisterListener(this.proximityListener);
        stopShutterThread();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onStop() {
        super.onStop();
        spotify.disconnectSpotifyAppRemote();
    }

    private void setSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String shutterUptime = sharedPreferences.getString(SHUTTER_UPTIME_KEY, "10");
        String sampleRate = sharedPreferences.getString(SAMPLE_RATE_KEY, "2");


        this.cameraActiveInterval_MS = Integer.valueOf(shutterUptime) * 1000;
        this.camFrag.setSampleRate((int) (Float.valueOf(sampleRate) * 1000));
        this.proximityListener.updateDowntime(cameraActiveInterval_MS);


        //Retrieve custom mappings
        int storedValue = sharedPreferences.getInt(UP_GESTURE_KEY, 1);
        VOLUME_UP = GESTURE_MAP[storedValue];
        storedValue = sharedPreferences.getInt(DOWN_GESTURE_KEY, 2);
        VOLUME_DOWN = GESTURE_MAP[storedValue];
        storedValue = sharedPreferences.getInt(SKIP_GESTURE_KEY, 5);
        SKIP = GESTURE_MAP[storedValue];
        storedValue = sharedPreferences.getInt(PREVIOUS_GESTURE_KEY, 6);
        PREVIOUS = GESTURE_MAP[storedValue];
        storedValue = sharedPreferences.getInt(PLAY_GESTURE_KEY, 3);
        PLAY = GESTURE_MAP[storedValue];
        storedValue = sharedPreferences.getInt(PAUSE_GESTURE_KEY, 4);
        PAUSE = GESTURE_MAP[storedValue];
        storedValue = sharedPreferences.getInt(LIKE_GESTURE_KEY, 0);
        LIKE_SONG = GESTURE_MAP[storedValue];
    }

    // This method will be called when the button is clicked
    private void pauseButtonClick() {
        if (mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).size() > 0) {
            MediaController controller = mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).get(0);
            controller.getTransportControls().pause();
        }
    }

    private void playButtonClick() {
        if (mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).size() > 0) {
            MediaController controller = mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).get(0);
            controller.getTransportControls().play();
        }
    }

    private void skipButtonClick() {
        if (mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).size() > 0) {
            MediaController controller = mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).get(0);
            controller.getTransportControls().skipToNext();
        }
    }

    private void previousButtonClick() {
        if (mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).size() > 0) {
            MediaController controller = mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).get(0);
            controller.getTransportControls().skipToPrevious();
        }
    }


    private BroadcastReceiver proximityAlertReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, camFrag)
                    .commit();

            shutterHandler.postDelayed(hideCamera, cameraActiveInterval_MS);
        }
    };

    /**
     * Recieves broadcast with most recent classification and handles result
     */
    private BroadcastReceiver MLReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String label = intent.getStringExtra("LABEL");

            if (label == null) label = "None";

            Log.d("LABEL", label);

            // Determine which action to take based off of label
            if(label.equals(LIKE_SONG)) {
                if (spotifyAuth) {
                    spotify.likeSpotifySong();
                    restartShutter();
                }
            }
            else if(label.equals(VOLUME_UP)) {
                volumeUp();
                restartShutter();
            }
            else if(label.equals(PAUSE)) {
                pauseButtonClick();
                restartShutter();
            }
            else if(label.equals(PLAY)) {
                playButtonClick();
                restartShutter();
            }
            else if(label.equals(VOLUME_DOWN)) {
                volumeDown();
                restartShutter();
            }
            else if(label.equals(SKIP)) {
                skipButtonClick();
                restartShutter();
            }
            else if(label.equals(PREVIOUS)) {
                previousButtonClick();
                restartShutter();
            }
        }
    };

    private void restartShutter() {
        shutterHandler.removeCallbacks(hideCamera);
        shutterHandler.postDelayed(hideCamera, cameraActiveInterval_MS);
    }

    private void volumeUp() {
        this.musicController.raiseVolume();
    }

    private void volumeDown() {
        this.musicController.lowerVolume();
    }

    public void volumeUpClicked(View v) {
        this.musicController.raiseVolume();
    }

    public void volumeDownClicked(View v) {
        this.musicController.lowerVolume();
    }

    public void settingsClicked(View v) {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        settingsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(settingsIntent);
    }

    /*public void settingsBackClicked(View v) {
        setContentView(R.layout.media_controller_interface);
    }*/


    /**
     * Runnable for shuttering camera
     */
    private final Runnable hideCamera = new Runnable() {
        @Override
        public void run() {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, shutterFrag).commit();
        }
    };

    private void startShutterThread() {
        shutterThread = new HandlerThread("shutter");
        shutterThread.start();
        shutterHandler = new Handler(shutterThread.getLooper());
        shutterHandler.post(hideCamera);
    }

    private void stopShutterThread() {
        if (shutterThread != null) {
            shutterThread.quitSafely();
            try {
                shutterThread.join();
                shutterThread = null;
                shutterHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Add connections for all physical buttons
     */
    private void initButtons() {
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
}
