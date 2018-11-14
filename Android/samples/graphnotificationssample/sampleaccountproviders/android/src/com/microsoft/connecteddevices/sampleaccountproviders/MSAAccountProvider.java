//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.connecteddevices.sampleaccountproviders;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Keep;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.microsoft.connecteddevices.core.AccessTokenRequestStatus;
import com.microsoft.connecteddevices.core.AccessTokenResult;
import com.microsoft.connecteddevices.base.AsyncOperation;
import com.microsoft.connecteddevices.base.EventListener;
import com.microsoft.connecteddevices.core.UserAccountProvider;
import com.microsoft.connecteddevices.core.UserAccount;
import com.microsoft.connecteddevices.core.UserAccountType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sample implementation of UserAccountProvider.
 * Exposes a single MSA account, that the user logs into via WebView, to CDP.
 * Follows OAuth2.0 protocol, but automatically refreshes tokens when they are close to expiring.
 *
 * Terms:
 *  - Scope:            OAuth feature, limits what a token actually gives permissions to.
 *                      https://www.oauth.com/oauth2-servers/scope/
 *  - Access token:     A standard JSON web token for a given scope.
 *                      This is the actual token/user ticket used to authenticate with CDP services.
 *                      https://oauth.net/2/
 *                      https://www.oauth.com/oauth2-servers/access-tokens/
 *  - Refresh token:    A standard OAuth refresh token.
 *                      Lasts longer than access tokens, and is used to request new access tokens/refresh access tokens when they expire.
 *                      This library caches one refresh token per user.
 *                      As such, the refresh token must already be authorized/consented to for all CDP scopes that will be used in the app.
 *                      https://oauth.net/2/grant-types/refresh-token/
 *  - Grant type:       Type of OAuth authorization request to make (ie: token, password, auth code)
 *                      https://oauth.net/2/grant-types/
 *  - Auth code:        OAuth auth code, can be exchanged for a token.
 *                      This library has the user sign in interactively for the auth code grant type,
 *                      then retrieves the auth code from the return URL.
 *                      https://oauth.net/2/grant-types/authorization-code/
 *  - Client ID:        ID of an app's registration in the MSA portal. As of the time of writing, the portal uses GUIDs.
 *
 * The flow of this library is described below:
 * Signing in
 *      1. signIn() is called (now treated as signing in)
 *      2. webview is presented to the user for sign in
 *      3. Use authcode returned from user's sign in to fetch refresh token
 *      4. Refresh token is cached - if the user does not sign out, but the app is restarted,
 *         the user will not need to enter their credentials/consent again when signIn() is called.
 *      4. Now treated as signed in. Account is exposed to CDP. UserAccountChangedEvent is fired.
 *
 * While signed in
 *      CDP asks for access tokens
 *          1. Check if access token is in cache
 *          2. If not in cache, request a new access token using the cached refresh token.
 *          3. If in cache but close to expiry, the access token is refreshed using the refresh token.
 *             The refreshed access token is returned.
 *          4. If in cache and not close to expiry, just return it.
 *
 * Signing out
 *      1. signOut() is called
 *      2. webview is quickly popped up to go through the sign out URL
 *      3. Cache is cleared.
 *      4. Now treated as signed out. Account is no longer exposed to CDP. UserAccountChangedEvent is fired.
 */
@Keep
public final class MSAAccountProvider implements UserAccountProvider, MSATokenCache.Listener {

    // region Constants
    private static final String TAG = MSAAccountProvider.class.getName();

    // CDP's SDK currently requires authorization for all features, otherwise platform initialization will fail.
    // As such, the user must sign in/consent for the following scopes. This may change to become more modular in the future.
    private static final String[] KNOWN_SCOPES = {
        "wl.offline_access",                                                // read and update user info at any time
        "ccs.ReadWrite",                                                    // device commanding scope
        "wns.connect",                                                      // push notification scope
        "https://activity.windows.com/UserActivity.ReadWrite.CreatedByApp", // default userdata.useractivities scope
    };

    // OAuth URLs
    private static final String REDIRECT_URL = "https://login.live.com/oauth20_desktop.srf";
    private static final String AUTHORIZE_URL = "https://login.live.com/oauth20_authorize.srf";
    private static final String LOGOUT_URL = "https://login.live.com/oauth20_logout.srf";
    // endregion

    // region Member Variables
    private final String mClientId;
    private final Map<String, String[]> mScopeOverrideMap;
    private UserAccount mAccount = null;
    private MSATokenCache mTokenCache;
    private boolean mSignInSignOutInProgress;

    private final Map<Long, EventListener<UserAccountProvider, Void>> mListenerMap = new ArrayMap<>();
    private long mNextListenerId = 1L;
    // endregion

    // region Constructor
    /**
     * @param clientId           id of the app's registration in the MSA portal
     * @param scopeOverrides     scope overrides for the app
     * @param context
     */
    public MSAAccountProvider(String clientId, final Map<String, String[]> scopeOverrides, Context context) {
        mClientId = clientId;
        mScopeOverrideMap = scopeOverrides;
        mTokenCache = new MSATokenCache(clientId, context);
        mTokenCache.addListener(new MSATokenCache.Listener() {
            @Override
            public void onTokenCachePermanentFailure() {
                onAccessTokenError((mAccount != null ? mAccount.getId() : null), null, true);
            }
        });

        if (mTokenCache.loadSavedRefreshToken()) {
            Log.i(TAG, "Loaded previous session for MSAAccountProvider. Starting as signed in.");
            mAccount = new UserAccount(UUID.randomUUID().toString(), UserAccountType.MSA);
        } else {
            Log.i(TAG, "No previous session could be loaded for MSAAccountProvider. Starting as signed out.");
        }
    }
    // endregion

    // region Private Helpers
    private List<String> getAuthScopes(final String[] incoming) {
        ArrayList<String> authScopes = new ArrayList<String>();

        for (String scope : incoming) {
            if (mScopeOverrideMap.containsKey(scope)) {
                for (String replacement : mScopeOverrideMap.get(scope)) {
                    authScopes.add(replacement);
                }
            } else {
                authScopes.add(scope);
            }
        }

        return authScopes;
    }

    private AsyncOperation<Void> notifyListenersAsync() {
        return AsyncOperation.supplyAsync(new AsyncOperation.Supplier<Void>() {
            @Override
            public Void get() {
                for (EventListener<UserAccountProvider, Void> listener : mListenerMap.values()) {
                    listener.onEvent(MSAAccountProvider.this, null);
                }
                return null;
            }
        });
    }

    private synchronized void addAccount() {
        Log.i(TAG, "Adding an account.");
        mAccount = new UserAccount(UUID.randomUUID().toString(), UserAccountType.MSA);
        notifyListenersAsync();
    }

    private synchronized void removeAccount() {
        if (isSignedIn()) {
            Log.i(TAG, "Removing account.");
            mAccount = null;
            mTokenCache.clearTokens();
            notifyListenersAsync();
        }
    }

    /**
     * Asynchronously requests a new access token for the provided scope(s) and caches it.
     * This assumes that the sign in helper is currently signed in.
     */
    private AsyncOperation<AccessTokenResult> requestNewAccessTokenAsync(final String scope) {
        // Need the refresh token first, then can use it to request an access token
        return mTokenCache.getRefreshTokenAsync()
            .thenComposeAsync(new AsyncOperation.ResultFunction<String, AsyncOperation<MSATokenRequest.Result>>() {
                @Override
                public AsyncOperation<MSATokenRequest.Result> apply(String refreshToken) {
                    return MSATokenRequest.requestAsync(mClientId, MSATokenRequest.GrantType.REFRESH, scope, null, refreshToken);
                }
            })
            .thenApplyAsync(new AsyncOperation.ResultFunction<MSATokenRequest.Result, AccessTokenResult>() {
                @Override
                public AccessTokenResult apply(MSATokenRequest.Result result) throws Throwable {
                    switch (result.getStatus()) {
                    case SUCCESS:
                        Log.i(TAG, "Successfully fetched access token.");
                        mTokenCache.setAccessToken(result.getAccessToken(), scope, result.getExpiresIn());
                        return new AccessTokenResult(AccessTokenRequestStatus.SUCCESS, result.getAccessToken());

                    case TRANSIENT_FAILURE:
                        Log.e(TAG, "Requesting new access token failed temporarily, please try again.");
                        return new AccessTokenResult(AccessTokenRequestStatus.TRANSIENT_ERROR, null);

                    default: // PERMANENT_FAILURE
                        Log.e(TAG, "Permanent error occurred while fetching access token.");
                        onAccessTokenError(mAccount.getId(), new String[] { scope }, true);
                        throw new IOException("Permanent error occurred while fetching access token.");
                    }
                }
            });
    }
    // endregion

    public String getClientId() {
        return mClientId;
    }

    // region Interactive Sign-in/out
    public synchronized boolean isSignedIn() {
        return mAccount != null;
    }

    /**
     * Pops up a webview for the user to sign in with their MSA, then uses the auth code returned to cache a refresh token for the user.
     * If a refresh token was already cached from a previous session, it will be used instead, and no webview will be displayed.
     */
    public synchronized AsyncOperation<Boolean> signIn(final Activity currentActivity) throws IllegalStateException {
        if (isSignedIn() || mSignInSignOutInProgress) {
            throw new IllegalStateException();
        }

        final String signInUrl = AUTHORIZE_URL + "?redirect_uri=" + REDIRECT_URL + "&response_type=code&client_id=" + mClientId +
                                 "&scope=" + TextUtils.join("+", getAuthScopes(KNOWN_SCOPES));
        final AsyncOperation<String> authCodeOperation = new AsyncOperation<>();
        final AsyncOperation<Boolean> signInOperation = new AsyncOperation<>();
        mSignInSignOutInProgress = true;

        final Dialog dialog = new Dialog(currentActivity);
        dialog.setContentView(R.layout.auth_dialog);
        final WebView web = (WebView)dialog.findViewById(R.id.webv);
        web.setWebChromeClient(new WebChromeClient());
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);

        web.loadUrl(signInUrl);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            web.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    onSignInPageFinishedInternal(url, dialog, authCodeOperation, signInOperation);
                }

                @Override
                @TargetApi(23)
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    super.onReceivedError(view, request, error);
                    onReceivedSignInErrorInternal(error.getDescription().toString(), authCodeOperation, signInOperation);
                }
            });
        } else {
            web.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    onSignInPageFinishedInternal(url, dialog, authCodeOperation, signInOperation);
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    super.onReceivedError(view, errorCode, description, failingUrl);
                    onReceivedSignInErrorInternal(description, authCodeOperation, signInOperation);
                }
            });
        }

        authCodeOperation // chain after successfully fetching the authcode (does not execute if authCodeOperation completed exceptionally)
            .thenComposeAsync(new AsyncOperation.ResultFunction<String, AsyncOperation<MSATokenRequest.Result>>() {
                @Override
                public AsyncOperation<MSATokenRequest.Result> apply(String authCode) {
                    return MSATokenRequest.requestAsync(mClientId, MSATokenRequest.GrantType.CODE, null, REDIRECT_URL, authCode);
                }
            })
            .thenAcceptAsync(new AsyncOperation.ResultConsumer<MSATokenRequest.Result>() {
                @Override
                public void accept(MSATokenRequest.Result result) {
                    synchronized (MSAAccountProvider.this) {
                        mSignInSignOutInProgress = false;
                    }

                    if (result.getStatus() == MSATokenRequest.Result.Status.SUCCESS) {
                        if (result.getRefreshToken() == null) {
                            Log.e(TAG, "Unexpected: refresh token is null despite succeeding in refresh.");
                            signInOperation.complete(false);
                        }

                        Log.i(TAG, "Successfully fetched refresh token.");
                        mTokenCache.setRefreshToken(result.getRefreshToken());
                        addAccount();
                        signInOperation.complete(true);

                    } else {
                        Log.e(TAG, "Failed to fetch refresh token using auth code.");
                        signInOperation.complete(false);
                    }
                }
            });

        dialog.show();
        dialog.setCancelable(true);

        return signInOperation;
    }

    /**
     * Signs the user out by going through the webview, then clears the cache and current state.
     */
    public synchronized void signOut(final Activity currentActivity) throws IllegalStateException {
        final String signOutUrl = LOGOUT_URL + "?client_id=" + mClientId + "&redirect_uri=" + REDIRECT_URL;
        mSignInSignOutInProgress = true;

        final Dialog dialog = new Dialog(currentActivity);
        dialog.setContentView(R.layout.auth_dialog);
        WebView web = (WebView)dialog.findViewById(R.id.webv);

        web.loadUrl(signOutUrl);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            web.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    onSignOutPageFinishedInternal(url, dialog);
                }

                @Override
                @TargetApi(23)
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    super.onReceivedError(view, request, error);
                    onReceivedSignOutErrorInternal(error.getDescription().toString());
                }
            });
        } else {
            web.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    onSignOutPageFinishedInternal(url, dialog);
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    super.onReceivedError(view, errorCode, description, failingUrl);
                    onReceivedSignOutErrorInternal(description);
                }
            });
        }
    }
    // endregion

    // region UserAccountProvider Overrides
    @Override
    public synchronized UserAccount[] getUserAccounts() {
        if (mAccount != null) {
            return new UserAccount[] { mAccount };
        }

        return new UserAccount[0];
    }

    @Override
    public synchronized AsyncOperation<AccessTokenResult> getAccessTokenForUserAccountAsync(final String accountId, final String[] scopes) {
        if (mAccount != null && accountId != null && accountId.equals(mAccount.getId()) && scopes.length > 0) {

            final String scope = TextUtils.join(" ", getAuthScopes(scopes));

            return mTokenCache.getAccessTokenAsync(scope).thenComposeAsync(
                new AsyncOperation.ResultFunction<String, AsyncOperation<AccessTokenResult>>() {
                    @Override
                    public AsyncOperation<AccessTokenResult> apply(String accessToken) {
                        if (accessToken != null) {
                            // token already exists in the cache, can early return
                            return AsyncOperation.completedFuture(new AccessTokenResult(AccessTokenRequestStatus.SUCCESS, accessToken));
                        } else {
                            // token does not yet exist in the cache, need to request a new one
                            return requestNewAccessTokenAsync(scope);
                        }
                    }
                });
        }

        // No access token is available
        return AsyncOperation.completedFuture(new AccessTokenResult(AccessTokenRequestStatus.TRANSIENT_ERROR, null));
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
    public synchronized void onAccessTokenError(String accountId, String[] scopes, boolean isPermanentError) {
        if (isPermanentError) {
            removeAccount();
        } else {
            mTokenCache.markAllTokensExpired();
        }
    }
    // endregion

    // region MSATokenCacheListener Overrides
    @Override
    public void onTokenCachePermanentFailure() {
        onAccessTokenError(null, null, true);
    }
    // endregion

    // region  Internal Helpers

    private void onSignInPageFinishedInternal(
        String url, Dialog dialog, AsyncOperation<String> authCodeOperation, AsyncOperation<Boolean> signInOperation) {
        if (url.startsWith(REDIRECT_URL)) {
            final Uri uri = Uri.parse(url);
            final String code = uri.getQueryParameter("code");
            final String error = uri.getQueryParameter("error");

            dialog.dismiss();

            if ((error != null) || (code == null) || (code.length() <= 0)) {
                synchronized (MSAAccountProvider.this) {
                    mSignInSignOutInProgress = false;
                }

                signInOperation.complete(false);
                authCodeOperation.completeExceptionally(
                    new Exception((error != null) ? error : "Failed to authenticate with unknown error"));
            } else {
                authCodeOperation.complete(code);
            }
        }
    }

    private void onReceivedSignInErrorInternal(
        String errorString, AsyncOperation<String> authCodeOperation, AsyncOperation<Boolean> signInOperation) {
        Log.e(TAG, "Encountered web resource loading error while signing in: \'" + errorString + "\'");
        synchronized (MSAAccountProvider.this) {
            mSignInSignOutInProgress = false;
        }

        signInOperation.complete(false);
        authCodeOperation.completeExceptionally(new Exception(errorString));
    }

    public void onSignOutPageFinishedInternal(String url, Dialog dialog) {
        if (!url.contains("oauth20_desktop.srf")) {
            // finishing off loading intermediate pages,
            // e.g., input username/password page, consent interrupt page, wrong username/password page etc.
            // no need to handle them, return early.
            return;
        }

        synchronized (MSAAccountProvider.this) {
            mSignInSignOutInProgress = false;
        }

        final Uri uri = Uri.parse(url);
        final String error = uri.getQueryParameter("error");
        if (error != null) {
            Log.e(TAG, "Signed out failed with error: " + error);
        }

        removeAccount();
        dialog.dismiss();
    }

    public void onReceivedSignOutErrorInternal(String errorString) {
        Log.e(TAG, "Encountered web resource loading error while signing out: \'" + errorString + "\'");
        synchronized (MSAAccountProvider.this) {
            mSignInSignOutInProgress = false;
        }
    }

    // endregion
}
