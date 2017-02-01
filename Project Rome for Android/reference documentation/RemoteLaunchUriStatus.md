# RemoteLaunchUriStatus enum
Specifies the result of an attempt to launch an application on a remote device via URI.

## Fields

|Member   |Value   |Description   |
|:--------|:-------|:-------------|
|UNKNOWN|0|The URI could not be successfully launched on the remote system.|
|SUCCESS|1|The URI was successfully launched on the remote system.|
|APP_UNAVAILABLE|2|The app is not installed on the remote system.|
|PROTOCOL_UNAVAILABLE|3|The application you are trying to activate on the remote system does not support this URI.|
|REMOTE_SYSTEM_UNAVAILABLE|4|The remote system could not be reached.|
|BUNDLE_TOO_LARGE|5|The amount of data you tried to send to the remote system exceeded the limit.|
|DENIED_BY_LOCAL_SYSTEM|6|The user is not authorized to launch an app on a remote system.|
|DENIED_BY_REMOTE_SYSTEM|7|The user is not signed in on the target device or may be blocked by group policy.|

## Public methods

### getValue
Returns the int value for this RemoteLaunchUriStatus.

`public int getValue()`

**Return value**  
The int value for this RemoteLaunchUriStatus

