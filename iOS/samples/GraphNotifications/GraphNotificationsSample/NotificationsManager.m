//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "NotificationsManager.h"
#import "Secrets.h"
#import <UserNotifications/UserNotifications.h>

@implementation NotificationsManager {
    NSMutableArray<MCDUserNotification*>* _notifications;
    NSInteger _listenerValue;
    NSMutableDictionary<NSNumber*, void(^)(void)>* _listenerMap;
    MCDUserDataFeed* _feed;
    MCDUserNotificationChannel* _channel;
    MCDUserNotificationReader* _reader;
    MCDEventSubscription* _readerSubscription;
}

- (instancetype)initWithAccount:(MCDConnectedDevicesAccount*)account
                       platform:(MCDConnectedDevicesPlatform*)platform {
    if (self = [super init]) {
        _notifications = [NSMutableArray array];
        _listenerValue = 0;
        _listenerMap = [NSMutableDictionary dictionary];
        
        // Initialize the feed and subscribe for notifications
        _feed = [MCDUserDataFeed getForAccount:account
                                platform:platform
                                activitySourceHost:APP_HOST_NAME];
        [_feed.syncStatusChanged subscribe:^(MCDUserDataFeed* _Nonnull sender,
                                             __unused MCDUserDataFeedSyncStatusChangedEventArgs* _Nonnull args) {
            NSLog(@"SyncStatus is %ld", (long)sender.syncStatus);
        }];
        NSArray<MCDUserDataFeedSyncScope*>* syncScopes = @[ [MCDUserNotificationChannel syncScope] ];
        [_feed subscribeToSyncScopesAsync:syncScopes
                callback:^(BOOL success __unused, NSError* _Nullable error __unused) {
            // Start syncing down notifications
            [_feed startSync];
        }];
        
        // Create the channel and reader
        _channel = [MCDUserNotificationChannel channelWithUserDataFeed:_feed];
        _reader = [_channel createReader];
        _readerSubscription = [_reader.dataChanged subscribe:^(MCDUserNotificationReader* source,
                                                               __unused MCDUserNotificationReaderDataChangedEventArgs* args){
            {
                NSLog(@"GraphNotificationsSample got an update!");
                [self _readFromCache:source];
            };
            
        }];
        
        [self _readFromCache:_reader];
    }
    return self;
}

- (NSInteger)addNotificationsChangedListener:(void(^)(void))listener {
    @synchronized (self) {
        _listenerMap[[NSNumber numberWithInteger:(++_listenerValue)]] = listener;
        return _listenerValue;
    }
}

- (void)removeListener:(NSInteger)token {
    @synchronized (self) {
        [_listenerMap removeObjectForKey:[NSNumber numberWithInteger:token]];
    }
}

- (void)refresh {
    @synchronized (self) {
        [_feed startSync];
        [self _readFromCache:_reader];
    }
}

- (void)markRead:(MCDUserNotification*)notification {
    if (notification.readState == MCDUserNotificationReadStateUnread) {
        NSLog(@"Marking notification %@ as read", notification.notificationId);
        notification.readState = MCDUserNotificationReadStateRead;
        [notification saveAsync:^(__unused MCDUserNotificationUpdateResult * _Nullable result, NSError * _Nullable error) {
            if (error) {
                NSLog(@"Failed to mark the notification as read with error %@", error);
            } else {
                NSLog(@"Successfully marked the notification as read");
            }
        }];
    }
}

- (void)deleteNotification:(MCDUserNotification*)notification {
    NSLog(@"Deleting notification %@", notification.notificationId);
    [_channel
     deleteUserNotificationAsync:notification.notificationId
     completion:^(__unused MCDUserNotificationUpdateResult* _Nullable result, NSError* _Nullable error) {
         if (error) {
             NSLog(@"Failed to delete notifications with error %@", error);
         } else {
             NSLog(@"Successfully deleted the notification");
         }
     }];
}

- (void)dismissNotification:(MCDUserNotification*)notification {
    if (notification.userActionState == MCDUserNotificationUserActionStateNoInteraction) {
        NSLog(@"Dismissing notification %@", notification.notificationId);
        [self dismissNotificationFromTrayWithId:notification.notificationId];
        notification.userActionState = MCDUserNotificationUserActionStateDismissed;
        [notification saveAsync:^(__unused MCDUserNotificationUpdateResult * _Nullable result, __unused NSError * _Nullable error) {
            if (error) {
                NSLog(@"Failed to dismiss notifications with error %@", error);
            } else {
                 NSLog(@"Successfully dismissed the notification");
            }
         }];
    }
}

- (void)dismissNotificationFromTrayWithId:(NSString *)notificationId {
    [[UNUserNotificationCenter currentNotificationCenter] removePendingNotificationRequestsWithIdentifiers:@[notificationId]];
    [[UNUserNotificationCenter currentNotificationCenter] removeDeliveredNotificationsWithIdentifiers:@[notificationId]];
}

- (void)dismissNotificationWithId:(NSString *)notificationId {
    @synchronized (self) {
        for (MCDUserNotification* notification in self.notifications) {
            if ([notification.notificationId isEqualToString:notificationId]) {
                [self dismissNotification:notification];
            }
        }
    }
}

- (void)_readFromCache:(MCDUserNotificationReader*)reader {
    NSLog(@"Read notifications from cache");
    [reader readBatchAsyncWithMaxSize:100 completion:^(NSArray<MCDUserNotification *> * _Nullable notifications,
                                                       NSError * _Nullable error) {
        if (error) {
            NSLog(@"Failed to read notifications with error %@", error);
        } else {
            NSLog(@"NotificationsManager got %ld notifications to process", notifications.count);
            [self _handleNotifications:notifications];
            
            // Notify the listeners
            for (void (^listener)(void) in _listenerMap.allValues) {
                listener();
            }
        }
    }];
}

- (void)_handleNotifications:(NSArray<MCDUserNotification*>*)notifications {
    @synchronized (self) {
        for (MCDUserNotification* notification in notifications) {
            NSUInteger index = [_notifications
                indexOfObjectPassingTest:^BOOL(MCDUserNotification* existingNotification, NSUInteger __unused innerIndex, BOOL* stop) {
                    if ([existingNotification.notificationId isEqualToString:notification.notificationId]) {
                        *stop = YES;
                        return YES;
                    }
                    return NO;
                }];
            
            if (index != NSNotFound) {
                [_notifications removeObjectAtIndex:index];
            }

            if (notification.status == MCDUserNotificationStatusActive) {
                NSLog(@"Notification %@ is active", notification.notificationId);
                [_notifications insertObject:notification atIndex:0];

                if ((notification.userActionState == MCDUserNotificationUserActionStateNoInteraction)
                    && (notification.readState == MCDUserNotificationReadStateUnread)) {
                    UNMutableNotificationContent* content = [UNMutableNotificationContent new];
                    content.title = @"New Graph Notification";
                    content.body = notification.content;
                    UNTimeIntervalNotificationTrigger* trigger = [UNTimeIntervalNotificationTrigger
                                                                  triggerWithTimeInterval:1 repeats:NO];
                    UNNotificationRequest* request = [UNNotificationRequest requestWithIdentifier:notification.notificationId
                                                                                          content:content trigger:trigger];
                    [[UNUserNotificationCenter currentNotificationCenter] addNotificationRequest:request
                                                                           withCompletionHandler:^(NSError * _Nullable error) {
                        if (error) {
                            NSLog(@"Failed to post local notification with error %@", error);
                        } else {
                            NSLog(@"Successfully posted local notification request");
                        }
                    }];
                } else {
                    [self dismissNotificationFromTrayWithId:notification.notificationId];
                }
            } else {
                NSLog(@"Notification %@ is deleted", notification.notificationId);
                [self dismissNotificationFromTrayWithId:notification.notificationId];
            }
        }

        NSLog(@"NotificationsManager now has %ld notifications", _notifications.count);
    }
}

- (void)_clearAll {
    @synchronized (self) {
        [_notifications removeAllObjects];
        [[UNUserNotificationCenter currentNotificationCenter] removeAllPendingNotificationRequests];
        [[UNUserNotificationCenter currentNotificationCenter] removeAllDeliveredNotifications];
    }
}

@end
