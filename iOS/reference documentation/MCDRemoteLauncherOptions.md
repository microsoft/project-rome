# class `MCDRemoteLauncherOptions` 

```
@interface MCDRemoteLauncherOptions : NSObject <NSCopying>
```  

A class to represent options for the remote launch feature.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
fallbackUri | The fallback URI to launch on the web in case the app launch URI fails.
preferredAppIds | The list of IDs of the apps which should be able to launch with this URI.

## Properties

### fallbackUri
`@property (nonatomic, readonly, copy, nullable) NSString* fallbackUri;`

The fallback URI to launch on the web in case the app launch URI fails.

### preferredAppIds
`@property (nonatomic, readonly, copy, nullable) NSArray* preferredAppIds;`

The list of IDs of the apps which should be able to launch with this URI.