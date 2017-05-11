# class `RemoteSystemConnectionRequest class`

```
@interface CDRemoteSystemConnectionRequest : NSObject
```

Represents an attempt to communicate with a specific remote system (device).

## Public constructors

### RemoteSystemConnectionRequest
Initializes a new instance of the RemoteSystemConnectionRequest class for a particular remote system.

`public RemoteSystemConnectionRequest(RemoteSystem remote)`

**Parameters**  
*remote* - the **RemoteSystem** object to attempt to connect to.

## Public methods

### getRemoteSystem
Retrieves the **RemoteSystem** object for this particular connection request.

`public RemoteSystem getRemoteSystem()`

**Return value**  
The **RemoteSystem** object for this particular connection request
