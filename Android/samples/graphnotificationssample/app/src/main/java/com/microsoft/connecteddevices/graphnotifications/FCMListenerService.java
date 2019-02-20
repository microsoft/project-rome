//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.connecteddevices.graphnotifications;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import com.microsoft.connecteddevices.ConnectedDevicesNotification;
import com.microsoft.connecteddevices.ConnectedDevicesPlatform;

import java.util.Map;

/**
 * Communicates with Firebase Cloud Messaging.
 */
public class FCMListenerService extends FirebaseMessagingService {
    private static final String TAG = "FCMListenerService";
    private static final String REGISTRATION_COMPLETE = "registrationComplete";
    private static final String TOKEN = "TOKEN";

    private static String sPreviousToken = "";

    @Override
    public void onCreate() {
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult().getToken();
                if (!token.isEmpty()) {
                    FCMListenerService.this.onNewToken(token);
                }
            }
        });
    }

    /**
     * Check whether it's a Rome notification or not.
     * If it is a rome notification,
     * It will notify the apps with the information in the notification.
     * @param  message  FCM class for messaging with a from a data field.
     */
    @Override
    public void onMessageReceived(RemoteMessage message) {
        Log.d(TAG, "FCM notification received from: " + message.getFrom());
        Map data = message.getData();
        ConnectedDevicesNotification notification = ConnectedDevicesNotification.tryParse(data);

        if (notification != null) {
            try {
                ConnectedDevicesPlatform platform = ConnectedDevicesManager.getConnectedDevicesManager(getApplicationContext()).getPlatform();

                // NOTE: it may be useful to attach completion to this async in order to know when the notification is done being processed.
                // This would be a good time to stop a background service or otherwise cleanup.
                platform.processNotificationAsync(notification);
            } catch (Exception e) {
                Log.e(TAG, "Failed to process FCM notification" + e.getMessage());
            }
        }
    }

    @Override
    public void onNewToken(String token) {
        if (token != null && !token.equals(sPreviousToken)) {
            sPreviousToken = token;
            Intent registrationComplete = new Intent(REGISTRATION_COMPLETE);
            registrationComplete.putExtra(TOKEN, token);
            LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
        }
    }
}
