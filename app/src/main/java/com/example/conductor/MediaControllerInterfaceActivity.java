package com.example.conductor;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.Fragment;
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

    MediaController mediaController;

    SpotifyHelper spotify;

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

    private final Handler mainThread = new Handler(Looper.getMainLooper());

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

        //Camera initialization
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        //Handle fragment swapping to turn camera on and off
        camFrag = new CameraFragment(cameraManager);

        seekBar = findViewById(R.id.seekbar);
        title_text = findViewById(R.id.shutter_title);
        artist_text = findViewById(R.id.shutter_artist);
        album_text = findViewById(R.id.shutter_album);
    }

    @Override
    protected void onStart() {
        super.onStart();
        spotify.initializeSpotifyAppRemote();
    }

    protected void onResume() {
        super.onResume();

        if (!mediaSessionManager.getActiveSessions(new ComponentName(MediaControllerInterfaceActivity.this, getClass())).isEmpty()) {
            List<MediaController> controllers = mediaSessionManager.getActiveSessions(new ComponentName(MediaControllerInterfaceActivity.this, getClass()));
            // Check if there are active media controllers
            if (!controllers.isEmpty())
                this.mediaController = controllers.get(0);
        }

        shutterFrag = new ShutterFragment(mediaController);

        int color = Color.parseColor("#664C33");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(color);

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this.proximityListener, sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setSettings();

        startShutterThread();

        title_text = findViewById(R.id.shutter_title);
        artist_text = findViewById(R.id.shutter_artist);
        album_text = findViewById(R.id.shutter_album);

        //Start all physical playback control buttons
        initButtons();

        registerMediaControllerCallback();
        seekBarManager = new SeekBarManager(mediaController, seekBar, this);
        seekBar.setOnSeekBarChangeListener(seekBarManager);
        startSeekUpdates();
        updateMetadata();
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
        if (mediaController != null) {
            MediaMetadata metadata = mediaController.getMetadata();
            if (metadata != null) {
                shutterFrag.updateAlbumArt();

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

                PlaybackState state = mediaController.getPlaybackState();
                ImageButton playPauseButton = findViewById(R.id.button_play_pause);
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING)
                    playPauseButton.setImageResource(R.drawable.ic_pause);
                else playPauseButton.setImageResource(R.drawable.ic_play);

                ImageButton favoriteButton = findViewById(R.id.button_heart);

                spotify.isLiked().thenAccept(liked -> {
                    if (liked) favoriteButton.setImageResource(R.drawable.ic_favorite);
                    else favoriteButton.setImageResource(R.drawable.ic_favorite_border);
                });
            }
        }
    }

    // This method will be called when the button is clicked
    private void pauseButtonClick() {
        if (mediaController != null)
            mediaController.getTransportControls().pause();
    }

    private void playButtonClick() {
        if (mediaController != null)
            mediaController.getTransportControls().play();
    }

    private void skipButtonClick() {
        if (mediaController != null)
            mediaController.getTransportControls().skipToNext();
    }

    private void previousButtonClick() {
        if (mediaController != null)
            mediaController.getTransportControls().skipToPrevious();
    }

    private void likeButtonClick() {
        spotify.isLiked().thenAccept(liked -> {
            boolean spotifyPlaying = mediaController.getPackageName().equals("com.spotify.music");
            if (liked) spotify.unlikeSpotifySong(spotifyPlaying);
            else spotify.likeSpotifySong(spotifyPlaying);
        });
    }

    private final BroadcastReceiver proximityAlertReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            FragmentManager fragmentManager = getSupportFragmentManager();

            if (!fragmentManager.isDestroyed()) {
                fragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, camFrag)
                        .commit();

                shutterHandler.postDelayed(hideCamera, cameraActiveInterval_MS);
            }
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
            if(label.equals(LIKE_SONG)) {
                likeButtonClick();
                restartShutter();
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
        if (shutterHandler != null) {
            shutterHandler.removeCallbacks(hideCamera);
            shutterHandler.postDelayed(hideCamera, cameraActiveInterval_MS);
        }
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
            FragmentManager fragmentManager = getSupportFragmentManager();

            if (!fragmentManager.isDestroyed()) {
                fragmentManager.beginTransaction().replace(R.id.fragment_container, shutterFrag).commit();
            }
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
            if (mediaController != null) {
                PlaybackState state = mediaController.getPlaybackState();
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
        favoriteButton.setOnClickListener(v -> likeButtonClick());
    }

    public void tutorialClicked(View v) {
        Intent turorialIntent = new Intent(this, TutorialActivity.class);
        turorialIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(turorialIntent);
    }

    private void registerMediaControllerCallback() {
        if (mediaController != null)
            mediaController.registerCallback(mediaControllerCallback);
    }

    private void unregisterMediaControllerCallback() {
        if (mediaController != null)
            mediaController.unregisterCallback(mediaControllerCallback);
    }

    private final MediaController.Callback mediaControllerCallback = new MediaController.Callback() {
        @Override
        public void onMetadataChanged(@Nullable MediaMetadata metadata) {
            super.onMetadataChanged(metadata);
            updateMetadata();
        }

        @Override
        public void onPlaybackStateChanged(@Nullable PlaybackState state) {
            super.onPlaybackStateChanged(state);

            if (state != null && (state.getState() == PlaybackState.STATE_PLAYING ||
                state.getState() == PlaybackState.STATE_PAUSED)) {
                updateMetadata();
            }
        }
    };
}
