# Communicate with a remote app service (Android client)
In addition to launching an app on a remote Windows device using a URI, your Android app can also call app services on Windows devices. This allows Android and Windows devices to communicate with each other via generic messages that can be handled by apps on both platforms. 

This gives you an almost unlimited number of ways to interact with Windows devices from your Android app&mdash;all without needing to bring an app to the foreground on the Windows device.

>Note: The code snippets in this guide will not work properly unless you have already initialized the remote systems platform by following the steps in [Launch an app on a remote device (Android client)](launch-a-remote-app-android.md).

## Make
First, your app must discover a remote device to connect to. See [Launch an app on a remote device (Android client)](launch-a-remote-app-android.md) for a simple way to do this, or [Discover remote devices (Android client)](disover-remote-device-android.md) for more in-depth options.

```java
// the "remoteSystem" object has already been selected.
// create a RemoteSystemConnectionRequest for it
RemoteSystemConnectionRequest connectionRequest = new RemoteSystemConnectionRequest(remoteSystem);
 
// Set the AppServiceName for the Windows host
String pServiceName = "com.microsoft.test.myservice"; 
     
// Set the PackageFamilyName for the Windows host 
String pfName = "Abc.AbcMyapplication_j9d5akq8073h6"; 

// Connect to the app service via an AppServiceClientConnection 
AppServiceClientConnection appServiceClientConnection = new AppServiceClientConnection(appServiceName, packageFamilyName, connectionRequest); 
 
// Set up a listener to send a callback when the AppServiceClientConnection is made 
AppServiceClientListener listener = new AppServicesClientListener(); 
appServiceClientConnection.addListener(listener); 
 
appServiceClientConnection.openRemoteAsync(); 

//...

// Close AppConnection 
appServiceClientConnection.close(); 
```
The listener object handles all the communication with the remote app service.

```java 
// Implement listener class for the AppConnection 
class AppConnectionListener implements IAppServiceClientConnectionListener { 
 
    public void openRemoteSuccess(AppServiceClientConnectionStatus status) { 
        If(status == SUCCESS){ 
        Bundle message = new Bundle(); 
        message.putChar(“here is my message”, ‘a’); 
        //send a message 
        appServiceClientConnection.sendMessageAsync(message); 
    } 
 
    public void responseReceived(AppServiceClientResponse response) { 
         
//check that the message was successfully transmitted 
        if(response.getStatus() ==  SUCCESS){ 
              //it worked, lets send another message 
              Bundle message = new Bundle(); 
              message.putChar(“another message”,’b’); 
              appServiceClientConnection.sendMessageAsync(message); 
             } 
        else{ 
            log(“The message was “ + response.getStatus().toString()); 
        } 
    } 
} 
```