package com.microsoft.connecteddevices.graphnotifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.microsoft.connecteddevices.AsyncOperation;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationRegistration;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationType;
import com.microsoft.connecteddevices.EventListener;

import java.util.HashMap;
import java.util.Map;

public class RomeNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = RomeNotificationReceiver.class.getName();
    private Map<Long, EventListener<RomeNotificationReceiver, ConnectedDevicesNotificationRegistration>> mListenerMap;
    private Long mNextListenerId = 0L;
    private ConnectedDevicesNotificationRegistration mNotificationRegistration;
    private AsyncOperation<ConnectedDevicesNotificationRegistration> mAsync;
    private Context mContext;
    private static final String RegistrationComplete = "registrationComplete";

    RomeNotificationReceiver(Context context) {
        mListenerMap = new HashMap<>();
        mContext = context;

        registerFCMBroadcastReceiver();
    }

    /**
     * This function returns Notification Registration after it completes async operation.
     * @return Notification Registration.
     */
    public synchronized AsyncOperation<ConnectedDevicesNotificationRegistration> getNotificationRegistrationAsync() {
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
    public synchronized long addNotificationProviderChangedListener(
        EventListener<RomeNotificationReceiver, ConnectedDevicesNotificationRegistration> listener) {
        mListenerMap.put(mNextListenerId, listener);
        return mNextListenerId++;
    }

    /**
     * This function removes the event listener.
     * @param  id  the id corresponds to the event listener that would be removed.
     */
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
            mNotificationRegistration = new ConnectedDevicesNotificationRegistration();
            mNotificationRegistration.setType(ConnectedDevicesNotificationType.FCM);
            mNotificationRegistration.setToken(token);
            mNotificationRegistration.setAppId(Secrets.FCM_SENDER_ID);
            mNotificationRegistration.setAppDisplayName("OneRomanApp");

            if (mAsync == null) {
                mAsync = new AsyncOperation<>();
            }
            mAsync.complete(mNotificationRegistration);
            mAsync = new AsyncOperation<>();

            for (EventListener<RomeNotificationReceiver, ConnectedDevicesNotificationRegistration> event : mListenerMap.values()) {
                event.onEvent(this, mNotificationRegistration);
            }

            Log.e(TAG, "Successfully completed FCM registration");
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
