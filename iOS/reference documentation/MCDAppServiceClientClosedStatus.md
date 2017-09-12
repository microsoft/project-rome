# enum `MCDAppServiceClientClosedStatus`

```
typedef NS_ENUM(NSInteger, MCDAppServiceClientClosedStatus)
```

Contains values that describe a closed connection to a remote app service.

## Fields

|Member   |Value   |Description   |
|--------|-------|-------------|
|MCDAppServiceClientClosedStatusCompleted |0| The endpoint for the app service closed gracefully.|
|MCDAppServiceClientClosedStatusCanceled |1| The endpoint for the app service was closed by the client or the system.|
|MCDAppServiceClientClosedStatusResourceLimitsExceeded |2| The endpoint for the app service was closed because the endpoint ran out of resources.|
|MCDAppServiceClientClosedStatusUnknown |3| An unknown error occurred.|