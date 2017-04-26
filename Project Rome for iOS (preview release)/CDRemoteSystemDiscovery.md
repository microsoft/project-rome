# class `CDRemoteSystemDiscovery` 

```
class CDRemoteSystemDiscovery
  : public NSObject
```  

A class used to find Remote Systems.



## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
`public virtual nullable instancetype initWithDiscoveryFilters:(nullable NSSet * filters)` | Initializes the CDPRemoteSystemDiscovery with a set of filters.  filters The set of filters.
`public virtual nullable NSError * start()` | Attempts to find RemoteSystems.
`public virtual nullable NSError * stop()` | Stops the active discovery.
`public virtual nullable NSError * findByHostName:(nonnull NSString * hostname)` | Attempts to find a PeerSystem proximaly by its IP.

## Members

#### `public virtual nullable instancetype initWithDiscoveryFilters:(nullable NSSet * filters)` 

Initializes the CDPRemoteSystemDiscovery with a set of filters.  filters The set of filters.

#### Returns
The initialized [CDRemoteSystemDiscovery](#interface_c_d_remote_system_discovery), otherwise nil.

#### `public virtual nullable NSError * start()` 

Attempts to find RemoteSystems.

#### Returns
An error describing why the discovery could not be initiated, otherwise nil.

#### `public virtual nullable NSError * stop()` 

Stops the active discovery.

#### Returns
An error describing why the discovery could not be stopped, otherwise nil.

#### `public virtual nullable NSError * findByHostName:(nonnull NSString * hostname)` 

Attempts to find a PeerSystem proximaly by its IP.

#### Returns
An error describing why the discovery could not be initiated, otherwise nil.