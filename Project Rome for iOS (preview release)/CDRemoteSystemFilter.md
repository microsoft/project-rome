//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import "CDRemoteSystem.h"

@protocol CDRemoteSystemFilter

/**
 * @brief Checks whether a Remote System is matched by the current filter.
 * @param remoteSystem The Remote System.
 * @returns Whether the filter matches the specified Remote System.
 */
-(BOOL)matchesRemoteSystem:(nonnull CDRemoteSystem*)remoteSystem;

@end
