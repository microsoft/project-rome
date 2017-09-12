# enum `MCDRemoteLauncherUriStatus`

`typedef NS_ENUM(NSInteger, MCDRemoteLauncherUriStatus)`

Contains values that describe the status of a remote app launch using a URI.

## Fields

 Member    |Value   |Description   |                  
------ |------- |--
MCDRemoteLauncherUriStatusUnknown | 0| The status is unknown.
MCDRemoteLauncherUriStatusSuccess | 1| The remote launch was successful.
MCDRemoteLauncherUriStatusAppUnavailable | 2 | The target app is unavailable.
MCDRemoteLauncherUriStatusProtocolUnavailable | 3 | The target app does not support this URI.
MCDRemoteLauncherUriStatusRemoteSystemUnavailable | 4 | The device to which the message was sent is unavailable.
MCDRemoteLauncherUriStatusBundleTooLarge | 5 | The data bundle sent to the target app was too large.
MCDRemoteLauncherUriStatusDeniedByLocalSystem | 6 | The client system has prevented use of the Remote Systems Platform.
MCDRemoteLauncherUriStatusDeniedByRemoteSystem | 7 | The target device has prevented use of the Remote Systems Platform.