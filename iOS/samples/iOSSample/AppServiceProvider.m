//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "AppServiceProvider.h"
#import "Secrets.h"

@implementation AppServiceProvider

- (instancetype)initWithDelegate:(id<AppServiceProviderDelegate>)delegate
{
    if (self = [super init])
    {
        _delegate = delegate;
        _appServiceInfo = [MCDAppServiceInfo infoWithName:APP_SERVICE_NAME packageId:PACKAGE_ID];
    }

    return self;
}

- (instancetype)init
{
    return [self initWithDelegate:nil];
}

#pragma mark - MCDAppServiceProvider Protocol Requirements

@synthesize appServiceInfo = _appServiceInfo;

- (void)connectionDidOpen:(MCDAppServiceConnection*)connection
{
    [_delegate appServiceProvider:self didOpenConnection:connection];

    id<AppServiceProviderDelegate> __weak weakDelegate = _delegate;
    [connection addRequestReceivedListener:^(MCDAppServiceConnection* connection, MCDAppServiceRequestReceivedEventArgs* args) {
        [weakDelegate appServiceConnection:connection didReceiveRequest:args];
    }];
}

@end
