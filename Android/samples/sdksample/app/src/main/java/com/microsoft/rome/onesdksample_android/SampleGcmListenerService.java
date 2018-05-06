//
// Copyright (C) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.microsoft.connecteddevices.core.NotificationReceiver;

/**
 * Communicates with Google Cloud Messaging.
 */

public class SampleGcmListenerService extends GcmListenerService {
    private static final String TAG = "GcmListenerService";

    /**
     * Check whether it's a rome notification or not.
     * If it is a rome notification,
     * It will notify the apps with the information in the notification.
     * @param  from  describes message sender.
     * @param  data  message data as String key/value pairs.
     */
    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG, "From: " + from);

        if (!NotificationReceiver.Receive(data)) {
            Log.d(TAG, "GCM client received a message that was not a Rome notification");
        }
    }
}