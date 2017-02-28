# AppServiceClientConnection class
This class manages a connection to an app service on a remote device TBD

## Syntax
`public final class AppServiceClientConnection`

## Public constructors

### AppServiceClientConnection
Initializes an instance of the AppServiceClientConnection class with a name, Id, connection listener, and response listener.

`public AppServiceClientConnection(String appServiceName, String appIdentifier, RemoteSystemConnectionRequest request, IAppServiceClientConnectionListener appServiceClientConnectionListener, IAppServiceResponseListener responseListener) throws InvalidParameterException`
**Parameters**  
*appServiceName* - TBD
*appIdentifier*
*request*
*appServiceClientConnectionListener*
*responseListener*

## Public methods

### closeAsync
Closes TBD

`public void closeAsync()`

### openRemoteAsync
Opens TBD

`public void openRemoteAsync() throws ConnectedDevicesException` 

### sendMessageAsync
sends TBD

`public void sendMessageAsync(Bundle messageBundle) throws ConnectedDevicesException`

**Parameters**  
*messageBundle* - the message TBD

### getAppServiceName
gets TBD

`public String getAppServiceName()`

**Return value**  
the TBD

### getPackageFamilyName
TBD

`public String getPackageFamilyName()`

**Return value**  
the TBD

### setAppLaunched
TBD

`private void setAppLaunched(boolean appLaunched)`

**Parameters**  
*appLaunched* - TBD
