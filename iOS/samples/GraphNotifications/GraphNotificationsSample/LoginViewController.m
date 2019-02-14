//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "LoginViewController.h"
#import "NotificationsManager.h"
#import "Secrets.h"
#import "AppDelegate.h"


@interface LoginViewController ()
@property (nonatomic) AADAccount* aadAccount;
@property (nonatomic) MSAAccount* msaAccount;
@end

@implementation LoginViewController

typedef NS_ENUM(NSInteger, LoginState)
{
    AAD,
    MSA,
    SIGNED_OUT
};

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view, typically from a nib.

    self.aadAccount = [[AADAccount alloc] initWithClientId:AAD_CLIENT_ID redirectUri:[NSURL URLWithString:AAD_REDIRECT_URI]];
    self.msaAccount = [[MSAAccount alloc] initWithClientId:MSA_CLIENT_ID scopeOverrides:@{@"https://activity.windows.com/UserActivity.ReadWrite.CreatedByApp" : @[@"https://activity.windows.com/UserActivity.ReadWrite.CreatedByApp", @"https://activity.windows.com/Notifications.ReadWrite.CreatedByApp"]}];

    AppDelegate* appDelegate = (AppDelegate*)([UIApplication sharedApplication].delegate);

    __weak typeof(self) weakSelf = self;
    [appDelegate.platform.accountManager.accessTokenRequested subscribe:^(MCDConnectedDevicesAccountManager * _Nonnull manager __unused, MCDConnectedDevicesAccessTokenRequestedEventArgs * _Nonnull args)
    {
        switch ([weakSelf getState])
        {
            case AAD:
            {
                [weakSelf.aadAccount getAccessTokenForUserAccountIdAsync:args.request.account.accountId scopes:args.request.scopes completion:^(NSString * _Nonnull token, NSError * _Nullable error)
                {
                    if (error)
                    {
                        [args.request completeWithErrorMessage:error.description];
                    }
                    else
                    {
                        [args.request completeWithAccessToken:token];
                    }
                }];
                break;
            }
            case MSA:
            {
                [weakSelf.msaAccount getAccessTokenForUserAccountIdAsync:args.request.account.accountId scopes:args.request.scopes completion:^(NSString * _Nonnull token, NSError * _Nullable error)
                {
                    if (error)
                    {
                        [args.request completeWithErrorMessage:error.description];
                    }
                    else
                    {
                        [args.request completeWithAccessToken:token];
                    }
                }];
                break;
            }
            case SIGNED_OUT:
            {
                [args.request completeWithErrorMessage:@"Not currently signed in!"];
                break;
            }
        }
    }];

    [appDelegate.platform.accountManager.accessTokenInvalidated subscribe:^(MCDConnectedDevicesAccountManager * _Nonnull manager __unused, MCDConnectedDevicesAccessTokenInvalidatedEventArgs * _Nonnull args)
    {
        NSLog(@"Access token invalidated for %@ account for %@ scopes", args.account.accountId, args.scopes);
    }];

    [self setButtonTextForState];

    switch([self getState])
    {
        case AAD:
            [NotificationsManager sharedInstance].account = self.aadAccount.mcdAccount;
            break;
        case MSA:
            [NotificationsManager sharedInstance].account = self.msaAccount.mcdAccount;
            break;
        case SIGNED_OUT:
            // No need to do anything
            break;
    }
}

- (IBAction)loginAAD
{
    __weak typeof(self) weakSelf = self;
    switch ([self getState])
    {
        case AAD:
            {
            [self.aadAccount signOutWithCompletionCallback:^(MCDConnectedDevicesAccount* account, NSError* error)
            {
                if (!error)
                {
                    dispatch_async(dispatch_get_main_queue(),
                    ^{
                        [weakSelf setButtonTextForState];
                        [weakSelf.view setNeedsDisplay];
                    });

                    [NotificationsManager sharedInstance].account = nil;
                }
                else
                {
                    NSLog(@"Failed to sign out AAD with reason %@", [error description]);
                }
            }];
            break;
        }
        case SIGNED_OUT:
            {
            [self.aadAccount signInWithCompletionCallback:^(MCDConnectedDevicesAccount* account, NSError* error)
            {
                if (!error)
                {
                    NSLog(@"Signed in to AAD with no error!");
                    dispatch_async(dispatch_get_main_queue(),
                    ^{
                        [weakSelf setButtonTextForState];
                        [weakSelf.view setNeedsDisplay];
                    });

                    [NotificationsManager sharedInstance].account = account;
                }
                else
                {
                    NSLog(@"Failed to sign in AAD with reason %@", [error description]);
                }
            }];
            break;
        }
        default:
            // Do nothing
            break;
    }
}

- (IBAction)loginMSA
{
    __weak typeof(self) weakSelf = self;
    switch ([self getState])
    {
        case MSA:
            {
            [self.msaAccount signOutWithCompletionCallback:^(MCDConnectedDevicesAccount* account, NSError* error)
            {
                if (!error)
                {
                    dispatch_async(dispatch_get_main_queue(),
                    ^{
                        [weakSelf setButtonTextForState];
                        [weakSelf.view setNeedsDisplay];
                    });

                    [NotificationsManager sharedInstance].account = nil;
                }
                else
                {
                    NSLog(@"Failed to sign out MSA with reason %@", [error description]);
                }
            }];
            break;
        }
        case SIGNED_OUT:
            {
            NSLog(@"Going to sign in with MSA");
            [self.msaAccount signInWithCompletionCallback:^(MCDConnectedDevicesAccount* account, NSError* error)
            {
                if (!error)
                {
                    NSLog(@"Signed in to MSA with no error!");

                    dispatch_async(dispatch_get_main_queue(),
                    ^{
                        [weakSelf setButtonTextForState];
                        [weakSelf.view setNeedsDisplay];
                    });

                    [NotificationsManager sharedInstance].account = account;
                }
                else
                {
                    NSLog(@"Failed to sign in MSA with reason %@", [error description]);
                }
            }];
            break;
        }
        default:
            // Do nothing
            break;
    }
}

- (void)setButtonTextForState
{
    dispatch_async(dispatch_get_main_queue(),
    ^{
        switch ([self getState])
        {
            case SIGNED_OUT:
                [self.aadButton setTitle:@"Login Work/School Account" forState:UIControlStateNormal];
                [self.msaButton setTitle:@"Login Personal Account" forState:UIControlStateNormal];
                break;
            case AAD:
                [self.aadButton setTitle:@"Logout" forState:UIControlStateNormal];
                [self.msaButton setTitle:@"" forState:UIControlStateNormal];
                break;
            case MSA:
                [self.aadButton setTitle:@"" forState:UIControlStateNormal];
                [self.msaButton setTitle:@"Logout" forState:UIControlStateNormal];
                break;
        }
    });
}

- (LoginState)getState
{
    @synchronized (self)
    {
        if (self.aadAccount.isSignedIn)
        {
            return AAD;
        }
        else if (self.msaAccount.isSignedIn)
        {
            return MSA;
        }
        else
        {
            return SIGNED_OUT;
        }
    }
}

@end
