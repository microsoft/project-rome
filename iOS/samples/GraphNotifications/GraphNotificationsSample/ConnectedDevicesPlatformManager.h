//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import <ConnectedDevices/MCDConnectedDevicesPlatform.h>
#import <Foundation/Foundation.h>
#import <PromiseKit/PromiseKit.h>

#import "MSAAccount.h"

#ifndef ConnectedDevicesPlatformManager_h

#define ConnectedDevicesPlatformManager_h

@class Account;
@class ConnectedDevicesPlatformManager;

@interface APNSManager : NSObject
- (AnyPromise*)getNotificationRegistrationAsync:(Account*)account;
- (void)setNotificationRegistration:(MCDConnectedDevicesNotificationRegistration*)registration accounts:(NSArray<Account*>*)accounts;
@end

typedef NS_ENUM(NSInteger, AccountRegistrationState) {
    AccountRegistrationStateInAppCacheAndSdkCache,
    AccountRegistrationStateInAppCacheOnly,
    AccountRegistrationStateInSdkCacheOnly
};

@interface Account : NSObject
- (instancetype)initWithMSAAccount:(MSAAccount*)msaAccount platform:(MCDConnectedDevicesPlatform*)platform apnsManager:(APNSManager*)apnsManager;
- (instancetype)initWithMCDAccount:(MCDConnectedDevicesAccount*)account state:(AccountRegistrationState)state platform:(MCDConnectedDevicesPlatform*)platform apnsManager:(APNSManager*)apnsManager;

- (AnyPromise*)prepareAccountAsync:(ConnectedDevicesPlatformManager*)platformManager;
- (AnyPromise*)getAccessTokenAsync:(NSArray<NSString*>*)scopes;
- (AnyPromise*)registerWithSdkAsync;
- (AnyPromise*)signOutAsync;

@property(nonatomic) AccountRegistrationState state;
@property(nonatomic) MCDConnectedDevicesAccount* mcdAccount;
@property(nonatomic) MCDConnectedDevicesPlatform* platform;
@property(nonatomic) APNSManager* apnsManager;

@end

@protocol ConnectedDevicesPlatformManagerDelegate
- (void)accountListDidUpdate:(NSArray<Account*>*)accounts;
@end

// This is a singleton object which holds onto the app's ConnectedDevicesPlatform and
// handles account management.
@interface ConnectedDevicesPlatformManager : NSObject
+ (instancetype)sharedInstance;

@property(nonatomic) MCDConnectedDevicesPlatform* platform;
@property(atomic) NSMutableArray<Account*>* accounts;
@property(nonatomic, weak) id<ConnectedDevicesPlatformManagerDelegate> delegate;
@property(nonatomic) APNSManager* apnsManager;
@property(nonatomic) AnyPromise* accountsPromise;

- (AnyPromise*)signInMsaAsync;
- (AnyPromise*)signOutAsync:(Account*)account;
- (NSMutableArray<Account*>*)deserializeAccounts;
- (void)setNotificationRegistration:(NSString*)deviceToken;
@end

#endif /* ConnectedDevicesPlatformManager_h */
