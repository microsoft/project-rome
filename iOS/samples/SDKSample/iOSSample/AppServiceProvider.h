//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import <ConnectedDevicesRemoteSystemsCommanding/ConnectedDevicesRemoteSystemsCommanding.h>
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@class AppServiceProvider;

@protocol AppServiceProviderDelegate
- (void)appServiceProvider:(AppServiceProvider*)self didOpenConnection:(MCDAppServiceConnection*)appServiceConnection;
- (void)appServiceConnection:(MCDAppServiceConnection*)appServiceConnection
           didReceiveRequest:(MCDAppServiceRequestReceivedEventArgs*)requestArgs;
@end

@interface AppServiceProvider : NSObject <MCDAppServiceProvider>
@property(nonatomic, weak) id<AppServiceProviderDelegate> delegate;
- (instancetype)initWithDelegate:(id<AppServiceProviderDelegate>)delegate;
@end
