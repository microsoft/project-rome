//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <ConnectedDevices/Core/Core.h>

#import "SampleAccountActionFailureReason.h"

typedef NS_ENUM(NSInteger, AADMSAAccountProviderSignInState)
{
    AADMSAAccountProviderSignInStateSignedOut,
    AADMSAAccountProviderSignInStateSignedInMSA,
    AADMSAAccountProviderSignInStateSignedInAAD,
};

// @brief A sample MCDUserAccountProvider that wraps around an AAD provider and an MSA provider.
// Supports only a single user account at a time - trying to log into more than one account at once will throw an exception.
// Any accounts logged into will be made available through the MCDUserAccountProvider interface.
//
// When signed into an AAD account, because of AAD limitations,
// only the first scope in scopes[] passed to for getAccessTokenForUserAccountIdAsync: and onAccessTokenError:, is used
//
// msaClientId is a guid from the app's registration in the msa apps portal
// aadApplicationId is a guid from the app's registration in the azure portal
// aadRedirectUri is a Uri specified in the azure portal
@interface AADMSAAccountProvider : NSObject <MCDUserAccountProvider>
@property(readonly, atomic) AADMSAAccountProviderSignInState signInState;
@property(readonly, nonatomic, copy, nonnull) NSString* msaClientId;
@property(readonly, nonatomic, copy, nonnull) NSString* aadApplicationId;

- (nullable instancetype)initWithMsaClientId:(nonnull NSString*)msaClientId
                            aadApplicationId:(nonnull NSString*)aadApplicationId
                              aadRedirectUri:(nonnull NSURL*)aadRedirectUri;
- (void)signInMSAWithCompletionCallback:(nonnull SampleAccountProviderCompletionBlock)callback;
- (void)signInAADWithCompletionCallback:(nonnull SampleAccountProviderCompletionBlock)callback;
- (void)signOutWithCompletionCallback:(nonnull SampleAccountProviderCompletionBlock)callback;

@end
