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


- (instancetype)initWithPlatform:(MCDConnectedDevicesPlatform*)platform;

@property (nonatomic) NSMutableDictionary<NSNumber*, void(^)(void)>* listenerMap;
@property (nonatomic) MCDConnectedDevicesPlatform* platform;
@property (nonatomic) MCDUserNotificationChannel* channel;
@property (nonatomic) MCDUserNotificationReader* reader;
@property (nonatomic) BOOL platformStarted;
@end

@implementation NotificationsManager
- (instancetype)initWithPlatform:(MCDConnectedDevicesPlatform*)platform
{
    if (self = [super init])
    {
        _notifications = [NSMutableArray array];
        _listenerValue = 0;
        _listenerMap = [NSMutableDictionary dictionary];
        _platform = platform;
        _platformStarted = NO;
    }

    return self;
}

- (void)setAccount:(MCDConnectedDevicesAccount*)account
{
    @synchronized (self)
    {
        if (account)
        {
            NSLog(@"GraphNotifications Updating NotificationsManager with account %@", account.accountId);
            AppDelegate* delegate = (AppDelegate*)([UIApplication sharedApplication].delegate);

            if (!self.platformStarted)
            {
                NSLog(@"GraphNotifications Starting Platform!");
                [delegate startPlatform];
                self.platformStarted = YES;
            }

            NSLog(@"GraphNotifications Adding account to platform");
            [self.platform.accountManager addAccountAsync:account callback:^(__unused MCDConnectedDevicesAddAccountResult* result, __unused NSError* error)
            {
                // Don't use `setAccount:` here or we'll end up in an infinite loop :(
                self->_account = account;
                NSLog(@"GraphNotifications Registering notifications for account %@", account.accountId);
                [delegate registerNotificationsForAccount:account callback:^(__unused MCDConnectedDevicesNotificationRegistrationResult* result, __unused NSError* error)
                {
                    NSLog(@"GraphNotifications Initializing UserDataFeed!");
                    MCDUserDataFeed* dataFeed;
                    @try
                    {
                        dataFeed = [MCDUserDataFeed getForAccount:account platform:self.platform activitySourceHost:APP_HOST_NAME];
                    }
                    @catch (NSException* e)
                    {
                        NSLog(@"GraphNotifications Failed to initialize UserDataFeed with %@", e.description);
                        return;
                    }

                    NSLog(@"GraphNotifications Susbcribing for sync scopes async!");
                    [dataFeed subscribeToSyncScopesAsync:@[[MCDUserNotificationChannel syncScope]] callback:^(__unused BOOL result, __unused NSError * error)
                    {
                        @synchronized (self)
                        {
                            NSLog(@"GraphNotifications Initializing channel and reader");
                            [dataFeed startSync];
                            self.channel = [MCDUserNotificationChannel channelWithUserDataFeed:dataFeed];
                            self.reader = [self.channel createReader];

                            __weak typeof(self) weakSelf = self;
                            _readerSubscription = [self.reader.dataChanged subscribe:^(__unused MCDUserNotificationReader* source, __unused MCDUserNotificationReaderDataChangedEventArgs* args)
                            {
                                NSLog(@"GraphNotifications Got an update!");
                                [weakSelf forceRead];
                            }];

                            [self forceRead];
                        }
                    }];
                }];
            }];
        } else if (self.platformStarted)
        {
            [self.platform.accountManager removeAccountAsync:account callback:^(__unused MCDConnectedDevicesRemoveAccountResult* result, NSError* error)
            {
                @synchronized (self)
                {
                    if (error)
                    {
                        NSLog(@"GraphNotifications failed to remove account from platform with error %@", error.description);
                    }

                    // Don't use `setAccount:` here or we'll end up in an infinite loop :(
                    self->_account = nil;
                    self.channel = nil;
                    self.reader = nil;
                }
            }];
        }
    };
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

+ (instancetype)startWithPlatform:(MCDConnectedDevicesPlatform*)platform
{
    @synchronized (self)
    {
        if (s_manager == nil)
        {
            s_manager = [[self alloc] initWithPlatform:platform];
        }

        return s_manager;
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
