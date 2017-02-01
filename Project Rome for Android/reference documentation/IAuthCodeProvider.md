# IAuthCodeProvider interface
Provides methods that produce the MSA OAuth credentials needed for initializing the Remote Systems platform. An implementation of this interface gets passed in to the [**Platform.initialize**](Platform.md) method.

## Syntax
`public interface IAuthCodeProvider`

## Public methods

### fetchAuthCodeAsync
Called within the **Platform.initialize** method when the Remote Systems platform requests an OAuth token.

`void fetchAuthCodeAsync(String url, Platform.IAuthCodeHandler handler)`

**Parameters**  
*url* - The URL that should be used to sign in the user with OAuth  
*handler* - The handler that the app will later invoke with the new auth code

### getClientId
Called within the **Platform.initialize** method when the Remote Systems platform requests the client app's id.

`String getClientId()`

**Return value**  
The client ID that represents the current application
