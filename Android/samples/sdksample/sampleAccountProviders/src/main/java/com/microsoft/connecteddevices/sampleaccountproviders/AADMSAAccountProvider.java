//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.connecteddevices.sampleaccountproviders;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Keep;
import android.util.Log;

import com.microsoft.connecteddevices.base.AsyncOperation;
import com.microsoft.connecteddevices.base.EventListener;
import com.microsoft.connecteddevices.core.AccessTokenResult;
import com.microsoft.connecteddevices.core.UserAccountProvider;
import com.microsoft.connecteddevices.core.UserAccount;

import java.util.Hashtable;
import java.util.Map;

/**
 * Sign in helper that provides an UserAccountProvider implementation that works with both AAD and MSA accounts.
 *
 * To use this class, call signInMSA()/signOutMSA()/signInAAD()/signOutAAD(),
 * then access the UserAccountProvider through getUserAccountProvider().
 */
@Keep
public final class AADMSAAccountProvider implements UserAccountProvider {
    private static final String TAG = AADMSAAccountProvider.class.getName();

    public enum State {
        SignedOut,
        SignedInMSA,
        SignedInAAD,
    }

    private MSAAccountProvider mMSAProvider;
    private AADAccountProvider mAADProvider;
    private EventListener<UserAccountProvider, Void> mListener;

    private final Map<Long, EventListener<UserAccountProvider, Void>> mListenerMap = new Hashtable<>();
    private long mNextListenerId = 1L;

    /**
     * @param msaClientId           id of the app's registration in the MSA portal
     * @param msaScopeOverrides     scope overrides for the app
     * @param aadClientId           id of the app's registration in the Azure portal
     * @param aadRedirectUri        redirect uri the app is registered with in the Azure portal
     * @param context
     */
    public AADMSAAccountProvider(
        String msaClientId, final Map<String, String[]> msaScopeOverrides, String aadClientId, String aadRedirectUri, Context context) {

        // Chain the inner events to the event provided by this helper
        mListener = new EventListener<UserAccountProvider, Void>() {
            @Override
            public void onEvent(UserAccountProvider provider, Void aVoid) {
                notifyListenersAsync();
            }
        };

        mMSAProvider = new MSAAccountProvider(msaClientId, msaScopeOverrides, context);
        mAADProvider = new AADAccountProvider(aadClientId, aadRedirectUri, context);

        if (mMSAProvider.isSignedIn() && mAADProvider.isSignedIn()) {
            // Shouldn't ever happen, but if it does, sign AAD out
            mAADProvider.signOut();
        }

        mMSAProvider.addUserAccountChangedListener(mListener);
        mAADProvider.addUserAccountChangedListener(mListener);
    }

    private AsyncOperation<Void> notifyListenersAsync() {
        return AsyncOperation.supplyAsync(new AsyncOperation.Supplier<Void>() {
            @Override
            public Void get() {
                for (EventListener<UserAccountProvider, Void> listener : mListenerMap.values()) {
                    listener.onEvent(AADMSAAccountProvider.this, null);
                }
                return null;
            }
        });
    }

    public synchronized State getSignInState() {
        if (mMSAProvider != null && mMSAProvider.isSignedIn()) {
            return State.SignedInMSA;
        }
        if (mAADProvider != null && mAADProvider.isSignedIn()) {
            return State.SignedInAAD;
        }
        return State.SignedOut;
    }

    public AsyncOperation<Boolean> signInMSA(final Activity currentActivity) throws IllegalStateException {
        if (getSignInState() != State.SignedOut) {
            throw new IllegalStateException("Already signed into an account!");
        }
        return mMSAProvider.signIn(currentActivity);
    }

    public void signOutMSA(final Activity currentActivity) throws IllegalStateException {
        if (getSignInState() != State.SignedInMSA) {
            throw new IllegalStateException("Not currently signed into an MSA!");
        }
        mMSAProvider.signOut(currentActivity);
    }

    public AsyncOperation<Boolean> signInAAD() throws IllegalStateException {
        if (getSignInState() != State.SignedOut) {
            throw new IllegalStateException("Already signed into an account!");
        }
        return mAADProvider.signIn();
    }

    public void signOutAAD() throws IllegalStateException {
        if (getSignInState() != State.SignedInAAD) {
            throw new IllegalStateException("Not currently signed into an AAD account!");
        }
        mAADProvider.signOut();
    }

    private UserAccountProvider getSignedInProvider() {
        switch (getSignInState()) {
        case SignedInMSA: return mMSAProvider;
        case SignedInAAD: return mAADProvider;
        default: return null;
        }
    }

    @Override
    public synchronized UserAccount[] getUserAccounts() {
        UserAccountProvider provider = AADMSAAccountProvider.this.getSignedInProvider();
        return (provider != null) ? provider.getUserAccounts() : new UserAccount[0];
    }

    @Override
    public synchronized AsyncOperation<AccessTokenResult> getAccessTokenForUserAccountAsync(
        final String userAccountId, final String[] scopes) {
        UserAccountProvider provider = AADMSAAccountProvider.this.getSignedInProvider();
        if (provider != null) {
            return provider.getAccessTokenForUserAccountAsync(userAccountId, scopes);
        }

        AsyncOperation<AccessTokenResult> ret = new AsyncOperation<AccessTokenResult>();
        ret.completeExceptionally(new IllegalStateException("Not currently signed in!"));
        return ret;
    }

    @Override
    public synchronized long addUserAccountChangedListener(EventListener<UserAccountProvider, Void> listener) {
        long id = mNextListenerId++;
        mListenerMap.put(id, listener);
        return id;
    }

    @Override
    public synchronized void removeUserAccountChangedListener(long id) {
        mListenerMap.remove(id);
    }

    @Override
    public synchronized void onAccessTokenError(String userAccountId, String[] scopes, boolean isPermanentError) {
        UserAccountProvider provider = AADMSAAccountProvider.this.getSignedInProvider();
        if (provider != null) {
            provider.onAccessTokenError(userAccountId, scopes, isPermanentError);
        } else {
            Log.e(TAG, "Not currently signed in!");
        }
    }
}