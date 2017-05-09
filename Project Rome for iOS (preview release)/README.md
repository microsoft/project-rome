# Project Rome for iOS (preview release)

Currently, Project Rome is implemented for iOS-client-to-Windows-host scenarios. You will need an iOS app development IDE and an iOS device or emulator to use this feature.

* Download the iOS SDK from the [binaries](binaries/) folder. The SDK is also compatible with the [CocoaPods](https://cocoapods.org/?q=projectrome) dependency manager.
* View the API reference documentation in the [reference documentation](reference%20documentation/) folder.
* See code samples for Rome iOS apps in the [sample](sample/) folder.

## Known issues

As this is a preview release, there are some known bugs in the Connected Devices platform for iOS.

|Description | Workaround |
| -----|-----|
|Discovery may stop working if the application has been running for over an hour. | Reinitialize the platform by calling **CDPlatform::shutdown** and then **CDPlatform::initWithOAuthCodeProviderDelegate** |
|Devices may connect over the cloud and not proximally (if available) on first discovery. | Initiate another discovery using **CDRemoteSystemDiscovery**. |
| You will receive a linker error regarding bit code in your new project. | Disable bitcode by selecting your project and going to Build Settings -> All -> Enable Bitcode|
|  Any consuming app's documents on drive will grow monotonically in size over time.| No current workaround.|
|App crashes without **CFBundleDisplayName** entry.|Create a **CFBundleDisplayName** entry in your _Info.plist_ file.|
|Connected Devices framework includes simulator (x86, x86_64) slices which will fail App Store ingestion. | Remove using lipo|

