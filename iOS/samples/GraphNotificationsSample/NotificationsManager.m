
#import "NotificationsManager.h"

static NotificationsManager* s_manager;

@interface NotificationsManager () {
    NSInteger _listenerValue;
    NSUInteger _readerRegistrationToken;
    NSMutableArray<MCDUserNotification*>* _notifications;
}

@property (nonatomic) NSMutableDictionary<NSNumber*, void(^)(void)>* listenerMap;
@property (nonatomic) MCDPlatform* platform;
@property (nonatomic) MCDUserNotificationChannel* channel;
@property (nonatomic) MCDUserNotificationReader* reader;
- (instancetype)initWithAccountProvider:(id<MCDUserAccountProvider>)accountProvider platform:(MCDPlatform*)platform;
@end

@implementation NotificationsManager
- (instancetype)initWithAccountProvider:(AADMSAAccountProvider*)accountProvider platform:(MCDPlatform*)platform {
    if (self = [super init]) {
        _listenerValue = 0;
        _listenerMap = [NSMutableDictionary dictionary];
        _accountProvider = accountProvider;
        _platform = platform;
        
        __weak typeof(self) weakSelf = self;
        [_accountProvider.userAccountChanged subscribe:^ {
            if ([weakSelf.accountProvider getUserAccounts].count > 0) {
                [weakSelf setupWithAccount:[weakSelf.accountProvider getUserAccounts][0]];
            } else {
                for (void (^listener)(void) in weakSelf.listenerMap.allValues) {
                    listener();
                }
            }
        }];
    }
    
    return self;
}

- (void)setupWithAccount:(MCDUserAccount*)account {
    @synchronized (self) {
        MCDUserDataFeed* dataFeed = [MCDUserDataFeed userDataFeedForAccount:account platform:_platform activitySourceHost:APP_HOST_NAME];
        self.channel = [MCDUserNotificationChannel userNotificationChannelWithUserDataFeed:dataFeed];
        self.reader = [self.channel createReader];
        
        __weak typeof(self) weakSelf;
        _readerRegistrationToken = [self.reader addDataChangedListener:^(__unused MCDUserNotificationReader* source) {
            [weakSelf forceRead];
        }];
    }
    
}

- (void)forceRead {
    [self.reader readBatchAsyncWithMaxSize:0 completion:^(NSArray<MCDUserNotification *> * _Nullable notifications, NSError * _Nullable error) {
        if (error) {
            NSLog(@"Failed to read batch with error %@", error);
        } else {
            [self _handleNotifications:notifications];
            for (void (^listener)(void) in self.listenerMap.allValues) {
                listener();
            }
        }
    }];
}

- (void)readNotificationAtIndex:(NSUInteger)index {
    @synchronized (self) {
        self.notifications[index].readState = MCDUserNotificationReadStateRead;
        [self.notifications[index] saveAsync:^(__unused MCDUserNotificationUpdateResult * _Nullable result, __unused NSError * _Nullable err) {
            // Do Nothing
        }];
    }
}

- (void)dismissNotificationAtIndex:(NSUInteger)index {
    @synchronized (self) {
        self.notifications[index].userActionState = MCDUserNotificationUserActionStateDismissed;
        [self.notifications[index] saveAsync:^(__unused MCDUserNotificationUpdateResult * _Nullable result, __unused NSError * _Nullable err) {
            // Do Nothing
        }];
    }
}

+ (instancetype)startWithAccountProvider:(AADMSAAccountProvider*)accountProvider platform:(MCDPlatform*)platform {
    @synchronized (self) {
        if (s_manager == nil) {
            s_manager = [[self alloc] initWithAccountProvider:accountProvider platform:platform];
        }
        
        return s_manager;
    }
}

+ (instancetype)sharedInstance {
    @synchronized (self) {
        return s_manager;
    }
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

- (NSArray<MCDUserNotification*>*)notifications {
    return _notifications;
}

- (void)_handleNotifications:(NSArray<MCDUserNotification*>*)notifications {
    @synchronized (self) {
        for (MCDUserNotification* notification in notifications) {
            NSUInteger index = [_notifications indexOfObjectPassingTest:^BOOL(MCDUserNotification * _Nonnull obj, __unused NSUInteger idx, __unused BOOL * _Nonnull stop) {
                return [obj.notificationId isEqualToString:notification.notificationId];
            }];
            
            if (index != NSNotFound) {
                [_notifications removeObjectAtIndex:index];
            }
            
            if (notification.status == MCDUserNotificationStatusActive) {
                [_notifications insertObject:notification atIndex:0];
            }
        }
    }
}

@end
