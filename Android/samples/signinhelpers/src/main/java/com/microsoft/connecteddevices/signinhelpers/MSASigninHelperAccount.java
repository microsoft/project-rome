//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.connecteddevices.signinhelpers;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Keep;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.microsoft.connecteddevices.AsyncOperation;
import com.microsoft.connecteddevices.ConnectedDevicesAccount;
import com.microsoft.connecteddevices.ConnectedDevicesAccountType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sample implementation of integration with the ConnectedDevicesAccountManager.
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
 *      4. Now treated as signed in. ConnectedDevicesAccount is exposed to CDP. ConnectedDevicesAccountChangedEvent is fired.
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
 *      4. Now treated as signed out. ConnectedDevicesAccount is no longer exposed to CDP. ConnectedDevicesAccountChangedEvent is fired.
 */
@Keep
public final class MSASigninHelperAccount implements SigninHelperAccount {

    // region Constants
    private static final String TAG = MSASigninHelperAccount.class.getName();

    // CDP's SDK currently requires authorization for all features, otherwise platform initialization will fail.
    // As such, the user must sign in/consent for the following scopes. This may change to become more modular in the future.
    private static final List<String> KNOWN_SCOPES = Arrays.asList("wl.offline_access", // read and update user info at any time
            "ccs.ReadWrite",                                                                // device commanding scope
            "dds.read",                                                                     // device discovery scope (discover other devices)
            "dds.register",                                                    // device discovery scope (allow discovering this device)
            "wns.connect",                                                     // push notification scope
            "asimovrome.telemetry",                                            // asimov token scope
            "https://activity.windows.com/UserActivity.ReadWrite.CreatedByApp" // default useractivities scope
            );

    // OAuth URLs
    private static final String REDIRECT_URL = "https://login.live.com/oauth20_desktop.srf";
    private static final String AUTHORIZE_URL = "https://login.live.com/oauth20_authorize.srf";
    private static final String LOGOUT_URL = "https://login.live.com/oauth20_logout.srf";
    // endregion

    // region Member Variables
    private final String mClientId;
    private final Map<String, String[]> mScopeOverrideMap;
    private ConnectedDevicesAccount mAccount;
    private MSATokenCache mTokenCache;

    private long mNextListenerId = 1L;
    // endregion

    // region Constructor
    /**
     * @param clientId           id of the app's registration in the MSA portal
     * @param scopeOverrides     scope overrides for the app
     * @param context
     */
    public MSASigninHelperAccount(String clientId, final Map<String, String[]> scopeOverrides, Context context) {
        mClientId = clientId;
        mScopeOverrideMap = scopeOverrides;
        mTokenCache = new MSATokenCache(clientId, context);
        mTokenCache.addListener(new MSATokenCache.Listener() {
            @Override
            public void onTokenCachePermanentFailure() {
                Log.e(TAG, "MSA Token Cache has hit a failure. The next login will require credentials.");
            }
        });

        if (mTokenCache.loadSavedRefreshToken()) {
            // Note: It is important to provide the correct account ID for the signed in account in the current session.
            String id = mTokenCache.readSavedAccountId();
            if (!id.isEmpty()) {
                Log.i(TAG, "Loaded previous session for MSASigninHelperAccount: " + id + ". Starting as signed in.");
                mAccount = new ConnectedDevicesAccount(id, ConnectedDevicesAccountType.MSA);
            } else {
                Log.w(TAG, "There exists a previous session of this app, however no ID exists. This is likely an upgrade of an older MSASigninHelperAccount version.");
            }
        } else {
            Log.i(TAG, "No previous session could be loaded for MSASigninHelperAccount. Starting as signed out.");
        }
    }
    // endregion

    // region Overrides
    /**
     * Pops up a webview for the user to sign in with their MSA, then uses the auth code returned to cache a refresh token for the user.
     * If a refresh token was already cached from a previous session, it will be used instead, and no webview will be displayed.
     */
    @Override
    public synchronized AsyncOperation<ConnectedDevicesAccount> signIn(final Activity currentActivity) throws IllegalStateException {
        if (isSignedIn()) {
            return AsyncOperation.completedFuture(mAccount);
        }

        final String signInUrl = AUTHORIZE_URL + "?redirect_uri=" + REDIRECT_URL + "&response_type=code&client_id=" + mClientId + "&scope=" + TextUtils.join("+", getAuthScopes(KNOWN_SCOPES));
        final AsyncOperation<String> authCodeOperation = new AsyncOperation<>();
        final AsyncOperation<ConnectedDevicesAccount> signInOperation = new AsyncOperation<>();

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
                    onSignInPageFinishedInternal(url, dialog, authCodeOperation);
                }

                @Override
                @TargetApi(23)
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    super.onReceivedError(view, request, error);
                    onReceivedSignInErrorInternal(error.getDescription().toString(), authCodeOperation);
                }
            });
        } else {
            web.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    onSignInPageFinishedInternal(url, dialog, authCodeOperation);
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    super.onReceivedError(view, errorCode, description, failingUrl);
                    onReceivedSignInErrorInternal(description, authCodeOperation);
                }
            });
        }

        authCodeOperation // chain after successfully fetching the authcode (does not execute if authCodeOperation completed exceptionally)
                .thenComposeAsync((AsyncOperation.ResultFunction<String, AsyncOperation<MSATokenRequest.Result>>) authCode -> MSATokenRequest.requestAsync(mClientId, MSATokenRequest.GrantType.CODE, null, REDIRECT_URL, authCode))
                .thenAcceptAsync((AsyncOperation.ResultConsumer<MSATokenRequest.Result>) result -> {
                    if (result.getStatus() == MSATokenRequest.Result.Status.SUCCESS) {
                        if (result.getRefreshToken() == null) {
                            signInOperation.completeExceptionally(
                                    new Exception("Unexpected: refresh token is null despite succeeding in refresh."));
                        }

                        Log.i(TAG, "Successfully fetched refresh token.");
                        // Persist the refresh token
                        mTokenCache.setRefreshToken(result.getRefreshToken());
                        // Generate an ID for the Account
                        String id = UUID.randomUUID().toString();
                        // Persist the Accounts ID so we can create an Account object with a matching ID
                        mTokenCache.saveAccountId(id);
                        // Complete the operation with the newly created account
                        signInOperation.complete(addAccount(id));
                    } else {
                        Log.e(TAG, "Failed to fetch refresh token using auth code.");
                        signInOperation.completeExceptionally(new Exception("Failed to fetch refresh token using auth code."));
                    }
                })
                .exceptionally(throwable -> {
                    signInOperation.completeExceptionally(throwable);
                    return null;
                });

        dialog.show();
        dialog.setCancelable(true);

        return signInOperation;
    }

    /**
     * Signs the user out by going through the webview, then clears the cache and current state.
     */
    @Override
    public synchronized AsyncOperation<ConnectedDevicesAccount> signOut(final Activity currentActivity) throws IllegalStateException {
        final String signOutUrl = LOGOUT_URL + "?client_id=" + mClientId + "&redirect_uri=" + REDIRECT_URL;

        final AsyncOperation<ConnectedDevicesAccount> signOutOperation = new AsyncOperation<>();
        final Dialog dialog = new Dialog(currentActivity);
        dialog.setContentView(R.layout.auth_dialog);
        WebView web = (WebView)dialog.findViewById(R.id.webv);

        web.loadUrl(signOutUrl);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            web.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    onSignOutPageFinishedInternal(url, dialog, signOutOperation);
                }

                @Override
                @TargetApi(23)
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    super.onReceivedError(view, request, error);
                    onReceivedSignOutErrorInternal(error.getDescription().toString(), signOutOperation);
                }
            });
        } else {
            web.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    onSignOutPageFinishedInternal(url, dialog, signOutOperation);
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    super.onReceivedError(view, errorCode, description, failingUrl);
                    onReceivedSignOutErrorInternal(description, signOutOperation);
                }
            });
        }

        return signOutOperation;
    }

    @Override
    public synchronized AsyncOperation<String> getAccessTokenAsync(final List<String> scopes) {
        final String scope = TextUtils.join(" ", getAuthScopes(scopes));

        return mTokenCache.getAccessTokenAsync(scope).thenComposeAsync((AsyncOperation.ResultFunction<String, AsyncOperation<String>>) accessToken -> {
            if (accessToken != null) {
                // Token already exists in the cache, can early return
                return AsyncOperation.completedFuture(accessToken);
            } else {
                // Token does not yet exist in the cache, need to request a new one
                return requestNewAccessTokenAsync(scope);
            }
        });
    }

    @Override
    public synchronized boolean isSignedIn() {
        return mAccount != null;
    }

    @Override
    public synchronized ConnectedDevicesAccount getAccount() {
        return mAccount;
    }
    // endregion

    // region Public Instance Methods
    public String getClientId() {
        return mClientId;
    }

    public void onSignOutPageFinishedInternal(String url, Dialog dialog, AsyncOperation<ConnectedDevicesAccount> signOutOperation) {
        if (!url.contains("oauth20_desktop.srf")) {
            // finishing off loading intermediate pages,
            // e.g., input username/password page, consent interrupt page, wrong username/password page etc.
            // no need to handle them, return early.
            return;
        }

        final Uri uri = Uri.parse(url);
        final String error = uri.getQueryParameter("error");
        if (error != null) {
            signOutOperation.completeExceptionally(new Exception("Signed out failed with error: " + error));
        } else {
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
        }

        dialog.dismiss();
    }

    public void onReceivedSignOutErrorInternal(String errorString, AsyncOperation<ConnectedDevicesAccount> signOutOperation) {
        Log.e(TAG, "Encountered web resource loading error while signing out: \'" + errorString + "\'");
        signOutOperation.completeExceptionally(new Exception(errorString));
    }
    // endregion

    // region Private Instance Methods
    private void onSignInPageFinishedInternal(String url, Dialog dialog, AsyncOperation<String> authCodeOperation) {
        if (url.startsWith(REDIRECT_URL)) {
            final Uri uri = Uri.parse(url);
            final String code = uri.getQueryParameter("code");
            final String error = uri.getQueryParameter("error");

            dialog.dismiss();

            if ((error != null) || (code == null) || (code.length() <= 0)) {
                authCodeOperation.completeExceptionally(
                        new Exception((error != null) ? error : "Failed to authenticate with unknown error"));
            } else {
                authCodeOperation.complete(code);
            }
        }
    }

    private void onReceivedSignInErrorInternal(String errorString, AsyncOperation<String> authCodeOperation) {
        Log.e(TAG, "Encountered web resource loading error while signing in: \'" + errorString + "\'");
        authCodeOperation.completeExceptionally(new Exception(errorString));
    }

    private List<String> getAuthScopes(final List<String> incoming) {
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

    private synchronized ConnectedDevicesAccount addAccount(String id) {
        Log.i(TAG, "Adding an account with ID: " + id);
        mAccount = new ConnectedDevicesAccount(id, ConnectedDevicesAccountType.MSA);
        return mAccount;
    }

    private synchronized ConnectedDevicesAccount removeAccount() {
        ConnectedDevicesAccount account = mAccount;
        // Only clear the tokens and assign member to null if signed in.
        if (isSignedIn()) {
            Log.i(TAG, "Removing account.");
            mAccount = null;
            // We clear all tokens we assume we only have 1 account added.
            mTokenCache.clearTokens();
        }

        return account;
    }

    /**
     * Asynchronously requests a new access token for the provided scope(s) and caches it.
     * This assumes that the sign in helper is currently signed in.
     */
    private AsyncOperation<String> requestNewAccessTokenAsync(final String scope) {
        // Need the refresh token first, then can use it to request an access token
        return mTokenCache.getRefreshTokenAsync()
                .thenComposeAsync((AsyncOperation.ResultFunction<String, AsyncOperation<MSATokenRequest.Result>>) refreshToken -> MSATokenRequest.requestAsync(mClientId, MSATokenRequest.GrantType.REFRESH, scope, null, refreshToken))
                .thenApplyAsync((AsyncOperation.ResultFunction<MSATokenRequest.Result, String>) result -> {
                    switch (result.getStatus()) {
                        case SUCCESS:
                            Log.i(TAG, "Successfully fetched access token.");
                            String token = result.getAccessToken();
                            mTokenCache.setAccessToken(token, scope, result.getExpiresIn());
                            return token;
                        case TRANSIENT_FAILURE: throw new IOException("Requesting new access token failed temporarily, please try again.");
                        default: // PERMANENT_FAILURE
                            throw new IOException("Permanent error occurred while fetching access token.");
                    }
                });
    }
    // endregion
}
