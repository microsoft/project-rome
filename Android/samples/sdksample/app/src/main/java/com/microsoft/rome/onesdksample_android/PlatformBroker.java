//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.microsoft.connecteddevices.ConnectedDevicesAccessTokenRequest;
import com.microsoft.connecteddevices.ConnectedDevicesAccount;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationRegistrationManager;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationRegistrationState;
import com.microsoft.connecteddevices.ConnectedDevicesPlatform;
import com.microsoft.connecteddevices.remotesystems.commanding.AppServiceProvider;
import com.microsoft.connecteddevices.remotesystems.commanding.LaunchUriProvider;
import com.microsoft.connecteddevices.signinhelpers.MSASigninHelperAccount;
import com.microsoft.connecteddevices.remotesystems.commanding.RemoteSystemAppRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

public class PlatformBroker {
    // region Member Variables
    private final String TAG = PlatformBroker.class.getName();

    public final String DATE_FORMAT = "MM/dd/yyyy HH:mm:ss";
    public final String TIMESTAMP_KEY = "TIMESTAMP_KEY";
    public final String PACKAGE_KEY = "PACKAGE_KEY";
    public final String GUID_KEY = "GUID_KEY";
    public final String PACKAGE_VALUE = "com.microsoft.oneRomanApp";

    private ConnectedDevicesPlatform sPlatform;
    private GcmNotificationReceiver gcmNotificationReceiver;
    private static PlatformBroker platformBroker;

    // region Constructor
    private PlatformBroker() { }
    // endregion

    public synchronized ConnectedDevicesPlatform getPlatform() {
        return sPlatform;
    }

    public static synchronized PlatformBroker getPlatformBroker() {
        if(platformBroker == null) {
            platformBroker = new PlatformBroker();
        }
        return platformBroker;
    }

    private synchronized void initializePlatform(Context context) {
        // Create Platform
        sPlatform = new ConnectedDevicesPlatform(context);

        // Note: Very important to subscribe to these events before starting the platform.
        // Subscribe to AccessTokenRequested event
        sPlatform.getAccountManager().accessTokenRequested().subscribe((accountManager, args) -> {
            ConnectedDevicesAccessTokenRequest request = args.getRequest();
            List<String> scopes = request.getScopes();

            MSASigninHelperAccount signinHelperAccount = AccountBroker.getSignInHelper();
            if (signinHelperAccount == null) {
                Log.w(TAG,"Failed to find a SigninHelperAccount matching the given account for the token request");
                request.completeWithErrorMessage("No account was found to get the token");
                return;
            }

            // Complete the request with a token
            signinHelperAccount.getAccessTokenAsync(scopes)
                    .whenComplete((token, throwable) -> {
                        request.completeWithAccessToken(token);
                    }).exceptionally(throwable -> {
                        request.completeWithErrorMessage("The Account could not return a token with those scopes");
                        return null;
                    });
        });

        // Subscribe to AccessTokenInvalidated event
        sPlatform.getAccountManager().accessTokenInvalidated().subscribe((accountManager, args) -> {
            // If access token in invalidated, refresh token and renew access token.
            AccountBroker.getSignInHelper().getAccessTokenAsync(args.getScopes());
        });

        // Subscribe to NotificationRegistrationStateChanged event
        sPlatform.getNotificationRegistrationManager().notificationRegistrationStateChanged().subscribe((notificationRegistrationManager, args) -> {
            // If notification registration state is expiring or expired, re-register for account again.
            ConnectedDevicesNotificationRegistrationState state = args.getState();
            if (state == ConnectedDevicesNotificationRegistrationState.EXPIRING || state == ConnectedDevicesNotificationRegistrationState.EXPIRED) {
                registerNotificationsForAccount(args.getAccount());
            }
        });
    }

    public synchronized ConnectedDevicesPlatform getOrInitializePlatform(Context context) {
        if (sPlatform == null) {
            initializePlatform(context);
        }

        return sPlatform;
    }

    public synchronized void startPlatform() {
        if (sPlatform == null) {
            Log.e(TAG,"Cannot start platform before initializing it");
        } else {
            sPlatform.start();
        }
    }

    public synchronized void createNotificationReceiver(Context context) {
        gcmNotificationReceiver = new GcmNotificationReceiver(context);
    }

    public synchronized void addAccountToAccountManager(ConnectedDevicesAccount account) {
        if (sPlatform == null)
        {
            Log.e(TAG,"Cannot add account before initializing the platform");
            return;
        }

        sPlatform.getAccountManager().addAccountAsync(account).whenCompleteAsync((connectedDevicesAddAccountResult, throwable) -> {
            // Note: If add account fails, retry again at a later time. Any operations that require
            // the account cannot be performed until the account has been added successfully.
            if (throwable != null){
                Log.e(TAG,"AccountManager addAccountAsync returned a throwable: " + throwable);
            } else {
                Log.i(TAG,"AccountManager addAccountAsync returned " + connectedDevicesAddAccountResult.getStatus().toString());
            }
        });
    }

    public synchronized ConnectedDevicesAccount getAccount(String id) {
        if(sPlatform == null) {
            Log.e(TAG,"Platform needs to be initialized before getting an account");
            return null;
        }

        List<ConnectedDevicesAccount> allAccounts = sPlatform.getAccountManager().getAccounts();
        if(allAccounts.isEmpty()) {
            Log.e(TAG,"No accounts found in the account manager.");
            return null;
        }

        for(ConnectedDevicesAccount acc : allAccounts) {
            if(acc.getId().equals(id)) {
                return acc;
            }
        }

        // No matching account was found
        Log.e(TAG,"No account with the given Id found in the account manager");
        return null;
    }

    public synchronized void registerNotificationsForAccount(ConnectedDevicesAccount account) {
        if (sPlatform == null) {
            Log.w(TAG, "Cannot register for notifications without platform being initialized");
            return;
        }

        // Get notification registration manager
        ConnectedDevicesNotificationRegistrationManager registrationManager = sPlatform.getNotificationRegistrationManager();

        Log.v(TAG, "Getting NotificationNotification to register for notifications for account: " + account.getId() + " with type: " + account.getType());
        gcmNotificationReceiver.getNotificationRegistrationAsync().whenCompleteAsync((connectedDevicesNotificationRegistration, throwable) -> {
                    String accountId = account.getId();

                    Log.v(TAG, "Registering for notifications for account: " + accountId);

                    registrationManager.registerForAccountAsync(account, connectedDevicesNotificationRegistration).whenCompleteAsync((result, throwable1) -> {
                                if (throwable1 != null) {
                                    Log.e(TAG, "RegistrationManager exception encountered " + throwable1);
                                } else if (result) {
                                    Log.i(TAG, "Successfully performed notification registration for given account");
                                } else {
                                    // If registration fails, it should be retried again when network is available.
                                    Log.e(TAG, "Failed to perform notification registration for given account." + throwable1);
                                }
                            }
                    );
                }
        );
    }

    public void register(Context context, ConnectedDevicesAccount account, ArrayList<AppServiceProvider> appServiceProviders, LaunchUriProvider launchUriProvider) {
        // Initialize the platform with all possible services
        // Crashing
        RemoteSystemAppRegistration registration = RemoteSystemAppRegistration.getForAccount(account, sPlatform);

        registration.setAttributes(new TreeMap<String, String>() {
            {
                put(TIMESTAMP_KEY, getInitialRegistrationDateTime(context));
                put(PACKAGE_KEY, PACKAGE_VALUE);
            }
        });

        // Add the given AppService and LaunchUri Providers to the registration
        if (appServiceProviders != null) {
            registration.setAppServiceProviders(appServiceProviders);
        }
        if (launchUriProvider != null) {
            registration.setLaunchUriProvider(launchUriProvider);
        }

        // Perform the registration by saving the object
        registration.saveAsync().thenAcceptAsync(
                // When app is successfully registered, other applications can discover it and begin sending commands to it.
                // If app registration fails to save, retry again when internet becomes available.
                success -> Log.d(TAG,"RemoteSystemAppRegistration was saved with success: " + success)
        ).exceptionally(throwable -> {
            Log.e(TAG,"RemoteSystemAppRegistration.saveAsync: " + throwable);
            return null;
        });
    }

    /**
     * Grab the initial registration date-time if one is found, otherwise generate a new one.
     * @param context
     * @return Datetime to insert into the RemoteSystemAppRegistration
     */
    private String getInitialRegistrationDateTime(final Context context) {
        SharedPreferences preferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);

        String timestamp;
        // Check that the SharedPreferences has the timestamp. This should be true after the first clean install -> Platform init.
        if (preferences.contains(TIMESTAMP_KEY)) {
            // The `getString` API requires a default value. Since we check that key exists we should never get the default value of empty
            // string.
            timestamp = preferences.getString(TIMESTAMP_KEY, "");
            if (timestamp.isEmpty()) {
                Log.e(TAG, "getInitialRegistrationDateTime failed to get the TimeStamp although the key exists");
                throw new RuntimeException("Failed to get TimeStamp after verifying it exists");
            }
        } else {
            // Create the initial timestamp for RemoteSystemApp registration and store it in SharedPreferences
            timestamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());
            preferences.edit().putString(TIMESTAMP_KEY, timestamp).apply();
        }

        return timestamp;
    }
}
