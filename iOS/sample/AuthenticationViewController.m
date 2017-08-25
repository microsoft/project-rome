//
//  Copyright (C) Microsoft Corporation. All rights reserved.
//

#import "AuthenticationViewController.h"
#import "ConnectedDevices/ConnectedDevices.h"
#import "OAuthMsaAuthenticator.h"

static NSString* const MsaRedirectUri = @"https://login.live.com/oauth20_desktop.srf";
static NSString* const MsaAuthorizeUri = @"https://login.live.com/oauth20_authorize.srf";
static NSString* const MsaOfflineAccessScope = @"offline_access";
static NSString* const MsaCcsReadWriteScope = @"ccs.ReadWrite";
static NSString* const MsaDdsReadScope = @"dds.read";
static NSString* const MsaDdsRegisterScope = @"dds.register";

@interface AuthenticationViewController (Private)
- (void)showAlertMessage:(NSString*)title message:(NSString*)message;
@end

@implementation AuthenticationViewController
{
    __weak IBOutlet UILabel* _statusLabel;
    __weak IBOutlet UIWebView* _webview;
    __weak IBOutlet UIActivityIndicatorView* _spinner;
    
    OAuthMSAAuthenticator* _oauthAuthenticator;
    
    MCDRefreshTokenCallback _getTokenCallback;
    
    BOOL _visible;
}

- (void)viewWillAppear:(BOOL)animated
{
    [self.navigationController setNavigationBarHidden:YES animated:animated];
    [super viewWillAppear:animated];
    
    self.title = @"Sign In";
    self.navigationItem.hidesBackButton = YES;
}

- (void)viewDidAppear:(BOOL)animated
{
    [super viewDidAppear:animated];
    _visible = YES;
}

- (void)viewWillDisappear:(BOOL)animated
{
    _visible = NO;
    [self.navigationController setNavigationBarHidden:NO animated:animated];
    [super viewWillDisappear:animated];
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    CGRect webViewBounds = _webview.frame;
    webViewBounds.origin.x = 0;
    webViewBounds.origin.y = 0;
    webViewBounds.size = self.view.frame.size;
    
    _webview.frame = webViewBounds;
    _webview.scalesPageToFit = YES;
    _webview.scrollView.bounces = NO;
    _webview.autoresizingMask = UIViewAutoresizingFlexibleHeight | UIViewAutoresizingFlexibleWidth |
    UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleBottomMargin |
    UIViewAutoresizingFlexibleLeftMargin | UIViewAutoresizingFlexibleRightMargin;
    
    _oauthAuthenticator = [[OAuthMSAAuthenticator alloc] initWithWebView:_webview withClientId:self.clientId];
    _oauthAuthenticator.delegate = self;
    
    _spinner = [[UIActivityIndicatorView alloc] initWithFrame:CGRectMake(135, 140, 50, 50)];
    _spinner.color = [UIColor blueColor];
    
    dispatch_async(dispatch_get_main_queue(), ^{
        [_spinner startAnimating];
        [self.view addSubview:_spinner];
    });
    
    [self hideWebView];
    
    if (_shouldSignOut)
    {
        [self setMessage:@"Signing Out..."];
        
        [MCDPlatform shutdown];
        
        [_oauthAuthenticator logout];
    }
    else
    {
        // Immediately on app start, try and automatically log the user in.
        // This will only work if a) the user has previously logged in once with the
        // app
        // and b) that has been within the last ~14 days.
        // If either of this is not true, the platform will getAccessCode on us.
        [self tryLogin];
    }
    
    _shouldSignOut = NO;
}

- (void)alertView:(__unused UIAlertView*)alertView clickedButtonAtIndex:(__unused NSInteger)buttonIndex
{
    [self tryLogin];
}

- (NSString*)clientId
{
    // Change to your own app's MSA app ID
    return @"8a284efa-414b-445e-8710-0fabde940942";
}

- (void)tryLogin
{
    [self setMessage:@"Signing you in..."];
    
    // Rome is initialized asynchronously.
    // We pass in ourselves as we implement the Refresh Token Provider delegate.
    [MCDPlatform startWithRefreshTokenProviderDelegate:self
                                            completion:^(NSError* clientError) {
                                                dispatch_async(dispatch_get_main_queue(), ^{
                                                    if (clientError)
                                                    {
                                                        [self showAlertMessage:@"Rome"
                                                                       message:@"There was a failure "
                                                         @"initializing the "
                                                         @"Rome Platform."];
                                                        return;
                                                    }
                                                    
                                                    [_spinner removeFromSuperview];
                                                    [self hideWebView];
                                                    [self performSegueWithIdentifier:@"showDiscovery" sender:self];
                                                });
                                            }];
}

- (void)showLogin:(NSString*)signInUri
{
    dispatch_async(dispatch_get_main_queue(), ^{
        if (!_visible)
        {
            [self showAlertMessage:@"MSA Login"
                           message:@"There was a problem refreshing your MSA token. "
             @"Please re-login."];
            return;
        }
        
        [self showWebView];
        
        [_spinner removeFromSuperview];
        [_oauthAuthenticator login:signInUri];
    });
}

- (void)showAlertMessage:(NSString*)title message:(NSString*)message
{
    dispatch_async(dispatch_get_main_queue(), ^{
        UIAlertView* alert =
        [[UIAlertView alloc] initWithTitle:title message:message delegate:self cancelButtonTitle:@"OK" otherButtonTitles:nil];
        
        [alert show];
    });
}

- (void)showWebView
{
    [self.navigationController setNavigationBarHidden:NO animated:NO];
    
    _webview.hidden = false;
}

- (void)hideWebView
{
    [self.navigationController setNavigationBarHidden:YES animated:NO];
    _webview.hidden = true;
}

- (void)setMessage:(NSString*)message
{
    [_statusLabel setText:message];
}

- (void)resetUI
{
    [self setMessage:@""];
}

#pragma Code Provider Delegate

- (NSError*)getRefreshToken:(MCDRefreshTokenCallback)completion
{
    // This method is called when the Rome platform needs a new OAuth Access Code.
    // We need to show the MSA Web View flow.
    // Once the user logs in successfully,
    // oauthMSAAuthenticator:didFinishWithAuthenticationResult:hasFailed:authCode:
    // will be invoked.
    // "ccs.ReadWrite+dds.register+dds.read+wl.offline_access";
    NSString* const MsaScopes =
    [NSString stringWithFormat:@"%@+%@+%@+%@", MsaCcsReadWriteScope, MsaDdsReadScope, MsaDdsRegisterScope, MsaOfflineAccessScope];
    
    NSString* signInUri = [NSString stringWithFormat:@"%@?redirect_uri=%@&response_type=code&client_id=%@&scope=%@",
                           MsaAuthorizeUri,
                           MsaRedirectUri,
                           self.clientId,
                           MsaScopes];
    
    NSLog(@"Showing signInUri %@", signInUri);
    [self showLogin:signInUri];
    
    // Stash away the callback so it can be called when we are done.
    _getTokenCallback = completion;
    
    return nil;
}

- (void)oauthMSAAuthenticator:(OAuthMSAAuthenticator*)authenticator
didFinishWithAuthenticationResult:(BOOL)isAuthenticated
                    hasFailed:(BOOL)hasFailed
                 refreshToken:(NSString*)refreshToken
{
    NSError* resultError = nil;
    
    if (isAuthenticated)
    {
        if (_getTokenCallback)
        {
            _getTokenCallback(resultError, refreshToken);
            _getTokenCallback = nil;
        }
        [self hideWebView];
    }
    else
    {
        NSString* appDomain = [[NSBundle mainBundle] bundleIdentifier];
        [[NSUserDefaults standardUserDefaults] removePersistentDomainForName:appDomain];
        
        [MCDPlatform shutdown];
        
        if (hasFailed)
        {
            [self showAlertMessage:@"Microsoft Account Issue"
                           message:@"There was a failure logging you in. Please try "
             @"again."];
        }
        else
        {
            dispatch_async(dispatch_get_main_queue(), ^{
                [self tryLogin];
            });
        }
    }
}

@end
