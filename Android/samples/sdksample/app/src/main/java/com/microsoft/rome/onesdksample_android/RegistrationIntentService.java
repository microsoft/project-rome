//
// Copyright (C) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

/**
 * Gets the GCM Registration token for SENDER_ID.
 * Subscribes to GCM topics with token.
 */
public class RegistrationIntentService extends IntentService {

    private static final String TAG = RegistrationIntentService.class.getName();
    private static final String IntentServiceName = "GCMRegIntentService";
    private static final String GCM_SENDER_ID = Secrets.GCM_SENDER_ID;
    private static final String[] TOPICS = {};
    private static final String RegistrationComplete = "registrationComplete";
    private static final String TOKEN = "TOKEN";

    public RegistrationIntentService() {
        super(IntentServiceName);
    }

    public RegistrationIntentService(String name) {
        super(name);
    }

    /**
     * Get the GCM token and send out broad cast.
     * @param  intent  The value passed to broadcast receiver.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            InstanceID.getInstance(this).deleteInstanceID();
        } catch (IOException e) { Log.e(TAG, "Failed to delete instanceID, " + e.getMessage()); }

        InstanceID instanceId = InstanceID.getInstance(this);
        String token = null;
        if (GCM_SENDER_ID == null || GCM_SENDER_ID.isEmpty()) {
            Log.i(TAG, "GCM Sender ID is null or empty, skipping GCM registration");
        } else {
            try {
                token = instanceId.getToken(GCM_SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                Log.i(TAG, "GCM Registration token: " + token);
            } catch (IOException e) { Log.e(TAG, "Failed to get token, " + e.getMessage()); }
        }

        Intent registrationComplete = new Intent(RegistrationComplete);
        registrationComplete.putExtra(TOKEN, token);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }
}