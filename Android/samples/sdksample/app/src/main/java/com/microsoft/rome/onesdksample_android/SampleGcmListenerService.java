//
// Copyright (C) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.microsoft.connecteddevices.ConnectedDevicesPlatform;
import com.microsoft.connecteddevices.ConnectedDevicesProcessNotificationOperation;
import com.microsoft.connecteddevices.remotesystems.commanding.AppServiceProvider;

import java.util.ArrayList;

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

        ConnectedDevicesPlatform platform;

        try {
            platform = ensurePlatformInitialized();
        } catch (Exception e) {
            Log.e(TAG, "Dropping cloud notification because platform could not be initialized", e);
            return;
        }

        ConnectedDevicesProcessNotificationOperation operation = platform.processNotification(data);
    }

    private ConnectedDevicesPlatform ensurePlatformInitialized() {
        // First see if we have an existing platform
        PlatformBroker platformBroker = PlatformBroker.getPlatformBroker();
        ConnectedDevicesPlatform platform = platformBroker.getPlatform();
        if (platform != null) {
            return platform;
        }

        // No existing platform, so we have to create our own
        GcmNotificationReceiver gcmNotificationProvider = new GcmNotificationReceiver(this);
        platformBroker.getOrInitializePlatform(getApplicationContext());
        platformBroker.startPlatform();
        platformBroker.createNotificationReceiver(this);
        platformBroker.registerNotificationsForAccount(platformBroker.getAccount(AccountBroker.getCurrentAccountId()));

        // TODO: Check for RemoteSystemAppRegistration for all accounts.

        return platformBroker.getPlatform();
    }
}