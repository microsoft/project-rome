//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <ConnectedDevices/ConnectedDevices.h>
#import <UserNotifications/UserNotifications.h>

@interface AppDelegate : UIResponder <UIApplicationDelegate, UNUserNotificationCenterDelegate>

@property (strong, nonatomic) UIWindow* window;
@property (strong, nonatomic) MCDConnectedDevicesPlatform* platform;
- (void)initializePlatform;
- (void)startPlatform;
- (void)registerNotificationsForAccount:(MCDConnectedDevicesAccount*)account callback:(void(^)(MCDConnectedDevicesNotificationRegistrationResult*,NSError*))callback;


@end

