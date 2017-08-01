# protocol `MCDOAuthCodeProviderDelegate`

```
@protocol MCDOAuthCodeProviderDelegate
```

Receives callbacks from the [MCDPlatform](MCDPlatform.md) when the platform is initialized to get OAuth access tokens.

## Summary
| Members | Descriptions  |                              
|---------|---------------|
|appId | The OAuth app id code.|
|getAccessCode | Asynchronously obtains a new OAuth access code.|

## Properties

### appId
`@property (nonatomic, readonly, copy, nonnull) NSString* appId;`

The OAuth app id code.

## Methods

### getAccessCode
`-(nullable NSError*)getAccessCode:(nonnull NSString*) signInUri completion:(nullable void (^)(NSError* _Nullable error, NSString* _Nullable accessCode))completion;`

#### Parameters
* `signInUri` URI to retrieve the OAuth access code.
* `completion` The block to invoke upon completion.

#### Returns
An error, if any occurred, along with the OAuth access code.