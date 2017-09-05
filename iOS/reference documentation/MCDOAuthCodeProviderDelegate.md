# protocol `MCDOAuthCodeProviderDelegate`

```
@protocol MCDOAuthCodeProviderDelegate
```

Receives callbacks from the [MCDPlatform](MCDPlatform.md) when the platform is initialized to get OAuth access tokens.

## Summary
| Members | Descriptions  |                              
|---------|---------------|
|MCDAuthCodeCallback| The block to invoke upon completion of the call to **getAccessCode**.|
|getAccessCode | Asynchronously obtains a new OAuth access code.|

## Structures

### MCDAuthCodeCallback
`typedef void (^MCDAuthCodeCallback)(NSError* _Nullable error, NSString* _Nullable accessCode);`

The block to invoke upon completion of the call to **getAccessCode**.

#### Parameters
* `error` The error, if any occurred.
* `accessCode` The OAuth access code, if one was successfully obtained.

## Methods

### getAccessCode
`-(nullable NSError*)getAccessCode:(nonnull NSString*)signInUri completion:(nullable MCDAuthCodeCallback)completion;`

Called when the Connected Devices platform must obtain a new OAuth access code.

#### Parameters
* `signInUri` URI to retrieve the OAuth access code.
* `completion` The block to invoke upon completion.

#### Returns
An error, if any occurred, along with the OAuth access code.