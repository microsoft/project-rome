# class `CDRemoteSystemKindFilter` 

```
@interface CDRemoteSystemKindFilter : NSObject<CDRemoteSystemFilter>
```  

A class used to filter remote systems based upon device kind.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
addKind | Adds a device type to the set of types allowed by this filter.

## Methods

### addKind
`public virtual void addKind:(nonnull NSString * initKind)`

Adds a device type to the set of types allowed by this filter.

#### Parameters
* `initKind` A string representing the device kind to add to the filter. This should be one of the values held by the [CDRemoteSystemKind](CDRemoteSystemKind.md) struct.
