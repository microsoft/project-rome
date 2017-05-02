# class `BinaryClient` 

```
class BinaryClient
  : public NSObject
```  

A class to send binary data using the Connected Devices platform as a client.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
initWithPeerSystemTarget | Initialize a [BinaryClient](#interface_binary_client) to a specific PeerSystemTarget.
sendData | Send a binary message across this [BinaryClient](#interface_binary_client).

## Members

### initWithPeerSystemTarget
`public virtual id initWithPeerSystemTarget:delegate:(PeerSystemTarget * peerSystemTarget,id< BinaryClientDelegate > delegate)` 

Initialize a [BinaryClient](#interface_binary_client) to a specific PeerSystemTarget.

#### Parameters
* `peerSystemTarget` The PeerSystemTarget to communicate with. 
* `delegate` A BinaryClientDelegate to receive events. 

#### Returns
A [BinaryClient](#interface_binary_client) object if successful, nil otherwise.

### sendData 
`public virtual NSError * sendData:(NSData * data)` 

Send a binary message across this [BinaryClient](#interface_binary_client).

#### Parameters
* `data` The data to send. 

#### Returns
An error if one occurred while sending the message.