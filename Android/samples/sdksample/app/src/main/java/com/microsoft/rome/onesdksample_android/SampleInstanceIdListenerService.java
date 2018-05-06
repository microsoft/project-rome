//
// Copyright (C) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Handle token refresh.
 */
public class SampleInstanceIdListenerService extends InstanceIDListenerService {
    /**
     * This function is called when the system determines that the tokens need to be refreshed.
     * Start GCMRegistrationIntentService to fetch Instance ID token for GCM.
     */
    @Override
    public void onTokenRefresh() {
        startService(new Intent(this, RegistrationIntentService.class));
    }
}