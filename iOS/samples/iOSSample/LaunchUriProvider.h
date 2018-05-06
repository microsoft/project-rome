//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import <ConnectedDevices/Hosting/Hosting.h>

@class LaunchUriProvider;

@protocol LaunchUriProviderDelegate
- (void)launchUriProvider:(LaunchUriProvider*)launchUriProvider didReceiveRequestForUri:(NSString*)uri;
@end

@interface LaunchUriProvider : NSObject <MCDLaunchUriProvider>
@property(nonatomic, weak) id<LaunchUriProviderDelegate> delegate;
- (instancetype)initWithDelegate:(id<LaunchUriProviderDelegate>)delegate;

- (void)onLaunchUriAsync:(nonnull NSString*)uri
         withFallbackUri:(nullable NSString*)fallbackUri
     preferredPackageIds:(nullable NSArray<NSString*>*)preferredPackageIds
              completion:(nonnull void (^)(BOOL, NSError* _Nullable))completionBlock;
@property(nonatomic, readonly, strong, nonnull) NSArray<NSString*>* supportedUriSchemes;

@end
