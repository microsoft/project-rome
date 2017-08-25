//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "OAuthMSAAuthenticator.h"

// 30s
static constexpr NSTimeInterval c_timeout = 30.0;

static NSString* const MsaTokenUri = @"https://login.live.com/oauth20_token.srf";
static NSString* const MsaRedirectUri = @"https://login.live.com/oauth20_desktop.srf";

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
        [self fireAuthenticationComplete:NO hasFailed:NO refreshToken:nil];
        return;
    }
    
    // Get the auth code, make a request for the access token
    NSDictionary* parameters = [self getQueryParameters:[tokenURL query]];
    NSString* authCode = [parameters objectForKey:@"code"];
    if (!authCode || [authCode length] == 0)
    {
        [self fireAuthenticationComplete:NO hasFailed:YES refreshToken:nil];
        return;
    }
    
    NSURL* url = [NSURL URLWithString:MsaTokenUri];
    NSMutableURLRequest* request =
    [NSMutableURLRequest requestWithURL:url cachePolicy:NSURLRequestUseProtocolCachePolicy timeoutInterval:c_timeout];
    
    [request setHTTPMethod:@"POST"];
    [request addValue:@"application/x-www-form-urlencoded" forHTTPHeaderField:@"Content-Type"];
    
    NSMutableDictionary* params = [[NSMutableDictionary alloc] init];
    [params setObject:_clientId forKey:@"client_id"];
    [params setObject:@"authorization_code" forKey:@"grant_type"];
    [params setObject:MsaRedirectUri forKey:@"redirect_uri"];
    [params setObject:authCode forKey:@"code"];
    NSData* postData = [self _encodeDictionary:params];
    [request setHTTPBody:postData];
    NSURLResponse* response = nil;
    
    NSLog(@"Making MSA OAuth HTTP request!");
    
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
    NSData* dataResponse = [NSURLConnection sendSynchronousRequest:request returningResponse:&response error:nil];
#pragma clang dianostic pop
    
    if (dataResponse)
    {
        NSError* parseError = nil;
        NSDictionary* dictionary = [NSJSONSerialization JSONObjectWithData:dataResponse options:0 error:&parseError];
        NSLog(@"Received %@", dictionary);
        NSString* newRefreshToken = [dictionary valueForKey:@"refresh_token"];
        if (newRefreshToken)
        {
            [self fireAuthenticationComplete:YES hasFailed:NO refreshToken:newRefreshToken];
            return;
        }
    }
    
    [self fireAuthenticationComplete:NO hasFailed:YES refreshToken:nil];
}

- (void)webView:(__unused UIWebView*)webView didFailLoadWithError:(NSError*)error
{
    // This gets invoked when we interrupt/cancel because we saw the oauth
    // complete page.
    int WebKitErrorFrameLoadInterruptedByPolicyChange = 102;
    if (error.code != WebKitErrorFrameLoadInterruptedByPolicyChange /*interrupted*/
        && error.code != NSURLErrorCancelled)
    {
        [self fireAuthenticationComplete:NO hasFailed:YES refreshToken:nil];
    }
}

- (void)fireAuthenticationComplete:(BOOL)isAuthenticated hasFailed:(BOOL)hasFailed refreshToken:(NSString*)refreshToken
{
    NSLog(@"Authentication complete with refreshToken %@", refreshToken);
    if (self.delegate &&
        [self.delegate respondsToSelector:@selector(oauthMSAAuthenticator:didFinishWithAuthenticationResult:hasFailed:refreshToken:)])
    {
        [self.delegate oauthMSAAuthenticator:self
           didFinishWithAuthenticationResult:isAuthenticated
                                   hasFailed:hasFailed
                                refreshToken:refreshToken];
    }
}

- (BOOL)isOAuthCompletionUri:(NSURL*)url
{
    NSString* path = [url path];
    if (path)
    {
        return [path rangeOfString:@"oauth20_desktop.srf"].location != NSNotFound;
    }
    
    return NO;
}

- (BOOL)containsAuthCodeUrl:(NSURL*)url
{
    NSString* queryString = [url query];
    if (queryString)
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

- (NSData*)_encodeDictionary:(NSDictionary*)dictionary
{
    NSMutableArray* parts = [[NSMutableArray alloc] init];
    for (NSString* key in dictionary)
    {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
        NSString* encodedValue = [[dictionary objectForKey:key] stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
        NSString* encodedKey = [key stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
#pragma clang diagnostic pop
        
        NSString* part = [NSString stringWithFormat:@"%@=%@", encodedKey, encodedValue];
        [parts addObject:part];
    }
    
    NSString* encodedDictionary = [parts componentsJoinedByString:@"&"];
    return [encodedDictionary dataUsingEncoding:NSUTF8StringEncoding];
}

@end
