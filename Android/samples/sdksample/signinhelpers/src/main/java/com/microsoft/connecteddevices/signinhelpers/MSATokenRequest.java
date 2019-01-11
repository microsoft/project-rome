//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.connecteddevices.signinhelpers;

import android.support.annotation.Keep;
import android.util.Log;
import android.util.Pair;

import com.microsoft.connecteddevices.AsyncOperation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Encapsulates a noninteractive request for an MSA token.
 * This request may be performed multiple times.
 */
@Keep
final class MSATokenRequest {

    private static final String TAG = MSATokenRequest.class.getName();

    // OAuth Token Grant Type
    public static final class GrantType {
        public static final String CODE = "authorization_code";
        public static final String REFRESH = "refresh_token";
    }

    /**
     * Class encapsulating the result of an MSATokenRequest.
     */
    public static final class Result {
        public static enum Status { SUCCESS, TRANSIENT_FAILURE, PERMANENT_FAILURE }

        private final Status mStatus;
        private String mAccessToken = null;
        private String mRefreshToken = null;
        private int mExpiresIn = 0;

        Result(Status status, JSONObject responseJson) {
            mStatus = status;

            if (responseJson != null) {
                mAccessToken = responseJson.optString("access_token", null);
                mRefreshToken = responseJson.optString("refresh_token", null);
                mExpiresIn = responseJson.optInt("expires_in"); // returns 0 if this key doesn't exist
            }
        }

        public Status getStatus() {
            return mStatus;
        }

        public String getAccessToken() {
            return mAccessToken;
        }

        public String getRefreshToken() {
            return mRefreshToken;
        }

        public int getExpiresIn() {
            return mExpiresIn;
        }
    }

    private final String mClientId;
    private final String mGrantType;
    private final String mScope;
    private final String mRedirectUri;

    public MSATokenRequest(String clientId, String grantType, String scope, String redirectUri) {
        mClientId = clientId;
        mGrantType = grantType;
        mScope = scope;
        mRedirectUri = redirectUri;
    }

    /**
     * Builds a query string from a list of name-value pairs.
     *
     * @param params Name-value pairs to compose the query string from
     * @return A query string composed of the provided name-value pairs
     * @throws UnsupportedEncodingException Thrown if encoding a name or value fails
     */
    private static String getQueryString(List<Pair<String, String>> params) throws UnsupportedEncodingException {
        StringBuilder queryStringBuilder = new StringBuilder();
        boolean isFirstParam = true;
        for (Pair<String, String> param : params) {
            if (isFirstParam) {
                isFirstParam = false;
            } else {
                queryStringBuilder.append("&");
            }

            queryStringBuilder.append(URLEncoder.encode(param.first, "UTF-8"));
            queryStringBuilder.append("=");
            queryStringBuilder.append(URLEncoder.encode(param.second, "UTF-8"));
        }

        return queryStringBuilder.toString();
    }

    /**
     * Fetch Token (Access or Refresh Token).
     * @param clientId - clientId of the app's registration in the MSA portal
     * @param grantType - one of the MSATokenRequest.GrantType constants
     * @param scope
     * @param redirectUri
     * @param token - authCode for GrantType.CODE, or refresh token for GrantType.REFRESH
     */
    public static AsyncOperation<MSATokenRequest.Result> requestAsync(
        final String clientId, final String grantType, final String scope, final String redirectUri, final String token) {
        if (token == null || token.length() <= 0) {
            Log.e(TAG, "Refresh token or auth code for MSATokenRequest was unexpectedly empty - treating as permanent failure.");
            return AsyncOperation.completedFuture(new MSATokenRequest.Result(Result.Status.PERMANENT_FAILURE, null));
        }

        return AsyncOperation.supplyAsync(new AsyncOperation.Supplier<MSATokenRequest.Result>() {
            @Override
            public MSATokenRequest.Result get() {
                HttpsURLConnection connection = null;
                MSATokenRequest.Result.Status status = Result.Status.TRANSIENT_FAILURE;
                JSONObject responseJson = null;

                try {
                    // Build the query string
                    List<Pair<String, String>> params = new LinkedList<>();
                    params.add(new Pair<>("client_id", clientId));
                    params.add(new Pair<>("grant_type", grantType));

                    if (grantType.equals(GrantType.CODE)) {
                        params.add(new Pair<>("redirect_uri", redirectUri));
                        params.add(new Pair<>("code", token));
                    } else if (grantType.equals(GrantType.REFRESH)) {
                        params.add(new Pair<>("scope", scope));
                        params.add(new Pair<>(grantType, token));
                    }

                    String queryString = getQueryString(params);

                    // Write the query string
                    URL url = new URL("https://login.live.com/oauth20_token.srf");
                    connection = (HttpsURLConnection)url.openConnection();
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    IOUtil.writeUTF8Stream(connection.getOutputStream(), queryString);

                    // Parse the response
                    int responseCode = connection.getResponseCode();
                    if (responseCode >= 500) {
                        status = Result.Status.TRANSIENT_FAILURE;
                    } else if (responseCode >= 400) {
                        status = Result.Status.PERMANENT_FAILURE;
                    } else if ((responseCode >= 200 && responseCode < 300) || responseCode == 304) {
                        status = Result.Status.SUCCESS;
                    } else {
                        status = Result.Status.TRANSIENT_FAILURE;
                    }

                    if (status == Result.Status.SUCCESS) {
                        responseJson = new JSONObject(IOUtil.readUTF8Stream(connection.getInputStream()));
                    } else {
                        Log.e(TAG, "Failed to get token with HTTP code: " + responseCode);
                    }

                } catch (IOException | JSONException e) {
                    Log.e(TAG, "Failed to get token: \"" + e.getLocalizedMessage() + "\"");
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                    return new MSATokenRequest.Result(status, responseJson);
                }
            }
        });
    }

    /**
     * Fetch token (Access or Refresh Token).
     * @param token - authCode for GrantType.CODE, or refresh token for GrantType.REFRESH
     */
    public AsyncOperation<MSATokenRequest.Result> requestAsync(String token) {
        return requestAsync(mClientId, mGrantType, mScope, mRedirectUri, token);
    }
}