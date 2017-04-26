# class `BinaryClient` 

```
class BinaryClient
  : public NSObject
```  

A class to send binary data using CDP as a client.



## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
`public virtual id initWithPeerSystemTarget:delegate:(PeerSystemTarget * peerSystemTarget,id< BinaryClientDelegate > delegate)` | Initialize a [BinaryClient](#interface_binary_client) to a specific PeerSystemTarget.
`public virtual NSError * sendData:(NSData * data)` | Send a binary message across this [BinaryClient](#interface_binary_client).

## Members

#### `public virtual id initWithPeerSystemTarget:delegate:(PeerSystemTarget * peerSystemTarget,id< BinaryClientDelegate > delegate)` 

Initialize a [BinaryClient](#interface_binary_client) to a specific PeerSystemTarget.

#### Parameters
* `peerSystemTarget` The PeerSystemTarget to communicate with. 


* `delegate` A BinaryClientDelegate to receive events. 





#### Returns
A [BinaryClient](#interface_binary_client) object if successful, nil otherwise.

#### `public virtual NSError * sendData:(NSData * data)` 

Send a binary message across this [BinaryClient](#interface_binary_client).

#### Parameters
* `data` The data to send. 





#### Returns
An error if one occured while sending the message.