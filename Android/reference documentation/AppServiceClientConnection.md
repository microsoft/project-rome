# AppServiceClientConnection class
This class manages a connection to an app service on a remote device.

## Syntax
`public final class AppServiceClientConnection`

## Public constructors

### AppServiceClientConnection
Initializes an instance of the AppServiceClientConnection class with a name and Id of the app service, a generic remote connection request, and a listeners for related connection events.

`public AppServiceClientConnection(String appServiceName, String appIdentifier, RemoteSystemConnectionRequest request, IAppServiceClientConnectionListener appServiceClientConnectionListener, IAppServiceResponseListener responseListener) throws InvalidParameterException`  

**Parameters**  
*appServiceName* - the name of the remote app service. [See Create and consume an app service](https://docs.microsoft.com/en-us/windows/uwp/launch-resume/how-to-create-and-consume-an-app-service) for details.  
*appIdentifier* - the package family name of the remote app service. See [See Create and consume an app service](https://docs.microsoft.com/en-us/windows/uwp/launch-resume/how-to-create-and-consume-an-app-service) for details.  
*request* - a [**RemoteSystemConnectionRequest**](RemoteSystemConnectionRequest.md) object representing the intent to connect to a specific remote system  
*appServiceClientConnectionListener* - an [**IAppServiceClientConnectionListener**](IAppServiceClientConnectionListener.md) that handles events related to the connection itself.  
*responseListener* - an [**IAppServiceResponseListener**](IAppServiceResponseListener.md) that handles messaging events with the remote app service  

## Public methods

### openRemoteAsync
Opens a connection to the remote app service specified in this class' constructor. If the connection fails to open, an exception is thrown.

`public void openRemoteAsync() throws ConnectedDevicesException` 

### sendMessageAsync
Sends a message to the connected remote app service consisting of key/value pairs.

`public void sendMessageAsync(Bundle messageBundle) throws ConnectedDevicesException`

**Parameters**  
*messageBundle* - a [**Bundle**](https://developer.android.com/reference/android/os/Bundle.html) object containing String keys mapped to values of variable types

### closeAsync
Closes the connection to the remote app service. This is recommended when the client app closes or stops.

`public void closeAsync()`

### getAppServiceName
Returns the app service name with which this AppServiceClientConnection instance was constructed.

`public String getAppServiceName()`

**Return value**  
The name string of the target app service.

### getPackageFamilyName
Returns the package family name with which this AppServiceClientConnection instance was constructed.

`public String getPackageFamilyName()`

**Return value**  
The package family name string of the target app service.
