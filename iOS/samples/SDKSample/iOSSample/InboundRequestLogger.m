//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "InboundRequestLogger.h"

@implementation InboundRequestLogger

+ (instancetype)sharedInstance {
    static dispatch_once_t onceToken;
    static InboundRequestLogger* sharedInstance;
    
    dispatch_once(&onceToken, ^{ sharedInstance = [[InboundRequestLogger alloc] init]; });
    return sharedInstance;
}

- (instancetype)init
{
    if (self = [super init])
    {
        _log = @"";
    }
    return self;
}

#pragma mark - LaunchUriProviderDelegate

- (void)launchUriProvider:(LaunchUriProvider*)launchUriProvider didReceiveRequestForUri:(NSString*)uri
{
    _log = [_log stringByAppendingFormat:@"Received request to launch uri:\n%@\n", uri];
    [_delegate logDidUpdate:[_log copy]];
}

#pragma mark - AppServiceProviderDelegate

- (void)appServiceProvider:(AppServiceProvider*)self didOpenConnection:(MCDAppServiceConnection*)appServiceConnection
{
    _log = [_log stringByAppendingFormat:@"New app service connection opened\n"];
    [_delegate logDidUpdate:[_log copy]];
}

- (void)appServiceConnection:(MCDAppServiceConnection*)appServiceConnection
           didReceiveRequest:(MCDAppServiceRequestReceivedEventArgs*)requestArgs
{
    _log = [_log stringByAppendingFormat:@"Received message:\n%@\n", requestArgs.request.message];
    [_delegate logDidUpdate:[_log copy]];
}

@end
