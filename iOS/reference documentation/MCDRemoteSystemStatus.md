# enum `MCDRemoteSystemStatus`
```
typedef NS_ENUM(NSInteger, MCDRemoteSystemStatus)
```

Contains values that describe the availability of a remote system.

## Fields

Name | Value | Description 
--------------------------------|--------------------------------|------------
MCDRemoteSystemStatusUnknown | 0 | The status is unknown.
MCDRemoteSystemStatusDiscoveringAvailability | 1 | The status of the remote system is being determined.
MCDRemoteSystemStatusAvailable | 2 | The remote system is reported as available.
MCDRemoteSystemStatusUnavailable | 3 | The remote system is reported as unavailable.