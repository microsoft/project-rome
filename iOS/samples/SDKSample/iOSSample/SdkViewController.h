//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import <ConnectedDevices/Core/MCDPlatform.h>
#import <UIKit/UIKit.h>

@interface SdkViewController : UIViewController

@property(weak, nonatomic) IBOutlet UIButton* activityFeedButton;
@property(weak, nonatomic) IBOutlet UIButton* deviceRelayButton;

@end
