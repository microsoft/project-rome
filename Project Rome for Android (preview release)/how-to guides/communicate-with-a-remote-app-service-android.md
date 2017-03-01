# Communicate with a remote app service (Android client)
In addition to launching an app on a remote Windows device using a URI, your Android app can also interact with app services on Windows devices. This allows Android and Windows devices to communicate with each other via generic messages that can be handled by the apps on both platforms. 

This provides an almost unlimited number of ways to communicate with Windows devices from your Android app&mdash;all without needing to bring an app to the foreground of the Windows device.

>Note: The code snippets in this guide will not work properly unless you have already initialized the remote systems platform by following the steps in [Getting started with Connected Devices (Android)](getting-started-rome-android.md).

## Set up the app service on the target device
In order to interact with an app service on a Windows device, you must already have a provider of that app service installed on the device. For information on setting this up, see the UWP version of this guide, [Communicate with a remote app service](https://msdn.microsoft.com/en-us/windows/uwp/launch-resume/communicate-with-a-remote-app-service). Regardless of which platform the client device is on, the setup procedure for the app service on the *host* device is exactly the same.

## Open an app service connection
Your app must first acquire a reference to a remote device. See [Getting started with Connected Devices (Android)](getting-started-rome-android.md) for a simple way to do this, or [Discover remote devices (Android client)](disover-remote-device-android.md) for more in-depth options. 

Your app will identify its targeted Windows app service by two strings: the *app service name* and *package family name*. These are found in the source code of the app service provider (see [Create and consume an app service](https://msdn.microsoft.com/windows/uwp/launch-resume/how-to-create-and-consume-an-app-service) for details).

```java
// the "remoteSystem" object has already been selected.
// create a RemoteSystemConnectionRequest for it
RemoteSystemConnectionRequest connectionRequest = new RemoteSystemConnectionRequest(remoteSystem);
 
// Set the AppServiceName for the Windows host
String appServiceName = "com.microsoft.test.myservice"; 
     
// Set the PackageFamilyName for the Windows host 
String packageFamilyName = "Abc.AbcMyapplication_j9d5akq8073h6"; 

// Connect to the app service via an AppServiceClientConnection 
AppServiceClientConnection appServiceClientConnection = new AppServiceClientConnection(appServiceName, packageFamilyName, connectionRequest); 
 
// Add a listener for connection events (this class is defined later)
AppServiceClientListener listener = new AppServicesClientListener(); 
appServiceClientConnection.addListener(listener); 

// open the connection
appServiceClientConnection.openRemoteAsync(); 

```
## Send and receive messages
An event listener is needed to handle all communication with the remote app service. You must create a class that implements **IAppServiceClientConnectionListener** to serve this purpose. 
>Note: Information is sent from the Android app to a Windows app service in the same way it is done between different activities of an Android app: through a **Bundle** object. The remote systems platform translates this into a [**ValueSet**](https://msdn.microsoft.com/library/windows/apps/windows.foundation.collections.valueset) object (of the .NET Framework), which can then be interpreted by the Windows app service. Information passed in the other direction undergoes the reverse translation.

```java 
// Implement listener class for the app service connection 
class AppConnectionListener implements IAppServiceClientConnectionListener { 
 
    public void openRemoteSuccess(AppServiceClientConnectionStatus status) { 
        if (status == SUCCESS???) { 
            // create and send a message to the app service
            Bundle message = new Bundle(); 
            message.putChar("here is my message", 'a');  
            appServiceClientConnection.sendMessageAsync(message); 
        }
    } 
 
    public void responseReceived(AppServiceClientResponse response) { 
         
        //check that the message was successfully transmitted 
        if (response.getStatus() == SUCCESS) { 
            // send another message to the app service 
            Bundle message = new Bundle(); 
            message.putChar("another message",'b'); 
            appServiceClientConnection.sendMessageAsync(message); 
        } else { 
            Log.i("message","The message status was " + response.getStatus().toString()); 
        } 
    } 
} 
```

When your app is finished interacting with the host device's app service, close the connection between the two devices.

```java
// Close connection 
appServiceClientConnection.close(); 
```


---
??? need a scenario where we receive message from app service.
??? why do we check the status in the openRemoteSuccess handler?
