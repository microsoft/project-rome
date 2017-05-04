//
//  Copyright (C) Microsoft Corporation. All rights reserved.
//

#import "ConnectedDevices/ConnectedDevices.h"
#import "OAuthMsaAuthenticatorDelegate.h"

@interface AuthenticationViewController
    : UIViewController <OAuthMSAAuthenticatorDelegate, UIAlertViewDelegate, CDOAuthCodeProviderDelegate>

@property (nonatomic, readwrite) BOOL shouldSignOut;

@end
