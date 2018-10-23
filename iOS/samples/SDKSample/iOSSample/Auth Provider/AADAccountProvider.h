//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <ConnectedDevices/Core/Core.h>

#import "SingleUserAccountProvider.h"

// @brief MCDUserAccountProvider that performs a log in/out flow using ADAL.
// Supports a single AAD user account.
// For getAccessTokenForUserAccountIdAsync: and onAccessTokenError:, because of ADAL limitations, only the first scope in scopes[] is used
@interface AADAccountProvider : NSObject <SingleUserAccountProvider>

// @brief clientId is a guid from the app's registration in the azure portal
// redirectUri is a Uri specified in the same portal
- (nullable instancetype)initWithClientId:(nonnull NSString*)clientId redirectUri:(nonnull NSURL*)redirectUri;

@property(readonly, nonatomic, copy, nonnull) NSString* clientId;
@property(readonly, nonatomic, copy, nonnull) NSURL* redirectUri;

@end
