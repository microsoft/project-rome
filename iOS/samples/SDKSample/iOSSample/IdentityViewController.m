//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "IdentityViewController.h"
#import "ConnectedDevicesPlatformManager.h"
#import "MainNavigationController.h"
#import <UIKit/UIKit.h>

@interface IdentityViewController ()
@property(nonatomic, strong) UITextView* textView;
@end

@implementation IdentityViewController {
    ConnectedDevicesPlatformManager* _platformManager;
}

- (instancetype)initWithNibName:(NSString *)nibNameOrNil
                         bundle:(NSBundle *)nibBundleOrNil {
    if (self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil]) {
        _platformManager = [ConnectedDevicesPlatformManager sharedInstance];
    }
    return self;
}

- (instancetype)initWithCoder:(NSCoder *)coder {
    if (self = [super initWithCoder:coder]) {
        _platformManager = [ConnectedDevicesPlatformManager sharedInstance];
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    [self _setStatusText:@"Checking accounts"];
}

- (void)viewWillAppear:(BOOL)animated
{
    
    _platformManager.accountsPromise.then(^{
        
        // This logic would be better if the UI supported more than one account. Since this is a simple app, just check for one account
        // that is synced to key off of for sign in state.
        NSArray<Account*>* accounts = _platformManager.accounts;
        Account* account = nil;
        if (accounts.count > 0) {
            account = [accounts objectAtIndex:[accounts indexOfObjectPassingTest:^BOOL (Account* account, NSUInteger index, BOOL* stop) {
                return account.state == AccountRegistrationStateInAppCacheAndSdkCache;
            }]];
        }
        
        if (account != nil)
        {
            [self _setStatusText:@"Currently signed in"];
            [_loginButton setTitle:(@"Sign Out") forState:UIControlStateNormal];
            [self _transitionToMainViewController];
        }
        else
        {
            [self _setStatusText:@"Currently signed out"];
            [_loginButton setTitle:(@"Sign In") forState:UIControlStateNormal];
        }
    });
}

- (IBAction)loginButtonPressed:(id)sender {
    // Currently, this sample only supports a single MSA account. Just find the first account in good standing to log out.
    Account* account = nil;
    if (_platformManager.accounts.count > 0) {
        NSInteger index = [_platformManager.accounts indexOfObjectPassingTest:^BOOL (Account* account, NSUInteger index, BOOL* stop) {
            return account.state == AccountRegistrationStateInAppCacheAndSdkCache;
        }];
        
        
        if (index != NSNotFound) {
            account = [_platformManager.accounts objectAtIndex:index];
        }
    }
    
    if (account == nil) {
        [self _setStatusText:@"Signing in.."];
        
        // Perform actual sign in
        [_platformManager signInMsaAsync].then(^{
            [self _setStatusText:[NSString stringWithFormat:@"Currently signed in"]];
            [self.loginButton setTitle:(@"Sign Out") forState:UIControlStateNormal];
            [self _transitionToMainViewController];
        }).catch(^(NSError* error){
            NSLog(@"%@", error);
            [self _setStatusText:[NSString stringWithFormat:@"Sign in failed!"]];
        });
    } else {
       [_platformManager signOutAsync:account].then(^{
            [self _setStatusText:[NSString stringWithFormat:@"Currently signed out"]];
            [self.loginButton setTitle:(@"Sign In") forState:UIControlStateNormal];
        }).catch(^(NSError* error){
            NSLog(@"%@", error);
            [self _setStatusText:[NSString stringWithFormat:@"Sign out failed!"]];
        });
    }
}

- (void)_transitionToMainViewController
{
    UIStoryboard* storyBoard = self.storyboard;
    UINavigationController* vc = [storyBoard instantiateViewControllerWithIdentifier:@"MainNavigationController"];
    vc.modalTransitionStyle = UIModalTransitionStyleFlipHorizontal;

    dispatch_async(dispatch_get_main_queue(), ^{ [self presentViewController:vc animated:YES completion:nil]; });
}

- (void)_setStatusText:(NSString*)text
{
    NSLog(@"%@", text);
    dispatch_async(dispatch_get_main_queue(), ^{ self.loginStatusLabel.text = text; });
}

@end
