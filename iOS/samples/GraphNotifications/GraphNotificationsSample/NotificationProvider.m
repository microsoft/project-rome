//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "NotificationProvider.h"
#import <UIKit/UIKit.h>

@implementation NotificationProvider
{
    MCDRegistrationUpdatedEvent* _registrationUpdated;
    MCDNotificationRegistration* _notificationRegistration;
}

+ (instancetype)sharedInstance
{
    static NotificationProvider* sharedInstance = nil;

    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{ sharedInstance = [[NotificationProvider alloc] init]; });

    return sharedInstance;
}

+ (void)updateNotificationRegistration:(MCDNotificationRegistration*)notificationRegistration
{
    NSLog(@"Raise notification registration changed event\nType: %ld\nApplication: %@", (long)notificationRegistration.type,
        notificationRegistration.identifier);
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0),
        ^{ [[NotificationProvider sharedInstance] _updateNotificationRegistration:notificationRegistration]; });
}

// MCDNotificationProvider
@synthesize registrationUpdated = _registrationUpdated;

- (void)getNotificationRegistrationAsync:(nonnull void (^)(MCDNotificationRegistration* _Nullable, NSError* _Nullable))completionBlock
{
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{ completionBlock(_notificationRegistration, nil); });
}

- (instancetype)init
{
    if (self = [super init])
    {
        @try
        {
            _registrationUpdated = [MCDRegistrationUpdatedEvent new];
        }
        @catch(NSException* e) {
            NSLog(@"Failed to create new MCDRegistrationUpdatedEvent with exception %@", e.description);
        }
    }

    return self;
}

- (void)_updateNotificationRegistration:(MCDNotificationRegistration*)notificationRegistration
{
    _notificationRegistration = notificationRegistration;
    @try
    {
        [_registrationUpdated raiseWithNotificationRegistration:notificationRegistration];
    }
    @catch(NSException* e) {
        NSLog(@"Failed to update notification registration with exception %@", e.description);
    }
}
@end
