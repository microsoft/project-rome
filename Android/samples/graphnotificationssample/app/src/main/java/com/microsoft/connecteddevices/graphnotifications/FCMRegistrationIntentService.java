//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.connecteddevices.graphnotifications;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

import java.io.IOException;

public class FCMRegistrationIntentService extends IntentService {
    private static final String TAG = FCMRegistrationIntentService.class.getName();
    private static final String RegistrationComplete = "registrationComplete";
    private static final String TOKEN = "TOKEN";
    private static final String IntentServiceName = "FCMRegIntentService";
    private static final String FCM_SENDER_ID = Secrets.FCM_SENDER_ID;

    public FCMRegistrationIntentService() {
        super(IntentServiceName);
    }

    public FCMRegistrationIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String token = "";
        if (FCM_SENDER_ID == null || FCM_SENDER_ID.isEmpty()) {
            Log.i(TAG, "FCM SenderID is null/empty, skipping FCM registration!");
        } else {
            try {
                token = FirebaseInstanceId.getInstance().getToken(FCM_SENDER_ID, "FCM");
                Log.i(TAG, "FCM registration token: " + token);
            } catch (IOException e) {
                Log.e(TAG, "Failed to get FCM registration token " + e.getMessage());
            }

            Intent registrationComplete = new Intent(RegistrationComplete);
            registrationComplete.putExtra(TOKEN, token);
            LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
        }
    }
}
