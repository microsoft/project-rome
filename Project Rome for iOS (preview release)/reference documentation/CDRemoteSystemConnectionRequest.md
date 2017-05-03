# class `CDRemoteSystemConnectionRequest` 

```
class CDRemoteSystemConnectionRequest
  : public NSObject
```  

A class used to express a connection request intent against a remote system.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
remoteSystem | The remote system to connect to.
initWithRemoteSystem | Initializes the [CDRemoteSystemConnectionRequest](#interface_c_d_remote_system_connection_request) with a [RemoteSystem]( ) instance.

## Properties

### remoteSystem
`@property (nonatomic, readonly, strong, nonnull)CDRemoteSystem* remoteSystem;`

The remote system to connect to.

## Methods

### initWithRemoteSystem
`-(nullable instancetype)initWithRemoteSystem:(nonnull CDRemoteSystem*)remoteSystem;`

Initializes the [CDRemoteSystemConnectionRequest](#interface_c_d_remote_system_connection_request) with a [RemoteSystem]( ) instance.