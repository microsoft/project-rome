//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "UserActivityViewController.h"
#import "AppDataSource.h"
#import <ConnectedDevices/UserActivities/UserActivities.h>
#import <ConnectedDevices/UserData/MCDUserDataFeed.h>
#import "Secrets.h"
#import <UIKit/UIKit.h>

// UserActivities can be done in 5 steps:
// Step #1: Get a UserActivity channel
// Step #2: Create a UserActivity
// Step #3: Publish the Activity
// Step #4: Start a new a session for the UserActivity
// Step #5: Read/sync your UserActivities

@implementation UserActivityViewController

- (void)viewDidLoad
{
    [super viewDidLoad];

    // You must be logged in to use UserActivities
    NSArray<MCDUserAccount*>* accounts = [[AppDataSource sharedInstance].accountProvider getUserAccounts];
    if (accounts.count > 0)
    {
        // Step #1: Get a UserActivity channel, getting the default channel        
        NSLog(@"Creating UserActivityChannel");
        NSArray<MCDUserAccount*>* accounts = [[AppDataSource sharedInstance].accountProvider getUserAccounts];
        MCDUserDataFeed* userDataFeed = [MCDUserDataFeed userDataFeedForAccount:accounts[0]
                                                                       platform:[AppDataSource sharedInstance].platform
                                                             activitySourceHost:CROSS_PLATFORM_APP_ID];
        NSArray<MCDUserDataFeedSyncScope*>* syncScopes = @[ [MCDUserActivityChannel syncScope] ];
        [userDataFeed addSyncScopes:syncScopes];
        self.channel = [MCDUserActivityChannel userActivityChannelWithUserDataFeed:userDataFeed];
    }
    else
    {
        NSLog(@"Must have an active account to publish activities!");
        self.createActivityStatusField.text = @"Need to be logged in!";
    }
}

- (IBAction)createActivityButton:(id)sender
{

    // Step #2: Create a UserActivity
    [self.channel getOrCreateUserActivityAsync:[[NSUUID UUID] UUIDString]
                                    completion:^(MCDUserActivity* activity, NSError* error) {
                                        if (error)
                                        {
                                            NSLog(@"%@", error);
                                            self.createActivityStatusField.text = @"Error creating activity!";
                                        }
                                        else if (!activity)
                                        {
                                            NSLog(@"No activity created!");
                                        }
                                        else
                                        {
                                            dispatch_async(dispatch_get_main_queue(), ^{
                                                self.activity = activity;

                                                // Create an activityId so the app knows so you know how to get back
                                                self.activityId.text = activity.activityId;
                                                self.createActivityStatusField.text = @"Created by iOSSample";
                                                // Create a deep link so the app can get right back to where it was
                                                self.activationUri.text = @"roman-app:";
                                                self.createActivityStatusField.text = @"Activity created";
                                            });
                                        }
                                    }];
}

- (IBAction)publishActivityButton:(id)sender
{
    self.createActivityStatusField.text = @"Saving";
    self.activity.activationUri = self.activationUri.text;
    // Set the display text for your activity.
    self.activity.visualElements.displayText = @"Visual Element, like an Adaptive Card";
    self.activity.visualElements.attribution.iconUri = @"https://www.microsoft.com/favicon.ico";

    // Step #3: Publish the Activity
    [self.activity saveAsync:^(NSError* error) {
        dispatch_async(dispatch_get_main_queue(), ^{
            if (error)
            {
                NSLog(@"%@", error);
                self.createActivityStatusField.text = @"Error saving activity";
            }
            else
            {
                self.createActivityStatusField.text = @"Saved successfully!";
                [self.sessionButton setTitle:@"Start Session" forState:normal];
            }
        });
    }];
}

- (IBAction)readActivityButton:(id)sender
{
    // Step #5: Read/sync your UserActivities
    [self.channel getRecentUserActivitiesAsync:(NSInteger)5
                                    completion:^(NSArray<MCDUserActivitySessionHistoryItem*>* _Nonnull result, NSError* _Nullable error) {
                                        dispatch_async(dispatch_get_main_queue(), ^{
                                            if (error)
                                            {
                                                self.readActivityStatusField.text = @"Error reading activity!";
                                            }
                                            else if (result.count == 0)
                                            {
                                                self.readActivityStatusField.text = @"Read completed, no activities returned";
                                            }
                                            else
                                            {
                                                self.readActivityStatusField.text = @"Read completed!";
                                                MCDUserActivitySessionHistoryItem* item = result.firstObject;
                                                self.activityList.text = item.userActivity.activityId;
                                                self.activityDisplay.text = item.userActivity.visualElements.displayText;
                                            }
                                        });
                                    }];
}

- (IBAction)manageSessionButton:(id)sender
{

    // Step #4: Start a new a session for the UserActivity
    if (self.session == nil)
    {
        self.session = [self.activity createSession];
        dispatch_async(dispatch_get_main_queue(), ^{
            NSLog(@"UserActivitySession has started %@", self.session);
            self.session = [self.activity createSession];
            dispatch_async(dispatch_get_main_queue(), ^{ [self.sessionButton setTitle:@"Stop Session" forState:normal]; });
        });
    }
    else
    {
        // Stop the UserActivitysession
        [self.session stop];
        self.session = nil;
        dispatch_async(dispatch_get_main_queue(), ^{
            NSLog(@"UserActivitySession has stopped %@", self.session);
            [self.sessionButton setTitle:@"Start Session" forState:normal];
        });
    }
}

@end
