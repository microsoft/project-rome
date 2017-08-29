# Getting started with Connected Devices (Android)
This guide shows you how to remotely launch a Universal Windows Platform (UWP) app or Windows desktop app on a Windows device from an app on an Android device. Refer to the [Android sample app](../sample/) for a working example.

Remote app launching can be useful when the user wishes to start a task on one device and finish it on another. For example, you might receive a Skype call on your Android phone and later wish to launch the Skype app on your desktop PC to continue the call there. Note, however, that the client's and host's apps do not need to be the same: your Android app can launch any Windows app on a connected Windows device.

Remote launch is achieved by sending a Uniform Resource Identifier (URI) from one device to another. A URI specifies a *scheme*, which determines which app(s) can handle its information. See [Launch the default app for a URI](https://msdn.microsoft.com/en-us/windows/uwp/launch-resume/launch-default-app) for information on using URIs to launch Windows apps.

## Preliminary setup for Connected Devices functionality

Before implementing device discovery and connectivity, there are a few steps you'll need to take to give your Android app the capability to connect to remote Windows devices.

First, you must register your app with Microsoft by following the instructions on the [Microsoft developer portal](https://apps.dev.microsoft.com/). This will allow your app to access Microsoft's Connected Devices platform by having users sign in to their Microsoft accounts (MSAs). After registration, do not copy the generated code from the site into your app. All the necessary code snippets are in this guide.

Add the connecteddevices-core and connecteddevices-sdk AAR dependencies into your app's build.gradle file.

```java
repositories {
    maven {
        url "https://projectrome.bintray.com/maven"
    }
}

dependencies { 
    compile(group: 'com.microsoft.connecteddevices', name: 'connecteddevices-sdk-armeabi-v7a', version: '0.6.2', ext: 'aar', classifier: 'release')
}

```

If you wish to use ProGuard in your app, add the ProGuard Rules for these new APIs. Create a file called *proguard-rules.txt* in the *App* folder of your project, and paste in the contents of [ProGuard_Rules_for_Android_Rome_SDK.txt](../ProGuard_Rules_for_Android_Rome_SDK.txt).

In your project's *AndroidManifest.xml* file, add the following permissions inside the `<manifest>` element (if they are not already present). This gives your app permission to connect to the Internet and to enable Bluetooth discovery on your device.

> Note: The Bluetooth-related permissions are only necessary for using Bluetooth discovery; they are not needed for the other features in the Connected Devices platform. Additionally, `ACCESS_COARSE_LOCATION` is only required on Android SDKs 21 and later. On Android SDKs 23 and later, the developer must also prompt the user to grant location access at runtime.

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

```

Next, go to the activity class where you would like the remote system discovery functionality to live (this may be the same activity in which MSA authentication is handled). Add the **connecteddevices** namespace.

```java
import com.microsoft.connecteddevices.*;
```

## Initialize the Connected Devices platform

Before any Connected Devices features can be used, the platform must be initialized. The **Platform.Initialize** method takes 3 parameters (the **Context** for the app, an **IAuthCodeProvider**, and an **IPlatformInitializationHandler**). It is recommended that you call this method from within the activity class' **onCreate** method.
 
The **IAuthCodeProvider**'s **fetchAuthCodeAsync** method is invoked whenever the platform needs the app to fetch an MSA authorization token. This will be called the first time the app is run and upon the expiration of a platform-managed Refresh Token. In this example, **fetchAuthCodeAsync** will invoke a user-defined method dedicated to MSA authentication. When the OAuth flow is complete, the app must call the **IAuthCodeHandler**'s **onAuthCodeFetched** method to extract the authorization code from the web view and supply that value back to the Connected Devices platform.

```java
Platform.initialize(getApplicationContext(),  
    // implement an IAuthCodeProvider
    new IAuthCodeProvider() { 
        @Override 
        // ConnectedDevices Platform needs the app to fetch a MSA auth_code using the given oauthUrl. 
        // When app has fetched the auth_code, it needs to invoke the 
        public void fetchAuthCodeAsync(String oauthUrl, final Platform.IAuthCodeHandler authCodeHandler) { 
            // launch the dedicated OAuth method, passing in the oauth URL and the IAuthCodeHandler instance provided by the platform
            performOAuthFlow(oauthUrl, authCodeHandler);
        }
 
        @Override 
        // Connected Devices platform also needs the app ID to initialize
        public String getClientId() { 
            // recommended: retrieve app ID previously and store as a global constant. 
            // The app ID is provided when you register your app on the Microsoft developer portal
            // (https://apps.dev.microsoft.com/)
            return APP_ID; 
        } 
    },  
    // Implement an IPlatformInitializationHandler - not required
    new IPlatformInitializationHandler() { 
        @Override 
        public void onDone() { 
            // execute code when initialization is complete
            // ...
        } 
    }
);
```

The **performOAuthFlow** method for manual user sign-in is defined here. Note that it references a **WebView** layout object, defined later on.

```java
public performOAuthFlow(String oauthUrl, final Platform.IAuthCodeHandler authCodeHandler) {
    WebView web;
    // the Dialog authDialog and WebView layout webv are defined separately
    web = (WebView) authDialog.findViewById(R.id.webv);
    
    // required WebView settings:
    web.setWebChromeClient(new WebChromeClient());
    web.getSettings().setJavaScriptEnabled(true);
    web.getSettings().setDomStorageEnabled(true);
    
    // load the given URL for auth code fetching
    web.loadUrl(oauthUrl);

    // define a WebViewClient to interact with this URL
    WebViewClient webViewClient = new WebViewClient() {
        boolean authComplete = false;
        
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            // check URL for authorization success. REDIRECT_URI should be defined 
            // at the class level as "https://login.live.com/oauth20_desktop.srf"
            if (url.startsWith(REDIRECT_URI)) {
                // extract the auth code from the url
                Uri uri = Uri.parse(url);
                String code = uri.getQueryParameter("code");
                String error = uri.getQueryParameter("error");
                
                if (code != null && !authComplete) {
                    // finally, pass auth code into the onAuthCodeFetched method,
                    // so the platform initialization can continue
                    authComplete = true;
                    authCodeHandler.onAuthCodeFetched(code);
                } else if (error != null) {    
                    // Handle error case 
                }
            }
        }
    };
    // set the WebViewClient to the WebView's URL
    web.setWebViewClient(webViewClient);

    // display the authentication dialog to the user
    authDialog.show();
    authDialog.setCancelable(true);
}
```

You must define the UI elements that will allow the app user to enter their MSA credentials when prompted. In the *res>layout* folder of your project, create a file called *auth_dialog.xml*. Paste in the following contents to create a simple **WebView** layout.

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical" >
    <WebView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:id="@+id/webv"/>
</LinearLayout>
```

Now go back to the activity class that contains the previous platform initialization work. In its **onCreate** method, declare a **Dialog** variable and couple it with your new **WebView** layout.

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    // ...

    authDialog = new Dialog(this);
    authDialog.setContentView(R.layout.auth_dialog);

    // ...
}
```

At this point you should have all the elements necessary to handle user sign-on and initialize the Connected Devices platform (by calling the **performOAuthFlow** method from anywhere in your app). When the platform has finished initializing, it will invoke the **IPlatformInitializationHandler.onDone** method. If the *succeeded* parameter is true, the platform has initialized, and the app can proceed to discover the user's devices.

## Implement device discovery

The Android client SDK, like the Windows implementation, uses a watcher pattern in which available devices are detected via network connection over a period of time and corresponding events are raised. This guide will show a simple scenario; for further details on connecting to Windows devices, see [Discover remote devices (Android client)](discover-remote-devices-android).

Get an instance of **RemoteSystemDiscovery** using its corresponding Builder class. At this point you must also instantiate a custom **RemoteSystemsListener** to handle the discovery events. You may want to show and maintain a list view of all the available remote devices and their basic info.

```java

// use the builder pattern to get a RemoteSystemDiscovery instance.
RemoteSystemDiscovery discovery = new RemoteSystemDiscovery.Builder()
    .setListener(new IRemoteSystemDiscoveryListener() {	// set the listener for discovery events
        @Override 
        public void onRemoteSystemAdded(RemoteSystem remoteSystem) { 
            // handle the added event. At minimum, you should acquire a 
            // reference to the discovered device.
        }
        @Override
        public void onRemoteSystemUpdated(RemoteSystem remoteSystem) {
            // update the reference to the device
        }
        @Override
        public void onRemoteSystemRemoved(String remoteSystemId) {
            // remove the reference to the device
        }
        @Override
        public void onComplete(){
            // execute code when the initial discovery process has completed
        }
    })
    .getResult(); // return a RemoteSystemDiscovery instance

// begin watching for remote devices
discovery.start(); 
```

Once **Start** is called, it will begin watching for remote system activity and will raise events when connected devices are discovered, updated, or removed from the set of detected devices.

## Select a connected device

At this point in your code, you have a list of **RemoteSystem** objects that refer to available connected Windows devices. The following code shows how to select one of these objects (ideally this is done through a UI control) and then use **RemoteLauncher** to launch a URI on it. This will cause the host device to launch the given URI with its default app for that URI scheme. You can use the **IRemoteLauncherListener** implementation to check whether the launch was successful.

```java
// a RemoteSystemConnectionRequest represents the connection to a single device (RemoteSystem object)
// the RemoteSystem object 'remoteSystem' has previously been acquired
RemoteSystemConnectionRequest connectionRequest = new RemoteSystemConnectionRequest(remoteSystem); 

// the URI to launch
Uri myUri = Uri.parse("http://www.bing.com");

// use RemoteLauncher to launch the URI over this connection
RemoteLauncher().LaunchUriAsync(connectionRequest, myUri.toString(),
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

## Related topics
[Discover remote devices (Android client)](discover-remote-devices-android.md)

