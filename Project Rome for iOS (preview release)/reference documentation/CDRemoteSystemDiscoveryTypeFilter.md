# class `CDRemoteSystemDiscoveryTypeFilter` 

```
@interface CDRemoteSystemDiscoveryTypeFilter : NSObject<CDRemoteSystemFilter>
```  

A class used to filter remote systems based upon discovery type.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
type | The discovery type to filter for.
initWithType | Initializes the [CDRemoteSystemDiscoveryTypeFilter]( ) with a discovery type.

## Properties

### type
`@property (nonatomic, readonly)CDRemoteSystemDiscoveryType type;`

The discovery type to filter for.

## Methods

### initWithType
`-(nullable instancetype)initWithType:(CDRemoteSystemDiscoveryType)initType;`

Initializes the [CDRemoteSystemDiscoveryTypeFilter]( ) with a discovery type.

#### Parameters
* `initType` The discovery type. 

#### Returns
The initialized [CDRemoteSystemDiscoveryTypeFilter](#interface_c_d_remote_system_discovery_type_filter) if successful, otherwise nil.