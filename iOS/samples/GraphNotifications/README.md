# GraphNotifications sample for iOS

Microsoft Graph notifications is built on the top of UserNotifications feature from [Project Rome](https://developer.microsoft.com/en-us/windows/project-rome) which is the Microsoft's cross-device experiences platform. See the [UserNotifications API reference](https://docs.microsoft.com/en-us/windows/project-rome/notifications/) for how-to guides and API docs that will help you get started integrating UserNotifications feature into your app.

Here you can find the sample app that showcase [Microsoft Graph notifications](https://docs.microsoft.com/en-us/graph/notifications-concept-overview) features on iOS. You will need an iOS app development IDE and iOS device or emulator to use this sample.

Steps to build and run the sample
1. git clone https://github.com/Microsoft/project-rome.git
2. cd ./project-rome/iOS/samples/GraphNotifications/
3. pod install (Note, you might have to do 'pod repo update' and 'pod update' if you run into build issues).
4. Open the generated GraphNotifications.xcworkspace in Xcode to build and run the sample.
5. For the sample to work properly, you need to update the values in 'Secrets.h' file. The values can be obtained by registering your app for authentication and push notifications. See [How-to guide for iOS](https://docs.microsoft.com/en-us/windows/project-rome/notifications/how-to-guide-for-ios) for complete guidance.
