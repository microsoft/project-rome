//
// Copyright (C) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.content.Intent;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Handle token refresh.
 */
public class SampleFCMInstanceIdListenerService extends FirebaseInstanceIdService {
    /**
     * This function is called when the system determines that the tokens need to be refreshed.
     * Start FCMRegistrationIntentService to fetch Instance ID token for FCM.
     */
    @Override
    public void onTokenRefresh() {
        startService(new Intent(this, FCMRegistrationIntentService.class));
    }
}
