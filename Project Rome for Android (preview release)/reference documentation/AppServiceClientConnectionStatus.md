# AppServiceClientConnectionStatus enum
Contains the values that describe a connection to a remote app service.

## Fields

|Member   |Value   |Description   |
|:--------|:-------|:-------------|
|UNKNOWN|0|The connection could not be established for an unknown reason.|
|SUCCESS|1|The connection was established successfully. |
|APP_NOT_INSTALLED |3 |TBD |
|APP_UNAVAILABLE |4 |TBD |
|APPSERVICE_UNAVAILABLE |5 | |
|NOT_AUTHORIZED |6 |This system? (TBD) is not authorized |
|REMOTE_SYSTEM_UNAVAILABLE |7 |The target remote device is no longer available for connection.|
|REMOTE_SYSTEM_NOT_SUPPORTEDBYAPP |8 |TBD |

## Public methods

### getValue
Returns the int value for this AppServiceClientConnectionStatus.

`public int getValue()`

**Return value**  
The int value for this AppServiceClientConnectionStatus
