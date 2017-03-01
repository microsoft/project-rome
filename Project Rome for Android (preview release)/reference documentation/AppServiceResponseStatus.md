# AppServiceResponseStatus enum
Contains values that describe the status of an app service's response message (whether message data was successfully passed back to the client app).

## Fields

|Member   |Value   |Description   |
|:--------|:-------|:-------------|
|SUCCESS |0 |The response message was received successfully. |
|FAILURE |1 |The response message TBD |
|RESOURCE_LIMITS_EXCEEDED |2 | |
|UNKNOWN |3 | |
|REMOTE_SYSTEM_UNAVAILABLE |4 | |
|MESSAGE_SIZE_TOO_LAEGE |5 | |

## Public methods

### getValue
Returns the int value for this AppServiceResponseStatus.

`public int getValue()`

**Return value**  
The int value for this AppServiceResponseStatus