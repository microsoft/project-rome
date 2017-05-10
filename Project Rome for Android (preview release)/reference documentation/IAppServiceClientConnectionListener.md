# IAppServiceClientConnectionListener interface
Contains methods that handle events related to the connection to a remote app service.

## Syntax
`public interface IAppServiceClientConnectionListener`

## Public methods

### onSuccess
Called when the connection to a remote app service was established successfully. 

`void onSuccess()`

### onError
Called when the Connected Devices platform failed to establish a connection to a remote app service.

`void onError(AppServiceClientConnectionStatus status)`

**Parameters**  
*status* - an [**AppServiceClientConnectionStatus**](AppServiceClientConnectionStatus.md) value describing the status of the connection (the cause of the error).

### onClosed
Called when the connection to a remote app service is closed.

`void onClosed(AppServiceClientConnectionClosedStatus status)`

**Parameters**  
*statues* - an [**AppServiceClientConnectionClosedStatus**](AppServiceClientConnectionClosedStatus.md) value describing the reason the app service connection was closed.