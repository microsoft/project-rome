//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "NotificationsViewController.h"
#import "NotificationsManager.h"
#import "ConnectedDevicesPlatformManager.h"

@implementation NotificationsViewController {
    NotificationsManager* _notificationsManager;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    [self refresh];
}

- (void)_initNotificationsManager {
    @synchronized (self) {
        if (_notificationsManager == nil) {
            _notificationsManager = [[ConnectedDevicesPlatformManager sharedInstance] notificationsManager];
            
            __weak typeof(self) weakSelf = self;
            [_notificationsManager addNotificationsChangedListener: ^{
                NSLog(@"NotificationsViewController notified of changes!");
                [weakSelf _updateView];
            }];
        }
    }
}

- (void)_updateView {
    dispatch_async(dispatch_get_main_queue(), ^{
           [self.tableView reloadData];
       });
}

- (IBAction)refresh {
    [self _initNotificationsManager];
    if (_notificationsManager != nil) {
        [_notificationsManager refresh];
    }
}

- (NSInteger)numberOfSectionsInTableView:(__unused UITableView*)tableView {
    return 1;
}

- (NSInteger)tableView:(nonnull __unused UITableView*)tableView numberOfRowsInSection:(__unused NSInteger)section {
    return _notificationsManager.notifications.count;
}

- (nonnull UITableViewCell*)tableView:(nonnull UITableView*)tableView cellForRowAtIndexPath:(nonnull NSIndexPath*)indexPath {
    UITableViewCell* cell = [tableView dequeueReusableCellWithIdentifier:@"NotificationCell" forIndexPath:indexPath];
    MCDUserNotification* notification = _notificationsManager.notifications[indexPath.row];
    NSLog(@"NotificationsViewController updating cell for notification %@", notification.notificationId);

    UILabel* idLabel = (UILabel*)[cell viewWithTag:1];
    [idLabel setText:notification.notificationId];
    [idLabel setFont:[UIFont boldSystemFontOfSize:20]];

    UILabel* contentLabel = (UILabel*)[cell viewWithTag:2];
    [contentLabel setText:notification.content];

    UILabel* actionStateLabel = (UILabel*)[cell viewWithTag:3];
    UIButton* dismissButton = (UIButton*)[cell viewWithTag:5];
    [dismissButton setTag:indexPath.row];
    if (notification.userActionState == MCDUserNotificationUserActionStateNoInteraction) {
        [actionStateLabel setText:@"No Interaction"];
        dismissButton.enabled = YES;
        [dismissButton addTarget:self action:@selector(handleDismiss:) forControlEvents:UIControlEventTouchUpInside];
    } else {
        [actionStateLabel setText:@"Dismissed"];
        dismissButton.enabled = NO;
    }

    UIButton* readButton = (UIButton*)[cell viewWithTag:4];
    [readButton setTag:indexPath.row];
    if (notification.readState == MCDUserNotificationReadStateUnread) {
        [idLabel setTextColor:[UIColor greenColor]];
        readButton.enabled = YES;
        [readButton addTarget:self action:@selector(handleRead:) forControlEvents:UIControlEventTouchUpInside];
    } else {
        [idLabel setTextColor:[UIColor redColor]];
        readButton.enabled = NO;
    }
    
    UIButton* deleteButton = (UIButton*)[cell viewWithTag:6];
    [deleteButton setTag:indexPath.row];
    [deleteButton addTarget:self action:@selector(handleDelete:) forControlEvents:UIControlEventTouchUpInside];
    
    return cell;
}

- (IBAction)handleRead:(UIButton*)button {
    MCDUserNotification* selected = _notificationsManager.notifications[button.tag];
    [_notificationsManager markRead:selected];
}

- (IBAction)handleDismiss:(UIButton*)button {
    MCDUserNotification* selected = _notificationsManager.notifications[button.tag];
    [_notificationsManager dismissNotification:selected];
}

- (IBAction)handleDelete:(UIButton*)button {
    MCDUserNotification* selected = _notificationsManager.notifications[button.tag];
    [_notificationsManager deleteNotification:selected];
}

@end
