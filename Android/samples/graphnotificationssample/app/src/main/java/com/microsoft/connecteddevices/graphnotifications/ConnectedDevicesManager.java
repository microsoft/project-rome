//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.connecteddevices.graphnotifications;

import android.app.Activity;
import android.content.Context;
import android.util.ArrayMap;
import android.util.Log;

import com.microsoft.connecteddevices.AsyncOperation;
import com.microsoft.connecteddevices.ConnectedDevicesAccessTokenRequest;
import com.microsoft.connecteddevices.ConnectedDevicesAccessTokenRequestedEventArgs;
import com.microsoft.connecteddevices.ConnectedDevicesAccessTokenInvalidatedEventArgs;
import com.microsoft.connecteddevices.ConnectedDevicesAccount;
import com.microsoft.connecteddevices.ConnectedDevicesAccountManager;
import com.microsoft.connecteddevices.ConnectedDevicesAccountType;
import com.microsoft.connecteddevices.ConnectedDevicesAddAccountResult;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationRegistration;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationRegistrationStateChangedEventArgs;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationType;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationRegistrationManager;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationRegistrationState;
import com.microsoft.connecteddevices.ConnectedDevicesPlatform;
import com.microsoft.connecteddevices.signinhelpers.AADSigninHelperAccount;
import com.microsoft.connecteddevices.signinhelpers.MSASigninHelperAccount;
import com.microsoft.connecteddevices.signinhelpers.SigninHelperAccount;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This is a singleton object which holds onto the app's ConnectedDevicesPlatform and handles account management.
 */
public class ConnectedDevicesManager {
    // region Member Variables
    private final String TAG = ConnectedDevicesManager.class.getName();

    private List<Account> mAccounts;
    private RomeNotificationReceiver mNotificationReceiver;
    private ConnectedDevicesPlatform mPlatform;

    private static ConnectedDevicesManager sConnectedDevicesManager;
    // endregion

    // region Constructors
    /**
     * This is a singleton object which holds onto the app's ConnectedDevicesPlatform and handles account management. 
     * @param context Application context
     */
    private ConnectedDevicesManager(Context context) {
        // Initialize list of known accounts
        mAccounts = new ArrayList<Account>();

        // Create the NotificationReceiver
        mNotificationReceiver = new RomeNotificationReceiver(context);

        // Create Platform
        mPlatform = new ConnectedDevicesPlatform(context);

        // Create a final reference to the list of accounts
        final List<Account> accounts = mAccounts;

        // Subscribe to the AccessTokenRequested event
        mPlatform.getAccountManager().accessTokenRequested().subscribe((accountManager, args) -> onAccessTokenRequested(accountManager, args, accounts));

        // Subscribe to AccessTokenInvalidated event
        mPlatform.getAccountManager().accessTokenInvalidated().subscribe((accountManager, args) -> onAccessTokenInvalidated(accountManager, args, accounts));

        // Subscribe to NotificationRegistrationStateChanged event
        mPlatform.getNotificationRegistrationManager().notificationRegistrationStateChanged().subscribe((notificationRegistrationManager, args) -> onNotificationRegistrationStateChanged(notificationRegistrationManager, args, accounts));

        // Start the platform as we have subscribed to the events it can raise
        mPlatform.start();

        // Pull the accounts from our app's cache and synchronize the list with the apps cached by 
        // ConnectedDevicesPlatform.AccountManager.
        List<Account> deserializedAccounts = deserializeAccounts(context);

        // Finally initialize the accounts. This will refresh registrations when needed, add missing accounts,
        // and remove stale accounts from the ConnectedDevicesPlatform AccountManager. The AsyncOperation associated
        // with all of this asynchronous work need not be waited on as any sub component work will be accomplished
        // in the synchronous portion of the call. If your app needs to sequence when other apps can see this app's registration
        // (i.e. when RemoteSystemAppRegistration SaveAsync completes) then it would be useful to use the AsyncOperation returned by
        // prepareAccountsAsync
        prepareAccounts(deserializedAccounts, context);
    }
    // endregion

    // region public static methods
    public static synchronized ConnectedDevicesManager getConnectedDevicesManager(Context context) {
        if (sConnectedDevicesManager == null) {
            sConnectedDevicesManager = new ConnectedDevicesManager(context);
        }
        return sConnectedDevicesManager;
    }
    // endregion

    // region public instance methods

    /**
     * Get the ConnectedDevicesPlatform owned by this ConnectedDevicesManager.
     * @return Platform
     */
    public ConnectedDevicesPlatform getPlatform() {
        return mPlatform;
    }

    /**
     * Attempt to ensure there is a signed in MSA Account
     * @param activity Application activity
     * @return The async result for when this operation completes
     */
    public synchronized AsyncOperation<Boolean> signInMsaAsync(final Activity activity) {
        // Create a SigninHelperAccount with a client id for msa, a map of requested scopes to override, and the context
        final Map<String, String[]> msaScopeOverrides = new ArrayMap<>();
        msaScopeOverrides.put("https://activity.windows.com/UserActivity.ReadWrite.CreatedByApp",
                new String[] { "https://activity.windows.com/UserActivity.ReadWrite.CreatedByApp",
                        "https://activity.windows.com/Notifications.ReadWrite.CreatedByApp"});
        SigninHelperAccount signInHelper = new MSASigninHelperAccount(Secrets.MSA_CLIENT_ID, msaScopeOverrides, (Context)activity);

        if (signInHelper.isSignedIn()) {
            Log.i(TAG, "Already signed in with a MSA account");
            return AsyncOperation.completedFuture(true);
        }

        Log.i(TAG, "Signing in with a MSA account");

        // Call signIn, which may prompt the user to enter credentials or just retreive a cached token if they exist and are valid
        return signInHelper.signIn(activity).thenComposeAsync((ConnectedDevicesAccount account) -> {
            // Prepare the account, adding it to the list of app's cached accounts is prepared successfully
            return prepareAccountAsync(new Account(signInHelper, AccountRegistrationState.IN_APP_CACHE_ONLY, mPlatform), (Context)activity);
        });
    }

    /**
     * Attempt to ensure there is a signed in MSA Account
     * @param activity Application activity
     * @return The async result for when this operation completes
     */
    public synchronized AsyncOperation<Boolean> signInAadAsync(final Activity activity) {
        // Create a SigninHelperAccount with a client id for msa, a map of requested scopes to override, and the context
        SigninHelperAccount signInHelper = new AADSigninHelperAccount(Secrets.AAD_CLIENT_ID, Secrets.AAD_REDIRECT_URI, (Context)activity);

        if (signInHelper.isSignedIn()) {
            Log.i(TAG, "Already signed in with a AAD account");
            return AsyncOperation.completedFuture(true);
        }

        Log.i(TAG, "Signing in in with a AAD account");

        // Call signIn, which may prompt the user to enter credentials or just retreive a cached token if they exist and are valid
        return signInHelper.signIn(activity).thenComposeAsync((ConnectedDevicesAccount account) -> {
            // Prepare the account, adding it to the list of app's cached accounts is prepared successfully
            return prepareAccountAsync(new Account(signInHelper, AccountRegistrationState.IN_APP_CACHE_ONLY, mPlatform), (Context)activity);
        });
    }

    /**
     * Sign out and remove the given Account from the ConnectedDevicesManager
     * @param activity Application activity
     * @return The async result for when this operation completes
     */
    public synchronized AsyncOperation<Boolean> logout(Activity activity) {
        // First remove this account from the list of "ready to go" accounts so it cannot be used while logging out
        Account accountToRemove = getSignedInAccount();
        mAccounts.remove(accountToRemove);

        // Now log out this account
        return accountToRemove.logoutAsync(activity).thenComposeAsync((ConnectedDevicesAccount account) -> {
            Log.i(TAG, "Successfully signed out account: " + account.getId());
            return AsyncOperation.completedFuture(true);
        });
    }

    /**
     * Get a list of "ready-to-go" accounts owned by this ConnectedDevicesManager.
     * @return accounts
     */
    public Account getSignedInAccount() {

        // Compare the app cached account to find a match in the sdk cached accounts
        if (mAccounts.size() > 0) {
            return mAccounts.get(0);
        }

        Log.e(TAG, "No signed in account found!");
        return null;
    }

    /**
     * Create a NotificationRegistration using the notification token gained from GCM/FCM.
     * @param token Notification token gained by the BroadcastReceiver
     */
    public synchronized void setNotificationRegistration(final String token) {
        // Get the NotificationRegistrationManager from the platform
        ConnectedDevicesNotificationRegistrationManager registrationManager = mPlatform.getNotificationRegistrationManager();

        // Create a NotificationRegistration obect to store all notification information
        ConnectedDevicesNotificationRegistration registration = new ConnectedDevicesNotificationRegistration();
        registration.setType(ConnectedDevicesNotificationType.FCM);
        registration.setToken(token);
        registration.setAppId(Secrets.FCM_SENDER_ID);
        registration.setAppDisplayName("GraphNotificationsSample");

        Log.i(TAG, "Completing the RomeNotificationReceiver operation with token: " + token);

        // For each prepared account, register for notifications
        for (final Account account : mAccounts) {
            registrationManager.registerForAccountAsync(account.getAccount(), registration)
                    .whenCompleteAsync((Boolean success, Throwable throwable) -> {
                        if (throwable != null) {
                            Log.e(TAG, "Exception encountered in registerForAccountAsync", throwable);
                        } else if (!success) {
                            Log.e(TAG, "Failed to register account " + account.getAccount().getId() + " for cloud notifications!");
                        } else {
                            Log.i(TAG, "Successfully registered account " + account.getAccount().getId() + " for cloud notifications");
                        }
                    });
        }

        // The two cases of receiving a new notification token are:
        // 1. A notification registration is asked for and now it is available. In this case there is a pending promise that was made
        //    at the time of requesting the information. It now needs to be completed.
        // 2. The account is already registered but for whatever reason the registration changes (GCM/FCM gives the app a new token)
        //
        // In order to most cleanly handle both cases set the new notification information and then trigger a re registration of all accounts
        // that are in good standing.
        RomeNotificationReceiver.setNotificationRegistration(registration);

        // For all the accounts which have been prepared successfully, perform SDK registration
        for (Account account : mAccounts) {
            if (account.getRegistrationState() == AccountRegistrationState.IN_APP_CACHE_AND_SDK_CACHE) {
                account.registerAccountWithSdkAsync();
            }
        }
    }

    // endregion

    // region private instance methods
    /**
     * Pull the accounts from our app's cache and synchronize the list with the 
     * apps cached by ConnectedDevicesPlatform.AccountManager.
     * @param context Application context
     * @return List of accounts from the app and SDK's cache
     */
    private List<Account> deserializeAccounts(Context context) {
        // Since our helper lib can only cache 1 app at a time, we create sign-in helper,
        // which does user account and access token management for us. Takes three parameters:
        // a client id for msa, a map of requested auto scopes to override, and the context
        SigninHelperAccount signInHelper = new MSASigninHelperAccount(Secrets.MSA_CLIENT_ID, new ArrayMap<String, String[]>(), context);

        // Get all of the ConnectedDevicesPlatform's added accounts
        List<ConnectedDevicesAccount> sdkCachedAccounts = mPlatform.getAccountManager().getAccounts();

        List<Account> returnAccounts = new ArrayList<Account>();

        // If there is a signed in account in the app's cache, find it exists in the SDK's cache
        if (signInHelper.isSignedIn()) {
            // Check if the account is also present in ConnectedDevicesPlatform.AccountManager.
            ConnectedDevicesAccount sdkCachedAccount = findFirst(sdkCachedAccounts, account -> accountsMatch(signInHelper.getAccount(), account));

            AccountRegistrationState registrationState;
            if (sdkCachedAccount != null) {
                // Account found in the SDK cache, remove it from the list of sdkCachedAccounts. After 
                // all the appCachedAccounts have been processed any accounts remaining in sdkCachedAccounts
                // are only in the SDK cache, and should be removed.
                registrationState = AccountRegistrationState.IN_APP_CACHE_AND_SDK_CACHE;
                sdkCachedAccounts.remove(sdkCachedAccount);
            } else {
                // Account not found in the SDK cache. Later when we initialize the Account,
                // it will be added to the SDK cache and perform registration.
                registrationState = AccountRegistrationState.IN_APP_CACHE_ONLY;
            }

            // Add the app's cached account with the correct registration state
            returnAccounts.add(new Account(signInHelper, registrationState, mPlatform));
        }

        // Add all the accounts which exist only in the SDK
        for (ConnectedDevicesAccount account : sdkCachedAccounts) {
            returnAccounts.add(new Account(account, mPlatform));
        }

        return returnAccounts;
    }

    /**
     * Replacement for the java.util.function.Predicate to support pre Java 8 / API 24.
     */
    interface Predicate<T> {
        public boolean test(T t);
    }

    /**
     * Replacement for stream.filter.findFirst to support pre Java 8 / API 24.
     * @param list List to search
     * @param predicate Predicate to use against the given list
     * @return First item matching the given predicate, null if none found
     */
    private static <T> T findFirst(List<T> list, Predicate<? super T> predicate) {
        for (T item : list) {
            if (predicate.test(item)) {
                return item;
            }
        }

        return null;
    }

    /**
     * Matcher function to compare ConnectedDevicesAccounts are equal
     * @param account1 ConnectedDevicesAccount 1 
     * @param account2 ConnectedDevicesAccount 2 
     * @return Boolean of if the given accounts match
     */
    private static boolean accountsMatch(ConnectedDevicesAccount account1, ConnectedDevicesAccount account2) {
        String accountId1 = account1.getId();
        ConnectedDevicesAccountType accountType1 = account1.getType();

        String accountId2 = account2.getId();
        ConnectedDevicesAccountType accountType2 = account2.getType();

        return accountId2.equals(accountId1) && accountType2.equals(accountType1);
    }
 
    /**
     * Prepare the accounts; refresh registrations when needed, add missing accounts and remove stale accounts from the ConnectedDevicesPlatform.AccountManager.
     * @param context Application context
     * @return A async operation that will complete when all accounts are prepared
     */
    private AsyncOperation<Void> prepareAccounts(List<Account> accounts, Context context) {
        List<AsyncOperation<Boolean>> operations = new ArrayList<>();

        // Kick off all the account preparation and store the AsyncOperations
        for (Account account : accounts) {
            operations.add(prepareAccountAsync(account, context));
        }

        // Return an operation that will complete when all of the operations complete.
        return AsyncOperation.allOf(operations.toArray(new AsyncOperation[operations.size()]));
    }

    /**
     * Attempt to prepare the account. If the account was prepared successfully, add it to the list of "ready to use" accounts.
     * @param context Application context
     * @return AsyncOperation with the exception captured
     */
    private AsyncOperation<Boolean> prepareAccountAsync(Account account, Context context) {
        Log.v(TAG, "Preparing account: " + account.getAccount().getId());

        // Add the account to the list of available accounts
        mAccounts.add(account);

        // Prepare the account, removing it from the list of accounts if it failed
        return account.prepareAccountAsync(context).thenComposeAsync((success) -> {
            // If an exception is raised or we gracefully fail to prepare the account, remove it
            if (success) {
                Log.i(TAG, "Account: " + account.getAccount().getId() + " is ready-to-go");
            } else {
                mAccounts.remove(account);
                Log.w(TAG, "Account: " + account.getAccount().getId() + " is not ready, removed from the list of ready-to-go accounts!");
            }

            // Return the success of the account preparation
            return AsyncOperation.completedFuture(success);
        }).exceptionally((Throwable throwable) -> {
            mAccounts.remove(account);
            Log.e(TAG, "Account: " + account.getAccount().getId() + " is not ready, removed from the list of ready-to-go accounts as an exception was encountered", throwable);
            // Return the account preparation was not successful
            return false;
        });
    }

    /**
     * This event is fired when there is a need to request a token. This event should be subscribed and ready to respond before any request is sent out.
     * @param sender ConnectedDevicesAccountManager which is making the request
     * @param args Contains arguments for the event
     * @param accounts List of accounts to search for
     */
    private void onAccessTokenRequested(ConnectedDevicesAccountManager sender, ConnectedDevicesAccessTokenRequestedEventArgs args, List<Account> accounts) {
        ConnectedDevicesAccessTokenRequest request = args.getRequest();
        List<String> scopes = request.getScopes();

        // Compare the app cached account to find a match in the sdk cached accounts
        Account account = findFirst(accounts, acc -> accountsMatch(request.getAccount(), acc.getAccount()));

        // We always need to complete the request, even if a matching account is not found
        if (account == null) {
            Log.e(TAG, "Failed to find a SigninHelperAccount matching the given account for the token request");
            request.completeWithErrorMessage("The app could not find a matching ConnectedDevicesAccount to get a token");
            return;
        }

        // Complete the request with a token
        account.getAccessTokenAsync(scopes)
            .thenAcceptAsync((String token) -> {
                request.completeWithAccessToken(token);
            }).exceptionally(throwable -> {
                request.completeWithErrorMessage("The Account could not return a token with those scopes");
                return null;
            });
    }

    /**
     * This event is fired when a token consumer reports a token error. The token provider needs to
     * either refresh their token cache or request a new user login to fix their account setup.
     * If access token in invalidated, refresh token and renew access token.
     * @param sender ConnectedDevicesAccountManager which is making the request
     * @param args Contains arguments for the event
     * @param accounts List of accounts to search for
     */
    private void onAccessTokenInvalidated(ConnectedDevicesAccountManager sender, ConnectedDevicesAccessTokenInvalidatedEventArgs args, List<Account> accounts) {
        Log.i(TAG, "Token invalidated for account: " + args.getAccount().getId());
    }

    /**
     * Event for when the registration state changes for a given account.
     * @param sender ConnectedDevicesNotificationRegistrationManager which is making the request
     * @param args Contains arguments for the event
     * @param accounts List of accounts to search for
     */
    private void onNotificationRegistrationStateChanged(ConnectedDevicesNotificationRegistrationManager sender, ConnectedDevicesNotificationRegistrationStateChangedEventArgs args, List<Account> accounts) {
        // If notification registration state is expiring or expired, re-register for account again.
        ConnectedDevicesNotificationRegistrationState state = args.getState();
        switch (args.getState()) {
            case UNREGISTERED:
                Log.w(TAG, "Notification registration state is unregistered for account: " + args.getAccount().getId());
                break;
            case REGISTERED:
                Log.i(TAG, "Notification registration state is registered for account: " + args.getAccount().getId());
                break;
            case EXPIRING: // fallthrough
            case EXPIRED:
            {
                 // Because the notificaiton registration is expiring, the per account registration work needs to be kicked off again.
                 // This means registering with the NotificationRegistrationManager as well as any sub component work like RemoteSystemAppRegistration.
                Log.i(TAG, "Notification " + args.getState() + " for account: " + args.getAccount().getId());
                Account account = findFirst(accounts, acc -> accountsMatch(args.getAccount(), acc.getAccount()));

                // If the account has been prepared for use then re-register the account with SDK
                if (account != null && account.getRegistrationState() == AccountRegistrationState.IN_APP_CACHE_AND_SDK_CACHE) {
                    account.registerAccountWithSdkAsync();
                }
                break;
            }
            default:
                break;
        }

    }
    // endregion
}
