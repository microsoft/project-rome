//
// Copyright (C) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.microsoft.connecteddevices.ConnectedDevicesPlatform;
import com.microsoft.connecteddevices.ConnectedDevicesProcessNotificationOperation;

import java.util.ArrayList;

/**
 * Communicates with Google Cloud Messaging.
 */

public class SampleGcmListenerService extends GcmListenerService {
    private final String TAG = SampleGcmListenerService.class.getName();

    /**
     * If it is a ConnectedDevices notification,
     * It will notify the apps with the information in the notification.
     * @param  from  describes message sender.
     * @param  data  message data as String key/value pairs.
     */
    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG, "GCM listener received data from: " + from);

        // Get a ConnectedDevicesPlatform to give the notification to
        ConnectedDevicesPlatform platform = ConnectedDevicesManager.getConnectedDevicesManager(getApplicationContext()).getPlatform();

        platform.processNotification(data).waitForCompletionAsync().thenAcceptAsync((Void v) -> {
            // The notification has finished being processed. The app is ready to
            // be shutdown or if woken from the background service, this is where
            // you would shutdown your background service early to be a good citizen.
        });
    }
}
