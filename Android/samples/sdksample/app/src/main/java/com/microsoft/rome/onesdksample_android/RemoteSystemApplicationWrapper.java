//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.content.Context;

import com.microsoft.connecteddevices.commanding.RemoteSystemConnectionRequest;
import com.microsoft.connecteddevices.discovery.RemoteSystem;
import com.microsoft.connecteddevices.discovery.RemoteSystemApplication;

/**
 * This class abstracts the type (either a Remote System or Application) and allows both objects to
 * be used as the same object.
 */
public class RemoteSystemApplicationWrapper {
    // region Member Variables
    // These objects are mutually exclusive, the wrapper can only be constructed with 1 type
    private RemoteSystem mRemoteSystem = null;
    private RemoteSystemApplication mRemoteSystemApplication = null;
    // endregion

    // region Constructor
    public RemoteSystemApplicationWrapper(RemoteSystemApplication remoteSystemApplication) {
        this.mRemoteSystemApplication = remoteSystemApplication;
    }

    public RemoteSystemApplicationWrapper(RemoteSystem remoteSystem) {
        this.mRemoteSystem = remoteSystem;
    }
    // endregion

    /**
     * Returns a string containing the type (either system or application) with the name of the object.
     * @return String containing the type (either system or application) with the name of the object.
     */
    public String getDisplayName() {
        if (mRemoteSystem != null) {
            return "[System] " + mRemoteSystem.getDisplayName();
        }
        return "[App] " + mRemoteSystemApplication.getDisplayName();
    }

    // returns string that will show the whether or not the system or application is available
    public String getAvailabilityStatus(Context context) {
        if (mRemoteSystem != null) {
            return "Status: " + mRemoteSystem.getStatus().toString();
        }
        String proximity =
            context.getString((mRemoteSystemApplication.getIsAvailableByProximity()) ? R.string.available_text : R.string.unavailable_text);
        String spatial = context.getString(
            (mRemoteSystemApplication.getIsAvailableBySpatialProximity()) ? R.string.available_text : R.string.unavailable_text);
        return String.format("Proximity: %s, Spatial: %s", proximity, spatial);
    }

    /**
     * Creates and returns a new RemoteSystemConnectionRequest containing the stored system or app.
     * @return
     */
    public RemoteSystemConnectionRequest getRemoteSystemConnectionRequest() {
        if (mRemoteSystem != null) {
            return new RemoteSystemConnectionRequest(mRemoteSystem);
        }
        return new RemoteSystemConnectionRequest(mRemoteSystemApplication);
    }
}