//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import <Foundation/Foundation.h>

extern NSString* _Nonnull const MsaTokenRequestGrantTypeCode;
extern NSString* _Nonnull const MsaTokenRequestGrantTypeRefresh;

typedef NS_ENUM(NSInteger, MSATokenRequestStatus)
{
    MSATokenRequestStatusSuccess,
    MSATokenRequestStatusTransientFailure,
    MSATokenRequestStatusPermanentFailure
};

@interface MSATokenRequestResult : NSObject
@property(readwrite, nonatomic) MSATokenRequestStatus status;

@property(readwrite, nullable, nonatomic, copy) NSString* accessToken;
@property(readwrite, nullable, nonatomic, copy) NSString* refreshToken;
@property(readwrite, nonatomic) NSInteger expiresIn;

@end

/**
 * @brief Encapsulates a noninteractive request for an MSA token.
 * This request may be performed multiple times.
 */
@interface MSATokenRequest : NSObject

/**
 * Fetches Token (Access or Refresh Token).
 * clientId - clientId of the app's registration in the MSA portal
 * grantType - one of the MsaTokenRequestGrantType constants
 * scope
 * redirectUri
 * token - authCode for MsaTokenRequestGrantTypeCode, or refresh token for MsaTokenRequestGrantTypeRefresh
 */
+ (void)doAsyncRequestWithClientId:(nonnull NSString*)clientId
                         grantType:(nonnull NSString*)grantType
                             scope:(nullable NSString*)scope
                       redirectUri:(nullable NSString*)redirectUri
                             token:(nonnull NSString*)token
                          callback:(nonnull void (^)(MSATokenRequestResult* _Nonnull result))callback;

+ (nullable instancetype)tokenRequestWithClientId:(nonnull NSString*)clientId
                                        grantType:(nonnull NSString*)grantType
                                            scope:(nullable NSString*)scope
                                      redirectUri:(nullable NSString*)redirectUri;

- (nullable instancetype)initWithClientId:(nonnull NSString*)clientId
                                grantType:(nonnull NSString*)grantType
                                    scope:(nullable NSString*)scope
                              redirectUri:(nullable NSString*)redirectUri;

@property(readonly, nonnull, nonatomic, copy) NSString* clientId;
@property(readonly, nonnull, nonatomic, copy) NSString* grantType;
@property(readonly, nullable, nonatomic, copy) NSString* scope;
@property(readonly, nullable, nonatomic, copy) NSString* redirectUri;

- (void)requestAsyncWithToken:(nonnull NSString*)token callback:(nonnull void (^)(MSATokenRequestResult* _Nonnull result))callback;

@end
