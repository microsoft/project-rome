# RemoteSystemDiscovery class
This class handles the discovery of remote systems.

## Syntax
`public final class RemoteSystemDiscovery`

## Nested classes

### RemoteSystemDiscovery.Builder class
Builder class for producing a RemoteSystemDiscovery instance with discovery filters and a listener for discovery events.

## Public methods

### start
Starts watching for discoverable devices, with a default timeout of 5 seconds.

`public void start() throws ConnectedDevicesException`

### stop
Stops watching for discoverable devices.

`public void stop() throws ConnectedDevicesException`

### findByHostName
Attempts to discover a device using an identifying string

`public void findByHostName(String hostname) throws ConnectedDevicesException`

#### Parameters  
*hostname* - the IP address or the machine name of the targeted remote device