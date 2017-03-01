# IAppServiceResponseListener interface
Handles the receiving of information from a remote app service.

## Syntax
`public interface IAppServiceResponseListener`

## Public methods

### responseReceived
Called when a response from a remote app service has been received through the Connected Devices platform.

`void responseReceived(AppServiceClientResponse response)`

**Parameters**  
*response* - an [**AppServiceClientResponse**](AppServiceClientResponse.md) instance representing the app service's response.