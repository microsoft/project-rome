# class `LauncherOptions` 

```
@interface LauncherOptions : NSObject
```  

A class describing a set of options for a launch operation.



## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
targetPeerSystem | The PeerSystem to target with this operation.
location | The screen location to lauch the application to.

## Members

### targetPeerSystem
`@property (nonatomic, strong) PeerSystem* targetPeerSystem;`

The PeerSystem to target with this operation.

### location
`@property AppLocation location;`

The screen location to lauch the application to.