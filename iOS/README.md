# Project Rome for iOS (preview release)

Currently, Project Rome is implemented for iOS-client-to-Windows-host scenarios. You will need an iOS app development IDE and an iOS device or emulator to use this feature.

* Download the iOS SDK from the [binaries](binaries/) folder. The SDK is also compatible with the [CocoaPods](https://cocoapods.org/?q=projectrome) dependency manager.
* View the API reference documentation in the [reference documentation](reference%20documentation/) folder.
* See code samples for Rome iOS apps in the [sample](sample/) folder.

## Preliminary setup for Remote Systems functionality on iOS

Before implementing device discovery and connectivity, there are a few steps you'll need to take to give your iOS app the capability to connect to remote Windows devices.

First, you must register your app with Microsoft by following the instructions on the [Microsoft developer portal](https://apps.dev.microsoft.com/). This will allow your app to access Microsoft's Remote Systems platform by having users sign in to their Microsoft accounts (MSAs). You will receive a client ID which you'll use to authenticate your app. To do this in the [sample](sample/) app, replace the value of _appId_ in _AuthenticationViewController.m_ with this value.

The simplest way to add the Remote Systems platform to your iOS app is by using the [CocoaPods](https://cocoapods.org/) dependency manager. Go to your iOS project's *Podfile* and insert the following entry:

```ObjectiveC
platform :ios, '10.0'

target 'ProjectName' do
  pod 'ProjectRomeSdk', '~>0.6.2'
end
```


## Known issues

As this is a preview release, there are some known bugs in the Remote Systems platform for iOS.

|Description | Workaround |
| -----|-----|
|Devices may connect over the cloud and not proximally (if available) on first discovery. | Initiate another discovery using **MCDRemoteSystemDiscoveryManager**. |
| You will receive a linker error regarding bit code in your new project. | Disable bitcode by selecting your project and going to Build Settings -> All -> Enable Bitcode|
|Remote Systems framework includes simulator (x86, x86_64) slices which will fail App Store ingestion. | Remove using lipo|

