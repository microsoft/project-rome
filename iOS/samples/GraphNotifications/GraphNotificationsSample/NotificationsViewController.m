//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "NotificationsViewController.h"

@interface NotificationsViewController()
{
}

@property (nonatomic) NSMutableArray<MCDUserNotification*>* notifications;
- (void)updateData;
- (void)handleTap:(UITapGestureRecognizer*)recognizer;
@end

@implementation NotificationsViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    _notifications = [NSMutableArray array];
    // Do any additional setup after loading the view, typically from a nib.

    __weak typeof(self) weakSelf = self;
    [[NotificationsManager sharedInstance] addNotificationsChangedListener:
    ^{
        NSLog(@"GraphNotifications NotificationsViewController notified of changes!");
        dispatch_async(dispatch_get_main_queue(),
        ^{
            [weakSelf updateData];
            [weakSelf.tableView reloadData];
        });
    }];

    dispatch_async(dispatch_get_main_queue(),
    ^{
        [self updateData];
        [self.tableView reloadData];
    });
}

- (NSInteger)numberOfSectionsInTableView:(__unused UITableView*)tableView
{
    return 1;
}

- (nonnull UITableViewCell*)tableView:(nonnull UITableView*)tableView cellForRowAtIndexPath:(nonnull NSIndexPath*)indexPath
{
    UITableViewCell* cell = [tableView dequeueReusableCellWithIdentifier:@"NotificationCell" forIndexPath:indexPath];

    MCDUserNotification* notification = self.notifications[indexPath.row];

    UILabel* idLabel = (UILabel*)[cell viewWithTag:1];
    [idLabel setText:notification.notificationId];

    UILabel* contentLabel = (UILabel*)[cell viewWithTag:2];
    [contentLabel setText:notification.content];

    UILabel* userActionStateLabel = (UILabel*)[cell viewWithTag:3];
    [userActionStateLabel setText:((notification.userActionState == MCDUserNotificationUserActionStateNoInteraction) ? @"No Interaction" : @"Activated")];

    UIButton* readButton = (UIButton*)[cell viewWithTag:4];
    if (notification.readState == MCDUserNotificationReadStateUnread)
    {
        readButton.enabled = YES;
        [readButton addTarget:self action:@selector(handleRead:) forControlEvents:UIControlEventTouchDown];
    }
    else
    {
        readButton.enabled = NO;
    }

    if (notification.userActionState == MCDUserNotificationUserActionStateNoInteraction)
    {
        [cell addGestureRecognizer:[[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(handleTap:)]];
    }
    return cell;
}

- (NSInteger)tableView:(nonnull __unused UITableView*)tableView numberOfRowsInSection:(__unused NSInteger)section
{
    return self.notifications.count;
}

- (void)handleTap:(UITapGestureRecognizer *)recognizer
{
    [[NotificationsManager sharedInstance] dismissNotification:(self.notifications[[self.tableView indexPathForCell:(UITableViewCell*)recognizer.view].row])];
}

- (void)updateData
{
    @synchronized (self)
    {
        [self.notifications removeAllObjects];
        [self.notifications addObjectsFromArray:[NotificationsManager sharedInstance].notifications];
        NSLog(@"GraphNotifications Got %ld valid objects for NotificationsViewController out of %ld", self.notifications.count, [NotificationsManager sharedInstance].notifications.count);
    }
}

- (void)handleRead:(id)button
{
    [[NotificationsManager sharedInstance] readNotification:(self.notifications[[self.tableView indexPathForCell:(UITableViewCell*)((UIView*)button).superview].row])];
}

- (IBAction)refresh
{
    [[NotificationsManager sharedInstance] refresh];
}
@end
