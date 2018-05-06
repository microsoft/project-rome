//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "AADMSAAccountProvider.h"

#import "AADAccountProvider.h"
#import "MSAAccountProvider.h"

static NSString* const AADMSAAccountProviderExceptionName = @"AADMSAAccountProviderException";

@interface AADMSAAccountProvider ()
@property(readonly, nonatomic, strong) MSAAccountProvider* msaProvider;
@property(readonly, nonatomic, strong) AADAccountProvider* aadProvider;
@end

@implementation AADMSAAccountProvider

@synthesize userAccountChanged = _userAccountChanged;

- (instancetype)initWithMsaClientId:(NSString*)msaClientId
                   aadApplicationId:(NSString*)aadApplicationId
                     aadRedirectUri:(NSURL*)aadRedirectUri
{
    if (self = [super init])
    {
        _userAccountChanged = [MCDUserAccountChangedEvent new];
        _msaProvider = [[MSAAccountProvider alloc] initWithClientId:msaClientId];
        _aadProvider = [[AADAccountProvider alloc] initWithClientId:aadApplicationId redirectUri:aadRedirectUri];

        if (_msaProvider.signedIn && _aadProvider.signedIn)
        {
            // Shouldn't ever happen, but if it does, sign out of AAD
            [_aadProvider signOutWithCompletionCallback:^(__unused BOOL success, __unused SampleAccountActionFailureReason reason){}];
        }

        [_msaProvider.userAccountChanged subscribe:^void() { [self.userAccountChanged raise]; }];
        [_aadProvider.userAccountChanged subscribe:^void() { [self.userAccountChanged raise]; }];
    }
    return self;
}

- (AADMSAAccountProviderSignInState)signInState
{
    @synchronized(self)
    {
        if (_msaProvider.signedIn)
        {
            return AADMSAAccountProviderSignInStateSignedInMSA;
        }
        else if (_aadProvider.signedIn)
        {
            return AADMSAAccountProviderSignInStateSignedInAAD;
        }
        return AADMSAAccountProviderSignInStateSignedOut;
    }
}

- (NSString*)msaClientId
{
    return _msaProvider.clientId;
}

- (NSString*)aadApplicationId
{
    return _aadProvider.clientId;
}

- (id<SingleUserAccountProvider>)_signedInProvider
{
    switch (self.signInState)
    {
    case AADMSAAccountProviderSignInStateSignedInMSA: return _msaProvider;
    case AADMSAAccountProviderSignInStateSignedInAAD: return _aadProvider;
    default: return nil;
    }
}

- (void)signInMSAWithCompletionCallback:(SampleAccountProviderCompletionBlock)callback
{
    if (self.signInState != AADMSAAccountProviderSignInStateSignedOut)
    {
        [NSException raise:AADMSAAccountProviderExceptionName format:@"Already signed into an account!"];
    }
    [_msaProvider signInWithCompletionCallback:callback];
}

- (void)signInAADWithCompletionCallback:(SampleAccountProviderCompletionBlock)callback
{
    if (self.signInState != AADMSAAccountProviderSignInStateSignedOut)
    {
        [NSException raise:AADMSAAccountProviderExceptionName format:@"Already signed into an account!"];
    }
    [_aadProvider signInWithCompletionCallback:callback];
}

- (void)signOutWithCompletionCallback:(__unused SampleAccountProviderCompletionBlock)callback
{
    id<SingleUserAccountProvider> signedInProvider = [self _signedInProvider];
    if (!signedInProvider)
    {
        [NSException raise:AADMSAAccountProviderExceptionName format:@"Not signed into an account!"];
    }
    [signedInProvider signOutWithCompletionCallback:callback];
}

- (void)getAccessTokenForUserAccountIdAsync:(NSString*)accountId
                                     scopes:(NSArray<NSString*>*)scopes
                                 completion:(void (^)(MCDAccessTokenResult*, NSError*))completionBlock
{
    id<SingleUserAccountProvider> signedInProvider = [self _signedInProvider];
    if (!signedInProvider)
    {
        [NSException raise:AADMSAAccountProviderExceptionName format:@"Not signed into an account!"];
    }
    [signedInProvider getAccessTokenForUserAccountIdAsync:accountId scopes:scopes completion:completionBlock];
}

- (NSArray<MCDUserAccount*>*)getUserAccounts
{
    return [[self _signedInProvider] getUserAccounts];
}

- (void)onAccessTokenError:(NSString*)accountId scopes:(NSArray<NSString*>*)scopes isPermanentError:(BOOL)isPermanentError
{
    id<SingleUserAccountProvider> signedInProvider = [self _signedInProvider];
    if (!signedInProvider)
    {
        [NSException raise:AADMSAAccountProviderExceptionName format:@"Not signed into an account!"];
    }
    [signedInProvider onAccessTokenError:accountId scopes:scopes isPermanentError:isPermanentError];
}

@end
