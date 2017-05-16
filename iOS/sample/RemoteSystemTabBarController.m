//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "RemoteSystemTabBarController.h"
#import "AppDataSource.h"

@implementation RemoteSystemTabBarController

- (void)viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];
    self.navigationItem.title = [[AppDataSource instance].selectedSystem displayName];
}

@end
