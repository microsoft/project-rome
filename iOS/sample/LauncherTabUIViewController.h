//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface LauncherTabUIViewController : UIViewController <UIAlertViewDelegate, UITextFieldDelegate>

@property (weak, nonatomic) IBOutlet UITextField* launchUriTextBox;
@property (weak, nonatomic) IBOutlet UIButton* launchUriButton;

@end
