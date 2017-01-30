# IRemoteSystemFoundListener interface

when is this used ??? in what classes/methods does it belong?

package com.microsoft.connecteddevices;

/**
 * Interface for a listener called when looking for a specific device
 */
public interface IRemoteSystemFoundListener {
    /**
     * Called when the remote system is found
     * @param remoteSystem Either the remote system or null if it was not found
     */
    void onFound(RemoteSystem remoteSystem);
}
