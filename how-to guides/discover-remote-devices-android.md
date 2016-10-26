# Discover remote devices (Android client)
With the Project "Rome" SDK for Android, you can connect to and communicate with your Windows devices from an Android device. The host devices do not need to have any special software installed in order to be discoverable by the client device, but they must be signed on with the same Microsoft account (MSA) that you used to authorize your Android app (communal devices that can accept anonymous connections, such as the Surface Hub and Xbox One, are also discoverable). See [Launch an app on a remote device (Android)](launch-a-remote-app-android.md) for an explanation of MSA authorization as well as the basic end-to-end scenario for Android-to-Windows remote connectivity.

This guide offers a closer look at how to discover Windows host devices from an Android client and utilize the more in-depth features in this area.

>Note: The code snippets in this guide will not work properly unless you have already initialized the remote systems platform by following the steps in [Launch an app on a remote device (Android)](launch-a-remote-app-android.md).

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

Next construct a **RemoteSystemDiscovery** object with a list (???) of filters.

```java
List<IRemoteSystemFilter> filters = new ArrayList<IRemoteSystemFilter>(); 
filters.add(kindFilter); 
filters.add(discoveryFilter); 
 
// Build a RemoteSystem to discover devices 
RemoteSystemDiscovery remoteSystemDiscovery = new RemoteSystemDiscovery(filters); 
```

From here, the procedure for handling events, retrieving **RemoteSystem** objects, and connecting to remote devices is exactly the same as in [Launch an app on a remote device (Android)](launch-a-remote-app-android.md). In short, the **RemoteSystem** objects are passed in as parameters of the **RemoteSystemAdded events**, which are raised by the **RemoteSystemDiscovery** object.

Filter objects must be constructed before the **RemoteSystemWatcher** object is initialized, because they are passed as a parameter into its constructor. The following code creates a filter of each type available and then adds them to a list.

## Discover devices by address input
Some devices may not be associated with a user's MSA or discoverable with a scan, but they can still be reached if the client app uses a direct address. This is often given in the form of an IP address, but several other formats are allowed (???).

A **RemoteSystem** object is retrieved if a valid host string is provided. If the address data is invalid, a null object reference is returned.

```java
RemoteSystemDiscover remoteSystemDiscovery = new RemoteSystemDiscovery();
RemoteSystem remoteSystem = remoteSystemDiscovery.findByHostName("???");
```

## Related topics
[Launch an app on a remote device (Android)](launch-a-remote-app-android.md)


---
builder pattern -???- wait to see what final API surface looks like
