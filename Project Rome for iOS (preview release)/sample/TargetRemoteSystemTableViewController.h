//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import <ConnectedDevices/ConnectedDevices.h>
#import <MessageUI/MFMailComposeViewController.h>
#import <UIKit/UIKit.h>

@interface TargetRemoteSystemTableViewController
    : UITableViewController <CDRemoteSystemDiscoveryManagerDelegate, MFMailComposeViewControllerDelegate>

@property (strong, nonatomic) NSMutableArray* discoveredSystems;
@property (strong, nonatomic) CDRemoteSystemDiscoveryManager* remoteSystemDiscoveryManager;

@end
