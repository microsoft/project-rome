//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "LoginViewController.h"
#import "ConnectedDevicesPlatformManager.h"

typedef NS_ENUM(NSInteger, LoginState) {
    AAD,
    MSA,
    SIGNED_OUT
};

@implementation LoginViewController {
    ConnectedDevicesPlatformManager* _platformManager;
}

- (void)viewDidLoad {
    [super viewDidLoad];

    _platformManager = [ConnectedDevicesPlatformManager sharedInstance];
    
    [self _setButtonTextForState:[self _getState]];
}

- (IBAction)loginMSA {
    LoginState state = [self _getState];
    
    if (state == SIGNED_OUT) {
        [self _setStatusText:@"Signing in MSA..."];
        
        // Perform MSA sign-in
        [_platformManager signInMsaAsync].then(^{
            [self _setStatusText:[NSString stringWithFormat:@"Currently signed in"]];
            [self _setButtonTextForState:MSA];
        }).catch(^(NSError* error){
            NSLog(@"%@", error);
            [self _setStatusText:[NSString stringWithFormat:@"MSA sign-in failed!"]];
        });
    } else {
        [_platformManager signOutAsync:_platformManager.accounts[0]].then(^{
            [self _setStatusText:[NSString stringWithFormat:@"Currently signed out"]];
            [self _setButtonTextForState:SIGNED_OUT];
        }).catch(^(NSError* error){
            NSLog(@"%@", error);
            [self _setStatusText:[NSString stringWithFormat:@"MSA sign-out failed!"]];
        });
    }
}

- (IBAction)loginAAD {
    LoginState state = [self _getState];
    
    if (state == SIGNED_OUT) {
        [self _setStatusText:@"Signing in AAD..."];
        
        // Perform AAD sign-in
        [_platformManager signInAadAsync].then(^{
            [self _setStatusText:[NSString stringWithFormat:@"Currently signed in"]];
            [self _setButtonTextForState:AAD];
        }).catch(^(NSError* error){
            NSLog(@"%@", error);
            [self _setStatusText:[NSString stringWithFormat:@"AAD sign-in failed!"]];
        });
    } else {
        [_platformManager signOutAsync:_platformManager.accounts[0]].then(^{
            [self _setStatusText:[NSString stringWithFormat:@"Currently signed out"]];
            [self _setButtonTextForState:SIGNED_OUT];
        }).catch(^(NSError* error){
            NSLog(@"%@", error);
            [self _setStatusText:[NSString stringWithFormat:@"AAD sign-out failed!"]];
        });
    }
}

- (LoginState)_getState {
    @synchronized (self) {
        Account* account = nil;
        if (_platformManager.accounts.count > 0) {
            NSInteger index = [_platformManager.accounts indexOfObjectPassingTest:^BOOL (Account* account, NSUInteger index, BOOL* stop) {
                return account.state == AccountRegistrationStateInAppCacheAndSdkCache;
            }];
            
            if (index != NSNotFound) {
                account = [_platformManager.accounts objectAtIndex:index];
            }
        }
        
        if (account != nil) {
            return account.mcdAccount.type == MCDConnectedDevicesAccountTypeAAD ? AAD : MSA;
        } else {
            return SIGNED_OUT;
        }
    }
}

- (void)_setButtonTextForState:(LoginState)state {
    dispatch_async(dispatch_get_main_queue(), ^{
           switch (state) {
               case SIGNED_OUT:
                   [self.aadButton setTitle:@"Login with AAD" forState:UIControlStateNormal];
                   [self.msaButton setTitle:@"Login with MSA" forState:UIControlStateNormal];
                   self.loginStatusLabel.text = @"Currently signed-out";
                   break;
               case AAD:
                   [self.aadButton setTitle:@"Logout" forState:UIControlStateNormal];
                   [self.msaButton setTitle:@"" forState:UIControlStateNormal];
                   self.loginStatusLabel.text = @"Currently signed-in with AAD";
                   break;
               case MSA:
                   [self.aadButton setTitle:@"" forState:UIControlStateNormal];
                   [self.msaButton setTitle:@"Logout" forState:UIControlStateNormal];
                   self.loginStatusLabel.text = @"Currently signed-in with MSA";
                   break;
           }
       });
}

- (void)_setStatusText:(NSString*)text {
    NSLog(@"%@", text);
    dispatch_async(dispatch_get_main_queue(), ^{ self.loginStatusLabel.text = text; });
}

@end
