# AppServiceClientResponse class
Represents information passed from a remote app service to the client app.

## Syntax
`public final class AppServiceClientResponse`

## Public methods

### getMessage
Retrieves the message sent by the app service, consisting of key/value pairs.

`public Bundle getMessage()`

**Return value**  
 a [**Bundle**](https://developer.android.com/reference/android/os/Bundle.html) object containing String keys mapped to values of variable types.

### getStatus
Retrieves the status of the remote app service's response.

`public AppServiceResponseStatus getStatus()`

**Return value**  
an [**AppServiceResponseStatus**](AppServiceResponseStatus.md) value describing the status of the message (such as a reason that the message data could not be received)