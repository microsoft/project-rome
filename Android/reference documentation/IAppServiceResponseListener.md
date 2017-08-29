# IAppServiceResponseListener interface
Handles the receiving of a message sent from a remote app service in response to a previously sent message.

## Syntax
`public interface IAppServiceResponseListener`

## Public methods

### responseReceived
Called when a response from a remote app service has been received through the Remote Systems platform.

`void responseReceived(AppServiceResponse response)`

#### Parameters  
*response* - an [**AppServiceResponse**](AppServiceResponse.md) instance representing the app service's response.