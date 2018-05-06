//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "MSATokenRequest.h"

NSString* const MsaTokenRequestGrantTypeCode = @"authorization_code";
NSString* const MsaTokenRequestGrantTypeRefresh = @"refresh_token";

static const NSTimeInterval MsaTokenRequestTimeout = 30.0;

// Helper function - encodes an NSDictionary to be usable as POST data in an NSURLRequest
static NSData* EncodeDictionary(NSDictionary<NSString*, NSString*>* dictionary)
{
    NSMutableArray<NSString*>* parts = [NSMutableArray<NSString*> new];
    for (NSString* key in dictionary)
    {
        NSString* encodedValue = [[dictionary objectForKey:key] stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
        NSString* encodedKey = [key stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];

        [parts addObject:[NSString stringWithFormat:@"%@=%@", encodedKey, encodedValue]];
    }

    NSString* encodedDictionary = [parts componentsJoinedByString:@"&"];
    return [encodedDictionary dataUsingEncoding:NSUTF8StringEncoding];
}

@interface MSATokenRequestResult ()
+ (instancetype)resultWithStatus:(MSATokenRequestStatus)status responseDictionary:(nullable NSDictionary*)responseDict;
@end

@implementation MSATokenRequestResult
+ (instancetype)resultWithStatus:(MSATokenRequestStatus)status responseDictionary:(NSDictionary*)responseDict
{
    MSATokenRequestResult* ret = [self new];
    if (ret)
    {
        ret.status = status;

        if (responseDict)
        {
            ret.accessToken = [responseDict valueForKey:@"access_token"];
            ret.refreshToken = [responseDict valueForKey:@"refresh_token"];
            ret.expiresIn = [[responseDict valueForKey:@"expires_in"] integerValue];
        }
    }
    return ret;
}

@end

@implementation MSATokenRequest

+ (void)doAsyncRequestWithClientId:(NSString*)clientId
                         grantType:(NSString*)grantType
                             scope:(NSString*)scope
                       redirectUri:(NSString*)redirectUri
                             token:(NSString*)token
                          callback:(void (^)(MSATokenRequestResult*))callback
{

    NSLog(@"Requesting token for scope %@", scope);

    NSURL* url = [NSURL URLWithString:@"https://login.live.com/oauth20_token.srf"];
    NSMutableURLRequest* request =
        [NSMutableURLRequest requestWithURL:url cachePolicy:NSURLRequestUseProtocolCachePolicy timeoutInterval:MsaTokenRequestTimeout];
    [request addValue:@"application/x-www-form-urlencoded" forHTTPHeaderField:@"Content-Type"];

    NSMutableDictionary<NSString*, NSString*>* params = [NSMutableDictionary<NSString*, NSString*> new];
    [params setObject:clientId forKey:@"client_id"];
    [params setObject:grantType forKey:@"grant_type"];
    if ([grantType isEqualToString:MsaTokenRequestGrantTypeCode])
    {
        [params setObject:redirectUri forKey:@"redirect_uri"];
        [params setObject:token forKey:@"code"];
    }
    else if ([grantType isEqualToString:MsaTokenRequestGrantTypeRefresh])
    {
        if (scope)
        {
            [params setObject:scope forKey:@"scope"];
        }
        [params setObject:token forKey:MsaTokenRequestGrantTypeRefresh];
    }
    request.HTTPBody = EncodeDictionary(params);
    request.HTTPMethod = @"POST";

    static NSOperationQueue* queue = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{ queue = [NSOperationQueue new]; });

    NSLog(@"MSATokenRequest issuing HTTP token request.");
    [NSURLConnection sendAsynchronousRequest:request
                                       queue:queue
                           completionHandler:^void(NSURLResponse* response, NSData* data, NSError* error) {

                               NSHTTPURLResponse* httpResponse = (NSHTTPURLResponse*)response; // This cast should always work
                               NSLog(@"MSATokenRequest response code %ld.", (long)httpResponse.statusCode);

                               MSATokenRequestStatus status = MSATokenRequestStatusTransientFailure;
                               if (httpResponse.statusCode >= 500)
                               {
                                   status = MSATokenRequestStatusTransientFailure;
                               }
                               else if (httpResponse.statusCode >= 400)
                               {
                                   status = MSATokenRequestStatusPermanentFailure;
                               }
                               else if ((httpResponse.statusCode >= 200 && httpResponse.statusCode < 300) || httpResponse.statusCode == 304)
                               {
                                   status = MSATokenRequestStatusSuccess;
                               }
                               else
                               {
                                   status = MSATokenRequestStatusTransientFailure;
                               }

                               if (data)
                               {
                                   NSDictionary* responseDict = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
                                   NSLog(@"MSATokenRequest data:%@", responseDict);
                                   callback([MSATokenRequestResult resultWithStatus:status responseDictionary:responseDict]);
                               }
                               else
                               {
                                   NSLog(@"MSATokenRequest error:%@", error);
                                   callback([MSATokenRequestResult resultWithStatus:status responseDictionary:nil]);
                               }
                           }];
}

+ (instancetype)tokenRequestWithClientId:(NSString*)clientId
                               grantType:(NSString*)grantType
                                   scope:(NSString*)scope
                             redirectUri:(NSString*)redirectUri
{
    return [[self alloc] initWithClientId:clientId grantType:grantType scope:scope redirectUri:redirectUri];
}

- (instancetype)initWithClientId:(NSString*)clientId
                       grantType:(NSString*)grantType
                           scope:(NSString*)scope
                     redirectUri:(NSString*)redirectUri
{
    if (self = [super init])
    {
        _clientId = [clientId copy];
        _grantType = [grantType copy];
        _scope = [scope copy];
        _redirectUri = [redirectUri copy];
    }
    return self;
}

- (void)requestAsyncWithToken:(NSString*)token callback:(void (^)(MSATokenRequestResult*))callback
{
    [[self class] doAsyncRequestWithClientId:_clientId
                                   grantType:_grantType
                                       scope:_scope
                                 redirectUri:_redirectUri
                                       token:token
                                    callback:callback];
}

@end
