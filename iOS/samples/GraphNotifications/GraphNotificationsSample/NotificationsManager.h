
#pragma once

#import <Foundation/Foundation.h>
#import <ConnectedDevices/Core/Core.h>
#import <ConnectedDevices/UserData/UserData.h>
#import <ConnectedDevices/UserData.UserNotifications/UserData.UserNotifications.h>
#import "AADMSAAccountProvider.h"

@interface NotificationsManager : NSObject
+ (instancetype)startWithAccountProvider:(AADMSAAccountProvider*)accountProvider platform:(MCDPlatform*)platform;
+ (instancetype)sharedInstance;

@property (nonatomic, readonly) AADMSAAccountProvider* accountProvider;
@property (nonatomic, readonly) NSArray<MCDUserNotification*>* notifications;

- (NSInteger)addNotificationsChangedListener:(void(^)(void))listener;
- (void)removeListener:(NSInteger)token;
- (void)forceRead;
- (void)dismissNotificationAtIndex:(NSUInteger)index;
- (void)readNotificationAtIndex:(NSUInteger)index;
@end
