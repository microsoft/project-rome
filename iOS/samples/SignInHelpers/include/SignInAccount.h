//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <ConnectedDevices/ConnectedDevices.h>

#import "SampleAccountActionFailureReason.h"

@protocol SignInAccount <NSObject>

- (void)signInWithCompletionCallback:(nonnull void (^)(MCDConnectedDevicesAccount* _Nonnull, NSError* _Nullable))callback;
- (void)signOutWithCompletionCallback:(nonnull void (^)(MCDConnectedDevicesAccount* _Nonnull, NSError* _Nullable))callback;

- (void)getAccessTokenForUserAccountIdAsync:(nonnull NSString*)accountId
                                     scopes:(nonnull NSArray<NSString*>*)scopes
                                 completion:(nonnull void (^)(NSString* _Nonnull, NSError* _Nullable))scompletionBlock;

- (BOOL)isSignedIn;
@property(readonly, nonatomic, nonnull) MCDConnectedDevicesAccount* mcdAccount;

@end
