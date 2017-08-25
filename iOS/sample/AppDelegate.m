//
//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "AppDelegate.h"

@implementation AppDelegate

- (BOOL)application:(__unused UIApplication*)application didFinishLaunchingWithOptions:(__unused NSDictionary*)launchOptions
{
    _dataSource = [AppDataSource new];
    
    return YES;
}

- (void)applicationDidEnterBackground:(__unused UIApplication*)application
{
    [MCDPlatform suspend];
}

- (void)applicationWillEnterForeground:(__unused UIApplication*)application
{
    [MCDPlatform resume];
}

- (void)applicationWillTerminate:(__used UIApplication*)application
{
    [MCDPlatform shutdown];
}

@end
