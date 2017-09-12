# class `MCDAppServiceClientConnectionManager`

```
@interface MCDAppServiceClientConnectionManager : NSObject
```

A class used to communicate with a remote device using app services. See [Create and consume an app service](https://docs.microsoft.com/windows/uwp/launch-resume/how-to-create-and-consume-an-app-service) for information on app services on Windows devices.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
delegate| The delegate that will receive events from this [MCDAppServiceClientConnectionManager](MCDAppServiceClientConnectionManager.md).
connectionRequest | The [MCDRemoteSystemConnectionRequest](MCDRemoteSystemConnectionRequest.md) associated with the target device.
appServiceName | The name of the app service on the target device. See [Create and consume an app service](https://docs.microsoft.com/windows/uwp/launch-resume/how-to-create-and-consume-an-app-service) for details. 
appIdentifier | The package family name of the remote app service. See [Create and consume an app service](https://docs.microsoft.com/windows/uwp/launch-resume/how-to-create-and-consume-an-app-service) for details.  
initWithConnectionRequest | Initializes the [MCDAppServiceClientConnectionManager](MCDAppServiceClientConnectionManager.md) with connection request details. The class instance will not request a connection until **openRemote** is called.
openRemote | Attempts to open a connection to the remote device. Successful connections will invoke the **appServiceClientConnectionManagerDidOpen** method of the delegate.
sendMessage | Sends a message to the remote app service and begins listening for a response. Responses are handled by the [MCDAppServiceClientConnectionManagerDelegate](MCDAppServiceClientConnectionManagerDelegate.md) for this class.
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

The name of the app service on the target device. See [Create and consume an app service](https://docs.microsoft.com/windows/uwp/launch-resume/how-to-create-and-consume-an-app-service) for details.  

### appIdentifier
`@property (nonatomic, readonly, copy, nonnull) NSString* appIdentifier;`

The package family name of the remote app service. See [Create and consume an app service](https://docs.microsoft.com/windows/uwp/launch-resume/how-to-create-and-consume-an-app-service) for details.  

## Methods

### initWithConnectionRequest 
`-(nullable instancetype)initWithConnectionRequest:(nonnull MCDRemoteSystemConnectionRequest*)request appServiceName:(nonnull NSString*)appServiceName appIdentifier:(nonnull NSString*)appIdentifier delegate:(nonnull id<MCDAppServiceClientConnectionManagerDelegate>)delegate;`

Initializes the [MCDAppServiceClientConnectionManager](MCDAppServiceClientConnectionManager.md) with connection request details. The class instance will not request a connection until **openRemote** is called.

#### Parameters
* `request` The [MCDRemoteSystemConnectionRequest](MCDRemoteSystemConnectionRequest.md) associated with the target device.
* `appServiceName` The name of the app service on the target device.
* `appIdentifier` The ID of the app service on the target device. 
* `delegate` The delegate that will receive events from this [MCDAppServiceClientConnectionManager](MCDAppServiceClientConnectionManager.md).

#### Returns
The initialized [MCDAppServiceClientConnectionManager](MCDAppServiceClientConnectionManager.md) if successful, otherwise nil.

### openRemote
`-(void)openRemote;`

Attempts to open a connection to the remote device. Successful connections will invoke the **appServiceClientConnectionManagerDidOpen** method of the delegate.

### sendMessage
`-(void)sendMessage:(nonnull NSDictionary*)dictionary;`

Sends a message to the remote app service and begins listening for a response. Responses are handled by the [MCDAppServiceClientConnectionManagerDelegate](MCDAppServiceClientConnectionManagerDelegate.md) for this class. This method should only be called after the connection was opened successfully.

#### Parameters
* `dictionary` The key-value set of data to be sent to the app service.

### close
`-(nullable NSError*)close;`

Closes the connection to the app service.