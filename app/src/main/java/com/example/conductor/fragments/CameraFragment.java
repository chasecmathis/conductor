package com.example.conductor.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.media.ImageReader;
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
import android.widget.ImageView;

import com.example.conductor.ML_Functions;
import com.example.conductor.R;

import org.tensorflow.lite.Interpreter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    private Interpreter tflite;


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


    public CameraFragment(CameraManager cManager, Interpreter tflite){
        this.cameraManager = cManager;
        this.tflite = tflite;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        //Access the view for displaying camera output
        textureView = view.findViewById(R.id.cameraView);
        bits = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bits);
        canvas.drawColor(Color.GREEN);
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
                    bits = Bitmap.createScaledBitmap(bits, 200, 200, true);
                    //bits = toGrayscale(bits);
                    //TODO Send this grayscale small bitmap to model
                    if(tflite != null){
                        float [][][][] testIn = formatInput(bits);
                        float [][] testOut = new float[1][29];
                        tflite.run(testIn, testOut);

                        int label = findMax(testOut[0]);

                        //Log.d("Confidence", Arrays.toString(testOut[0]));
                        sendMLAlertIntent(label);

                        //Log.d("Returned", String.valueOf(MAPPING.charAt(label)));
                    }
                    else{
                        Log.d("TFLITE ERROR", "NULL");
                    }
                }
            }
            samplerHandler.postDelayed(sample, SAMPLE_RATE_MS);

        }
    };

    private int findMax(float[] confidenceArray){
        float maxValue = 0;
        int maxIndex = 0;
        for(int i = 0; i < confidenceArray.length; i++){
            if(confidenceArray[i] > maxValue){
                maxIndex = i;
                maxValue = confidenceArray[i];
            }
        }

        return maxIndex;
    }

    /**
     * Generated by ChatGPT for turning image grayscale
     *
     * @param originalBitmap
     * @return grayscale bitmap
     */
    private Bitmap toGrayscale(Bitmap originalBitmap) {
        int width, height;
        height = originalBitmap.getHeight();
        width = originalBitmap.getWidth();

        Bitmap grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(grayscaleBitmap);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter colorMatrixFilter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorMatrixFilter);
        canvas.drawBitmap(originalBitmap, 0, 0, paint);
        return grayscaleBitmap;
    }

    private float[][][][] formatInput(Bitmap bitmap){
        float [][][][] pixelArray = new float [1][200][200][1];
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                int pixel = bitmap.getPixel(x, y);
                // Extracting ARGB values
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;
                // Assuming grayscale, taking average of RGB values
                int grayscale = (red + green + blue) / 3;
                pixelArray[0][y][x][0] = grayscale;
            }
        }
        return pixelArray;
    }

    private void sendMLAlertIntent(int mlVal) {
        Intent intent = new Intent("ML_MESSAGE");
        intent.putExtra("VALUE", mlVal);
        Context parent = this.getContext();
        if(parent != null) {
            LocalBroadcastManager.getInstance(parent).sendBroadcast(intent);
        }
    }

}