package com.example.conductor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private AudioManager audioManager;
    private MusicController musicController;
    private ProximityEventListener proximityListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        this.musicController = new MusicController(this.audioManager);
        this.proximityListener = new ProximityEventListener(this);

        //listen for proximity events
        IntentFilter filter = new IntentFilter("PROXIMITY_ALERT");
        registerReceiver(proximityAlertReceiver, filter, RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this.proximityListener,
            sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
            SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    protected void onPause() {
        super.onPause();

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.unregisterListener(this.proximityListener);
    }

    private BroadcastReceiver proximityAlertReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "Object detected in close proximity!", Toast.LENGTH_SHORT).show();
            Log.d("DEBUG", "xCyx: detected object in close proximity");
        }
    };



    public void volumeUpClicked(View v) {
        Toast.makeText(this, "Volume up button clicked", Toast.LENGTH_SHORT).show();
        this.musicController.raiseVolume();
    }

    public void volumeDownClicked(View v) {
        Toast.makeText(this, "Volume down button clicked", Toast.LENGTH_SHORT).show();
        this.musicController.lowerVolume();
    }

    public void settingsClicked(View v) {
        setContentView(R.layout.settings_page);
    }

    public void settingsBackClicked(View v) {
        setContentView(R.layout.activity_main);
    }
}
