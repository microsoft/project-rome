# Discover remote devices (Android client)
With the "Project Rome" SDK for Android, you can connect to and communicate with your Windows devices from an Android device. The host devices do not need to have any special software installed in order to be discoverable by the client device, but they must be signed on with the same Microsoft account (MSA) that you used to authorize your Android app (communal devices that can accept anonymous connections, such as the Surface Hub and Xbox One, are also discoverable). See [Getting started with Connected Devices (Android)](getting-started-rome-android.md) for an explanation of MSA authorization as well as the basic end-to-end scenario for Android-to-Windows remote connectivity.

This guide offers a closer look at how to discover Windows host devices from an Android client and utilize the more in-depth features in this area.

>Note: The code snippets in this guide will not work properly unless you have already initialized the Connected Devices platform by following the steps in [Getting started with Connected Devices (Android)](getting-started-rome-android.md).

## Filter the set of discoverable devices
In cases where you are only looking for certain types of devices to connect to, you can narrow down the set of discoverable devices by using a **RemoteSystemDiscovery** object with filters. Filters can detect the discovery type (local network vs. cloud connection), device type (desktop, mobile device, Xbox, Hub, and Holographic), and availability status (the status of a device's availability to use Remote System features). See examples of all three filter types below. 

```java
// Device type filter:
// make a list of allowed devices (in this case, only Surface Hub) 
List<String> kinds = new ArrayList<String>(); 
kinds.add(RemoteSystemKinds.HUB);
// construct a filter with the given list
RemoteSystemKindFilter kindFilter = new RemoteSystemKindFilter(kinds); 
 
// Discovery type filter:
// in this case, only discover devices through cloud connection
RemoteSystemDiscoveryTypeFilter discoveryFilter = new RemoteSystemDiscoveryTypeFilter(RemoteSystemDiscoveryType.CLOUD); 
 
// Status type filter:
// only discover devices which are readily available for connection
RemoteSystemStatusTypeFilter statusFilter = new RemoteSystemStatusTypeFilter(RemoteSystemStatusType.AVAILABLE);
```
For a look at all of the options available to each filter type, see the reference documentation of the filter objects being used.

Next, pass these filters into a **RemoteSystemDiscovery.Builder** instance, and use it to produce a **RemoteSystemDiscovery** object.

```java
RemoteSystemDiscovery discovery = new RemoteSystemDiscovery.Builder()
    .filter(kindFilter) // add the filters
    .filter(discoveryFilter)
    .filter(statusFilter)
    .setListener(new IRemoteSystemDiscoveryListener() { 
        //...
    })                  // set the listener for discovery events
    .getResult();       // return a RemoteSystemDiscovery instance


```

## Implement the handler for discovery events
From here, the procedure for handling events, retrieving **RemoteSystem** objects, and connecting to remote devices is exactly the same as in [Getting started with Connected Devices (Android)](getting-started-rome-android.md). In short, the **RemoteSystem** objects are passed in as parameters of the **RemoteSystemAdded** events, which are raised by the **RemoteSystemDiscovery** object and handled by the implementation of **IRemoteSystemDiscoveryListener** that was provided to it.

## Discover devices by address input
Some devices may not be associated with a user's MSA or discoverable with a scan, but they can still be reached if the client app uses a direct address. This can either be the IP address or the machine name for the device. If a valid host string is provided, the corresponding **onRemoteSystemAdded** event will be thrown and handled.

```java
// discover using IP address
String ipAddress = "198.51.100.0";
remoteSystemDiscovery.findByHostName(ipAddress);

// discover using machine name
String deviceName = "DESKTOP-ABCD"
remoteSystemDiscovery.findByHostName(ipAddress);

// events are raised and handled by the IRemoteSystemDiscoveryListener implementation
```

## Related topics
[Getting started with Connected Devices (Android)](getting-started-rome-android.md)
