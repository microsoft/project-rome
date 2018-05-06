//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <ConnectedDevices/Core/Core.h>

#import "SingleUserAccountProvider.h"

/**
 * @brief
 * Sample implementation of MCDUserAccountProvider.
 * Exposes a single MSA account, that the user logs into via UIWebView, to CDP.
 * Follows OAuth2.0 protocol, but automatically refreshes tokens when they are close to expiring.
 */
@interface MSAAccountProvider : NSObject <SingleUserAccountProvider>
// @brief clientId is a guid from the app's registration in the msa portal
- (nullable instancetype)initWithClientId:(nonnull NSString*)clientId;
@property(readonly, nonatomic, copy, nonnull) NSString* clientId;
@end
