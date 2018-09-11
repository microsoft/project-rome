//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.support.annotation.NonNull;

import com.microsoft.connecteddevices.base.AsyncOperation;
import com.microsoft.connecteddevices.commanding.RemoteLauncherOptions;

/**
 * Returns success for all incoming LaunchUri requests and logs the request to the Launch Page.
 */
public class SimpleLaunchHandler extends BaseLaunchHandler {
    public SimpleLaunchHandler(MainActivity mainActivity) {
        super(mainActivity);
    }

    /**
     * Handle the launch URI request by calling the base function then notifying the HostingFragment.
     * The HostingFragment has the responsibility of logging the event in it's page specific log.
     */
    @Override
    public @NonNull AsyncOperation<Boolean> onLaunchUriAsync(@NonNull String uri, RemoteLauncherOptions options) {
        AsyncOperation<Boolean> result = super.onLaunchUriAsync(uri, options);

        // Inform the hosting fragment that we received a URI
        mMainActivity.getHostingFragment().logTrafficMessage("SimpleLaunchHandler received uri: " + uri);

        return result;
    }
}
