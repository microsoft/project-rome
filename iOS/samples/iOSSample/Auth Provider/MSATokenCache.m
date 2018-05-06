//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "MSATokenCache.h"
#import "MSATokenRequest.h"

static NSString* const MsaOfflineAccessScope = @"wl.offline_access";

static NSString* const JsonTokenKey = @"refresh_token";
static NSString* const JsonExpirationKey = @"expires";

// Max number of times to try to refresh a token through transient failures
static const NSUInteger MsaTokenRefreshMaxRetries = 3;

// How quickly to retry refresh token refreshes on transient failure; 30 minutes.
static const int64_t MsaRefreshTokenRetryInterval = 30 * 60;

// How quickly to retry access token refreshes on transient failure; 3 minutes.
static const int64_t MsaAccessTokenRetryInterval = 3 * 60;

// How long a refresh token is expected to last without expiring; 10 days.
static const NSTimeInterval MsaRefreshTokenExpirationInterval = 10 * 24 * 60 * 60;

// Time from expiration at which a refresh token is considered 'close to expiring'; 7 days.
// (This value is intended to be aggressive and keep the refresh token relatively far from expiry)
static const NSTimeInterval MsaRefreshTokenCloseToExpiryInterval = 7 * 24 * 60 * 60;

// Time from expiration at which an access token is considered 'close to expiring'; 5 minutes.
static const NSTimeInterval MsaAccessTokenCloseToExpiryInterval = 5 * 60;

// @brief Private helper class, encapsulates a single MSA token to be cached, and how to refresh it.
@interface MSATokenCacheItem : NSObject
+ (nullable instancetype)cacheItemWithToken:(nonnull NSString*)token
                                  expiresIn:(NSTimeInterval)expiry
                                refreshWith:(nonnull MSATokenRequest*)refreshRequest
                                     parent:(nonnull MSATokenCache*)parent;
- (nullable instancetype)initWithToken:(nonnull NSString*)token
                             expiresIn:(NSTimeInterval)expiry
                           refreshWith:(nonnull MSATokenRequest*)refreshRequest
                                parent:(nonnull MSATokenCache*)parent;

// Asynchronously fetches the token held by this item, refreshing it if necessary.
- (void)getTokenAsync:(nonnull void (^)(NSString* _Nullable token))callback;

@property(readwrite, nonnull, nonatomic, copy) NSString* token;
@property(readwrite, nonnull, nonatomic, strong) NSDate* expirationDate;
@property(readwrite, nonnull, nonatomic, strong) MSATokenRequest* refreshRequest;
@property(readwrite, nonnull, nonatomic, strong) MSATokenCache* parent;
@property(readonly, nonatomic) NSTimeInterval closeToExpiryInterval;
@property(readonly, nonatomic) int64_t retryInterval;

// Private helper for refreshing this token. Only to be used by this class and its subclass.
// Returns the refresh token needed to refresh the token held by this item.
// For access tokens, this gets the refresh token held by the cache.
// For refresh tokens, just return the currently-held token.
- (void)getRefreshTokenAsync:(nonnull void (^)(NSString* _Nullable token))callback;

// Private helper for refreshing this token. Only to be used by this class and its subclass.
// For access tokens, sets the new token and expiration.
// For refresh tokens, marks current access tokens as expired, and caches the refresh token in persistent storage.
- (void)onSuccessfulRefresh:(nonnull MSATokenRequestResult*)result;

@end

// @brief Subclass of MSATokenCacheItem for refresh tokens
@interface MSARefreshTokenCacheItem : MSATokenCacheItem
+ (nullable instancetype)loadSavedRefreshTokenWithParent:(nonnull MSATokenCache*)parent;
- (void)saveRefreshToken;
@end

// MSATokenCache privates
@interface MSATokenCache ()
@property(readonly, nonnull, nonatomic, copy) NSString* clientId;
@property(readonly, nonnull, nonatomic, strong) NSMutableDictionary<NSString*, MSATokenCacheItem*>* cachedAccessTokens; // keyed on scopes
@property(readwrite, nullable, nonatomic, strong) MSARefreshTokenCacheItem* cachedRefreshToken;
- (void)markAccessTokensExpired;
@end

@implementation MSATokenCacheItem

+ (instancetype)cacheItemWithToken:(NSString*)token
                         expiresIn:(NSTimeInterval)expiry
                       refreshWith:(MSATokenRequest*)refreshRequest
                            parent:(MSATokenCache*)parent
{
    return [[self alloc] initWithToken:token expiresIn:expiry refreshWith:refreshRequest parent:parent];
}

- (instancetype)initWithToken:(NSString*)token
                    expiresIn:(NSTimeInterval)expiry
                  refreshWith:(MSATokenRequest*)refreshRequest
                       parent:(MSATokenCache*)parent
{
    if (self = [super init])
    {
        _token = [token copy];
        _expirationDate = [NSDate dateWithTimeIntervalSinceNow:expiry];
        _refreshRequest = refreshRequest;
        _parent = parent;
    }
    return self;
}

- (void)getTokenAsync:(void (^)(NSString*))callback
{
    [self getTokenAsync:callback maxRetries:MsaTokenRefreshMaxRetries];
}

- (void)getTokenAsync:(void (^)(NSString*))callback maxRetries:(NSUInteger)maxRetries
{
    if ([_expirationDate timeIntervalSinceNow] >= self.closeToExpiryInterval)
    {
        // If expiration date is sufficiently far away
        callback(self.token);
    }
    else
    {
        // If expired or close to it, get the refresh token and attempt to refresh with it
        [self getRefreshTokenAsync:^void(NSString* refreshToken) {
            if (!refreshToken)
            {
                // Unable to get the refresh token even after retrying
                // Consider as a permanent failure and call back with no tokens
                NSLog(@"Unable to get refresh token. Cancelling refresh and removing all tokens from cache.");
                [_parent clearTokens];
                callback(nil);
            }

            NSLog(@"Refreshing token...");
            [_refreshRequest
                requestAsyncWithToken:refreshToken
                             callback:^void(MSATokenRequestResult* result) {
                                 switch (result.status)
                                 {
                                 case MSATokenRequestStatusSuccess:
                                 {
                                     [self onSuccessfulRefresh:result];
                                     callback(self.token);
                                     break;
                                 }
                                 case MSATokenRequestStatusTransientFailure:
                                 {
                                     if (maxRetries > 0)
                                     {
                                         // Retry the refresh
                                         NSLog(@"Encountered transient error when refreshing token, retrying in %lld seconds...",
                                             self.retryInterval);
                                         dispatch_time_t retryTime = dispatch_time(DISPATCH_TIME_NOW, self.retryInterval * NSEC_PER_SEC);
                                         dispatch_after(retryTime, dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0),
                                             ^{ [self getTokenAsync:callback maxRetries:(maxRetries - 1)]; });
                                     }
                                     else
                                     {
                                         // Reached max number of retries
                                         NSLog(@"Reached max number of retries for refreshing token.");
                                         callback(nil);
                                     }
                                     break;
                                 }
                                 default: // PermanentFailure
                                 {
                                     NSLog(@"Permanent error occurred while refreshing token. Clearing the cache...");
                                     [_parent clearTokens];
                                     [_parent.delegate onTokenCachePermanentFailure];
                                     callback(nil);
                                     break;
                                 }
                                 }
                             }];
        }];
    }
}

- (NSTimeInterval)closeToExpiryInterval
{
    return MsaAccessTokenCloseToExpiryInterval; // Base class expects access tokens
}

- (int64_t)retryInterval
{
    return MsaAccessTokenRetryInterval; // Base class expects access tokens
}

- (void)getRefreshTokenAsync:(void (^)(NSString*))callback
{
    [_parent getRefreshTokenAsync:callback]; // Base class expects access tokens, grab the refresh token from the parent
}

- (void)onSuccessfulRefresh:(MSATokenRequestResult*)result
{
    @synchronized(self)
    {
        NSString* newToken = result.accessToken;
        NSAssert(newToken.length > 0, @"UNEXPECTED: Refresh access token succeeded but access token was empty.");

        NSLog(@"Successfully refreshed access token.");
        self.token = newToken;
        self.expirationDate = [NSDate dateWithTimeIntervalSinceNow:result.expiresIn];
    }
}

@end

@implementation MSARefreshTokenCacheItem

+ (instancetype)loadSavedRefreshTokenWithParent:(MSATokenCache*)parent
{
    NSLog(@"Loading refresh token from keychain...");

    // clang-format off
    NSDictionary* keychainMatchQuery = @{
        (id) kSecClass          : (id) kSecClassGenericPassword,
        (id) kSecAttrGeneric    : parent.clientId,
        (id) kSecMatchLimit     : (id) kSecMatchLimitOne, // Only match one keychain item
        (id) kSecReturnData     : @YES                    // Return the data itself rather than a ref
    };
    // clang-format on

    CFTypeRef keychainItems = NULL;
    OSStatus keychainStatus = SecItemCopyMatching((CFDictionaryRef)keychainMatchQuery, &keychainItems);
    if (keychainStatus == errSecItemNotFound)
    {
        NSLog(@"No refresh token found in keychain.");
        return nil;
    }
    else if (keychainStatus != errSecSuccess)
    {
        NSLog(@"Unable to load refresh token from keychain with OSStatus %d", (int)keychainStatus);
        return nil;
    }

    NSError* jsonError = nil;
    CFDataRef tokenData = (CFDataRef)keychainItems;
    id deserializedTokenData = [NSJSONSerialization JSONObjectWithData:(__bridge NSData*)tokenData options:0 error:&jsonError];
    if (jsonError)
    {
        NSLog(@"Encountered JSON error \'%@\' while trying to load refresh token from keychain.", jsonError);
        return nil;
    }
    else if (![deserializedTokenData isKindOfClass:[NSDictionary class]])
    {
        NSLog(@"Loaded refresh token data from keychain was in an unexpected format. Will not load.");
        return nil;
    }

    NSDictionary* tokenDict = (NSDictionary*)deserializedTokenData;
    NSString* loadedRefreshToken = (NSString*)(tokenDict[JsonTokenKey]);

    NSDateFormatter* dateFormatter = [NSDateFormatter new];
    dateFormatter.dateStyle = NSDateFormatterFullStyle;
    dateFormatter.timeStyle = NSDateFormatterFullStyle;
    NSDate* loadedRefreshTokenExpiry = [dateFormatter dateFromString:(NSString*)(tokenDict[JsonExpirationKey])];
    if (!loadedRefreshToken || !loadedRefreshTokenExpiry)
    {
        NSLog(@"Loaded refresh token data from keychain was incomplete or corrupted.");
        return nil;
    }

    NSTimeInterval timeUntilExpiration = [loadedRefreshTokenExpiry timeIntervalSinceDate:[NSDate date]];
    MSATokenRequest* refreshRequest = [MSATokenRequest tokenRequestWithClientId:parent.clientId
                                                                      grantType:MsaTokenRequestGrantTypeRefresh
                                                                          scope:MsaOfflineAccessScope
                                                                    redirectUri:nil];
    MSARefreshTokenCacheItem* ret =
        [self cacheItemWithToken:loadedRefreshToken expiresIn:timeUntilExpiration refreshWith:refreshRequest parent:parent];

    NSLog(@"Successfully loaded refresh token from keychain.");
    return ret;
}

- (void)saveRefreshToken
{
    NSLog(@"Saving refresh token to keychain...");

    NSDateFormatter* dateFormatter = [NSDateFormatter new];
    dateFormatter.dateStyle = NSDateFormatterFullStyle;
    dateFormatter.timeStyle = NSDateFormatterFullStyle;
    NSDictionary* tokenDict = @{ JsonTokenKey : self.token, JsonExpirationKey : [dateFormatter stringFromDate:self.expirationDate] };

    NSError* jsonError = nil;
    NSData* tokenData = [NSJSONSerialization dataWithJSONObject:tokenDict options:0 error:&jsonError];
    if (jsonError)
    {
        NSLog(@"Encountered JSON error \'%@\' while trying to save refresh token to keychain. Will not save.", jsonError);
        return;
    }

    // clang-format off
    NSDictionary* keychainSearchQuery = @{
        (id) kSecClass          : (id) kSecClassGenericPassword,
        (id) kSecAttrGeneric    : self.parent.clientId
    };
    // clang-format on

    OSStatus keychainStatus = SecItemUpdate((CFDictionaryRef)keychainSearchQuery, (CFDictionaryRef) @{ (id) kSecValueData : tokenData });
    if (keychainStatus == errSecItemNotFound)
    {
        // After a device restart, this keychain item is only accessible after the device is unlocked at least once.
        // This keychain item is not migrated when restoring a backup from another device.
        id accessAttribute = (id)kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly;

        NSMutableDictionary* keychainAddQuery = [keychainSearchQuery mutableCopy];
        [keychainAddQuery addEntriesFromDictionary:@{ (id) kSecAttrAccessible : accessAttribute, (id) kSecValueData : tokenData }];
        keychainStatus = SecItemAdd((CFDictionaryRef)keychainAddQuery, NULL);
    }

    if (keychainStatus != errSecSuccess)
    {
        NSLog(@"Failed to save refresh token data to keychain with OSStatus %d.", (int)keychainStatus);
    }

    NSLog(@"Successfully saved refresh token data to keychain.");
}

- (NSTimeInterval)closeToExpiryInterval
{
    return MsaRefreshTokenCloseToExpiryInterval;
}

- (int64_t)retryInterval
{
    return MsaRefreshTokenRetryInterval;
}

- (void)getRefreshTokenAsync:(void (^)(NSString*))callback
{
    callback(self.token); // Since this cache item holds a refresh token, just return it
}

- (void)onSuccessfulRefresh:(MSATokenRequestResult*)result
{
    @synchronized(self)
    {
        NSString* newToken = result.refreshToken;
        NSAssert(newToken.length > 0, @"UNEXPECTED: Refresh refresh token succeeded but access token was empty.");

        NSLog(@"Successfully refreshed refresh token.");
        self.token = newToken;
        self.expirationDate = [NSDate dateWithTimeIntervalSinceNow:MsaRefreshTokenExpirationInterval];
        [self saveRefreshToken];
        [self.parent markAccessTokensExpired];
    }
}

@end

// MSATokenCache implementation
@implementation MSATokenCache

+ (instancetype)cacheWithClientId:(NSString*)clientId delegate:(id<MSATokenCacheDelegate>)delegate
{
    return [[self alloc] initWithClientId:clientId delegate:delegate];
}

- (instancetype)initWithClientId:(NSString*)clientId delegate:(id<MSATokenCacheDelegate>)delegate
{
    if (self = [super init])
    {
        _clientId = [clientId copy];
        _delegate = delegate;
        _cachedAccessTokens = [NSMutableDictionary<NSString*, MSATokenCacheItem*> new];
    }
    return self;
}

- (void)setRefreshToken:(NSString*)refreshToken
{
    MSATokenRequest* refreshRequest = [MSATokenRequest tokenRequestWithClientId:_clientId
                                                                      grantType:MsaTokenRequestGrantTypeRefresh
                                                                          scope:MsaOfflineAccessScope
                                                                    redirectUri:nil];
    @synchronized(self)
    {
        _cachedRefreshToken = [MSARefreshTokenCacheItem cacheItemWithToken:refreshToken
                                                                 expiresIn:MsaRefreshTokenExpirationInterval
                                                               refreshWith:refreshRequest
                                                                    parent:self];
        [_cachedRefreshToken saveRefreshToken];
        [self markAccessTokensExpired];
    }
}

- (void)setAccessToken:(NSString*)accessToken forScope:(NSString*)scope expiresIn:(NSTimeInterval)expiry
{
    MSATokenRequest* refreshRequest =
        [MSATokenRequest tokenRequestWithClientId:_clientId grantType:MsaTokenRequestGrantTypeRefresh scope:scope redirectUri:nil];

    @synchronized(self)
    {
        [_cachedAccessTokens
            setValue:[MSATokenCacheItem cacheItemWithToken:accessToken expiresIn:expiry refreshWith:refreshRequest parent:self]
              forKey:scope];
    }
}

- (void)getRefreshTokenAsync:(void (^)(NSString*))callback
{
    @synchronized(self)
    {
        if (_cachedRefreshToken)
        {
            [_cachedRefreshToken getTokenAsync:callback];
        }
        else
        {
            callback(nil);
        }
    }
}

- (void)getAccessTokenForScopeAsync:(NSString*)scope callback:(void (^)(NSString*))callback
{
    @synchronized(self)
    {
        MSATokenCacheItem* item = [_cachedAccessTokens valueForKey:scope];
        if (item)
        {
            [item getTokenAsync:callback];
        }
        else
        {
            callback(nil);
        }
    }
}

- (NSArray<NSString*>*)allScopes
{
    return [_cachedAccessTokens allKeys];
}

- (BOOL)loadSavedRefreshToken
{
    MSARefreshTokenCacheItem* loadedRefreshToken = [MSARefreshTokenCacheItem loadSavedRefreshTokenWithParent:self];
    if (loadedRefreshToken)
    {
        if ([loadedRefreshToken.expirationDate compare:[NSDate date]] != NSOrderedDescending)
        {
            NSLog(@"Refresh token loaded from keychain was expired. Ignoring.");
            return NO;
        }

        @synchronized(self)
        {
            _cachedRefreshToken = loadedRefreshToken;
            [self markAllTokensExpired]; // Force a refresh on everything on first use
        }
    }

    return (loadedRefreshToken != nil);
}

- (void)clearTokens
{
    NSLog(@"Clearing token data from cache...");

    @synchronized(self)
    {
        [_cachedAccessTokens removeAllObjects];
        _cachedRefreshToken = nil;
    }

    // clang-format off
    NSDictionary* keychainDeleteQuery = @{
        (id) kSecClass          : (id) kSecClassGenericPassword,
        (id) kSecAttrGeneric    : _clientId
    };
    // clang-format on

    OSStatus keychainStatus = SecItemDelete((CFDictionaryRef)keychainDeleteQuery);
    if (keychainStatus != errSecSuccess)
    {
        NSLog(@"Unable to clear token data from keychain with OSStatus %d. Data might still be loaded on next run.", (int)keychainStatus);
    }

    NSLog(@"Done clearing token data from cache.");
}

- (void)markAccessTokensExpired
{
    @synchronized(self)
    {
        for (MSATokenCacheItem* cachedAccessToken in _cachedAccessTokens.allValues)
        {
            cachedAccessToken.expirationDate = [NSDate distantPast];
        }
    }
}

- (void)markAllTokensExpired
{
    @synchronized(self)
    {
        _cachedRefreshToken.expirationDate = [NSDate distantPast];
        [self markAccessTokensExpired];
    }
}

@end
