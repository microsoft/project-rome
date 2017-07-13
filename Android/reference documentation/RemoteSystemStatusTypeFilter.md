# RemoteSystemStatusTypeFilter
An **IRemoteSystemFilter** that limits the set of discoverable remote systems by allowing only those of a specific availability status.

## Syntax
`public class RemoteSystemStatusTypeFilter implements IRemoteSystemFilter`

## Public constructors

### RemoteSystemStatusTypeFilter
Initializes an instance of the **RemoteSystemStatusTypeFilter** class.

`public RemoteSystemStatusTypeFilter(RemoteSystemStatusType type)`

#### Parameters  
*type* - the status type to target.

## Public methods

### getType
Returns the status type being targeted.

`public RemoteSystemStatusType getType()`

#### return value  
The status type being targeted

### filter
Checks whether a remote system passes through the filter.

`public boolean filter(RemoteSystem remoteSystem)`

#### Parameters  
*remoteSystem* - the **RemoteSystem** to check

#### return value  
**true** if *remoteSystem* passes, otherwise **false**