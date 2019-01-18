//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.microsoft.connecteddevices.remotesystems.commanding.AppServiceProvider;

import java.util.ArrayList;
/**
 * LoginActivity uses the sampleAccountProviders library to trigger auth flow
 * On successful sign in, the user is redirected to the ModuleSelectFragment where they can select Device Relay or Activities
 */
public class LoginActivity extends AppCompatActivity {
    // region Member Variables
    private static final String TAG = LoginActivity.class.getName();

    private AccountBroker accountBroker;
    // endregion

    // region Overrides
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        PlatformBroker.getPlatformBroker().getOrInitializePlatform(this);
        accountBroker = new AccountBroker(getBaseContext());

        // Initialize buttons in case the user clicks out of the webview
        findViewById(R.id.sign_in_button)
            .setOnClickListener(v -> signIn());

        signIn();
    }
    // endregion

    private void signIn() {
        Log.d(TAG, "Start signin");
        accountBroker.signIn(this, (account, throwable) -> {
            Log.d(TAG, "Sign in completed.");

            Log.v(TAG, "Start the platform");
            PlatformBroker platformBroker = PlatformBroker.getPlatformBroker();
            platformBroker.startPlatform();

            if (accountBroker.isSignedIn()) {
                Log.d(TAG, "Adding received account to account manager");
                // Adding account to Account Manager
                PlatformBroker.getPlatformBroker().addAccountToAccountManager(account);

                // Saving Id to Account Broker, so it can be used in other places
                AccountBroker.setCurrentAccountId(account.getId());

                Log.v(TAG, "Register for notifications");
                platformBroker.createNotificationReceiver(this);
                // This cannot be called now
                platformBroker.registerNotificationsForAccount(platformBroker.getAccount(AccountBroker.getCurrentAccountId()));

                Log.v(TAG, "Register the Relay SDK");
                ArrayList<AppServiceProvider> appServiceProviders = new ArrayList<>();
                appServiceProviders.add(new PingPongService());
                appServiceProviders.add(new EchoService());

                platformBroker.register(this, platformBroker.getAccount(AccountBroker.getCurrentAccountId()), appServiceProviders, new SimpleLaunchHandler());

                launchMainActivity();
            }
        });
    }

    private void launchMainActivity() {
        Log.d(TAG, "Launching MainActivity");
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}
