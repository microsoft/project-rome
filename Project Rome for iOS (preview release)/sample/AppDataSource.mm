//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "AppDataSource.h"
#import "AppDelegate.h"

@implementation AppDataSource

+ (AppDataSource*)instance
{
    id<AppDelegateProtocol> dataDelegate = (id<AppDelegateProtocol>)[UIApplication sharedApplication].delegate;
    return (AppDataSource*)dataDelegate.dataSource;
}

@end
