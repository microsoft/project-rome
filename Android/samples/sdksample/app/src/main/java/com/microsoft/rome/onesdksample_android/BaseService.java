/*
 * Copyright (C) Microsoft Corporation. All rights reserved.
 */

package com.microsoft.rome.onesdksample_android;

import android.support.annotation.NonNull;
import android.util.Log;

import com.microsoft.connecteddevices.base.EventListener;
import com.microsoft.connecteddevices.commanding.AppServiceConnection;
import com.microsoft.connecteddevices.commanding.AppServiceRequestReceivedEventArgs;
import com.microsoft.connecteddevices.discovery.AppServiceDescription;
import com.microsoft.connecteddevices.hosting.AppServiceConnectionOpenedEventArgs;
import com.microsoft.connecteddevices.hosting.AppServiceProvider;

/**
 * Base class for app services.
 *
 * Subclasses must implement at least onEvent to handle incoming requests.
 */
public abstract class BaseService implements AppServiceProvider, EventListener<AppServiceConnection, AppServiceRequestReceivedEventArgs> {
    // region Member Variables
    private static final String TAG = BaseService.class.getName();
    protected MainActivity mMainActivity;

    private AppServiceDescription mDescription;
    // This must be kept alive to receive AppService request messages
    private AppServiceConnection mConnection;
    // endregion

    // region Constructor
    public BaseService(MainActivity mainActivity, AppServiceDescription appServiceDescription) {
        mMainActivity = mainActivity;
        mDescription = appServiceDescription;
    }
    // endregion

    // region Overrides
    /**
     * Receive incoming connections and listen for subsequent requests.
     */
    @Override
    public void onConnectionOpened(final @NonNull AppServiceConnectionOpenedEventArgs args) {
        AppServiceConnection connection = args.getAppServiceConnection();
        Log.i(TAG, "Opened incoming connection to app service " + connection.getAppServiceDescription().getPackageId() + "/" +
                       connection.getAppServiceDescription().getName());
        connection.addRequestReceivedListener(this);

        // Capture the AppServiceConnection reference and hold on to it for a while to allow us to receive incoming requests
        mConnection = connection;
    }

    @Override
    public @NonNull AppServiceDescription getAppServiceDescription() {
        return mDescription;
    }
    // endregion
}
