# protocol `CDOAuthCodeProviderDelegate`

A protocol for returning OAuth information.

`@protocol CDOAuthCodeProviderDelegate`

## Properties
### appId
`@property (nonatomic, readonly, copy, nonnull) NSString* appId;`

The OAuth app id code.

## Methods
### getAccessCode
`-(nullable NSError*)getAccessCode:(nonnull NSString*) signInUri completion:(nullable void (^)(NSError* _Nullable error, NSString* _Nullable accessCode))completionBlock;`

Asynchronously obtains a new OAuth access code.

**Parameters**
* `signinUrl` The URI which should be shown in a WebView.
* `completionBlock` Callback which should be invoked when the OAuth access code is obtained or an error occurs.

**Returns**

An error if there was a failure preparing the async request.
