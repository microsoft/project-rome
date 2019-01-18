//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import <Foundation/Foundation.h>
#import "SignInAccount.h"
#import <ADAL/ADAL.h>
#import <ConnectedDevices/ConnectedDevices/ConnectedDevices.h>
#import "SignInAccount.h"

@interface AADAccount : NSObject <SignInAccount>

- (nullable instancetype)initWithClientId:(nonnull NSString*)clientId redirectUri:(nonnull NSURL*)redirectUri;
- (void)signInWithCompletionCallback:(nonnull void (^)(MCDConnectedDevicesAccount* _Nonnull, NSError* _Nullable))callback;
- (void)signOutWithCompletionCallback:(nonnull void (^)(MCDConnectedDevicesAccount* _Nonnull, NSError* _Nullable))callback;

@property(readonly, nonatomic, copy, nonnull) NSString* clientId;
@property(readonly, nonatomic, copy, nonnull) NSURL* redirectUri;
@property(readonly) BOOL isSignedIn;
@property(readonly, nonatomic, nonnull) MCDConnectedDevicesAccount* mcdAccount;
@end
