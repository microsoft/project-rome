# class `CDRemoteSystemDiscoveryTypeFilter` 

```
@interface CDRemoteSystemDiscoveryTypeFilter : NSObject<CDRemoteSystemFilter>
```  

A class used to filter remote systems based upon discovery type.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
type | The discovery type to filter for.
initWithType | Initializes the [CDRemoteSystemDiscoveryTypeFilter](CDRemoteSystemDiscoveryTypeFilter.md) with a [CDRemoteSystemDiscoveryType](CDRemoteSystemDiscoveryType.md).

## Properties

### type
`@property (nonatomic, readonly)CDRemoteSystemDiscoveryType type;`

The discovery type to filter for.

## Methods

### initWithType
`-(nullable instancetype)initWithType:(CDRemoteSystemDiscoveryType)initType;`

Initializes the [CDRemoteSystemDiscoveryTypeFilter](CDRemoteSystemDiscoveryTypeFilter.md) with a [CDRemoteSystemDiscoveryType](CDRemoteSystemDiscoveryType.md).

#### Parameters
* `initType` The discovery type. 

#### Returns
The initialized [CDRemoteSystemDiscoveryTypeFilter](#CDRemoteSystemDiscoveryTypeFilter.md) if successful, otherwise nil.