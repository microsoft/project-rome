package com.microsoft.rome.onesdksample_android;
//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

import android.support.annotation.NonNull;

import com.microsoft.connecteddevices.base.AsyncOperation;
import com.microsoft.connecteddevices.hosting.LaunchUriProvider;

/**
 * Base class for launch uri handler.
 */
public abstract class BaseLaunchHandler implements LaunchUriProvider {
    protected MainActivity mMainActivity;

    public BaseLaunchHandler(MainActivity fragment) {
        mMainActivity = fragment;
    }

    @Override
    public @NonNull AsyncOperation<Boolean> onLaunchUriAsync(@NonNull String uri, String fallbackUri, String[] preferredAppIds) {
        // State that the LaunchUri request has been successfully handled
        return AsyncOperation.completedFuture(true);
    }

    @Override
    public String[] getSupportedUriSchemes() {
        return new String[0];
    }
}
