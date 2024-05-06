package com.example.conductor.util;

import android.media.AudioManager;

/**
 * Helper class for controlling music volume.
 */
public class MusicController {

    // AudioManager instance for controlling audio settings
    private final AudioManager audioManager;

    /**
     * Constructor for MusicController.
     *
     * @param manager The AudioManager instance.
     */
    public MusicController(AudioManager manager) {
        this.audioManager = manager;
    }

    /**
     * Lower the volume of the music stream.
     */
    public void lowerVolume() {
        // Adjust the volume of the music stream to lower it
        this.audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
    }

    /**
     * Raise the volume of the music stream.
     */
    public void raiseVolume() {
        // Adjust the volume of the music stream to raise it
        this.audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
    }
}
