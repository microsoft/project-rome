# class `MCDAppServiceClientResponse`

```
@interface MCDAppServiceClientResponse : NSObject
```

A class that represents a response received from a connected remote app service.

## Summary

Members                        | Descriptions                                
--------------------------------|---------------------------------------------
message | The app service message received.
status | The status of the message received.
initWithDictionary | Initializes a [MCDAppServiceClientResponse](MCDAppServiceClientResponse.md) with an **NSDictionary** instance.

## Properties

### message
`@property (nonatomic, readonly, copy, nullable)NSDictionary* message;`

The app service message received.

### status
`@property (nonatomic, readonly)MCDAppServiceResponseStatus status;`

The status of the message received.

## Methods

### initWithDictionary
`-(nullable instancetype)initWithDictionary:(nonnull NSDictionary*)dictionary status:(MCDAppServiceResponseStatus)status;`

Initializes a [MCDAppServiceClientResponse](MCDAppServiceClientResponse.md) with an **NSDictionary** instance.

#### Parameters
* `dictionary` The key-value set of data.
* `status` The status to attach to this response instance.

#### Returns
The initialized [MCDAppServiceClientResponse](MCDAppServiceClientResponse.md) instance.

