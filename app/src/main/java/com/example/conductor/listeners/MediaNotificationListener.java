package com.example.conductor.listeners;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * Service for listening to media notifications.
 */
public class MediaNotificationListener extends NotificationListenerService {

    // Tag for logging
    private static final String TAG = "MediaNotificationListener";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // Check if the notification is a media notification and handle as needed
        if (isMediaNotification(sbn)) {
            // Log the package name of the posted media notification
            Log.d(TAG, "Media Notification Posted: " + sbn.getPackageName());
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Handle notification removal if needed
    }

    /**
     * Checks if the notification is a media notification.
     *
     * @param sbn The StatusBarNotification instance representing the notification.
     * @return True if the notification is a media notification, false otherwise.
     */
    protected boolean isMediaNotification(StatusBarNotification sbn) {
        // Check if the notification category is CATEGORY_TRANSPORT, which indicates a media notification
        // You can add more sophisticated checks based on your requirements
        return sbn.getNotification().category != null &&
                sbn.getNotification().category.equals(Notification.CATEGORY_TRANSPORT);
    }
}
