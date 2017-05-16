# class `CDPlatform` 

```
@interface CDPlatform : NSObject
```  

A static class to perform global scale commands to the Connected Devices platform.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
initWithOAuthCodeProviderDelegate | Initializes the CDP Platform with an instance of the OAuth Token Provider Delegate. Must be invoked before attempting to use any CDP functionality.
suspend | Suspends the CDP Platform. This should be called when your app is sent to the background.
resume | Resumes the CDP platform. This should be called when your app enters the foreground.
shutdown | Shutdown the CDP Platform.

## Members

### initWithOAuthCodeProviderDelegate
`+(void)initWithOAuthCodeProviderDelegate: (id<CDOAuthCodeProviderDelegate>)delegate completion:(void (^)(NSError* error))completionBlock;`

Initializes the CDP Platform with an instance of the OAuth Token Provider Delegate. Must be invoked before attempting to use any CDP functionality. Not thread safe.

#### Parameters
* `delegate` The OAuth token provider delegate.
* `completionBlock` Callback which should be invoked when the OAuth access code is obtained or an error occurs.

### suspend
`+(void)suspend;` 

Suspends the CDP Platform. This should be called when your app is sent to the background.

### resume
`+(void)resume;`

Resumes the CDP platform. This should be called when your app enters the foreground.

### shutdown
`+(void)shutdown;`

Shutdown the CDP Platform.