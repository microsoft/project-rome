//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import <Foundation/Foundation.h>
#import <ConnectedDevices/ConnectedDevices.h>
#import <ConnectedDevicesUserData/ConnectedDevicesUserData.h>
#import <ConnectedDevicesUserDataUserNotifications/ConnectedDevicesUserDataUserNotifications.h>

@interface NotificationsManager : NSObject
- (instancetype)initWithAccount:(MCDConnectedDevicesAccount*)account
                       platform:(MCDConnectedDevicesPlatform*)platform;

@property (nonatomic, readonly) NSArray<MCDUserNotification*>* notifications;

- (NSInteger)addNotificationsChangedListener:(void(^)(void))listener;
- (void)removeListener:(NSInteger)token;
- (void)refresh;
- (void)markRead:(MCDUserNotification*)notification;
- (void)deleteNotification:(MCDUserNotification*)notification;
- (void)dismissNotification:(MCDUserNotification*)notification;
- (void)dismissNotificationWithId:(NSString*)notificationId;
@end
