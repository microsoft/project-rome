# RemoteSystemKindFilter class
An [**IRemoteSystemFilter**](IRemoteSystemFilter.md) that limits the set of discoverable remote systems by allowing only those of specific device types.

## Syntax
`public final class RemoteSystemKindFilter implements IRemoteSystemFilter`

## Public constructors

### RemoteSystemKindFilter
Initializes an instance of the RemoteSystemKindFilter class with a list of string representations of device types to target.

`public RemoteSystemKindFilter(List<RemoteSystemKind> kinds)`

**Parameters**  
*kinds* - A list of string representations of the device types to target. These strings should conform to the values in the **RemoteSystemKind** enum.

## Public methods

### getKinds
Returns a list of the kinds of remote systems being targeted.

`public List<RemoteSystemKind> getKinds()`
    
**Return value**  
A list of the kinds of remote systems being targeted.

### filter
Check whether a remote system passes through the filter.

`public boolean filter(RemoteSystem remoteSystem)`

**Parameters**  
*filter* - the **RemoteSystem** to check

**Return value**  
**true** if *remoteSystem* passes, otherwise **false**
    