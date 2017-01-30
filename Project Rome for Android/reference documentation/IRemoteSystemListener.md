how is this used - how does it attach to a RemoteSystem ???

package com.microsoft.connecteddevices;

public interface IRemoteSystemListener {
    void onConnecting();
    void onConnected();
    void onDisconnecting();
    void onDisconnected();
    void onError(String message);
}
