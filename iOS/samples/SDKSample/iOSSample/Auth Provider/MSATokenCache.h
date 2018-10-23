//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import <Foundation/Foundation.h>

// @brief Receives callback from the cache for any permanent failures
@protocol MSATokenCacheDelegate <NSObject>
- (void)onTokenCachePermanentFailure;
@end

// @brief Interface for caching and automatically refreshing MSA refresh and access tokens.
// Refresh tokens are automatically saved to disk.
// On permanent failure (cannot retry), a callback is sent to the delegate.
// These interfaces currently only support one user. forUser: will be added after platform support for multi-user is enabled.
@interface MSATokenCache : NSObject
+ (nullable instancetype)cacheWithClientId:(nonnull NSString*)clientId delegate:(nullable id<MSATokenCacheDelegate>)delegate;
- (nullable instancetype)initWithClientId:(nonnull NSString*)clientId delegate:(nullable id<MSATokenCacheDelegate>)delegate;

// @brief Adds/gets tokens to/from the cache, automatically refreshing them once expired.
- (void)setRefreshToken:(nonnull NSString*)refreshToken;
- (void)setAccessToken:(nonnull NSString*)accessToken forScope:(nonnull NSString*)scope expiresIn:(NSTimeInterval)expiry;
- (void)getRefreshTokenAsync:(nonnull void (^)(NSString* _Nullable accessToken))callback;
- (void)getAccessTokenForScopeAsync:(nonnull NSString*)scope callback:(nonnull void (^)(NSString* _Nullable accessToken))callback;

// @brief Returns the scopes for which there are currently access tokens cached.
- (nonnull NSArray<NSString*>*)allScopes;

// @brief Attempts to load a refresh token that was previously saved, and returns the success value of the operation.
// If successful, the loaded refresh token can be retrieved from getRefreshTokenAsync:
- (BOOL)loadSavedRefreshToken;

// @brief Clears the cache, including the saved refresh token.
- (void)clearTokens;

// @brief Marks all tokens as expired, such that a refresh will be attempted before the next time any token is returned.
- (void)markAllTokensExpired;

@property(nonatomic, readwrite, nullable, strong) id<MSATokenCacheDelegate> delegate;
@end