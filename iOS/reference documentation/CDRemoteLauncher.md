# class `CDRemoteLauncher` 

```
@interface CDRemoteLauncher : NSObject
```  

A class used to find remote systems.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
connectionRequest | The CDRemoteSystemConnectionRequest.
initWithConnectionRequest | Initializes the [CDRemoteLauncher](CDRemoteLauncher.md) with a [CDRemoteSystemConnectionRequest](CDRemoteSystemConnectionRequest.md).
launchUri | Launches a URI against the Remote System specified in the previously initialized [CDRemoteSystemConnectionRequest](CDRemoteSystemConnectionRequest.md).  uri The URI to launch.
launchUri | Launches a URI with options against the Remote System specified in the previously initialized [CDRemoteSystemConnectionRequest](CDRemoteSystemConnectionRequest.md).

## Properties

### connectionRequest
`@property (nonatomic, readonly, strong, nonnull)CDRemoteSystemConnectionRequest* connectionRequest;`

The CDRemoteSystemConnectionRequest.

## Methods

### initWithConnectionRequest
`-(nullable instancetype)initWithConnectionRequest:(nonnull CDRemoteSystemConnectionRequest*)request;`

Initializes the [CDRemoteLauncher](CDRemoteLauncher.md) with a [CDRemoteSystemConnectionRequest](CDRemoteSystemConnectionRequest.md).

#### Returns
The initialized [CDRemoteLauncher](CDRemoteLauncher.md), otherwise nil.

### launchUri
`-(nullable NSError*)launchUri:(nonnull NSString*)uri withCompletion:(nullable void (^)(CDRemoteLauncherUriStatus))completionBlock;` 

Launches a URI against the Remote System specified in the previously initialized [CDRemoteSystemConnectionRequest](CDRemoteSystemConnectionRequest.md).

#### Parameters
* `uri` The URI to launch.
* `completionBlock` The block to invoke when the async request either succeeds or fails. 

#### Returns
An error, if any occurred. 

### launchUri
`-(nullable NSError*)launchUri:(nonnull NSString*)uri withOptions:(nonnull CDRemoteLauncherOptions*)options withCompletion:(nonnull void (^)(CDRemoteLauncherUriStatus))completionBlock;` 

Launches a URI with options against the Remote System specified in the previously initialized [CDRemoteSystemConnectionRequest](#CDRemoteSystemConnectionRequest.md).

#### Parameters
* `uri` The URI to launch.
* `options` The launcher options. 
* `completionBlock` The block to invoke when the async request either succeeds or fails. 

#### Returns
An error, if any occurred. 