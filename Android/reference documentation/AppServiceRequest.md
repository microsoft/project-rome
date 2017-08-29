# AppServiceRequest class
Represents a message sent from one app service to another over a remote app service connection.

## Syntax 
`public final class AppServiceRequest`

## Public methods

### getMessage
Retrieves the message that was sent, consisting of key/value pairs.

`public Bundle getMessage()`

#### return value  
A [**Bundle**](https://developer.android.com/reference/android/os/Bundle.html) object containing String keys mapped to values of variable types.

### sendResponseAsync
Sends a response message to the remote app service that sent the original message.

`public void sendResponseAsync(Bundle response, IAppServiceResponseStatusListener listener)`

#### Parameters
*response* - A [**Bundle**](https://developer.android.com/reference/android/os/Bundle.html) object containing the data to be sent. 
*listener* - The listener that will handle a status report on the response message being sent by this method.