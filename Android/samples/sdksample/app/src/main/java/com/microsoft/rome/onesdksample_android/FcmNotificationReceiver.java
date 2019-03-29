/*
 * Copyright (C) Microsoft Corporation. All rights reserved.
 */

package com.microsoft.rome.onesdksample_android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.microsoft.connecteddevices.AsyncOperation;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationRegistration;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationType;

public class FcmNotificationReceiver extends BroadcastReceiver {
    // region Member Variables
    private static final String TAG = FcmNotificationReceiver.class.getName();
    private static final String RegistrationComplete = "registrationComplete";

    private Context mContext;
    private static AsyncOperation<ConnectedDevicesNotificationRegistration> sNotificationRegistrationOperation; 
    // endregion

    FcmNotificationReceiver(Context context) {
        mContext = context;

        registerFcmBroadcastReceiver();
    }

    /**
     * TODO: Comment
     * @param registration
     */
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
    public synchronized static AsyncOperation<ConnectedDevicesNotificationRegistration> getNotificationRegistrationAsync() {
        // Create the registration operation if it the registration has not been received yet
        if (sNotificationRegistrationOperation == null) {
            sNotificationRegistrationOperation = new AsyncOperation<>();
        }
        return sNotificationRegistrationOperation;
    }

    /**
     * When GCM has been registered, this will get fired.
     * @param context the application's context.
     * @param intent the broadcast intent sent to any interested BroadcastReceiver components.
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
            Log.e(TAG, "Got notification that GCM had been registered, but token is null. Was app ID set in FcmRegistrationIntentService?");
        } else if (token.isEmpty()) {
            Log.e(TAG, "FcmNotificationReceiver gained the a token however it was empty");
        } else {
            Log.i(TAG, "FcmNotificationReceiver gained the token: " + token);

            ConnectedDevicesManager.getConnectedDevicesManager(context).setNotificationRegistration(token);
        }

        mContext.startService(new Intent(mContext, SampleFCMListenerService.class));
    }

    /**
     * This function is called to start GCM registration service.
     * Start GCMRegistrationIntentService to register with GCM.
     */
    private void startFcmRegistrationIntentService() {
        Intent registrationIntentService = new Intent(mContext, FCMRegistrationIntentService.class);
        mContext.startService(registrationIntentService);
    }

    /**
     * This function is called to register GCM.
     */
    private void registerFcmBroadcastReceiver() {
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this, new IntentFilter(RegistrationComplete));
        startFcmRegistrationIntentService();
    }
}
