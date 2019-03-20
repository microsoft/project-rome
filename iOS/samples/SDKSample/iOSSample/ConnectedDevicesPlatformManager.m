//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "ConnectedDevicesPlatformManager.h"
#import <ConnectedDevicesRemoteSystems/ConnectedDevicesRemoteSystems.h>
#import <ConnectedDevicesRemoteSystemsCommanding/ConnectedDevicesRemoteSystemsCommanding.h>
#import <ConnectedDevicesUserData/ConnectedDevicesUserData.h>
#import <ConnectedDevicesUserDataUserActivities/ConnectedDevicesUserDataUserActivities.h>

#import "InboundRequestLogger.h"
#import "MSAAccount.h"
#import "Secrets.h"

@implementation APNSManager {
    NSMutableDictionary<NSString*, PMKAdapter>* _pendingOperationBlocks;
    MCDConnectedDevicesNotificationRegistration* _notificationRegistration;
}

- (instancetype)init {
    if (self = [super init]) {
        _pendingOperationBlocks = [NSMutableDictionary new];
    }
    
    return self;
}

- (AnyPromise*)getNotificationRegistrationAsync:(Account*)account {
    @synchronized (self) {
        if (_notificationRegistration != nil) {
            return [AnyPromise promiseWithValue:_notificationRegistration];
        } else {
            // If there isn't already a notification registration available, set up a pending operation to complete when one does
            // become available. NOTE: this code uses the accountId as the key which is not guaranteed to be unique across account types.
            // If your app uses multiple account types that may have conflicting ids, use a different unique key.
            AnyPromise* pendingOperation = [AnyPromise promiseWithAdapterBlock:^(PMKAdapter _Nonnull adapter) {
                [self->_pendingOperationBlocks setObject:adapter forKey:account.mcdAccount.accountId];
            }];
            
            return pendingOperation;
        }
    }
}

- (void)setNotificationRegistration:(MCDConnectedDevicesNotificationRegistration*)registration accounts:(NSArray<Account*>*)accounts{
    NSMutableDictionary<NSString*, PMKAdapter>* pendingOperationBlocksToComplete;
    BOOL needsUpdating = NO;
    @synchronized (self) {
        // NOTE: this code assumes that the token is the only piece of the notificaiton registration that is changing.
        // If your app uses multiple notification providers, wants to change other information, etc. this logic needs
        // to be updated to accomdate those situations.
        needsUpdating = [_notificationRegistration.token isEqualToString:registration.token];
        pendingOperationBlocksToComplete = _pendingOperationBlocks;
        _pendingOperationBlocks = [NSMutableDictionary new];
        _notificationRegistration = registration;
    }
    
    // Complete any pending requests to get the notificaiton registration.
    for (NSString* accountKey in pendingOperationBlocksToComplete) {
        PMKAdapter adapter = pendingOperationBlocksToComplete[accountKey];
        adapter(registration, nil);
    }

    if (needsUpdating) {
        for (Account* account in accounts) {
            // Only reregister the accounts that didn't have pending operations get completed just above. 
            // Also make sure that only accounts in good standing are registered.
            // 
            // NOTE: this code uses the accountId as the key which is not guaranteed to be unique across account types.
            // If your app uses multiple account types that may have conflicting ids, use a different unique key.
            if (account.state == AccountRegistrationStateInAppCacheAndSdkCache && nil == [pendingOperationBlocksToComplete objectForKey:account.mcdAccount.accountId]) {
                [account registerWithSdkAsync];
            }
        }
    }
}

@end

@interface Account () {
    MSAAccount* _msaAccount;
}

- (void)clearSubcomponents;

@end

@implementation Account

- (instancetype)initWithMCDAccount:(MCDConnectedDevicesAccount*)mcdAccount state:(AccountRegistrationState)state platform:(MCDConnectedDevicesPlatform*)platform apnsManager:(APNSManager*)apnsManager {
    if (self = [super init]) {
        self.mcdAccount = mcdAccount;
        self.state = state;
        self.platform = platform;
        self.apnsManager = apnsManager;
    }
    
    return self;
}

- (instancetype)initWithMSAAccount:(MSAAccount *)msaAccount platform:(MCDConnectedDevicesPlatform*)platform apnsManager:(APNSManager*)apnsManager {
    if (self = [super init]) {
        _msaAccount = msaAccount;
        if (!_msaAccount.isSignedIn) {
            return nil;
        }
        
        self.mcdAccount = _msaAccount.mcdAccount;
        self.platform = platform;
        self.apnsManager = apnsManager;
    }
    
    return self;
}

- (AnyPromise*)prepareAccountAsync:(ConnectedDevicesPlatformManager*)platformManager {
    // Accounts can be in 3 different scenarios:
    // 1: cached account in good standing (initialized in the SDK and our token cache).
    // 2: account missing from the SDK but present in our cache: Add and initialize account.
    // 3: account missing from our cache but present in the SDK. Log the account out async.
    
    // Subcomponents (e.g. UserDataFeed or RemoteSystemAppRegistration) should only be initialized when an account is in both the app cache
    // and the SDK cache.
    // For scenario 1, initialize our subcomponents. This is the only case that can have incoming notifications and thus
    //    the asynchronous portion of preparing an account need not be waited before processing incoming notifications.
    // For scenario 2, subcomponents will be initialized after InitializeAccountAsync registers the account with the SDK. Because this is a new account, an incoming notification could only
    //    be for this account if was an account that was previously added, then removed, and now being added back. In this case its just a race condition that
    //    does not matter if it can't be routed as the sender is using out of date information that just happens to align with what is soon to be the new information.
    // For scenario 3, InitializeAccountAsync will unregister the account and subcomponents will never be initialized (and therefore any notifications for it will be dropped).
    if (self.state == AccountRegistrationStateInAppCacheAndSdkCache) {
        // Scenario 1
        [self initializeSubcomponents];
        return [self registerWithSdkAsync];
    } else if (self.state == AccountRegistrationStateInAppCacheOnly){
        // Scenario 2, add the account to the SDK
        AnyPromise* addAccountPromise = [AnyPromise promiseWithAdapterBlock:^(PMKAdapter _Nonnull adapter) {
            [self.platform.accountManager addAccountAsync:self.mcdAccount callback:adapter];
        }];
        
        addAccountPromise.catch(^ {
           [platformManager.accounts removeObject:self];
        });
        
        return addAccountPromise.then(^(MCDConnectedDevicesAddAccountResult* result) {
            switch (result.status) {
                case MCDConnectedDevicesAccountAddedStatusSuccess:
                    self.state = AccountRegistrationStateInAppCacheAndSdkCache;
                    [self initializeSubcomponents];
                    return [self registerWithSdkAsync];
                case MCDConnectedDevicesAccountAddedStatusErrorServiceFailed:
                    // If the service failed, ideally this could be tried again later but this
                    // simple app will just fail.
                    break;
                case MCDConnectedDevicesAccountAddedStatusErrorNoNetwork:
                    // If there is no network, ideally this could be tried again later but this
                    // simple app will just fail.
                    break;
                case MCDConnectedDevicesAccountAddedStatusErrorTokenRequestFailed:
                    // Token request failed! Make sure that the token library is properly configured.
                    break;
                case MCDConnectedDevicesAccountAddedStatusErrorNoTokenRequestSubscriber:
                    // This means that the event is no longer being listened to and therefore can't complete the
                    // request. This indidcates a programming error.
                    break;
                case MCDConnectedDevicesAccountAddedStatusErrorUnknown:
                    // This means that something totally unknown happened. This should not happen in normal operation.
                    break;
            }
            
            @throw NSGenericException;
        });
    } else {
        // Scenario 3, remove the account from the SDK
        return [AnyPromise promiseWithAdapterBlock:^(PMKAdapter _Nonnull adapter) {
            [self.platform.accountManager removeAccountAsync:self.mcdAccount callback:adapter];
        }].ensure(^{
            [platformManager.accounts removeObject:self];
        });
    }
}

- (AnyPromise*)registerWithSdkAsync
{

    if (self.state != AccountRegistrationStateInAppCacheAndSdkCache) {
        return [AnyPromise promiseWithValue:[NSError errorWithDomain:@"AccountException" code:0 userInfo:nil]];
    }

    return [self.apnsManager getNotificationRegistrationAsync:self].then(^(MCDConnectedDevicesNotificationRegistration* registration) {
        NSLog(@"Registering APNS with ConnectedDevicesPlatform");
        return [AnyPromise promiseWithAdapterBlock:^(PMKAdapter _Nonnull adapter) {
            [self.platform.notificationRegistrationManager registerAsync:self.mcdAccount registration:registration completion:adapter];
        }];
    }).then(^(MCDConnectedDevicesNotificationRegistrationResult* result) {

        // It would be a good idea for apps to take a look at the different statuses here and perhaps attempt some sort of remediation.
        // For example, web failure may indicate that a web service was temporarily in a bad state and retries may be successful.
        // 
        // NOTE: this approach was chosen rather than using NSError values to help separate "expected" / "retry-able" errors from real 
        // errors / exceptions and keep the error-channel logic clean and simple.
        if (result.status != MCDConnectedDevicesNotificationRegistrationStatusSuccess) {
            return [AnyPromise promiseWithValue:[NSError errorWithDomain:@"RegistrationError" code:result.status userInfo:nil]];
        }

        // Do operations that require notification registration now. Like publishing the RemoteSystemAppRegistration or saving UserDataFeed sync scopes
        // Until the RemoteSystemAppRegistration is successfully saved, any outgoing communication to remote apps may
        // not receive responses as the remote app will not necessarily know who it is communicating with.
        MCDRemoteSystemAppRegistration* registration =
        [MCDRemoteSystemAppRegistration getForAccount:self.mcdAccount platform:self.platform];
        
        [registration publishAsync:^(MCDRemoteSystemAppRegistrationPublishResult* publishResult, NSError* error) {
            // A more complete sample would properly gate any outbound communication until this point.
            if (error || (publishResult.status != MCDRemoteSystemAppRegistrationPublishStatusSuccess))
            {
                NSLog(@"Failed to register remote system");
            }
            else
            {
                NSLog(@"Successfully registered remote system.");
            }
        }];
        
        // For UserDataFeed, adjust the sync scopes so that the types the app cares about synced down. Until this completes, the app will not
        // get data of the desired type (UserActivities vs UserNotifications) and will not receive notifications when the data changes.
        MCDUserDataFeed* userDataFeed = [MCDUserDataFeed getForAccount:self.mcdAccount
                                                              platform:self.platform
                                                    activitySourceHost:CROSS_PLATFORM_APP_ID];

        NSArray<MCDUserDataFeedSyncScope*>* syncScopes = @[ [MCDUserActivityChannel syncScope] ];
        [userDataFeed subscribeToSyncScopesAsync:syncScopes
                                        callback:^(BOOL success __unused, NSError* _Nullable error __unused) {
                                            // Based on your app's needs this could be a good place to start syncing down activity feeds etc. 
                                        }];

        // This sample simply kicks off registration of the UserDataFeed and RemoteSystemAppRegistration but does not return a meaningful promise
        // here to gate other operations on completion on either / both of these steps. This is more scenario dependent on what operations the app cares
        // about.
        return [AnyPromise promiseWithValue:nil];
    });
}

- (AnyPromise*)signOutAsync
{
    // First remove the account out from the ConnectedDevices SDK. The SDK may call back for access tokens to perform
    // unregistration with services
    [self clearSubcomponents];
    return [AnyPromise promiseWithAdapterBlock:^(PMKAdapter _Nonnull adapter) {
        [self.platform.accountManager removeAccountAsync:self.mcdAccount callback:adapter];
    }].then(^{
        // After its gone from the sdk, it is safe to sign out from the token library and clean up the account list.
        self.state = AccountRegistrationStateInAppCacheOnly;
        return [AnyPromise promiseWithAdapterBlock:^(PMKAdapter _Nonnull adapter) {
            [_msaAccount signOutWithCompletionCallback:adapter];
        }];
    });
}

- (void)initializeSubcomponents {
    // Do initial per account immediate initialization work. Like creating the RemoteSystemAppRegistration or getting a UserDataFeed.
    // First up is the RemoteSystemAppRegistration
    MCDRemoteSystemAppRegistration* registration =
    [MCDRemoteSystemAppRegistration getForAccount:self.mcdAccount platform:self.platform];
    [registration setAttributes:@{ @"sample_key" : @"sample_value" }];
    
    NSMutableArray* providers = [NSMutableArray array];
    // Add each of the implemented AppServicesProviders to the registration
    AppServiceProvider* appServiceProvider = [[AppServiceProvider alloc] initWithDelegate:[InboundRequestLogger sharedInstance]];
    [providers addObject:appServiceProvider];
    
    [registration setLaunchUriProvider:[[LaunchUriProvider alloc] initWithDelegate:[InboundRequestLogger sharedInstance]]];
    [registration setAppServiceProviders:providers];
    
    // Now handle UserDataFeed
    MCDUserDataFeed* userDataFeed = [MCDUserDataFeed getForAccount:self.mcdAccount
                                                          platform:self.platform
                                                activitySourceHost:CROSS_PLATFORM_APP_ID];
    [userDataFeed.syncStatusChanged subscribe:^(MCDUserDataFeed* _Nonnull sender, MCDUserDataFeedSyncStatusChangedEventArgs* _Nonnull __unused args) {
        NSLog(@"SyncStatus is %ld", (long)sender.syncStatus);
    }];
}

- (void)clearSubcomponents {
    // If your app needs to stop using a sub component for some reason, this would be a good place to reset a user data feed for instance.
}

- (AnyPromise*)getAccessTokenAsync:(NSArray<NSString*>*)scopes {
    return [AnyPromise promiseWithAdapterBlock:^(PMKAdapter _Nonnull adapter) {
        [_msaAccount getAccessTokenForUserAccountIdAsync:self.mcdAccount.accountId scopes:scopes completion:adapter];
    }];
}

@end

@interface ConnectedDevicesPlatformManager ()
- (void)serializeAccountsToCache;
- (void)accountListChanged;
@end

@implementation ConnectedDevicesPlatformManager

+ (instancetype)sharedInstance {
    static dispatch_once_t onceToken;
    static ConnectedDevicesPlatformManager* sharedInstance;
    
    dispatch_once(&onceToken, ^{ sharedInstance = [[ConnectedDevicesPlatformManager alloc] init]; });
    return sharedInstance;
}

- (instancetype)init {
    if (self = [super init]) {
        self.accounts = [NSMutableArray new];
        self.apnsManager = [APNSManager new];
        
        // Construct and initialize a platform. All we are doing here is hooking up event handlers before
        // calling ConnectedDevicesPlatform Start. After Start is called events may begin to fire.
        self.platform = [MCDConnectedDevicesPlatform new];
        
        __weak ConnectedDevicesPlatformManager* weakSelf = self;
        [weakSelf.platform.accountManager.accessTokenRequested subscribe:^(MCDConnectedDevicesAccountManager* _Nonnull manager, MCDConnectedDevicesAccessTokenRequestedEventArgs* _Nonnull args) {
            NSLog(@"Token requested by platform for %@ and %@", args.request.account.accountId, [args.request.scopes componentsJoinedByString:@","]);
            
            Account* account = nil;
            if (weakSelf.accounts.count > 0) {
                NSInteger index = [weakSelf.accounts indexOfObjectPassingTest:^BOOL (Account* account, NSUInteger index, BOOL* stop) {
                    return ([account.mcdAccount.accountId isEqualToString: args.request.account.accountId] && account.mcdAccount.type == args.request.account.type) ? YES : NO;
                }];
                
                if (index != NSNotFound) {
                    account = [weakSelf.accounts objectAtIndex:index];
                }
            }

            if (account != nil) {
                [account getAccessTokenAsync:args.request.scopes].then(^(NSString* token) {
                    NSLog(@"Token Request Succeeded");
                    [args.request completeWithAccessToken:token];
                }).catch(^(NSError* error) {
                    NSLog(@"Token Request Failed. Could not get token.");
                    [args.request completeWithErrorMessage:error.localizedDescription];
                });
            } else {
                NSLog(@"Token Request Failed. Could not find account.");
                [args.request completeWithErrorMessage:@"Token Request Failed. Could not find account."];
            }
        }];
        
        [self.platform.accountManager.accessTokenInvalidated
         subscribe:^(MCDConnectedDevicesAccountManager* _Nonnull manager __unused,
                     MCDConnectedDevicesAccessTokenInvalidatedEventArgs* _Nonnull request) {
             NSLog(@"Token invalidated for account: %@", request.account.accountId);
         }];
        
        [self.platform.notificationRegistrationManager.notificationRegistrationStateChanged subscribe:^(MCDConnectedDevicesNotificationRegistrationManager* _Nonnull manager __unused, MCDConnectedDevicesNotificationRegistrationStateChangedEventArgs* _Nonnull args) {
             switch (args.state) {
                 case MCDConnectedDevicesNotificationRegistrationStateRegistered:
                     NSLog(@"Notifications registered for account ID: %@", args.account.accountId);
                     break;
                 case MCDConnectedDevicesNotificationRegistrationStateUnregistered:
                     NSLog(@"Notifications unregistered for account ID: %@", args.account.accountId);
                     break;
                 case MCDConnectedDevicesNotificationRegistrationStateExpired:
                 case MCDConnectedDevicesNotificationRegistrationStateExpiring:
                 {
                     // Because the notificaiton registration is expiring, the per account registration work needs to be kicked off again.
                     // This means registering with the NotificationRegistrationManager as well as any sub component work like RemoteSystemAppRegistration.
                     NSLog(@"Notifications expiring / expired for account ID: %@", args.account.accountId);
                     
                     Account* account = nil;
                     if (weakSelf.accounts.count > 0) {
                         account = [weakSelf.accounts objectAtIndex:[weakSelf.accounts indexOfObjectPassingTest:^BOOL (Account* account, NSUInteger index, BOOL* stop) {
                             return ([account.mcdAccount.accountId isEqualToString: args.account.accountId] && account.mcdAccount.type == args.account.type) ? YES : NO;
                                                                          }]];
                     }
                     
                     if (account != nil && account.state == AccountRegistrationStateInAppCacheAndSdkCache) {
                         [account registerWithSdkAsync];
                     }
                 }
                     break;
                 default:
                     break;
             }
         }];
        
        [self.platform start];
        
        // Pull the accounts from our app's cache and synchronize the list with the apps cached by
        // ConnectedDevicesPlatform AccountManager.
        self.accounts = [self deserializeAccounts];
        
        // Finally initialize the accounts. This will refresh registrations when needed, add missing accounts,
        // and remove stale accounts from the ConnectedDevicesPlatform AccountManager. The promise associated
        // with all of this asynchronous work need not be waited on as any sub component work will be accomplished
        // in the synchronous portion of the call. If your app needs to sequence when other apps can see this app's registration
        // (i.e. when RemoteSystemAppRegistration PublishAsync completes) then it would be useful to use the promise returned by
        // prepareAccountsAsync
        self.accountsPromise = [self prepareAccountsAsync];
    }
    return self;
}

- (NSMutableArray<Account*>*)deserializeAccounts {
    // Add all cached accounts from the platform.
    NSMutableArray<MCDConnectedDevicesAccount*>* sdkCachedAccounts = [NSMutableArray arrayWithArray:self.platform.accountManager.accounts];

    // Ideally the token library would support multiple app cached accounts; If this is nil then there is no account the app knows about.
    Account* appCachedAccount = [[Account alloc] initWithMSAAccount:[[MSAAccount alloc] initWithClientId:CLIENT_ID scopeOverrides:@{}] platform:self.platform apnsManager:self.apnsManager];
    
    NSMutableArray<Account*>* accountList = [NSMutableArray new];
    if (appCachedAccount != nil) {
        MCDConnectedDevicesAccount* matchingAccount = nil;
        for (MCDConnectedDevicesAccount* account in sdkCachedAccounts) {
            if ([appCachedAccount.mcdAccount.accountId isEqualToString:account.accountId] && appCachedAccount.mcdAccount.type == account.type) {
                matchingAccount = account;
                break;
            }
        }
        
        if (matchingAccount != nil) {
            appCachedAccount.state = AccountRegistrationStateInAppCacheAndSdkCache;
            [sdkCachedAccounts removeObject:matchingAccount];
        } else {
            appCachedAccount.state = AccountRegistrationStateInAppCacheOnly;
        }
        
        [accountList addObject:appCachedAccount];
    }
    
    // Add the remaining SDK only accounts (these need to be removed from the SDK)
    for (MCDConnectedDevicesAccount* account in sdkCachedAccounts) {
        [accountList addObject:[[Account alloc] initWithMCDAccount:account state:AccountRegistrationStateInSdkCacheOnly platform:self.platform apnsManager:self.apnsManager]];
    }

    return accountList;
}

- (AnyPromise*)prepareAccountsAsync {
    NSMutableArray<AnyPromise*>* promises = [NSMutableArray new];
    for (Account* account in self.accounts) {
        [promises addObject:[account prepareAccountAsync:self]];
    }
    
    return PMKWhen(promises).then(^(NSArray* __unused promises) {  
        [self accountListChanged];
    });
}

- (void)accountListChanged {
    [self.delegate accountListDidUpdate:self.accounts];
    [self serializeAccountsToCache];
}

- (void)serializeAccountsToCache {
    // Unlike the samples on other platforms, since the token helpers don't support multiple accounts and handle persisting the token,
    // there is nothing to do here. At most the app can know about one account. If your token library does support multiple accounts, it would
    // be ideal to make sure that accounts are always persisted out to disk when the set of accounts changes.
}

- (AnyPromise*)signInMsaAsync {
    MSAAccount* msaAccount = [[MSAAccount alloc] initWithClientId:CLIENT_ID scopeOverrides:@{}];
    return [AnyPromise promiseWithAdapterBlock:^(PMKAdapter _Nonnull adapter) {
        [msaAccount signInWithCompletionCallback:adapter];
    }].then(^{
        Account* account = [[Account alloc] initWithMSAAccount:msaAccount platform:self.platform apnsManager:self.apnsManager];
        account.state = AccountRegistrationStateInAppCacheOnly;
        [self.accounts addObject:account];
        return [account prepareAccountAsync:self];
    }).then(^{
        [self accountListChanged];
    });
}

- (AnyPromise*)signOutAsync:(Account*)account {
    return [account signOutAsync].then(^{
        [self.accounts removeObjectAtIndex:[self.accounts indexOfObjectPassingTest:^BOOL(Account * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
            return [obj.mcdAccount.accountId isEqualToString:account.mcdAccount.accountId] && obj.mcdAccount.type == account.mcdAccount.type;
        }]];
        
        [self accountListChanged];
    });
}

- (void)setNotificationRegistration:(NSString*)tokenString {
    
    MCDConnectedDevicesNotificationRegistration* registration = [MCDConnectedDevicesNotificationRegistration new];
    
    if ([[UIApplication sharedApplication] isRegisteredForRemoteNotifications])
    {
        registration.type = MCDNotificationTypeAPN;
    }
    else
    {
        registration.type = MCDNotificationTypePolling;
    }
    
    registration.appId = [[NSBundle mainBundle] bundleIdentifier];
    registration.appDisplayName = (NSString*)[[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleDisplayName"];
    registration.token = tokenString;
    
    // The two cases of receiving a new notification token are:
    // 1. A notification registration is asked for and now it is available. In this case there is a pending promise that was made
    //    at the time of requesting the information. It now needs completed.
    // 2. The account is already registered but for whatever reason the registration changes (APNS gives the app a new token)
    //
    // In order to most cleany handle both cases set the new notification information and then trigger a re registration of all accounts
    // that are in good standing.
    [self.apnsManager setNotificationRegistration:registration accounts:self.accounts];
}


@end
