package com.example.conductor.listeners;

import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.conductor.R;
import com.example.conductor.activities.MediaControllerInterfaceActivity;

/**
 * The main UI page of the app has a progress bar that shows how far along the song is from
 * beginning to end. Users can grab this progress bar and slide it forward or backward to fast
 * forward or skip backward in the song. This class facilitates that behavior.
 */
public class SeekBarManager implements SeekBar.OnSeekBarChangeListener {

    private MediaController mediaController;
    private SeekBar seekBar;
    private TextView songPositionTextView;
    private MediaControllerInterfaceActivity activity;
    int progress;

    public SeekBarManager(MediaController mediaController, SeekBar seekBar, MediaControllerInterfaceActivity activity) {
        this.mediaController = mediaController;
        this.seekBar = seekBar;
        this.songPositionTextView = activity.findViewById(R.id.song_position);
    }

    /** This function tracks where the user has moved the progress bar to
     *
     * @param seekBar The SeekBar whose progress has changed
     * @param progress The current progress level. This will be in the range 0 ... 100
     * @param fromUser True if the progress change was initiated by the user.
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // Update the current playback position based on seek bar progress
        Log.d("SongProgress", String.valueOf(progress));
        this.progress = progress;
    }

    /**
     * When the user slides on the progress bar, the music should stop until we know
     * where to play from again
     * @param seekBar The SeekBar in which the touch gesture began
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mediaController.getTransportControls().pause();
        Log.d("xCyx SongProgress", "onStart");
    }

    /**
     * When the user lets to progress bar go, we want to start playing music
     * from wherever they skipped to
     * @param seekBar The SeekBar in which the touch gesture began
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // Make music catch up to progress bar
        mediaController.getTransportControls().seekTo(progress);
        mediaController.getTransportControls().play();
        Log.d("xCyx SongProgress", "onStop");
    }

    /**
     * This method is used for displaying to the screen the time in seconds that the current
     * song has played as well as the total length of the song
     */
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

    /**
     * Helper function for formatting our times as min:seconds, which is standard for music apps
     * @param timeInMillis
     * @return
     */
    private String formatTime(long timeInMillis) {
        long minutes = (timeInMillis / 1000) / 60;
        long seconds = (timeInMillis / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
