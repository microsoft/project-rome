# Communicate with a remote app service (Android client)
In addition to launching an app on a remote Windows device using a URI, your Android app can also interact with app services on Windows devices. This allows Android and Windows devices to communicate with each other via generic messages that can be handled by the apps on both platforms. 

This provides an almost unlimited number of ways to communicate with Windows devices from your Android app&mdash;all without needing to bring an app to the foreground of the Windows device. See the [Android sample app](../sample/) for a working example of remote app service connectivity.

>Note: The code snippets in this guide will not work properly unless you have already initialized the remote systems platform by following the steps in [Getting started with Connected Devices (Android)](getting-started-rome-android.md).

## Set up the app service on the target device
This guide will use the Random Number Generator app service for UWP, which is available on the [Windows universal samples repo](https://github.com/Microsoft/Windows-universal-samples/tree/master/Samples/AppServices). For instructions on how to write your own UWP app service, see [Create and consume an app service](how-to-create-and-consume-an-app-service.md).

Whether you are using an already-made app service or writing your own, you will need to make a few edits in order to make the service compatible with Connected Devices. In Visual Studio, go to the app service provider project and select its Package.appxmanifest file. Right-click and select **View Code** to view the full contents of the file. Find the **Extension** element that defines the project as an app service and names its parent project.

``` xml
...
<Extensions>
    <uap:Extension Category="windows.appService" EntryPoint="RandomNumberService.RandomNumberGeneratorTask">
        <uap:AppService Name="com.microsoft.randomnumbergenerator"/>
    </uap:Extension>
</Extensions>
...
```

Change the namespace of the **AppService** element to **uap3** and add the **SupportsRemoteSystems** attribute:

``` xml
...
<uap3:AppService Name="com.microsoft.randomnumbergenerator" SupportsRemoteSystems="true"/>
...
```

In order to use elements in this new namespace, you must add the namespace definition at the top of the manifest file.

``` xml
<?xml version="1.0" encoding="utf-8"?>
<Package
  xmlns="http://schemas.microsoft.com/appx/manifest/foundation/windows10"
  xmlns:mp="http://schemas.microsoft.com/appx/2014/phone/manifest"
  xmlns:uap="http://schemas.microsoft.com/appx/manifest/uap/windows10"
  xmlns:uap3="http://schemas.microsoft.com/appx/manifest/uap/windows10/3">
  ...
</Package>
```

Build your app service provider project and deploy it to the target device(s).

## Open an app service connection on the client device
Your Android app must acquire a reference to a remote device. See [Getting started with Connected Devices (Android)](getting-started-rome-android.md) for a simple way to do this, or [Discover remote devices (Android client)](disover-remote-device-android.md) for more in-depth options. 

Your app will identify its targeted Windows app service by two strings: the *app service name* and *package family name*. These are found in the source code of the app service provider (see [Create and consume an app service](https://msdn.microsoft.com/windows/uwp/launch-resume/how-to-create-and-consume-an-app-service) for details). It also must implement an [**IAppServiceClientConnectionListener**](../IAppServiceClientConnectionListener) and [**IAppServiceResponseListener**](../IAppServiceResponseListener) to handle events related to the connection itself and communications over that connection. This is done in the next section.

```java
// the "remoteSystem" object reference has already been selected.
// create a RemoteSystemConnectionRequest for it
RemoteSystemConnectionRequest connectionRequest = new RemoteSystemConnectionRequest(remoteSystem);
 
// Set the AppServiceName for the Windows host
String appServiceName = "com.microsoft.randomnumbergenerator"; 
     
// Set the PackageFamilyName for the Windows host 
String packageFamilyName = "Microsoft.SDKSamples.AppServicesProvider.CS_8wekyb3d8bbwe"; 

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
    public void onClosed(AppServiceClientConnectionClosedStatus status) {
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
* [Getting started with Connected Devices (Android)](getting-started-rome-android.md)
* [Create and consume an app service](how-to-create-and-consume-an-app-service.md).
