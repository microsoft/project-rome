//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "TargetRemoteSystemTableViewController.h"
#import <ConnectedDevices/ConnectedDevices.h>
#import <MessageUI/MFMailComposeViewController.h>
#import "AppDelegate.h"
#import "AuthenticationViewController.h"

@implementation TargetRemoteSystemTableViewController
{
    BOOL clearTable;
}

- (id)initWithStyle:(UITableViewStyle)style
{
    self = [super initWithStyle:style];
    return self;
}

- (AppDataSource*)applicationData
{
    id<AppDelegateProtocol> dataDelegate = (id<AppDelegateProtocol>)[UIApplication sharedApplication].delegate;
    return (AppDataSource*)dataDelegate.dataSource;
}

- (void)viewDidLoad
{
    [super viewDidLoad];

    self.navigationItem.hidesBackButton = YES;

    UIRefreshControl* refreshControl = [UIRefreshControl new];

    [refreshControl addTarget:self action:@selector(refreshPeerSystemQuery) forControlEvents:UIControlEventValueChanged];

    self.refreshControl = refreshControl;

    self.navigationController.navigationBar.translucent = NO;
}

- (void)prepareForSegue:(UIStoryboardSegue*)segue sender:(__unused id)sender
{
    if ([segue.identifier isEqualToString:@"signOut"])
    {
        ((AuthenticationViewController*)segue.destinationViewController).shouldSignOut = YES;
    }
}

- (void)refreshPeerSystemQuery
{
    if (self.discoveredSystems)
    {
        [self.remoteSystemDiscoveryManager stopDiscovery];
    }

    self.discoveredSystems = [NSMutableArray new];
    self.remoteSystemDiscoveryManager = [[MCDRemoteSystemDiscoveryManager alloc] initWithDelegate:self];

    [self.remoteSystemDiscoveryManager startDiscovery];
    [self setPrompt:@"Discovering All Devices..."];

    [self refreshTable];
    [self.refreshControl beginRefreshing];
}

- (void)viewWillAppear:(BOOL)animated
{
    clearTable = YES;
    [self.tableView reloadData];
    clearTable = NO;

    [self refreshPeerSystemQuery];
    [super viewWillAppear:animated];
}

- (void)viewWillDisappear:(BOOL)animated
{
    [super viewWillDisappear:animated];
    [self setPrompt:nil];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
}

#pragma mark - Table view data source

- (NSInteger)numberOfSectionsInTableView:(__unused UITableView*)tableView
{
    return 1;
}

- (NSInteger)tableView:(__unused UITableView*)tableView numberOfRowsInSection:(__unused NSInteger)section
{
    if (clearTable)
    {
        return 0;
    }
    else
    {
        return [self.discoveredSystems count];
    }
}

- (UITableViewCell*)tableView:(UITableView*)tableView cellForRowAtIndexPath:(NSIndexPath*)indexPath
{
    UITableViewCell* cell = [tableView dequeueReusableCellWithIdentifier:@"TargetCell"];

    if ([self.discoveredSystems count] == 0)
    {
        return nil;
    }

    MCDRemoteSystem* system = self.discoveredSystems[indexPath.row];

    cell.textLabel.text = system.displayName;

    if (system.isAvailableByProximity)
    {
        cell.detailTextLabel.text = @"Available Proximally";
    }
    else
    {
        cell.detailTextLabel.text = @"Available via Cloud";
    }

    cell.accessoryType = UITableViewCellAccessoryDisclosureIndicator;

    return cell;
}

- (void)tableView:(__unused UITableView*)tableView didSelectRowAtIndexPath:(NSIndexPath*)indexPath
{
    self.applicationData.selectedSystem = [self.discoveredSystems objectAtIndex:indexPath.row];

    dispatch_async(dispatch_get_main_queue(), ^(void) {
        [self performSegueWithIdentifier:@"appContentSegue" sender:self];
    });
}

- (void)setPrompt:(NSString*)newPrompt
{
    dispatch_async(dispatch_get_main_queue(), ^(void) {
        self.navigationItem.prompt = newPrompt;
    });
}

- (void)refreshTable
{
    dispatch_async(dispatch_get_main_queue(), ^(void) {
        [self.tableView reloadData];
        [self.tableView setNeedsLayout];
        [self.tableView setNeedsDisplay];
    });
}

#pragma mark CDRemoveSystemDiscoveryManagerDelegate

- (void)remoteSystemDiscoveryManager:(__unused MCDRemoteSystemDiscoveryManager*)discoveryManager
                             didFind:(MCDRemoteSystem*)remoteSystem
{
    @synchronized(self)
    {
        if ([remoteSystem.displayName length] > 0)
        {
            [self.discoveredSystems addObject:remoteSystem];
            [self refreshTable];
        }
    }
}

- (void)remoteSystemDiscoveryManager:(__unused MCDRemoteSystemDiscoveryManager*)discoveryManager
                           didUpdate:(MCDRemoteSystem*)remoteSystem
{
    NSString* id = remoteSystem.id;

    @synchronized(self)
    {
        for (unsigned i = 0; i < self.discoveredSystems.count; i++)
        {
            MCDRemoteSystem* currentRemoteSystem = [self.discoveredSystems objectAtIndex:i];
            NSString* currentId = currentRemoteSystem.id;

            if ([currentId isEqualToString:id])
            {
                [self.discoveredSystems replaceObjectAtIndex:i withObject:remoteSystem];
                break;
            }
        }

        [self refreshTable];
    }
}

- (void)remoteSystemDiscoveryManagerDidComplete:(__unused MCDRemoteSystemDiscoveryManager*)discoveryManager
                                      withError:(NSError*)error
{
    if (error)
    {
        [self setPrompt:@"Discovery Complete with Error"];
    }
    else
    {
        [self setPrompt:@"Discovery Complete"];
    }

    [self.refreshControl endRefreshing];
}

@end
