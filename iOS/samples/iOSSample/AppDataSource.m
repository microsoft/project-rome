//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "AppDataSource.h"
#import "Secrets.h"

@implementation AppDataSource

+ (AppDataSource*)sharedInstance
{
    static dispatch_once_t onceToken;
    static AppDataSource* sharedInstance;

    dispatch_once(&onceToken, ^{ sharedInstance = [[AppDataSource alloc] init]; });
    return sharedInstance;
}

- (instancetype)init
{
    if (self = [super init])
    {
        _notificationProvider = [NotificationProvider new];

        // You will need a valid clientId to initialize the platform
        // Register your app with Microsoft - https://apps.dev.microsoft.com/ to get clientId
        // The platform requires a valid OAuth token to initialize
        // This sample provides the source files that are used to acquired the token under the 'Auth Provider' directory
        // The only requirement is to obtain a valid OAuth token
        // This sample shows one way it can be done; we are giving you the option to use the sample auth code or use your own
        // scopeOverrides allows you to override scopes that are requested by the auth provider. Apps do not normally need to override scopes.
        NSDictionary<NSString*, NSArray<NSString*>*>* scopeOverrides = @{};
        _accountProvider = [[MSAAccountProvider alloc] initWithClientId:CLIENT_ID scopeOverrides:scopeOverrides];

        _inboundRequestLogger = [InboundRequestLogger new];
    }
    return self;
}

@end
