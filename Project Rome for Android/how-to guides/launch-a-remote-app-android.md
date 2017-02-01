# Launch an app on a remote device (Android)
This guide shows you how to remotely launch a Universal Windows Platform (UWP) app or Windows desktop app on a Windows device from an app on an Android device. Refer to the [Android sample app](?) for a working example.

Remote app launching can be useful when the user wishes to start a task on one device and finish it on another. For example, you might receive a Skype call on your Android phone and later wish to launch the Skype app on your desktop PC to continue the call there. Note, however, that the client's and host's apps do not need to be the same: your Android app can launch any Windows app on a connected Windows device.

Remote launch is achieved by sending a Uniform Resource Identifier (URI) from one device to another. A URI specifies a *scheme*, which determines which app(s) can handle its information. See [Launch the default app for a URI](https://msdn.microsoft.com/en-us/windows/uwp/launch-resume/launch-default-app) for information on using URIs to launch Windows apps.

## Initial setup for Connected Devices functionality

Before implementing device discovery and connectivity, there are a few steps you'll need to take to give your Android app the capability to connect to remote Windows devices.

First, you must register your app with Microsoft by following the instructions on the [Microsoft developer portal](https://apps.dev.microsoft.com/). This will allow your app to access Microsoft's Connected Devices platform by having users sign in to their Microsoft accounts (MSAs). After registration, do not copy the generated code from the site into your app.

Add the connecteddevices-core and connecteddevices-sdk AAR dependencies into your app's build.gradle file.

maven repository?

```java
dependencies { 
    compile(group: 'com.microsoft.connecteddevices', name: 'connecteddevices-core-armv7', version: '0.1.04', ext: 'aar', classifier: 'externalRelease') 
    compile(group: 'com.microsoft.connecteddevices', name: 'connecteddevices-sdk-armv7', version: '0.1.04', ext: 'aar', classifier: 'externalRelease') 
}
```

proguard rules?

Next, go to the activity class where you would like the remote system discovery functionality to live (this may be the same activity in which MSA authentication is handled). Add the **connecteddevices** namespace.

```java
import com.microsoft.connecteddevices.*;
```

Before any Connected Devices features can be used, the platform must be initialized. The **Platform.Initialize** method takes 3 parameters (the **Context** for the app, an **IAuthCodeProvider**, and an **IPlatformInitializationHandler**). 
 
The **IAuthCodeProvider**'s **fetchAuthCodeAsync** method is invoked whenever the platform needs the app to fetch an MSA authorization code. This will be called the first time the app is run and upon the expiration of a platform-managed refresh token. 
 
When **fetchAuthCodeAsync** is invoked, the app will need to open a web view with the given OAuth URL string. The user will then need to complete the sign-in to their MSA and accept the permissions. When the OAuth flow is complete, the app will extract the auth code from the web view and supply that value back to the Connected Devices platform by invoking the **IAuthCodeHandler.onAuthCodeFetched** method.

```java
Platform.initialize(getApplicationContext(),  
    new IAuthCodeProvider() { 
        @Override 
        /** 
         * ConnectedDevices Platform needs the app to fetch a MSA auth_code using the given oauthUrl. 
         * When app has fetched the auth_code, it needs to invoke the authCodeHandler onAuthCodeFetched method. 
         */ 
        public void fetchAuthCodeAsync(String oauthUrl, Platform.IAuthCodeHandler authCodeHandler) { 
            // Platform needs an MSA Auth code 
            _oauthUrl = oauthUrl; 
            _authCodeHandler = authCodeHandler; 
            runOnUiThread(new Runnable() { 
                @Override 
                public void run() { 
                    _signInButton.setVisibility(View.VISIBLE); 
                    _signInButton.setEnabled(true); 
                } 
            }); 
        } 
 
        @Override 
        /** 
         * Connected Devices platform needs your app's registered client ID. 
         */ 
        public String getClientId() { 
                return CLIENT_ID; 
        } 
    },  
    new IPlatformInitializationHandler() { 
        @Override 
        public void onDone(boolean succeeded) { 
            if (succeeded) { 
                Log.i(TAG, "Initialized platform successfully"); 
                Intent intent = new Intent(MainActivity.this, DeviceRecyclerActivity.class); 
                startActivity(intent); 
            } else { 
                Log.e(TAG, "Error initializing platform"); 
            } 
        } 
    }
);
```

When the ConnectedDevices platform has finished initializing, it will invoke the **IPlatformInitializationHandler.onDone** method the. If the *succeeded* parameter is true, the platform has initialized, and the app can proceed to now discover the user's devices.

webview?

## Implement device discovery

The Android client SDK, like the Windows implementation, uses a watcher pattern in which available devices are detected via network connection over a period of time and corresponding events are raised. This guide will show a simple scenario; for further details on connecting to Windows devices, see [Discover remote devices (Android client)](discover-remote-devices-android).

Get an instance of **RemoteSystemDiscovery** using its corresponding Builder class. At this point you must also instantiate a custom **RemoteSystemsListener** to handle the discovery events. You may want to show and maintain a list view of all the available remote devices and their basic info.

```java
RemoteSystemDiscovery.Builder discoveryBuilder; 

discoveryBuilder = new RemoteSystemDiscovery.Builder().setListener(new IRemoteSystemDiscoveryListener() { 
    @Override 
    public void onRemoteSystemAdded(RemoteSystem remoteSystem) { 
        // handle the added event. At minimum, you should acquire a reference to the discovered device.
    }
    @Override
    public void onRemoteSystemUpdated(RemoteSystem remoteSystem) {
        // update the reference to the device
    }
    @Override
    public void onRemoteSystemRemoved(String remoteSystemId) {
        // remove the reference to the device
    }
} 

// get the discovery instance
RemoteSystemDiscovery discovery = discoveryBuilder.getResult(); 
// begin watching for remote devices
discovery.start(); 
```

Once **Start** is called, it will begin watching for remote system activity and will raise events when connected devices are discovered, updated, or removed from the set of detected devices.

## Select a connected device

At this point in your code, you have a list of **RemoteSystem** objects that refer to available connected Windows devices. The following code shows how to select one of these objects (ideally this is done through a UI control) and then use a **RemoteLauncher** to launch a URI on it. This will cause the host device to launch the given URI with its default app for that URI scheme. You can use the **IRemoteLauncherListener** implementation to check whether the launch was successful.

```java
// a RemoteSystemConnectionRequest represents the connection to a single device (RemoteSystem object)
// the RemoteSystem object 'remoteSystem' has previously been acquired
RemoteSystemConnectionRequest connectionRequest = new RemoteSystemConnectionRequest(remoteSystem); 

// the URI to launch
Uri myUri = Uri.parse("http://www.bing.com");

// get a RemoteLauncher and use it to launch the URI over this connection
new RemoteLauncher().LaunchUriAsync(connectionRequest, url,
    new IRemoteLauncherListener() {
        @Override
        public void onCompleted(RemoteLaunchUriStatus status) {
            if (status == SUCCESS) {
                // handle success case
            } else {
                // handle fail case, using 'status' to get more info
            }
        }
    }
);
```

The **LaunchUriAsync** method can also take a **RemoteLauncherOptions** object 


 

 
// perform the launch of a URI on the remote device  
Uri myUri = Uri.parse("http://www.bing.com");
launcher.launchUriAsync(myUri, connectionRequest); 
```




