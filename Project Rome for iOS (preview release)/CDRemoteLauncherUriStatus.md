//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

typedef NS_ENUM(NSInteger, CDRemoteLauncherUriStatus) {
    CDRemoteLauncherUriStatusUnknown = 0,
    CDRemoteLauncherUriStatusSuccess = 1,
    CDRemoteLauncherUriStatusAppUnvailable = 2,
    CDRemoteLauncherUriStatusProtocolUnavailable  = 3,
    CDRemoteLauncherUriStatusRemoteSystemUnavailable = 4,
    CDRemoteLauncherUriStatusBundleTooLarge = 5,
    CDRemoteLauncherUriStatusDeniedByLocalSystem = 6,
    CDRemoteLauncherUriStatusDeniedByRemoteSystem = 7
};
