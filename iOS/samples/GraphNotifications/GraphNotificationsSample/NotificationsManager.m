//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "NotificationsManager.h"
#import "Secrets.h"
#import "AppDelegate.h"
#import <UserNotifications/UserNotifications.h>

static NotificationsManager* s_manager;

@interface NotificationsManager ()
{
    NSInteger _listenerValue;
    MCDEventSubscription* _readerSubscription;
    NSMutableArray<MCDUserNotification*>* _notifications;
}


@property (nonatomic) NSMutableDictionary<NSNumber*, void(^)(void)>* listenerMap;
@property (nonatomic) MCDConnectedDevicesPlatform* platform;
@property (nonatomic) MCDUserNotificationChannel* channel;
@property (nonatomic) MCDUserNotificationReader* reader;
@property (nonatomic) BOOL platformStarted;
@end

@implementation NotificationsManager
- (instancetype)initWithPlatformManager:(ConnectedDevicesPlatformManager*)platformManager
{
    if (self = [super init])
    {
        _notifications = [NSMutableArray array];
        _listenerValue = 0;
        _listenerMap = [NSMutableDictionary dictionary];
        _platform = platformManager.platform;
        _platformStarted = NO;
        MCDUserDataFeed* _userDataFeed = [MCDUserDataFeed getForAccount:platformManager.accounts[0].mcdAccount
                                                              platform:self.platform
                                                    activitySourceHost:MSA_CLIENT_ID];
        [_userDataFeed startSync];
        self.channel = [MCDUserNotificationChannel channelWithUserDataFeed:_userDataFeed];
        self.reader = [self.channel createReader];
        _readerSubscription = [self.reader.dataChanged subscribe:^(__unused MCDUserNotificationReader* source, __unused MCDUserNotificationReaderDataChangedEventArgs* args){
            {
                NSLog(@"GraphNotifications Got an update!");
            };
            
        }];
        [self forceRead];
    }
    return self;
}


- (void)forceRead
{
    [self.reader readBatchAsyncWithMaxSize:NSUIntegerMax completion:^(NSArray<MCDUserNotification *> * _Nullable notifications, NSError * _Nullable error)
    {
        if (error)
        {
            NSLog(@"GraphNotifications Failed to read batch with error %@", error);
        }
        else
        {
            NSLog(@"GraphNotifications NotificationsManager got %ld notifications", notifications.count);
            [self _handleNotifications:notifications];
            for (void (^listener)(void) in self.listenerMap.allValues)
            {
                listener();
            }
        }
    }];
}

- (void)readNotification:(MCDUserNotification*)notification
{
    @synchronized (self)
    {
        notification.readState = MCDUserNotificationReadStateRead;
        [notification saveAsync:^(__unused MCDUserNotificationUpdateResult * _Nullable result, __unused NSError * _Nullable err)
        {
            NSLog(@"GraphNotifications Read notification with result %d error %@", result.succeeded, err);
        }];
    }
}

- (void)dismissNotificationFromTrayWithId:(NSString *)notificationId
{
    [[UNUserNotificationCenter currentNotificationCenter] removePendingNotificationRequestsWithIdentifiers:@[notificationId]];
    [[UNUserNotificationCenter currentNotificationCenter] removeDeliveredNotificationsWithIdentifiers:@[notificationId]];
}

- (void)dismissNotificationWithId:(NSString *)notificationId
{
    @synchronized (self)
    {
        for (MCDUserNotification* notification in self.notifications)
        {
            if ([notification.notificationId isEqualToString:notificationId])
            {
                [self dismissNotification:notification];
            }
        }
    }
}

- (void)dismissNotification:(MCDUserNotification*)notification
{
    @synchronized (self)
    {
        [self dismissNotificationFromTrayWithId:notification.notificationId];
        notification.userActionState = MCDUserNotificationUserActionStateActivated;
        [notification saveAsync:^(__unused MCDUserNotificationUpdateResult * _Nullable result, __unused NSError * _Nullable err)
        {
            NSLog(@"GraphNotifications Dismiss notification with result %d error %@", result.succeeded, err);
        }];
    }
}

+ (instancetype)sharedInstance
{
    @synchronized (self)
    {
        return s_manager;
    }
}

- (NSInteger)addNotificationsChangedListener:(void(^)(void))listener
{
    @synchronized (self)
    {
        _listenerMap[[NSNumber numberWithInteger:(++_listenerValue)]] = listener;
        return _listenerValue;
    }
}

- (void)removeListener:(NSInteger)token
{
    @synchronized (self)
    {
        [_listenerMap removeObjectForKey:[NSNumber numberWithInteger:token]];
    }
}

- (NSArray<MCDUserNotification*>*)notifications
{
    return _notifications;
}

- (void)_handleNotifications:(NSArray<MCDUserNotification*>*)notifications
{
    @synchronized (self)
    {
        NSLog(@"GraphNotifications Got %ld notifications!", notifications.count);
        for (MCDUserNotification* notification in notifications)
        {
            for (NSUInteger i = 0; i < _notifications.count; ++i)
            {
                if ([_notifications[i].notificationId isEqualToString:notification.notificationId])
                {
                    NSLog(@"GraphNotifications Found a match for %@", notification.notificationId);
                    [_notifications removeObjectAtIndex:i];
                    break;
                }
            }

            if (notification.status == MCDUserNotificationStatusActive)
            {
                NSLog(@"GraphNotifications Notification is active %@", notification.notificationId);
                [_notifications insertObject:notification atIndex:0];

                if ((notification.userActionState == MCDUserNotificationUserActionStateNoInteraction) && (notification.readState == MCDUserNotificationReadStateUnread))
                {
                    UNMutableNotificationContent* content = [UNMutableNotificationContent new];
                    content.title = @"New MSGraph Notification";
                    content.body = notification.content;
                    UNTimeIntervalNotificationTrigger* trigger = [UNTimeIntervalNotificationTrigger triggerWithTimeInterval:1 repeats:NO];
                    UNNotificationRequest* request = [UNNotificationRequest requestWithIdentifier:notification.notificationId content:content trigger:trigger];

                    [[UNUserNotificationCenter currentNotificationCenter] addNotificationRequest:request withCompletionHandler:^(NSError * _Nullable error)
                    {
                        if (error)
                        {
                            NSLog(@"GraphNotifications Failed to post local notification with error %@", error);
                        }
                        else
                        {
                            NSLog(@"GraphNotifications Successfully posted local notification request");
                        }
                    }];
                }
                else
                {
                    [self dismissNotificationFromTrayWithId:notification.notificationId];
                }
            }
            else
            {
                NSLog(@"GraphNotifications Notification is deleted %@", notification.notificationId);
                [self dismissNotificationFromTrayWithId:notification.notificationId];
            }
        }

        NSLog(@"GraphNotifications NotificationsManager now has %ld notifications", _notifications.count);
    }
}

- (void)_clearNotifications
{
    @synchronized (self)
    {
        [_notifications removeAllObjects];
        [[UNUserNotificationCenter currentNotificationCenter] removeAllPendingNotificationRequests];
        [[UNUserNotificationCenter currentNotificationCenter] removeAllDeliveredNotifications];
        for (void (^listener)(void) in self.listenerMap.allValues)
        {
            listener();
        }
    }
}

- (void)refresh
{
    NSLog(@"GraphNotifications Refreshing NotificationsManager");
    @synchronized (self)
    {
        [self _clearNotifications];

        // Starts setup based on new account, which since this is the current account will just refresh all of the actual setup
        [self setAccount:self.account];
    }
}

@end
