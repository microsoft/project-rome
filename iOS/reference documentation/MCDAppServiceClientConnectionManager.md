# class `MCDAppServiceClientConnectionManager`

```
@interface MCDAppServiceClientConnectionManager : NSObject
```

A class used to communicate with a remote device using app services.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
delegate| The delegate that will receive events from this [MCDAppServiceClientConnectionManager](MCDAppServiceClientConnectionManager.md).
connectionRequest | The [MCDRemoteSystemConnectionRequest](MCDRemoteSystemConnectionRequest.md) associated with the target device.
appServiceName | The name of the app service on the target device.
appIdentifier | The ID of the app service on the target device. 
initWithConnectionRequest | Initializes the [MCDAppServiceClientConnectionManager](MCDAppServiceClientConnectionManager.md) with a connection request.
openRemote | Opens the connection to the app service.
sendMessage | Sends a message to the remote app service and begins listening for a response. Responses are handled by the [MCDAppServiceClientConnectionManagerDelegate](MCDAppServiceClientConnectionManagerDelegate.md) for this class. If the connection is over the cloud, the listener times out after 60 seconds.
close | Closes the connection to the app service.


## Properties

### delegate
`@property (nonatomic, readonly, weak, nullable)id<MCDAppServiceClientConnectionManagerDelegate> delegate;`

The delegate that will receive events from this [MCDAppServiceClientConnectionManager](MCDAppServiceClientConnectionManager.md).

### connectionRequest 
`@property (nonatomic, readonly, strong, nonnull)MCDRemoteSystemConnectionRequest* connectionRequest;`

The [MCDRemoteSystemConnectionRequest](MCDRemoteSystemConnectionRequest.md) associated with the target device.

### appServiceName 
`@property (nonatomic, readonly, copy, nonnull) NSString* appServiceName;`

The name of the app service on the target device.

### appIdentifier
`@property (nonatomic, readonly, copy, nonnull) NSString* appIdentifier;`

The ID of the app service on the target device. 

## Methods

### initWithConnectionRequest 
`-(nullable instancetype)initWithConnectionRequest:(nonnull MCDRemoteSystemConnectionRequest*)request appServiceName:(nonnull NSString*)appServiceName appIdentifier:(nonnull NSString*)appIdentifier delegate:(nonnull id<MCDAppServiceClientConnectionManagerDelegate>)delegate;`

Initializes the [MCDAppServiceClientConnectionManager](MCDAppServiceClientConnectionManager.md) with a connection request.

#### Parameters
* `request` The [MCDRemoteSystemConnectionRequest](MCDRemoteSystemConnectionRequest.md) associated with the target device.
* `appServiceName` The name of the app service on the target device.
* `appIdentifier` The ID of the app service on the target device. 
* `delegate` The delegate that will receive events from this [MCDAppServiceClientConnectionManager](MCDAppServiceClientConnectionManager.md).

#### Returns
The initialized [MCDAppServiceClientConnectionManager](MCDAppServiceClientConnectionManager.md) if successful, otherwise nil.

### openRemote
`-(void)openRemote;`

Opens the connection to the app service.

### sendMessage
`-(void)sendMessage:(nonnull NSDictionary*)dictionary;`

Sends a message to the remote app service and begins listening for a response. Responses are handled by the [MCDAppServiceClientConnectionManagerDelegate](MCDAppServiceClientConnectionManagerDelegate.md) for this class. If the connection is over the cloud, the listener times out after 60 seconds.

#### Parameters
* `dictionary` The key-value set of data to be sent to the app service.

### close
`-(nullable NSError*)close;`

Closes the connection to the app service.