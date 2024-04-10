package com.example.conductor;

import android.content.Context;
import android.media.AudioManager;

public class MusicController {

    private final AudioManager audioManager;
    public MusicController(AudioManager manager) {
        this.audioManager = manager;
    }

    public void lowerVolume() {
        this.audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
    }

    public void raiseVolume() {
        this.audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
    }


}
