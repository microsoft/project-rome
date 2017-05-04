# class `BinaryClient` 

```
@interface BinaryClient : NSObject
```  

A class to send binary data to another system using the Connected Devices platform as a client.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
delegate | A delegate to receive events from this [BinaryClient](binaryclient.md).
initWithPeerSystemTarget | Initialize a [BinaryClient](binaryclient.md) to a specific PeerSystemTarget.
sendData | Send a binary message using this [BinaryClient](binaryclient.md).


## Properties

### delegate
`@property (nonatomic, strong) id<BinaryClientDelegate> delegate;`

A delegate to receive events from this [BinaryClient](binaryclient.md).

## Methods

### initWithPeerSystemTarget
`-(id)initWithPeerSystemTarget:(PeerSystemTarget*)peerSystemTarget delegate:(id<BinaryClientDelegate>)delegate;` 

Initialize a [BinaryClient](binaryclient.md) to a specific PeerSystemTarget.

#### Parameters
* `peerSystemTarget` The PeerSystemTarget to communicate with. 
* `delegate` A BinaryClientDelegate to receive events. 

#### Returns
A [BinaryClient](binaryclient.md) object if successful, nil otherwise.

### sendData 
`-(NSError*)sendData:(NSData*)data;` 

Send a binary message across this [BinaryClient](binaryclient.md).

#### Parameters
* `data` The data to send. 

#### Returns
An error if one occurred while sending the message.