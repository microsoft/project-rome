//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "AppServiceViewController.h"
#import "AppDataSource.h"
#import <Foundation/Foundation.h>

@implementation AppServiceViewController

- (void)viewDidLoad
{
    [super viewDidLoad];

    _messages.text = [AppDataSource sharedInstance].inboundRequestLogger.log;
    if ([_messages.text length] == 0)
    {
        _messages.text = @"No message received yet";
    }
    [AppDataSource sharedInstance].inboundRequestLogger.delegate = self;
}

#pragma mark - InboundRequestLogger Delegate

- (void)logDidUpdate:(NSString*)log
{
    dispatch_async(dispatch_get_main_queue(), ^{ self.messages.text = log; });
}

@end
