# AppServiceRequest class
TBD

## Syntax 
`public final class AppServiceRequest`

## Public methods

### getMessage
Retrieves the message sent by the remote app service, consisting of key/value pairs.

`public Bundle getMessage()`

#### return value  
A [**Bundle**](https://developer.android.com/reference/android/os/Bundle.html) object containing String keys mapped to values of variable types.

### sendResponseAsync
TBD

`public void sendResponseAsync(Bundle response, IAppServiceResponseStatusListener listener)`

#### Parameters
*response* - A [**Bundle**](https://developer.android.com/reference/android/os/Bundle.html) object containing the data to be sent. 
*listener* - The response listener that will indicate whether TBD.