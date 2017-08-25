//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "LauncherTabUIViewController.h"
#import "AppDataSource.h"
#import "ConnectedDevices/ConnectedDevices.h"

@implementation LauncherTabUIViewController
{
    MCDRemoteLauncher* _launcher;
}

- (void)viewDidLoad
{
    [super viewDidLoad];

    self.parentViewController.title = [AppDataSource instance].selectedSystem.displayName;

    [self setControlsForConnectivity:YES];

    self.tabBarController.tabBar.hidden = YES;
    self.launchUriTextBox.delegate = self;
}

- (void)setControlsForConnectivity:(BOOL)isConnected
{
    dispatch_async(dispatch_get_main_queue(), ^(void) {
        [self.launchUriButton setEnabled:isConnected];
        [self.launchUriTextBox setEnabled:isConnected];
    });
}

- (IBAction)launchUriButtonClicked:(__unused id)sender
{
    [self launchUri];
}

- (void)launchUri
{
    NSString* uri = [self.launchUriTextBox.text length] == 0 ? self.launchUriTextBox.placeholder : self.launchUriTextBox.text;

    [self launchUriWithString:uri];
}

- (BOOL)textFieldShouldReturn:(UITextField*)textField
{
    if (textField == self.launchUriTextBox)
    {
        [textField resignFirstResponder];
        [self launchUri];
        return NO;
    }
    return YES;
}

- (void)launchUriWithString:(NSString*)uri
{
    MCDRemoteSystem* system = [AppDataSource instance].selectedSystem;

    if (!system)
    {
        return;
    }

    MCDRemoteSystemConnectionRequest* request = [[MCDRemoteSystemConnectionRequest alloc] initWithRemoteSystem:system];

    [MCDRemoteLauncher launchUri:uri
                    withRequest:request
                 withCompletion:^(MCDRemoteLauncherUriStatus status) {
                     dispatch_async(dispatch_get_main_queue(), ^{

                         NSString* title = nil;
                         NSString* message = nil;

                         if (status == MCDRemoteLauncherUriStatusSuccess)
                         {
                             title = @"RemoteSystem Success";
                             message = @"The URI was successfully launched on the remote device.";
                         }
                         else if (status == MCDRemoteLauncherUriStatusRemoteSystemUnavailable)
                         {
                             title = @"RemoteSystem Unvailable";
                             message = @"The Remote System is unavailable.";
                         }
                         else if (status == MCDRemoteLauncherUriStatusDeniedByLocalSystem)
                         {
                             title = @"Denied by Local System";
                             message = @"The request was denied by the local system.";
                         }
                         else if (status == MCDRemoteLauncherUriStatusDeniedByLocalSystem)
                         {
                             title = @"Denied by Remote System";
                             message = @"The request was denied by the remote system.";
                         }
                         else
                         {
                             title = @"Unknown Error";
                             message = @"An unknown error has occurred.";
                         }

                         UIAlertView* alert = [[UIAlertView alloc] initWithTitle:title
                                                                         message:message
                                                                        delegate:nil
                                                               cancelButtonTitle:@"OK"
                                                               otherButtonTitles:nil];

                         [alert show];
                     });
                 }];
}

@end
