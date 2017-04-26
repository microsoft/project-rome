# class `CDRemoteLauncher` 

```
class CDRemoteLauncher
  : public NSObject
```  

A class used to find RemoteSystems.



## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
`public virtual (unavailable("init not available. Please use initWithConnectionRequest." __attribute__()` | 
`public virtual nullable instancetype initWithConnectionRequest:(nonnull `[`CDRemoteSystemConnectionRequest`](#interface_c_d_remote_system_connection_request)` * request)` | Initializes the [CDRemoteLauncher](#interface_c_d_remote_launcher) with a [CDRemoteSystemConnectionRequest](#interface_c_d_remote_system_connection_request).
`public virtual nullable NSError * launchUri:withCompletion:(nonnull NSString * uri,nullable void(^)(CDRemoteLauncherUriStatus) completionBlock)` | Launches a URI against the Remote System specified in the previously initialized [CDRemoteSystemConnectionRequest](#interface_c_d_remote_system_connection_request).  uri The URI to launch.
`public virtual nullable NSError * launchUri:withOptions:withCompletion:(nonnull NSString * uri,nonnull `[`CDRemoteLauncherOptions`](#interface_c_d_remote_launcher_options)` * options,nonnull void(^)(CDRemoteLauncherUriStatus) completionBlock)` | Initializes the [CDRemoteLauncher](#interface_c_d_remote_launcher) with a [CDRemoteSystemConnectionRequest](#interface_c_d_remote_system_connection_request).

## Members

#### `public virtual (unavailable("init not available. Please use initWithConnectionRequest." __attribute__()` 





#### `public virtual nullable instancetype initWithConnectionRequest:(nonnull `[`CDRemoteSystemConnectionRequest`](#interface_c_d_remote_system_connection_request)` * request)` 

Initializes the [CDRemoteLauncher](#interface_c_d_remote_launcher) with a [CDRemoteSystemConnectionRequest](#interface_c_d_remote_system_connection_request).

#### Returns
The initialized [CDRemoteLauncher](#interface_c_d_remote_launcher), otherwise nil.

#### `public virtual nullable NSError * launchUri:withCompletion:(nonnull NSString * uri,nullable void(^)(CDRemoteLauncherUriStatus) completionBlock)` 

Launches a URI against the Remote System specified in the previously initialized [CDRemoteSystemConnectionRequest](#interface_c_d_remote_system_connection_request).  uri The URI to launch.

#### Parameters
* `withCompletion` The block to invoke when the async request either succeeds or fails. 





#### Returns
The initialized [CDRemoteLauncher](#interface_c_d_remote_launcher), otherwise nil.

#### `public virtual nullable NSError * launchUri:withOptions:withCompletion:(nonnull NSString * uri,nonnull `[`CDRemoteLauncherOptions`](#interface_c_d_remote_launcher_options)` * options,nonnull void(^)(CDRemoteLauncherUriStatus) completionBlock)` 

Initializes the [CDRemoteLauncher](#interface_c_d_remote_launcher) with a [CDRemoteSystemConnectionRequest](#interface_c_d_remote_system_connection_request).

#### Parameters
* `options` The launcher options. 


* `withCompletion` The block to invoke when the async request either succeeds or fails. 





#### Returns
The initialized [CDRemoteLauncher](#interface_c_d_remote_launcher), otherwise nil.