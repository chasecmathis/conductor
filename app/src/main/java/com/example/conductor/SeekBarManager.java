package com.example.conductor;

import android.content.ComponentName;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

public class SeekBarManager implements SeekBar.OnSeekBarChangeListener {

    private MediaController mediaController;
    private SeekBar seekBar;
    private TextView songPositionTextView;
    git
    private MediaControllerInterfaceActivity activity;
    int progress;

    public SeekBarManager(MediaController mediaController, SeekBar seekBar, MediaControllerInterfaceActivity activity) {
        this.mediaController = mediaController;
        this.seekBar = seekBar;
        this.songPositionTextView = activity.findViewById(R.id.song_position);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // Update the current playback position based on seek bar progress
        Log.d("SongProgress", String.valueOf(progress));
        this.progress = progress;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mediaController.getTransportControls().pause();
        Log.d("xCyx SongProgress", "onStart");
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // Make music catch up to progress bar
        mediaController.getTransportControls().seekTo(progress);
        mediaController.getTransportControls().play();
        Log.d("xCyx SongProgress", "onStop");
    }

    public void updateSeekBarProgress() {
        if (mediaController != null) {
            MediaMetadata metadata = mediaController.getMetadata();
            if (metadata != null) {
                long totalTime = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
                seekBar.setMax((int) totalTime);

                long elapsedTime;
                if (mediaController.getPlaybackState() != null) {
                    elapsedTime = mediaController.getPlaybackState().getPosition();
                } else {
                    elapsedTime = 0;
                }

                // Update seek bar progress
                seekBar.setProgress((int) elapsedTime, true);

                // Update elapsed time and total time TextViews
                songPositionTextView.setText(formatTime(elapsedTime) + " / " + formatTime(totalTime));
            }
        }
    }

    private String formatTime(long timeInMillis) {
        long minutes = (timeInMillis / 1000) / 60;
        long seconds = (timeInMillis / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
