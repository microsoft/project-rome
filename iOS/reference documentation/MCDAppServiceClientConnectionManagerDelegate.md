# protocol `MCDAppServiceClientConnectionManagerDelegate`

```
@protocol MCDAppServiceClientConnectionManagerDelegate <NSObject>
```

Set of methods to be implemented by objects acting as delegates for the [MCDAppServiceClientConnectionManager](MCDAppServiceClientConnectionManager.md) class.

## Summary

Members     | Descriptions
----- | -----
appServiceClientConnectionManagerDidOpen | Called when a connection to an app service has been established.
appServiceClientConnectionManager:didFail | Called when there was a failure communicating with the Remote System.
appServiceClientConnectionManager:didClose | Called when a connection to a Remote System has closed.
appServiceClientConnectionManager:didReceiveResponse | Called when the client has received an app service message from the remote system.

## Methods

### appServiceClientConnectionManagerDidOpen
`@optional
-(void)appServiceClientConnectionManagerDidOpen:(nonnull MCDAppServiceClientConnectionManager*)manager;`

Called when a connection to an app service has been established.

#### Parameters
* `manager` The delegating [MCDAppServiceClientConnectionManager](MCDAppServiceClientConnectionManager.md)

### appServiceClientConnectionManager:didFail
`@optional
-(void)appServiceClientConnectionManager:(nonnull MCDAppServiceClientConnectionManager*)manager didFail:(MCDAppServiceClientConnectionStatus)status;`

Called when there was a failure communicating with the Remote System.

#### Parameters
* `manager` The delegating [MCDAppServiceClientConnectionManager](MCDAppServiceClientConnectionManager.md)
* `status` The current connection status.

### appServiceClientConnectionManager:didClose
`@optional
-(void)appServiceClientConnectionManager:(nonnull MCDAppServiceClientConnectionManager*)manager didClose:(MCDAppServiceClientClosedStatus)status;`

Called when a connection to a Remote System has closed.

#### Parameters
* `manager` The delegating [MCDAppServiceClientConnectionManager](MCDAppServiceClientConnectionManager.md)
* `status` The connection closure status.

### appServiceClientConnectionManager:didReceiveResponse
`@optional
-(void)appServiceClientConnectionManager:(nonnull MCDAppServiceClientConnectionManager*)manager didReceiveResponse:(nonnull MCDAppServiceClientResponse*)response;`

Called when the client has received an app service message from the remote system.

#### Parameters
* `manager` The delegating [MCDAppServiceClientConnectionManager](MCDAppServiceClientConnectionManager.md)
* `response` The incoming message.