package com.example.conductor.listeners;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class MediaNotificationListener extends NotificationListenerService {

    private static final String TAG = "MediaNotificationListener";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // Check if the notification is a media notification and handle as needed
        if (isMediaNotification(sbn)) {
            // Log.d(TAG, "Media Notification Posted: " + sbn.getPackageName());
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Handle notification removal if needed
    }

    protected boolean isMediaNotification(StatusBarNotification sbn) {
        // Check if the notification is a media notification
        // You can add more sophisticated checks based on your requirements
        return sbn.getNotification().category != null &&
                sbn.getNotification().category.equals(Notification.CATEGORY_TRANSPORT);
    }
}

