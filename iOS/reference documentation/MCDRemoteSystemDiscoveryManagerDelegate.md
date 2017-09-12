# protocol `MCDRemoteSystemDiscoveryManagerDelegate`

```
@protocol MCDRemoteSystemDiscoveryManagerDelegate <NSObject>
```

Set of methods to be implemented by objects acting as delegates for the [MCDRemoteSystemDiscoveryManager](MCDRemoteSystemDiscoveryManager.md) class.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
remoteSystemDiscoveryManager:didFind | Called when a remote system has been discovered.
remoteSystemDiscoveryManager:didRemove | Called when a previously discovered remote system has been removed.
remoteSystemDiscoveryManager:didUpdate | Called when a previously discovered remote system has been updated.
remoteSystemDiscoveryManagerDidComplete | Called when the discovery operation has completed successfully.

## Methods

### remoteSystemDiscoveryManager:didFind
`@optional
-(void)remoteSystemDiscoveryManager:(nonnull MCDRemoteSystemDiscoveryManager*)discoveryManager didFind:(nonnull MCDRemoteSystem*)remoteSystem;`

Called when a remote system has been discovered.

#### Parameters
* `discoveryManager` The delegating [MCDRemoteSystemDiscoveryManager](MCDRemoteSystemDiscoveryManager.md) instance.
* `didFind` The discovered [MCDRemoteSystem](MCDRemoteSystem.md) instance.

### remoteSystemDiscoveryManager:didRemove
`@optional
-(void)remoteSystemDiscoveryManager:(nonnull MCDRemoteSystemDiscoveryManager*)discoveryManager didRemove:(nonnull MCDRemoteSystem*)remoteSystem;`

Called when a previously discovered remote system has been removed.

#### Parameters
* `discoveryManager` The delegating [MCDRemoteSystemDiscoveryManager](MCDRemoteSystemDiscoveryManager.md) instance.
* `didRemove` The removed [MCDRemoteSystem](MCDRemoteSystem.md) instance.

### remoteSystemDiscoveryManager:didUpdate
`@optional
-(void)remoteSystemDiscoveryManager:(nonnull MCDRemoteSystemDiscoveryManager*)discoveryManager didUpdate:(nonnull MCDRemoteSystem*)remoteSystem;`

Called when a previously discovered remote system has been updated.

#### Parameters
* `discoveryManager` The delegating [MCDRemoteSystemDiscoveryManager](MCDRemoteSystemDiscoveryManager.md) instance.
* `didUpdate` The updated [MCDRemoteSystem](MCDRemoteSystem.md) instance.


### remoteSystemDiscoveryManagerDidComplete
`@optional
-(void)remoteSystemDiscoveryManagerDidComplete:(nonnull MCDRemoteSystemDiscoveryManager*)discoveryManager withError:(nullable NSError*)error;`

Called when the discovery operation has completed.

#### Parameters
* `discoveryManager` The delegating [MCDRemoteSystemDiscoveryManager](MCDRemoteSystemDiscoveryManager.md) instance.
* `withError` Nil on success, otherwise an error indicating why discovery failed.