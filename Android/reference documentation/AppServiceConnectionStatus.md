# AppServiceConnectionStatus enum
Contains the values that describe a connection to a remote app service.

## Fields

|Member   |Value   |Description   |
|:--------|:-------|:-------------|
|UNKNOWN|0|The connection could not be established for an unknown reason.|
|SUCCESS|1|The connection was established successfully. |
|APP_NOT_INSTALLED |3 |The app service provider indicated by *appServiceName* is not installed on the target device. |
|APP_UNAVAILABLE |4 |The provider app on the target device is not available for remote connection. |
|APPSERVICE_UNAVAILABLE |5 |The app service on the target device is not available for remote connection. |
|NOT_AUTHORIZED |6 |The client device is not authorized to support remote connectivity. |
|REMOTE_SYSTEM_UNAVAILABLE |7 |The target remote device is no longer available for connection.|
|REMOTE_SYSTEM_NOT_SUPPORTEDBYAPP |8 |The client app is not configured to support remote connectivity. |

## Public methods

### getValue
Returns the int value for this AppServiceConnectionStatus.

`public int getValue()`

#### return value  
The int value for this AppServiceConnectionStatus
