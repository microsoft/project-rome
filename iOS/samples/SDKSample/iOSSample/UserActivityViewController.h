//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import "MSAAccount.h"
#import <ConnectedDevices/ConnectedDevices.h>
#import <ConnectedDevicesUserDataUserActivities/ConnectedDevicesUserDataUserActivities.h>
#import <UIKit/UIKit.h>

@interface UserActivityViewController : UIViewController

@property(nonatomic) MCDUserActivitySession* session;
@property(nonatomic) MCDUserActivityChannel* channel;
@property(nonatomic) MCDUserActivity* activity;
@property(weak, nonatomic) IBOutlet UIButton* sessionButton;

@property(weak, nonatomic) IBOutlet UITextField* activationUri;
@property(weak, nonatomic) IBOutlet UITextField* activityId;
@property(weak, nonatomic) IBOutlet UITextField* activityList;
@property(weak, nonatomic) IBOutlet UITextField* activityDisplay;
@property(weak, nonatomic) IBOutlet UITextField* createActivityStatusField;
@property(weak, nonatomic) IBOutlet UITextField* readActivityStatusField;

- (IBAction)createActivityButton:(id)sender;
- (IBAction)publishActivityButton:(id)sender;
- (IBAction)readActivityButton:(id)sender;
- (IBAction)manageSessionButton:(id)sender;

@end
