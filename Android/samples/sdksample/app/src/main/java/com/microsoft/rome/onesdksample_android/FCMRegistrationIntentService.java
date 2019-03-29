//
// Copyright (C) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import java.io.IOException;

/**
 * Gets the FCM Registration token for SENDER_ID.
 * Subscribes to FCM topics with token.
 */
public class FCMRegistrationIntentService extends IntentService {
    private static final String TAG = FCMRegistrationIntentService.class.getName();
    private static final String IntentServiceName = "FCMRegIntentService";
    private static final String FCM_SENDER_ID = Secrets.GCM_SENDER_ID;
    private static final String[] TOPICS = {};
    private static final String RegistrationComplete = "registrationComplete";
    private static final String TOKEN = "TOKEN";

    public FCMRegistrationIntentService() {
        super(IntentServiceName);
    }

    public FCMRegistrationIntentService(String name) {
        super(name);
    }

    /**
     * Get the GCM token and send out broad cast.
     * @param  intent  The value passed to broadcast receiver.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        String token = null;
        if (FCM_SENDER_ID == null || FCM_SENDER_ID.isEmpty()) {
            Log.i(TAG, "FCM Sender ID is null or empty, skipping FCM registration");
        } else {
            try {
                Log.i(TAG, "FCM Registration token: " + token);
                token = FirebaseInstanceId.getInstance().getToken(FCM_SENDER_ID, "FCM");
                Log.i(TAG, "FCM Registration token: " + token);
            } catch (IOException e) { Log.e(TAG, "Failed to get token, " + e.getMessage()); }
        }

        Intent registrationComplete = new Intent(RegistrationComplete);
        registrationComplete.putExtra(TOKEN, token);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }
}
