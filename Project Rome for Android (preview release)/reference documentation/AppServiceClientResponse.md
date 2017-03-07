# AppServiceClientResponse class
Represents information passed from a remote app service to the client app, namely the status of the last message sent by the client app and (optionally) a new message from the remote app service.

## Syntax
`public final class AppServiceClientResponse`

## Public methods

### getMessage
Retrieves the message sent by the remote app service, consisting of key/value pairs.

`public Bundle getMessage()`

**Return value**  
 A [**Bundle**](https://developer.android.com/reference/android/os/Bundle.html) object containing String keys mapped to values of variable types.

### getStatus
Retrieves the status of the last message sent from the client app to the remote app service.

`public AppServiceResponseStatus getStatus()`

**Return value**  
An [**AppServiceResponseStatus**](AppServiceResponseStatus.md) value describing the status of the most recent message sent to the remote app service (such as a reason why the message data was not delivered).