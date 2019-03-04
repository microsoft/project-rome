//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.ArrayMap;
import android.util.Log;

import com.microsoft.connecteddevices.ConnectedDevicesAccessTokenRequest;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationRegistration;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationRegistrationResult;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationRegistrationStatus;
import com.microsoft.connecteddevices.ConnectedDevicesAccount;
import com.microsoft.connecteddevices.ConnectedDevicesAccountManager;
import com.microsoft.connecteddevices.ConnectedDevicesAddAccountResult;
import com.microsoft.connecteddevices.ConnectedDevicesAccountAddedStatus;
import com.microsoft.connecteddevices.ConnectedDevicesRemoveAccountResult;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationRegistrationManager;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationRegistrationState;
import com.microsoft.connecteddevices.ConnectedDevicesPlatform;
import com.microsoft.connecteddevices.remotesystems.commanding.AppServiceProvider;
import com.microsoft.connecteddevices.remotesystems.commanding.LaunchUriProvider;
import com.microsoft.connecteddevices.signinhelpers.SigninHelperAccount;
import com.microsoft.connecteddevices.remotesystems.commanding.RemoteSystemAppRegistration;
import com.microsoft.connecteddevices.remotesystems.commanding.RemoteSystemAppRegistrationPublishResult;
import com.microsoft.connecteddevices.remotesystems.commanding.RemoteSystemAppRegistrationPublishStatus;
import com.microsoft.connecteddevices.AsyncOperation;
import com.microsoft.connecteddevices.ConnectedDevicesAccount;

import java.lang.IllegalStateException;
import java.lang.Throwable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Holds all state and logic for a single app + sdk account.
 */
public class Account {
    // region Member Variables
    private final String TAG = Account.class.getName();

    public final String DATE_FORMAT = "MM/dd/yyyy HH:mm:ss";
    public final String TIMESTAMP_KEY = "TIMESTAMP_KEY";
    public final String PACKAGE_KEY = "PACKAGE_KEY";
    public final String PACKAGE_VALUE = "com.microsoft.rome.onesdksample_android";

    private SigninHelperAccount mSignInHelper;
    private ConnectedDevicesAccount mAccount;
    private AccountRegistrationState mState;
    private ConnectedDevicesPlatform mPlatform;
    private RemoteSystemAppRegistration mRegistration;
    // endregion

    // region Constructors
    /**
     * This constructor is for when the Account does not exist in the app cache,
     * but does exist in the SDK cache.
     * @param account
     * @param platform
     */
    public Account(ConnectedDevicesAccount account, ConnectedDevicesPlatform platform) {
        mAccount = account;
        mState = AccountRegistrationState.IN_SDK_CACHE_ONLY;
        mPlatform = platform;
    }

    /**
     * This constructor is for when the Account is signed in.
     * @param helperAccount
     * @param state
     * @param platform
     */
    public Account(SigninHelperAccount helperAccount, AccountRegistrationState state, ConnectedDevicesPlatform platform) {
        // This account needs to be signed in, else the `Account` cannot be created
        mSignInHelper = helperAccount;
        mAccount = helperAccount.getAccount();
        mState = state;
        mPlatform = platform;
    }
    // endregion

    // region public instance methods
    /**
     * Perform all actions required to have this account signed in, added to the
     * ConnectedDevicesPlatform.AccountManager and registered with the Rome platform.
     * @param context Application context
     * @return The async result for this operation
     */
    public AsyncOperation<Boolean> prepareAccountAsync(final Context context) {
        // Accounts can be in 3 different scenarios:
        // 1: cached account in good standing (initialized in the SDK and our token cache).
        // 2: account missing from the SDK but present in our cache: Add and initialize account.
        // 3: account missing from our cache but present in the SDK. Log the account out async

        // Subcomponents (e.g. UserDataFeed) can only be initialized when an account is in both the app cache
        // and the SDK cache.
        // For scenario 1, initialize our subcomponents.
        // For scenario 2, subcomponents will be initialized after InitializeAccountAsync registers the account with the SDK.
        // For scenario 3, InitializeAccountAsync will unregister the account and subcomponents will never be initialized.
        switch (mState) {
            // Scenario 1
            case IN_APP_CACHE_AND_SDK_CACHE:
                initializeSubcomponents(context);
                return registerAccountWithSdkAsync();
            // Scenario 2
            case IN_APP_CACHE_ONLY: {
                // Add the this account to the ConnectedDevicesPlatform.AccountManager
                return mPlatform.getAccountManager().addAccountAsync(mAccount).thenComposeAsync((ConnectedDevicesAddAccountResult result) -> {
                    // We failed to add the account, so exit with a failure to prepare bool
                    if (result.getStatus() != ConnectedDevicesAccountAddedStatus.SUCCESS) {
                        Log.e(TAG, "Failed to add account " + mAccount.getId() + " to the AccountManager due to " + result.getStatus());
                        return AsyncOperation.completedFuture(false);
                    }

                    // Set the registration state of this account as in both app and sdk cache
                    mState = AccountRegistrationState.IN_APP_CACHE_AND_SDK_CACHE;
                    initializeSubcomponents(context);
                    return registerAccountWithSdkAsync();
                });
            }
            // Scenario 3
            case IN_SDK_CACHE_ONLY:
                // Remove the account from the SDK since the app has no knowledge of it
                mPlatform.getAccountManager().removeAccountAsync(mAccount);
                // This account could not be prepared
                return AsyncOperation.completedFuture(false);
            default:
                // This account could not be prepared
                return AsyncOperation.completedFuture(false);
        }
    }

    /**
     * Performs non-blocking registrations for this account, which are
     * for notifications then for the relay SDK.
     * @return The async result for this operation
     */
    public AsyncOperation<Boolean> registerAccountWithSdkAsync() {
        if (mState != AccountRegistrationState.IN_APP_CACHE_AND_SDK_CACHE) {
            AsyncOperation<Boolean> toReturn = new AsyncOperation<>();
            toReturn.completeExceptionally(new IllegalStateException("Cannot register this account due to bad state: " + mAccount.getId()));
            return toReturn;
        }

        // Grab the shared GCM/FCM notification token from this app's BroadcastReceiver
        return GcmNotificationReceiver.getNotificationRegistrationAsync().thenComposeAsync((ConnectedDevicesNotificationRegistration notificationRegistration) -> {
            // Perform the registration using the NotificationRegistration
            return mPlatform.getNotificationRegistrationManager().registerAsync(mAccount, notificationRegistration)
                .thenComposeAsync((ConnectedDevicesNotificationRegistrationResult result) -> {
                    // It would be a good idea for apps to take a look at the different statuses here and perhaps attempt some sort of remediation.
                    // For example, web failure may indicate that a web service was temporarily in a bad state and retries may be successful.
                    if (result.getStatus() != ConnectedDevicesNotificationRegistrationStatus.SUCCESS) {
                        Log.e(TAG, "Failed to perform notification registration for account: " + mAccount.getId());
                        return AsyncOperation.completedFuture(false);
                    }

                    Log.i(TAG, "Successfully performed notification registration for account:" + mAccount.getId());
                    // Perform the relay SDK registration by saving the RemoteSystemAppRegistration object
                    return mRegistration.publishAsync().thenApplyAsync((RemoteSystemAppRegistrationPublishResult publishResult) -> {
                        // It would be a good idea for apps to take a look at the different statuses here and perhaps attempt some sort of remediation.
                        // For example, no network may mean that the app should retry once connectivity is re-established.
                        Log.v(TAG, "RemoteSystemHostRegistration was published with result: " + result.getStatus().toString());
                        return publishResult.getStatus() == RemoteSystemAppRegistrationPublishStatus.SUCCESS;
                    });
                });
        });
    }

    /**
     * Perform any initial per account immediate initialization work.
     * @param context Application context
     */
    public void initializeSubcomponents(final Context context) {
        if (mState != AccountRegistrationState.IN_APP_CACHE_AND_SDK_CACHE) {
            throw new IllegalStateException("Cannot initialize subcomponents of this account due to bad state: " + mAccount.getId());
        }

        // Create our attributes, a timestamp for registration and package,
        // used to identity the RemoteSystemApp and sort by newest
        Map<String, String> attributes = new TreeMap<String, String>() {
            {
                put(TIMESTAMP_KEY, getInitialRegistrationDateTime(context));
                put(PACKAGE_KEY, PACKAGE_VALUE);
            }
        };

        // Create our AppService providers
        ArrayList<AppServiceProvider> appServiceProviders = new ArrayList<>();
        appServiceProviders.add(new PingPongService());
        appServiceProviders.add(new EchoService());

        // Create our LaunchUri provider 
        LaunchUriProvider launchUriProvider = new SimpleLaunchHandler();

        // Save the RemoteSystemAppRegistration to be "saved" later
        mRegistration = createRemoteSystemAppRegistration(attributes, appServiceProviders, launchUriProvider);
    }

    /**
     * Get an access token for this account which satisfies the given scopes 
     * @param scopes Scopes the access token must have been requested with
     * @return The async result for this operation
     */
    public AsyncOperation<String> getAccessTokenAsync(final List<String> scopes) {
        return mSignInHelper.getAccessTokenAsync(scopes);
    }

    /**
     * Tear down and sign this account out
     * @param activity Application activity
     * @return The async result for this operation
     */
    public AsyncOperation<ConnectedDevicesAccount> logoutAsync(Activity activity) {
        clearSubcomponents();
        return mSignInHelper.signOut(activity);
    }

    /**
     * Get the ConnectedDevicesAccount
     * @return account
     */
    public ConnectedDevicesAccount getAccount() {
        return mAccount;
    }

    /**
     * Get the AccountRegistrationState
     * @return account
     */
    public AccountRegistrationState getRegistrationState() {
        return mState;
    }
    // endregion

    // region private instance methods
    /**
     * Create a RemoteSystemAppRegistration with the given state
     * @param attributes Sets the key/value attributes of the application registration.
     * @param appServiceProviders Sets the app service providers associated with the registration.
     * @param launchUriProvider Sets the URI launcher for this application.
     * @return RemoteSystemAppRegistration
     */
    private RemoteSystemAppRegistration createRemoteSystemAppRegistration(Map<String, String> attributes, ArrayList<AppServiceProvider> appServiceProviders, LaunchUriProvider launchUriProvider) {
        // Create the hosting registration object for this account
        RemoteSystemAppRegistration registration = RemoteSystemAppRegistration.getForAccount(mAccount, mPlatform);

        // Set this registration's attributes
        registration.setAttributes(attributes);

        // Set the given AppService and LaunchUri Providers to the registration
        if (appServiceProviders != null) {
            registration.setAppServiceProviders(appServiceProviders);
        }
        if (launchUriProvider != null) {
            registration.setLaunchUriProvider(launchUriProvider);
        }

        return registration;
    }

    /**
     * Grab the initial registration date-time if one is found, otherwise generate a new one.
     * @param context Application context
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

    /**
     * If your app needs to stop using a sub component for some reason,
     * this would be a good place to reset a user data feed for instance.
     */
    private void clearSubcomponents() {
    }
    // endregion
}
