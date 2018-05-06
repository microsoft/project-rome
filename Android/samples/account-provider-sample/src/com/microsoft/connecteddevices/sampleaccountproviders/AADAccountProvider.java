//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.connecteddevices.sampleaccountproviders;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.ValueCallback;

import com.microsoft.aad.adal.ADALError;
import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationException;
import com.microsoft.aad.adal.AuthenticationResult;
import com.microsoft.aad.adal.AuthenticationResult.AuthenticationStatus;
import com.microsoft.aad.adal.PromptBehavior;
import com.microsoft.aad.adal.TokenCacheItem;

import com.microsoft.connecteddevices.core.AccessTokenRequestStatus;
import com.microsoft.connecteddevices.core.AccessTokenResult;
import com.microsoft.connecteddevices.base.AsyncOperation;
import com.microsoft.connecteddevices.base.EventListener;
import com.microsoft.connecteddevices.core.UserAccountProvider;
import com.microsoft.connecteddevices.core.UserAccount;
import com.microsoft.connecteddevices.core.UserAccountType;

import java.lang.InterruptedException;
import java.util.Iterator;
import java.util.Map;

/**
 * Sign in helper that provides an UserAccountProvider implementation for AAD using the ADAL library.
 * To use this class, call signIn()/signOut(), then use the standard UserAccountProvider functions.
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
public final class AADAccountProvider implements UserAccountProvider {
    private static final String TAG = AADAccountProvider.class.getName();

    private final String mClientId;
    private final String mRedirectUri;
    private final AuthenticationContext mAuthContext;

    private UserAccount mAccount; // Initialized when signed in

    private final Map<Long, EventListener<UserAccountProvider, Void>> mListenerMap = new ArrayMap<>();
    private long mNextListenerId = 1L;

    /**
     * @param clientId              id of the app's registration in the Azure portal
     * @param redirectUri           redirect uri the app is registered with in the Azure portal
     * @param context
     */
    public AADAccountProvider(String clientId, String redirectUri, Context context) {
        mClientId = clientId;
        mRedirectUri = redirectUri;

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(context);
        }

        mAuthContext = new AuthenticationContext(context, "https://login.microsoftonline.com/common", false);

        Log.i(TAG, "Checking if previous AADAccountProvider session can be loaded...");
        Iterator<TokenCacheItem> tokenCacheItems = mAuthContext.getCache().getAll();
        while (tokenCacheItems.hasNext()) {
            TokenCacheItem item = tokenCacheItems.next();
            if (item.getIsMultiResourceRefreshToken() && item.getClientId().equals(mClientId)) {
                mAccount = new UserAccount(item.getUserInfo().getUserId(), UserAccountType.AAD);
                break;
            }
        }

        if (mAccount != null) {
            Log.i(TAG, "Loaded previous AADAccountProvider session, starting as signed in.");
        } else {
            Log.i(TAG, "No previous AADAccountProvider session could be loaded, starting as signed out.");
        }
    }

    private AsyncOperation<Void> notifyListenersAsync() {
        return AsyncOperation.supplyAsync(new AsyncOperation.Supplier<Void>() {
            @Override
            public Void get() {
                for (EventListener<UserAccountProvider, Void> listener : mListenerMap.values()) {
                    listener.onEvent(AADAccountProvider.this, null);
                }
                return null;
            }
        });
    }

    public String getClientId() {
        return mClientId;
    }

    public synchronized boolean isSignedIn() {
        return mAccount != null;
    }

    public synchronized AsyncOperation<Boolean> signIn() throws IllegalStateException {
        if (isSignedIn()) {
            throw new IllegalStateException("AADAccountProvider: Already signed in!");
        }

        final AsyncOperation<Boolean> ret = new AsyncOperation<>();

        // If the user has not previously consented for this default resource for this app,
        // the interactive flow will ask for user consent for all resources used by the app.
        // If the user previously consented to this resource on this app, and more resources are added to the app later on,
        // a consent prompt for all app resources will be raised when an access token for a new resource is requested -
        // see getAccessTokenForUserAccountAsync()
        final String defaultResource = "https://graph.windows.net";

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
                    ret.complete(false);
                }

                @Override
                public void onSuccess(AuthenticationResult result) {
                    if (result == null || result.getStatus() != AuthenticationStatus.Succeeded || result.getUserInfo() == null) {
                        ret.complete(false);
                    } else {
                        mAccount = new UserAccount(result.getUserInfo().getUserId(), UserAccountType.AAD);
                        ret.complete(true);
                        notifyListenersAsync();
                    }
                }
            });

        return ret;
    }

    public synchronized void signOut() throws IllegalStateException {
        if (!isSignedIn()) {
            throw new IllegalStateException("AADAccountProvider: Not currently signed in!");
        }

        // Delete cookies
        final CookieManager cookieManager = CookieManager.getInstance();

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookie();
            CookieSyncManager.getInstance().sync();
        } else {
            cookieManager.removeAllCookies(new ValueCallback<Boolean>() {
                @Override
                public void onReceiveValue(Boolean value) {
                    cookieManager.flush();
                }
            });
        }

        mAccount = null;
        mAuthContext.getCache().removeAll();
        notifyListenersAsync();
    }

    @Override
    public synchronized UserAccount[] getUserAccounts() {
        if (mAccount != null) {
            return new UserAccount[] { mAccount };
        }

        return new UserAccount[0];
    }

    @Override
    public synchronized AsyncOperation<AccessTokenResult> getAccessTokenForUserAccountAsync(
        final String userAccountId, final String[] scopes) {
        if (mAccount == null || !mAccount.getId().equals(userAccountId)) {
            return AsyncOperation.completedFuture(new AccessTokenResult(AccessTokenRequestStatus.TRANSIENT_ERROR, null));
        }

        final AsyncOperation<AccessTokenResult> ret = new AsyncOperation<>();
        mAuthContext.acquireTokenSilentAsync(scopes[0], mClientId, mAccount.getId(), new AuthenticationCallback<AuthenticationResult>() {
            @Override
            public void onError(Exception e) {
                if ((e instanceof AuthenticationException) &&
                    ((AuthenticationException)e).getCode() == ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED) {
                    // This error only returns from acquireTokenSilentAsync when an interactive prompt is needed.
                    // ADAL has an MRRT, but the user has not consented for this resource/the MRRT does not cover this resource.
                    // Usually, users consent for all resources the app needs during the interactive flow in signIn().
                    // However, if the app adds new resources after the user consented previously, signIn() will not prompt.
                    // Escalate to the UI thread and do an interactive flow,
                    // which should raise a new consent prompt for all current app resources.
                    Log.i(TAG, "A resource was requested that the user did not previously consent to. "
                                   + "Attempting to raise an interactive consent prompt.");

                    final AuthenticationCallback<AuthenticationResult> reusedCallback = this; // reuse this callback
                    new Handler(Looper.getMainLooper())
                        .post(new Runnable() {
                            @Override
                            public void run() {
                                mAuthContext.acquireToken(
                                    scopes[0], mClientId, mRedirectUri, null, PromptBehavior.Auto, null, reusedCallback);
                            }
                        });
                    return;
                }

                Log.e(TAG, "getAccessTokenForUserAccountAsync hit an exception: " + e.toString() + ". This may be transient.");
                ret.complete(new AccessTokenResult(AccessTokenRequestStatus.TRANSIENT_ERROR, null));
            }

            @Override
            public void onSuccess(AuthenticationResult result) {
                if (result == null || result.getStatus() != AuthenticationStatus.Succeeded || TextUtils.isEmpty(result.getAccessToken())) {

                    ret.complete(new AccessTokenResult(AccessTokenRequestStatus.TRANSIENT_ERROR, null));
                } else {
                    ret.complete(new AccessTokenResult(AccessTokenRequestStatus.SUCCESS, result.getAccessToken()));
                }
            }
        });

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
        if (mAccount != null && mAccount.getId().equals(userAccountId)) {
            if (isPermanentError) {
                try {
                    signOut();
                } catch (IllegalStateException e) {
                    // Already signed out in between checking if signed in and now. No need to do anything.
                    Log.e(TAG, "Already signed out in onAccessTokenError. This error is most likely benign: " + e.toString());
                }
            } else {
                // If not a permanent error, try to refresh the tokens
                try {
                    mAuthContext.acquireTokenSilentSync(scopes[0], mClientId, userAccountId);
                } catch (AuthenticationException e) {
                    Log.e(TAG, "Exception in ADAL when trying to refresh token: \'" + e.toString() + "\'");
                } catch (InterruptedException e) { Log.e(TAG, "Interrupted while trying to refresh token: \'" + e.toString() + "\'"); }
            }
        } else {
            Log.e(TAG, "onAccessTokenError was called, but AADAccountProvider was not signed in.");
        }
    }
}