# Project Rome

"Project Rome" is a project code name for Microsoft's cross-device experiences platform exposed via the Microsoft Graph and platform-specific native SDKs. This SDK toolkit, consisting of API sets on multiple development platforms, enables multiple cross-device and connected-device feature capability sets that allow yorur apps on client devices to interact with your apps and services backend, all centralized around a logged in user identity. The user identity is represented by Microsoft Account (MSA) or Azure Active Directiory (AAD) account. These capability sets include but are not limited to user activities, notifications, device relay, and nearby share.  

Visit the [Project Rome landing page](https://developer.microsoft.com/en-us/windows/project-rome) for more general information about Project Rome.

See the [Project Rome docs](https://docs.microsoft.com/windows/project-rome/) for how-to guides and API reference docs that will help you get started integrating Project Rome features into your app.

See the [Cross-Device Experience docs](https://developer.microsoft.com/en-us/graph/docs/concepts/cross-device-concept-overview) under Microsoft Graph node to find out more about how Project Rome feature capabilities are exposed via Microsoft Graph REST API endpoint. 

## SDK

Project Rome is currently implemented for the below platforms. Follow the links for samples and SDK downloads.

[windows-sdk]:             https://developer.microsoft.com/en-us/windows/downloads
[windows-sdk-badge]:       https://img.shields.io/badge/sdk-April%202018%20Update-brightgreen.svg
[windows-sample]:          https://github.com/Microsoft/Windows-universal-samples/tree/master/Samples/RemoteSystems

[winredist-sdk]:           https://www.nuget.org/packages/Microsoft.ConnectedDevices.UserNotifications
[winredist-sdk-badge]:     https://img.shields.io/nuget/v/Microsoft.ConnectedDevices.UserNotifications.svg
[winredist-sample]:        Windows/samples

[xamarin-sdk]:             https://www.nuget.org/packages/Microsoft.ConnectedDevices.Xamarin.Droid
[xamarin-sdk-badge]:       https://img.shields.io/nuget/v/Microsoft.ConnectedDevices.Xamarin.Droid.svg
[xamarin-sample]:          https://github.com/Microsoft/project-rome/tree/0.8.1/Xamarin/samples

[ios-sdk]:                 https://cocoapods.org/pods/ProjectRomeSdk
[ios-sdk-badge]:           https://img.shields.io/cocoapods/v/ProjectRomeSdk.svg
[ios-sample]:              iOS/samples 

[android-sdk]:             https://bintray.com/projectrome/maven/com.microsoft.connecteddevices:connecteddevices-sdk/_latestVersion
[android-sdk-badge]:       https://api.bintray.com/packages/projectrome/maven/com.microsoft.connecteddevices%3Aconnecteddevices-sdk/images/download.svg
[android-sample]:          Android/samples


[graph-relay]:             https://developer.microsoft.com/graph/docs/api-reference/beta/resources/project_rome_overview
[graph-activities]:        https://developer.microsoft.com/graph/docs/api-reference/v1.0/resources/activity-feed-api-overview
[graph-notification]:      https://developer.microsoft.com/graph/docs/api-reference/beta/resources/notifications-api-overview

[graph-relay-badge]:       https://img.shields.io/badge/Device_Relay-Beta-orange.svg
[graph-activities-badge]:  https://img.shields.io/badge/Activities-1.0-brightgreen.svg
[graph-notification-badge]:https://img.shields.io/badge/Graph_Notifications-Beta-orange.svg

[graph-relay-sample]:        https://developer.microsoft.com/graph/docs/api-reference/beta/resources/project_rome_overview
[graph-activities-sample]:   https://developer.microsoft.com/graph/docs/api-reference/v1.0/resources/activity-feed-api-overview
[graph-notification-sample]: https://developer.microsoft.com/graph/docs/api-reference/beta/resources/notifications-api-overview



|   Platform                        | Features                                                         |           SDK Package                          |   Samples                                       |
| :-------------------------------- | :--------------------------------------------------------------- |:---------------------------------------------- | :---------------------------------------------- |
| **Windows SDK**                   | Device Relay, Activities/Timeline                                | [![SDK][windows-sdk-badge]][windows-sdk]       | [Project Rome for Windows samples][windows-sample] (GitHub)
| **Windows (Preview)**             |                                    Microsoft Graph Notifications | [![Nuget][winredist-sdk-badge]][winredist-sdk] | [Graph Notifications for Windows samples][winredist-sample] (GitHub)
| **Android (Preview)**             | Device Relay, Activities/Timeline, Microsoft Graph Notifications | [![Maven][android-sdk-badge]][android-sdk]     | [Project Rome for Android samples][android-sample] (GitHub)
| **iOS (Preview)**                 | Device Relay, Activities/Timeline, Microsoft Graph Notifications | [![CocoaPod][ios-sdk-badge]][ios-sdk]          | [Project Rome for iOS samples][ios-sample] (Preview)
| **Xamarin for Android (Preview)** | Device Relay                                                     | [![Nuget][xamarin-sdk-badge]][xamarin-sdk]     | [Xamarin for Android samples][xamarin-sample] (Preview)
| **MSGraph**                       | Device Relay, Activities/Timeline, Microsoft Graph Notifications | [![REST][graph-relay-badge]][graph-relay]<br> [![REST][graph-activities-badge]][graph-activities]<br>[![REST][graph-notification-badge]][graph-notification]          | [Device Relay][graph-relay-sample](Preview)<br>[Activities/Timeline][graph-activities-sample]<br>[Graph Notifications][graph-notification-sample](Preview)

## Project Rome blog posts
* [Cross-device experiences with Project Rome](https://blogs.windows.com/buildingapps/2016/10/11/cross-device-experience-with-project-rome/#iQTseFlAMJRopU9k.97)

* [Going social: Project Rome, Maps, & Social Network Integration](https://blogs.windows.com/buildingapps/2016/10/27/going-social-project-rome-maps-social-network-integration-app-dev-on-xbox-series/#SCfoEZ1q8c1yBMei.97)

* [Announcing Project Rome Android SDK](https://blogs.windows.com/buildingapps/2017/02/08/announcing-project-rome-android-sdk/#obDkvwkXOGa3tcTx.97)

* [Project Rome for Android Update: Now with App Services Support](https://blogs.windows.com/buildingapps/2017/03/23/project-rome-android-update-now-app-services-support/#DBm1Ic4JX8vXv2h0.97)

* [Building a Remote Control Companion App for Android with Project Rome](https://blog.xamarin.com/building-remote-control-companion-app-android-project-rome/)

* [New Share Experience in Windows 10 Creators Update](https://blogs.windows.com/buildingapps/2017/04/06/new-share-experience-windows-10-creators-update/#OGskrWcLLlrCTCSH.97)

* [Web-to-App Linking with AppUriHandlers](https://blogs.windows.com/buildingapps/2016/10/14/web-to-app-linking-with-appurihandlers/#fIh7USaxBYS8JqfT.97)

## Other resources

* [Web-to-app linking](https://docs.microsoft.com/en-us/windows/uwp/launch-resume/web-to-app-linking)

* [//Build 2016 talk](https://channel9.msdn.com/Events/Build/2016/B831)

* [MS Dev Show podcast](http://msdevshow.com/2016/11/project-rome-with-shawn-henry/)


## Privacy
See [Microsoft's privacy statement](https://privacy.microsoft.com/en-us/privacystatement/) for more information. 

## Microsoft Open Source Code of Conduct
This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/).
For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

## Give feedback

|[UserVoice](https://wpdev.uservoice.com/forums/110705-universal-windows-platform/category/183208-connected-apps-and-devices-project-rome)|[Feedback Hub](https://support.microsoft.com/en-us/help/4021566/windows-10-send-feedback-to-microsoft-with-feedback-hub-app)|[Contact Us](mailto:projectrometeam@microsoft.com)|
|-----|-----|-----|