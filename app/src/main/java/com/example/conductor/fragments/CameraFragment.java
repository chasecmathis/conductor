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

import com.example.conductor.util.GestureRecognition;
import com.example.conductor.R;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;

import java.util.ArrayList;
import java.util.concurrent.Executor;

/**
 * A simple fragment for controlling the camera.
 */
public class CameraFragment extends Fragment {

    // Character mapping for gestures
    private final String MAPPING = "abcdefghijklmnopqrstuvwxyz012";

    // Request code for camera permission
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    // Sample rate in milliseconds for capturing frames
    private int SAMPLE_RATE_MS = 2000;

    // Texture view for displaying camera output
    private TextureView textureView;

    // Camera manager for accessing the camera
    private CameraManager cameraManager;

    // Camera device for capturing images
    private CameraDevice cameraDevice;

    // Camera capture session for processing captured images
    private CameraCaptureSession cameraCaptureSession;

    // Capture request builder for creating capture requests
    private CaptureRequest.Builder captureRequestBuilder;

    // Preview size of the camera output
    private Size previewSize;

    // Background handler for background tasks
    private Handler backgroundHandler;

    // Background thread for background tasks
    private HandlerThread backgroundThread;

    // Handler for sampling frames from camera
    private Handler samplerHandler;

    // Thread for sampling frames from camera
    private HandlerThread samplerThread;

    // Gesture recognition module
    private GestureRecognition gestureRecognition;

    // Bitmap for storing camera frame
    Bitmap bits;

    // Surface for camera preview
    Surface cameraSurface;

    // Executor for handling background tasks
    private class HandlerExecutor implements Executor {

        private Handler handler;

        public HandlerExecutor(Handler bg) {
            handler = bg;
        }

        /**
         * Executes the given command at some time in the future.
         *
         * @param command the runnable task
         */
        @Override
        public void execute(Runnable command) {
            handler.post(command);
        }
    }

    /**
     * Constructor for CameraFragment.
     *
     * @param cManager The camera manager instance.
     */
    public CameraFragment(CameraManager cManager){
        this.cameraManager = cManager;
    }

    /**
     * Sets the sample rate for capturing frames.
     *
     * @param rate The sample rate in milliseconds.
     */
    public void setSampleRate(int rate) {
        this.SAMPLE_RATE_MS = rate;
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        // Access the view for displaying camera output
        textureView = view.findViewById(R.id.cameraView);
        // Initialize bitmap for camera frame
        bits = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bits);
        canvas.drawColor(Color.GREEN);

        // Initialize gesture recognition
        this.gestureRecognition = new GestureRecognition(this.getActivity());

        return view;
    }

    /**
     * Called immediately after onCreateView(LayoutInflater, ViewGroup, Bundle) has returned,
     * but before any saved state has been restored in to the view.
     *
     * @param view               The View returned by onCreateView(LayoutInflater, ViewGroup, Bundle).
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Set the listener for the view
        textureView.setSurfaceTextureListener(surfaceTextureListener);
    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        /**
         * Invoked when a TextureView's SurfaceTexture is ready for use.
         *
         * @param surfaceTexture The SurfaceTexture.
         * @param width          The width of the surface, in pixels.
         * @param height         The height of the surface, in pixels.
         */
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            openCamera();
        }

        /**
         * Invoked when the specified SurfaceTexture's buffersize is changed.
         *
         * @param surfaceTexture The updated SurfaceTexture.
         * @param width          The new width of the surface, in pixels.
         * @param height         The new height of the surface, in pixels.
         */
        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            // No action needed
        }

        /**
         * Invoked when the specified SurfaceTexture is about to be destroyed.
         *
         * @param surfaceTexture The SurfaceTexture being destroyed.
         * @return true if the listener has consumed the event; false otherwise.
         */
        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            return true;
        }

        /**
         * Invoked when the specified SurfaceTexture is updated through updateTexImage().
         *
         * @param surfaceTexture The SurfaceTexture.
         */
        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
            // No action needed
        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        /**
         * Called when the camera device is opened successfully.
         *
         * @param camera The camera device that has been opened.
         */
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            if(backgroundHandler != null){
                createCameraPreview();
            }
        }

        /**
         * Called when the camera device is no longer available for use.
         *
         * @param camera The camera device that has been disconnected

        .
         */
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        /**
         * Called when the camera device has encountered a serious error.
         *
         * @param camera The camera device that encountered the error.
         * @param error  The error code.
         */
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            // Make camera null to prevent further use
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    /**
     * Opens the camera to begin capturing frames.
     */
    private void openCamera() {
        // Request permissions if not granted
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

    /**
     * Creates a preview of the camera output.
     */
    private void createCameraPreview() {
        SurfaceTexture texture = textureView.getSurfaceTexture();
        if (texture != null) {
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        }

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
        /**
         * Called when the camera capture session is fully configured and ready for action.
         *
         * @param session The session being configured.
         */
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

        /**
         * Called when the camera capture session configuration fails.
         *
         * @param session The session that failed configuration.
         */
        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.d("Failure", "Configure Failed");
        }
    };

    /**
     * Retrieves the ID of the front-facing camera.
     *
     * @return The ID of the front-facing camera.
     */
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

    /**
     * Called when the fragment is visible to the user and actively running.
     */
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

    /**
     * Called when the Fragment is no longer resumed.
     */
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

    /**
     * Closes the camera and releases resources.
     */
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

    /**
     * Starts the background thread for processing tasks.
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    /**
     * Starts the sampling thread for capturing frames.
     */
    private void startSamplingThread() {
        samplerThread = new HandlerThread("CameraSampling");
        samplerThread.start();
        samplerHandler = new Handler(samplerThread.getLooper());
        samplerHandler.post(sample);
    }

    /**
     * Stops the background thread.
     */
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

    /**
     * Stops the sampling thread.
     */
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

    /**
     * Runnable for sampling frames from the camera.
     */
    private final Runnable sample = new Runnable() {
        @Override
        public void run() {
            if(textureView != null){
                bits = textureView.getBitmap();
                if(bits != null){
                    bits = Bitmap.createScaledBitmap(bits, 224, 224, true);
                    MPImage image = new BitmapImageBuilder(bits).build();
                    String label = gestureRecognition.gestureInference(image);

                    if(!label.equals("None")){
                        sendMLAlertIntent(label);
                    }
                }
            }
            samplerHandler.postDelayed(sample, SAMPLE_RATE_MS);

        }
    };

    /**
     * Sends a broadcast intent with the ML label.
     *
     * @param mlVal The ML label to send.
     */
    private void sendMLAlertIntent(String mlVal) {
        Intent intent = new Intent("LABEL");
        intent.putExtra("LABEL", mlVal);
        Context parent = this.getContext();
        if(parent != null) {
            LocalBroadcastManager.getInstance(parent).sendBroadcast(intent);
        }
    }

}