# enum `MCDRemoteSystemAccessStatus`

```
typedef NS_ENUM(NSInteger, MCDRemoteAccessStatus)
```

Contains values that describe the app's access to use the Connected Devices Platform.

## Fields

|Name |Value |Description
|---|---|---
MCDRemoteSystemUnspecified|0|Access is denied for an unspecified reason.
MCDRemoteSystemAllowed|1| Access is allowed.
MCDRemoteSystemSystemDenied|2| Access is denied to this app by the system.
MCDRemoteSystemUserDenied|3| Access has been denied to this app by this particular user.