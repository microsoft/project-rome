//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import <ConnectedDevices/Core/Core.h>
#import <UIKit/UIKit.h>

// @brief provides an sample implementation of MCDNotificationProvider
@interface NotificationProvider : NSObject <MCDNotificationProvider>
// @brief class method update the notification registration provider with new notification registration
- (void)updateNotificationRegistration:(MCDNotificationRegistration*)notificationRegistration;
@end
