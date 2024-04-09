package com.example.conductor;

import android.content.Context;

import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.processors.ClassifierOptions;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer;
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult;

public class GestureRecognition {

    public static final String GESTURE_PATH = "gesture_recognizer.task";
    private final GestureRecognizer gestureRecognizer;

    public GestureRecognition(Context context) {
        BaseOptions baseOptions =
                BaseOptions.builder().setModelAssetPath(GESTURE_PATH).build();

        ClassifierOptions classifierOptions =
                ClassifierOptions.builder()
                        .setMaxResults(1)
                        .build();

        GestureRecognizer.GestureRecognizerOptions gestureOptions
                = GestureRecognizer.GestureRecognizerOptions.builder()
                .setBaseOptions(baseOptions)
                .setNumHands(1)
                .setCannedGesturesClassifierOptions(classifierOptions)
                .setMinHandDetectionConfidence(0.5F)
                .setMinTrackingConfidence(0.5F)
                .setMinHandPresenceConfidence(0.5F)
                .setRunningMode(RunningMode.IMAGE)
                .build();

        this.gestureRecognizer = GestureRecognizer.createFromOptions(context, gestureOptions);
    }

    public String gestureInference(MPImage image) {
        GestureRecognizerResult result = gestureRecognizer.recognize(image);
        String label;
        // Check label that was returned!
        if (result.gestures().size() > 0) {
            label = result.gestures().get(0).get(0).categoryName();
        } else {
            label = "None";
        }

        return label;
    }
}
