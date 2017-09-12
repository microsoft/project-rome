# class `MCDRemoteLauncherOptions` 

```
@interface MCDRemoteLauncherOptions : NSObject <NSCopying>
```  

A class to represent options for the remote launch feature.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
fallbackUri | The fallback URI to launch on the web in case the app launch URI fails.
preferredAppIds | A list of **NSString** objects representing IDs of the apps that should be able to launch with this URI. For Windows apps, the ID will be the app's package family name. 

## Properties

### fallbackUri
`@property (nonatomic, copy, nullable) NSString* fallbackUri;`

The fallback URI to launch on the web in case the app launch URI fails.

### preferredAppIds
`@property (nonatomic, copy, nullable) NSArray* preferredAppIds;`

A list of **NSString** objects representing IDs of the apps that should be able to launch with this URI. For Windows apps, the ID will be the app's package family name.