//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

/** @brief The result of an action against a Remote System. */
typedef NS_ENUM(NSInteger, CDRemoteSystemStatus) {
    /** @brief The status of the RemoteSystem is unknown. */
    CDRemoteSystemStatusUnknown = 0,
    /** @brief The status of the RemoteSystem is still being determined. */
    CDRemoteSystemStatusDiscoveringAvailability,
    /** @brief The RemoteSystem is reported as being available. */
    CDRemoteSystemStatusAvailable,
    /** @brief The RemoteSystem is reported as being unavailable. */
    CDRemoteSystemStatusUnavailable
};
