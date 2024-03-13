package com.example.conductor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;

import com.example.conductor.fragments.CameraFragment;

public class MainActivity extends AppCompatActivity {

    private final int CAMERA_PERMISSION_REQUEST_CODE = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Camera initialization
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);

        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        CameraFragment camFrag = new CameraFragment(cameraManager);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, camFrag)
                .commit();
    }
}