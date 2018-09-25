package com.microsoft.connecteddevices.graphnotifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.microsoft.connecteddevices.base.AsyncOperation;
import com.microsoft.connecteddevices.base.EventListener;
import com.microsoft.connecteddevices.core.NotificationProvider;
import com.microsoft.connecteddevices.core.NotificationRegistration;
import com.microsoft.connecteddevices.core.NotificationType;

import java.util.HashMap;
import java.util.Map;

public class RomeNotificationProvider extends BroadcastReceiver implements NotificationProvider {
    private Map<Long, EventListener<NotificationProvider, NotificationRegistration>> mListenerMap;
    private Long mNextListenerId = 0L;
    private NotificationRegistration mNotificationRegistration;
    private AsyncOperation<NotificationRegistration> mAsync;
    private Context mContext;
    private static final String RegistrationComplete = "registrationComplete";

    RomeNotificationProvider(Context context) {
        mListenerMap = new HashMap<>();
        mContext = context;

        registerFCMBroadcastReceiver();
    }

    /**
     * This function returns Notification Registration after it completes async operation.
     * @return Notification Registration.
     */
    @Override
    public synchronized AsyncOperation<NotificationRegistration> getNotificationRegistrationAsync() {
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
        EventListener<NotificationProvider, NotificationRegistration> listener) {
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
     * When FCM has been registered, this will get fired.
     * @param  context  the application's context.
     * @param  intent   the broadcast intent sent to any interested BroadcastReceiver components.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String token = null;
        String action = intent.getAction();

        Log.i("Receiver", "Broadcast received: " + action);

        if (action.equals(RegistrationComplete)) {
            token = intent.getExtras().getString("TOKEN");
        }

        if (token == null) {
            Log.e("GraphNotifications",
                "Got notification that FCM had been registered, but token is null. Was app ID set in FCMRegistrationIntentService?");
        }

        synchronized (this) {
            mNotificationRegistration = new NotificationRegistration(NotificationType.FCM, token, Secrets.FCM_SENDER_ID, "GraphNotifications");

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
    /**
        * This function is called to start FCM registration service.
        * Start FCMRegistrationIntentService to register with FCM.
        */
    private void startService() {
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
