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

    /**
     * This function returns Notification Registration after it completes async operation.
     * @return Notification Registration.
     */
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
        String token = null;
        String action = intent.getAction();

        Log.i(TAG, "Broadcast received: " + action);

        if (action.equals(RegistrationComplete)) {
            token = intent.getExtras().getString("TOKEN");
        }

        if (token == null) {
            Log.e(TAG,
                "Got notification from FCM but token is null. Was app ID set in FCMListenerService?");
        } else if (token.isEmpty()) {
            Log.e(TAG, "RomeNotificationReceiver gained the a token however it was empty");
        } else {
            Log.i(TAG, "RomeNotificationReceiver gained the token: " + token);

            ConnectedDevicesManager.getConnectedDevicesManager(context).setNotificationRegistration(token);
        }
    }

    /**
        * This function is called to start FCM registration service.
        * Start FCMRegistrationIntentService to register with FCM.
        */
    private void startService() {
        Log.e(TAG, "Starting FCMListenerService");
        Intent registrationIntentService = new Intent(mContext, FCMListenerService.class);
        mContext.startService(registrationIntentService);
    }

    /**
     * This function is called to register FCM.
     */
    private void registerFCMBroadcastReceiver() {
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this, new IntentFilter(RegistrationComplete));
        startService();
    }
}
