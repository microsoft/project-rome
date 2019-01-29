//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.connecteddevices.signinhelpers;

import android.app.Activity;
import android.support.annotation.Keep;

import com.microsoft.connecteddevices.ConnectedDevicesAccount;
import com.microsoft.connecteddevices.AsyncOperation;

import java.util.List;

@Keep
public interface SigninHelperAccount {
    AsyncOperation<ConnectedDevicesAccount> signIn(Activity currentActivity);
    AsyncOperation<ConnectedDevicesAccount> signOut(Activity currentActivity);
    AsyncOperation<String> getAccessTokenAsync(final List<String> scopes);
    boolean isSignedIn();
    ConnectedDevicesAccount getAccount();
}
