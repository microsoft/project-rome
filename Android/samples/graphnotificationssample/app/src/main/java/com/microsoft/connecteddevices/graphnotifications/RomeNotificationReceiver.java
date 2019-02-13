//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.connecteddevices.graphnotifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.microsoft.connecteddevices.AsyncOperation;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationRegistration;

public class RomeNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = RomeNotificationReceiver.class.getName();
    private static final String RegistrationComplete = "registrationComplete";

    private static AsyncOperation<ConnectedDevicesNotificationRegistration> sNotificationRegistrationOperation;

    private Context mContext;

    RomeNotificationReceiver(Context context) {
        mContext = context;
        registerFCMBroadcastReceiver();
    }

    public static synchronized void setNotificationRegistration(ConnectedDevicesNotificationRegistration registration) {
        // Create the registration operation if it has not been requested already
        if (sNotificationRegistrationOperation == null) {
            sNotificationRegistrationOperation = new AsyncOperation<>();
        }

        // Complete the operation with the registration, to be fetched later
        sNotificationRegistrationOperation.complete(registration);
    }

    public static synchronized AsyncOperation<ConnectedDevicesNotificationRegistration> getNotificationRegistrationAsync() {
        // Create the registration operation if it the registration has not been received yet
        if (sNotificationRegistrationOperation == null) {
            sNotificationRegistrationOperation = new AsyncOperation<>();
        }

        return sNotificationRegistrationOperation;
    }

    /**
     * When FCM has been registered, this will get fired.
     * @param  context  the application's context.
     * @param  intent   the broadcast intent sent to any interested BroadcastReceiver components.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "Broadcast received: " + action);

        String token = null;
        if (action.equals(RegistrationComplete)) {
            token = intent.getExtras().getString("TOKEN");
        }

        if (token == null || token.isEmpty()) {
            Log.e(TAG, "RomeNotificationReceiver gained a token however it is null/empty, check FCMRegistrationIntentService");
        } else {
            Log.i(TAG, "RomeNotificationReceiver gained a token: " + token);
            ConnectedDevicesManager.getConnectedDevicesManager(context).setNotificationRegistration(token);
        }

        mContext.startService(new Intent(mContext, FCMListenerService.class));
    }

    /**
        * This function is called to start FCM registration service.
        * Start FCMRegistrationIntentService to register with FCM.
        */
    private void startFCMRegistrationIntentService() {
        Intent registrationIntentService = new Intent(mContext, FCMRegistrationIntentService.class);
        mContext.startService(registrationIntentService);
    }

    /**
     * This function is called to register FCM.
     */
    private void registerFCMBroadcastReceiver() {
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this, new IntentFilter(RegistrationComplete));
        startFCMRegistrationIntentService();
    }
}
