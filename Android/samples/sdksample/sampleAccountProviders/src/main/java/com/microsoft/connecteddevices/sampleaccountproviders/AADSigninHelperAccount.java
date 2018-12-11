//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.connecteddevices.signinhelpers;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Keep;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.microsoft.aad.adal.ADALError;
import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationException;
import com.microsoft.aad.adal.AuthenticationResult;
import com.microsoft.aad.adal.AuthenticationResult.AuthenticationStatus;
import com.microsoft.aad.adal.PromptBehavior;
import com.microsoft.aad.adal.TokenCacheItem;

import com.microsoft.connecteddevices.AsyncOperation;
import com.microsoft.connecteddevices.ConnectedDevicesAccount;
import com.microsoft.connecteddevices.ConnectedDevicesAccountType;

import java.util.Iterator;
import java.util.List;

/**
 * Sign in helper that provides integration to the ConnectedDevicesAccountManager for AAD using the ADAL library.
 *
 * Notes about AAD/ADAL:
 *  - Resource          An Azure web service/app, such as https://graph.windows.net, or a CDP service.
 *  - Scope             Individual permissions within a resource
 *  - Access Token      A standard JSON web token for a given scope.
 *                      This is the actual token/user ticket used to authenticate with CDP services.
 *                      https://oauth.net/2/
 *                      https://www.oauth.com/oauth2-servers/access-tokens/
 *  - Refresh token:    A standard OAuth refresh token.
 *                      Lasts longer than access tokens, and is used to request new access tokens/refresh access tokens when they expire.
 *                      ADAL manages this automatically.
 *                      https://oauth.net/2/grant-types/refresh-token/
 *  - MRRT              Multiresource refresh token. A refresh token that can be used to fetch access tokens for more than one resource.
 *                      Getting one requires the user consent to all the covered resources. ADAL manages this automatically.
 */
@Keep
public final class AADSigninHelperAccount implements SigninHelperAccount {
    // region Constants
    private static final String TAG = AADSigninHelperAccount.class.getName();

    private static final String LOGIN_URL = "https://login.microsoftonline.com/common";
    private static final String GRAPH_URL = "https://graph.windows.net";
    // endregion

    // region Member Variables
    private final String mClientId;
    private final String mRedirectUri;
    private final AuthenticationContext mAuthContext;

    private ConnectedDevicesAccount mAccount; // Initialized when signed in

    private long mNextListenerId = 1L;
    // endregion

    /**
     * @param clientId              id of the app's registration in the Azure portal
     * @param redirectUri           redirect uri the app is registered with in the Azure portal
     * @param context
     */
    public AADSigninHelperAccount(String clientId, String redirectUri, Context context) {
        mClientId = clientId;
        mRedirectUri = redirectUri;

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(context);
        }

        mAuthContext = new AuthenticationContext(context, LOGIN_URL, false);

        Log.i(TAG, "Checking if previous AADSigninHelperAccount session can be loaded...");
        Iterator<TokenCacheItem> tokenCacheItems = mAuthContext.getCache().getAll();
        while (tokenCacheItems.hasNext()) {
            TokenCacheItem item = tokenCacheItems.next();
            if (item.getIsMultiResourceRefreshToken() && item.getClientId().equals(mClientId)) {
                mAccount = new ConnectedDevicesAccount(item.getUserInfo().getUserId(), ConnectedDevicesAccountType.AAD);
                break;
            }
        }

        if (mAccount != null) {
            Log.i(TAG, "Loaded previous AADSigninHelperAccount session, starting as signed in.");
        } else {
            Log.i(TAG, "No previous AADSigninHelperAccount session could be loaded, starting as signed out.");
        }
    }

    public String getClientId() {
        return mClientId;
    }

    public synchronized boolean isSignedIn() {
        return mAccount != null;
    }

    @Override
    public synchronized AsyncOperation<ConnectedDevicesAccount> signIn(final Activity currentActivity) throws IllegalStateException {
        if (isSignedIn()) {
            throw new IllegalStateException("AADSigninHelperAccount: Already signed in!");
        }

        final AsyncOperation<ConnectedDevicesAccount> signInOperation = new AsyncOperation<>();

        // If the user has not previously consented for this default resource for this app,
        // the interactive flow will ask for user consent for all resources used by the app.
        // If the user previously consented to this resource on this app, and more resources are added to the app later on,
        // a consent prompt for all app resources will be raised when an access token for a new resource is requested -
        // see getAccessTokenForAccountAsync()
        final String defaultResource = GRAPH_URL;

        mAuthContext.acquireToken( //
                defaultResource,       // resource
                mClientId,             // clientId
                mRedirectUri,          // redirectUri
                null,                  // loginHint
                PromptBehavior.Auto,   // promptBehavior
                null,                  // extraQueryParameters
                new AuthenticationCallback<AuthenticationResult>() {
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "acquireToken encountered an exception: " + e.toString() + ". This may be transient.");
                        signInOperation.completeExceptionally(
                                new Exception("acquireToken encountered an exception: " + e.toString() + ". This may be transient."));
                    }

                    @Override
                    public void onSuccess(AuthenticationResult result) {
                        if (result == null || result.getStatus() != AuthenticationStatus.Succeeded || result.getUserInfo() == null) {
                            signInOperation.completeExceptionally(
                                    new Exception("The result of the authentication was not successful or the user info was empty"));
                        } else {
                            signInOperation.complete(addAccount(result.getUserInfo().getUserId()));
                        }
                    }
                });

        return signInOperation;
    }

    @Override
    public synchronized AsyncOperation<ConnectedDevicesAccount> signOut(final Activity currentActivity) throws IllegalStateException {
        if (!isSignedIn()) {
            throw new IllegalStateException("AADSigninHelperAccount: Not currently signed in!");
        }

        final AsyncOperation<ConnectedDevicesAccount> signOutOperation = new AsyncOperation<>();

        // Delete cookies
        final CookieManager cookieManager = CookieManager.getInstance();

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookie();
            CookieSyncManager.getInstance().sync();
        } else {
            cookieManager.removeAllCookies(value -> cookieManager.flush());
        }

        // Complete the operation with the removed account
        signOutOperation.complete(removeAccount());
        return signOutOperation;
    }

    private synchronized ConnectedDevicesAccount addAccount(String id) {
        Log.i(TAG, "Adding an account.");
        mAccount = new ConnectedDevicesAccount(id, ConnectedDevicesAccountType.AAD);
        return mAccount;
    }

    private synchronized ConnectedDevicesAccount removeAccount() {
        ConnectedDevicesAccount account = mAccount;
        // Only clear the tokens and assign member to null if signed in.
        if (isSignedIn()) {
            Log.i(TAG, "Removing account.");
            mAccount = null;
            mAuthContext.getCache().removeAll();
        }

        return account;
    }

    @Override
    public synchronized AsyncOperation<String> getAccessTokenAsync(final List<String> scopes) {
        if (!isSignedIn()) {
            throw new IllegalStateException("AADSigninHelperAccount: Not currently signed in!");
        }

        final AsyncOperation<String> getAccessTokenOperation = new AsyncOperation<>();
        final String scope = scopes.get(0);

        // acquireTokenSilent(scope, new AcquireTokenHandler(scope, getAccessTokenOperation));
        return getAccessTokenOperation;
    }

    private class AcquireTokenHandler implements AuthenticationCallback<AuthenticationResult> {

        public AcquireTokenHandler(String scope, AsyncOperation<String> getAccessTokenOperation) {
            mScope = scope;
            mGetAccessTokenOperation = getAccessTokenOperation;
        }

        @Override
        public void onError(Exception e) {
            if (!(e instanceof AuthenticationException)) {
                mGetAccessTokenOperation.completeExceptionally(
                        new Exception("AcquireTokenHandler received unknown exception" + e.toString()));
                return;
            }
            if (((AuthenticationException)e).getCode() != ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED) {
                Log.e(TAG, "AcquireTokenHandler hit an exception: " + e.toString() + ". This may be transient.");
                return;
            }

            // This error only returns from acquireTokenSilentAsync when an interactive prompt is needed.
            // ADAL has an MRRT, but the user has not consented for this resource/the MRRT does not cover this resource.
            // Usually, users consent for all resources the app needs during the interactive flow in signIn().
            // However, if the app adds new resources after the user consented previously, signIn() will not prompt.
            // Escalate to the UI thread and do an interactive flow,
            // which should raise a new consent prompt for all current app resources.
            Log.i(TAG, "A resource was requested that the user did not previously consent to. "
                    + "Attempting to raise an interactive consent prompt.");

            final AuthenticationCallback<AuthenticationResult> reusedCallback = this; // reuse this callback
            new Handler(Looper.getMainLooper()).post(new AcquireTokenRunnable(mScope, mGetAccessTokenOperation, reusedCallback));
        }

        @Override
        public void onSuccess(AuthenticationResult result) {
            if (result == null || result.getStatus() != AuthenticationStatus.Succeeded || TextUtils.isEmpty(result.getAccessToken())) {
                mGetAccessTokenOperation.completeExceptionally(
                        new Exception("TRANSIENT_ERROR with error " + result.getStatus().toString()));
            } else {
                mGetAccessTokenOperation.complete(result.getAccessToken());
            }
        }

        private String mScope;
        private AsyncOperation<String> mGetAccessTokenOperation;
    }

    private class AcquireTokenRunnable implements Runnable {

        public AcquireTokenRunnable(
                String scope, AsyncOperation<String> getAccessTokenOperation, AuthenticationCallback<AuthenticationResult> callback) {
            mScope = scope;
            mGetAccessTokenOperation = getAccessTokenOperation;
            mCallback = callback;
        }

        @Override
        public void run() {
            synchronized (AADSigninHelperAccount.this) {
                // Check if still signed in at this point
                if (!isSignedIn()) {
                    Log.e(TAG, "Tried to escalate to interactive prompt, but user was signed out in the middle.");
                    mGetAccessTokenOperation.completeExceptionally(new Exception("TRANSIENT_ERROR"));
                    return;
                }
            }

            acquireToken(mScope, mCallback);
        }

        private String mScope;
        private AsyncOperation<String> mGetAccessTokenOperation;
        private AuthenticationCallback<AuthenticationResult> mCallback;
    }

    private void acquireTokenSilent(String scope, AuthenticationCallback<AuthenticationResult> callback) {
        mAuthContext.acquireTokenSilentAsync(scope, mClientId, mAccount.getId(), callback);
    }

    private void acquireToken(String scope, AuthenticationCallback<AuthenticationResult> callback) {
        mAuthContext.acquireToken(scope, mClientId, mRedirectUri, null, PromptBehavior.Auto, null, callback);
    }
}
