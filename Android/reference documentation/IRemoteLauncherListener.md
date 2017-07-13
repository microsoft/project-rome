# IRemoteLauncherListener interface
Interface to handle the completion of a remote app launch.

## Syntax
`public interface IRemoteLauncherListener` 

## Public methods

### onCompleted
Called when the remote URI launch has completed.

`void onCompleted(RemoteLauncherUriStatus status)`

#### Parameters  
*status* - the status of the remote launch