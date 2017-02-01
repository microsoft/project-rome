# RemoteSystemConnectionType enum
Contains the values which correspond to the possible types of connections between the client device and remote system.

## Fields

|Member   |Value   |Description   |
|:--------|:-------|:-------------|
|UDP |1 |Connection via User Data Protocol |
|CLOUD |3 |Connection via the cloud |
|RFCOMM |5 |Connection via the RFCOMM Bluetooth protocol |

## Public methods

### getValue
Gets the integer value of this RemoteSystemConnectionType instance

`public int getValue()`

**Return value**  
The integer value of this instance

### fromInt
Gets the ConnectedDevicesError for the given value.

`public static ConnectedDevicesError fromInt(int value)`

**Parameters**  
*value* - the value representing a ConnectedDevicesError

**Return value**  
The ConnectedDevicesError for the given value.

### listFromInt
Returns a list of RemoteSystemConnectionType instances for the array of values provided.

`public static List<RemoteSystemConnectionType> listFromInt(int[] values)`

**Parameters**  
*values* - an array of integer values corresponding to RemoteSystemConnectionType values

**Return value**  
A list of RemoteSystemConnectionType instances corresponding to the values provided. Returns an empty list if *values* does not contain any valid values.



