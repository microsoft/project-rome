# protocol `CDRemoteSystemDiscoveryDelegate`

```
@protocol CDRemoteSystemDiscoveryDelegate <NSObject>
```

Set of methods to be implemented by objects acting as delegates for the [CDRemoteSystemDiscovery]( ) class.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
remoteSystemDiscoveryFound | Called when a remote system has been discovered.
remoteSystemDiscoveryRemoved | Called when a previously discovered remote system has been removed.
remoteSystemDiscoveryUpdated | Called when a previously discovered remote system has been updated.
remoteSystemDiscoveryCompleted | Called when the discovery operation has completed successfully.

## methods

### remoteSystemDiscoveryFound
`@optional
-(void)remoteSystemDiscoveryFound:(nonnull CDRemoteSystemDiscovery*)discovery remoteSystem:(nonnull CDRemoteSystem*)remoteSystem;`

Called when a remote system has been discovered.

#### Parameters
* `discovery` The delegating [CDRemoteSystemDiscovery]( ) instance.
* `remoteSystem` The discovered [RemoteSystem]( ) instance.

### remoteSystemDiscoveryRemoved
`@optional
-(void)remoteSystemDiscoveryRemoved:(nonnull CDRemoteSystemDiscovery*)discovery remoteSystem:(nonnull CDRemoteSystem*)remoteSystem;`

Called when a previously discovered remote system has been removed.

#### Parameters
* `discovery` The delegating [CDRemoteSystemDiscovery]( ) instance.
* `remoteSystem` The removed [RemoteSystem]( ) instance.

### remoteSystemDiscoveryRemoved
`@optional
-(void)remoteSystemDiscoveryUpdated:(nonnull CDRemoteSystemDiscovery*)discovery remoteSystem:(nonnull CDRemoteSystem*)remoteSystem;`

Called when a previously discovered remote system has been updated.

#### Parameters
* `discovery` The delegating [CDRemoteSystemDiscovery]( ) instance.
* `remoteSystem` The updated [RemoteSystem]( ) instance.


### remoteSystemDiscoveryCompleted
`@optional
-(void)remoteSystemDiscoveryCompleted:(nonnull CDRemoteSystemDiscovery*)discovery;`

Called when the discovery operation has completed successfully.

#### Parameters
* `discovery` The delegating [CDRemoteSystemDiscovery]( ) instance.

