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

    private final int CAMERA_PERMISSION_REQUEST_CODE = 7;

//    private final String NONE = "None";

    private final String VOLUME_UP_1 = "Thumb_Up";

    private final String VOLUME_UP_2 = "Pointing_Up";

    private final String VOLUME_DOWN = "Thumb_Down";

    private final String PAUSE = "Open_Palm";

    private final String PLAY = "Closed_Fist";

    private final String SKIP = "Victory";

    private final String PREVIOUS = "ILoveYou";

    private AudioManager audioManager;
    private MusicController musicController;
    private ProximityEventListener proximityListener;

    private CameraFragment camFrag;
    private ShutterFragment shutterFrag;

    private Handler mainThread = new Handler(Looper.getMainLooper());

    private Handler shutterHandler;
    private HandlerThread shutterThread;


    private int cameraActiveInterval_MS = 5000;

    /**
     * Main initializer for starting Conductor
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
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
        LocalBroadcastManager.getInstance(this).registerReceiver(proximityAlertReceiver,
                filter);

        IntentFilter MLfilter = new IntentFilter("LABEL");
        LocalBroadcastManager.getInstance(this).registerReceiver(MLReceiver,
                MLfilter);

        // Start media session manager for controlling music
        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);

        //Start all physical playback control buttons
        initButtons();

        //Camera initialization
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        //Handle fragment swapping to turn camera on and off
        camFrag = new CameraFragment(cameraManager);
        shutterFrag = new ShutterFragment(mediaSessionManager, this);
        startShutterThread();

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

        int color = Color.parseColor("#664C33");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(color);

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this.proximityListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                SensorManager.SENSOR_DELAY_NORMAL);
        startShutterThread();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    protected void onPause() {
        super.onPause();
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.unregisterListener(this.proximityListener);
        stopShutterThread();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
            Log.d("DEBUG", "xCyx: detected object in close proximity");
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
            switch (label) {
                case VOLUME_UP_1:
                case VOLUME_UP_2:
                    volumeUp();
                    restartShutter();
                    break;
                case PAUSE:
                    pauseButtonClick();
                    restartShutter();
                    break;
                case PLAY:
                    playButtonClick();
                    restartShutter();
                    break;
                case VOLUME_DOWN:
                    volumeDown();
                    restartShutter();
                    break;
                case SKIP:
                    skipButtonClick();
                    restartShutter();
                    break;
                case PREVIOUS:
                    previousButtonClick();
                    restartShutter();
                    break;
                default:
                    break;
            }
        }
    };

    private void restartShutter(){
        shutterHandler.removeCallbacks(hideCamera);
        shutterHandler.postDelayed(hideCamera, cameraActiveInterval_MS);
    }

    private void volumeUp(){
        this.musicController.raiseVolume();
    }

    private void volumeDown(){
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
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, shutterFrag)
                    .commit();
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
    private void initButtons(){
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