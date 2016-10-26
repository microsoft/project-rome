# Communicate with a remote app service (Android client)
In addition to launching an app on a remote Windows device using a URI, your Android app can also call app services on Windows devices. This allows Android and Windows devices to communicate with each other via generic messages that can be handled by apps on both platforms. 

This gives you an almost unlimited number of ways to interact with Windows devices from your Android app&mdash;all without needing to bring an app to the foreground on the Windows device.

>Note: The code snippets in this guide will not work properly unless you have already initialized the remote systems platform by following the steps in [Launch an app on a remote device (Android client)](launch-a-remote-app-android.md).

## Set up the app service on the target device
In order to interact with an app service on a Windows device, you must already have a provider of that app service installed on the device. For instructions on how to set this up, see the UWP-client version of this guide, [Communicate with a remote app service](https://msdn.microsoft.com/en-us/windows/uwp/launch-resume/communicate-with-a-remote-app-service). Regardless of which platform the client device is on, the setup procedure for the app service on the host device is exactly the same.

## Open an app service connection
Your app must first discover the remote device to connect to. See [Launch an app on a remote device (Android client)](launch-a-remote-app-android.md) for a simple way to do this, or [Discover remote devices (Android client)](disover-remote-device-android.md) for more in-depth options. Your app will identify its targeted Windows app service by two strings: the *app service name* and *package family name*. These are found in the source code of the app service provider (see [Create and consume an app service](https://msdn.microsoft.com/windows/uwp/launch-resume/how-to-create-and-consume-an-app-service) for details).

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
## Communicate with the app service
A listener object is needed to handle all communication with the remote app service. Create a class that implements **IAppServiceClientConnectionListener** to serve this purpose.

```java 
// Implement listener class for the app service connection 
class AppConnectionListener implements IAppServiceClientConnectionListener { 
 
    public void openRemoteSuccess(AppServiceClientConnectionStatus status) { 
        if (status == SUCCESS) { 
            Bundle message = new Bundle(); 
            message.putChar("here is my message", 'a'); 
            //send the message 
            appServiceClientConnection.sendMessageAsync(message); 
        }
    } 
 
    public void responseReceived(AppServiceClientResponse response) { 
         
        //check that the message was successfully transmitted 
        if (response.getStatus() ==  SUCCESS) { 
            //it worked, lets send another message 
            Bundle message = new Bundle(); 
            message.putChar("another message",'b'); 
            appServiceClientConnection.sendMessageAsync(message); 
        } else { 
            log("The message was " + response.getStatus().toString()); 
        } 
    } 
} 
```

When your app is finished messaging/querying the host device's app service, close the connection between the two devices.

```java
// Close AppConnection 
appServiceClientConnection.close(); 
```