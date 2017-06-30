# class `MCDRemoteSystemDiscoveryManager` 

```
@interface MCDRemoteSystemDiscoveryManager : NSObject
```  

A class used to discover remote systems.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
delegate | The delegate that will receive events from this [MCDRemoteSystemDiscoveryManager](MCDRemoteSystemDiscoveryManager.md).
initWithDelegate | Initializes the [MCDRemoteSystemDiscoveryManager](MCDRemoteSystemDiscoveryManager.md) with a delegate.
initWithDelegate:withFilters | Initializes the [MCDRemoteSystemDiscoveryManager](MCDRemoteSystemDiscoveryManager.md) with a delegate and a set of discovery filters.
startDiscovery | Begins discovering remote systems.
stop | Stops the active discovery.
StartWithHostName | Attempts to find a system proximally using its IP address.

## Properties

### delegate
`@property (nonatomic, readonly weak, nonnull) id< MCDRemoteSystemDiscoveryManagerDelegate>* delegate;`

The delegate that will receive events from this [MCDRemoteSystemDiscoveryManager](MCDRemoteSystemDiscoveryManager.md).

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
* `filters` The set of discovery filters to apply to remote system discovery.

#### Returns
The initialized [MCDRemoteSystemDiscoveryManager](MCDRemoteSystemDiscoveryManager.md) if successful, otherwise nil.

### startDiscovery
`-(nullable NSError*)startDiscovery;` 

Begins discovering remote systems.

#### Returns
An error describing why the discovery could not be initiated, otherwise nil.

### stopDiscovery
`(nullable NSError*)stopDiscovery;` 

Stops the active discovery.

#### Returns
An error describing why the discovery could not be stopped, otherwise nil.

### startDiscoveryWithHostName
`-(nullable NSError*) startWithHostName:` 

Attempts to find a system proximally using its IP address.

#### Returns
An error describing why the discovery could not be initiated, otherwise nil.