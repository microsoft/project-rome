//
//  DataViewController.m
//  GraphNotifications
//
//  Created by Allen Ballway on 8/23/18.
//  Copyright © 2018 Microsoft. All rights reserved.
//

#import "LoginViewController.h"
#import "NotificationsManager.h"

@interface LoginViewController ()
@property (nonatomic) AADMSAAccountProvider* accountProvider;
@end

@implementation LoginViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view, typically from a nib.
    
    self.accountProvider = [NotificationsManager sharedInstance].accountProvider;
    [self setButtonTextForState:self.accountProvider.signInState];
}

- (IBAction)loginAAD {
    __weak typeof(self) weakSelf = self;
    switch (self.accountProvider.signInState) {
        case AADMSAAccountProviderSignInStateSignedInAAD: {
            [self.accountProvider signOutWithCompletionCallback:^(BOOL successful, SampleAccountActionFailureReason reason) {
                if (successful) {
                    [weakSelf setButtonTextForState:weakSelf.accountProvider.signInState];
                } else {
                    NSLog(@"Failed to sign out AAD with reason %ld", reason);
                }
            }];
            break;
        }
        case AADMSAAccountProviderSignInStateSignedOut: {
            [self.accountProvider signInAADWithCompletionCallback:^(BOOL successful, SampleAccountActionFailureReason reason) {
                if (successful) {
                    [weakSelf setButtonTextForState:weakSelf.accountProvider.signInState];
                } else {
                    NSLog(@"Failed to sign in with AAD with reason %ld", reason);
                }
            }];
            break;
        }
        default:
            // Do nothing
            break;
    }
}

- (IBAction)loginMSA {
    __weak typeof(self) weakSelf = self;
    switch (self.accountProvider.signInState) {
        case AADMSAAccountProviderSignInStateSignedInMSA: {
            [self.accountProvider signOutWithCompletionCallback:^(BOOL successful, SampleAccountActionFailureReason reason) {
                if (successful) {
                    [weakSelf setButtonTextForState:weakSelf.accountProvider.signInState];
                } else {
                    NSLog(@"Failed to sign out MSA with reason %ld", reason);
                }
            }];
            break;
        }
        case AADMSAAccountProviderSignInStateSignedOut: {
            [self.accountProvider signInMSAWithCompletionCallback:^(BOOL successful, SampleAccountActionFailureReason reason) {
                if (successful) {
                    [weakSelf setButtonTextForState:weakSelf.accountProvider.signInState];
                } else {
                    NSLog(@"Failed to sign in with MSA with reason %ld", reason);
                }
            }];
            break;
        }
        default:
            // Do nothing
            break;
    }
}

- (void)setButtonTextForState:(AADMSAAccountProviderSignInState)state {
    dispatch_async(dispatch_get_main_queue(), ^{
        switch (state) {
            case AADMSAAccountProviderSignInStateSignedOut:
                [self.aadButton setTitle:@"Login Work/School Account" forState:UIControlStateNormal];
                [self.msaButton setTitle:@"Login Personal Account" forState:UIControlStateNormal];
                break;
            case AADMSAAccountProviderSignInStateSignedInAAD:
                [self.aadButton setTitle:@"Logout" forState:UIControlStateNormal];
                [self.msaButton setTitle:@"" forState:UIControlStateNormal];
                break;
            case AADMSAAccountProviderSignInStateSignedInMSA:
                [self.aadButton setTitle:@"" forState:UIControlStateNormal];
                [self.msaButton setTitle:@"Logout" forState:UIControlStateNormal];
                break;
        }
    });
}
@end
