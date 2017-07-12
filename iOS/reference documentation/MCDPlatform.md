# class `MCDPlatform` 

```
@interface MCDPlatform : NSObject
```  

A static class to perform global scale commands to the Connected Devices platform.

## Summary

 Members  | Descriptions                                
----------|-----------
startWithOAuthCodeProviderDelegate | Initializes the Connected Devices Platform with an instance of the OAuth Token Provider Delegate. Must be invoked before attempting to use any Connected Devices functionality.
suspend | Suspends the Connected Devices Platform. This should be called when your app is sent to the background.
resume | Resumes the Connected Devices platform. This should be called when your app enters the foreground.
shutdown | Shuts down the Connected Devices Platform.

## Members

### startWithOAuthCodeProviderDelegate
`+(void) startWithOAuthCodeProviderDelegate: (id<MCDOAuthCodeProviderDelegate>)delegate completion:(void (^)(NSError* error)completion);`

Initializes the Connected Devices Platform with an instance of the OAuth Token Provider Delegate. Must be invoked before attempting to use any Connected Devices functionality. Not thread safe.

#### Parameters
* `delegate` The OAuth token provider delegate.
* `completion` The block to invoke upon completion.

### suspend
`+(void)suspend;` 

Suspends the Connected Devices Platform. This should be called when your app is sent to the background.

### resume
`+(void)resume;`

Resumes the Connected Devices platform. This should be called when your app enters the foreground.

### shutdown
`+(void)shutdown;`

Shuts down the Connected Devices Platform.