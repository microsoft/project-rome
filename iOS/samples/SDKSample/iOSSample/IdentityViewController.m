//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "IdentityViewController.h"
#import "AppDataSource.h"
#import "MSAAccountProvider.h"
#import "MainNavigationController.h"
#import <ConnectedDevices/Core/MCDPlatform.h>
#import <UIKit/UIKit.h>

@interface IdentityViewController ()
@property(nonatomic, strong) UITextView* textView;
@end

@implementation IdentityViewController

- (void)viewDidLoad
{
    [super viewDidLoad];

    if ([AppDataSource sharedInstance].accountProvider.signedIn)
    {
        // Wait just long enough for this ViewController to be added to the stack before trying to transition
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 10), dispatch_get_main_queue(), ^{ [self _transitionToMainViewController]; });
    }
}

- (void)viewWillAppear:(BOOL)animated
{
    if ([AppDataSource sharedInstance].accountProvider.signedIn)
    {
        [self _setStatusText:@"Currently signed in"];
        [_loginButton setTitle:(@"Sign Out") forState:UIControlStateNormal];
    }
    else
    {
        [self _setStatusText:@"Currently signed out"];
        [_loginButton setTitle:(@"Sign In") forState:UIControlStateNormal];
    }
}

- (IBAction)loginButtonPressed:(id)sender
{
    // Currently, this sample only supports MSA accounts
    if (![AppDataSource sharedInstance].accountProvider.signedIn)
    {
        [self _setStatusText:@"Signing in.."];

        // Sign in
        [[AppDataSource sharedInstance].accountProvider
            signInWithCompletionCallback:^(BOOL successful, SampleAccountActionFailureReason reason) {
                [self _setStatusText:[NSString stringWithFormat:@"%@ [%ld]", (successful ? @"Currently signed in" : @"Sign in failed"),
                                               (long)reason]];
                [self.loginButton setTitle:(@"Sign Out") forState:UIControlStateNormal];

                if (successful)
                {
                    // Once sign-in has completed successfully, it's time to initialize the platform in sdkViewController
                    [self _transitionToMainViewController];
                }
            }];
    }
    else
    {
        [[AppDataSource sharedInstance].accountProvider
            signOutWithCompletionCallback:^(BOOL successful, SampleAccountActionFailureReason reason) {
                [self _setStatusText:[NSString stringWithFormat:@"%@ [%ld]", (successful ? @"Currently signed out" : @"Sign out failed"),
                                               (long)reason]];
                [self.loginButton setTitle:(@"Sign In") forState:UIControlStateNormal];
            }];
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
