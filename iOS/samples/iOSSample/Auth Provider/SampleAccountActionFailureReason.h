//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import <Foundation/Foundation.h>

// @brief MSA failure reason for sign in or sign out action
typedef NS_ENUM(NSInteger, SampleAccountActionFailureReason)
{
    SampleAccountActionNoFailure,
    SampleAccountActionFailureReasonAlreadySignedIn,
    SampleAccountActionFailureReasonAlreadySignedOut,
    SampleAccountActionFailureReasonUserCancelled,
    SampleAccountActionFailureReasonFailToRetrieveAuthCode,
    SampleAccountActionFailureReasonFailToRetrieveRefreshToken,
    SampleAccountActionFailureReasonSigninSignOutInProgress,
    SampleAccountActionFailureReasonUnknown,
    SampleAccountActionFailureReasonADAL,
};

typedef void (^SampleAccountProviderCompletionBlock)(BOOL successful, SampleAccountActionFailureReason reason);