## 1.2.0


### Deprecated (2 changes)

- Add improved async methods for APIs that could hit web endpoints. Deprecate old versions that only returned success/fail.
- Add new mechanism to be able to check up front if a notification is intended for the ConnectedDevicesPlatform. Deprecate old notification processing.


### Fixed (1 change)

- Update UserDataFeedSyncScope to be a class instead of an interface.

## 1.1.0


### Added (1 change)

- Added ConnectedDevicesPlatformSettings.


### Changed (1 change)

- Change ProgressCallback parameters for NearShareSender to instead return AsyncOperationWithProgress objects.


### Fixed (1 change)

- Fix ProcessNotification not being able to be called until RemoteSystemAppRegistration SaveAsync has started.

## 1.0.0


### Changed (42 changes)

- Add copy attribute to Collection properties on MCDRemoteSystemAppRegistration.
- Add EventArgs classes to RemoteSystems Events for future flexibility.
- Add underscrores to some values to correctly separate words in AppServiceConnectionStatus.java.
- Adjust Commanding Events to include EventArgs objects for future flexibility. As a result, refactor RemoteSystemAppRegistration.
- Adjust property annotations for MCDUserNotification.
- Adjust property annotations on MCDUserActivityVisualElements to correctly indicate nullability and copy.
- Adjust UserDataFeed and UserNotifications events to have EventArgs for future maintainability.
- `AppServiceConnection.OpenRemoteAsync` now requires the given `IRemoteSystemConnectionRequest` to contain a `RemoteSystemApplication` when targeting a non-windows device.
- Change AppServiceDescription to AppServiceInfo for better clarity on object usage.
- Change iOS MCDRemoteSystemConnectionInfo isProximal property to be proximal with isProximal getter.
- Change iOS String and Collection read/write properties to correctly indicate copy attribute.
- Change MCDLaunchUriProvider property supportedUriSchemes to be nullable.
- Change MCDRemoteSystemAppRegistration launchUriProvider property to be nullable and readwrite.
- Change MCDUserActivity isRoamable property to roamable with isRoamable getter and setters.
- Change MCDUserActivityState values to include MCDUserActivityState prefix.
- Change MCDUserDataFeedSyncScope to be a protocol.
- Change MCDUserDataFeed userDataFeedForAccount to getForAccount.
- Change ProgressCallback parameters for NearShareSender to instead return AsyncOperationWithProgress objects.
- Change `RemoteLauncherOptions` on `LaunchUriAsync` to be nullable.
- Change RemoteSystemAppRegistration to have clear ownership and change Attributes and AppServiceProviders properties to allow for clearing information.
- Change RemoteSystemPlatform Ios value to be IOS.
- Class `AppServiceConnectionOpenedEventArgs` renamed to `AppServiceConnectionOpenedInfo`.
- Class `RemoteSystemApplication[RegistrationBuilder]` renamed to `RemoteSystemApp[RegistrationBuilder]`.
- Combine RemoteSystemAppCommandingRegistration and RemoteSystemAppHostingRegistration into RemoteSystemAppRegistration.
- Function `IAppServiceConnectionOpenedInfo.GetRemoteSystemApplication` renamed to `IAppServiceConnectionOpenedInfo.GetRemoteSystemApp`.
- Function `IRemoteSystemApplicationRegistration.Start` renamed to `IRemoteSystemApplicationRegistration.Save`.
- Function `RemoteSystem::GetApplications` renamed to `RemoteSystem::GetApps`.
- Change LaunchUriProvider.OnLaunchUriAsync  to accept RemoteLauncherOptions instead of a FallbackUri and PreferredPackageIds.
- Make sure that Connected Devices objects in properties are marked as retain not copy.
- Merge Core and Base namespaces/package/framework into a single root named ConnectedDevices.
- Merge 'RemoteSystemAppRegistrationBuilder' into 'RemoteSystemAppRegistration'.
- Move Commanding and Hosting namespaces to a single new RemoteSystems.Commanding namespace.
- Property `MCDRemoteSystemApplication.applicationId` renamed to `MCDRemoteSystemApp.identifier`.
- Rename AuthorizationKind property to just kind.
- Rename Discovery namespace/package/framework to be RemoteSystems and put NearShare underneath it.
- Rename LocalVisibilityKind property to just kind.
- Rename RemoteLaunchUriStatus DataSetTooLarge to ValueSetTooLarge.
- Rename RemoteSystemPlatform property to just platform.
- Rename and change signature of SendSingleMessageAsync to SendStatelessMessageAsync.
- Reorganize all user data related namespaces to be under a userdata root namespace.
- Rewrite MCDPlatform's initialization path and its interaction model for accounts, notifications, and service registrations.
- Standardize iOS enums to all be NSInteger rather than NSUInteger.


### Added (1 change)

- Add MCDUserNotificationChannel Initializer `initWithUserDataFeed`.


### Fixed (1 change)

- Fix issue in AsyncOperation continuations still being run after manually being completed or cancelled.


### Removed (1 change)

- Remove UserNotificationReaderOptions constructors except for default.

