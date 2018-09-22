//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import <UIKit/UIKit.h>
#import <ConnectedDevices/Core/Core.h>

// @brief provides an sample implementation of MCDNotificationProvider
@interface NotificationProvider : NSObject <MCDNotificationProvider>

// @brief get the shared instance of MCDNotificationProvider
+ (nullable instancetype)sharedInstance;

// @brief class method update the notification registration provider with new notification registration
+ (void)updateNotificationRegistration:(nonnull MCDNotificationRegistration*)notificationRegistration;

@end
