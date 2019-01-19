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
     * If it is a rome notification,
     * It will notify the apps with the information in the notification.
     * @param  from  describes message sender.
     * @param  data  message data as String key/value pairs.
     */
    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG, "GCM listener received data from: " + from);

        // Get an existing ConnectedDevicesManager or initialize one and give it the notification
        ConnectedDevicesManager.getOrInitializeConnectedDevicesManager(getApplicationContext()).receiveNotificationAsync(data);
    }
}
