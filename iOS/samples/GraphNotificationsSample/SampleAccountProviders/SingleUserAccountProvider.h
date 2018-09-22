//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import <ConnectedDevices/Core/Core.h>

#import "SampleAccountActionFailureReason.h"

// @brief Protocol for a MCDUserAccountProvider that supports logging into/out of a single user account.
@protocol SingleUserAccountProvider <MCDUserAccountProvider>
- (void)signInWithCompletionCallback:(nonnull SampleAccountProviderCompletionBlock)callback;
- (void)signOutWithCompletionCallback:(nonnull SampleAccountProviderCompletionBlock)callback;
@property(readonly, atomic) BOOL signedIn;

@end
