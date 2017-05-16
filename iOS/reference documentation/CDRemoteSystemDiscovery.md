# class `CDRemoteSystemDiscovery` 

```
@interface CDRemoteSystemDiscovery : NSObject
```  

A class used to discover remote systems.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
delegate | The delegate that will receive events from this [CDRemoteSystemDiscovery](CDRemoteSystemDiscovery.md).
initWithDiscoveryFilters | Initializes the [CDRemoteSystemDiscovery](CDRemoteSystemDiscovery.md) with a set of filters.
start | Begins discovering remote systems.
stop | Stops the active discovery.
findByHostName | Attempts to find a system proximally using its IP address.

## Properties

### delegate
`@property (nonatomic, readwrite, weak, nullable)id<CDRemoteSystemDiscoveryDelegate> delegate;`

The delegate that will receive events from this [CDRemoteSystemDiscovery](CDRemoteSystemDiscovery.md).

## Methods

### initWithDiscoveryFilters
`-(nullable instancetype)initWithDiscoveryFilters:(nullable NSSet*)filters;` 

Initializes the [CDRemoteSystemDiscovery](CDRemoteSystemDiscovery.md) with a set of filters.

#### Returns
The initialized [CDRemoteSystemDiscovery](CDRemoteSystemDiscovery.md), otherwise nil.

### start
`-(nullable NSError*)start;` 

Begins discovering remote systems.

#### Returns
An error describing why the discovery could not be initiated, otherwise nil.

### stop
`(nullable NSError*)stop;` 

Stops the active discovery.

#### Returns
An error describing why the discovery could not be stopped, otherwise nil.

### findByHostName
`-(nullable NSError*)findByHostName:(nonnull NSString*)hostname;` 

Attempts to find a system proximally using its IP address.

#### Returns
An error describing why the discovery could not be initiated, otherwise nil.