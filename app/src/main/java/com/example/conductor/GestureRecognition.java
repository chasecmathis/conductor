package com.example.conductor;

import android.content.Context;
import android.util.Log;

import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.processors.ClassifierOptions;
import com.google.mediapipe.tasks.components.processors.proto.ClassifierOptionsProto;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer;
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult;

import java.util.HashMap;
import java.util.List;

public class GestureRecognition {

    public static final String GESTURE_PATH = "gesture_recognizer.task";
    private final GestureRecognizer gestureRecognizer;
    private final HashMap<String, Integer> gestureIndexMap;

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
        this.gestureIndexMap = createGestureIndexMap();
    }

    private HashMap<String, Integer> createGestureIndexMap() {
        HashMap<String, Integer> map = new HashMap<>();
        // Add your gesture mapping here
        map.put("None", -1);
        map.put("Closed_Fist", 0);
        map.put("Open_Palm", 1);
        map.put("Pointing_Up", 2);
        map.put("Thumb_Down", 3);
        map.put("Thumb_Up", 4);
        map.put("Victory", 5);
        map.put("ILoveYou", 6);
        return map;
    }

    public int gestureInference(MPImage image) {
        GestureRecognizerResult result = gestureRecognizer.recognize(image);
        String label = null;
        if (result.gestures().size() > 0) {
            label = result.gestures().get(0).get(0).categoryName();
        } else {
            label = "None";
        }
        Log.d("Image", label);

        try {
            return gestureIndexMap.get(label);
        } catch (NullPointerException nullPointerException) {
            return -1;
        }
    }
}
