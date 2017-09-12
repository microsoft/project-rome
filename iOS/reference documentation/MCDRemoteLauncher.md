# class `MCDRemoteLauncher` 

```
@interface MCDRemoteLauncher : NSObject
```  

A class used to launch an app on a remote device using a URI.

## Summary

 Members | Descriptions                                
----|---------
launchUri | Launches a URI against the Remote System specified in an [MCDRemoteSystemConnectionRequest](MCDRemoteSystemConnectionRequest.md).
launchUri | Launches a URI with options against the Remote System specified in an [MCDRemoteSystemConnectionRequest](MCDRemoteSystemConnectionRequest.md).

## Methods

### launchUri
`+(void)launchUri:(nonnull NSString*)uri withRequest:(nonnull MCDRemoteSystemConnectionRequest*)request withCompletion:(nullable void (^)(MCDRemoteLauncherUriStatus))completion;` 

Launches a URI against the Remote System specified in an [MCDRemoteSystemConnectionRequest](MCDRemoteSystemConnectionRequest.md).

#### Parameters
* `uri` The URI to launch.
* `request` The connection request.
* `completion` The block to invoke upon completion.

### launchUri
`+(void)launchUri:(nonnull NSString*)uri withRequest:(nonnull MCDRemoteSystemConnectionRequest*)request withOptions:(nonnull MCDRemoteLauncherOptions*)options withCompletion:(nonnull void (^)(MCDRemoteLauncherUriStatus))completion;`

Launches a URI with options against the Remote System specified in an [MCDRemoteSystemConnectionRequest](MCDRemoteSystemConnectionRequest.md).

#### Parameters
* `uri` The URI to launch.
* `request` The connection request.
* `options` The launcher options.
* `completion` The block to invoke upon completion.
