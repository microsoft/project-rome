//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.connecteddevices.graphnotifications;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.ArrayMap;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.microsoft.connecteddevices.ConnectedDevicesPlatform;

import java.util.Map;

/**
 * Communicates with Firebase Cloud Messaging.
 */
public class FCMListenerService extends FirebaseMessagingService {
    private static final String TAG = "FCMListenerService";

    private static final String RegistrationComplete = "registrationComplete";
    private static final String TOKEN = "TOKEN";

    private static String s_previousToken = null;

    @Override
    public void onCreate() {
        PlatformManager.getInstance().createNotificationReceiver(this);
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
     * Check whether it's a rome notification or not.
     * If it is a rome notification,
     * It will notify the apps with the information in the notification.
     * @param  message  FCM class for messaging with a from a data field.
     */
    @Override
    public void onMessageReceived(RemoteMessage message) {
        Log.d(TAG, "From: " + message.getFrom());
        Map data = message.getData();
        ConnectedDevicesPlatform platform =  ensurePlatformInitialized();
        platform.processNotification(data);
    }

    @Override
    public void onNewToken(String token) {
        if (token != null && !token.equals(s_previousToken)) {
            s_previousToken = token;
            Intent registrationComplete = new Intent(RegistrationComplete);
            registrationComplete.putExtra(TOKEN, token);
            LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
        }
    }

    synchronized ConnectedDevicesPlatform ensurePlatformInitialized() {
        // First see if we have an existing platform
        ConnectedDevicesPlatform platform = PlatformManager.getInstance().getPlatform();
        if (platform != null) {
            return platform;
        }

        // No existing platform, so we have to create our own
        return PlatformManager.getInstance().createPlatform(getApplicationContext());
    }

}
