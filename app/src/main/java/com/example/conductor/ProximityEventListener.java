package com.example.conductor;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class ProximityEventListener implements SensorEventListener {

    private static final float PROXIMITY_THRESHOLD = 5.0f;
    private static final double CYCLING_INTERVAL_MS = 1000;

    private long lastAlert = 0;

    private int downTime;
    private Context context;

    public ProximityEventListener(Context c, int downTime) {
        this.context = c;
        this.downTime = downTime;
    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            float distance = event.values[0];
            long timeElapsed = System.currentTimeMillis() - this.lastAlert;
            //Only trigger alert if it's been at least 10 seconds since the last one
            if (distance < PROXIMITY_THRESHOLD && timeElapsed > downTime) {
                this.lastAlert = System.currentTimeMillis();
                sendProximityAlertIntent();
            }
        }
    }

    protected void updateDowntime(int time_MS){
        downTime = time_MS;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void sendProximityAlertIntent() {
        Intent intent = new Intent("PROXIMITY_ALERT");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
//        context.sendBroadcast(intent);
        Log.d("Broadcasting", "Bro");
    }
}
