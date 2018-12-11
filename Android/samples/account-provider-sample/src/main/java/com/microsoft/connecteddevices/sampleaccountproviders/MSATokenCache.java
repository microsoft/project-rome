//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.connecteddevices.signinhelpers;

import android.content.Context;
import android.support.annotation.Keep;
import android.util.ArrayMap;
import android.util.Log;
import android.content.SharedPreferences;

import com.microsoft.connecteddevices.AsyncOperation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Caches MSA access and refresh tokens, automatically refreshing them as needed when fetching from the cache.
 * Cached refresh tokens are persisted across sessions.
 */
@Keep
final class MSATokenCache {
    private static final String TAG = MSATokenCache.class.getName();

    // Max number of times to try to refresh a token through transient failures
    private static final int TOKEN_REFRESH_MAX_RETRIES = 3;

    // How quickly to retry refreshing a token when encountering a transient failure
    private static final long MSA_REFRESH_TOKEN_RETRY_SECONDS = 30 * 60; // 30 minutes
    private static final long MSA_ACCESS_TOKEN_RETRY_SECONDS = 3 * 60;   // 3 minutes

    // How long it takes a refresh token to expire
    private static final int MSA_REFRESH_TOKEN_EXPIRATION_SECONDS = 10 * 24 * 60 * 60; // 10 days

    // How long before expiry to consider a token in need of a refresh.
    // (MSA_REFRESH_TOKEN_CLOSE_TO_EXPIRY_SECONDS is intended to be aggressive and keep the refresh token relatively far from expiry)
    private static final int MSA_REFRESH_TOKEN_CLOSE_TO_EXPIRY_SECONDS = 7 * 24 * 60 * 60; // 7 days
    private static final int MSA_ACCESS_TOKEN_CLOSE_TO_EXPIRY_SECONDS = 5 * 60;            // 5 minutes

    private static final String MSA_OFFLINE_ACCESS_SCOPE = "wl.offline_access";
    private static final String GUID_ID_KEY = "GUID_ID_KEY";

    private static final ScheduledExecutorService sRetryExecutor = Executors.newSingleThreadScheduledExecutor();

    /**
     * Helper function. Returns a Date n seconds from now.
     */
    private static final Date getDateSecondsAfterNow(int seconds) {
        return getDateSecondsAfter(null, seconds);
    }

    /**
     * Helper function. Returns a Date n seconds after date.
     */
    private static final Date getDateSecondsAfter(Date date, int seconds) {
        Calendar calendar = Calendar.getInstance(); // sets time to current
        if (date != null) {
            calendar.setTime(date);
        }
        calendar.add(Calendar.SECOND, seconds);
        return calendar.getTime();
    }

    /**
     * Private helper class wrapping a cached access token. Responsible for refreshing it on-demand.
     */
    private class MSATokenCacheItem {
        protected String mToken;
        protected Date mCloseToExpirationDate; // Actual expiration date is used less than this, so just cache this instead
        protected final MSATokenRequest mRefreshRequest;

        public MSATokenCacheItem(String token, int expiresInSeconds, MSATokenRequest refreshRequest) {
            mToken = token;
            mCloseToExpirationDate = getDateSecondsAfterNow(expiresInSeconds - getCloseToExpirySeconds());
            mRefreshRequest = refreshRequest;
        }

        /**
         * Returns the number of seconds before expiry that this token is considered in need of a refresh.
         */
        protected int getCloseToExpirySeconds() {
            return MSA_ACCESS_TOKEN_CLOSE_TO_EXPIRY_SECONDS; // Base class expects access tokens
        }

        /**
         * Returns the number of seconds to wait before retrying, when a refresh fails with a transient error.
         */
        protected long getRetrySeconds() {
            return MSA_ACCESS_TOKEN_RETRY_SECONDS; // Base class expects access tokens
        }

        /**
         * Returns the refresh token to use to refresh the token held by this item.
         * For access tokens, this gets the refresh token held by the cache.
         * For refresh tokens, this just returns the currently-held token.
         */
        protected AsyncOperation<String> getRefreshTokenAsync() {
            return MSATokenCache.this.getRefreshTokenAsync();
        }

        /**
         * Steps to complete after a successful refresh.
         * For access tokens, sets the new token and new expiration.
         * For refresh tokens, marks current access tokens as expired, and caches the refresh token in persistent storage.
         */
        protected synchronized void onSuccessfulRefresh(MSATokenRequest.Result result) {
            Log.i(TAG, "Successfully refreshed access token.");
            mToken = result.getAccessToken();
            mCloseToExpirationDate = getDateSecondsAfterNow(result.getExpiresIn() - getCloseToExpirySeconds());
        }

        /**
         * Private helper - asynchronously fetches the token held by this item.
         * If the token is close to expiry, refreshes it first.
         * If this refresh fails due to transient error, recursively retries up to remainingRetries times to refresh.
         *
         * @param operation         AsyncOperation to return the token on
         * @param remainingRetries  number of times to retry refreshing, in the case of transient error
         * @return the operation that was passed in
         */
        private AsyncOperation<String> _getTokenAsyncInternal(final AsyncOperation<String> operation, final int remainingRetries) {
            if (!needsRefresh()) {
                operation.complete(mToken); // Already have a non-stale token, can just return with it
                return operation;
            }

            getRefreshTokenAsync()
                    .thenComposeAsync((AsyncOperation.ResultFunction<String, AsyncOperation<MSATokenRequest.Result>>) refreshToken -> mRefreshRequest.requestAsync(refreshToken))
                    .thenAcceptAsync((AsyncOperation.ResultConsumer<MSATokenRequest.Result>) result -> {
                        switch (result.getStatus()) {
                            case SUCCESS:
                                onSuccessfulRefresh(result);
                                operation.complete(mToken);
                                break;

                            case TRANSIENT_FAILURE:
                                // Recursively retry the refresh, if there are still remaining retries
                                if (remainingRetries <= 0) {
                                    Log.e(TAG, "Reached max number of retries for refreshing token.");
                                    operation.complete(null);

                                } else {
                                    Log.i(TAG, "Transient error while refreshing token, retrying in " + getRetrySeconds() + "seconds...");
                                    sRetryExecutor.schedule(() -> {
                                        _getTokenAsyncInternal(operation, remainingRetries - 1);
                                    }, getRetrySeconds(), TimeUnit.SECONDS);
                                }
                                break;

                            default: // PERMANENT_FAILURE
                                Log.e(TAG, "Permanent error occurred while refreshing token.");
                                MSATokenCache.this.onPermanentFailure();
                                operation.complete(null);
                                break;
                        }
                    });

            return operation;
        }

        /**
         * Asynchronously fetches the token held by this item, refreshing it if necessary.
         */
        public AsyncOperation<String> getTokenAsync() {
            AsyncOperation<String> ret = new AsyncOperation<String>();
            return _getTokenAsyncInternal(ret, TOKEN_REFRESH_MAX_RETRIES);
        }

        public boolean needsRefresh() {
            return mCloseToExpirationDate.before(new Date());
        }

        public boolean isExpired() {
            return getDateSecondsAfter(mCloseToExpirationDate, getCloseToExpirySeconds()).before(new Date());
        }

        public synchronized void markExpired() {
            mCloseToExpirationDate = new Date(0); // Start of epoch
        }
    }

    /**
     * Private helper class wrapping a cached refresh token. Responsible for refreshing it on demand. Can translate to/from json format.
     */
    private final class MSARefreshTokenCacheItem extends MSATokenCacheItem {
        private static final String JSON_TOKEN_KEY = "refresh_token";
        private static final String JSON_EXPIRATION_KEY = "expires";

        public MSARefreshTokenCacheItem(String token, int expiresInSeconds, MSATokenRequest refreshRequest) {
            super(token, expiresInSeconds, refreshRequest);
        }

        public MSARefreshTokenCacheItem(JSONObject json) throws IOException, JSONException, ParseException {
            super(null, 0, new MSATokenRequest(mClientId, MSATokenRequest.GrantType.REFRESH, MSA_OFFLINE_ACCESS_SCOPE, null));

            mToken = json.optString(JSON_TOKEN_KEY);
            String dateString = json.optString(JSON_EXPIRATION_KEY);
            if (mToken == null || dateString == null) {
                throw new IOException("Saved refresh token was improperly formatted.");
            }

            Date expirationDate = DateFormat.getDateTimeInstance().parse(dateString);
            mCloseToExpirationDate = getDateSecondsAfter(expirationDate, -MSA_REFRESH_TOKEN_CLOSE_TO_EXPIRY_SECONDS);
        }

        protected int getCloseToExpirySeconds() {
            return MSA_REFRESH_TOKEN_CLOSE_TO_EXPIRY_SECONDS;
        }

        protected long getRetrySeconds() {
            return MSA_REFRESH_TOKEN_RETRY_SECONDS;
        }

        protected AsyncOperation<String> getRefreshTokenAsync() {
            return AsyncOperation.completedFuture(mToken);
        }

        protected synchronized void onSuccessfulRefresh(MSATokenRequest.Result result) {
            Log.i(TAG, "Successfully refreshed refresh token.");
            mToken = result.getRefreshToken();
            mCloseToExpirationDate =
                    getDateSecondsAfterNow(MSA_REFRESH_TOKEN_EXPIRATION_SECONDS - MSA_REFRESH_TOKEN_CLOSE_TO_EXPIRY_SECONDS);
            MSATokenCache.this.markAccessTokensExpired();
            MSATokenCache.this.trySaveRefreshToken();
        }

        public synchronized JSONObject toJSON() throws JSONException {
            // Get the actual expiration date
            Date expirationDate = getDateSecondsAfter(mCloseToExpirationDate, MSA_REFRESH_TOKEN_CLOSE_TO_EXPIRY_SECONDS);

            JSONObject ret = new JSONObject();
            ret.put(JSON_TOKEN_KEY, mToken);
            ret.put(JSON_EXPIRATION_KEY, DateFormat.getDateTimeInstance().format(expirationDate));
            return ret;
        }
    }

    /**
     * Provides callbacks when the cache encounters a permanent failure and has to wipe its state.
     */
    public static interface Listener { void onTokenCachePermanentFailure(); }

    private final String mClientId;
    private final Context mContext;

    private MSARefreshTokenCacheItem mCachedRefreshToken = null;
    private final Map<String, MSATokenCacheItem> mCachedAccessTokens = new ArrayMap<>();

    private final Collection<Listener> mListeners = new ArrayList<>();

    public MSATokenCache(String clientId, Context context) {
        mClientId = clientId;
        mContext = context;
    }

    /**
     * Returns a file in application-specific storage that's used to persist the refresh token across sessions.
     */
    private File getRefreshTokenSaveFile() throws IOException {
        Context appContext = mContext.getApplicationContext();
        File appDirectory = appContext.getDir(appContext.getPackageName(), Context.MODE_PRIVATE);
        if (appDirectory == null) {
            throw new IOException("Could not access app directory.");
        }

        return new File(appDirectory, "samplemsaaccountprovider.dat");
    }

    /**
     * Tries to save the current refresh token to persistent storage.
     */
    private void trySaveRefreshToken() {
        Log.i(TAG, "Trying to save refresh token...");
        try {
            File file = getRefreshTokenSaveFile();
            JSONObject json = file.exists() ? new JSONObject(IOUtil.readUTF8Stream(new FileInputStream(file))) : new JSONObject();

            json.put(mClientId, mCachedRefreshToken.toJSON());
            IOUtil.writeUTF8Stream(new FileOutputStream(file), json.toString());

            Log.i(TAG, "Saved refresh token.");

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Exception while saving refresh token. \"" + e.getLocalizedMessage() + "\" Will not save.");
        }
    }

    /**
     * Tries to read a saved refresh token from persistent storage, and return it as an MSARefreshTokenItem.
     */
    private MSARefreshTokenCacheItem tryReadSavedRefreshToken() {
        Log.i(TAG, "Trying to read saved refresh token...");
        try {
            File file = getRefreshTokenSaveFile();

            if (!file.exists()) {
                Log.i(TAG, "No saved refresh token was found.");
                return null;
            }

            JSONObject json = new JSONObject(IOUtil.readUTF8Stream(new FileInputStream(file)));
            JSONObject innerJson = json.optJSONObject(mClientId);

            if (innerJson == null) {
                Log.i(TAG, "Could not read saved refresh token.");
                return null;
            }

            Log.i(TAG, "Read saved refresh token.");
            return new MSARefreshTokenCacheItem(innerJson);

        } catch (IOException | JSONException | ParseException e) {
            Log.e(TAG, "Exception reading saved refresh token. \"" + e.getLocalizedMessage() + "\"");
            return null;
        }
    }

    /**
     * Tries to delete the saved refresh token for this app in persistent storage.
     */
    private void tryClearSavedRefreshToken() {
        Log.i(TAG, "Trying to delete saved refresh token...");
        try {
            File file = getRefreshTokenSaveFile();
            if (!file.exists()) {
                Log.i(TAG, "No saved refresh token was found.");
                return;
            }

            try {
                // Try to remove just a section of the json corresponding to client id
                JSONObject json = new JSONObject(IOUtil.readUTF8Stream(new FileInputStream(file)));
                json.remove(mClientId);

                if (json.length() <= 0) {
                    file.delete(); // Just delete the file if the json would be empty
                } else {
                    IOUtil.writeUTF8Stream(new FileOutputStream(file), json.toString());
                }
            } catch (JSONException e) {
                // Failed to parse the json, just delete everything
                file.delete();
            }

            Log.i(TAG, "Deleted saved refresh token.");

        } catch (IOException e) { Log.e(TAG, "Failed to delete saved refresh token. \"" + e.getLocalizedMessage() + "\""); }
    }

    /**
     * Marks access tokens as expired, such that a refresh is performed before returning, when the access token is next requested.
     */
    private synchronized void markAccessTokensExpired() {
        for (MSATokenCacheItem cachedAccessToken : mCachedAccessTokens.values()) {
            cachedAccessToken.markExpired();
        }
    }

    /**
     * Calls back any listeners that the cache has encountered a permanent failure, and that they should perform any needed error-handling.
     */
    private void onPermanentFailure() {
        clearTokens();
        for (Listener listener : mListeners) {
            listener.onTokenCachePermanentFailure();
        }
    }

    public void setRefreshToken(String refreshToken) {
        MSATokenRequest refreshRequest = new MSATokenRequest(mClientId, MSATokenRequest.GrantType.REFRESH, MSA_OFFLINE_ACCESS_SCOPE, null);

        synchronized (this) {
            mCachedRefreshToken = new MSARefreshTokenCacheItem(refreshToken, MSA_REFRESH_TOKEN_EXPIRATION_SECONDS, refreshRequest);
            markAccessTokensExpired();
            trySaveRefreshToken();
        }
    }

    public void setAccessToken(String accessToken, String scope, int expiresInSeconds) {
        MSATokenRequest refreshRequest = new MSATokenRequest(mClientId, MSATokenRequest.GrantType.REFRESH, scope, null);

        synchronized (this) {
            mCachedAccessTokens.put(scope, new MSATokenCacheItem(accessToken, expiresInSeconds, refreshRequest));
        }
    }

    public synchronized AsyncOperation<String> getRefreshTokenAsync() {
        if (mCachedRefreshToken != null) {
            return mCachedRefreshToken.getTokenAsync();
        } else {
            return AsyncOperation.completedFuture(null);
        }
    }

    public synchronized AsyncOperation<String> getAccessTokenAsync(String scope) {
        MSATokenCacheItem cachedAccessToken = mCachedAccessTokens.get(scope);
        if (cachedAccessToken != null) {
            return cachedAccessToken.getTokenAsync();
        } else {
            return AsyncOperation.completedFuture(null);
        }
    }

    public void saveAccountId(String id) {
        // Get the shared preferences
        SharedPreferences preferences = mContext.getSharedPreferences(mContext.getPackageName(), Context.MODE_PRIVATE);

        // Save the given ID to the shared preferences
        preferences.edit().putString(GUID_ID_KEY, id).apply();
    }

    public String readSavedAccountId() {
        // Get the shared preferences
        SharedPreferences preferences = mContext.getSharedPreferences(mContext.getPackageName(), Context.MODE_PRIVATE);

        // Grab the value of the key with a default value of empty string
        String id = preferences.getString(GUID_ID_KEY, "");
        // Check that we found a value and not the default value
        if (id.isEmpty()) {
            Log.e(TAG, "readSavedAccountId failed to get the ID");
        }
        return id;
    }

    public synchronized void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public synchronized void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    public Set<String> allScopes() {
        return mCachedAccessTokens.keySet();
    }

    /**
     * Tries to load a saved refresh token from disk. If successful, the loaded refresh token is used as this cache's refresh token.
     * @return Whether a saved refresh token was loaded successfully.
     */
    public boolean loadSavedRefreshToken() {
        Log.i(TAG, "Trying to load saved refresh token...");
        MSARefreshTokenCacheItem savedRefreshToken = tryReadSavedRefreshToken();

        if (savedRefreshToken == null) {
            Log.i(TAG, "Failed to load saved refresh token.");
            return false;
        }

        if (savedRefreshToken.isExpired()) {
            Log.i(TAG, "Read saved refresh token, but was expired. Ignoring.");
            return false;
        }

        Log.i(TAG, "Successfully loaded saved refresh token.");
        mCachedRefreshToken = savedRefreshToken;
        markAllTokensExpired(); // Force a refresh on everything on first use
        return true;
    }

    /**
     * Clears all tokens from the cache, and any saved refresh tokens belonging to this app in persistent storage.
     */
    public synchronized void clearTokens() {
        mCachedAccessTokens.clear();
        mCachedRefreshToken = null;
        tryClearSavedRefreshToken();
    }

    /**
     * Marks all tokens as expired, such that a refresh is performed before returning, when a token is next requested.
     */
    public synchronized void markAllTokensExpired() {
        mCachedRefreshToken.markExpired();
        markAccessTokensExpired();
    }
}
