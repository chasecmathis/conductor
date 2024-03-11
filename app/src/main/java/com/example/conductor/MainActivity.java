package com.example.conductor;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private AudioManager audioManager;
    private MusicController musicController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        this.musicController = new MusicController(this.audioManager);
    }

    public void playButtonClicked(View v) {

        Toast.makeText(this, "Play button clicked", Toast.LENGTH_SHORT).show();
    }

    public void volumeUpClicked(View v) {
        Toast.makeText(this, "Volume up button clicked", Toast.LENGTH_SHORT).show();
        this.musicController.lowerVolume();
    }
}
