//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import <WebKit/WebKit.h>
#import "OAuthMsaAuthenticatorDelegate.h"

@interface OAuthMSAAuthenticator : NSObject <UIWebViewDelegate>

@property (nonatomic, readonly, weak) UIWebView* webView;
@property (nonatomic, readwrite, weak) id<OAuthMSAAuthenticatorDelegate> delegate;
@property (nonatomic, readonly, copy) NSString* clientId;

- (instancetype)initWithWebView:(nonnull UIWebView*)webView withClientId:(nonnull NSString*)clientId;

- (void)fireAuthenticationComplete:(BOOL)isAuthenticated hasFailed:(BOOL)hasFailed refreshToken:(NSString*)refreshToken;

- (NSError*)login:(NSString*)signInUri;
- (NSError*)logout;

@end
