//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "SdkViewController.h"
#import "ConnectedDevicesPlatformManager.h"
#import "AppServiceProvider.h"
#import "IdentityViewController.h"
#import "LaunchUriProvider.h"
#import <ConnectedDevices/MCDConnectedDevicesPlatform.h>
#import <ConnectedDevicesRemoteSystemsCommanding/MCDRemoteSystemAppRegistration.h>
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@interface SdkViewController() {
    ConnectedDevicesPlatformManager* _platformManager;
}
@end

@implementation SdkViewController : UIViewController

- (instancetype)initWithNibName:(NSString *)nibNameOrNil
                         bundle:(NSBundle *)nibBundleOrNil {
    if (self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil]) {
        _platformManager = [ConnectedDevicesPlatformManager sharedInstance];
    }
    return self;
}

- (instancetype)initWithCoder:(NSCoder *)coder {
    if (self = [super initWithCoder:coder]) {
        _platformManager = [ConnectedDevicesPlatformManager sharedInstance];
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];

    UIBarButtonItem* signOutButton =
        [[UIBarButtonItem alloc] initWithTitle:@"Sign Out" style:UIBarButtonItemStyleDone target:self action:@selector(_signOutClicked:)];
    self.navigationItem.rightBarButtonItem = signOutButton;
}

- (void)_signOutClicked:(id)sender
{
    NSMutableArray<AnyPromise*>* logoutPromises = [NSMutableArray new];
    for (Account*  account in _platformManager.accounts) {
        [logoutPromises addObject:[_platformManager signOutAsync:account]];
    }
    
    PMKWhen(logoutPromises).then(^{
        NSLog(@"Currently signed out");
        [self dismissViewControllerAnimated:YES completion:nil];
    }).catch(^(NSError* e){
        NSLog(@"Sign out failed with error: %@", e);
    });
}

@end
