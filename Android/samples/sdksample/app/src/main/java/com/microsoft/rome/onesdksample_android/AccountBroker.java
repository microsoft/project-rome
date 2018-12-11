//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.app.Activity;
import android.content.Context;
import android.util.ArrayMap;
import android.util.Log;

import com.microsoft.connecteddevices.AsyncOperation;
import com.microsoft.connecteddevices.ConnectedDevicesAccount;
import com.microsoft.connecteddevices.signinhelpers.MSASigninHelperAccount;

/**
 * Broker for the MSA account provider, providing a static abstracted interface for gaining the AccountProvider
 */
public class AccountBroker {
    // region Member Variables
    private static final String TAG = AccountBroker.class.getName();
    private static MSASigninHelperAccount mSignInHelper;
    private static String currentAccountId;
    // endregion

    public AccountBroker(Context context) {
        // Create sign-in helper from helper lib, which does user account and access token management for us
        // Takes three parameters: a client id for msa, a map of requested auto scopes to override, and the context
        mSignInHelper = new MSASigninHelperAccount(Secrets.MSA_CLIENT_ID, new ArrayMap<String, String[]>(), context);
    }

    public void signIn(Activity activity, AsyncOperation.ResultBiConsumer<ConnectedDevicesAccount, Throwable> signInCompletionHandler) {
        // Grab the ClientID associated with that name
        Log.v(TAG, "Starting sign in process");
        try {
            AsyncOperation<ConnectedDevicesAccount> signInOperation = mSignInHelper.signIn(activity);
            signInOperation.whenComplete(signInCompletionHandler);
        } catch (Exception e) { Log.e(TAG, "Exception when signing in: " + e.toString()); }
    }

    public static synchronized MSASigninHelperAccount getSignInHelper() {
        return mSignInHelper;
    }

    public static synchronized String getCurrentAccountId(){
        return currentAccountId;
    }

    public static synchronized void setCurrentAccountId(String id) {
        currentAccountId = id;
    }

    public boolean isSignedIn() {
        return mSignInHelper.isSignedIn();
    }
}
