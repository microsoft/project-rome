//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import <ConnectedDevices/ConnectedDevices.h>

@class OAuthMSAAuthenticator;

@protocol OAuthMSAAuthenticatorDelegate <NSObject>

- (void)oauthMSAAuthenticator:(OAuthMSAAuthenticator*)authenticator
didFinishWithAuthenticationResult:(BOOL)isAuthenticated
                    hasFailed:(BOOL)hasFailed
                 refreshToken:(NSString*)refreshToken;

@end
