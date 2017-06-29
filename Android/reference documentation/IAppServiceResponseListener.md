# IAppServiceResponseListener interface
Handles the receiving of information from a remote app service.

## Syntax
`public interface IAppServiceResponseListener`

## Public methods

### responseReceived
Called when a response from a remote app service has been received through the Connected Devices platform.

`void responseReceived(AppServiceResponse response)`

**Parameters**  
*response* - an [**AppServiceResponse**](AppServiceResponse.md) instance representing the app service's response.