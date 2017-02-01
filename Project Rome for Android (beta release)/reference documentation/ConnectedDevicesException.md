# ConnectedDevicesException class
Defines a Remote-Systems-specific exception.

## Syntax
`public final class ConnectedDevicesException extends Exception`

## Public fields

### result
The failure code associated with the exception

`public final ConnectedDevicesResult result`

## Public constructors

### ConnectedDevicesException
Initializes a new instance of the ConnectedDevicesException class with a message.

`public ConnectedDevicesException(String message)`

**Parameters**  
*message* - the message for this exception

### ConnectedDevicesException
Initializes a new instance of the ConnectedDevicesException class with a message and error codes.

`public ConnectedDevicesException(int independentError, int platformError, String message)`

**Parameters**  
*independentError* - the Remote Systems error code for this exception  
*platformError* - the Android system error code for this exception  
*message* - the message for this exception  

### ConnectedDevicesException
Initializes a new instance of the ConnectedDevicesException class with a message, error codes, and a **Throwable** cause.

`public ConnectedDevicesException(int independentError, int platformError, String message, Throwable throwable)`

**Parameters**  
*independentError* - the Remote Systems error code for this exception  
*platformError* - the Android system error code for this exception  
*message* - the message for this exception  
*throwable* - the cause of this exception  