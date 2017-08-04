# IAppServiceRequestListener interface
Handles the receiving of incoming app service requests over the remote app service connection.

## Syntax
`public interface IAppServiceRequestListener`

## Public methods

### requestReceived
Called when a request for remote app service connectivity has been received through the Remote Systems platform.

`void requestReceived(AppServiceRequest request)`

#### Parameters  
*request* - an [**AppServiceRequest**](AppServiceRequest.md) instance representing the service request from a remote app service.