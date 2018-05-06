//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.microsoft.connecteddevices.base.AsyncOperation;

/**
 * LoginActivity uses the sampleAccountProviders library to trigger auth flow
 * On successful sign in, the user is redirected to the ModuleSelectFragment where they can select Device Relay or Activities
 */
public class LoginActivity extends AppCompatActivity {
    // region Member Variables
    private static final String TAG = LoginActivity.class.getName();

    private AccountProviderBroker accountProviderBroker;
    // endregion

    // region Overrides
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        accountProviderBroker = new AccountProviderBroker(getBaseContext());

        // Initialize buttons incase the user clicks out of the webview
        findViewById(R.id.sign_in_button)
            .setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    signIn();
                }
            });

        if (!accountProviderBroker.isSignedIn()) {
            signIn();
        } else {
            launchMainActivity();
        }
    }
    // endregion

    private void signIn() {
        accountProviderBroker.signIn(this, new SignInCompletionHandler());
    }

    /**
     * Implementation of callback for signIn operations
     * On sign in success, change view to ModuleSelectFragment (where user can choose between device relay & activities)
     */
    private class SignInCompletionHandler implements AsyncOperation.ResultBiConsumer<Boolean, Throwable> {
        @Override
        public void accept(final Boolean signInSuccess, final Throwable e) throws Throwable {
            Log.d(TAG, "Sign in complete " + Boolean.toString(signInSuccess));

            if (signInSuccess) {
                launchMainActivity();
            }
        }
    }

    private void launchMainActivity() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}
