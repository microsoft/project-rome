//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import <Foundation/Foundation.h>
#import <ConnectedDevices/ConnectedDevices.h>
#import <ConnectedDevicesUserData/ConnectedDevicesUserData.h>
#import <ConnectedDevicesUserDataUserNotifications/ConnectedDevicesUserDataUserNotifications.h>
#import "ConnectedDevicesPlatformManager.h"
#import "AADAccount.h"
#import "MSAAccount.h"

@interface NotificationsManager : NSObject

+ (instancetype)sharedInstance;

@property (nonatomic, readonly) NSArray<MCDUserNotification*>* notifications;
@property (nonatomic) MCDConnectedDevicesAccount* account;

- (instancetype)initWithPlatformManager:(ConnectedDevicesPlatformManager*)platform;
- (NSInteger)addNotificationsChangedListener:(void(^)(void))listener;
- (void)removeListener:(NSInteger)token;
- (void)forceRead;
- (void)refresh;
- (void)dismissNotification:(MCDUserNotification*)notification;
- (void)dismissNotificationWithId:(NSString*)notificationId;
- (void)readNotification:(MCDUserNotification*)notification;
@end
