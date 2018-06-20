//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "SdkViewController.h"
#import "AppDataSource.h"
#import "AppServiceProvider.h"
#import "IdentityViewController.h"
#import "LaunchUriProvider.h"
#import "NotificationProvider.h"
#import <ConnectedDevices/Core/MCDPlatform.h>
#import <ConnectedDevices/Hosting/MCDHostingRemoteSystemApplicationRegistrationBuilder.h>
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@implementation SdkViewController : UIViewController

- (void)viewDidLoad
{
    [super viewDidLoad];

    // Wait to enable the buttons until platform is initialized
    [self.deviceRelayButton setEnabled:NO];
    [self.activityFeedButton setEnabled:NO];

    UIBarButtonItem* signOutButton =
        [[UIBarButtonItem alloc] initWithTitle:@"Sign Out" style:UIBarButtonItemStyleDone target:self action:@selector(_signOutClicked:)];
    self.navigationItem.rightBarButtonItem = signOutButton;

    [self initializePlatform];
}

- (void)initializePlatform
{
    // Only register for APNs if this app is enabled for push notifications
    NotificationProvider* notificationProvider;
    if ([[UIApplication sharedApplication] isRegisteredForRemoteNotifications])
    {
        notificationProvider = [AppDataSource sharedInstance].notificationProvider;
    }
    else
    {
        NSLog(@"Initializing platform without a notification provider!");
        notificationProvider = nil;
    }

    // Initialize platform
    [AppDataSource sharedInstance].platform = [MCDPlatform platformWithAccountProvider:[AppDataSource sharedInstance].accountProvider notificationProvider:notificationProvider];

    // App is registered asynchronously.
    MCDHostingRemoteSystemApplicationRegistrationBuilder* builder = [MCDHostingRemoteSystemApplicationRegistrationBuilder new];
    [builder setLaunchUriProvider:[[LaunchUriProvider alloc] initWithDelegate:[AppDataSource sharedInstance].inboundRequestLogger]];
    [builder addAppServiceProvider:[[AppServiceProvider alloc] initWithDelegate:[AppDataSource sharedInstance].inboundRequestLogger]];
    [builder addAttribute:@"ExampleAttribute" forName:@"ExampleName"];
    MCDRemoteSystemApplicationRegistration* registration = [builder buildRegistration];
    
    [registration addCloudRegistrationStatusChangedListener:^(MCDUserAccount * _Nonnull account, MCDCloudRegistrationStatus status) {
        NSLog(@"Cloud Registration Status Changed listener");
        switch (status) {
            case MCDCloudRegistrationStatusFailed:
                NSLog(@"Cloud registration completed with status Failed");
                break;
            case MCDCloudRegistrationStatusInProgress:
                NSLog(@"Cloud registration in progress");
                break;
            case MCDCloudRegistrationStatusNotStarted:
                NSLog(@"Cloud registration not started");
                break;
            case MCDCloudRegistrationStatusSucceeded:
                dispatch_async(dispatch_get_main_queue(), ^{
                    // The app has been registered.  It is safe to enable button.
                    [self.deviceRelayButton setEnabled:YES];
                    [self.activityFeedButton setEnabled:YES];
                });
                break;
        }
    }];
    
    [registration start];
}

- (void)_signOutClicked:(id)sender
{
    // Disable buttons when starting sign-out
    [self.deviceRelayButton setEnabled:NO];
    [self.activityFeedButton setEnabled:NO];

    if ([AppDataSource sharedInstance].accountProvider.signedIn)
    {
        [[AppDataSource sharedInstance].accountProvider
            signOutWithCompletionCallback:^(BOOL successful, SampleAccountActionFailureReason reason) {
                NSLog(@"%@", (successful ? @"Currently signed out" : @"Sign out failed"));
                dispatch_async(dispatch_get_main_queue(), ^{ [self dismissViewControllerAnimated:YES completion:nil]; });
            }];
    }
    else
    {
        // If we're somehow already signed out, just dismiss
        [self dismissViewControllerAnimated:YES completion:nil];
    }
}

@end
