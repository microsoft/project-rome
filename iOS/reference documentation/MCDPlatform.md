# class `MCDPlatform` 

```
@interface MCDPlatform : NSObject
```  

A static class to perform global scale commands to the Remote Systems platform.

## Summary

 Members  | Descriptions                                
----------|-----------
startWithOAuthCodeProviderDelegate | Initializes the Remote Systems Platform with an instance of [MCDOAuthCodeProviderDelegate](MCDOAuthCodeProviderDelegate.md). Must be invoked before attempting to use any Remote Systems functionality.
suspend | Suspends the Remote Systems Platform. This should be called when your app is sent to the background.
resume | Resumes the Remote Systems platform. This should be called when your app enters the foreground.
shutdown | Shuts down the Remote Systems Platform.

## Methods

### startWithOAuthCodeProviderDelegate
`+(void) startWithOAuthCodeProviderDelegate: (id<MCDOAuthCodeProviderDelegate>)delegate completion:(void (^)(NSError* error)completion);`

Initializes the Remote Systems Platform with an instance of [MCDOAuthCodeProviderDelegate](MCDOAuthCodeProviderDelegate.md). Must be invoked before attempting to use any Remote Systems functionality. Not thread safe.

#### Parameters
* `delegate` The [MCDOAuthCodeProviderDelegate](MCDOAuthCodeProviderDelegate.md) instance.
* `completion` The block to invoke upon completion.

### suspend
`+(void)suspend;` 

Suspends the Remote Systems Platform. This should be called when your app is sent to the background.

### resume
`+(void)resume;`

Resumes the Remote Systems platform. This should be called when your app enters the foreground.

### shutdown
`+(void)shutdown;`

Shuts down the Remote Systems Platform.