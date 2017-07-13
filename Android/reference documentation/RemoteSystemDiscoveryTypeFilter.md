# RemoteSystemDiscoveryTypeFilter class

An **IRemoteSystemFilter** that limits the set of discoverable remote systems by allowing only those of a specific discovery type.

## Syntax
`public final class RemoteSystemDiscoveryTypeFilter implements IRemoteSystemFilter`

## Public constructors
   
### RemoteSystemDiscoveryTypeFilter
Initializes an instance of the RemoteSystemDiscoveryTypeFilter class.

`public public RemoteSystemDiscoveryTypeFilter(RemoteSystemDiscoveryType type)` 

#### Parameters  
*type* - the discovery type to target.

## Public methods

### getType
Returns the discovery type being targeted.

`public RemoteSystemDiscoveryType getType()`

#### return value  
The discovery type being targeted

### filter
Checks whether a remote system passes through the filter.

`public boolean filter(RemoteSystem remoteSystem)`

#### Parameters  
*remoteSystem* - the **RemoteSystem** to check

#### return value  
**true** if *remoteSystem* passes, otherwise **false**