# ITokenProvider interface
Provides methods that produce the MSA OAuth credentials needed for initializing the Remote Systems platform. An implementation of this interface gets passed in to the [**Platform.initialize**](Platform.md) method.

## Syntax
`public interface ITokenProvider`

## Public methods

### getToken
Called within the **Platform.initialize** method when the Remote Systems platform requests an OAuth token.

`String getToken()`

**Return value**
An OAuth token for the currently logged in MSA (Microsoft account)

### getClientId
Called within the **Platform.initialize** method when the Remote Systems platform requests the client app's id.

`String getClientId()`

**Return value**
The client ID that represents the current application
