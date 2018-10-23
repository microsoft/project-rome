//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import "InboundRequestLogger.h"
#import <UIKit/UIKit.h>

@interface AppServiceViewController : UIViewController <InboundRequestLoggerDelegate>
@property(weak, nonatomic) IBOutlet UITextView* messages;
@end
