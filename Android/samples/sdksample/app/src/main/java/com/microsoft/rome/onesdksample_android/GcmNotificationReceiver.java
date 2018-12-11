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

public class GcmNotificationReceiver extends BroadcastReceiver {
    // region Member Variables
    private static final String TAG = GcmNotificationReceiver.class.getName();
    private static final String RegistrationComplete = "registrationComplete";

    private ConnectedDevicesNotificationRegistration mNotificationRegistration;
    private AsyncOperation<ConnectedDevicesNotificationRegistration> mNotificationRegistrationOperation;
    private Context mContext;
    // endregion

    GcmNotificationReceiver(Context context) {
        mContext = context;

        registerGcmBroadcastReceiver();
    }

    /**
     * This function returns Notification Registration after it completes async operation.
     * @return Notification Registration.
     */
    public synchronized AsyncOperation<ConnectedDevicesNotificationRegistration> getNotificationRegistrationAsync() {
        if (mNotificationRegistrationOperation == null) {
            mNotificationRegistrationOperation = new AsyncOperation<>();
        }
        if (mNotificationRegistration != null) {
            mNotificationRegistrationOperation.complete(mNotificationRegistration);
        }
        return mNotificationRegistrationOperation;
    }

    /**
     * When GCM has been registered, this will get fired.
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
            Log.e(TAG, "Got notification that GCM had been registered, but token is null. Was app ID set in GcmRegistrationIntentService?");
        }

        synchronized (this) {
            mNotificationRegistration = new ConnectedDevicesNotificationRegistration();
            mNotificationRegistration.setNotificationType(ConnectedDevicesNotificationType.GCM);
            mNotificationRegistration.setToken(token);
            mNotificationRegistration.setAppId(Secrets.GCM_SENDER_ID);
            mNotificationRegistration.setAppDisplayName("OneRomanApp");

            // Create the async event for returning the NotificationRegistration if it does not exist
            if (mNotificationRegistrationOperation == null) {
                mNotificationRegistrationOperation = new AsyncOperation<>();
            }

            Log.i(TAG, "Completing the GcmNotificationReceiver operation with token: " + token);

            // Complete the async event so the NotificationRegistration can be accessed when needed
            mNotificationRegistrationOperation.complete(mNotificationRegistration);
        }

        mContext.startService(new Intent(mContext, SampleGcmListenerService.class));
    }

    /**
     * This function is called to start GCM registration service.
     * Start GCMRegistrationIntentService to register with GCM.
     */
    private void startGcmRegistrationIntentService() {
        Intent registrationIntentService = new Intent(mContext, RegistrationIntentService.class);
        mContext.startService(registrationIntentService);
    }

    /**
     * This function is called to register GCM.
     */
    private void registerGcmBroadcastReceiver() {
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this, new IntentFilter(RegistrationComplete));
        startGcmRegistrationIntentService();
    }
}