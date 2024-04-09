package com.example.conductor.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.example.conductor.GestureRecognition;
import com.example.conductor.R;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;


import java.util.ArrayList;
import java.util.concurrent.Executor;

/**
 * A simple fragment for controlling the camera
 */
public class CameraFragment extends Fragment {


    private final String MAPPING = "abcdefghijklmnopqrstuvwxyz012";

    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private final int SAMPLE_RATE_MS = 2000;
    private TextureView textureView;
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size previewSize;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private Handler samplerHandler;
    private HandlerThread samplerThread;

    private GestureRecognition gestureRecognition;

    Bitmap bits;

    Surface cameraSurface;


    private class HandlerExecutor implements Executor {

        private Handler handler;

        public HandlerExecutor(Handler bg) {
            handler = bg;
        }

        @Override
        public void execute(Runnable command) {
            handler.post(command);
        }
    }


    public CameraFragment(CameraManager cManager){
        this.cameraManager = cManager;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        //Access the view for displaying camera output
        textureView = view.findViewById(R.id.cameraView);
        bits = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bits);
        canvas.drawColor(Color.GREEN);

        // Intitialize gesture recognition
        this.gestureRecognition = new GestureRecognition(this.getActivity());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Set the listener for the view
        textureView.setSurfaceTextureListener(surfaceTextureListener);
    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            // No action needed
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
            // No action needed
        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            if(backgroundHandler != null){
                createCameraPreview();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            //Make camera null to prevent further use
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private void openCamera() {
        //Request permissions if not granted
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }
        try {
            String cameraId = getCameraId();
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            previewSize = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(SurfaceTexture.class)[0];
            cameraManager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreview() {
        SurfaceTexture texture = textureView.getSurfaceTexture();
        //TODO check that texture is non null
        texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        cameraSurface = new Surface(texture);
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(cameraSurface);
            OutputConfiguration outputConfig = new OutputConfiguration(cameraSurface);
            ArrayList<OutputConfiguration> outputList = new ArrayList<>();
            outputList.add(outputConfig);
            SessionConfiguration configs = new SessionConfiguration(SessionConfiguration.SESSION_REGULAR,
                    outputList,
                    new HandlerExecutor(backgroundHandler),
                    cameraCB);
            cameraDevice.createCaptureSession(configs);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    CameraCaptureSession.StateCallback cameraCB = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            if (cameraDevice == null) {
                return;
            }
            cameraCaptureSession = session;
            try {
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.d("Failure", "Configure Failed");
        }
    };

    private String getCameraId() {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        startSamplingThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        stopSamplingThread();
        if(cameraSurface != null){
            cameraSurface.release();
        }
        super.onPause();
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void startSamplingThread() {
        samplerThread = new HandlerThread("CameraSampling");
        samplerThread.start();
        samplerHandler = new Handler(samplerThread.getLooper());
        samplerHandler.post(sample);
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private void stopSamplingThread() {
        if (samplerThread != null) {
            samplerThread.quitSafely();
            try {
                samplerThread.join();
                samplerThread = null;
                samplerHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }



    private final Runnable sample = new Runnable() {
        @Override
        public void run() {
            if(textureView != null){
                bits = textureView.getBitmap();
                if(bits != null){
                    bits = Bitmap.createScaledBitmap(bits, 224, 224, true);
                    //TODO Send this grayscale small bitmap to model
                    MPImage image = new BitmapImageBuilder(bits).build();
                    int res = gestureRecognition.gestureInference(image);

                    if(res != -1){
                        sendMLAlertIntent(res);
                    }
                }
            }
            samplerHandler.postDelayed(sample, SAMPLE_RATE_MS);

        }
    };

    private void sendMLAlertIntent(int mlVal) {
        Intent intent = new Intent("ML_MESSAGE");
        intent.putExtra("VALUE", mlVal);
        Context parent = this.getContext();
        if(parent != null) {
            LocalBroadcastManager.getInstance(parent).sendBroadcast(intent);
        }
    }

}