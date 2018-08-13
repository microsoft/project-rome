//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "AADAccountProvider.h"

#import <ADAL/ADAL.h>

static NSString* const AADAccountProviderExceptionName = @"AADAccountProviderException";

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
@interface AADAccountProvider ()
{
    ADAuthenticationContext* _authContext;
    ADTokenCacheItem* _tokenCacheItem;
}
@end

@implementation AADAccountProvider
@synthesize userAccountChanged = _userAccountChanged;

- (instancetype)initWithClientId:(NSString*)clientId redirectUri:(NSURL*)redirectUri
{
    if (self = [super init])
    {
        _clientId = [clientId copy];
        _redirectUri = [redirectUri copy];
        _userAccountChanged = [MCDUserAccountChangedEvent new];

#if TARGET_OS_IPHONE
        // Don't share token cache between apps, only need them to be cached for this application
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

- (void)_raiseAccountChangedEvent
{
    NSLog(@"Raise Account changed event");
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        // fire event on a different thread
        [self.userAccountChanged raise];
    });
}

- (BOOL)signedIn
{
    @synchronized(self)
    {
        return _tokenCacheItem != nil;
    }
}

- (void)signInWithCompletionCallback:(SampleAccountProviderCompletionBlock)callback
{
    if (self.signedIn)
    {
        callback(NO, SampleAccountActionFailureReasonAlreadySignedIn);
        return;
    }

    // If the user has not previously consented for this default resource for this app,
    // the interactive flow will ask for user consent for all resources used by the app.
    // If the user previously consented to this resource on this app, and more resources are added to the app later on,
    // a consent prompt for all app resources will be raised when an access token for a new resource is requested -
    // see getAccessTokenForUserAccountIdAsync:
    NSString* defaultResource = @"https://graph.windows.net";

    [_authContext acquireTokenWithResource:defaultResource
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
                                   }
                                   [self _raiseAccountChangedEvent];
                                   callback(YES, SampleAccountActionNoFailure);
                                   break;
                               }
                               case AD_USER_CANCELLED:
                               {
                                   callback(NO, SampleAccountActionFailureReasonUserCancelled);
                                   break;
                               }
                               case AD_FAILED:
                               default:
                               {
                                   NSLog(@"Error occurred in ADAL when signing in to an AAD account. Status: %u, Error: %@", result.status,
                                       result.error);
                                   callback(NO, SampleAccountActionFailureReasonADAL);
                                   break;
                               }
                               }
                           }];
}

- (void)signOutWithCompletionCallback:(SampleAccountProviderCompletionBlock)callback
{
    @synchronized(self)
    {
        if (!self.signedIn)
        {
            callback(NO, SampleAccountActionFailureReasonAlreadySignedOut);
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
            NSLog(@"Failed to remove token from ADAL cache, error %@", error);
            callback(NO, SampleAccountActionFailureReasonADAL);
            return;
        }

        // Delete cookies
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

    [self _raiseAccountChangedEvent];
    callback(YES, SampleAccountActionNoFailure);
}

- (void)getAccessTokenForUserAccountIdAsync:(NSString*)accountId
                                     scopes:(NSArray<NSString*>*)scopes
                                 completion:(void (^)(MCDAccessTokenResult*, NSError*))completionBlock
{
    @synchronized(self)
    {
        if (!self.signedIn || ![accountId isEqualToString:_tokenCacheItem.userInformation.uniqueId])
        {
            completionBlock(nil, [NSError errorWithDomain:@"AADAccountProvider"
                                                     code:0
                                                 userInfo:@{
                                                     @"Reason" : @"AADAccountProvider does not provide this account."
                                                 }]);
            return;
        }

        // Try to fetch the token silently in the background, escalating to the ui thread if needed for a unique case (see below)
        __weak __block void (^weakAdalCallback)(ADAuthenticationResult*); // __weak __block is needed for recursive blocks under ARC
        __block void (^adalCallback)(ADAuthenticationResult*) = ^void(ADAuthenticationResult* adalResult) {
            MCDAccessTokenResult* result;
            NSError* error;

            switch (adalResult.status)
            {
            case AD_SUCCEEDED:
            {
                result =
                    [[MCDAccessTokenResult alloc] initWithAccessToken:adalResult.accessToken status:MCDAccessTokenRequestStatusSuccess];
                break;
            }
            case AD_USER_CANCELLED:
            {
                error = [NSError errorWithDomain:@"AADAccountProvider" code:0 userInfo:@{ @"Reason" : @"Cancelled by user." }];
                break;
            }
            case AD_FAILED:
            default:
            {
                if (adalResult.error.code == AD_ERROR_SERVER_USER_INPUT_NEEDED)
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
                        [_authContext acquireTokenWithResource:scopes[0]
                                                      clientId:_clientId
                                                   redirectUri:_redirectUri
                                               completionBlock:weakAdalCallback];
                    });
                    return;
                }

                error = [NSError errorWithDomain:@"AADAccountProvider" code:0 userInfo:@{ @"Reason" : @"Unknown ADAL error." }];
                break;
            }
            }

            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{ completionBlock(result, error); });
        };

        weakAdalCallback = adalCallback;
        [_authContext acquireTokenSilentWithResource:scopes[0]
                                            clientId:_clientId
                                         redirectUri:_redirectUri
                                              userId:_tokenCacheItem.userInformation.userId
                                     completionBlock:adalCallback];
    }
}

- (NSArray<MCDUserAccount*>*)getUserAccounts
{
    @synchronized(self)
    {
        return _tokenCacheItem ?
                   @[ [[MCDUserAccount alloc] initWithAccountId:_tokenCacheItem.userInformation.uniqueId type:MCDUserAccountTypeAAD] ] :
                   nil;
    }
}

- (void)onAccessTokenError:(NSString*)accountId scopes:(NSArray<NSString*>*)scopes isPermanentError:(BOOL)isPermanentError
{
    @synchronized(self)
    {
        if ([accountId isEqualToString:_tokenCacheItem.userInformation.uniqueId])
        {
            if (isPermanentError)
            {
                _tokenCacheItem = nil;
                [self _raiseAccountChangedEvent];
            }
            else
            {
                // If not a permanent error, try just refreshing the token by calling ADAL's acquireToken: again
                [_authContext acquireTokenWithResource:scopes[0]
                                              clientId:_clientId
                                           redirectUri:_redirectUri
                                       completionBlock:^(__unused ADAuthenticationResult* result){}];
            }
        }
        else
        {
            NSLog(@"accountId was not found in AADAccountProvider.");
        }
    }
}

@end
