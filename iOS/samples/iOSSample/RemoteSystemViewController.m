//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "RemoteSystemViewController.h"
#import "IdentityViewController.h"
#import "LaunchAndMessageViewController.h"
#import <ConnectedDevices/Commanding/MCDRemoteSystemApplicationRegistrationBuilder.h>
#import <ConnectedDevices/Core/MCDPlatform.h>
#import <ConnectedDevices/Discovery/MCDRemoteSystemApplication.h>
#import <ConnectedDevices/Discovery/MCDRemoteSystemAuthorizationKindFilter.h>
#import <ConnectedDevices/Discovery/MCDRemoteSystemDiscoveryTypeFilter.h>
#import <ConnectedDevices/Discovery/MCDRemoteSystemKindFilter.h>
#import <ConnectedDevices/Discovery/MCDRemoteSystemKinds.h>
#import <ConnectedDevices/Discovery/MCDRemoteSystemLocalVisibilityKindFilter.h>
#import <ConnectedDevices/Discovery/MCDRemoteSystemStatusTypeFilter.h>
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@interface RemoteSystemViewController ()
{
    // Create a RemoteSystemWatcher to discover devices
    MCDRemoteSystemWatcher* _watcher;
}
@end

@implementation RemoteSystemViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    _delegate = self;

    _discoveredSystems = [NSMutableArray new];
}

- (IBAction)discoverDevicesButtonPressed:(id)sender
{
    [self startDiscovery];
}

// Handle when RemoteSystems are added
- (void)_onRemoteSystemAdded:(MCDRemoteSystem*)system
{
    @synchronized(self)
    {
        [_discoveredSystems addObject:system];
    }

    [_delegate remoteSystemsDidUpdate];
}

// Handle when RemoteSystems are updated
- (void)_onRemoteSystemUpdated:(MCDRemoteSystem*)system
{
    @synchronized(self)
    {
        for (unsigned i = 0; i < _discoveredSystems.count; i++)
        {
            MCDRemoteSystem* cachedSystem = [_discoveredSystems objectAtIndex:i];
            if ([cachedSystem.displayName isEqualToString:system.displayName])
            {
                [_discoveredSystems replaceObjectAtIndex:i withObject:system];
                break;
            }
        }
    }

    [_delegate remoteSystemsDidUpdate];
}

// Handle when RemoteSystems are removed
- (void)_onRemoteSystemRemoved:(MCDRemoteSystem*)system
{
    @synchronized(self)
    {
        for (unsigned i = 0; i < _discoveredSystems.count; i++)
        {
            MCDRemoteSystem* cachedSystem = [_discoveredSystems objectAtIndex:i];
            if ([cachedSystem.displayName isEqualToString:system.displayName])
            {
                [_discoveredSystems removeObjectAtIndex:i];
                break;
            }
        }
    }

    [_delegate remoteSystemsDidUpdate];
}

// Start watcher with filter for transport types, form factors
- (void)startWatcherWithFilter:(NSMutableArray<NSObject<MCDRemoteSystemFilter>*>*)remoteSystemFilter
{
    // Stop previous watcher if one exists
    if (_watcher)
    {
        [_watcher stop];
    }

    // Clear out previously discovered systems, so we're starting fresh
    @synchronized(self)
    {
        [_discoveredSystems removeAllObjects];
    }
    [_delegate remoteSystemsDidUpdate];

    _watcher =
        (remoteSystemFilter.count > 0) ? [[MCDRemoteSystemWatcher alloc] initWithFilters:remoteSystemFilter] : [MCDRemoteSystemWatcher new];

    RemoteSystemViewController* __weak weakSelf = self;
    [_watcher addRemoteSystemAddedListener:^(
        __unused MCDRemoteSystemWatcher* watcher, MCDRemoteSystem* system) { [weakSelf _onRemoteSystemAdded:system]; }];

    [_watcher addRemoteSystemUpdatedListener:^(
        __unused MCDRemoteSystemWatcher* watcher, MCDRemoteSystem* system) { [weakSelf _onRemoteSystemUpdated:system]; }];

    [_watcher addRemoteSystemRemovedListener:^(
        __unused MCDRemoteSystemWatcher* watcher, MCDRemoteSystem* system) { [weakSelf _onRemoteSystemRemoved:system]; }];
    [_watcher start];
}

- (void)startDiscovery
{
    NSMutableArray<NSObject<MCDRemoteSystemFilter>*>* filters = [NSMutableArray new];
    [filters addObject:[MCDRemoteSystemLocalVisibilityKindFilter filterWithLocalVisibilityKind:MCDRemoteSystemLocalVisibilityKindShowAll]];

    [self startWatcherWithFilter:filters];
}

- (void)prepareForSegue:(UIStoryboardSegue*)segue sender:(id)sender
{

    // Choose from one of the RemoteSystemApplications in the list to communicate with
    if ([segue.identifier isEqualToString:@"launcher"])
    {
        UITableViewCell* sourceCell = (UITableViewCell*)sender;
        NSIndexPath* indexPath = [self.tableView indexPathForCell:sourceCell];
        MCDRemoteSystem* selectedSystem = [self.discoveredSystems objectAtIndex:indexPath.section];
        MCDRemoteSystemApplication* selectedApplication = [selectedSystem.applications objectAtIndex:indexPath.row];
        LaunchAndMessageViewController* destination = (LaunchAndMessageViewController*)segue.destinationViewController;
        destination.selectedApplication = selectedApplication;
    }
}

#pragma mark - Table view data source

- (NSInteger)numberOfSectionsInTableView:(__unused UITableView*)tableView
{
    return self.discoveredSystems.count;
}

- (NSInteger)tableView:(__unused UITableView*)tableView numberOfRowsInSection:(__unused NSInteger)section
{
    return [[self.discoveredSystems objectAtIndex:section] applications].count;
}

- (NSString*)tableView:(__unused UITableView*)tableView titleForHeaderInSection:(NSInteger)section
{
    MCDRemoteSystem* system = [self.discoveredSystems objectAtIndex:section];
    return [NSString stringWithFormat:@"%@, id: %@, kind: %@, mfr: %@, model: %@", system.displayName, system.systemId, system.kind,
                     system.manufacturerDisplayName, system.modelDisplayName];
}

- (CGFloat)tableView:(__unused UITableView*)tableView heightForRowAtIndexPath:(__unused NSIndexPath*)indexPath
{
    // 4 lines of text, each 20 tall, with 10 padding top and bottom.
    return 4 * 20 + 20;
}

- (UITableViewCell*)tableView:(UITableView*)tableView cellForRowAtIndexPath:(nonnull NSIndexPath*)indexPath
{
    NSString* SimpleIdentitfier = @"SimpleIdentifier";

    // Return RemoteApplications
    UITableViewCell* cell = [tableView dequeueReusableCellWithIdentifier:SimpleIdentitfier];

    if ([self.discoveredSystems count] == 0)
    {
        return nil;
    }

    // A RemoteSystem is mapped to the device
    // A RemoteApplication belongs to a RemoteSystem, simply, the app running on the device
    MCDRemoteSystem* system = [self.discoveredSystems objectAtIndex:indexPath.section];
    MCDRemoteSystemApplication* application = [system.applications objectAtIndex:indexPath.row];

    cell.textLabel.numberOfLines = 0;
    cell.textLabel.lineBreakMode = NSLineBreakByTruncatingTail;
    cell.textLabel.text = [NSString stringWithFormat:@"%@\nproximal:%@\nspatial:%@\nid:%@", application.displayName,
                                    application.isAvailableByProximity ? @"YES" : @"NO",
                                    application.isAvailableBySpatialProximity ? @"YES" : @"NO", application.applicationId];

    return cell;
}

#pragma mark - Table view delegate
- (void)tableView:(__unused UITableView*)tableView didSelectRowAtIndexPath:(NSIndexPath*)indexPath
{
    MCDRemoteSystem* selectedSystem = [self.discoveredSystems objectAtIndex:indexPath.section];

    self.selectedSystem = selectedSystem;
    self.selectedApplication = [selectedSystem.applications objectAtIndex:indexPath.row];
}

- (void)tableView:(__unused UITableView*)tableView didDeselectRowAtIndexPath:(__unused NSIndexPath*)indexPath
{
    self.selectedSystem = nil;
    self.selectedApplication = nil;
}

#pragma mark - RemoteSystems Delegate
- (void)remoteSystemsDidUpdate
{
    dispatch_async(dispatch_get_main_queue(), ^(void) {
        [self.tableView reloadData];
        [self.tableView setNeedsLayout];
        [self.tableView setNeedsDisplay];
    });
}

@end
