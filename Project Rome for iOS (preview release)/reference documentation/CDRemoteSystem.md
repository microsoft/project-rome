# class `CDRemoteSystem` 

```
@interface CDRemoteSystem : NSObject
```  

A class to represent a remote system.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
id | The identifier for this remote system.
displayName | The friendly display name of this remote system.
kind | The device type of this remote system.
isAvailableByProximity | Indicates whether the remote system can be reached by proximal connection (UDP or Bluetooth).
status | The availability of the remote system.

## Properties

### id
`@property (nonatomic, readonly, copy, nonnull) NSString* id;`

The identifier for this remote system.

### displayName
`@property (nonatomic, readonly, copy, nonnull) NSString* displayName;`

The friendly display name of this remote system.

### kind
`@property (nonatomic, readonly, copy, nonnull) NSString* kind;`

The device type of this remote system.

### isAvailableByProximity
`@property (nonatomic, readonly) BOOL isAvailableByProximity;`
Indicates whether the remote system can be reached by proximal connection (UDP or Bluetooth).

### status
`@property (nonatomic, readonly) CDRemoteSystemStatus status;`
The availability of the remote system.