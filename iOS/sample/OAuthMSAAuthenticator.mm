//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "OAuthMSAAuthenticator.h"

@implementation OAuthMSAAuthenticator

- (id)initWithWebView:(UIWebView*)webView withClientId:(NSString*)clientId
{
    self = [super init];
    if (self)
    {
        _clientId = clientId;
        _webView = webView;
        _webView.delegate = static_cast<id<UIWebViewDelegate>>(self);
    }

    return self;
}

- (NSError*)login:(NSString*)signInUri
{
    NSURLRequest* urlRequest = [NSURLRequest requestWithURL:[NSURL URLWithString:signInUri]];
    [_webView loadRequest:urlRequest];

    return nil;
}

- (NSError*)logout
{
    NSString* logoutUrl = @"https://login.live.com/"
                          @"oauth20_logout.srf?client_id=%@&redirect_uri=https:/"
                          @"/login.live.com/oauth20_desktop.srf";
    logoutUrl = [NSString stringWithFormat:logoutUrl, _clientId];

    NSURLRequest* urlRequest = [NSURLRequest requestWithURL:[NSURL URLWithString:logoutUrl]];
    [_webView loadRequest:urlRequest];

    return nil;
}

- (void)webViewDidFinishLoad:(__unused UIWebView*)webView
{
    NSURL* tokenURL = _webView.request.URL;
    if (![self isOAuthCompletionUri:tokenURL])
    {
        return;
    }

    if (![self containsAuthCodeUrl:tokenURL])
    {
        // Logging out
        [self fireAuthenticationComplete:NO hasFailed:NO authCode:nil];
        return;
    }

    // Get the auth code, make a request for the access token
    NSDictionary* parameters = [self getQueryParameters:[tokenURL query]];
    NSString* authCode = [parameters objectForKey:@"code"];
    if (!authCode || [authCode length] == 0)
    {
        [self fireAuthenticationComplete:NO hasFailed:YES authCode:nil];
        return;
    }

    [self fireAuthenticationComplete:YES hasFailed:NO authCode:authCode];
}

- (void)webView:(__unused UIWebView*)webView didFailLoadWithError:(NSError*)error
{
    // This gets invoked when we interrupt/cancel because we saw the oauth
    // complete page.
    int WebKitErrorFrameLoadInterruptedByPolicyChange = 102;
    if (error.code != WebKitErrorFrameLoadInterruptedByPolicyChange /*interrupted*/
        && error.code != NSURLErrorCancelled)
    {
        [self fireAuthenticationComplete:NO hasFailed:YES authCode:nil];
    }
}

- (void)fireAuthenticationComplete:(BOOL)isAuthenticated hasFailed:(BOOL)hasFailed authCode:(NSString*)authCode
{
    if (self.delegate &&
        [self.delegate respondsToSelector:@selector(oauthMSAAuthenticator:didFinishWithAuthenticationResult:hasFailed:authCode:)])
    {
        [self.delegate oauthMSAAuthenticator:self
            didFinishWithAuthenticationResult:isAuthenticated
                                    hasFailed:hasFailed
                                     authCode:authCode];
    }
}

- (BOOL)isOAuthCompletionUri:(NSURL*)url
{
    NSString* path = [url path];
    if (path != nil)
    {
        return [path rangeOfString:@"oauth20_desktop.srf"].location != NSNotFound;
    }

    return NO;
}

- (BOOL)containsAuthCodeUrl:(NSURL*)url
{
    NSString* queryString = [url query];
    if (queryString != nil)
    {
        return [queryString rangeOfString:@"code="].location != NSNotFound;
    }

    return NO;
}

- (NSDictionary*)getQueryParameters:(NSString*)query
{
    NSMutableDictionary* params = [NSMutableDictionary new];
    NSArray* fragmentParameters = [query componentsSeparatedByString:@"&"];

    for (NSString* parameter in fragmentParameters)
    {
        NSArray* elements = [parameter componentsSeparatedByString:@"="];
        if (elements.count > 1)
        {
            [params setObject:elements[1] forKey:elements[0]];
        }
    }

    return params;
}

@end
