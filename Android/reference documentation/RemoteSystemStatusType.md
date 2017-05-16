# RemoteSystemStatusType enum
Contains the values that describe a remote system's status type, for the purpose of filtered discovery. This is a simplification of the [**RemoteSystemStatus**](RemoteSystemStatus.md) enumeration and is used to construct a [**RemoteSystemStatusTypeFilter**](RemoteSystemStatusTypeFilter.md) object.

## Fields

|Member   |Value   |Description   |
|:--------|:-------|:-------------|
|AVAILABLE|1|The remote system must have a Status value of **Available** in order to be discoverable.|
|ANY|2|The remote system can have any availability status and be discoverable.|