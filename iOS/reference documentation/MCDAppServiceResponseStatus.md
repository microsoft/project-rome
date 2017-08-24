# enum `MCDAppServiceResponseStatus`

```
typedef NS_ENUM(NSInteger, MCDAppServiceResponseStatus)
```

Contains values that describe the status of a response message from a remote app service.

## Fields

Name         | Value  | Description                               
--------|-------------|-----
MCDAppServiceResponseStatusSuccess |0| The app service successfully received and processed the message.
MCDAppServiceResponseStatusFailure |1| The app service failed to receive and process the message.
MCDAppServiceResponseStatusResourceLimitsExceeded |2| The app service exited because not enough resources were available.
MCDAppServiceResponseStatusUnknown |3| An unknown error occurred.
MCDAppServiceResponseStatusRemoteSystemUnavailable |4| The device to mwhich the message was sent is not available.
MCDAppServiceResponseStatusMessageTooLarge |5| The app service failed to process the message because it is too large.