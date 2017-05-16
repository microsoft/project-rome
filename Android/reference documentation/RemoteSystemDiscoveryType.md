# RemoteSystemDiscoveryType enum
Contains the values that describe how a remote system is able to be discovered.

## Syntax
`public enum RemoteSystemDiscoveryType`

|Member|Value|Description|
|:---|:---|:---| 
|ANY|0xFFFF|The remote system is discoverable both through a proximal connection or through cloud connection.|
|CLOUD|1|The remote system is only discoverable through cloud connection.|
|PROXIMAL|1 << 1|The remote system is only discoverable through a proximal connection, such as a local network or Bluetooth connection.|

