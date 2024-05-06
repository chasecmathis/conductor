package com.example.conductor.util;

import android.content.Context;

import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.processors.ClassifierOptions;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer;
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult;

/**
 * Helper class for gesture recognition using MediaPipe.
 */
public class GestureRecognition {

    // Path to the gesture recognizer model
    public static final String GESTURE_PATH = "gesture_recognizer.task";

    // GestureRecognizer instance
    private final GestureRecognizer gestureRecognizer;

    /**
     * Constructor for GestureRecognition.
     *
     * @param context The application context.
     */
    public GestureRecognition(Context context) {
        // Configure base options for the gesture recognizer
        BaseOptions baseOptions =
                BaseOptions.builder().setModelAssetPath(GESTURE_PATH).build();

        // Configure options for the gesture classifier
        ClassifierOptions classifierOptions =
                ClassifierOptions.builder()
                        .setMaxResults(1)
                        .build();

        // Configure options for the gesture recognizer
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

        // Create the gesture recognizer instance
        this.gestureRecognizer = GestureRecognizer.createFromOptions(context, gestureOptions);
    }

    /**
     * Perform gesture inference on the given image.
     *
     * @param image The input image.
     * @return The inferred gesture label.
     */
    public String gestureInference(MPImage image) {
        // Perform gesture recognition on the input image
        GestureRecognizerResult result = gestureRecognizer.recognize(image);
        String label;
        // Check the label returned by the recognizer
        if (result.gestures().size() > 0) {
            label = result.gestures().get(0).get(0).categoryName();
        } else {
            label = "None";
        }

        return label;
    }
}
