# ConnectedDevicesError enum
Indicates an error condition that has occurred in the platform.

## Fields

|Member   |Value   |Description   |
|:--------|:-------|:-------------|
|SUCCESS |0 |The operation was successful. |
|FAIL |0x80000006 |The operation failed. |
|UNKNOWN |0x8FFFFFFF |Unknown. |

## Public methods

### fromInt
Gets the ConnectedDevicesError for the given value.

`public static ConnectedDevicesError fromInt(int value)`

**Parameters**  
*value* - the value representing a ConnectedDevicesError

**Return value**  
The ConnectedDevicesError for the given value.

### getValue
Gets the integer value of this ConnectedDevicesError instance

`public int getValue()`

**Return value**
The integer value of this instance