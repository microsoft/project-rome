//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once
#import <UIKit/UIKit.h>

@interface LoginViewController : UIViewController
- (IBAction)loginAAD;
- (IBAction)loginMSA;
@property (strong, nonatomic) IBOutlet UIButton *aadButton;
@property (strong, nonatomic) IBOutlet UIButton *msaButton;

@end

