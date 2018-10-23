//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import <ConnectedDevices/Core/MCDPlatform.h>
#import <UIKit/UIKit.h>

@interface IdentityViewController : UIViewController

@property(weak, nonatomic) IBOutlet UILabel* loginStatusLabel;
@property(weak, nonatomic) IBOutlet UIButton* loginButton;

@end
