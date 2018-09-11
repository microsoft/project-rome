//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "LaunchUriProvider.h"
#import <UIKit/UIApplication.h>

@implementation LaunchUriProvider

- (instancetype)initWithDelegate:(id<LaunchUriProviderDelegate>)delegate
{
    if (self = [super init])
    {
        _delegate = delegate;
    }

    return self;
}

- (void)onLaunchUriAsync:(nonnull NSString*)uri
                 options:(nullable MCDRemoteLauncherOptions*)options
              completion:(nonnull void (^)(BOOL, NSError* _Nullable))completionBlock
{
    [_delegate launchUriProvider:self didReceiveRequestForUri:uri];
    dispatch_async(dispatch_get_main_queue(), ^{
        [[UIApplication sharedApplication] openURL:[NSURL URLWithString:uri]
            options:@{}
            completionHandler:^(BOOL success) { completionBlock(success, nil); }];
    });
}

- (NSArray<NSString*>*)supportedUriSchemes
{
    return @[ @"http", @"https" ];
}

@end
