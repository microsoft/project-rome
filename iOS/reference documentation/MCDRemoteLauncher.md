# class `MCDRemoteLauncher` 

```
@interface MCDRemoteLauncher : NSObject
```  

A class used to launch an app on a remote device using a URI.

## Summary

 Members | Descriptions                                
----|---------
connectionRequest | The [MCDRemoteSystemConnectionRequest](MCDRemoteSystemConnectionRequest.md) associated with the target device.
initWithConnectionRequest | Initializes the [MCDRemoteLauncher](MCDRemoteLauncher.md) with a [MCDRemoteSystemConnectionRequest](MCDRemoteSystemConnectionRequest.md).
launchUri | Launches a URI against the Remote System specified in the previously initialized [MCDRemoteSystemConnectionRequest](MCDRemoteSystemConnectionRequest.md).
launchUri | Launches a URI with options against the Remote System specified in the previously initialized [MCDRemoteSystemConnectionRequest](MCDRemoteSystemConnectionRequest.md).
launchUri | Launches a URI with options and data against the Remote System specified in the previously initialized [MCDRemoteSystemConnectionRequest](MCDRemoteSystemConnectionRequest.md).

## Properties

### connectionRequest
`@property (nonatomic, readonly, strong, nonnull)MCDRemoteSystemConnectionRequest* connectionRequest;`

The [MCDRemoteSystemConnectionRequest](MCDRemoteSystemConnectionRequest.md) associated with the target device.

## Methods

### initWithConnectionRequest
`-(nullable instancetype)initWithConnectionRequest:(nonnull MCDRemoteSystemConnectionRequest*)request;`

Initializes the [MCDRemoteLauncher](MCDRemoteLauncher.md) with a [MCDRemoteSystemConnectionRequest](MCDRemoteSystemConnectionRequest.md).

#### Parameters
* `request` the connection request associated with the target device.

#### Returns
The initialized [MCDRemoteLauncher](MCDRemoteLauncher.md), otherwise nil.

### launchUri
`-(nullable NSError*)launchUri:(nonnull NSString*)uri withCompletion:(nullable void (^)(MCDRemoteLauncherUriStatus))completionBlock;` 

Launches a URI against the Remote System specified in the previously initialized [MCDRemoteSystemConnectionRequest](MCDRemoteSystemConnectionRequest.md).

#### Parameters
* `uri` The URI to launch.
* `withCompletion` The block to invoke upon completion.

#### Returns
An error, if any occurred. 

### launchUri
`-(nullable NSError*)launchUri:(nonnull NSString*)uri withOptions:(nonnull MCDRemoteLauncherOptions*)options withCompletion:(nonnull void (^)(MCDRemoteLauncherUriStatus));`

Launches a URI with options against the Remote System specified in the previously initialized [MCDRemoteSystemConnectionRequest](MCDRemoteSystemConnectionRequest.md).

#### Parameters
* `uri` The URI to launch.
* `withOptions` The launcher options.
* `withCompletion` The block to invoke upon completion.

#### Returns
An error, if any occurred.