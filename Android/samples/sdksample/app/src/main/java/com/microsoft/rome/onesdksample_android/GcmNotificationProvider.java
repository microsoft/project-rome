/*
 * Copyright (C) Microsoft Corporation. All rights reserved.
 */

package com.microsoft.rome.onesdksample_android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.microsoft.connecteddevices.base.AsyncOperation;
import com.microsoft.connecteddevices.base.EventListener;
import com.microsoft.connecteddevices.core.NotificationProvider;
import com.microsoft.connecteddevices.core.NotificationRegistration;
import com.microsoft.connecteddevices.core.NotificationType;

import java.util.HashMap;
import java.util.Map;

/**
 * Implement NotificationProvider interface.
 * It is constructed when the platform is initialized.
 */
public class GcmNotificationProvider extends BroadcastReceiver implements NotificationProvider {
    // region Member Variables
    private static final String TAG = GcmNotificationProvider.class.getName();
    private static final String RegistrationComplete = "registrationComplete";
    private static final String GCM_SENDER_ID = Secrets.GCM_SENDER_ID;

    private Map<Long, EventListener<NotificationProvider, NotificationRegistration>> mListenerMap;
    private Long mNextListenerId = 0L;
    private NotificationRegistration mNotificationRegistration;
    private AsyncOperation<NotificationRegistration> mAsync;
    private Context mContext;
    // endregion

    GcmNotificationProvider(Context context) {
        mListenerMap = new HashMap<>();
        mContext = context;

        registerGcmBroadcastReceiver();
    }

    /**
     * This function returns Notification Registration after it completes async operation.
     * @return Notification Registration.
     */
    @Override
    public synchronized @NonNull AsyncOperation<NotificationRegistration> getNotificationRegistrationAsync() {
        if (mAsync == null) {
            mAsync = new AsyncOperation<>();
        }
        if (mNotificationRegistration != null) {
            mAsync.complete(mNotificationRegistration);
        }
        return mAsync;
    }

    /**
     * This function adds new event listener to notification provider.
     * @param  listener  the EventListener.
     * @return id        next event listener id.
     */
    @Override
    public synchronized long addNotificationProviderChangedListener(
        @NonNull EventListener<NotificationProvider, NotificationRegistration> listener) {
        mListenerMap.put(mNextListenerId, listener);
        return mNextListenerId++;
    }

    /**
     * This function removes the event listener.
     * @param  id  the id corresponds to the event listener that would be removed.
     */
    @Override
    public synchronized void removeNotificationProviderChangedListener(long id) {
        mListenerMap.remove(id);
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
        } else {
            synchronized (this) {
                mNotificationRegistration =
                    new NotificationRegistration(NotificationType.GCM, token, GCM_SENDER_ID, "onesdksample_android");

                if (mAsync == null) {
                    mAsync = new AsyncOperation<>();
                }
                mAsync.complete(mNotificationRegistration);
                mAsync = new AsyncOperation<>();

                for (EventListener<NotificationProvider, NotificationRegistration> event : mListenerMap.values()) {
                    event.onEvent(this, mNotificationRegistration);
                }
            }
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