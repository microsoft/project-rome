# class `MCDRemoteSystemStatusTypeFilter`

```
@interface MCDRemoteSystemStatusTypeFilter : NSObject<MCDRemoteSystemFilter>
```

A class used to filter remote systems based on availability status type.

## Summary

|Members       | Descriptions      | 
|---------------|-----------------|
|type | The remote system status type of this filter instance.|
|initWithStatusType | Initializes the [MCDRemoteSystemStatusTypeFilter](MCDRemoteSystemStatusTypeFilter.md) with a [MCDRemoteSystemStatusType](MCDRemoteSystemStatusType.md).|

## Properties

### type
`@property (nonatomic, readonly)MCDRemoteSystemStatusType type;`

The remote system status type of this filter instance.

## Methods

### initWithStatusType
`-(nullable instancetype)initWithStatusType:(MCDRemoteSystemStatusType)statusType;`

Initializes the [MCDRemoteSystemStatusTypeFilter](MCDRemoteSystemStatusTypeFilter.md) with a [MCDRemoteSystemStatusType](MCDRemoteSystemStatusType.md).

#### Parameters
* `statusType` The remote system status type.