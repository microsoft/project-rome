# Communicate with a remote app service (Android client)
In addition to launching an app on a remote Windows device using a URI, your Android app can also interact with app services on Windows devices. This allows Android and Windows devices to communicate with each other via generic messages that can be handled by the apps on both platforms. 

This provides an almost unlimited number of ways to communicate with Windows devices from your Android app&mdash;all without needing to bring an app to the foreground of the Windows device. See the [Android sample app](../sample/) for a working example of remote app service connectivity.

>Note: The code snippets in this guide will not work properly unless you have already initialized the remote systems platform by following the steps in [Getting started with Connected Devices (Android)](getting-started-rome-android.md).

## Set up the app service on the target device
In order to interact with an app service on a Windows device, you must already have a provider of that app service installed on the device. For information on how to set this up, see the UWP version of this guide, [Communicate with a remote app service](https://msdn.microsoft.com/en-us/windows/uwp/launch-resume/communicate-with-a-remote-app-service). Regardless of which platform the client device is on, the setup procedure for the app service on the *target* device is exactly the same.

## Open an app service connection
Your app must first acquire a reference to a remote device. See [Getting started with Connected Devices (Android)](getting-started-rome-android.md) for a simple way to do this, or [Discover remote devices (Android client)](disover-remote-device-android.md) for more in-depth options. 

Your app will identify its targeted Windows app service by two strings: the *app service name* and *package family name*. These are found in the source code of the app service provider (see [Create and consume an app service](https://msdn.microsoft.com/windows/uwp/launch-resume/how-to-create-and-consume-an-app-service) for details). It also must implement an [**IAppServiceClientConnectionListener**](../IAppServiceClientConnectionListener) and [**IAppServiceResponseListener**](../IAppServiceResponseListener) to handle events related to the connection itself and communications over that connection. This is done in the next section.

```java
// the "remoteSystem" object reference has already been selected.
// create a RemoteSystemConnectionRequest for it
RemoteSystemConnectionRequest connectionRequest = new RemoteSystemConnectionRequest(remoteSystem);
 
// Set the AppServiceName for the Windows host
String appServiceName = "com.microsoft.example"; 
     
// Set the PackageFamilyName for the Windows host 
String packageFamilyName = "Abc.Example_abc123"; 

// Instantiate implementations of IAppServiceClientConnectionListener and IAppServiceResponseListener (defined in the next section)
IAppServiceClientConnectionListener connectionListener = new AppServiceClientConnectionListener();
IAppServiceResponseListener responseListener = new AppServiceResponseListener();

// Construct an AppServiceClientConnection 
AppServiceClientConnection appServiceClientConnection = new AppServiceClientConnection(appServiceName, packageFamilyName, connectionRequest, connectionListener, responseListener); 

// open the connection (will throw a ConnectedDevicesException if there is an error)
try {
    appServiceClientConnection.openRemoteAsync(); 
} catch (ConnectedDevicesException e) {
    e.printStackTrace();
}
```

## Handle connection events

Here, the implementations of the listener interfaces used above are defined. These classes handle connection-related events as well as events that represent response messages from the app service.

```java 
// Define the connection listener class:
class AppServiceClientConnectionListener implements IAppServiceClientConnectionListener { 
    
    @Override
    public void onSuccess() {
        Log.i("MyActivityName", "AppServiceClientConnectionListener onSuccess");
        // connection was successful. initiate messaging or adjust UI to enable a messaging scenario.
    }

    @Override
    public void onError(AppServiceClientConnectionStatus status) {
        Log.e("MyActivityName", "AppServiceClientConnectionListener onError status [" + status.toString()+"]");
        // failed to establish connection. inspect the cause of error and reflect back to the UI
    }

    @Override
    public void onClosed(AppServiceClientClosedStatus status) {
        Log.i("MyActivityName", "AppServiceClientConnectionListener onClosed status [" + status.toString()+"]");
        // the connection closed. inspect the cause of closure and reflect back to the UI
    }
} 

// Define the response listener class:
class AppServiceResponseListener implements IAppServiceResponseListener { 
 
    @Override
    public void responseReceived(AppServiceClientResponse response) {
        AppServiceResponseStatus status = response.getStatus();

        if (status == AppServiceResponseStatus.SUCCESS) {
            // last message was delivered successfully

            Bundle bundle = response.getMessage();
            Log.i("MyActivityName", "Received successful AppService response");
            // parse the expected key/value data stored in "bundle"
        } else {
            Log.e("MyActivityName", "IAppServiceResponseListener.responseReceived status != SUCCESS");
            // inspect "status" for the cause of unsuccessful message delivery
        }
    }
} 
```

## Send messages to the app service

Once the app service connection is established, sending a message to the app service is simple and can be done from anywhere in the app that has a reference to the connection instance.

```java
Bundle newMessage = new Bundle();
// populate the Bundle with keys and values that the app service will be able to handle.
//...//

// use the AppServiceClientConnection instance to send the message (will throw a ConnectedDevicesException if there is an error)
try {
    appServiceClientConnection.sendMessageAsync(newMessage);
} catch (ConnectedDevicesException e) {
    e.printStackTrace();
}
```

The app service's response will be received and parsed by the handler to the **IAppServiceResponseListener.responseReceived** event.

>Note: Information is sent from the Android app to a Windows app service in the same way it is done between different activities of an Android app: through a [**Bundle**](https://developer.android.com/reference/android/os/Bundle.html) object. The Connected Devices platform translates this into a [**ValueSet**](https://msdn.microsoft.com/library/windows/apps/windows.foundation.collections.valueset) object (of the .NET Framework), which can then be interpreted by the Windows app service. Information passed in the other direction undergoes the reverse translation.

## Finish app service communication

When your app is finished interacting with the target device's app service, close the connection between the two devices.

```java
// Close connection 
appServiceClientConnection.close(); 
```

## Related topics
[Getting started with Connected Devices (Android)](getting-started-rome-android.md)