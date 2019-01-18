//
//  AppDelegate.h
//  GraphNotifications
//
//  Created by Allen Ballway on 8/23/18.
//  Copyright © 2018 Microsoft. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <ConnectedDevices/ConnectedDevices.h>
#import <UserNotifications/UserNotifications.h>

@interface AppDelegate : UIResponder <UIApplicationDelegate, UNUserNotificationCenterDelegate>

@property (strong, nonatomic) UIWindow* window;
@property (strong, nonatomic) MCDConnectedDevicesPlatform* platform;
- (void)initializePlatform;
- (void)startPlatform;
- (void)registerNotificationsForAccount:(MCDConnectedDevicesAccount*)account callback:(void(^)(BOOL,NSError*))callback;


@end

