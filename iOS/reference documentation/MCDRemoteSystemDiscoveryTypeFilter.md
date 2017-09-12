# class `MCDRemoteSystemDiscoveryTypeFilter` 

```
@interface MCDRemoteSystemDiscoveryTypeFilter : NSObject<MCDRemoteSystemFilter>
```  

A class used to filter remote systems based upon discovery type.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
type | The discovery type to filter for.
initWithType | Initializes the [MCDRemoteSystemDiscoveryTypeFilter](MCDRemoteSystemDiscoveryTypeFilter.md) with a [MCDRemoteSystemDiscoveryType](MCDRemoteSystemDiscoveryType.md).

## Properties

### type
`@property (nonatomic, readonly)MCDRemoteSystemDiscoveryType type;`

The discovery type to filter for.

## Methods

### initWithType
`-(nullable instancetype)initWithType:(MCDRemoteSystemDiscoveryType)initType;`

Initializes the [MCDRemoteSystemDiscoveryTypeFilter](MCDRemoteSystemDiscoveryTypeFilter.md) with a [MCDRemoteSystemDiscoveryType](MCDRemoteSystemDiscoveryType.md).

#### Parameters
* `initType` The discovery type. 

#### Returns
The initialized [MCDRemoteSystemDiscoveryTypeFilter](MCDRemoteSystemDiscoveryTypeFilter.md) if successful, otherwise nil.