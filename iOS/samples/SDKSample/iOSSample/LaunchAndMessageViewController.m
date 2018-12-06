//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "LaunchAndMessageViewController.h"
#import "Secrets.h"
#import <ConnectedDevices/RemoteSystems.Commanding/RemoteSystems.Commanding.h>
#import <ConnectedDevices/RemoteSystems/MCDRemoteSystemApp.h>
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

// For app service messaging, an app service must be available from another device
// For this sample, use the UWP test app found here - http://aka.ms/romeapp
// Messaging can done in 4 steps:
// Step #1:  Establish an app service connection
// Step #2:  Create a message to send
// Step #3:  Send a message using the app service connection
// Step #4:  Send the message and get a response

@interface LaunchAndMessageViewController ()
{
    MCDAppServiceConnection* _appServiceConnection;
    NSUInteger _requestReceivedRegistration;
    NSUInteger _serviceClosedRegistration;
    NSDateFormatter* _dateFormatter;
}
@end

@implementation LaunchAndMessageViewController

- (instancetype)initWithCoder:(NSCoder*)aDecoder
{
    if (self = [super initWithCoder:aDecoder])
    {
        _dateFormatter = [[NSDateFormatter alloc] init];
        [_dateFormatter setDateFormat:@"MM/dd/yyyy HH:mm:ss"];
    }
    return self;
}

- (void)viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];
    if (_selectedApplication)
    {
        self.applicationNameLabel.text = _selectedApplication.displayName;
    }
}

// Send a remote launch of a uri to RemoteSystemApp
- (IBAction)launchUriButton:(id)sender
{
    NSString* uri = self.uriField.text;
    MCDRemoteLauncher* remoteLauncher = [[MCDRemoteLauncher alloc] init];
    MCDRemoteSystemConnectionRequest* connectionRequest =
        [MCDRemoteSystemConnectionRequest requestWithRemoteSystemApp:self.selectedApplication];
    [remoteLauncher launchUriAsync:uri
             withConnectionRequest:connectionRequest
                        completion:^(MCDRemoteLaunchUriStatus result, NSError* _Nullable error) {
                            if (error)
                            {
                                NSLog(@"LaunchURI [%@]: ERROR: %@", uri, error);
                                return;
                            }

                            if (result == MCDRemoteLaunchUriStatusSuccess)
                            {
                                NSLog(@"LaunchURI [%@]: Success!", uri);
                            }
                            else
                            {
                                NSLog(@"LaunchURI [%@]: Failed with code %d", uri, (int)result);
                            }
                        }];
}

// Step #1:  Establish an app service connection
- (IBAction)connectAppServiceButton:(id)sender
{
    MCDAppServiceConnection* connection = nil;
    @synchronized(self)
    {
        connection = _appServiceConnection;
        if (!connection)
        {
            connection = _appServiceConnection = [MCDAppServiceConnection new];
            connection.appServiceInfo = [MCDAppServiceInfo infoWithName:APP_SERVICE_NAME packageId:PACKAGE_ID];
            _serviceClosedRegistration = [connection.serviceClosed subscribe:^(__unused MCDAppServiceConnection* connection,
                MCDAppServiceClosedEventArgs* args) { [self appServiceConnection:connection closedWithStatus:args.status]; }];
        }
    }

    @try
    {
        MCDRemoteSystemConnectionRequest* connectionRequest =
            [MCDRemoteSystemConnectionRequest requestWithRemoteSystemApp:self.selectedApplication];
        [connection openRemoteAsync:connectionRequest
                         completion:^(MCDAppServiceConnectionStatus status, NSError* error) {
                             if (error)
                             {
                                 NSLog(@"ConnectAppService: ERROR: %@", error);
                                 return;
                             }
                             if (status != MCDAppServiceConnectionStatusSuccess)
                             {
                                 NSLog(@"ConnectAppService: Failed with code %d", (int)status);
                                 return;
                             }
                             NSLog(@"Successfully connected!");
                             dispatch_async(
                                 dispatch_get_main_queue(), ^{ self.appServiceStatusLabel.text = @"App service connected! no ping sent"; });
                         }];
    }
    @catch (NSException* ex)
    {
        NSLog(@"ConnectAppService: EXCEPTION! %@", ex);
    }
}

// Step #3:  Send a message using the app service connection
- (IBAction)sendAppServiceButton:(id)sender
{
    if (!_appServiceConnection)
    {
        return;
    }

    // Step #4:  Send the message and get a response
    @try
    {
        [_appServiceConnection sendMessageAsync:[self _createPingMessage]
                                     completion:^(MCDAppServiceResponse* response, NSError* error) {
                                         if (error)
                                         {
                                             NSLog(@"SendPing: ERROR: %@", error);
                                             return;
                                         }

                                         if (response.status != MCDAppServiceResponseStatusSuccess)
                                         {
                                             NSLog(@"SendPing: Response received with bad status code %d", (int)response.status);
                                             return;
                                         }

                                         NSString* creationDateString = response.message[@"CreationDate"];
                                         if (creationDateString)
                                         {
                                             NSDate* date = [self->_dateFormatter dateFromString:creationDateString];
                                             if (date)
                                             {
                                                 NSTimeInterval diff = [[NSDate date] timeIntervalSinceDate:date];
                                                 dispatch_async(dispatch_get_main_queue(),
                                                     ^{ self.appServiceStatusLabel.text = [NSString stringWithFormat:@"%g", diff]; });
                                             }
                                         }
                                     }];
    }
    @catch (NSException* ex)
    {
        NSLog(@"SendPing: EXCEPTION! %@", ex);
    }
}

// Step #2:  Create a message to send
- (NSDictionary*)_createPingMessage
{
    return @{
        @"Type" : @"ping",
        @"CreationDate" : [_dateFormatter stringFromDate:[NSDate date]],
        @"TargetId" : _selectedApplication.identifier
    };
}

- (void)appServiceConnection:(__unused MCDAppServiceConnection*)connection closedWithStatus:(MCDAppServiceClosedStatus)status
{
    NSLog(@"AppService closed with status %d", (int)status);
    dispatch_async(
        dispatch_get_main_queue(), ^{ self.appServiceStatusLabel.text = [NSString stringWithFormat:@"disconnected (%d)", (int)status]; });
}

@end
