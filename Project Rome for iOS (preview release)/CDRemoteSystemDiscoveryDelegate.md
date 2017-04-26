//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "CDRemoteSystem.h"

@class CDRemoteSystemDiscovery;

/**
 * @brief Set of methods to be implemented to act as a CDRemoteSystemDiscovery delegate.
 */
@protocol CDRemoteSystemDiscoveryDelegate <NSObject>

/**
 * @brief Called when a Remote System has been discovered.
 * @remarks Optional
 * @param discovery The delegating Remote System Discovery.
 * @param remoteSystem The discovered Remote System.
 */
@optional
-(void)remoteSystemDiscoveryFound:(nonnull CDRemoteSystemDiscovery*)discovery remoteSystem:(nonnull CDRemoteSystem*)remoteSystem;

/**
 * @brief Called when a previously discovered Remote System has been removed.
 * @remarks Optional
 * @param discovery The delegating CDRemoteSystemDiscovery.
 * @param remoteSystem The discovered Remote System.
 */
@optional
-(void)remoteSystemDiscoveryRemoved:(nonnull CDRemoteSystemDiscovery*)discovery remoteSystem:(nonnull CDRemoteSystem*)remoteSystem;

/**
 * @brief Called when a previously discovered RemoteSystem has been updated.
 * @remarks Optional
 * @param discovery The delegating CDRemoteSystemDiscovery.
 * @param remoteSystem The discovered Remote System.
 */
@optional
-(void)remoteSystemDiscoveryUpdated:(nonnull CDRemoteSystemDiscovery*)discovery remoteSystem:(nonnull CDRemoteSystem*)remoteSystem;

/**
 * @brief Called when the discovery operation has completed successfully.
 * @param discovery The delegating CDRemoteSystemDiscovery.
 */
@optional
-(void)remoteSystemDiscoveryCompleted:(nonnull CDRemoteSystemDiscovery*)discovery;

@end
