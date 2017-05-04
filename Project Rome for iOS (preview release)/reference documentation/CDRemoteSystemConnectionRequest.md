# class `CDRemoteSystemConnectionRequest` 

```
@interface CDRemoteSystemConnectionRequest : NSObject
```  

A class used to express a connection request intent against a remote system.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
remoteSystem | The remote system to connect to.
initWithRemoteSystem | Initializes the [CDRemoteSystemConnectionRequest](CDRemoteSystemConnectionRequest.md) with a [CDRemoteSystem](CDRemoteSystem.md) instance.

## Properties

### remoteSystem
`@property (nonatomic, readonly, strong, nonnull)CDRemoteSystem* remoteSystem;`

The remote system to connect to.

## Methods

### initWithRemoteSystem
`-(nullable instancetype)initWithRemoteSystem:(nonnull CDRemoteSystem*)remoteSystem;`

Initializes the [CDRemoteSystemConnectionRequest](CDRemoteSystemConnectionRequest.md) with a [CDRemoteSystem](CDRemoteSystem.md) instance.