//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import <ConnectedDevices/ConnectedDevices.h>
#import <Foundation/Foundation.h>

@interface AppDataSource : NSObject

@property (readwrite, nonatomic, weak) MCDRemoteSystem* selectedSystem;

+ (AppDataSource*)instance;

@end
