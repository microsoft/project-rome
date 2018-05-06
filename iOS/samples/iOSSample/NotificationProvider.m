//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "NotificationProvider.h"
#import "ConnectedDevices/Core/MCDPlatform.h"
#import <UIKit/UIKit.h>

@implementation NotificationProvider
{
    MCDRegistrationUpdatedEvent* _registrationUpdated;
    MCDNotificationRegistration* _notificationRegistration;
}

- (instancetype)init
{
    if (self = [super init])
    {
        _registrationUpdated = [MCDRegistrationUpdatedEvent new];
    }

    return self;
}

- (void)updateNotificationRegistration:(MCDNotificationRegistration*)notificationRegistration
{
    NotificationProvider* __weak weakSelf = self;
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0),
        ^{ [weakSelf _updateNotificationRegistration:notificationRegistration]; });
}

#pragma mark - MCDNotificationProvider Protocol Requirements

@synthesize registrationUpdated = _registrationUpdated;

- (void)getNotificationRegistrationAsync:(nonnull void (^)(MCDNotificationRegistration* _Nullable, NSError* _Nullable))completionBlock
{
    dispatch_async(
        dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{ completionBlock(self->_notificationRegistration, nil); });
}

- (void)_updateNotificationRegistration:(MCDNotificationRegistration*)notificationRegistration
{
    _notificationRegistration = notificationRegistration;

    [_registrationUpdated raiseWithNotificationRegistration:notificationRegistration];
}
@end
