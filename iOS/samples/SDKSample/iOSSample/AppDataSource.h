//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import "InboundRequestLogger.h"
#import "MSAAccountProvider.h"
#import "NotificationProvider.h"
#import <ConnectedDevices/Core/MCDPlatform.h>
#import <Foundation/Foundation.h>

@interface AppDataSource : NSObject
+ (AppDataSource*)sharedInstance;
@property(nonatomic) NotificationProvider* notificationProvider;
@property(nonatomic) MSAAccountProvider* accountProvider;
@property(nonatomic) InboundRequestLogger* inboundRequestLogger;
@property(nonatomic) MCDPlatform* platform;
@end
