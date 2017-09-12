# class `MCDRemoteSystemDiscoveryManager` 

```
@interface MCDRemoteSystemDiscoveryManager : NSObject
```  

A class used to discover remote systems.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
delegate | The delegate to receive events that result from starting a discovery.
initWithDelegate | Initializes the [MCDRemoteSystemDiscoveryManager](MCDRemoteSystemDiscoveryManager.md) with a delegate.
initWithDelegate:withFilters | Initializes the [MCDRemoteSystemDiscoveryManager](MCDRemoteSystemDiscoveryManager.md) with a delegate and a set of discovery filters.
startDiscovery | Begins discovering remote systems. It will cause an existing discovery to restart.
stop | Stops the active discovery.
startDiscoveryWithHostName | Attempts to find a system proximally using its IP address.

## Properties

### delegate
`@property (nonatomic, readonly, weak, nullable) id<MCDRemoteSystemDiscoveryManagerDelegate> delegate;`

The delegate to receive events that result from starting a discovery.

## Methods

### initWithDelegate
`-(nullable instancetype)initWithDelegate:(id<MCDRemoteSystemDiscoveryManagerDelegate>):delegate filters:(NSSet*)filters;`

Initializes the [MCDRemoteSystemDiscoveryManager](MCDRemoteSystemDiscoveryManager.md) with a delegate.

#### Parameters
* `delegate` The delegate to use for initialization.

#### Returns
The initialized [MCDRemoteSystemDiscoveryManager](MCDRemoteSystemDiscoveryManager.md) if successful, otherwise nil.

### initWithDelegate:withFilters
`-(nullable instancetype)initWithDelegate:(nonnull id<MCDRemoteSystemDiscoveryManagerDelegate>)delegate withFilters:(nonnull NSSet*)filters;`

Initializes the [MCDRemoteSystemDiscoveryManager](MCDRemoteSystemDiscoveryManager.md) with a delegate and a set of discovery filters.

#### Parameters
* `delegate` The delegate to use for initialization.
* `filters` The set of discovery filters (implementing the **[MCDRemoteSystemFilter](MCDRemoteSystemFilter.md)** interface) to apply to remote system discovery. 

#### Returns
The initialized [MCDRemoteSystemDiscoveryManager](MCDRemoteSystemDiscoveryManager.md) if successful, otherwise nil.

### startDiscovery
`-(void)startDiscovery;` 

Begins discovering remote systems. It will cause an existing discovery to restart.

### stopDiscovery
`(void)stopDiscovery;` 

Stops the active discovery.

### startDiscoveryWithHostName
`-(void)startDiscoveryWithHostName:(nonnull NSString*)hostname;` 

Attempts to find a system proximally using its IP address.
