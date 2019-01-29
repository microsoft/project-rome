//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "AppServiceViewController.h"
#import "ConnectedDevicesPlatformManager.h"
#import "InboundRequestLogger.h"
#import <Foundation/Foundation.h>

@implementation AppServiceViewController

- (void)viewDidLoad
{
    [super viewDidLoad];

    _messages.text = [InboundRequestLogger sharedInstance].log;
    if ([_messages.text length] == 0)
    {
        _messages.text = @"No message received yet";
    }
    [InboundRequestLogger sharedInstance].delegate = self;
}

#pragma mark - InboundRequestLogger Delegate

- (void)logDidUpdate:(NSString*)log
{
    dispatch_async(dispatch_get_main_queue(), ^{ self.messages.text = log; });
}

@end
