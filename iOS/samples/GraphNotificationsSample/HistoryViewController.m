
#import <Foundation/Foundation.h>
#import "HistoryViewController.h"
#import "AdaptiveCards/ACFramework.h"


@interface HistoryViewController() {
}
@property (nonatomic) NSMutableArray<MCDUserNotification*>* notifications;
- (void)updateData;
- (void)handleTap:(UITapGestureRecognizer*)recognizer;
@end

@implementation HistoryViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view, typically from a nib.
    
    __weak typeof(self) weakSelf = self;
    [[NotificationsManager sharedInstance] addNotificationsChangedListener:^{
        [self updateData];
        dispatch_async(dispatch_get_main_queue(), ^{
            [weakSelf.tableView reloadData];
        });
    }];
    
    [self updateData];
}

- (NSInteger)numberOfSectionsInTableView:(__unused UITableView*)tableView
{
    return 1;
}

- (nonnull UITableViewCell*)tableView:(nonnull UITableView*)tableView cellForRowAtIndexPath:(nonnull __unused NSIndexPath*)indexPath
{
    UITableViewCell* cell = [tableView dequeueReusableCellWithIdentifier:@"NotificationCell"];
    MCDUserNotification* notification = self.notifications[indexPath.row];
    
    ACOAdaptiveCardParseResult* parseResult = [ACOAdaptiveCard fromJson:notification.content];
    ACRRenderResult* renderResult = [ACRRenderer render:parseResult.card config:[ACOHostConfig new] widthConstraint:0.0f];
    
    [cell.contentView addSubview:renderResult.view];
    [cell addGestureRecognizer:[[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(handleTap:)]];
    return cell;
}

- (NSInteger)tableView:(nonnull __unused UITableView*)tableView numberOfRowsInSection:(__unused NSInteger)section
{
    return self.notifications.count;
}

- (void)handleTap:(UITapGestureRecognizer *)recognizer {
    [[NotificationsManager sharedInstance] readNotificationAtIndex:[self.tableView indexPathForCell:(UITableViewCell*)recognizer.view].row];
}

- (void)updateData {
    @synchronized (self) {
        [self.notifications removeAllObjects];
        for (MCDUserNotification* notification in [NotificationsManager sharedInstance].notifications) {
            if (notification.status == MCDUserNotificationStatusActive && notification.readState == MCDUserNotificationReadStateUnread && notification.userActionState != MCDUserNotificationUserActionStateNoInteraction) {
                [self.notifications addObject:notification];
            }
        }
    }
}

@end

