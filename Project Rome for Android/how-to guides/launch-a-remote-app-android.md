# Launch an app on a remote device (Android)
This guide shows you how to remotely launch a Universal Windows Platform (UWP) app or Windows desktop app on a Windows device from an app on an Android device.

Remote app launching can be useful when the user wishes to start a task on one device and finish it on another. For example, you might receive a Skype call on your Android phone and later wish to launch the Skype app on your desktop PC to continue the call there. Note, however, that the client's and host's apps do not need to be the same: your Android app can launch any Windows app on a connected Windows device.

Remote launch is achieved by sending a Uniform Resource Identifier (URI) from one device to another. A URI specifies a *scheme*, which determines which app(s) can handle its information. See [Launch the default app for a URI](https://msdn.microsoft.com/en-us/windows/uwp/launch-resume/launch-default-app) for information on using URIs to launch Windows apps.

## Initial setup for Remote Systems functionality

Before implementing device discovery and connectivity, there are a few steps you'll need to take to give your Android app the capability to connect to remote Windows devices.

First, you must register your app with Microsoft by following the instructions on the [Microsoft developer portal](https://apps.dev.microsoft.com/). Copy the provided code blocks to their respective locations in your Android app project. This will allow your app to access Microsoft's remote systems platform by having users sign in to their Microsoft accounts (MSAs).

Next, go to the activity class where you would like the remote system discovery functionality to live (this may be the same activity in which MSA authentication is handled). Add the **remotesystems** namespace.

```java
import com.microsoft.remotesystems (or rome???)
```

**add .aar files as module dependencies here**

** update androidmanifest** (taken care of in ms dev web instructions)

Next, you must initialize the remote systems platform with your app's user Id, device Id, and access token. The user Id, also known as the app Id or client Id in this scenario, is unique to your app and was obtained upon registering with Microsoft in the step above. You can find it on the [app list page of the Microsoft developer portal](https://apps.dev.microsoft.com/#/appList).  

The device Id, or Android Id, is unique to the *user* on the device and can be obtained with the following method call:

```java
final string deviceId = android.provider.Settings.Secure.getString(getApplicationContext().getContentResolver(),android.provider.Settings.Secure.ANDROID_ID);
```

Finally, the app's access token is obtained when the user signs on to his or her MSA through the app. To reference it programatically, find the **onSuccess** event handler in the authentication code provided by the Microsoft developer portal. The access token is stored in the **AuthenticationResult** object that is passed in:

```java
@Override
public void onSuccess(AuthenticationResult result)
{
    Log.v(AUTH_TAG, "Successfully obtained token, still need to validate");
    if (result != null && !result.getAccessToken().isEmpty())
    {
        try
        {
            String accessToken = result.getAccessToken();
            //...
```

Now you have obtained the credentials necessary to initialize the remote systems platform. 

```java
// initialize remote systems platform
RemoteSystemsPlatform remoteSystemsPlatform = new RemoteSystemsPlatform();
remoteSystemsPlatform.initialize(deviceId, userId, accessToken);
```

## Implement device discovery

The Android client SDK, like the Windows implementation, uses a watcher pattern in which available devices are detected via Network connection over a period of time and corresponding events are raised. This guide will show a simple scenario; for further details on connecting to Windows devices, see [Discover remote devices (Android client)](discover-remote-devices-android).

Use a **RemoteSystemDiscovery** object to watch for remote system events. Then, you instantiate a custom **RemoteSystemsListener** to handle these events.

>Note: the **RemoteSystemsListener** class will be implemented next.

```java
// Create a RemoteSystemDiscovery for discovery of devices 
RemoteSystemDiscovery remoteSystemDiscovery = new RemoteSystemDiscovery(); 
 
// Set up a listener to receive a callback when a new system is seen as available by the RemoteSystemDiscovery 
RemoteSystemsListener listener = new RemoteSystemsListener(); 
remoteSystemDiscovery.addListener(listener); 
 
// Start remote system discovery 
remoteSystemDiscovery.start();
```

Once **Start** is called, it will begin watching for remote system activity and will raise events when remote systems are discovered, updated, or removed from the set of detected devices. You must extend the the IRemoteSystemsDiscoveryListener class (what is already implemented in this superclass???) to handle the remote system events.

In this example, the listener class maintains a map of the available remote systems and their device Ids. It also has a basic outline for displaying this changing set of remote systems on the UI.


```java
// Implement listener class 
class RemoteSystemsListener extends??? IRemoteSystemsDiscoveryListener { 
    // this map will hold RemoteSystem objects and their unique Id strings. A RemoteSystem object represents a connected remote device.
    private Map<String, RemoteSystem> mDiscoveredDevices; 
 
    // handle when a new system is seen as available by the watcher 
    public void onRemoteSystemAdded(RemoteSystem remoteSystem) { 
        // add to the map
        mDiscoveredDevices.put(remoteSystem.getId(), remoteSystem); 
        
        runOnUiThread(new Thread(new Runnable() { 
            public void run() { 
                // update UI to show the new system (recommended)
            } 
        }); 
    } 
 
    // handle when a previously added remote system is updated 
    public void onRemoteSystemUpdated(RemoteSystem remoteSystem) { 
        // update the map item
        mDiscoveredDevices.put(remoteSystem.getId(), remoteSystem); 
    } 
 
    // handle when a remote system is removed 
    public void onRemoteSystemRemoved(RemoteSystem remoteSystem) { 
        // remove the map item
        mDiscoveredDevices.remove(remoteSystem.getId(); 

        runOnUiThread(new Thread(new Runnable() { 
            public void run() { 
                // update UI to remove the missing system (recommended)
            } 
        }); 
    } 
} 
```

## Select a remote system
At this point in your code, you have a **RemoteSystemsListener** object, `listener`, which should contain a map of **RemoteSystem** objects and their Id strings (assuming Windows devices were discovered). Select one of these objects (ideally through a UI control) and then use a **RemoteLauncher** to launch a URI on it. This should cause the host device to launch the given URI with its default app for that URI scheme. Optionally, you can implement (???) an IRemoteLauncherListener to handle events related to the launching of the URI, such as checking whether the launch was successful.

```java
// the "remoteSystem" object has been picked from the set of discovered devices

RemoteLauncher launcher = new RemoteLauncher();   
// this class implements (???) IRemoteLauncherListener and must be defined:
RemoteSystemLauncherListener launcherListener = new RemoteSystemLauncherListener();
launcher.addListener(launcherListener);
 
// a RemoteSystemConnectionRequest represents the connection to a single device (RemoteSystem object)
RemoteSystemConnectionRequest connectionRequest = new RemoteSystemConnectionRequest(remoteSystem); 
 
// perform the launch of a URI on the remote device  
Uri myUri = Uri.parse("http://www.bing.com");
launcher.launchUriAsync(myUri, connectionRequest); 
```




