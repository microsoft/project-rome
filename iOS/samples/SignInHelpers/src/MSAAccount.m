//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "MSAAccount.h"
#import "MSATokenCache.h"
#import "MSATokenRequest.h"

/**
 * Terms:
 *  - Scope:            OAuth feature, limits what a token actually gives permissions to.
 *                      https://www.oauth.com/oauth2-servers/scope/
 *  - Access token:     A standard JSON web token for a given scope.
 *                      This is the actual token/user ticket used to authenticate with CDP services.
 *                      https://oauth.net/2/
 *                      https://www.oauth.com/oauth2-servers/access-tokens/
 *  - Refresh token:    A standard OAuth refresh token.
 *                      Lasts longer than access tokens, and is used to request new access tokens/refresh access tokens when they expire.
 *                      This library caches one refresh token per user.
 *                      As such, the refresh token must already be authorized/consented to for all CDP scopes that will be used in the app.
 *                      https://oauth.net/2/grant-types/refresh-token/
 *  - Grant type:       Type of OAuth authorization request to make (ie: token, password, auth code)
 *                      https://oauth.net/2/grant-types/
 *  - Auth code:        OAuth auth code, can be exchanged for a token.
 *                      This library has the user sign in interactively for the auth code grant type,
 *                      then retrieves the auth code from the return URL.
 *                      https://oauth.net/2/grant-types/authorization-code/
 *  - Client ID:        ID of an app's registration in the MSA portal. As of the time of writing, the portal uses GUIDs.
 *
 * The flow of this library is described below:
 * Signing in
 *      1. signInWithCompletionCallback: is called
 *      2. UIWebView is presented to the user for sign in
 *      3. Use authcode returned from user's sign in to fetch refresh token
 *      4. Refresh token is cached - if the user does not sign out, but the app is restarted,
 *         the user will not need to enter their credentials/consent again when signInWithCompletionCallback: is called.
 *      4. Now treated as signed in. Account is exposed to CDP. userAccountChanged event is fired.
 *
 * While signed in
 *      CDP asks for access tokens
 *          1. Check if access token is in cache
 *          2. If not in cache, request a new access token using the cached refresh token.
 *          3. If in cache but close to expiry, the access token is refreshed using the refresh token.
 *             The refreshed access token is returned.
 *          4. If in cache and not close to expiry, just return it.
 *
 * Signing out
 *      1. signOutWithCompletionCallback: is called
 *      2. UIWebView is quickly popped up to go through the sign out URL
 *      3. Cache is cleared.
 *      4. Now treated as signed out. Account is no longer exposed to CDP. userAccountChanged event is fired.
 */

/*
- (nullable instancetype)initWithClientId:(nonnull NSString*)clientId
                              accountType:(MCDConnectedDevicesAccountType)type
                           scopeOverrides:(nullable NSDictionary<NSString*, NSArray<NSString*>*>*)scopes
                              redirectUri:(nonnull NSURL*)redirectUri;

- (nullable instancetype)initMSAWithClientId:(nonnull NSString*)clientId
                              scopeOverrides:(nullable NSDictionary<NSString*, NSArray<NSString*>*>*)scopes;

- (nullable instancetype)initAADWithClientId:(nonnull NSString*)clientId redirectUri:(nonnull NSURL*)redirectUri;*/

#pragma mark - Constants
// CDP's SDK currently requires authorization for all features, otherwise platform initialization will fail.
// As such, the user must sign in/consent for the following scopes. This may change to become more modular in the future.
static NSString* const MsaRequiredScopes =                               //
    @"wl.offline_access+"                                                // read and update user info at any time
    @"ccs.ReadWrite+"                                                    // device commanding scope
    @"dds.read+"                                                         // device discovery scope (discover other devices)
    @"dds.register+"                                                     // device discovery scope (allow discovering this device)
    @"wns.connect+"                                                      // push notification scope
    @"asimovrome.telemetry+"                                             // asimov token scope
    @"https://activity.windows.com/UserActivity.ReadWrite.CreatedByApp"; // default useractivities scope

// OAuth URLs
static NSString* const MsaRedirectUrl = @"https://login.live.com/oauth20_desktop.srf";
static NSString* const MsaAuthorizeUrl = @"https://login.live.com/oauth20_authorize.srf";
static NSString* const MsaLogoutUrl = @"https://login.live.com/oauth20_logout.srf";

// NSError constants
static NSString* const MsaAccountProviderErrorDomain = @"MSAAccountProvider";

#pragma mark - Static Helpers
// Helper function - gets the NSURLQueryItem matching name
static NSURLQueryItem* GetQueryItemForName(NSArray<NSURLQueryItem*>* queryItems, NSString* name)
{
    NSUInteger index = [queryItems indexOfObjectPassingTest:^BOOL(NSURLQueryItem* queryItem, __unused NSUInteger idx, __unused BOOL* stop) {
        return [queryItem.name isEqualToString:name];
    }];
    return (index != NSNotFound) ? queryItems[index] : nil;
}

#pragma mark - Private Members
@interface MSAAccount () <MSATokenCacheDelegate, UIWebViewDelegate>
{
    NSString* _clientId;
    NSDictionary<NSString*, NSArray<NSString*>*>* _scopeOverrides;
    MSATokenCache* _tokenCache;
    BOOL _signInSignOutInProgress;
    void (^_signInSignOutCallback)(MCDConnectedDevicesAccount*, NSError*);
    UIWebView* _webView;
}
@end

#pragma mark - Implementation
@implementation MSAAccount

- (instancetype)initWithClientId:(NSString*)clientId scopeOverrides:(NSDictionary<NSString*, NSArray<NSString*>*>*)scopes
{
    NSLog(@"MSAAccountProvider initWithClientId");

    if (self = [super init])
    {
        _clientId = [clientId copy];
        _scopeOverrides = [scopes copy];

        _tokenCache = [MSATokenCache cacheWithClientId:_clientId delegate:self];

        _signInSignOutInProgress = NO;

        if ([_tokenCache loadSavedRefreshToken])
        {
            NSLog(@"Loaded previous session for MSAAccountProvider. Starting as signed in.");

            _mcdAccount = [[MCDConnectedDevicesAccount alloc] initWithAccountId:[[_tokenCache getAccountId] UUIDString]
                                                                           type:MCDConnectedDevicesAccountTypeMSA];
        }
        else
        {
            NSLog(@"No previous session could be loaded for MSAAccountProvider. Starting as signed out.");
        }
    }

    return self;
}

#pragma mark - Private Helpers
- (NSString*)_getAuthScopes:(NSArray<NSString*>*)incoming
{
    NSMutableArray<NSString*>* scopes = [NSMutableArray new];
    for (NSString* scope in incoming)
    {
        NSArray<NSString*>* replacements = [_scopeOverrides objectForKey:scope];
        if (replacements)
        {
            [scopes addObjectsFromArray:replacements];
        }
        else
        {
            [scopes addObject:scope];
        }
    }
    return [scopes componentsJoinedByString:@"+"];
}

- (void)_loadWebRequest:(NSString*)requestUri
{
    @synchronized(self)
    {
        UIViewController* rootVC = [[[[UIApplication sharedApplication] delegate] window] rootViewController];

        // lazy init
        if (!_webView)
        {
            _webView = [[UIWebView alloc] initWithFrame:rootVC.view.bounds];
            _webView.delegate = self;
        }

        [rootVC.view addSubview:_webView];

        NSURLRequest* urlRequest = [NSURLRequest requestWithURL:[NSURL URLWithString:requestUri]];
        [_webView loadRequest:urlRequest];
    }
}

- (void)_signInSignOutSucceededAsync:(BOOL)__unused successful error:(NSError*)error
{
    dispatch_async(dispatch_get_main_queue(), ^{ [_webView removeFromSuperview]; });
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        _signInSignOutCallback(_mcdAccount, error);
        _signInSignOutCallback = nil;
        _signInSignOutInProgress = NO;
    });
}

#pragma mark - Interactive Sign In/Out
- (BOOL)isSignedIn
{
    @synchronized(self)
    {
        return _mcdAccount != nil;
    }
}

/**
 * Pops up a webview for the user to sign in with their MSA, then uses the auth code returned to cache a refresh token for the user.
 * If a refresh token was already cached from a previous session, it will be used instead, and no webview will be displayed.
 */
- (void)signInWithCompletionCallback:(void (^)(MCDConnectedDevicesAccount*, NSError*))signInCallback
{
    @synchronized(self)
    {
        _signInSignOutCallback = signInCallback;

        if (self.isSignedIn || _signInSignOutInProgress)
        {
            // if already signed in or in the process, callback immediately with failure and reason
            NSError* error = [NSError errorWithDomain:MsaAccountProviderErrorDomain
                                                 code:(self.isSignedIn ? SampleAccountActionFailureReasonAlreadySignedIn :
                                                                         SampleAccountActionFailureReasonSigninSignOutInProgress)
                                             userInfo:nil];
            [self _signInSignOutSucceededAsync:NO error:error];
            return;
        }

        _signInSignOutInProgress = YES;

        // issue request to sign in
        NSArray* scopes = [MsaRequiredScopes componentsSeparatedByString:@"+"];
        [self _loadWebRequest:[NSString stringWithFormat:@"%@?redirect_uri=%@&response_type=code&client_id=%@&scope=%@", MsaAuthorizeUrl,
                                        MsaRedirectUrl, _clientId, [self _getAuthScopes:scopes]]];
    }
}

/**
 * Signs the user out by going through the webview, then clears the cache and current state.
 */
- (void)signOutWithCompletionCallback:(void (^)(MCDConnectedDevicesAccount*, NSError*))signOutCallback
{
    @synchronized(self)
    {
        _signInSignOutCallback = signOutCallback;

        if (!self.isSignedIn || _signInSignOutInProgress)
        {
            // if already signed out or in the process, callback immediately with failure
            NSError* error = [NSError errorWithDomain:MsaAccountProviderErrorDomain
                                                 code:(self.isSignedIn ? SampleAccountActionFailureReasonSigninSignOutInProgress :
                                                                         SampleAccountActionFailureReasonAlreadySignedOut)
                                             userInfo:nil];
            [self _signInSignOutSucceededAsync:NO error:error];
            return;
        }

        _signInSignOutInProgress = YES;

        // issue request to sign out
        [self _loadWebRequest:[NSString stringWithFormat:@"%@?client_id=%@&redirect_uri=%@", MsaLogoutUrl, _clientId, MsaRedirectUrl]];
    }
}

- (void)removeAccount
{
    @synchronized(self)
    {
        NSLog(@"Remove the account");

        // clean account states
        if (self.isSignedIn)
        {
            NSLog(@"Remove the account - clean cached account state");
            _mcdAccount = nil;
            [_tokenCache clearTokens];
        }
    }
}

/**
 * Continuation for signIn/signOut after the webview completes.
 */
- (void)webViewDidFinishLoad:(UIWebView*)webView
{
    @synchronized(self)
    {
        // Validate the URL
        NSURLComponents* tokenURLComponents = [NSURLComponents componentsWithURL:webView.request.URL resolvingAgainstBaseURL:nil];

        if (![tokenURLComponents.path containsString:@"oauth20_desktop.srf"])
        {
            // finishing off loading intermediate pages,
            // e.g., input username/password page, consent interrupt page, wrong username/password page etc.
            // no need to handle them, return early.
            return;
        }

        NSArray<NSURLQueryItem*>* tokenURLQueryItems = tokenURLComponents.queryItems;

        NSURLQueryItem* errorResponse = GetQueryItemForName(tokenURLQueryItems, @"error");
        if (errorResponse)
        {
            // sign in or sign out ending in failure
            NSError* error = [NSError errorWithDomain:MsaAccountProviderErrorDomain
                                                 code:SampleAccountActionFailureReasonGeneric
                                             userInfo:@{ NSLocalizedDescriptionKey : errorResponse.description }];
            [self _signInSignOutSucceededAsync:NO error:error];
            return;
        }

        NSString* authCode = GetQueryItemForName(tokenURLQueryItems, @"code").value;
        if (!authCode)
        {
            // sign out ended in success
            // Delete cookies
            NSHTTPCookieStorage* cookieJar = [NSHTTPCookieStorage sharedHTTPCookieStorage];
            NSArray<NSHTTPCookie*>* cookies = [[cookieJar cookies] copy];
            for (NSHTTPCookie* cookie in cookies)
            {
                if ([cookie.domain containsString:@"live.com"])
                {
                    [cookieJar deleteCookie:cookie];
                }
            }

            [self removeAccount];
            [self _signInSignOutSucceededAsync:YES
                                         error:nil];
        }
        else
        {
            // sign in ended in success
            if (authCode.length <= 0)
            {
                // very unusual
                [self _signInSignOutSucceededAsync:NO
                                             error:[NSError errorWithDomain:MsaAccountProviderErrorDomain
                                                                       code:SampleAccountActionFailureReasonFailToRetrieveAuthCode
                                                                   userInfo:nil]];
                return;
            }

            // Fetch a refresh token using the auth code
            void (^requestRefreshTokenTokenCallback)(MSATokenRequestResult*) = ^void(MSATokenRequestResult* result) {
                if (result.status == MSATokenRequestStatusSuccess)
                {
                    NSString* newRefreshToken = result.refreshToken;
                    NSAssert(newRefreshToken != nil, @"refresh token can not be null when refreshing refresh token succeeded");

                    NSLog(@"Successfully fetch the root refresh token.");
                    NSUUID* accountId = [NSUUID UUID];
                    [_tokenCache setRefreshToken:newRefreshToken withAccountId:accountId];
                    @synchronized(self)
                    {
                        NSLog(@"Adding an account.");
                        _mcdAccount = [[MCDConnectedDevicesAccount alloc] initWithAccountId:[accountId UUIDString]
                                                                                       type:MCDConnectedDevicesAccountTypeMSA];
                    }
                    [self _signInSignOutSucceededAsync:YES error:nil];
                }
                else
                {
                    NSLog(@"Failed to fetch root refresh token using authcode.");
                    [self _signInSignOutSucceededAsync:NO
                                                 error:[NSError errorWithDomain:MsaAccountProviderErrorDomain
                                                                           code:SampleAccountActionFailureReasonFailToRetrieveRefreshToken
                                                                       userInfo:nil]];
                }
            };

            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
                NSLog(@"Fetch root refresh token using authcode.");
                [MSATokenRequest doAsyncRequestWithClientId:_clientId
                                                  grantType:MsaTokenRequestGrantTypeCode
                                                      scope:nil
                                                redirectUri:MsaRedirectUrl
                                                      token:authCode
                                                   callback:requestRefreshTokenTokenCallback];
            });
        }
    }
}

/**
 * Continuation for signIn/signOut after the webview completes with a failure.
 */
- (void)webView:(UIWebView*)__unused webView didFailLoadWithError:(NSError*)error
{
    @synchronized(self)
    {
        // This gets invoked when we interrupt/cancel because we saw the oauth complete page.
        int WebKitErrorFrameLoadInterruptedByPolicyChange = 102;
        if (error.code != WebKitErrorFrameLoadInterruptedByPolicyChange /*interrupted*/
            && error.code != NSURLErrorCancelled)
        {
            [self _signInSignOutSucceededAsync:NO
                                         error:[NSError errorWithDomain:MsaAccountProviderErrorDomain
                                                                   code:SampleAccountActionFailureReasonUserCancelled
                                                               userInfo:nil]];
        }
    }
}

- (void)getAccessTokenForUserAccountIdAsync:(NSString*)accountId
                                     scopes:(NSArray<NSString*>*)scopes
                                 completion:(void (^)(NSString*, NSError*))completionBlock
{
    if (![accountId isEqualToString:_mcdAccount.accountId])
    {
        NSLog(@"accountId did not match logged in account - is the user signed in?");
        completionBlock([NSString stringWithFormat:@"Account ID %@ does it match account ID %@", accountId, _mcdAccount.accountId],
            [NSError errorWithDomain:MsaAccountProviderErrorDomain code:SampleAccountActionFailureReasonInvalidAccountId userInfo:nil]);
        return;
    }

    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        @synchronized(self)
        {
            // check if access token cache already has a valid token
            NSString* accessTokenScope = [self _getAuthScopes:scopes];

            // clang-format off
            [_tokenCache getAccessTokenForScopeAsync:accessTokenScope callback:^void(NSString* accessToken)
            {
                if (accessToken.length > 0)
                {
                    NSLog(@"Found valid access token for scope %@ in cache, return early", accessTokenScope);
                    completionBlock(accessToken, nil);
                    return;
                }

                NSLog(@"Didn't find valid access token for scope %@ in cache, try to fetch it", accessTokenScope);
                [self _requestNewAccessTokenAsync:accessTokenScope callback:completionBlock];
            }];
            // clang-format on
        }
    });
}

- (void)onAccessTokenError:(NSString*)__unused accountId scopes:(NSArray<NSString*>*)__unused scopes isPermanentError:(BOOL)isPermanentError
{
    @synchronized(self)
    {
        if (isPermanentError)
        {
            [self removeAccount];
        }
        else
        {
            [_tokenCache markAllTokensExpired];
        }
    }
}

#pragma mark - MSATokenCache Delegate
- (void)onTokenCachePermanentFailure
{
    if (_mcdAccount)
    {
        [self onAccessTokenError:_mcdAccount.accountId scopes:[_tokenCache allScopes] isPermanentError:YES];
    }
}

/**
 * Asynchronously requests a new access token for the provided scope(s) and caches it.
 * This assumes that the sign in helper is currently signed in.
 */
- (void)_requestNewAccessTokenAsync:(NSString*)scope callback:(void (^)(NSString*, NSError*))completionBlock
{
    // Need the refresh token first, then can use it to request an access token
    [_tokenCache getRefreshTokenAsync:^void(NSString* refreshToken) {
        NSLog(@"Fetching access token for scope:%@", scope);
        [MSATokenRequest
            doAsyncRequestWithClientId:_clientId
                             grantType:MsaTokenRequestGrantTypeRefresh
                                 scope:scope
                           redirectUri:nil
                                 token:refreshToken
                              callback:^void(MSATokenRequestResult* result) {
                                  switch (result.status)
                                  {
                                  case MSATokenRequestStatusSuccess:
                                  {
                                      NSLog(@"Successfully fetched access token.");
                                      [_tokenCache setAccessToken:result.accessToken
                                                    withAccountId:[NSUUID UUID]
                                                         forScope:scope
                                                        expiresIn:result.expiresIn];

                                      completionBlock(result.accessToken, nil);
                                      break;
                                  }
                                  case MSATokenRequestStatusTransientFailure:
                                  {
                                      NSLog(@"Requesting new access token failed temporarily, please try again.");
                                      completionBlock(@"Requesting new access token failed. Try again.",
                                          [NSError errorWithDomain:MsaAccountProviderErrorDomain
                                                                     code:SampleAccountActionFailureReasonAccessTokenTemporaryError
                                                                 userInfo:nil]);
                                      break;
                                  }
                                  default: // PermanentFailure
                                  {
                                      NSLog(@"Permanent error occurred while fetching access token.");
                                      [self onAccessTokenError:_mcdAccount.accountId scopes:@[ scope ] isPermanentError:YES];
                                      completionBlock(@"Permanent error occurred fetching access token.",
                                          [NSError errorWithDomain:MsaAccountProviderErrorDomain
                                                                     code:SampleAccountActionFailureReasonAccessTokenPermanentError
                                                                 userInfo:nil]);
                                      break;
                                  }
                                  }
                              }];
    }];
}

@end
