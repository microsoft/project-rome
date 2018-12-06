package com.microsoft.rome.onesdksample_android;
//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

import android.support.annotation.NonNull;

import com.microsoft.connecteddevices.base.AsyncOperation;
import com.microsoft.connecteddevices.remotesystems.commanding.LaunchUriProvider;
import com.microsoft.connecteddevices.remotesystems.commanding.RemoteLauncherOptions;

/**
 * Base class for launch uri handler.
 */
public abstract class BaseLaunchHandler implements LaunchUriProvider {
    protected MainActivity mMainActivity;

    public BaseLaunchHandler(MainActivity fragment) {
        mMainActivity = fragment;
    }

    @Override
    public @NonNull AsyncOperation<Boolean> onLaunchUriAsync(@NonNull String uri, RemoteLauncherOptions options) {
        // State that the LaunchUri request has been successfully handled
        return AsyncOperation.completedFuture(true);
    }

    @Override
    public String[] getSupportedUriSchemes() {
        return new String[0];
    }
}
