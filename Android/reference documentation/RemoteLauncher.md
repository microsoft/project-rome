# RemoteLauncher class
This class handles the launching of an app on a remote device through the use of a URI.

## Syntax
`public final class RemoteLauncher extends Listenable<IRemoteLauncherListener>`

## Public methods

### LaunchUriAsync
Starts the default app associated with the URI scheme name for the specified URI on a remote device.

`public static void LaunchUriAsync(RemoteSystemConnectionRequest remoteSystemConnectionRequest, String uri, IRemoteLauncherListener listener) throws ConnectedDevicesException`

#### Parameters  
*remoteSystemConnectionRequest* - Specifies which remote system to connect to  
*uri* - The URI which will cause the launching of an app, according to its scheme  
*listener* - The **IRemoteLauncherListener** to handle the outcome of this launch attempt

### LaunchUriAsync
Starts the default app associated with the URI scheme name for the specified URI on a remote device, using the specified options.

`public static void LaunchUriAsync(RemoteSystemConnectionRequest remoteSystemConnectionRequest, String uri, RemoteLauncherOptions options, IRemoteLauncherListener listener) throws ConnectedDevicesException`

#### Parameters  
*remoteSystemConnectionRequest* - Specifies which remote system to connect to  
*uri* - The URI which will cause the launching of an app, according to its scheme  
*options* - The launch specifications for the app  
*listener* - The **IRemoteLauncherListener** to handle the outcome of this launch attempt