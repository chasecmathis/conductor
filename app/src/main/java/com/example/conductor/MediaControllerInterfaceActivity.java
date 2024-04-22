package com.example.conductor;

import androidx.annotation.Nullable;
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
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.conductor.fragments.CameraFragment;
import com.example.conductor.fragments.ShutterFragment;

import java.util.List;

/**
 * The main activity for Conductor
 * Initializes key modules and handles interaction with media control
 */
public class MediaControllerInterfaceActivity extends AppCompatActivity {
    MediaSessionManager mediaSessionManager;

    SpotifyHelper spotify;

    private boolean spotifyAuth;

    private final int CAMERA_PERMISSION_REQUEST_CODE = 7;

    private final String LIKE_SONG = "Thumb_Up";

    private final String VOLUME_UP = "Pointing_Up";

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

    private final Handler mainThread = new Handler(Looper.getMainLooper());

    private Handler shutterHandler;
    private HandlerThread shutterThread;

    private int cameraActiveInterval_MS = 5000;

    private static final String PREFS_NAME = "settings";
    private static final String SAMPLE_RATE_KEY = "sample_rate_key";
    private static final String SHUTTER_UPTIME_KEY = "shutter_uptime_key";
    private SharedPreferences sharedPreferences;

    private final int SEEK_MS = 500;

    private TextView title_text;
    private TextView artist_text;
    private TextView album_text;

    private SeekBarManager seekBarManager;

    private SeekBar seekBar;

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

        seekBar = findViewById(R.id.seekbar);
        seekBarManager = new SeekBarManager(mediaSessionManager, seekBar, this);
    }

    public void onPauseButtonClick(View view) {
        pauseButtonClick();
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

        Log.d("On resume", "");

        int color = Color.parseColor("#664C33");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(color);

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this.proximityListener, sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);
        startShutterThread();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setSettings();

        title_text = findViewById(R.id.shutter_title);
        artist_text = findViewById(R.id.shutter_artist);
        album_text = findViewById(R.id.shutter_album);

        registerMediaControllerCallback();
        startSeekUpdates();
    }

    protected void onPause() {
        super.onPause();
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.unregisterListener(this.proximityListener);
        stopShutterThread();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        unregisterMediaControllerCallback();
        stopSeekUpdates();
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

        this.cameraActiveInterval_MS = Integer.parseInt(shutterUptime) * 1000;
        this.camFrag.setSampleRate((int) (Float.parseFloat(sampleRate) * 1000));
    }

    // Method to start updating metadata constantly
    private void startSeekUpdates() {
        mainThread.post(updateSeekRunnable);
    }

    // Method to stop updating metadata
    private void stopSeekUpdates() {
        mainThread.removeCallbacks(updateSeekRunnable);
    }

    // Runnable to update metadata constantly
    private final Runnable updateSeekRunnable = new Runnable() {
        @Override
        public void run() {
            seekBarManager.updateSeekBarProgress();
            mainThread.postDelayed(this, SEEK_MS);
        }
    };

    // Method to update metadata
    private void updateMetadata() {
        // Update title, artist, and album views
        if (!mediaSessionManager.getActiveSessions(new ComponentName(MediaControllerInterfaceActivity.this, getClass())).isEmpty()) {
            MediaController controller = mediaSessionManager.getActiveSessions(new ComponentName(MediaControllerInterfaceActivity.this, getClass())).get(0);
            MediaMetadata metadata = controller.getMetadata();
            if (metadata != null) {
                if (metadata.getText(MediaMetadata.METADATA_KEY_TITLE) != null) {
                    String title = metadata.getText(MediaMetadata.METADATA_KEY_TITLE).toString();
                    title_text.setText(title);
                }
                if (metadata.getText(MediaMetadata.METADATA_KEY_ARTIST) != null) {
                    String artist = metadata.getText(MediaMetadata.METADATA_KEY_ARTIST).toString();
                    artist_text.setText(artist);
                }
                if (metadata.getText(MediaMetadata.METADATA_KEY_ALBUM) != null) {
                    String album = metadata.getText(MediaMetadata.METADATA_KEY_ALBUM).toString();
                    album_text.setText(album);
                }

                PlaybackState state = controller.getPlaybackState();
                ImageButton playPauseButton = findViewById(R.id.button_play_pause);
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING)
                    playPauseButton.setImageResource(R.drawable.ic_pause);
                else playPauseButton.setImageResource(R.drawable.ic_play);

                ImageButton favoriteButton = findViewById(R.id.button_heart);
                if (spotify.isLiked().getNow(false)) favoriteButton.setImageResource(R.drawable.ic_favorite);
                else favoriteButton.setImageResource(R.drawable.ic_favorite_border);
            }
        }
    }

    // This method will be called when the button is clicked
    private void pauseButtonClick() {
        if (!mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).isEmpty()) {
            MediaController controller = mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).get(0);
            controller.getTransportControls().pause();
        }
    }

    private void playButtonClick() {
        if (!mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).isEmpty()) {
            MediaController controller = mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).get(0);
            controller.getTransportControls().play();
        }
    }

    private void skipButtonClick() {
        if (!mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).isEmpty()) {
            MediaController controller = mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).get(0);
            controller.getTransportControls().skipToNext();
        }
    }

    private void previousButtonClick() {
        if (!mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).isEmpty()) {
            MediaController controller = mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).get(0);
            controller.getTransportControls().skipToPrevious();
        }
    }


    private final BroadcastReceiver proximityAlertReceiver = new BroadcastReceiver() {
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
    private final BroadcastReceiver MLReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String label = intent.getStringExtra("LABEL");

            if (label == null) label = "None";

            Log.d("LABEL", label);

            // Determine which action to take based off of label
            switch (label) {
                case LIKE_SONG:
                    if (spotifyAuth) {
                        spotify.likeSpotifySong();
                        restartShutter();
                    }
                    break;
                case VOLUME_UP:
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

    public void settingsClicked(View v) {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        settingsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(settingsIntent);
    }

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
        ImageButton playPauseButton = findViewById(R.id.button_play_pause);
        playPauseButton.setOnClickListener(v -> {
            // Toggle play/pause behavior
            if (!mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).isEmpty()) {
                MediaController controller = mediaSessionManager.getActiveSessions(new ComponentName(this, getClass())).get(0);
                PlaybackState state = controller.getPlaybackState();
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                    pauseButtonClick();
                    playPauseButton.setImageResource(R.drawable.ic_play);
                } else if (state != null && state.getState() == PlaybackState.STATE_PAUSED) {
                    playButtonClick();
                    playPauseButton.setImageResource(R.drawable.ic_pause);
                }
            }
        });

        ImageButton skipButton = findViewById(R.id.button_skip);
        skipButton.setOnClickListener(v -> {
            // Toggle play/pause behavior
            skipButtonClick();
        });

        ImageButton previousButton = findViewById(R.id.button_previous);
        previousButton.setOnClickListener(v -> {
            // Toggle play/pause behavior
            previousButtonClick();
        });

        ImageButton favoriteButton = findViewById(R.id.button_heart);
        favoriteButton.setOnClickListener(v -> {
            spotify.isLiked().thenAccept(liked -> {
                if (liked) spotify.unlikeSpotifySong();
                else spotify.likeSpotifySong();
            });
        });
    }

    private void registerMediaControllerCallback() {
        if (!mediaSessionManager.getActiveSessions(new ComponentName(MediaControllerInterfaceActivity.this, getClass())).isEmpty()) {
            List<MediaController> controllers = mediaSessionManager.getActiveSessions(new ComponentName(MediaControllerInterfaceActivity.this, getClass()));
            // Check if there are active media controllers
            if (!controllers.isEmpty()) {
                MediaController mediaController = controllers.get(0);
                Log.d("Controller", "Controller is " + mediaController.getPackageName());
                mediaController.registerCallback(mediaControllerCallback);
            }
        }
    }

    private void unregisterMediaControllerCallback() {
        if (!mediaSessionManager.getActiveSessions(new ComponentName(MediaControllerInterfaceActivity.this, getClass())).isEmpty()) {
            List<MediaController> controllers = mediaSessionManager.getActiveSessions(new ComponentName(MediaControllerInterfaceActivity.this, getClass()));
            // Check if there are active media controllers
            if (!controllers.isEmpty()) {
                MediaController mediaController = controllers.get(0);
                mediaController.unregisterCallback(mediaControllerCallback);
            }
        }
    }

    private final MediaController.Callback mediaControllerCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            // Handle playback state changes here
            Log.d("Playback State", "Changed: " + state.toString());
            updateMetadata();
        }
    };
}
