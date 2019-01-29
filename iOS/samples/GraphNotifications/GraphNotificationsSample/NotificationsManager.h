
#pragma once

#import <Foundation/Foundation.h>
#import <ConnectedDevices/ConnectedDevices.h>
#import <ConnectedDevicesUserData/ConnectedDevicesUserData.h>
#import <ConnectedDevicesUserDataUserNotifications/ConnectedDevicesUserDataUserNotifications.h>
#import "AADAccount.h"
#import "MSAAccount.h"

@interface NotificationsManager : NSObject
+ (instancetype)startWithPlatform:(MCDConnectedDevicesPlatform*)platform;

+ (instancetype)sharedInstance;

@property (nonatomic, readonly) NSArray<MCDUserNotification*>* notifications;
@property (nonatomic) MCDConnectedDevicesAccount* account;

- (NSInteger)addNotificationsChangedListener:(void(^)(void))listener;
- (void)removeListener:(NSInteger)token;
- (void)forceRead;
- (void)refresh;
- (void)dismissNotification:(MCDUserNotification*)notification;
- (void)dismissNotificationWithId:(NSString*)notificationId;
- (void)readNotification:(MCDUserNotification*)notification;
@end
