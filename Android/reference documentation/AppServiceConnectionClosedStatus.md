# AppServiceConnectionClosedStatus
Contains the values that describe the reason that an app service connection was closed.

## Fields

|Member   |Value   |Description   |
|:--------|:-------|:-------------|
|COMPLETED |0 | The app service connection was closed intentionally by the client app. |
|CANCELLED |1 | The app service connection was closed due to network failure. |
|RESOURCE_LIMITS_EXCEEDED |2 | The app service connection exceeded its allotted program memory. |
|UNKNOWN |3 | The app service connection closed for an unknown reason.|

## Public methods

### getValue
Returns the int value for this AppServiceConnectionClosedStatus.

`public int getValue()`

#### return value  
The int value for this AppServiceConnectionClosedStatus