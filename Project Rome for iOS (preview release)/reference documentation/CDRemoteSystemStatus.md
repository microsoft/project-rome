# enum `CDRemoteSystemStatus`
```
typedef NS_ENUM(NSInteger, CDRemoteSystemStatus)
```

Contains values that describe the availability of a remote system.

## Fields

Name | Value | Description 
--------------------------------|--------------------------------|------------
CDRemoteSystemStatusUnknown | 0 | The status is unknown.
CDRemoteSystemStatusDiscoveringAvailability | 1 | The status of the remote system is being determined.
CDRemoteSystemStatusAvailable | 2 | The remote system is reported as available.
CDRemoteSystemStatusUnavailable | 3 | The remote system is reported as unavailable.