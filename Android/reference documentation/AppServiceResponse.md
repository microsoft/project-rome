# AppServiceResponse class
Represents a message passed from a remote app service to the client app in response to a previously sent message.

## Syntax
`public final class AppServiceResponse`

## Public methods

### getMessage
Retrieves the message sent by the remote app service, consisting of key/value pairs.

`public Bundle getMessage()`

#### return value  
A [**Bundle**](https://developer.android.com/reference/android/os/Bundle.html) object containing String keys mapped to values of variable types.

### getStatus
Retrieves the status of the response from the remote app service.

`public AppServiceResponseStatus getStatus()`

#### return value  
An [**AppServiceResponseStatus**](AppServiceResponseStatus.md) value describing the status of the response.