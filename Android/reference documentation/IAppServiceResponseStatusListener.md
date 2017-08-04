# IAppServiceResponseStatusListener interface
Handles the receiving of a status report on an outgoing app service response message.

## Syntax
`public interface IAppServiceResponseStatusListener`

## Public methods

### statusReceived
Called when a status report has been received.

`void statusReceived(AppServiceResponseStatus status)`

#### Parameters  
*status* - an [**AppServiceResponseStatus**](AppServiceResponseStatus.md) value representing the status of the sent response.