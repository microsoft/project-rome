//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <ConnectedDevices/ConnectedDevices.h>
#import "SignInAccount.h"

@interface MSAAccount : NSObject <SignInAccount>
- (nullable instancetype)initWithClientId:(nonnull NSString*)clientId
                           scopeOverrides:(nullable NSDictionary<NSString*, NSArray<NSString*>*>*)scopes;
- (void)signInWithCompletionCallback:(nonnull void (^)(MCDConnectedDevicesAccount* _Nonnull, NSError* _Nullable))callback;
- (void)signOutWithCompletionCallback:(nonnull void (^)(MCDConnectedDevicesAccount* _Nonnull, NSError* _Nullable))callback;

@property(readonly, nonatomic, copy, nonnull) NSString* clientId;
@property(readonly) BOOL isSignedIn;
@property(readonly, nonatomic, nonnull) MCDConnectedDevicesAccount* mcdAccount;
@property(readonly, nonatomic, copy, nonnull) NSDictionary<NSString*, NSArray<NSString*>*>* scopeOverrides;
- (void)removeAccount;
@end
