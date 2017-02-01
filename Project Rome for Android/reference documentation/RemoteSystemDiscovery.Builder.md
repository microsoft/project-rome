# RemoteSystemDiscovery.Builder class
Builder class for producing a [**RemoteSystemDiscovery**](RemoteSystemDiscovery.md) instance with discovery filters and a listener for discovery events.

## Syntax
`public static class Builder`

## Public methods

### filter
Adds an **IRemoteSystemFilter** to this builder's list of filters and returns this builder object.

`public Builder filter(IRemoteSystemFilter filter)`

**Parameters**  
*filter* - the **IRemoteSystemFilter** to add

**Return value**  
This builder object

### setListener
Sets an **IRemoteSystemDiscoveryListener** to handle the discovery events that this builder's resulting **RemoteSystemDiscovery** object will raise, and returns this builder object

`public Builder setListener(IRemoteSystemDiscoveryListener listener)`

**Parameters**  
*listener* - the **IRemoteSystemDiscoveryListener** to handle discovery events

**Return value**  
This builder object

### getResult
Creates and returns the **RemoteSystemDiscovery** object from this builder, with the specified filters and event listener.

`public RemoteSystemDiscovery getResult()`

**Return value**  
The **RemoteSystemDiscovery** object created by this builder