# enum `MCDAppServiceClientConnectionStatus`

```
typedef NS_ENUM(NSInteger, MCDAppServiceClientConnectionStatus)
```

Contains values that describe the status of a connection to a remote app service. See [Create and consume an app service](https://docs.microsoft.com/windows/uwp/launch-resume/how-to-create-and-consume-an-app-service) for information on app services on Windows devices.

## Fields

|Member   |Value   |Description   |
|--------|-------|-------------|
|MCDAppServiceClientConnectionStatusSuccess | 0| The connection to the app service was opened successfully.
|MCDAppServiceClientConnectionStatusAppNotInstalled | 1| The package for the app service to which a connection was attempted is not installed on the device. Check that the package is installed before trying to open a connection to the app service.
|MCDAppServiceClientConnectionStatusAppUnavailable | 2| The package for the app service to which a connection was attempted is temporarily unavailable. Try to connect again later.
|MCDAppServiceClientConnectionStatusAppServiceUnavailable | 3| The app with the specified package family name is installed and available, but the app does not declare support for the specified app service. Check that the name of the app service and the version of the app are correct.
|MCDAppServiceClientConnectionStatusUnknown | 4| An unknown error occurred.
|MCDAppServiceClientConnectionStatusRemoteSystemUnavailable | 5| The device to which a connection was attempted is not available.
|MCDAppServiceClientConnectionStatusRemoteSystemNotSupportedByApp | 6| This app does not support remote connections to the device you attempted to connect with.
|MCDAppServiceClientConnectionStatusNotAuthorized | 7| The user of this app is not authorized to connect to the service.