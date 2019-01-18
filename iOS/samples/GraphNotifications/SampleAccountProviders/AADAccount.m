//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "AADAccount.h"

#import <ADAL/ADAL.h>

static NSString* const AADAccountProviderErrorDomain = @"AADAccount";

/**
 * Notes about AAD/ADAL:
 *  - Resource          An Azure web service/app, such as https://graph.windows.net, or a CDP service.
 *  - Scope             Individual permissions within a resource
 *  - Access Token      A standard JSON web token for a given scope.
 *                      This is the actual token/user ticket used to authenticate with CDP services.
 *                      https://oauth.net/2/
 *                      https://www.oauth.com/oauth2-servers/access-tokens/
 *  - Refresh token:    A standard OAuth refresh token.
 *                      Lasts longer than access tokens, and is used to request new access tokens/refresh access tokens when they expire.
 *                      ADAL manages this automatically.
 *                      https://oauth.net/2/grant-types/refresh-token/
 *  - MRRT              Multiresource refresh token. A refresh token that can be used to fetch access tokens for more than one resource.
 *                      Getting one requires the user consent to all the covered resources. ADAL manages this automatically.
 */
@interface AADAccount ()
{
    ADAuthenticationContext* _authContext;
    ADTokenCacheItem* _tokenCacheItem;
}
@end

@implementation AADAccount

@synthesize isSignedIn;
@synthesize mcdAccount;

- (nullable instancetype)initWithClientId:(nonnull NSString*)clientId redirectUri:(nonnull NSURL*)redirectUri
{
    if (self = [super init])
    {
        _clientId = [clientId copy];
        _redirectUri = [redirectUri copy];

#if TARGET_OS_IPHONE
        // Don't share token cache between applications, only need them to be cached for this application
        // Without this, the MRRT is not cached, and the acquireTokenSilentWithResource: in getAccessToken
        // always fails with AD_ERROR_SERVER_USER_INPUT_NEEDED
        [[ADAuthenticationSettings sharedInstance] setDefaultKeychainGroup:nil];
#endif

        ADAuthenticationError* error = nil;
        _authContext =
            [ADAuthenticationContext authenticationContextWithAuthority:@"https://login.microsoftonline.com/common" error:&error];
        if (error)
        {
            NSLog(@"Error creating ADAuthenticationContext for AADAccountProvider: %@.", error);
            return nil;
        }

        NSLog(@"Checking if previous AADAccountProvider session can be loaded...");
#if TARGET_OS_IPHONE
        NSArray<ADTokenCacheItem*>* tokenCacheItems = [[ADKeychainTokenCache defaultKeychainCache] allItems:nil];
#else
        NSArray<ADTokenCacheItem*>* tokenCacheItems = [[ADTokenCache defaultCache] allItems:nil];
#endif
        if (tokenCacheItems.count > 0)
        {
            for (ADTokenCacheItem* item in tokenCacheItems)
            {
                if (item.isMultiResourceRefreshToken && [_clientId isEqualToString:item.clientId])
                {
                    _tokenCacheItem = item;
                    break;
                }
            }

            if (_tokenCacheItem)
            {
                NSLog(@"Loaded previous AADAccountProvider session, starting as signed in.");
            }
            else
            {
                NSLog(@"No previous AADAccountProvider session could be loaded, starting as signed out.");
            }
        }
    }

    return self;
}

- (BOOL)isSignedIn
{
    @synchronized(self)
    {
        isSignedIn = _tokenCacheItem != nil;
        return isSignedIn;
    }
}

- (void)signInWithCompletionCallback:(void (^)(MCDConnectedDevicesAccount*, NSError*))callback
{
    if (self.isSignedIn)
    {
        callback(
            NO, [NSError errorWithDomain:AADAccountProviderErrorDomain code:SampleAccountActionFailureReasonAlreadySignedIn userInfo:nil]);
        return;
    }

    // If the user has not previously consented for this default resource for this app,
    // the interactive flow will ask for user consent for all resources used by the app.
    // If the user previously consented to this resource on this app, and more resources are added to the app later on,
    // a consent prompt for all app resources will be raised when an access token for a new resource is requested -
    // see getAccessTokenForUserAccountIdAsync:
    NSString* defaultResource = @"https://graph.windows.net";

    [_authContext
        acquireTokenWithResource:defaultResource
                        clientId:_clientId
                     redirectUri:_redirectUri
                 completionBlock:^(ADAuthenticationResult* result) {
                     switch (result.status)
                     {
                     case AD_SUCCEEDED:
                     {
                         @synchronized(self)
                         {
                             _tokenCacheItem = result.tokenCacheItem;
                             mcdAccount = [[MCDConnectedDevicesAccount alloc] initWithAccountId:_tokenCacheItem.userInformation.uniqueId
                                                                                           type:MCDConnectedDevicesAccountTypeAAD];
                             callback(mcdAccount, nil);
                         }
                         break;
                     }
                     case AD_USER_CANCELLED:
                     {
                         callback(nil, [NSError errorWithDomain:AADAccountProviderErrorDomain
                                                           code:SampleAccountActionFailureReasonUserCancelled
                                                       userInfo:nil]);
                         break;
                     }
                     case AD_FAILED:
                     default:
                     {
                         NSString* errorString =
                             [NSString stringWithFormat:@"Error occurred in ADAL when signing in to an AAD account. Status: %u, Error: %@",
                                       result.status, result.error];
                         NSLog(@"%@", errorString);
                         callback(nil, [NSError errorWithDomain:AADAccountProviderErrorDomain
                                                           code:SampleAccountActionFailureReasonADAL
                                                       userInfo:@{ NSLocalizedDescriptionKey : errorString }]);
                         break;
                     }
                     }
                 }];
}

- (void)signOutWithCompletionCallback:(void (^)(MCDConnectedDevicesAccount*, NSError*))callback
{
    @synchronized(self)
    {
        if (!self.isSignedIn)
        {
            callback(nil,
                [NSError errorWithDomain:AADAccountProviderErrorDomain code:SampleAccountActionFailureReasonAlreadySignedOut userInfo:nil]);
            return;
        }

        ADAuthenticationError* error;
#if TARGET_OS_IPHONE
        BOOL removed = [[ADKeychainTokenCache defaultKeychainCache] removeAllForClientId:_clientId error:&error];
#else
        // The above convenience method does not exist on OSX
        BOOL removed;
        NSArray<ADTokenCacheItem*>* tokenCacheItems = [[ADTokenCache defaultCache] allItems:&error];
        if (!error)
        {
            for (ADTokenCacheItem* item in tokenCacheItems)
            {
                if ([item.clientId isEqualToString:_clientId])
                {
                    removed = [[ADTokenCache defaultCache] removeItem:item error:&error];

                    if (!removed || error)
                    {
                        break;
                    }
                }
            }
        }
#endif

        if (!removed || error)
        {
            NSString* errorString = [NSString stringWithFormat:@"Failed to remove token from ADAL cache, error %@", error];
            NSLog(@"%@", errorString);
            callback(nil, [NSError errorWithDomain:AADAccountProviderErrorDomain
                                              code:SampleAccountActionFailureReasonADAL
                                          userInfo:@{ NSLocalizedDescriptionKey : errorString }]);
            return;
        }

        NSArray<NSString*>* cookieNamesToDelete =
            @[ @"SignInStateCookie", @"ESTSAUTHPERSISTENT", @"ESTSAUTHLIGHT", @"ESTSAUTH", @"ESTSSC" ];

        NSHTTPCookieStorage* cookieJar = [NSHTTPCookieStorage sharedHTTPCookieStorage];
        for (NSHTTPCookie* cookie in [cookieJar cookies])
        {
            if ([cookieNamesToDelete containsObject:cookie.name])
            {
                [cookieJar deleteCookie:cookie];
            }
        }

        _tokenCacheItem = nil;
    }

    callback(mcdAccount, nil);
    mcdAccount = nil;
}

- (NSError*)_errorFromAdalStatus:(ADAuthenticationResult*)result
{
    switch (result.status)
    {
    case AD_SUCCEEDED: { return nil;
    }
    case AD_USER_CANCELLED: { return [NSError errorWithDomain:@"AADAccountProvider" code:0 userInfo:@{ @"Reason" : @"Cancelled by user." }];
    }
    case AD_FAILED:
    default: { return [NSError errorWithDomain:@"AADAccountProvider" code:0 userInfo:@{ @"Reason" : @"Unknown ADAL error." }];
    }
    }
}

- (void)getAccessTokenForUserAccountIdAsync:(NSString*)accountId
                                     scopes:(NSArray<NSString*>*)scopes
                                 completion:(void (^)(NSString*, NSError*))completionBlock
{
    @synchronized(self)
    {
        if (!self.isSignedIn || ![accountId isEqualToString:_tokenCacheItem.userInformation.uniqueId])
        {
            completionBlock(nil, [NSError errorWithDomain:@"AADAccountProvider"
                                                     code:0
                                                 userInfo:@{
                                                     @"Reason" : @"AADAccountProvider does not provide this account."
                                                 }]);
            return;
        }

        // Try to fetch the token silently in the background, escalating to the ui thread if needed for a unique case (see below)
        void (^adalCallback)(ADAuthenticationResult*) = ^void(ADAuthenticationResult* adalResult) {

            if ((adalResult.status == AD_FAILED) && (adalResult.error.code == AD_ERROR_SERVER_USER_INPUT_NEEDED))
            {
                // This error only returns from acquireTokenSilentWithResource: when an interactive prompt is needed.
                // ADAL has an MRRT, but the user has not consented for this resource/the MRRT does not cover this resource.
                // Usually, users consent for all resources the app needs during the interactive flow in signInWith...:
                // However, if the app adds new resources after the user consented previously, signIn will not prompt.
                // Escalate to the UI thread and do an interactive flow,
                // which should raise a new consent prompt for all current app resources.
                NSLog(@"A resource was requested that the user did not previously consent to. "
                      @"Attempting to raise an interactive consent prompt.");

                dispatch_async(dispatch_get_main_queue(), ^{
                    [_authContext
                        acquireTokenWithResource:scopes[0]
                                        clientId:_clientId
                                     redirectUri:_redirectUri
                                 completionBlock:^void(ADAuthenticationResult* adalResult) {
                                     // Check if still signed in at this point
                                     if (!self.isSignedIn || ![accountId isEqualToString:_tokenCacheItem.userInformation.uniqueId])
                                     {
                                         completionBlock(nil, [NSError errorWithDomain:@"AADAccountProvider"
                                                                                  code:0
                                                                              userInfo:@{
                                                                                  @"Reason" : @"Tried to escalate to interactive prompt, "
                                                                                              @"but user was signed out in the middle."
                                                                              }]);
                                     }
                                     else
                                     {
                                         NSString* accessToken = adalResult.accessToken;
                                         dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0),
                                             ^{ completionBlock(accessToken, nil); });
                                     }
                                 }];
                });
                return;
            }

            NSError* error = [self _errorFromAdalStatus:adalResult];
            dispatch_async(
                dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{ completionBlock(adalResult.accessToken, error); });
        };

        [_authContext acquireTokenSilentWithResource:scopes[0]
                                            clientId:_clientId
                                         redirectUri:_redirectUri
                                              userId:_tokenCacheItem.userInformation.userId
                                     completionBlock:adalCallback];
    }
}

@end
