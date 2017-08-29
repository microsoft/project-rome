# RemoteSystem class
This class manages the attributes of a discovered remote system (device) which may be reachable by UDP, Bluetooth, or cloud connection.

## Syntax
`public final class RemoteSystem extends Listenable<IRemoteSystemListener>`

## Public methods

### getDisplayName
Returns the "friendly name" of the remote system.

`public String getDisplayName()`

#### return value  
The "friendly name" of the remote system
   
### getKind
Returns the kind of the remote system.

`public RemoteSystemKinds getKind()`

#### return value  
The **RemoteSystemKinds** value corresponding to the kind of device this remote system is classified as

### getStatus
Returns the status of the remote system.

`public RemoteSystemStatus getStatus()`

#### return value  
The **RemoteSystemStatus** value representing the remote system's availability status

### getId
Returns the unique device id of the remote system.

`public String getId()`

#### return value  
The unique device id of the remote system

### isAvailableByProximity
Checks whether the device is available by either UDP or Bluetooth.

`public boolean isAvailableByProximity()`

#### return value  
**true** if the the device is available by UDP or Bluetooth, otherwise **false**

### isAvailableByCloud
Checks whether the device is available cloud connection.

`public boolean isAvailableByCloud()`

#### return value  
**true** if the the device is available by cloud connection, otherwise **false**
