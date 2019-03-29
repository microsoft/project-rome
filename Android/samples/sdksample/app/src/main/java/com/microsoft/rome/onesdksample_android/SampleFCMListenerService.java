//
// Copyright (C) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import com.microsoft.connecteddevices.ConnectedDevicesNotification;
import com.microsoft.connecteddevices.ConnectedDevicesPlatform;

import java.util.Map;

/**
 * Communicates with Firebase Cloud Messaging.
 */
public class SampleFCMListenerService extends FirebaseMessagingService {
    private final String TAG = SampleFCMListenerService.class.getName();

    /**
     * Check whether it's a rome notification or not.
     * If it is a rome notification,
     * It will notify the apps with the information in the notification.
     * @param  message  FCM class for messaging with a from a data field.
     */
    @Override
    public void onMessageReceived(RemoteMessage message) {
        Log.d(TAG, "ListenerService received message from: " + message.getFrom());

        ConnectedDevicesNotification notification = ConnectedDevicesNotification.tryParse(message.getData());
        if (notification == null) {
            Log.w(TAG, "The FCM notification is not a ConnectedDevicesNotification");
            return;
        }

        // Get a ConnectedDevicesPlatform to give the notification to
        ConnectedDevicesPlatform platform = ConnectedDevicesManager.getConnectedDevicesManager(getApplicationContext()).getPlatform();

        platform.processNotificationAsync(notification).thenAcceptAsync((Void v) -> {
            // The notification has finished being processed. The app is ready to
            // be shutdown or if woken from the background service, this is where
            // you would shutdown your background service early to be a good citizen.
        });
    }
}
