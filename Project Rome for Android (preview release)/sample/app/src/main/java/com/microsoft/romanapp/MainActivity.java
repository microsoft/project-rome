//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.romanapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.connecteddevices.IAuthCodeProvider;
import com.microsoft.connecteddevices.IPlatformInitializationHandler;
import com.microsoft.connecteddevices.Platform;
import com.microsoft.connecteddevices.PlatformInitializationStatus;

public class MainActivity extends FragmentActivity {
    // Use your own Client ID, assigned when your app was registered with MSA.
    private static String CLIENT_ID = Secrets.CLIENT_ID;

    private TextView _statusOutput;
    private Button _signInButton;
    private String _oauthUrl;
    WebView _web;
    Dialog _authDialog;
    private Platform.IAuthCodeHandler _authCodeHandler;
    private static String REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf";

    private static final String TAG = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _statusOutput = (TextView) findViewById(R.id.status_output);
        _signInButton = (Button) findViewById(R.id.sign_in_button);
        _authDialog = new Dialog(this);
        _authDialog.setContentView(R.layout.auth_dialog);
        _web = (WebView) _authDialog.findViewById(R.id.webv);
        _web.setWebChromeClient(new WebChromeClient());
        _web.getSettings().setJavaScriptEnabled(true);
        _web.getSettings().setDomStorageEnabled(true);

        appendStatus("Initializing Platform");
        appendStatus("Platform will attempt to use previously saved refresh token");
        Platform.initialize(getApplicationContext(), new IAuthCodeProvider() {
            @Override
            /**
             * ConnectedDevices Platform needs the app to fetch a MSA auth_code using the given oauthUrl.
             * When app is fetched the auth_code, it needs to invoke the authCodeHandler onAuthCodeFetched method.
             */
            public void fetchAuthCodeAsync(String oauthUrl, Platform.IAuthCodeHandler handler) {
                _oauthUrl = oauthUrl;
                _authCodeHandler = handler;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        _signInButton.setVisibility(View.VISIBLE);
                        _signInButton.setEnabled(true);
                    }
                });
            }

            @Override
            /**
             * ConnectedDevices Platform needs your app's registered client ID.
             */
            public String getClientId() {
                return CLIENT_ID;
            }
        }, new IPlatformInitializationHandler() {

            @Override
            public void onDone() {
                Log.i(TAG, "Initialized platform successfully");
                Intent intent = new Intent(MainActivity.this, DeviceRecyclerActivity.class);
                startActivity(intent);
            }

            @Override
            public void onError(PlatformInitializationStatus status) {
                if (status == PlatformInitializationStatus.PLATFORM_FAILURE) {
                    Log.e(TAG, "Error initializing platform");
                } else if (status == PlatformInitializationStatus.TOKEN_ERROR) {
                    Log.e(TAG, "Error refreshing tokens");
                }
            }
        });
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        Platform.resume();
    }

    @Override
    public void onPause() {
        Platform.suspend();
        super.onPause();
    }

    public void onLoginClick(View view) {
        _signInButton.setEnabled(false);
        _web.loadUrl(_oauthUrl);

        WebViewClient webViewClient = new WebViewClient() {
            boolean authComplete = false;

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (url.startsWith(REDIRECT_URI)) {
                    Uri uri = Uri.parse(url);
                    String code = uri.getQueryParameter("code");
                    String error = uri.getQueryParameter("error");
                    if (code != null && !authComplete) {
                        authComplete = true;
                        _authDialog.dismiss();
                        Log.i(TAG, "OAuth sign-in finished successfully");

                        if (_authCodeHandler != null) {
                            _authCodeHandler.onAuthCodeFetched(code);
                        }
                    } else if (error != null) {
                        authComplete = true;
                        Log.e(TAG, "OAuth sign-in failed with error: " + error);
                        Intent resultIntent = new Intent();
                        setResult(Activity.RESULT_CANCELED, resultIntent);
                        Toast.makeText(getApplicationContext(), "Error Occurred: " + error, Toast.LENGTH_SHORT).show();

                        _authDialog.dismiss();
                    }
                }
            }
        };

        _web.setWebViewClient(webViewClient);
        _authDialog.show();
        _authDialog.setCancelable(true);
    }

    private void appendStatus(final String status) {
        if (_statusOutput == null) {
            Log.e(TAG, "StatusOutput field is null");
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String prevStatus = _statusOutput.getText().toString();
                String newStatus = prevStatus + "\n" + status;

                _statusOutput.setText(newStatus);
            }
        });
    }
}
