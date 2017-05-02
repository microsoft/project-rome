# class `CDRemoteLauncher` 

```
class CDRemoteLauncher
  : public NSObject
```  

A class used to find remote systems.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
connectionRequest | The CDRemoteSystemConnectionRequest.
initWithConnectionRequest | Initializes the [CDRemoteLauncher](#interface_c_d_remote_launcher) with a [CDRemoteSystemConnectionRequest](#interface_c_d_remote_system_connection_request).
launchUri | Launches a URI against the Remote System specified in the previously initialized [CDRemoteSystemConnectionRequest](#interface_c_d_remote_system_connection_request).  uri The URI to launch.
launchUri | Launches a URI with options against the Remote System specified in the previously initialized [CDRemoteSystemConnectionRequest](#interface_c_d_remote_system_connection_request).

## Properties

### connectionRequest
`@property (nonatomic, readonly, strong, nonnull)CDRemoteSystemConnectionRequest* connectionRequest;`

The CDRemoteSystemConnectionRequest.

## Methods

### initWithConnectionRequest
`-(nullable instancetype)initWithConnectionRequest:(nonnull CDRemoteSystemConnectionRequest*)request;`

Initializes the [CDRemoteLauncher](#interface_c_d_remote_launcher) with a [CDRemoteSystemConnectionRequest](#interface_c_d_remote_system_connection_request).

#### Returns
The initialized [CDRemoteLauncher](#interface_c_d_remote_launcher), otherwise nil.

### launchUri
`-(nullable NSError*)launchUri:(nonnull NSString*)uri withCompletion:(nullable void (^)(CDRemoteLauncherUriStatus))completionBlock;` 

Launches a URI against the Remote System specified in the previously initialized [CDRemoteSystemConnectionRequest](#interface_c_d_remote_system_connection_request).

#### Parameters
* `uri` The URI to launch.
* `completionBlock` The block to invoke when the async request either succeeds or fails. 

#### Returns
An error, if any occurred. 

### launchUri
`-(nullable NSError*)launchUri:(nonnull NSString*)uri withOptions:(nonnull CDRemoteLauncherOptions*)options withCompletion:(nonnull void (^)(CDRemoteLauncherUriStatus))completionBlock;` 

Launches a URI with options against the Remote System specified in the previously initialized [CDRemoteSystemConnectionRequest](#interface_c_d_remote_system_connection_request).

#### Parameters
* `uri` The URI to launch.
* `options` The launcher options. 
* `completionBlock` The block to invoke when the async request either succeeds or fails. 

#### Returns
An error, if any occurred. 