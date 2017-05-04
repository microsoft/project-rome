//
//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "AppDelegate.h"

static const NSString* HockeyAppId = @"5649c03af8954a5c9b7ba98b801096a6";

@implementation AppDelegate

- (BOOL)application:(__unused UIApplication*)application didFinishLaunchingWithOptions:(__unused NSDictionary*)launchOptions
{
    _dataSource = [AppDataSource new];
    
    return YES;
}

- (void)applicationDidEnterBackground:(__unused UIApplication*)application
{
    [CDPlatform suspend];
}

- (void)applicationWillEnterForeground:(__unused UIApplication*)application
{
    [CDPlatform resume];
}

- (void)applicationWillTerminate:(__used UIApplication*)application
{
    [CDPlatform shutdown];
}

@end
