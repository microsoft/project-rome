# IRemoteSystemFilter interface
Provides the ability to check whether a **RemoteSystem** passes through the implemented filter.

## Syntax
`public interface IRemoteSystemFilter`

## Public methods

### filter
Called to check whether a **RemoteSystem** passes through the implemented filter.

`boolean filter (RemoteSystem remoteSystem)`

**Parameters**  
*remoteSystem* - the **RemoteSystem** object to check against the filter