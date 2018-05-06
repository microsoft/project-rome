//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import "AppServiceProvider.h"
#import "LaunchUriProvider.h"

@protocol InboundRequestLoggerDelegate
- (void)logDidUpdate:(NSString*)log;
@end

@interface InboundRequestLogger : NSObject <LaunchUriProviderDelegate, AppServiceProviderDelegate>
@property(nonatomic, weak) id<InboundRequestLoggerDelegate> delegate;
@property(nonatomic, readonly, copy) NSString* log;
@end
