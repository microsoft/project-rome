# ConnectedDevicesResult class
The result of an action by the Remote Systems platform

## Syntax
`public final class ConnectedDevicesResult`

## Public fields

### error
The Remote Systems platform error

`public final ConnectedDevicesError error`

### platformError
The value of the Android device platform error

`public final int platformError`

## Public methods

### isSuccess
 Determines whether the instance represents a successful result.

`public boolean isSuccess()`

 **Return value**  
 **true** if this instance represents a success, **false** if not

### isFailure
Determines whether the instance represents a failed result.

`public boolean isFailure()`

**Return value**  
 **true** if this instance represents a failure, **false** if not
