# IAppServiceConnectionListener interface
Contains methods that handle events related to the connection to a remote app service.

## Syntax
`public interface IAppServiceConnectionListener`

## Public methods

### onSuccess
Called when the connection to a remote app service was established successfully. 

`void onSuccess()`

### onError
Called when the Remote Systems platform failed to establish a connection to a remote app service.

`void onError(AppServiceConnectionStatus status)`

#### Parameters  
*status* - an [**AppServiceConnectionStatus**](AppServiceConnectionStatus.md) value describing the status of the connection (the cause of the error).

### onClosed
Called when the connection to a remote app service is closed.

`void onClosed(AppServiceConnectionClosedStatus status)`

#### Parameters  
*statues* - an [**AppServiceConnectionClosedStatus**](AppServiceConnectionClosedStatus.md) value describing the reason the app service connection was closed.