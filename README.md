# Project Rome

"Project Rome" is a project code name for Microsoft's cross-device experiences platform using the Microsoft Graph. This toolkit, consisting of API sets on multiple development platforms, allows an app on a client (local) device to interact with apps and services on a host (remote) device that is signed in with or receptive to the Microsoft Account (MSA) or Azure Active Directory (AAD) account on the client device. This allows developers to program cross-device and cross-platform experiences that are centered around user tasks rather than devices.

Project Rome is currently implemented for the following scenarios. Follow the links for each corresponding section.

[windows-sdk]:             https://developer.microsoft.com/en-us/windows/downloads
[windows-sdk-badge]:       https://img.shields.io/badge/sdk-Creators%20Update-brightgreen.svg?style=flat-square
[windows-sample]:          https://github.com/Microsoft/Windows-universal-samples/tree/master/Samples/RemoteSystems
[windows-docs]:            https://docs.microsoft.com/en-us/windows/uwp/launch-resume/connected-apps-and-devices

[xamarin-sdk]:             https://www.nuget.org/packages/Microsoft.ConnectedDevices.Xamarin.Droid
[xamarin-sdk-badge]:       https://img.shields.io/nuget/v/Microsoft.ConnectedDevices.Xamarin.Droid.svg?style=flat-square
[xamarin-sample]:          Xamarin/samples

[ios-sdk]:                 https://cocoapods.org/?q=ProjectRomeSdk
[ios-sdk-badge]:           https://img.shields.io/cocoapods/v/ProjectRomeSdk.svg?style=flat-square
[ios-sample]:              iOS/sample 
[ios-docs]:                iOS/reference%20documentation

[android-sdk]:             https://bintray.com/projectrome/maven/public_sdk/_latestVersion
[android-sdk-badge]:       https://img.shields.io/bintray/v/projectrome/maven/public_sdk.svg?style=flat-square
[android-sample]:          Android/sample
[android-docs]:            Android/reference%20documentation

[graph-sdk]:               https://developer.microsoft.com/en-us/graph/code-samples-and-sdks
[graph-sdk-badge]:         https://img.shields.io/badge/REST-Beta-orange.svg?style=flat-square
[graph-sample]:            https://developer.microsoft.com/en-us/graph/code-samples-and-sdks
[graph-docs]:              MSGraph/

|  Platform Samples                       |           SDK Package                           | API Docs
| --------------------------------------: | :---------------------------------------------: | :----------:
| **[Windows][windows-sample]**           |  [![SDK][windows-sdk-badge]][windows-sdk]       | [docs][windows-docs]
| **[Android][android-sample] (Preview)** | [![Maven][android-sdk-badge]][android-sdk]      | [docs][android-docs]
| **[iOS][ios-sample] (Preview)**         |     [![CocoaPod][ios-sdk-badge]][ios-sdk]       | [docs][ios-docs]
| **[Xamarin for Android][xamarin-sample] (Preview)** |[![Nuget][xamarin-sdk-badge]][xamarin-sdk]       | Coming Soon
| **[MSGraph][graph-sample] (Preview)**   |[![REST][graph-sdk-badge]][graph-sdk]            | [docs][graph-docs]

## Project Rome blog posts
* [Cross-device experiences with Project Rome](https://blogs.windows.com/buildingapps/2016/10/11/cross-device-experience-with-project-rome/#iQTseFlAMJRopU9k.97)

* [Going social: Project Rome, Maps, & Social Network Integration](https://blogs.windows.com/buildingapps/2016/10/27/going-social-project-rome-maps-social-network-integration-app-dev-on-xbox-series/#SCfoEZ1q8c1yBMei.97)

* [Announcing Project Rome Android SDK](https://blogs.windows.com/buildingapps/2017/02/08/announcing-project-rome-android-sdk/#obDkvwkXOGa3tcTx.97)

* [Project Rome for Android Update: Now with App Services Support](https://blogs.windows.com/buildingapps/2017/03/23/project-rome-android-update-now-app-services-support/#DBm1Ic4JX8vXv2h0.97)

* [Building a Remote Control Companion App for Android with Project Rome](https://blog.xamarin.com/building-remote-control-companion-app-android-project-rome/)

* [New Share Experience in Windows 10 Creators Update](https://blogs.windows.com/buildingapps/2017/04/06/new-share-experience-windows-10-creators-update/#OGskrWcLLlrCTCSH.97)

* [Web-to-App Linking with AppUriHandlers](https://blogs.windows.com/buildingapps/2016/10/14/web-to-app-linking-with-appurihandlers/#fIh7USaxBYS8JqfT.97)

## MSDN docs and other resources
* [Connected apps and devices (Project "Rome") for UWP](https://docs.microsoft.com/en-us/windows/uwp/launch-resume/connected-apps-and-devices)

MS Graph docs: List the user's devices https://developer.microsoft.com/en-us/graph/docs/api-reference/beta/api/user_list_devices

MS Graph docs: Send a command to a device https://developer.microsoft.com/en-us/graph/docs/api-reference/beta/api/send_device_command

MS Graph docs: Get command status https://developer.microsoft.com/en-us/graph/docs/api-reference/beta/api/get_device_command_status

* [Web-to-app linking](https://docs.microsoft.com/en-us/windows/uwp/launch-resume/web-to-app-linking)

* [//Build 2016 talk](https://channel9.msdn.com/Events/Build/2016/B831)

* [MS Dev Show podcast](http://msdevshow.com/2016/11/project-rome-with-shawn-henry/)


## Privacy
See [Microsoft's privacy statement](https://privacy.microsoft.com/en-us/privacystatement/) for more information. 

## Microsoft Open Source Code of Conduct
This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/).
For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
