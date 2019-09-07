# Graph Notifications Sample App Service

## Terminology
 
* Sample App/Client App => Windows App/iOS App/Android App/Web App where user signs in 
* Sample API/App Service => Service that wants to post a notification
* Azure AD => Identity Service
* AFS => Internal Microsoft Service to support graph notifications scenario
* UNS ID => User Notification Subscription Identifier that is used to identify a user of a particular app on a device

## Summary

When posting a notification for a user, an App Service (A.S.) like Outlook, needs a way to identify the user. The current Graph Notifications code supports this through **User Notification Subscription ID (a.k.a., UNS ID)**. The client app where the user signs in (like Outlook app on Windows/iOS/Android), can send the UNS ID over to the App Service which can then be used to send notifications to the user. 

## Sample

This is the sample code for an App Service that can create and send a notification for a user using a UNS ID as described above.

## Setting up the sample

* Clone the code from the repo

* Please update the following in the code:
  * Step 0 : AppServiceClientId (in Web.config): This is the App Service Id registered in Azure Portal
  * Step 1 : AppServiceSecret (in Web.config): This is the App Service Secret registered in Azure Portal
  * Step 2 : UnsId (in Web.config): This is the UNS ID that the client app receives as part of the response after subscribing to notifications
  * Step 3 : TargetHostName (Payload.json): This is the Domain name that was verified and associated with the App Service Id in Dev Portal
  * Step 4 : (Optional) TenantId (in UnsIdController.cs) : This is Id of the tenant in which the App Service is created in Azure Portal. This only needs to be updated if the App Service is created in a tenant other than the default tenant

* Deploy locally using IIS Express in VS
 
## Explanation of demo flow: 

* When the demo is launched, the HomeController reads the notification payload and calls the UnsIdController with this payload.
* UnsIdController reads the client ID, client secret and obtains an App Service Auth token. Next, it reads the UnsId and calls Microsoft Graph with the payload to post a notification using the Auth token and UnsId.
  * See PostNotificationRequestResponse.txt for an example on how the request to and response from Microsoft Graph looks like when posting a notification

### Other Useful Links:
 
* [Register an App with Azure Active Directory (AAD) V2 endpoint](https://docs.microsoft.com/en-us/azure/active-directory/develop/quickstart-v2-register-an-app)
* [Authorize access to Azure Active Directory (AAD) web applications](https://docs.microsoft.com/en-us/azure/active-directory/develop/v1-oauth2-client-creds-grant-flow)
 
* How to enable Cross-device experience for notifications:
  * [Guide for Android](https://docs.microsoft.com/en-us/windows/project-rome/notifications/how-to-guide-for-android)
  * [Guide for iOS](https://docs.microsoft.com/en-us/windows/project-rome/notifications/how-to-guide-for-ios)
  * [Guide for Windows](https://docs.microsoft.com/en-us/windows/project-rome/notifications/how-to-guide-for-windows)
 
