//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import <UIKit/UIKit.h>

#import "AppDataSource.h"

@protocol AppDelegateProtocol
- (AppDataSource*)dataSource;
@end

@interface AppDelegate : UIResponder <UIApplicationDelegate, AppDelegateProtocol>

@property (nonatomic, strong) UIWindow* window;
@property (nonatomic, strong) AppDataSource* dataSource;

@end
