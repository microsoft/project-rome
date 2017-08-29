# AppServiceConnection class
This class manages a connection to an app service on a remote device.

## Syntax
`public final class AppServiceConnection`

## Public constructors

### AppServiceConnection
Initializes an instance of the AppServiceConnection class with a name and Id of the app service, a generic remote connection request, and a listeners for related connection events.

`public AppServiceConnection(String appServiceName, String appIdentifier, RemoteSystemConnectionRequest request, IAppServiceConnectionListener appServiceConnectionListener, IAppServiceRequestListener requestListener) throws InvalidParameterException`  

#### Parameters  
*appServiceName* - the name of the remote app service. [See Create and consume an app service](https://docs.microsoft.com/en-us/windows/uwp/launch-resume/how-to-create-and-consume-an-app-service) for details.  
*appIdentifier* - the package family name of the remote app service. See [See Create and consume an app service](https://docs.microsoft.com/en-us/windows/uwp/launch-resume/how-to-create-and-consume-an-app-service) for details.  
*request* - a [**RemoteSystemConnectionRequest**](RemoteSystemConnectionRequest.md) object representing the intent to connect to a specific remote system  
*appServiceConnectionListener* - an [**IAppServiceConnectionListener**](IAppServiceConnectionListener.md) that handles events related to the connection itself.  
*requestListener* - an [**IAppServiceRequestListener**](IAppServiceRequestListener.md) that handles incoming app service requests over the remote app service connection.

## Public methods

### openRemoteAsync
Opens a connection to the remote app service specified in this class' constructor. If the connection fails to open, an exception is thrown.

`public void openRemoteAsync() throws ConnectedDevicesException` 

### sendMessageAsync
Sends a message to the connected remote app service consisting of key/value pairs.

`public void sendMessageAsync(Bundle messageBundle,  IAppServiceResponseListener responseListener) throws ConnectedDevicesException`

#### Parameters  
*messageBundle* - a [**Bundle**](https://developer.android.com/reference/android/os/Bundle.html) object containing String keys mapped to values of variable types  
*responseListener* - an [**IAppServiceResponseListener**](IAppServiceResponseListener.md) that handles the receipt of the remote app service's response to the message being sent.

### closeAsync
Closes the connection to the remote app service. This is recommended when the client app closes or stops.

`public void closeAsync()`

### getAppServiceName
Returns the app service name with which this AppServiceConnection instance was constructed.

`public String getAppServiceName()`

#### return value  
The name string of the target app service.

### getPackageFamilyName
Returns the package family name with which this AppServiceConnection instance was constructed.

`public String getPackageFamilyName()`

#### return value  
The package family name string of the target app service.
