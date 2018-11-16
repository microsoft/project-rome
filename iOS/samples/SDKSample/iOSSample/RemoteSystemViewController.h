//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import <ConnectedDevices/RemoteSystems/MCDRemoteSystemFilter.h>
#import <ConnectedDevices/RemoteSystems/MCDRemoteSystemWatcher.h>
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@protocol RemoteSystemsDelegate
- (void)remoteSystemsDidUpdate;
@end

@interface RemoteSystemViewController : UITableViewController <UITableViewDataSource, UITableViewDelegate, RemoteSystemsDelegate>

@property(nonatomic, weak, nullable) id<RemoteSystemsDelegate> delegate;
@property(readonly, strong, nullable) NSMutableArray<MCDRemoteSystem*>* discoveredSystems;
@property MCDRemoteSystem* selectedSystem;
@property MCDRemoteSystemApp* selectedApplication;
@property(weak, nonatomic) IBOutlet UIButton* discoverDevicesButton;

- (void)startWatcherWithFilter:(nonnull NSMutableArray<NSObject<MCDRemoteSystemFilter>*>*)remoteSystemFilter;

@end
